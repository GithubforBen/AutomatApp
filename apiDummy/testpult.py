#!/usr/bin/env python3
"""Testpult - Weboberfläche für die lokale Testumgebung.

Ein Prozess, zwei Aufgaben:

  * Er spielt die drei Geräte, die der Automat anspricht (Ausgabe 8081,
    Scanner 8082, Station 8083), und merkt sich alles, was die Java-App an
    sie schickt. Damit ersetzt er die drei ``dummyExternal.py``-Instanzen.
  * Er liefert unter http://localhost:8070 eine Oberfläche, mit der sich
    Karten am Automaten oder an der Station scannen lassen, ein Gerät
    abschalten oder eine Ausgabe klemmen lässt.

Scans laufen wie beim echten ESP über die Website (Port 8080). Nur die
Kartenliste und die Fehlermeldung einer klemmenden Ausgabe gehen direkt an
das Backend; dafür wird der HMAC-Client der Website mitbenutzt.

Umgebungsvariablen (alle optional):
  TESTPULT_PORT         Port der Oberfläche          (8070)
  TESTPULT_WEBSITE      Website                      (http://127.0.0.1:8080)
  TESTPULT_BACKEND      Java-Backend                 (http://127.0.0.1:8000)
  TESTPULT_WEBSITE_SRC  Ordner mit hmac_client.py    (~/WebstormProjects/automatWebsiteTwo/src)
"""

import collections
import json
import logging
import os
import random
import sys
import threading
import time

import requests
from flask import Flask, jsonify, request, send_file
from werkzeug.serving import make_server

UI_PORT = int(os.environ.get("TESTPULT_PORT", "8070"))
WEBSITE = os.environ.get("TESTPULT_WEBSITE", "http://127.0.0.1:8080")
BACKEND = os.environ.get("TESTPULT_BACKEND", "http://127.0.0.1:8000")
WEBSITE_SRC = os.environ.get(
    "TESTPULT_WEBSITE_SRC",
    os.path.expanduser("~/WebstormProjects/automatWebsiteTwo/src"))

HERE = os.path.dirname(os.path.abspath(__file__))

DEVICES = {
    "ausgabe": {"label": "Ausgabe", "port": 8081},
    "scanner": {"label": "Scanner", "port": 8082},
    "station": {"label": "Station", "port": 8083},
}

# Der HMAC-Client liest das Secret aus der .env der Website. Fehlt er, laufen
# Scans trotzdem - nur Kartenliste und "Ausgabe klemmt" fallen weg.
try:
    sys.path.insert(0, WEBSITE_SRC)
    import hmac_client  # noqa: E402
    HMAC_ERROR = None
except Exception as e:  # pragma: no cover - hängt von der lokalen Umgebung ab
    hmac_client = None
    HMAC_ERROR = f"{type(e).__name__}: {e}"


# --------------------------------------------------------------------------
# Gemeinsamer Zustand
# --------------------------------------------------------------------------

lock = threading.Lock()
events = collections.deque(maxlen=400)
event_counter = 0
device_state = {name: {"online": True, "lastPing": None} for name in DEVICES}
dispense_mode = {"value": "ok"}  # "ok" | "jam"


def log_event(source: str, text: str, level: str = "info", detail=None):
    global event_counter
    with lock:
        event_counter += 1
        events.append({
            "id": event_counter,
            "t": time.time(),
            "source": source,
            "text": text,
            "level": level,
            "detail": detail,
        })


def fmt_card(rfid) -> str:
    return " ".join(str(b) for b in rfid) if rfid else "(leer)"


# --------------------------------------------------------------------------
# Geräte-Dummies (8081-8083)
# --------------------------------------------------------------------------

def make_device_app(name: str) -> Flask:
    app = Flask(f"device-{name}")
    label = DEVICES[name]["label"]

    @app.before_request
    def offline_guard():
        # Ein 503 lässt HttpURLConnection genauso scheitern wie ein totes
        # Gerät - die Java-App springt dann auf den Fehlerbildschirm.
        if not device_state[name]["online"]:
            return "offline (Testpult)", 503
        return None

    @app.route("/ping", methods=["GET", "POST"])
    def ping():
        device_state[name]["lastPing"] = time.time()
        return "1"

    @app.route("/re-enable", methods=["GET", "POST"])
    def re_enable():
        body = request.get_json(silent=True) or {}
        log_event(name, f"Fach reaktiviert: {body.get('name', '?')}", detail=body)
        return "Added:" + str(body.get("name", ""))

    @app.route("/fill", methods=["POST"])
    def fill():
        body = request.get_json(silent=True) or {}
        log_event(name, f"Aufgefüllt: {body.get('name', '?')} auf {body.get('nr', '?')}", detail=body)
        return "Added:" + str(body.get("name", "")) + ":" + str(body.get("nr", ""))

    @app.route("/mint")
    def mint():
        return jsonify({"da": 100, "weg": 150})

    @app.route("/sweets")
    def sweets():
        return jsonify({
            "0": {"name": "Mentos", "hours": 2},
            "1": {"name": "Duplo", "hours": 2},
            "2": {"name": "Kinder", "hours": 2},
            "3": {"name": "Maoam", "hours": 2},
            "4": {"name": "Smarties", "hours": 2},
            "5": {"name": "Haribo", "hours": 2},
            "6": {"name": "Brause", "hours": 2},
            "7": {"name": "Stats", "hours": -1},
        })

    @app.route("/dispense", methods=["POST"])
    def dispense():
        body = request.get_json(silent=True) or {}
        nr = body.get("nr")
        if nr is None:
            log_event(name, "Ausgabe ohne Fachnummer angefordert", "bad", body)
            return "error: 235"
        text = f"Fach {nr} ausgeben, {body.get('cost', '?')} h, Karte {fmt_card(body.get('usr'))}"
        if dispense_mode["value"] == "jam":
            log_event(name, text + " - klemmt", "warn", body)
            # Wie boot.py: sofort quittieren, den Fehlschlag danach melden.
            threading.Thread(target=report_dispense_failed, args=(nr,), daemon=True).start()
        else:
            log_event(name, text, "ok", body)
        return "success"

    @app.route("/alarm_on", methods=["GET", "POST"])
    def alarm_on():
        log_event(name, "Alarm an", "warn")
        return "success"

    @app.route("/alarm_off", methods=["GET", "POST"])
    def alarm_off():
        log_event(name, "Alarm aus")
        return "success"

    return app


def report_dispense_failed(nr: int):
    time.sleep(1.0)
    if hmac_client is None:
        log_event("ausgabe", "Fehlschlag nicht gemeldet: HMAC-Client fehlt", "bad")
        return
    try:
        r = hmac_client.post(BACKEND + "/dispense/failed", json_body={"nr": nr}, timeout=5)
        if r.status_code == 200:
            log_event("ausgabe", f"Fehlschlag von Fach {nr} ans Backend gemeldet, Fach ist jetzt gesperrt", "warn")
        else:
            log_event("ausgabe", f"Backend lehnt Fehlermeldung ab: HTTP {r.status_code}", "bad", r.text[:200])
    except requests.RequestException as e:
        log_event("ausgabe", f"Backend nicht erreichbar: {type(e).__name__}", "bad")


# --------------------------------------------------------------------------
# Oberfläche (8090)
# --------------------------------------------------------------------------

ui = Flask("testpult")


@ui.route("/")
def index():
    return send_file(os.path.join(HERE, "testpult.html"))


def reachable(url: str) -> bool:
    # Jede HTTP-Antwort zählt - auch 401/403 heißt: der Dienst läuft.
    try:
        requests.get(url, timeout=1.5)
        return True
    except requests.RequestException:
        return False


@ui.route("/api/state")
def state():
    since = int(request.args.get("since", 0))
    with lock:
        new_events = [e for e in events if e["id"] > since]
        devices = {
            name: {**DEVICES[name], **device_state[name]} for name in DEVICES
        }
    return jsonify({
        "now": time.time(),
        "events": new_events,
        "devices": devices,
        "dispenseMode": dispense_mode["value"],
        "hmac": HMAC_ERROR is None,
    })


@ui.route("/api/services")
def services():
    return jsonify({
        "backend": reachable(BACKEND + "/ping"),
        "website": reachable(WEBSITE + "/ping"),
    })


@ui.route("/api/cards")
def cards():
    if hmac_client is None:
        return jsonify({"error": "HMAC-Client nicht geladen: " + HMAC_ERROR}), 503
    try:
        students = json.loads(hmac_client.get(BACKEND + "/student/all", timeout=5).text)["students"]
        teachers = json.loads(hmac_client.get(BACKEND + "/teacher/allTeachers", timeout=5).text)["teachers"]
    except Exception as e:
        return jsonify({"error": f"Backend nicht erreichbar ({type(e).__name__})"}), 503

    # Mehrere Personen können sich eine Karte teilen - deshalb nach Karte gruppieren.
    by_card = collections.OrderedDict()
    without_card = []
    for kind, people in (("Schüler*in", students), ("Lehrkraft", teachers)):
        for p in people:
            person = {
                "name": f"{p.get('firstName', '')} {p.get('lastName', '')}".strip(),
                "type": kind,
                "admin": p.get("level") == "ADMIN",
                "courses": [k.get("name") for k in p.get("kurse", [])],
            }
            rfid = p.get("rfid") or []
            if not rfid:
                without_card.append(person)
                continue
            by_card.setdefault(tuple(rfid), []).append(person)

    return jsonify({
        "cards": [{"rfid": list(k), "holders": v} for k, v in by_card.items()],
        "withoutCard": without_card,
    })


@ui.route("/api/scan", methods=["POST"])
def scan():
    body = request.get_json(silent=True) or {}
    where = body.get("where")
    rfid = body.get("rfid")
    if where not in ("automat", "station") or not isinstance(rfid, list) \
            or not rfid or not all(isinstance(b, int) and 0 <= b <= 255 for b in rfid):
        return jsonify({"error": "Ungültige Anfrage"}), 400

    path = "/scan" if where == "automat" else "/station_scan"
    source = "automat" if where == "automat" else "station"
    try:
        r = requests.post(WEBSITE + path, data=json.dumps(rfid),
                          headers={"Content-Type": "application/json"}, timeout=15)
    except requests.RequestException as e:
        log_event(source, f"Karte {fmt_card(rfid)}: Website nicht erreichbar", "bad")
        return jsonify({"ok": False, "error": f"Website nicht erreichbar ({type(e).__name__})"})

    text = r.text.strip()
    try:
        parsed = json.loads(text)
    except ValueError:
        parsed = None

    if where == "station":
        if isinstance(parsed, dict) and "name" in parsed:
            verb = "kommt" if parsed.get("cameIn") else "geht"
            log_event(source, f"{parsed['name']} {verb} (Karte {fmt_card(rfid)})", "ok", parsed)
        else:
            log_event(source, f"Karte {fmt_card(rfid)} abgelehnt: {text[:80]}", "bad")
    else:
        log_event(source, f"Karte {fmt_card(rfid)} gescannt")

    return jsonify({"ok": r.status_code == 200, "status": r.status_code,
                    "body": text, "parsed": parsed})


@ui.route("/api/device", methods=["POST"])
def set_device():
    body = request.get_json(silent=True) or {}
    name = body.get("name")
    if name not in DEVICES:
        return jsonify({"error": "Unbekanntes Gerät"}), 400
    online = bool(body.get("online"))
    device_state[name]["online"] = online
    log_event(name, "wieder eingeschaltet" if online else "ausgeschaltet", "info" if online else "warn")
    return jsonify({"ok": True})


@ui.route("/api/dispense_mode", methods=["POST"])
def set_dispense_mode():
    mode = (request.get_json(silent=True) or {}).get("mode")
    if mode not in ("ok", "jam"):
        return jsonify({"error": "Unbekannter Modus"}), 400
    dispense_mode["value"] = mode
    log_event("ausgabe", "Ausgabe klemmt ab jetzt" if mode == "jam" else "Ausgabe funktioniert wieder",
              "warn" if mode == "jam" else "info")
    return jsonify({"ok": True})


@ui.route("/api/random_card")
def random_card():
    """Eine Karte, die garantiert noch niemandem gehört."""
    taken = set()
    if hmac_client is not None:
        try:
            for path, key in (("/student/all", "students"), ("/teacher/allTeachers", "teachers")):
                for p in json.loads(hmac_client.get(BACKEND + path, timeout=5).text)[key]:
                    taken.add(tuple(p.get("rfid") or []))
        except Exception:
            pass
    while True:
        card = [random.randint(0, 255) for _ in range(4)]
        if tuple(card) not in taken:
            return jsonify({"rfid": card})


# --------------------------------------------------------------------------
# Start
# --------------------------------------------------------------------------

def serve(app: Flask, port: int):
    server = make_server("0.0.0.0", port, app, threaded=True)
    threading.Thread(target=server.serve_forever, daemon=True).start()


def main():
    logging.getLogger("werkzeug").setLevel(logging.WARNING)
    busy = []
    for name, dev in DEVICES.items():
        try:
            serve(make_device_app(name), dev["port"])
        except OSError:
            busy.append(f"{dev['label']} ({dev['port']})")
    if busy:
        print("Port belegt: " + ", ".join(busy) + " - läuft noch dummyExternal.py?")
        sys.exit(1)
    try:
        serve(ui, UI_PORT)
    except OSError:
        print(f"Port {UI_PORT} belegt - läuft das Testpult schon?")
        sys.exit(1)

    if HMAC_ERROR:
        print("Hinweis: HMAC-Client nicht geladen (" + HMAC_ERROR + ") - keine Kartenliste.")
    print(f"Testpult läuft: http://localhost:{UI_PORT}")
    print("Geräte: Ausgabe 8081, Scanner 8082, Station 8083")
    log_event("testpult", "gestartet")
    try:
        while True:
            time.sleep(3600)
    except KeyboardInterrupt:
        pass


if __name__ == "__main__":
    main()
