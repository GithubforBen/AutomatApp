# Läuft beim Start vor main.py. Das Programm selbst steht in main.py.
#
# Sicherheitsnetz für Updates über WLAN (siehe ota.py): Diese Datei wird nie
# über WLAN aktualisiert, nur per USB. Nach einem Update läuft die neue Version
# auf Probe. Meldet sich der Automat nicht innerhalb von TRIAL_MS mit einem
# /ping (dann bestätigt main.py die Version), startet der ESP neu; nach
# MAX_BOOTS Fehlversuchen kommen die alten Dateien zurück.

import os
import ujson
import machine

STATE = "ota_state.json"
OLD = "ota_old"
VERSION = "ota_version"
TRIAL_MS = 180000
MAX_BOOTS = 3


def _load():
    try:
        with open(STATE) as f:
            return ujson.load(f)
    except Exception:
        return None


def _save(state):
    with open(STATE, "w") as f:
        ujson.dump(state, f)


def _exists(path):
    try:
        os.stat(path)
        return True
    except OSError:
        return False


def _rollback(state):
    print("[boot] Update zurückgerollt auf Version", state.get("previous"))
    for path in state.get("files", []):
        old = OLD + "/" + path
        try:
            if _exists(path):
                os.remove(path)
            # Gab es die Datei vorher nicht, bleibt sie einfach weg.
            if _exists(old):
                os.rename(old, path)
        except OSError as e:
            print("[boot] Konnte", path, "nicht zurückholen:", e)
    with open(VERSION, "w") as f:
        f.write(state.get("previous", "none"))
    os.remove(STATE)


def _check_trial(timer):
    state = _load()
    if state and state.get("phase") == "trial":
        print("[boot] Neue Version nicht bestätigt - Neustart")
        machine.reset()


_state = _load()
if _state:
    phase = _state.get("phase")
    if phase == "swap":
        # Strom weg mitten im Austausch der Dateien.
        _rollback(_state)
    elif phase == "trial":
        _state["boots"] = _state.get("boots", 0) + 1
        if _state["boots"] > MAX_BOOTS:
            _rollback(_state)
        else:
            _save(_state)
            print("[boot] Version", _state.get("version"), "auf Probe, Start", _state["boots"], "von", MAX_BOOTS)
            _timer = machine.Timer(0)  # Referenz behalten
            _timer.init(mode=machine.Timer.ONE_SHOT, period=TRIAL_MS, callback=_check_trial)
