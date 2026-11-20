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

# Wohin gescannte Karten gehen: an die Website (Route /scan), die sie an den
# Automaten weiterreicht - oder, wenn auf der Website gerade "Karte scannen"
# läuft, dort übernimmt. Die Adresse des Rechners lernt der Scanner aus dem
# ersten /ping des Automaten; SERVER_IP setzt sie fest (z.B. "192.168.188.20").
SERVER_IP = None
WEBSITE_PORT = 8080

# Pins
BUZZER_PINS = (22, 23)
RFID_SCK = 4
RFID_MOSI = 0
RFID_MISO = 2
RFID_SDA = 16

# Dieselbe Karte gilt erst wieder als neuer Scan, wenn sie so lange weg war.
CARD_GONE_MS = 1500
