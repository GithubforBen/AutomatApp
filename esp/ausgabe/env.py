# Liest geheime Einstellungen (WLAN, HMAC) aus der Datei .env im Wurzelverzeichnis
# des ESP. Die .env wird nicht eingecheckt - Vorlage siehe .env.example.
#
# Format: eine Zuweisung pro Zeile, KEY=wert. Leerzeilen und Zeilen mit # werden
# ignoriert, Anführungszeichen um den Wert werden entfernt.

ENV_FILE = ".env"


def load(path=ENV_FILE):
    values = {}
    try:
        f = open(path)
    except OSError:
        raise OSError("{} fehlt - bitte aus .env.example anlegen und auf den ESP kopieren".format(path))
    with f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, value = line.split("=", 1)
            value = value.strip()
            if len(value) >= 2 and value[0] == value[-1] and value[0] in "\"'":
                value = value[1:-1]
            values[key.strip()] = value
    return values


def require(values, key):
    if key not in values or not values[key]:
        raise KeyError("{} fehlt in {}".format(key, ENV_FILE))
    return values[key]
