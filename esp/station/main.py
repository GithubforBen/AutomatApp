# Station des B.A.M.B.I.-Automaten (MicroPython, ESP32 mit RC522-Leser und 20x4-LCD)
#
# * Liest Karten und bucht Kommen/Gehen über die Website (POST /station_scan,
#   JSON-Liste der Kartenbytes). Die Antwort des Automaten ("Herzlich
#   Willkommen, ...") steht danach einige Sekunden auf dem Display.
# * Beantwortet die Anfragen des Automaten: /ping, /status, /version, /update.
# * Unten rechts zeigt das Display die WLAN-Stärke (0-4 Balken); während eines
#   Updates über WLAN steht dort "Update im Gange" mit Prozentzahl.
# * Summer: ein Ton sofort beim Lesen der Karte, drei, wenn die Buchung
#   nicht geklappt hat.
#
# Einstellungen stehen in config.py.

import time
import socket
import network
import _thread
import ujson
from machine import Pin, SoftSPI, SoftI2C
import rfid
import config
import ota

# Vom Automaten gelernt (Absender des ersten /ping) bzw. zuletzt gelesen.
state = {"server_ip": None, "last_card": None, "screen": None}

IDLE_LINES = ["", "    MINT-Zentrum", "   Karte vorhalten", ""]

# WLAN-Stärke als eigenes Zeichen (5 Pixel breit, 8 hoch): 0 bis 4 Balken.
# Belegt die Zeichen 0-4 im CGRAM des Displays.
SIGNAL_GLYPHS = [
    [0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x15],  # keine Verbindung: nur Punkte
    [0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, 0x10],
    [0x00, 0x00, 0x00, 0x00, 0x04, 0x04, 0x14, 0x14],
    [0x00, 0x00, 0x01, 0x01, 0x05, 0x05, 0x15, 0x15],
    [0x01, 0x01, 0x05, 0x05, 0x15, 0x15, 0x15, 0x15],
]
SIGNAL_EVERY_MS = 5000


def signal_level(wlan):
    """WLAN-Stärke als 0-4 Balken."""
    if not wlan.isconnected():
        return 0
    try:
        rssi = wlan.status("rssi")
    except Exception:
        return 0
    for level, limit in ((4, -55), (3, -65), (2, -75), (1, -85)):
        if rssi >= limit:
            return level
    return 0


def log(*args):
    print("[station]", *args)


# ---------------------------------------------------------------------------
# Display und Summer
# ---------------------------------------------------------------------------

# Zeichensatz A00 der üblichen HD44780-Module: Umlaute liegen nicht dort, wo
# Unicode sie hat - ohne Umrechnung stand bei "Jürgen" Unsinn auf dem Display.
A00 = {"ä": "\xe1", "ö": "\xef", "ü": "\xf5", "ß": "\xe2", "Ä": "\xe1", "Ö": "\xef", "Ü": "\xf5", "°": "\xdf"}


def lcd_text(text):
    text = str(text)
    out = ""
    for ch in text:
        if config.LCD_ROM == "A00" and ch in A00:
            out += A00[ch]
        elif ord(ch) < 128 or (config.LCD_ROM == "A02" and ord(ch) < 256):
            out += ch
        else:
            out += "?"
    return out


class Screen:
    def __init__(self):
        self._lock = _thread.allocate_lock()
        self._lcd = None
        self._lines = [None, None, None, None]  # was gerade auf dem Display steht
        self._wanted = ["", "", "", ""]  # was zuletzt angezeigt werden sollte
        self._signal = 0
        # Fortschritt eines Updates: setzt der Update-Thread, zeichnet die Hauptschleife.
        self._progress = None
        self._progress_shown = None
        self.shown_until = 0
        try:
            from pcf8574 import PCF8574
            from hd44780 import HD44780
            from lcd import LCD
            i2c = SoftI2C(sda=Pin(config.LCD_SDA, Pin.OUT), scl=Pin(config.LCD_SCL, Pin.OUT), freq=config.LCD_I2C_FREQ)
            pcf8574 = PCF8574(i2c)
            self._lcd = LCD(HD44780(pcf8574, num_lines=4, num_columns=20), pcf8574)
            self._lcd.clear()
            self._lcd.backlight_on()
            self._define_glyphs()
        except Exception as e:
            # Ohne Display soll die Station trotzdem buchen.
            log("Display nicht ansprechbar:", e)

    def _define_glyphs(self):
        hd = self._lcd.get_hd44780()
        for slot, rows in enumerate(SIGNAL_GLYPHS):
            hd._write_command(hd.LCD_SET_CGRAM_ADDR | (slot << 3))
            for row in rows:
                hd._write_data(row)

    def show(self, lines, hold_ms=0, quiet=False):
        """Zeigt bis zu vier Zeilen; mit hold_ms kehrt danach idle() zurück."""
        lines = (list(lines) + ["", "", "", ""])[:4]
        if not quiet:
            log("Display:", " | ".join(str(line) for line in lines))
        self.shown_until = time.ticks_add(time.ticks_ms(), hold_ms) if hold_ms else 0
        self._wanted = lines
        self._render()

    def _render(self):
        if not self._lcd:
            return
        self._lock.acquire()
        try:
            for i in range(4):
                text = lcd_text(self._wanted[i])[:20]
                if i == 3:
                    # Letzte Spalte unten rechts: WLAN-Stärke.
                    text = text[:19] + " " * (19 - len(text[:19])) + chr(self._signal)
                # Nur geänderte Zeilen schreiben - jedes Zeichen kostet Zeit auf dem Bus.
                if text != self._lines[i]:
                    self._lcd.write_line(text, i)
                    self._lines[i] = text
        except Exception as e:
            log("Display-Fehler:", e)
        finally:
            self._lock.release()

    def set_signal(self, level):
        if level != self._signal:
            self._signal = level
            self._render()

    def update_progress(self, percent, phase):
        """Rückruf aus ota.py. Läuft im Update-Thread - der hat auf dem ESP32 nur
        wenig Stack, für den Weg bis zum Display (I2C) reichte der nicht. Darum hier
        nur merken; draw_progress() in der Hauptschleife zeichnet."""
        self._progress = (percent, phase)

    def draw_progress(self):
        """Zeigt den zuletzt gemeldeten Update-Fortschritt, falls er sich geändert hat."""
        progress = self._progress
        if progress is None or progress == self._progress_shown:
            return
        self._progress_shown = progress
        percent, phase = progress
        if phase == "failed":
            self._progress = None
            self._progress_shown = None
            self.show(["    Update ist", "  fehlgeschlagen", "", "Alte Version bleibt"], 5000)
            return
        if phase == "download":
            filled = percent * 18 // 100
            self.show(["  Update im Gange", "", "[" + "#" * filled + " " * (18 - filled) + "]",
                       "      {:3d} %".format(percent)], quiet=True)
        elif phase == "install":
            self.show(["  Update im Gange", "", "  Installiere ...", "      100 %"])
        elif phase == "restart":
            self.show(["  Update im Gange", "", "  Neustart ...", ""])

    def idle(self):
        self.show(IDLE_LINES)

    def expired(self):
        return self.shown_until and time.ticks_diff(time.ticks_ms(), self.shown_until) >= 0


class Buzzer:
    def __init__(self, pins):
        self._pins = [Pin(p, Pin.OUT) for p in pins]
        self._set(False)

    def _set(self, on):
        for pin in self._pins:
            pin.value(1 if on else 0)

    def beep_now(self):
        """Kurzer Ton im Hintergrund - das Buchen läuft währenddessen schon los."""
        _thread.start_new_thread(self.beep, (1, 80))

    def beep(self, times=1, on_ms=100, off_ms=100):
        for i in range(times):
            self._set(True)
            time.sleep_ms(on_ms)
            self._set(False)
            if i < times - 1:
                time.sleep_ms(off_ms)


# ---------------------------------------------------------------------------
# HTTP: Anfragen des Automaten beantworten, Karten an die Website schicken
# ---------------------------------------------------------------------------

def respond(client, status, body, content_type="text/plain; charset=utf-8"):
    reason = {200: "OK", 404: "Not Found"}.get(status, "OK")
    data = body.encode()
    head = "HTTP/1.1 {} {}\r\nContent-Type: {}\r\nContent-Length: {}\r\nConnection: close\r\n\r\n".format(
        status, reason, content_type, len(data))
    client.sendall(head.encode() + data)


def handle(client, address):
    try:
        client.settimeout(3)
        request = client.recv(1024)
        parts = request.split(b"\r\n", 1)[0].decode().split()
        path = parts[1].split("?")[0] if len(parts) > 1 else ""
        # /status und /version nicht: die fragt auch mal ein anderer Rechner ab.
        if path in ("/ping", "/update") and state["server_ip"] != address[0]:
            # Nur der Automat fragt hier an - so lernt die Station, wohin sie buchen muss.
            state["server_ip"] = address[0]
            log("Automat unter", address[0])
        if path == "/ping":
            ota.confirm()
            respond(client, 200, "1")
        elif path == "/version":
            respond(client, 200, ota.version())
        elif path == "/update":
            respond(client, 200, ota.start(config.SERVER_IP or address[0], config.BACKEND_PORT, config.BOARD,
                                           config.HMAC_KEY_ID, config.HMAC_KEY,
                                           ota.header(request, "X-Unix-Time"),
                                           state["screen"].update_progress if state["screen"] else None))
        elif path == "/status":
            respond(client, 200, ujson.dumps({
                "server": config.SERVER_IP or state["server_ip"],
                "lastCard": state["last_card"],
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


def http_post(host, port, path, body, timeout=8):
    """Schickt einen POST und gibt (Status, Antworttext) zurück."""
    sock = socket.socket()
    sock.settimeout(timeout)
    try:
        sock.connect(socket.getaddrinfo(host, port)[0][-1])
        data = body.encode()
        head = "POST {} HTTP/1.1\r\nHost: {}:{}\r\nContent-Type: application/json\r\nContent-Length: {}\r\nConnection: close\r\n\r\n".format(
            path, host, port, len(data))
        sock.sendall(head.encode() + data)
        response = b""
        while True:
            chunk = sock.recv(512)
            if not chunk:
                break
            response += chunk
        end = response.find(b"\r\n\r\n")
        status = int(response.split(b" ", 2)[1])
        return status, response[end + 4:].decode() if end >= 0 else ""
    finally:
        sock.close()


# ---------------------------------------------------------------------------
# Buchen
# ---------------------------------------------------------------------------

def book(card, screen, buzzer):
    """Bucht Kommen/Gehen und zeigt das Ergebnis. Gibt die Anzeige zurück."""
    server = config.SERVER_IP or state["server_ip"]
    if not server:
        log("Noch kein /ping vom Automaten - weiß nicht, wohin buchen")
        buzzer.beep(3, 60, 80)
        return ["Keine Verbindung", "zum Automaten", "", "Bitte spaeter"]
    screen.show(["", "   Einen Moment ...", "", ""])
    try:
        status, text = http_post(server, config.WEBSITE_PORT, "/station_scan", ujson.dumps(card))
    except Exception as e:
        log("Website", server, "nicht erreichbar:", e)
        buzzer.beep(3, 60, 80)
        return ["Keine Verbindung", "zum Automaten", "", "Bitte spaeter"]
    try:
        return ujson.loads(text)["text"]
    except Exception:
        # Die Website antwortet "login failed", wenn die Karte niemandem gehört.
        log("Keine Buchung, Antwort:", status, text[:60])
        buzzer.beep(3, 60, 80)
        if "login failed" in text:
            return ["Karte unbekannt", "", "Bitte bei einer", "Lehrkraft melden"]
        return ["Buchung fehlgeschlagen", "", "Bitte nochmal", "versuchen"]


def valid_uid(uid):
    # Kartennummer: 4 Bytes plus Prüfbyte (XOR der vier). Halbe Lesungen fallen so raus.
    return uid and len(uid) == 5 and (uid[0] ^ uid[1] ^ uid[2] ^ uid[3]) == uid[4]


def scan_loop(wlan, screen, buzzer):
    spi = SoftSPI(baudrate=100000, polarity=0, phase=0,
                  sck=Pin(config.RFID_SCK, Pin.OUT),
                  mosi=Pin(config.RFID_MOSI, Pin.OUT),
                  miso=Pin(config.RFID_MISO, Pin.OUT))
    sda = Pin(config.RFID_SDA, Pin.OUT)
    last_uid = None
    last_seen = 0
    booked_uid = None
    booked_at = 0
    booked_lines = None
    signal_checked = time.ticks_ms()
    screen.set_signal(signal_level(wlan))

    while True:
        if not wlan.isconnected():
            screen.set_signal(0)
            screen.show(["Keine Verbindung", "zum WLAN", "", ""])
            connect_wlan(wlan)
            screen.idle()
        if time.ticks_diff(time.ticks_ms(), signal_checked) >= SIGNAL_EVERY_MS:
            signal_checked = time.ticks_ms()
            screen.set_signal(signal_level(wlan))
        screen.draw_progress()
        if screen.expired():
            screen.idle()
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
                # Sofort piepen: so ist klar, dass die Karte gelesen wurde -
                # noch bevor der Automat geantwortet hat.
                buzzer.beep_now()
                log("Karte", card)
                state["last_card"] = card
                if card == booked_uid and time.ticks_diff(now, booked_at) < config.SAME_CARD_PAUSE_MS:
                    # Gerade erst gebucht: nicht gleich wieder ausbuchen.
                    log("Gerade erst gebucht - ignoriert")
                    screen.show(booked_lines, config.SHOW_MS)
                else:
                    lines = book(card, screen, buzzer)
                    screen.show(lines, config.SHOW_MS)
                    booked_uid, booked_at, booked_lines = card, time.ticks_ms(), lines
                # Das Buchen kann dauern - die Karte lag in der Zeit weiter auf.
                last_seen = time.ticks_ms()
        time.sleep_ms(50)


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
    screen = Screen()
    state["screen"] = screen
    screen.show(["", "    MINT-Zentrum", "   Starte ...", ""])
    buzzer = Buzzer(config.BUZZER_PINS)
    wlan = network.WLAN(network.STA_IF)
    wlan.active(False)
    time.sleep_ms(100)
    wlan.active(True)
    connect_wlan(wlan)
    _thread.start_new_thread(serve, (config.HTTP_PORT,))
    screen.idle()
    buzzer.beep(2, 60, 80)  # bereit
    scan_loop(wlan, screen, buzzer)


if __name__ == "__main__":
    main()
