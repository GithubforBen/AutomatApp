# Ausgabe des B.A.M.B.I.-Automaten (MicroPython, ESP32)
#
# * Beantwortet die Anfragen des Automaten: /ping, /dispense, /status.
# * Gibt Süßigkeiten nacheinander aus einer Warteschlange aus - der Server
#   bleibt dabei erreichbar (früher lief der Motor im Server selbst, und der
#   Automat hielt die Ausgabe währenddessen für ausgefallen).
# * Meldet jede Ausgabe ans Backend: /dispense/success, wenn die Lichtschranke
#   die Süßigkeit gesehen hat, sonst /dispense/failed. Bleibt die Erfolgsmeldung
#   aus, sperrt das Backend das Fach ("Fächer automatisch sperren").
# * Spricht eine Lichtschranke an, ohne dass etwas ausgegeben wird (jemand greift
#   in den Automaten), meldet sie /dispense/unexpected - das Backend löst den
#   Alarm am Scanner aus.
# * /dispense und /update nimmt sie nur vom Automaten an: Die Anfragen sind mit
#   dem HMAC-Schlüssel signiert (signing.py) - sonst könnte jeder im WLAN
#   Süßigkeiten ausgeben lassen.
#
# Einstellungen stehen in config.py.

# Zuerst die Motor-Pins: Beim Start ziehen die Boot-Pins GPIO 0 und 15 (C, B)
# hoch, damit ist ein Fach ausgewählt und hat Haltestrom. Das soll nur so kurz
# wie möglich dauern - also vor allen anderen Imports (config/.env, ota,
# signing), die Zeit brauchen und auch fehlschlagen können.
from machine import Pin
from time import sleep_us

A = Pin(2, Pin.OUT)  # 2-13
B = Pin(15, Pin.OUT)  # 15-4
C = Pin(0, Pin.OUT)  # 0-2
STEP = Pin(13, Pin.OUT)
DIR = Pin(4, Pin.OUT)
ALWAYS_ON = Pin(12, Pin.OUT)

direction = 0
ALWAYS_ON.value(1)


def idle_pins():
    """Kein Fach ausgewählt (A/B/C = 0) - die Inverter ziehen alle Enable-Pins
    hoch, kein Motor hat Haltestrom. Mehrfach aufrufen schadet nicht."""
    STEP.value(0)
    DIR.value(0)
    A.value(0)
    B.value(0)
    C.value(0)
    ALWAYS_ON.value(1)


def release_motors():
    """Haltestrom aus für alle Motoren.

    Der Multiplexer verbindet nur den gewählten Kanal; die Eingänge der
    Inverter an den anderen Kanälen hängen dann offen und behalten ihre
    Ladung. Ein Eingang, der einmal hoch war (z.B. beim Booten, wenn die
    Boot-Pins einen Kanal wählen), hält seinen Motor sonst weiter aktiv.
    Deshalb jeden Kanal 1-7 einmal mit ALWAYS_ON = 0 wählen (Eingang entladen,
    Enable bleibt hoch) und erst dann Kanal 0 - der hat keinen Motor."""
    ALWAYS_ON.value(0)
    for bits in ((1, 1, 1), (0, 1, 1), (1, 0, 1), (0, 0, 1), (1, 1, 0), (0, 1, 0), (1, 0, 0)):
        A.value(bits[0])
        B.value(bits[1])
        C.value(bits[2])
        sleep_us(50)
    idle_pins()


release_motors()

import time
import socket
import network
import _thread
import ujson
import config
import ota
import signing

# Vom Automaten gelernt (Absender des ersten /ping). last_end: Ende der letzten
# Ausgabe (ticks_ms) - kurz danach darf eine Lichtschranke noch nachzucken.
state = {"server_ip": None, "busy": False, "last_end": time.ticks_ms()}



def log(*args):
    print("[ausgabe]", *args)


# ---------------------------------------------------------------------------
# Motor und Lichtschranken (unverändert aus dem bisherigen boot.py)
# ---------------------------------------------------------------------------

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
    release_motors()
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

# Gesperrt, solange der Motor dreht. Die Schrittimpulse kommen aus einer
# Python-Schleife mit 5-ms-Pausen; jeder andere Thread, der in der Zeit rechnet
# oder in einer kurzen Pause wartet, hält die Interpreter-Sperre (GIL) und
# verzögert einzelne Schritte - der Motor läuft dann unrund. Deshalb warten
# Lichtschranken-Überwachung und Meldungen hier, ohne Rechenzeit zu brauchen.
motor_lock = _thread.allocate_lock()


def wait_motor_idle():
    motor_lock.acquire()
    motor_lock.release()


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
        motor_lock.acquire()
        try:
            ok = dispense(nr)
        except Exception as e:
            log("Motorfehler:", e)
            ok = False
            release_motors()
        finally:
            state["last_end"] = time.ticks_ms()
            state["busy"] = False
            motor_lock.release()
        if ok:
            report("/dispense/success", nr)
        else:
            log("Keine Süßigkeit erkannt, Fach", nr)
            report("/dispense/failed", nr)


# ---------------------------------------------------------------------------
# Zeit und Signatur (für die Meldung ans Backend)
# ---------------------------------------------------------------------------

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


# Meldungen ans Backend laufen über einen eigenen Thread - so wartet weder die
# Ausgabe noch die Überwachung der Lichtschranken auf das Netz.
outbox = []
outbox_lock = _thread.allocate_lock()


def report(path, nr, details=None):
    outbox_lock.acquire()
    try:
        outbox.append((path, nr, details))
    finally:
        outbox_lock.release()


def send_report(path, nr, details=None):
    server = config.SERVER_IP or state["server_ip"]
    if not server:
        log(path, "nicht gemeldet: noch kein /ping vom Automaten")
        return
    data = {"nr": nr}
    if details:
        data.update(details)
    body = ujson.dumps(data)
    for attempt in range(3):
        # Nicht senden, während der Motor dreht (z.B. nächstes Fach in der Warteschlange).
        wait_motor_idle()
        try:
            headers = signing.headers(config.HMAC_KEY_ID, config.HMAC_KEY, body)
            headers["Content-Type"] = "application/json"
            status = http_post(server, config.SERVER_PORT, path, body, headers)
            if status == 200:
                log(path, "für Fach", nr, "gemeldet")
                return
            log("Backend lehnt", path, "ab, Status", status)
        except Exception as e:
            log("Backend nicht erreichbar:", e)
        time.sleep(2)


def reporter():
    # Jede Sperre wird im selben Thread freigegeben, der sie geholt hat - ältere
    # MicroPython-Versionen stürzen sonst ab (FreeRTOS-Mutex).
    while True:
        # Während der Motor dreht, hier warten statt nachzusehen.
        wait_motor_idle()
        outbox_lock.acquire()
        try:
            item = outbox.pop(0) if outbox else None
        finally:
            outbox_lock.release()
        if item is None:
            time.sleep_ms(200)
            continue
        try:
            send_report(item[0], item[1], item[2])
        except Exception as e:
            log("Meldung fehlgeschlagen:", e)


# ---------------------------------------------------------------------------
# Alarm: Lichtschranke ohne Ausgabe
# ---------------------------------------------------------------------------

# Entprellung über die Zeit, nicht über die Zahl der Messungen: Stockt die
# Schleife (WLAN, andere Threads), lägen "aufeinanderfolgende" Messungen
# sonst weit auseinander, und drei einzelne Störimpulse sähen aus wie eine
# lange Unterbrechung.
#
# blocked_since[num]: seit wann (ticks_ms) ununterbrochen unterbrochen, sonst None.
blocked_since = {num: None for num in SENSES}
# Kurze Impulse je Lichtschranke seit der letzten Meldung: [Anzahl, längster in ms].
glitches = {num: [0, 0] for num in SENSES}
# Wann je Fach zuletzt ein Eingriff gemeldet wurde (ticks_ms).
last_alarm = {}
# Letzte Messung (ticks_ms) - liegt sie zu lange zurück, gilt eine laufende
# Unterbrechung als unbekannt und beginnt neu.
last_sample = [None]
# Letzte Meldung, für /status.
last_trigger = [None]
MAX_SAMPLE_GAP_MS = 50
BOOT_TICKS = time.ticks_ms()


def min_blocked_ms():
    return getattr(config, "ALARM_MIN_BLOCKED_MS", 150)


def dispensing():
    if state["busy"] or queue:
        return True
    return time.ticks_diff(time.ticks_ms(), state["last_end"]) < config.ALARM_GRACE_MS


def sensor_values():
    return "".join(str(SENSES[num].value()) for num in sorted(SENSES))


def end_blocked(num, now):
    since = blocked_since[num]
    if since is None:
        return
    blocked_since[num] = None
    duration = time.ticks_diff(now, since)
    if duration < min_blocked_ms():
        g = glitches[num]
        g[0] += 1
        if duration > g[1]:
            g[1] = duration


def check_sensors():
    """Einmal alle Lichtschranken ansehen. Ist eine länger als
    ALARM_MIN_BLOCKED_MS unterbrochen, ohne dass gerade ausgegeben wird, greift
    jemand in den Automaten."""
    now = time.ticks_ms()
    gap = None if last_sample[0] is None else time.ticks_diff(now, last_sample[0])
    last_sample[0] = now
    if dispensing():
        for num in blocked_since:
            blocked_since[num] = None
        return
    if gap is not None and gap > MAX_SAMPLE_GAP_MS:
        # Schleife hat gestockt: was dazwischen war, ist unbekannt.
        for num in blocked_since:
            if blocked_since[num] is not None:
                blocked_since[num] = now
    for num in SENSES:
        if SENSES[num].value() != 1:
            end_blocked(num, now)
            continue
        if blocked_since[num] is None:
            blocked_since[num] = now
            continue
        duration = time.ticks_diff(now, blocked_since[num])
        if duration < min_blocked_ms():
            continue
        last = last_alarm.get(num)
        if last is not None and time.ticks_diff(now, last) < config.ALARM_REPEAT_MS:
            continue
        last_alarm[num] = now
        nr = num - 1  # Lichtschranke num gehört zu Fach nr (siehe dispense())
        details = {
            "sensor": num,
            "blocked_ms": duration,
            "sensors": sensor_values(),
            "glitches": {str(k): v[:] for k, v in glitches.items() if v[0]},
            "since_dispense_ms": time.ticks_diff(now, state["last_end"]),
            "uptime_s": time.ticks_diff(now, BOOT_TICKS) // 1000,
        }
        try:
            details["rssi"] = network.WLAN(network.STA_IF).status("rssi")
        except Exception:
            pass
        for v in glitches.values():
            v[0] = 0
            v[1] = 0
        last_trigger[0] = details
        log("Eingriff! Lichtschranke", num, "seit", duration, "ms unterbrochen, alle:", details["sensors"])
        report("/dispense/unexpected", nr, details)


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
        try:
            body_text = body.decode()
        except Exception:
            body_text = ""
        # Vom Automaten signiert? Nur dann zählt die Anfrage als seine.
        signed = "x-signature" in headers and \
            signing.verify(headers, body_text, config.HMAC_KEY_ID, config.HMAC_KEY)

        # Die Adresse des Automaten lernt die Ausgabe nur aus signierten Anfragen.
        if signed and state["server_ip"] != address[0]:
            state["server_ip"] = address[0]
            log("Automat unter", address[0])

        if path in ("/dispense", "/update") and not signed:
            log("Unsignierte Anfrage", path, "von", address[0], "abgelehnt")
            respond(client, 403, "signature required")
        elif path == "/ping":
            if signed:
                ota.confirm()
            respond(client, 200, "1")
        elif path == "/version":
            respond(client, 200, ota.version())
        elif path == "/update":
            # Nicht mitten in einer Ausgabe neu starten.
            if state["busy"] or queue:
                respond(client, 200, "busy")
            else:
                respond(client, 200, ota.start(config.SERVER_IP or address[0], config.SERVER_PORT, config.BOARD,
                                               config.HMAC_KEY_ID, config.HMAC_KEY))
        elif path == "/dispense" and method == "POST":
            try:
                nr = int(ujson.loads(body_text)["nr"])
            except Exception:
                respond(client, 400, "nr fehlt")
                return
            if not 0 <= nr <= 7:
                respond(client, 400, "ungueltiges Fach")
                return
            if ota.running():
                # Gleich startet der ESP neu - der Auftrag ginge verloren.
                respond(client, 200, "update")
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
                "time": signing.now(),
                # Zur Fehlersuche: Lichtschranken (1 = unterbrochen), kurze
                # Impulse seit der letzten Meldung, letzter gemeldeter Eingriff.
                "sensors": sensor_values(),
                "glitches": {str(k): v for k, v in glitches.items() if v[0]},
                "last_alarm": last_trigger[0],
                # Im Leerlauf A/B/C = 0 (Kanal 0), ALWAYS_ON = 1.
                "pins": {"A": A.value(), "B": B.value(), "C": C.value(), "ALWAYS_ON": ALWAYS_ON.value(),
                         "STEP": STEP.value(), "DIR": DIR.value()},
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
    signing.sync_ntp(wlan)
    _thread.start_new_thread(worker, ())
    _thread.start_new_thread(reporter, ())
    _thread.start_new_thread(serve, (config.HTTP_PORT,))
    # Lichtschranken überwachen und das WLAN im Blick behalten.
    last_wlan_check = time.ticks_ms()
    while True:
        # Während der Motor dreht, gibt es nichts zu überwachen (dispensing()) -
        # hier warten, ohne dem Motor Rechenzeit wegzunehmen.
        wait_motor_idle()
        if config.ALARM_ENABLED:
            try:
                check_sensors()
            except Exception as e:
                log("Überwachung der Lichtschranken:", e)
        if time.ticks_diff(time.ticks_ms(), last_wlan_check) > 5000:
            last_wlan_check = time.ticks_ms()
            # Sicherheitsnetz: im Leerlauf hat kein Motor Haltestrom. Nur mit
            # motor_lock, damit keine beginnende Ausgabe abgewählt wird.
            if motor_lock.acquire(0):
                try:
                    if not state["busy"]:
                        release_motors()
                finally:
                    motor_lock.release()
            if not wlan.isconnected():
                connect_wlan(wlan)
        time.sleep_ms(10)


if __name__ == "__main__":
    main()
