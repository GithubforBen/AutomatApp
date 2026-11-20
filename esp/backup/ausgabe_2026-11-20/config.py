# Einstellungen der Ausgabe (ESP32 mit Schrittmotor und Lichtschranken).

# WLAN
WLAN_SSID = "MINTSeeheim"
WLAN_PASSWORD = "DEIN_PASSWORT"

# Feste Adresse der Ausgabe - muss zu "dispenser-url" in der config.yaml des
# Automaten passen. Netzmaske, Gateway und DNS kommen weiter per DHCP.
STATIC_IP = "192.168.188.202"

# Auf diesem Port fragt der Automat an (/ping, /dispense).
HTTP_PORT = 80

# Fehlgeschlagene Ausgaben meldet die Ausgabe direkt ans Backend des Automaten
# (POST /dispense/failed), signiert mit einem HMAC-Schlüssel aus dessen
# HMAC-Bildschirm. Die Adresse lernt sie aus dem ersten /ping des Automaten;
# SERVER_IP setzt sie fest.
SERVER_IP = None
SERVER_PORT = 8000
HMAC_KEY_ID = "ausgabe"
HMAC_KEY = b"BASE64_SCHLUESSEL_AUS_DER_APP"