# Kartenscanner des B.A.M.B.I.-Automaten (MicroPython, ESP32 mit RC522-Leser)
#
# * Liest Karten und schickt sie signiert ans Backend (POST /scanned). Das
#   meldet die Karte am Automaten an oder nimmt sie für "Karte scannen" auf der
#   Website entgegen.
# * Beantwortet die Anfragen des Automaten: /ping, /alarm_on, /alarm_off, /status,
#   /version, /update. Alarm und Update nur mit Signatur des Automaten (signing.py).
# * Summer: ein kurzer Ton pro gelesener Karte, drei kurze, wenn sie nicht
#   ankam, Intervallton solange der Alarm an ist.
#
# Einstellungen stehen in config.py.

import time
import socket
import network
import _thread
import ujson
from machine import Pin, SoftSPI
import rfid
import config
import ota
import signing

# Vom Automaten gelernt (Absender des ersten /ping) bzw. zuletzt gelesen.
state = {"server_ip": None, "last_card": None}


def log(*args):
    print("[scanner]", *args)


# ---------------------------------------------------------------------------
# Summer
# ---------------------------------------------------------------------------

class Buzzer:
    def __init__(self, pins):
        self._pins = [Pin(p, Pin.OUT) for p in pins]
        self._lock = _thread.allocate_lock()
        self.alarm = False
        self._set(False)
        _thread.start_new_thread(self._alarm_loop, ())

    def _set(self, on):
        for pin in self._pins:
            pin.value(1 if on else 0)

    def beep(self, times=1, on_ms=100, off_ms=100):
        self._lock.acquire()
        try:
            for i in range(times):
                self._set(True)
                time.sleep_ms(on_ms)
                self._set(False)
                if i < times - 1:
                    time.sleep_ms(off_ms)
        finally:
            self._lock.release()

    def set_alarm(self, on):
        self.alarm = on
        if not on:
            self._set(False)
        log("Alarm", "an" if on else "aus")

    def _alarm_loop(self):
        while True:
            if not self.alarm:
                time.sleep_ms(100)
                continue
            self._lock.acquire()
            try:
                if self.alarm:
                    self._set(True)
                    time.sleep_ms(400)
                    self._set(False)
            finally:
                self._lock.release()
            time.sleep_ms(400)


# ---------------------------------------------------------------------------
# HTTP: Anfragen des Automaten beantworten, Karten an die Website schicken
# ---------------------------------------------------------------------------

def respond(client, status, body, content_type="text/plain; charset=utf-8"):
    reason = {200: "OK", 400: "Bad Request", 404: "Not Found"}.get(status, "OK")
    data = body.encode()
    head = "HTTP/1.1 {} {}\r\nContent-Type: {}\r\nContent-Length: {}\r\nConnection: close\r\n\r\n".format(
        status, reason, content_type, len(data))
    client.sendall(head.encode() + data)


class Server:
    def __init__(self, port, buzzer):
        self._port = port
        self._buzzer = buzzer

    def start(self):
        _thread.start_new_thread(self._run, ())

    def _run(self):
        # Ein Fehler darf den Server nie dauerhaft beenden - neu starten.
        while True:
            server = None
            try:
                server = socket.socket()
                server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
                server.bind(socket.getaddrinfo("0.0.0.0", self._port)[0][-1])
                server.listen(4)
                log("HTTP-Server auf Port", self._port)
                while True:
                    client, address = server.accept()
                    self._handle(client, address)
            except Exception as e:
                log("Server-Fehler, starte neu:", e)
                if server:
                    server.close()
                time.sleep(1)

    def _handle(self, client, address):
        try:
            client.settimeout(3)
            request = client.recv(1024)
            parts = request.split(b"\r\n", 1)[0].decode().split()
            if len(parts) < 2:
                respond(client, 400, "bad request")
                return
            path = parts[1].split("?")[0]

            # Vom Automaten signiert? Nur dann zählt die Anfrage als seine
            # (alle Anfragen des Automaten sind GET, also ohne Körper).
            headers = signing.parse_headers(request)
            signed = "x-signature" in headers and \
                signing.verify(headers, "", config.HMAC_KEY_ID, config.HMAC_KEY)

            # So lernt der Scanner, wohin er die Karten schicken muss.
            if signed and state["server_ip"] != address[0]:
                state["server_ip"] = address[0]
                log("Automat unter", address[0])

            if path in ("/alarm_on", "/alarm_off", "/update") and not signed:
                # Sonst könnte jeder im WLAN den Alarm abschalten.
                log("Unsignierte Anfrage", path, "von", address[0], "abgelehnt")
                respond(client, 403, "signature required")
            elif path == "/ping":
                if signed:
                    ota.confirm()
                respond(client, 200, "1")
            elif path == "/version":
                respond(client, 200, ota.version())
            elif path == "/update":
                respond(client, 200, ota.start(config.SERVER_IP or address[0], config.BACKEND_PORT, config.BOARD,
                                               config.HMAC_KEY_ID, config.HMAC_KEY))
            elif path == "/alarm_on":
                self._buzzer.set_alarm(True)
                respond(client, 200, "success")
            elif path == "/alarm_off":
                self._buzzer.set_alarm(False)
                respond(client, 200, "success")
            elif path == "/status":
                respond(client, 200, ujson.dumps({
                    "alarm": self._buzzer.alarm,
                    "server": config.SERVER_IP or state["server_ip"],
                    "lastCard": state["last_card"],
                }), "application/json")
            else:
                respond(client, 404, "not found")
        except Exception as e:
            log("Anfrage von", address[0], "fehlgeschlagen:", e)
        finally:
            client.close()


def http_post(host, port, path, body, timeout=5):
    """Schickt einen signierten POST und gibt den HTTP-Status zurück."""
    sock = socket.socket()
    sock.settimeout(timeout)
    try:
        sock.connect(socket.getaddrinfo(host, port)[0][-1])
        data = body.encode()
        head = "POST {} HTTP/1.1\r\nHost: {}:{}\r\nContent-Type: application/json\r\nContent-Length: {}\r\nConnection: close\r\n".format(
            path, host, port, len(data))
        for key, value in signing.headers(config.HMAC_KEY_ID, config.HMAC_KEY, body).items():
            head += "{}: {}\r\n".format(key, value)
        head += "\r\n"
        sock.sendall(head.encode() + data)
        return int(sock.recv(64).split()[1])
    finally:
        sock.close()


# ---------------------------------------------------------------------------
# WLAN und Karten
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


def valid_uid(uid):
    # Kartennummer: 4 Bytes plus Prüfbyte (XOR der vier). Halbe Lesungen fallen so raus.
    return uid and len(uid) == 5 and (uid[0] ^ uid[1] ^ uid[2] ^ uid[3]) == uid[4]


def send_card(card, buzzer):
    server = config.SERVER_IP or state["server_ip"]
    if not server:
        log("Noch kein /ping vom Automaten - weiß nicht, wohin mit der Karte")
        buzzer.beep(3, 60, 80)
        return
    try:
        # Signiert direkt ans Backend (früher unsigniert über die Website -
        # da konnte jeder im WLAN eine fremde Karte vortäuschen).
        status = http_post(server, config.BACKEND_PORT, "/scanned", ujson.dumps({"rfid": card}))
    except Exception as e:
        log("Automat", server, "nicht erreichbar:", e)
        status = None
    if status != 200:
        log("Karte nicht angekommen, Status", status)
        buzzer.beep(3, 60, 80)
    else:
        log("Karte übermittelt")


def scan_loop(wlan, buzzer):
    spi = SoftSPI(baudrate=100000, polarity=0, phase=0,
                  sck=Pin(config.RFID_SCK, Pin.OUT),
                  mosi=Pin(config.RFID_MOSI, Pin.OUT),
                  miso=Pin(config.RFID_MISO, Pin.OUT))
    sda = Pin(config.RFID_SDA, Pin.OUT)
    last_uid = None
    last_seen = 0

    while True:
        if not wlan.isconnected():
            connect_wlan(wlan)
        try:
            uid = rfid.read(spi, sda, True)
        except Exception as e:
            log("Lesefehler:", e)
            uid = None

        if valid_uid(uid):
            now = time.ticks_ms()
            card = list(uid)
            # Liegt die Karte einfach weiter auf dem Leser, zählt das nicht als neuer Scan.
            repeat = card == last_uid and time.ticks_diff(now, last_seen) < config.CARD_GONE_MS
            last_uid, last_seen = card, now
            if not repeat:
                log("Karte", card)
                state["last_card"] = card
                buzzer.beep()
                send_card(card, buzzer)
                # Das Senden kann Sekunden dauern (Timeout) - die Karte lag in
                # der Zeit weiter auf und gilt nicht als neu aufgelegt.
                last_seen = time.ticks_ms()
        time.sleep_ms(50)


def main():
    buzzer = Buzzer(config.BUZZER_PINS)
    wlan = network.WLAN(network.STA_IF)
    wlan.active(False)
    time.sleep_ms(100)
    wlan.active(True)
    connect_wlan(wlan)
    # Karten gehen signiert ans Backend - dafür muss die Uhr stimmen.
    signing.sync_ntp(wlan)
    Server(config.HTTP_PORT, buzzer).start()
    buzzer.beep(2, 60, 80)  # bereit
    scan_loop(wlan, buzzer)


if __name__ == "__main__":
    main()
