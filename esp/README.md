# ESP32-Programme

| Ordner | Gerät | Adresse |
|---|---|---|
| `ausgabe/` | Ausgabe (Schrittmotor, Lichtschranken) | 192.168.188.202 |
| `scanner/` | Kartenscanner am Automaten (Summer für den Alarm) | 192.168.188.201 |
| `station/` | Station zum Kommen/Gehen (LCD) | 192.168.188.203 |
| `common/` | kommt auf jedes Gerät (`.env`-Leser, Updates, HMAC, `boot.py`) | |

Jedes Gerät bekommt `common/` **und** seinen eigenen Ordner, dazu eine `.env` mit WLAN und
HMAC-Schlüssel (Vorlage: `.env.example` im Ordner des Geräts, Schlüssel aus dem
HMAC-Bildschirm des Automaten).

## Signierte Anfragen

Automat und Geräte signieren ihre Anfragen gegenseitig mit dem HMAC-Schlüssel aus der `.env`
(`common/signing.py`, auf dem Automaten `esp-key-id` in der `config.yaml`):

* Die Ausgabe gibt nur auf signierte `/dispense` etwas aus, der Scanner schaltet den Alarm nur
  auf signierte `/alarm_on`/`/alarm_off`, alle drei aktualisieren sich nur auf signierte `/update`.
* Scanner und Station schicken Karten signiert direkt ans Backend (`/scanned` bzw. `/login`).
* Die Adresse des Automaten lernen die Geräte nur aus signierten Anfragen.
* Die Uhr stellen sie per NTP; klappt das nicht (z.B. im Schul-WLAN gesperrt), stellt sie die
  erste signierte Anfrage des Automaten - er pingt alle drei alle fünf Sekunden.

**Ohne Schlüssel in der `.env` geht nichts mehr:** Die Ausgabe gibt nichts aus, Scanner und
Station werden ihre Karten nicht los.

## Updates über WLAN

Die ESPs holen sich ihre Dateien selbst vom Automaten - aus dessen Projektordner, also immer
die Version, mit der der Automat läuft. Der Automat prüft 30 s nach dem Start (also auch nach
„Automat aktualisieren“) und danach alle 15 Minuten die Version jedes ESP und stößt bei
Abweichung ein Update an. Die Ausgabe wartet damit, bis sie nichts mehr ausgibt; solange der
Alarm läuft, wird nichts aktualisiert.

* Die Dateiliste ist mit dem HMAC-Schlüssel signiert, jede Datei per SHA-256 geprüft.
* Eine neue Version läuft auf Probe: Meldet sich der Automat nicht innerhalb von 3 Minuten mit
  einem Ping, startet der ESP neu; nach 3 Fehlversuchen kommen die alten Dateien zurück.
* **`boot.py` und `.env` werden nie über WLAN übertragen** - Änderungen daran nur per USB.
* Ohne HMAC-Schlüssel in der `.env` läuft das Gerät normal, nur ohne Updates über WLAN.

Protokoll: im Log des Automaten nach `ESP-Update` suchen, auf dem ESP nach `[ota]`/`[boot]`.

## Einmalig per USB aufspielen

Nötig beim ersten Mal (ältere Programme können sich noch nicht selbst aktualisieren) und wenn
sich `boot.py` oder die `.env` ändert. Mit [mpremote](https://docs.micropython.org/en/latest/reference/mpremote.html),
hier für die Ausgabe (für die anderen `ausgabe` ersetzen):

```bash
cd esp/common  && mpremote cp -r . :
cd ../ausgabe  && mpremote cp -r . : && mpremote reset
```

Dabei landen auch `.env` und `.env.example` auf dem Gerät - die `.env` muss dort liegen.
