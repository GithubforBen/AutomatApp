Read the docs.mc!

## Installation auf dem Automaten

Installiert Automat und Website (Raspberry Pi OS, Debian, Ubuntu) und richtet den Autostart ein.
Als normaler Benutzer ausführen - am Gerät oder per SSH; am Bildschirm des Geräts muss dabei
jemand angemeldet sein, damit der Automat starten kann:

```bash
curl -fsSLO https://raw.githubusercontent.com/GithubforBen/AutomatApp/master/install.sh
bash install.sh
```

Passwörter fragt das Skript über Dialogfenster ab. Beim ersten Start legt man am Automaten das
Datenbank-Passwort fest; danach empfängt das Skript den Website-Schlüssel direkt vom Automaten.
Den ersten Admin-Zugang der Website legt man im Skript oder beim ersten Öffnen der Website an.

### Aktualisieren

Auf dem Desktop liegt der Knopf **„Automat aktualisieren“**. Er holt die neueste Version von
Automat und Website, baut neu und startet beide neu (danach am Automaten die PIN eingeben).
Datenbank, `config.yaml` und der Website-Schlüssel bleiben unverändert.

Wer noch mit der ersten Version des Skripts installiert hat, holt Knopf und Update-Funktion einmalig so:

```bash
git -C ~/automat/AutomatApp pull --ff-only && bash ~/automat/AutomatApp/install.sh update
```

| Befehl | Zweck |
|---|---|
| `bash ~/automat/AutomatApp/install.sh update` | aktualisieren (wie der Knopf) |
| `bash ~/automat/AutomatApp/install.sh` | alles neu einrichten (Pakete, Dienst, Autostart) |
| `bash ~/automat/AutomatApp/install.sh hmac` | Verbindung Website ↔ Automat neu einrichten |
| `bash ~/automat/AutomatApp/install.sh admin` | Admin-Zugang der Website festlegen |
| `bash ~/automat/AutomatApp/install.sh uninstall` | Autostart, Dienst und Knopf entfernen (Daten bleiben) |

Protokolle liegen in `~/automat/logs`, die Website läuft als systemd-Dienst `automat-website`.

## API Dummy Simulation

For development and testing purposes, a dummy external API is provided in `apiDummy/dummyExternal.py`. This script
simulates all four external components: Website, Dispenser, Scanner, and Station.

### Running the Dummies

To run all four simulation instances on ports 8080, 8081, 8082, and 8083:

```bash
python3 apiDummy/dummyExternal.py
```

To run a single instance on a specific port (e.g., 8085):

```bash
python3 apiDummy/dummyExternal.py 8085
```

The Java application expects the following default URLs for its components (can be configured in `config.yaml`):

- Website: `http://127.0.0.1:8080`
- Dispenser: `http://192.168.188.200`
- Scanner: `http://192.168.188.201`
- Station: `http://192.168.188.203` (from `src/main/resources/config.yaml`)
