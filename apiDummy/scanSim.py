#!/usr/bin/env python3
"""Interaktiver Karten-Scan-Simulator.

Ersetzt die echte Scanner-Hardware. Alle Requests gehen an die Website
(Port 8080), genau wie beim echten ESP - die Website übernimmt das
HMAC-Signieren Richtung Java-Backend. Es wird also weder ein Login-Cookie
noch ein HMAC-Secret in diesem Skript gebraucht.

Drei Scan-Arten:
  * scanned  -> /scan          Automat: Karte am Ausgabeautomaten
  * login    -> /station_scan  Station: Kommen/Gehen (bucht Zeit)
  * assign   -> /scan          füllt "Karte scannen" im Website-Popup
                               (identisch zu 'scanned', nur ist dann in der
                                Website gerade der Scan-Modus aktiv)
"""

import json
import sys

import requests

WEBSITE = "http://127.0.0.1:8080"

# Bekannte Karten aus der Testdatenbank. "-" trennt nur optisch.
PRESETS = {
    "1": ("David Junke (Schüler)", [99, 253, 101, 0, 251]),
    "2": ("TestStudent 01bba404 (Schüler)", [200, 200, 200, 200]),
    "3": ("TestStudent 669928fe (Schüler)", [201, 201, 201, 201]),
    "4": ("Test TEzjk (Lehrkraft)", [0, 0, 0, 0, 0]),
    "5": ("Sammel-Karte Schüler", [251, 251, 251, 251]),
    "6": ("Sammel-Karte Lehrkräfte", [250, 250, 250, 250]),
    "9": ("Unbekannte Karte", [1, 2, 3, 4]),
}


def post(path: str, rfid: list):
    url = WEBSITE + path
    try:
        r = requests.post(url, data=json.dumps(rfid),
                          headers={"Content-Type": "application/json"}, timeout=15)
    except requests.RequestException as e:
        print(f"  !! Website nicht erreichbar ({e.__class__.__name__}): {url}")
        return
    print(f"  -> {path}  HTTP {r.status_code}")
    body = r.text.strip()
    if not body:
        return
    try:
        parsed = json.loads(body)
        print("  " + json.dumps(parsed, indent=2, ensure_ascii=False).replace("\n", "\n  "))
    except ValueError:
        print(f"  {body}")


def parse_rfid(raw: str):
    """Akzeptiert '99,253,101,0,251' genauso wie '99 253 101 0 251'."""
    parts = [p for p in raw.replace(",", " ").split() if p]
    try:
        return [int(p) for p in parts]
    except ValueError:
        return None


def choose_card(arg: str):
    if arg in PRESETS:
        name, rfid = PRESETS[arg]
        print(f"  Karte: {name} {rfid}")
        return rfid
    rfid = parse_rfid(arg)
    if rfid:
        print(f"  Karte: eigene {rfid}")
        return rfid
    print(f"  !! '{arg}' ist keine bekannte Karte und keine Byte-Liste.")
    return None


def menu():
    print()
    print("  Karten")
    for key, (name, rfid) in PRESETS.items():
        print(f"    {key}  {name:<34} {rfid}")
    print()
    print("  Befehle")
    print("    a <karte>   Automat  - Karte am Ausgabeautomaten scannen")
    print("    s <karte>   Station  - Kommen/Gehen buchen")
    print("    z <karte>   Zuweisen - für 'Karte scannen' im Website-Popup")
    print("    <karte>     wie 'a'")
    print("    m           dieses Menü")
    print("    q           beenden")
    print()
    print("  <karte> ist eine Ziffer von oben oder eigene Bytes, z.B. '99 253 101 0 251'")
    print()


def main():
    print("=" * 62)
    print("  Karten-Scan-Simulator   ->  " + WEBSITE)
    print("=" * 62)
    try:
        requests.get(WEBSITE + "/ping", timeout=5)
    except requests.RequestException:
        print("  !! Website (Port 8080) läuft nicht - bitte zuerst starten.")
    menu()

    while True:
        try:
            line = input("scan> ").strip()
        except (EOFError, KeyboardInterrupt):
            print()
            return

        if not line:
            continue
        if line in ("q", "quit", "exit"):
            return
        if line in ("m", "menu", "h", "help", "?"):
            menu()
            continue

        cmd, _, rest = line.partition(" ")
        if cmd in ("a", "s", "z"):
            arg = rest.strip()
            if not arg:
                print("  !! Bitte eine Karte angeben, z.B. 'a 1'")
                continue
        else:
            cmd, arg = "a", line

        rfid = choose_card(arg)
        if rfid is None:
            continue

        if cmd == "s":
            post("/station_scan", rfid)
        else:
            post("/scan", rfid)


if __name__ == "__main__":
    sys.exit(main())
