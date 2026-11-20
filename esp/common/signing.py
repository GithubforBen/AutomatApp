# Signierte Anfragen zwischen Automat und ESP32 - in beide Richtungen.
#
# Wie die Website und das Backend (HMACFilter): HMAC-SHA256 über
# "keyId:zeitstempel:nonce:body" mit dem Schlüssel aus der .env, in den Köpfen
# X-API-KEY-ID, X-TIMESTAMP, X-NONCE und X-SIGNATURE.
#
# * headers(): signiert eine Anfrage an das Backend (Karten, Meldungen).
# * verify(): prüft eine Anfrage des Automaten. Ohne gültige Signatur werden
#   Ausgabe, Alarm und Updates abgelehnt - sonst könnte jeder im WLAN den
#   Automaten bedienen.
#
# Uhr: Signaturen sind nur mit richtiger Uhrzeit gültig. Klappt NTP nicht, stellt
# die erste gültig signierte Anfrage des Automaten die Uhr (früher tat das ein
# unsignierter Kopf, den jeder schicken konnte).

import os
import time
import binascii
import hashlib
import machine
import hmac

# time.time() zählt auf dem ESP32 ab 2000, Unix-Zeit ab 1970.
EPOCH_OFFSET = 946684800 if time.gmtime(0)[0] == 2000 else 0
# So alt darf eine Anfrage höchstens sein (Sekunden).
MAX_AGE = 120
# Die letzten Nonces - jede signierte Anfrage gilt nur einmal.
NONCE_MEMORY = 64

_state = {"synced": False}
_nonces = []


def log(*args):
    print("[signing]", *args)


def now():
    return int(time.time()) + EPOCH_OFFSET


def _b64(raw):
    return binascii.b2a_base64(raw).decode().strip()


def _sign(key, key_id, timestamp, nonce, body):
    if isinstance(key, str):
        key = key.encode()
    data = "{}:{}:{}:{}".format(key_id, timestamp, nonce, body).encode()
    return _b64(hmac.new(key, data, hashlib.sha256).digest())


def headers(key_id, key, body=""):
    """Köpfe für eine signierte Anfrage ans Backend."""
    timestamp = now()
    nonce = binascii.hexlify(os.urandom(16)).decode()
    return {
        "X-API-KEY-ID": key_id,
        "X-TIMESTAMP": str(timestamp),
        "X-NONCE": nonce,
        "X-SIGNATURE": _sign(key, key_id, timestamp, nonce, body),
    }


def parse_headers(raw):
    """Köpfe einer rohen HTTP-Anfrage (bytes) als dict mit kleinen Namen."""
    result = {}
    head = raw.split(b"\r\n\r\n", 1)[0]
    for line in head.split(b"\r\n")[1:]:
        if b":" in line:
            key, value = line.split(b":", 1)
            try:
                result[key.strip().lower().decode()] = value.strip().decode()
            except Exception:
                pass
    return result


def set_clock(unix_time):
    tm = time.gmtime(int(unix_time) - EPOCH_OFFSET)
    machine.RTC().datetime((tm[0], tm[1], tm[2], tm[6], tm[3], tm[4], tm[5], 0))


def mark_synced():
    """Die Uhr stimmt (z.B. per NTP gestellt)."""
    _state["synced"] = True


def is_synced():
    return _state["synced"]


def verify(request_headers, body, key_id, key):
    """Ist die Anfrage vom Automaten signiert? request_headers mit kleinen Namen,
    body als str (bei GET leer). Stellt bei Bedarf die Uhr."""
    if not key_id or not key:
        return False
    kid = request_headers.get("x-api-key-id")
    timestamp = request_headers.get("x-timestamp")
    nonce = request_headers.get("x-nonce")
    signature = request_headers.get("x-signature")
    if not (kid and timestamp and nonce and signature) or kid != key_id:
        return False
    try:
        ts = int(timestamp)
    except ValueError:
        return False
    if not hmac.compare_digest(_sign(key, kid, ts, nonce, body), signature):
        return False
    current = now()
    # Zu alt: abgelehnt. Neuere gehen immer - eine falsch gehende Uhr holt so
    # die nächste echte Anfrage wieder ein.
    if _state["synced"] and ts < current - MAX_AGE:
        log("Signierte Anfrage zu alt:", current - ts, "s")
        return False
    if nonce in _nonces:
        log("Anfrage wiederholt (gleiche Nonce) - abgelehnt")
        return False
    _nonces.append(nonce)
    if len(_nonces) > NONCE_MEMORY:
        _nonces.pop(0)
    if not _state["synced"] or abs(ts - current) > 2:
        try:
            set_clock(ts)
            _state["synced"] = True
        except Exception as e:
            log("Uhr nicht gestellt:", e)
    return True


def sync_ntp(wlan=None):
    """Uhr per NTP stellen (mehrere Server, auch das Gateway). Klappt es nicht,
    stellt die erste signierte Anfrage des Automaten sie."""
    import ntptime
    hosts = ["pool.ntp.org", "de.pool.ntp.org", "time.google.com"]
    if wlan is not None:
        try:
            hosts.append(wlan.ifconfig()[2])
        except Exception:
            pass
    for host in hosts:
        try:
            ntptime.host = host
            ntptime.settime()
            mark_synced()
            log("Uhrzeit per NTP von", host)
            return True
        except Exception as e:
            log("NTP über", host, "fehlgeschlagen:", e)
    log("NTP nicht erreichbar - die erste signierte Anfrage des Automaten stellt die Uhr")
    return False
