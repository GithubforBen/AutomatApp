# Updates über WLAN: Der ESP holt seine Dateien vom Backend des Automaten
# (GET /esp/manifest, GET /esp/file). Das Backend liefert sie aus seinem
# Projektordner - also immer genau die Version, mit der der Automat läuft.
#
# Ablauf:
# * Der Automat fragt /version ab und schickt /update, wenn sie nicht zu seiner
#   passt (beim Start und dann regelmäßig).
# * Der ESP lädt die Dateiliste. Das Backend signiert sie mit dem HMAC-Schlüssel
#   samt einer Zufallszahl des ESP - so lässt sich weder fremder Code noch eine
#   alte Liste unterschieben. Jede Datei wird gegen ihre SHA-256 geprüft.
# * Geänderte Dateien landen erst in ota_new/, dann werden sie getauscht (alte
#   nach ota_old/) und der ESP startet neu. boot.py rollt zurück, wenn die neue
#   Version sich nicht bestätigt (siehe confirm()).
#
# Nicht übertragen werden boot.py (das Sicherheitsnetz) und .env (Zugangsdaten).

import os
import time
import socket
import ujson
import hashlib
import binascii
import _thread
import machine
import hmac

STATE = "ota_state.json"
VERSION = "ota_version"
NEW = "ota_new"
OLD = "ota_old"

# time.time() zählt auf dem ESP32 ab 2000, Unix-Zeit ab 1970.
EPOCH_OFFSET = 946684800 if time.gmtime(0)[0] == 2000 else 0

_status = {"running": False, "confirmed": False}


def log(*args):
    print("[ota]", *args)


# ---------------------------------------------------------------------------
# Für main.py
# ---------------------------------------------------------------------------

def running():
    return _status["running"]


def version():
    try:
        with open(VERSION) as f:
            return f.read().strip() or "none"
    except OSError:
        return "none"


def confirm():
    """Die laufende Version funktioniert (der Automat hat sie erreicht). Beim
    ersten /ping nach einem Update aufrufen - danach kostet es nichts mehr."""
    if _status["confirmed"]:
        return
    _status["confirmed"] = True
    try:
        with open(STATE) as f:
            state = ujson.load(f)
    except Exception:
        return
    if state.get("phase") == "trial":
        os.remove(STATE)
        _rmtree(OLD)
        log("Version", state.get("version"), "bestätigt")


def header(request, name):
    """Liest einen Kopf aus einer rohen HTTP-Anfrage (bytes)."""
    name = name.lower().encode()
    for line in request.split(b"\r\n")[1:]:
        if not line:
            break
        if b":" in line:
            key, value = line.split(b":", 1)
            if key.strip().lower() == name:
                return value.strip().decode()
    return None


def set_clock(unix_time):
    """Stellt die Uhr - die Anfragen ans Backend sind signiert und nur mit
    richtiger Uhrzeit gültig."""
    tm = time.gmtime(int(unix_time) - EPOCH_OFFSET)
    machine.RTC().datetime((tm[0], tm[1], tm[2], tm[6], tm[3], tm[4], tm[5], 0))


def start(host, port, board, key_id, key, unix_time=None):
    """Startet ein Update im Hintergrund. Gibt die Antwort für den Automaten zurück."""
    if not key_id or not key:
        return "kein HMAC-Schlüssel in .env"
    if _status["running"]:
        return "läuft schon"
    if unix_time:
        try:
            set_clock(unix_time)
        except Exception as e:
            log("Uhrzeit nicht übernommen:", e)
    _status["running"] = True
    _thread.start_new_thread(_run, (host, port, board, key_id, key))
    return "ok"


def _run(host, port, board, key_id, key):
    try:
        update(host, port, board, key_id, key)
    except Exception as e:
        log("Update abgebrochen:", e)
    finally:
        _status["running"] = False


# ---------------------------------------------------------------------------
# Update
# ---------------------------------------------------------------------------

def update(host, port, board, key_id, key):
    if isinstance(key, str):
        key = key.encode()
    client = _Client(host, port, key_id, key)

    status, headers, body, nonce = client.get("/esp/manifest?board=" + board)
    if status != 200:
        raise OSError("Dateiliste: HTTP {}".format(status))
    expected = _b64(hmac.new(key, (nonce + ":").encode() + body, hashlib.sha256).digest())
    if not hmac.compare_digest(expected, headers.get("x-manifest-signature", "")):
        raise OSError("Signatur der Dateiliste stimmt nicht")
    manifest = ujson.loads(body)
    if manifest.get("board") != board:
        raise OSError("Dateiliste ist für " + str(manifest.get("board")))
    target = manifest["version"]
    if target == version():
        log("Schon aktuell:", target)
        return

    changed = [f for f in manifest["files"] if _sha256_file(f["path"]) != f["sha256"]]
    if not changed:
        # Dateien stimmen schon (z.B. frisch per USB aufgespielt) - nur merken.
        with open(VERSION, "w") as fh:
            fh.write(target)
        log("Dateien schon aktuell, Version", target)
        return
    needed = sum(f["size"] for f in changed)
    stat = os.statvfs("/")
    free = stat[0] * stat[3]
    if needed * 2 + 8192 > free:
        raise OSError("Zu wenig Speicher: {} Bytes nötig, {} frei".format(needed * 2, free))

    log("Update", version(), "->", target, "-", len(changed), "Datei(en)")
    _rmtree(NEW)
    for f in changed:
        dest = NEW + "/" + f["path"]
        _makedirs(dest)
        digest = client.download("/esp/file?board={}&path={}".format(board, f["path"]), dest)
        if digest != f["sha256"]:
            raise OSError("Prüfsumme von {} stimmt nicht".format(f["path"]))

    # Austauschen. Bricht das ab (Strom weg), holt boot.py die alten Dateien zurück.
    paths = [f["path"] for f in changed]
    state = {"phase": "swap", "version": target, "previous": version(), "files": paths, "boots": 0}
    _rmtree(OLD)
    _write_json(STATE, state)
    for path in paths:
        if _exists(path):
            _makedirs(OLD + "/" + path)
            os.rename(path, OLD + "/" + path)
        _makedirs(path)
        os.rename(NEW + "/" + path, path)
    with open(VERSION, "w") as fh:
        fh.write(target)
    state["phase"] = "trial"
    _write_json(STATE, state)
    _rmtree(NEW)
    log("Update auf", target, "installiert - Neustart")
    time.sleep(1)
    machine.reset()


class _Client:
    def __init__(self, host, port, key_id, key):
        self.host = host
        self.port = port
        self.key_id = key_id
        self.key = key

    def _request(self, path):
        timestamp = int(time.time()) + EPOCH_OFFSET
        nonce = binascii.hexlify(os.urandom(16)).decode()
        # Wie HMACFilter im Backend: keyId:timestamp:nonce:body (GET -> leerer Body).
        data = "{}:{}:{}:".format(self.key_id, timestamp, nonce).encode()
        signature = _b64(hmac.new(self.key, data, hashlib.sha256).digest())
        sock = socket.socket()
        sock.settimeout(10)
        sock.connect(socket.getaddrinfo(self.host, self.port)[0][-1])
        head = ("GET {} HTTP/1.1\r\nHost: {}:{}\r\nX-API-KEY-ID: {}\r\nX-TIMESTAMP: {}\r\n"
                "X-NONCE: {}\r\nX-SIGNATURE: {}\r\nConnection: close\r\n\r\n").format(
            path, self.host, self.port, self.key_id, timestamp, nonce, signature)
        sock.sendall(head.encode())
        # Kopf lesen
        data = b""
        while b"\r\n\r\n" not in data:
            chunk = sock.recv(512)
            if not chunk:
                break
            data += chunk
            if len(data) > 8192:
                raise OSError("Antwortkopf zu groß")
        head, _, rest = data.partition(b"\r\n\r\n")
        lines = head.decode().split("\r\n")
        status = int(lines[0].split()[1])
        headers = {}
        for line in lines[1:]:
            if ":" in line:
                k, v = line.split(":", 1)
                headers[k.strip().lower()] = v.strip()
        return sock, status, headers, rest, nonce

    def _body_chunks(self, sock, headers, rest):
        length = int(headers.get("content-length", "-1"))
        received = 0
        if rest:
            received += len(rest)
            yield rest
        while length < 0 or received < length:
            chunk = sock.recv(1024)
            if not chunk:
                break
            received += len(chunk)
            yield chunk
        if 0 <= length != received:
            raise OSError("Antwort unvollständig ({} von {} Bytes)".format(received, length))

    def get(self, path):
        sock, status, headers, rest, nonce = self._request(path)
        try:
            body = b"".join(self._body_chunks(sock, headers, rest))
        finally:
            sock.close()
        return status, headers, body, nonce

    def download(self, path, dest):
        """Lädt in eine Datei und gibt deren SHA-256 (hex) zurück."""
        sock, status, headers, rest, nonce = self._request(path)
        try:
            if status != 200:
                raise OSError("{}: HTTP {}".format(path, status))
            h = hashlib.sha256()
            with open(dest, "wb") as fh:
                for chunk in self._body_chunks(sock, headers, rest):
                    h.update(chunk)
                    fh.write(chunk)
            return binascii.hexlify(h.digest()).decode()
        finally:
            sock.close()


# ---------------------------------------------------------------------------
# Dateien
# ---------------------------------------------------------------------------

def _b64(raw):
    return binascii.b2a_base64(raw).decode().strip()


def _exists(path):
    try:
        os.stat(path)
        return True
    except OSError:
        return False


def _is_dir(path):
    try:
        return os.stat(path)[0] & 0x4000 != 0
    except OSError:
        return False


def _makedirs(file_path):
    """Legt die Ordner für file_path an (nicht die Datei selbst)."""
    parts = file_path.split("/")[:-1]
    current = ""
    for part in parts:
        current = part if not current else current + "/" + part
        if not _exists(current):
            os.mkdir(current)


def _rmtree(path):
    if not _exists(path):
        return
    if _is_dir(path):
        for name in os.listdir(path):
            _rmtree(path + "/" + name)
        os.rmdir(path)
    else:
        os.remove(path)


def _sha256_file(path):
    try:
        h = hashlib.sha256()
        with open(path, "rb") as fh:
            while True:
                chunk = fh.read(1024)
                if not chunk:
                    break
                h.update(chunk)
        return binascii.hexlify(h.digest()).decode()
    except OSError:
        return None


def _write_json(path, value):
    with open(path, "w") as fh:
        ujson.dump(value, fh)
