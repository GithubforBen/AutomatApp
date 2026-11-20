# Einstellungen des Kartenscanners (ESP32 mit RC522-Leser).
# WLAN-Zugangsdaten und Schlüssel stehen in .env (nicht eingecheckt, siehe .env.example).

import env

_env = env.load()

# WLAN
WLAN_SSID = env.require(_env, "WLAN_SSID")
WLAN_PASSWORD = env.require(_env, "WLAN_PASSWORD")

# Feste Adresse des Scanners - muss zu "scanner-url" in der config.yaml des
# Automaten passen. Netzmaske, Gateway und DNS kommen weiter per DHCP.
STATIC_IP = "192.168.188.201"

# Auf diesem Port fragt der Automat den Scanner an (/ping, /alarm_on, /alarm_off).
HTTP_PORT = 80

# Name im Ordner esp/ (Updates über WLAN, siehe ota.py), Port des Backends und
# der HMAC-Schlüssel aus dem HMAC-Bildschirm des Automaten (in .env). Mit dem
# Schlüssel signiert der Scanner ihre Karten und erkennt Anfragen des Automaten -
# ohne ihn nimmt das Backend keine Karten an.
BOARD = "scanner"
BACKEND_PORT = 8000
HMAC_KEY_ID = env.get(_env, "HMAC_KEY_ID")
HMAC_KEY = env.get(_env, "HMAC_KEY")

# Wohin gescannte Karten gehen: ans Backend des Automaten (Route /scanned).
# Die Adresse lernt der Scanner aus der ersten signierten Anfrage des
# Automaten; SERVER_IP setzt sie fest (z.B. "192.168.188.20").
SERVER_IP = None

# Pins
BUZZER_PINS = (22, 23)
RFID_SCK = 4
RFID_MOSI = 0
RFID_MISO = 2
RFID_SDA = 16

# Dieselbe Karte gilt erst wieder als neuer Scan, wenn sie so lange weg war.
CARD_GONE_MS = 1500
