# Einstellungen der Ausgabe (ESP32 mit Schrittmotor und Lichtschranken).
# WLAN-Zugangsdaten und Schlüssel stehen in .env (nicht eingecheckt, siehe .env.example).

import env

_env = env.load()

# WLAN
WLAN_SSID = env.require(_env, "WLAN_SSID")
WLAN_PASSWORD = env.require(_env, "WLAN_PASSWORD")

# Feste Adresse der Ausgabe - muss zu "dispenser-url" in der config.yaml des
# Automaten passen. Netzmaske, Gateway und DNS kommen weiter per DHCP.
STATIC_IP = "192.168.188.202"

# Auf diesem Port fragt der Automat an (/ping, /dispense).
HTTP_PORT = 80

# Name im Ordner esp/ - darunter holt sich die Ausgabe ihre Updates (siehe ota.py).
BOARD = "ausgabe"

# Fehlgeschlagene Ausgaben meldet die Ausgabe direkt ans Backend des Automaten
# (POST /dispense/failed), signiert mit einem HMAC-Schlüssel aus dessen
# HMAC-Bildschirm. Die Adresse lernt sie aus dem ersten /ping des Automaten;
# SERVER_IP setzt sie fest.
SERVER_IP = None
SERVER_PORT = 8000
HMAC_KEY_ID = env.require(_env, "HMAC_KEY_ID")
HMAC_KEY = env.require(_env, "HMAC_KEY").encode()

# Alarm: Spricht eine Lichtschranke an, ohne dass gerade ausgegeben wird, meldet
# die Ausgabe einen Eingriff und das Backend schaltet den Alarm am Scanner ein.
ALARM_ENABLED = True
# Nach einer Ausgabe so lange nicht überwachen (fallende Süßigkeit, Nachlauf).
ALARM_GRACE_MS = 3000
# So lange muss eine Lichtschranke ohne Unterbrechung unterbrochen sein, bevor
# es als Eingriff zählt. Eine Hand braucht deutlich länger; kürzere Impulse
# (Störungen, eine nachrutschende Süßigkeit) zählen nur als "Zucker" und
# stehen in der nächsten Meldung bzw. in /status.
ALARM_MIN_BLOCKED_MS = 150
# Bleibt eine Lichtschranke unterbrochen, frühestens nach dieser Zeit erneut melden.
ALARM_REPEAT_MS = 10000
