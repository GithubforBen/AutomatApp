#!/usr/bin/env python3
"""Interaktiver Karten-Scan-Simulator.

Ersetzt die echte Scanner-Hardware. Wie die echten Geräte gehen alle Scans
signiert direkt ans Java-Backend (Port 8000). Signiert wird mit dem
hmac_client der Website (liest deren .env), siehe SCANSIM_WEBSITE_SRC.

Drei Scan-Arten:
  * scanned  -> /scanned  Automat: Karte am Ausgabeautomaten
  * login    -> /login    Station: Kommen/Gehen (bucht Zeit)
  * assign   -> /scanned  füllt "Karte scannen" im Website-Popup
                          (identisch zu 'scanned', nur wartet die Website
                           dann gerade auf eine Karte)
"""

import json
import os
import sys

import requests

BACKEND = os.environ.get("SCANSIM_BACKEND", "http://127.0.0.1:8000")
WEBSITE_SRC = os.environ.get(
    "SCANSIM_WEBSITE_SRC", os.path.expanduser("~/WebstormProjects/automatWebsiteTwo/src"))
sys.path.insert(0, WEBSITE_SRC)
import hmac_client  # noqa: E402  (signiert wie die echten Geräte)

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
    url = BACKEND + path
    try:
        r = hmac_client.post(url, json_body={"rfid": rfid}, timeout=15)
    except requests.RequestException as e:
        print(f"  !! Backend nicht erreichbar ({e.__class__.__name__}): {url}")
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
    print("  Karten-Scan-Simulator   ->  " + BACKEND)
    print("=" * 62)
    try:
        requests.get(BACKEND + "/teacher/exists", timeout=5)
    except requests.RequestException:
        print("  !! Backend (Port 8000) läuft nicht - bitte zuerst starten.")
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
            post("/login", rfid)
        else:
            post("/scanned", rfid)


if __name__ == "__main__":
    sys.exit(main())
