# Einstellungen der Station (ESP32 mit RC522-Leser und 20x4-LCD über I2C).
# WLAN-Zugangsdaten und Schlüssel stehen in .env (nicht eingecheckt, siehe .env.example).

import env

_env = env.load()

# WLAN
WLAN_SSID = env.require(_env, "WLAN_SSID")
WLAN_PASSWORD = env.require(_env, "WLAN_PASSWORD")

# Feste Adresse der Station - muss zu "station-url" in der config.yaml des
# Automaten passen. Netzmaske, Gateway und DNS kommen weiter per DHCP.
STATIC_IP = "192.168.188.203"

# Auf diesem Port fragt der Automat an (/ping, /status).
HTTP_PORT = 80

# Updates über WLAN (siehe ota.py): Name im Ordner esp/, Port des Backends und
# der HMAC-Schlüssel aus dem HMAC-Bildschirm des Automaten (in .env). Ohne
# Schlüssel läuft die Station normal, nur ohne Updates über WLAN.
BOARD = "station"
BACKEND_PORT = 8000
HMAC_KEY_ID = env.get(_env, "HMAC_KEY_ID")
HMAC_KEY = env.get(_env, "HMAC_KEY")

# Wohin gescannte Karten gehen: an die Website (Route /station_scan), die das
# Kommen/Gehen beim Automaten bucht und dessen Antwort zurückgibt. Die Adresse
# des Rechners lernt die Station aus dem ersten /ping des Automaten;
# SERVER_IP setzt sie fest (z.B. "192.168.188.20").
SERVER_IP = None
WEBSITE_PORT = 8080

# Pins
LCD_SDA = 19
LCD_SCL = 18
# Takt des Display-Busses. 100 kHz ist die Spezifikation des PCF8574; bei
# Anzeigefehlern (lange Kabel) wieder auf 40000 stellen.
LCD_I2C_FREQ = 100000
BUZZER_PINS = (22, 23)
RFID_SCK = 14
RFID_MOSI = 27
RFID_MISO = 26
RFID_SDA = 12

# Zeichensatz des Displays: "A00" (die üblichen Module, ä/ö/ü an eigenen
# Stellen) oder "A02" (europäisch, wie Latin-1).
LCD_ROM = "A00"

# So lange bleibt die Antwort auf dem Display stehen.
SHOW_MS = 5000
# Dieselbe Karte gilt erst wieder als neuer Scan, wenn sie so lange weg war.
CARD_GONE_MS = 1500
# Wer gerade gebucht hat, wird so lange nicht gleich wieder ausgebucht
# (zweimal kurz hintereinander vorgehalten).
SAME_CARD_PAUSE_MS = 10000
