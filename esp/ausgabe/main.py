# Ausgabe des B.A.M.B.I.-Automaten (MicroPython, ESP32)
#
# * Beantwortet die Anfragen des Automaten: /ping, /dispense, /status.
# * Gibt Süßigkeiten nacheinander aus einer Warteschlange aus - der Server
#   bleibt dabei erreichbar (früher lief der Motor im Server selbst, und der
#   Automat hielt die Ausgabe währenddessen für ausgefallen).
# * Meldet Ausgaben ohne Lichtschranken-Signal ans Backend (/dispense/failed),
#   das Fach wird dann gesperrt.
#
# Einstellungen stehen in config.py.

import time
import socket
import network
import _thread
import ujson
import hashlib
import binascii
import os
import ntptime
import hmac
from machine import Pin, RTC
import config

# Vom Automaten gelernt (Absender des ersten /ping).
state = {"server_ip": None, "busy": False}

# time.time() zählt auf dem ESP32 ab 2000, Unix-Zeit ab 1970.
EPOCH_OFFSET = 946684800 if time.gmtime(0)[0] == 2000 else 0


def log(*args):
    print("[ausgabe]", *args)


# ---------------------------------------------------------------------------
# Motor und Lichtschranken (unverändert aus dem bisherigen boot.py)
# ---------------------------------------------------------------------------

A = Pin(2, Pin.OUT)  # 2-13
B = Pin(15, Pin.OUT)  # 15-4
C = Pin(0, Pin.OUT)  # 0-2
STEP = Pin(13, Pin.OUT)
DIR = Pin(4, Pin.OUT)
ALWAYS_ON = Pin(12, Pin.OUT)

direction = 0
ALWAYS_ON.value(1)

SENSES = {
    1: Pin(14, Pin.IN),
    2: Pin(27, Pin.IN),
    3: Pin(26, Pin.IN),
    4: Pin(25, Pin.IN),
    5: Pin(32, Pin.IN),
    6: Pin(33, Pin.IN),
    7: Pin(34, Pin.IN),
}

mapping = [
    [0, 0, 0],  # A;B;C
    [1, 0, 0],
    [0, 1, 0],
    [1, 1, 0],
    [0, 0, 1],
    [1, 0, 1],
    [0, 1, 1],
    [1, 1, 1]
]

A.value(0)
B.value(0)
C.value(0)


def step(num):
    """Dreht Fach num, bis die Lichtschranke eine Süßigkeit gesehen hat.
    Gibt zurück, ob das geklappt hat."""
    sense = SENSES[num]
    DIR.value(direction)
    time.sleep(2)
    was_one = 0
    success = False
    # Fach 6 dreht nur halb so weit und danach ein Stück weiter (Eigenheit dieses Geräts).
    for x in range(750 // 2 if num == 6 else 750):
        STEP.on()
        time.sleep(1 / 200)
        STEP.off()
        time.sleep(1 / 200)
        if sense.value() == 1:
            was_one = was_one + 1
        elif was_one >= 3:
            log("Lichtschranke ausgelöst (Motor", num, ")")
            success = True
            break
        else:
            was_one = 0
    for x in range(750 // 2 if num == 6 else 4):
        STEP.on()
        time.sleep(1 / 200)
        STEP.off()
        time.sleep(1 / 200)
    STEP.value(0)
    DIR.value(0)
    time.sleep(1)
    ALWAYS_ON.value(0)
    A.value(0)
    B.value(0)
    C.value(0)
    ALWAYS_ON.value(1)
    return success


def dispense(x):
    x += 1
    if x >= 8:
        x = 1
    y = mapping[x]
    A.value(y[0])
    B.value(y[1])
    C.value(y[2])
    return step(x)


# ---------------------------------------------------------------------------
# Warteschlange: eine Ausgabe nach der anderen
# ---------------------------------------------------------------------------

queue = []
queue_lock = _thread.allocate_lock()


def enqueue(nr):
    queue_lock.acquire()
    try:
        queue.append(nr)
        return len(queue)
    finally:
        queue_lock.release()


def worker():
    while True:
        queue_lock.acquire()
        try:
            nr = queue.pop(0) if queue else None
        finally:
            queue_lock.release()
        if nr is None:
            time.sleep_ms(50)
            continue
        state["busy"] = True
        log("Gebe Fach", nr, "aus")
        try:
            ok = dispense(nr)
        except Exception as e:
            log("Motorfehler:", e)
            ok = False
        state["busy"] = False
        if not ok:
            log("Keine Süßigkeit erkannt, Fach", nr)
            report_failed(nr)


# ---------------------------------------------------------------------------
# Zeit und Signatur (für die Meldung ans Backend)
# ---------------------------------------------------------------------------

def sync_time():
    """NTP über mehrere Server (auch das Gateway). Klappt das nicht, stellt der
    Automat die Uhr über den Kopf X-Unix-Time seiner Pings."""
    hosts = ["pool.ntp.org", "de.pool.ntp.org", "time.google.com"]
    try:
        hosts.append(network.WLAN(network.STA_IF).ifconfig()[2])
    except Exception:
        pass
    for host in hosts:
        for attempt in range(2):
            try:
                ntptime.host = host
                ntptime.settime()
                log("Uhrzeit per NTP von", host)
                return True
            except Exception as e:
                log("NTP über", host, "fehlgeschlagen:", e)
                time.sleep(1)
    log("NTP nicht erreichbar - warte auf die Uhrzeit vom Automaten")
    return False


def set_time_from_unix(unix_ts):
    tm = time.gmtime(unix_ts - EPOCH_OFFSET)
    RTC().datetime((tm[0], tm[1], tm[2], tm[6], tm[3], tm[4], tm[5], 0))


def random_string(length):
    chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    return "".join(chars[b % len(chars)] for b in os.urandom(length))


def signed_headers(body):
    # Muss zur Prüfung im Backend passen (HMACFilter: Abweichung höchstens 30 s).
    timestamp = int(time.time()) + EPOCH_OFFSET
    nonce = random_string(16)
    data = "{}:{}:{}:{}".format(config.HMAC_KEY_ID, timestamp, nonce, body).encode()
    signature = binascii.b2a_base64(hmac.new(config.HMAC_KEY, data, hashlib.sha256).digest()).decode().strip()
    return {
        "X-API-KEY-ID": config.HMAC_KEY_ID,
        "X-TIMESTAMP": str(timestamp),
        "X-NONCE": nonce,
        "X-SIGNATURE": signature,
        "Content-Type": "application/json",
    }


def http_post(host, port, path, body, headers, timeout=5):
    """Schickt einen POST und gibt den HTTP-Status zurück."""
    sock = socket.socket()
    sock.settimeout(timeout)
    try:
        sock.connect(socket.getaddrinfo(host, port)[0][-1])
        data = body.encode()
        head = "POST {} HTTP/1.1\r\nHost: {}:{}\r\n".format(path, host, port)
        for key in headers:
            head += "{}: {}\r\n".format(key, headers[key])
        head += "Content-Length: {}\r\nConnection: close\r\n\r\n".format(len(data))
        sock.sendall(head.encode() + data)
        return int(sock.recv(64).split()[1])
    finally:
        sock.close()


def report_failed(nr):
    server = config.SERVER_IP or state["server_ip"]
    if not server:
        log("Fehlschlag nicht gemeldet: noch kein /ping vom Automaten")
        return
    body = ujson.dumps({"nr": nr})
    for attempt in range(3):
        try:
            status = http_post(server, config.SERVER_PORT, "/dispense/failed", body, signed_headers(body))
            if status == 200:
                log("Fehlschlag von Fach", nr, "gemeldet - Fach ist gesperrt")
                return
            log("Backend lehnt die Meldung ab, Status", status)
        except Exception as e:
            log("Backend nicht erreichbar:", e)
        time.sleep(2)


# ---------------------------------------------------------------------------
# HTTP-Server für den Automaten
# ---------------------------------------------------------------------------

def respond(client, status, body, content_type="text/plain; charset=utf-8"):
    reason = {200: "OK", 400: "Bad Request", 404: "Not Found"}.get(status, "OK")
    data = body.encode()
    head = "HTTP/1.1 {} {}\r\nContent-Type: {}\r\nContent-Length: {}\r\nConnection: close\r\n\r\n".format(
        status, reason, content_type, len(data))
    client.sendall(head.encode() + data)


def read_request(client):
    """Liest Kopf und (per Content-Length) den ganzen Körper - der kann in
    einem zweiten Netzwerkpaket kommen."""
    data = b""
    while data.find(b"\r\n\r\n") < 0 and len(data) < 8192:
        chunk = client.recv(512)
        if not chunk:
            break
        data += chunk
    end = data.find(b"\r\n\r\n")
    head = data[:end] if end >= 0 else data
    body = data[end + 4:] if end >= 0 else b""
    lines = head.decode().split("\r\n")
    parts = lines[0].split()
    headers = {}
    for line in lines[1:]:
        if ":" in line:
            key, value = line.split(":", 1)
            headers[key.strip().lower()] = value.strip()
    length = int(headers.get("content-length", "0"))
    while len(body) < length:
        chunk = client.recv(512)
        if not chunk:
            break
        body += chunk
    method = parts[0] if parts else ""
    path = parts[1].split("?")[0] if len(parts) > 1 else ""
    return method, path, headers, body


def handle(client, address):
    try:
        client.settimeout(3)
        method, path, headers, body = read_request(client)

        if path in ("/ping", "/dispense", "/status"):
            if state["server_ip"] != address[0]:
                state["server_ip"] = address[0]
                log("Automat unter", address[0])

        if path == "/ping":
            if "x-unix-time" in headers:
                try:
                    set_time_from_unix(int(headers["x-unix-time"]))
                except Exception as e:
                    log("Uhrzeit aus /ping nicht übernommen:", e)
            respond(client, 200, "1")
        elif path == "/dispense" and method == "POST":
            try:
                nr = int(ujson.loads(body)["nr"])
            except Exception:
                respond(client, 400, "nr fehlt")
                return
            if not 0 <= nr <= 7:
                respond(client, 400, "ungueltiges Fach")
                return
            # Sofort bestätigen: die Ausgabe selbst dauert Sekunden.
            waiting = enqueue(nr)
            log("Ausgabe Fach", nr, "angenommen, in Warteschlange:", waiting)
            respond(client, 200, "success")
        elif path == "/status":
            respond(client, 200, ujson.dumps({
                "busy": state["busy"],
                "queue": len(queue),
                "server": config.SERVER_IP or state["server_ip"],
                "time": int(time.time()) + EPOCH_OFFSET,
            }), "application/json")
        else:
            respond(client, 404, "not found")
    except Exception as e:
        log("Anfrage von", address[0], "fehlgeschlagen:", e)
    finally:
        client.close()


def serve(port):
    # Ein Fehler darf den Server nie dauerhaft beenden - neu starten.
    while True:
        server = None
        try:
            server = socket.socket()
            server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            server.bind(socket.getaddrinfo("0.0.0.0", port)[0][-1])
            server.listen(4)
            log("HTTP-Server auf Port", port)
            while True:
                client, address = server.accept()
                handle(client, address)
        except Exception as e:
            log("Server-Fehler, starte neu:", e)
            if server:
                server.close()
            time.sleep(1)


# ---------------------------------------------------------------------------
# WLAN und Start
# ---------------------------------------------------------------------------

def connect_wlan(wlan):
    if not wlan.isconnected():
        log("Verbinde mit WLAN", config.WLAN_SSID)
        wlan.connect(config.WLAN_SSID, config.WLAN_PASSWORD)
        started = time.ticks_ms()
        while not wlan.isconnected():
            if time.ticks_diff(time.ticks_ms(), started) > 20000:
                log("WLAN nicht erreichbar, neuer Versuch")
                wlan.disconnect()
                wlan.connect(config.WLAN_SSID, config.WLAN_PASSWORD)
                started = time.ticks_ms()
            time.sleep_ms(200)
    if config.STATIC_IP:
        # Feste Adresse, Netzmaske/Gateway/DNS aber vom DHCP übernehmen.
        ip, mask, gateway, dns = wlan.ifconfig()
        if ip != config.STATIC_IP:
            wlan.ifconfig((config.STATIC_IP, mask, gateway, dns))
    log("WLAN verbunden:", wlan.ifconfig())


def main():
    wlan = network.WLAN(network.STA_IF)
    wlan.active(False)
    time.sleep_ms(100)
    wlan.active(True)
    connect_wlan(wlan)
    sync_time()
    _thread.start_new_thread(worker, ())
    _thread.start_new_thread(serve, (config.HTTP_PORT,))
    # WLAN im Blick behalten; fällt es weg, neu verbinden.
    while True:
        time.sleep(5)
        if not wlan.isconnected():
            connect_wlan(wlan)


if __name__ == "__main__":
    main()
