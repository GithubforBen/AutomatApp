#!/usr/bin/env bash
# B.A.M.B.I. - Installation von Automat (JavaFX-App mit Backend) und Website.
#
# Für Raspberry Pi OS, Debian und Ubuntu. Richtet alles für den Autostart ein:
#   * Website als systemd-Dienst "automat-website" (startet beim Hochfahren)
#   * Automat als Autostart-Eintrag der Desktop-Sitzung (braucht einen Bildschirm)
#
# Aufruf (als normaler Benutzer, nicht als root):
#   bash install.sh              installieren bzw. aktualisieren
#   bash install.sh hmac         Verbindung Website <-> Automat (neu) einrichten
#   bash install.sh admin        Admin-Zugang der Website festlegen
#   bash install.sh uninstall    Autostart und Dienst entfernen (Daten bleiben)
#
# Passwörter werden über Dialogfenster abgefragt (zenity, sonst whiptail) und
# nirgends gespeichert - außer dem Website-Schlüssel, der in automatWebsite/.env
# landen muss (nur für den Benutzer lesbar). Das Datenbank-Passwort legt man am
# Automaten selbst fest; es verlässt das Gerät nie.
#
# Umgebungsvariablen:
#   AUTOMAT_DIR          Installationsordner (Standard: ~/automat)
#   AUTOMAT_UI           zenity | whiptail | text  (Standard: automatisch)
#   AUTOMAT_SKIP_SYSTEM  1 = keine Pakete, kein systemd, kein sudo (zum Testen)

set -Eeuo pipefail

INSTALL_DIR="${AUTOMAT_DIR:-$HOME/automat}"
APP_REPO="https://github.com/GithubforBen/AutomatApp.git"
APP_BRANCH="master"
WEB_REPO="https://github.com/2Bor3d/automatWebsite.git"
WEB_BRANCH="main"

APP_DIR="$INSTALL_DIR/AutomatApp"
WEB_DIR="$INSTALL_DIR/automatWebsite"
BIN_DIR="$INSTALL_DIR/bin"
LOG_DIR="$INSTALL_DIR/logs"
LOG="$LOG_DIR/install.log"
LAUNCHER="$BIN_DIR/automat-start.sh"
SERVICE="automat-website"
SERVICE_FILE="/etc/systemd/system/$SERVICE.service"
AUTOSTART_FILE="$HOME/.config/autostart/automat.desktop"
MENU_FILE="$HOME/.local/share/applications/automat.desktop"
MAIN_CLASS="de.schnorrenbergers.automat.BetterMain"

BACKEND_URL="http://127.0.0.1:8000"
WEBSITE_PORT=8080
# Der HMAC-Bildschirm des Automaten schickt den Schlüssel per "nc <IP> 12345".
HMAC_PORT=12345
# Diesen Zugang mit bekanntem Passwort haben ältere Stände automatisch angelegt.
DEFAULT_ADMIN_MAIL="test@gmail.com"
PLACEHOLDER="noch-nicht-eingerichtet"

SKIP_SYSTEM="${AUTOMAT_SKIP_SYSTEM:-0}"
TITLE="B.A.M.B.I. Installation"
SUDO_PW=""
JDK_HOME=""
UI=""
NEW_PASSWORD=""
ADMIN_MISSING=0

trap 'SUDO_PW=""; NEW_PASSWORD=""' EXIT
trap 'on_error $LINENO' ERR

# ---------------------------------------------------------------------------
# Dialoge: zenity (grafisch), whiptail (Terminal) oder einfache Textabfrage
# ---------------------------------------------------------------------------

has_display() { [[ -n "${DISPLAY:-}${WAYLAND_DISPLAY:-}" ]]; }

# /dev/tty existiert auch ohne Terminal, lässt sich dann aber nicht öffnen.
has_tty() { (: </dev/tty) 2>/dev/null; }

choose_ui() {
    case "${AUTOMAT_UI:-}" in
        zenity | whiptail | text) UI="$AUTOMAT_UI"; return ;;
    esac
    if has_display && command -v zenity >/dev/null; then
        UI=zenity
    elif command -v whiptail >/dev/null && has_tty; then
        UI=whiptail
    else
        UI=text
    fi
}

# Liest eine Zeile vom Terminal - auch wenn das Skript per "curl | bash" läuft.
tty_read() {
    local silent=$1 answer
    if has_tty; then
        if [[ $silent == 1 ]]; then IFS= read -rs answer </dev/tty; echo >/dev/tty; else IFS= read -r answer </dev/tty; fi
    else
        if [[ $silent == 1 ]]; then IFS= read -rs answer; else IFS= read -r answer; fi
    fi
    printf '%s' "$answer"
}

ui_info() {  # Titel Text
    case $UI in
        zenity) zenity --info --no-markup --width 460 --title "$1" --text "$2" 2>/dev/null || true ;;
        whiptail) whiptail --title "$1" --msgbox "$2" 20 74 </dev/tty >/dev/tty || true ;;
        *) printf '\n== %s ==\n%s\n\n[Enter] ' "$1" "$2" >&2; tty_read 0 >/dev/null ;;
    esac
}

ui_error() {  # Titel Text
    case $UI in
        zenity) zenity --error --no-markup --width 520 --title "$1" --text "$2" 2>/dev/null || true ;;
        whiptail) whiptail --title "$1" --msgbox "$2" 22 76 </dev/tty >/dev/tty || true ;;
        *) printf '\n!! %s !!\n%s\n\n[Enter] ' "$1" "$2" >&2; tty_read 0 >/dev/null ;;
    esac
}

ui_confirm() {  # Titel Text -> 0 = ja
    case $UI in
        zenity) zenity --question --no-markup --width 460 --title "$1" --text "$2" --ok-label Ja --cancel-label Nein 2>/dev/null ;;
        whiptail) whiptail --title "$1" --yes-button Ja --no-button Nein --yesno "$2" 20 74 </dev/tty >/dev/tty ;;
        *)
            printf '\n== %s ==\n%s\n[j/N] ' "$1" "$2" >&2
            [[ $(tty_read 0) =~ ^[jJyY] ]]
            ;;
    esac
}

ui_input() {  # Titel Text Vorgabe -> Antwort auf stdout, 1 = abgebrochen
    case $UI in
        zenity) zenity --entry --width 460 --ok-label Weiter --cancel-label Abbrechen --title "$1" --text "$2" --entry-text "${3:-}" 2>/dev/null ;;
        whiptail) whiptail --title "$1" --ok-button Weiter --cancel-button Abbrechen --inputbox "$2" 14 74 "${3:-}" 3>&1 1>/dev/tty 2>&3 </dev/tty ;;
        *)
            if [[ -n ${3:-} ]]; then printf '\n== %s ==\n%s [%s] ' "$1" "$2" "$3" >&2; else printf '\n== %s ==\n%s ' "$1" "$2" >&2; fi
            local answer; answer=$(tty_read 0)
            printf '%s' "${answer:-${3:-}}"
            ;;
    esac
}

ui_password() {  # Titel Text -> Passwort auf stdout, 1 = abgebrochen
    case $UI in
        zenity) zenity --entry --hide-text --width 460 --ok-label Weiter --cancel-label Abbrechen --title "$1" --text "$2" 2>/dev/null ;;
        whiptail) whiptail --title "$1" --ok-button Weiter --cancel-button Abbrechen --passwordbox "$2" 14 74 3>&1 1>/dev/tty 2>&3 </dev/tty ;;
        *) printf '\n== %s ==\n%s ' "$1" "$2" >&2; tty_read 1 ;;
    esac
}

ui_choice() {  # Titel Text Kennung1 Beschriftung1 ... -> Kennung auf stdout
    local title=$1 text=$2; shift 2
    case $UI in
        zenity)
            local picked
            while true; do
                picked=$(zenity --list --width 560 --height 320 --ok-label Weiter --cancel-label Abbrechen \
                    --title "$title" --text "$text" \
                    --column Kennung --column Auswahl --hide-column 1 --print-column 1 "$@" 2>/dev/null) || return 1
                # "Weiter" ohne Auswahl liefert eine leere Antwort - dann nochmal fragen.
                [[ -n $picked ]] && { printf '%s' "$picked"; return 0; }
            done
            ;;
        whiptail) whiptail --title "$title" --ok-button Weiter --cancel-button Abbrechen --menu "$text" 22 76 $(($# / 2)) "$@" 3>&1 1>/dev/tty 2>&3 </dev/tty ;;
        *)
            printf '\n== %s ==\n%s\n' "$title" "$text" >&2
            local i=1 tags=()
            while (($#)); do printf '  %d) %s\n' "$i" "$2" >&2; tags+=("$1"); shift 2; i=$((i + 1)); done
            printf 'Auswahl: ' >&2
            local n; n=$(tty_read 0)
            [[ $n =~ ^[0-9]+$ ]] && ((n >= 1 && n <= ${#tags[@]})) || return 1
            printf '%s' "${tags[$((n - 1))]}"
            ;;
    esac
}

# ---------------------------------------------------------------------------
# Ausgabe, Fehler, Hilfsfunktionen
# ---------------------------------------------------------------------------

step() { printf '\n\033[1;32m==>\033[0m %s\n' "$*"; printf '\n==> %s\n' "$*" >>"$LOG"; }
note() { printf '    %s\n' "$*"; printf '    %s\n' "$*" >>"$LOG"; }

# Führt einen Befehl aus und schreibt seine Ausgabe nur ins Protokoll.
run() { "$@" >>"$LOG" 2>&1; }

fail() {
    trap - ERR
    local details=""
    [[ -f $LOG ]] && details=$(tail -n 15 "$LOG")
    ui_error "$TITLE - Fehler" "$1"$'\n\n'"Letzte Zeilen aus $LOG:"$'\n'"$details"
    printf '\n\033[1;31mFehler:\033[0m %s\nProtokoll: %s\n' "$1" "$LOG" >&2
    exit 1
}

on_error() { fail "Unerwarteter Fehler in Zeile $1."; }

cancelled() {
    trap - ERR
    printf '\nAbgebrochen.\n' >&2
    exit 1
}

# ---------------------------------------------------------------------------
# Administratorrechte: Passwort einmal per Dialog, danach sudo aus dem Cache
# ---------------------------------------------------------------------------

need_root() {
    [[ $SKIP_SYSTEM == 1 ]] && return
    sudo -n true 2>/dev/null && return
    local tries
    for tries in 1 2 3; do
        SUDO_PW=$(ui_password "Administratorrechte" \
            "Für Pakete und den Website-Dienst braucht die Installation Administratorrechte."$'\n\n'"Passwort von $USER:") || cancelled
        if printf '%s\n' "$SUDO_PW" | sudo -S -p '' -v 2>/dev/null; then
            return
        fi
        ui_error "Administratorrechte" "Das Passwort stimmt nicht (Versuch $tries von 3)."
    done
    fail "Keine Administratorrechte."
}

as_root() {
    sudo -n true 2>/dev/null || printf '%s\n' "$SUDO_PW" | sudo -S -p '' -v 2>/dev/null
    sudo -n "$@"
}

# ---------------------------------------------------------------------------
# Java 21
# ---------------------------------------------------------------------------

java_major() {  # Pfad zu java -> Hauptversion
    "$1" -version 2>&1 | awk -F'"' '/version/ { split($2, v, "."); print v[1]; exit }'
}

find_jdk() {
    local candidates=("$INSTALL_DIR/jdk")
    [[ -n ${JAVA_HOME:-} ]] && candidates+=("$JAVA_HOME")
    if command -v javac >/dev/null; then
        candidates+=("$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")")
    fi
    candidates+=(/usr/lib/jvm/java-21-openjdk-* /usr/lib/jvm/*21*)
    local home
    for home in "${candidates[@]}"; do
        [[ -x $home/bin/javac && -x $home/bin/java ]] || continue
        local version
        version=$(java_major "$home/bin/java")
        if [[ $version =~ ^[0-9]+$ ]] && ((version >= 21)); then
            JDK_HOME="$home"
            return 0
        fi
    done
    return 1
}

# Falls die Paketquellen kein Java 21 haben (z.B. Raspberry Pi OS Bookworm):
# Eclipse Temurin in den Installationsordner laden.
install_temurin() {
    local arch
    case $(uname -m) in
        x86_64) arch=x64 ;;
        aarch64 | arm64) arch=aarch64 ;;
        armv7l) arch=arm ;;
        *) fail "Für die Architektur $(uname -m) gibt es kein Java 21 zum Herunterladen." ;;
    esac
    note "Lade Java 21 (Eclipse Temurin, $arch) herunter"
    local tmp="$INSTALL_DIR/jdk.tar.gz"
    run curl -fL --retry 3 -o "$tmp" \
        "https://api.adoptium.net/v3/binary/latest/21/ga/linux/$arch/jdk/hotspot/normal/eclipse" ||
        fail "Java 21 konnte nicht heruntergeladen werden."
    rm -rf "$INSTALL_DIR/jdk"
    mkdir -p "$INSTALL_DIR/jdk"
    run tar -xzf "$tmp" --strip-components=1 -C "$INSTALL_DIR/jdk" || fail "Java 21 konnte nicht entpackt werden."
    rm -f "$tmp"
}

# ---------------------------------------------------------------------------
# Installationsschritte
# ---------------------------------------------------------------------------

install_packages() {
    step "Pakete installieren"
    if [[ $SKIP_SYSTEM == 1 ]]; then note "übersprungen (AUTOMAT_SKIP_SYSTEM=1)"; return; fi
    command -v apt-get >/dev/null || fail "Dieses Skript braucht ein Debian-artiges System (apt-get)."

    local pkgs=(git curl python3 python3-venv whiptail)
    command -v nc >/dev/null || pkgs+=(netcat-openbsd)
    has_display && pkgs+=(zenity)
    run as_root apt-get update || fail "Paketlisten konnten nicht geladen werden (Internet?)."
    if ! find_jdk && apt-cache show openjdk-21-jdk >/dev/null 2>&1; then
        pkgs+=(openjdk-21-jdk)
    fi
    note "${pkgs[*]}"
    run as_root env DEBIAN_FRONTEND=noninteractive apt-get install -y "${pkgs[@]}" ||
        fail "Pakete konnten nicht installiert werden."
    choose_ui
}

ensure_java() {
    step "Java 21 prüfen"
    if ! find_jdk; then
        install_temurin
        find_jdk || fail "Java 21 wurde nicht gefunden."
    fi
    note "$JDK_HOME"
}

sync_repo() {  # URL Zweig Ordner
    local url=$1 branch=$2 dir=$3
    if [[ -d $dir/.git ]]; then
        if [[ -n $(git -C "$dir" status --porcelain --untracked-files=no) ]]; then
            note "$(basename "$dir"): lokale Änderungen vorhanden - nicht aktualisiert"
            return
        fi
        run git -C "$dir" fetch origin "$branch" || fail "$(basename "$dir") konnte nicht aktualisiert werden."
        run git -C "$dir" checkout -q "$branch"
        run git -C "$dir" merge --ff-only "origin/$branch" ||
            fail "$(basename "$dir") lässt sich nicht automatisch aktualisieren (abweichende Commits)."
        note "$(basename "$dir"): $(git -C "$dir" log --oneline -1)"
    else
        run git clone --branch "$branch" "$url" "$dir" || fail "$url konnte nicht geladen werden."
        note "$(basename "$dir"): neu geladen"
    fi
}

download_sources() {
    step "Quellcode laden"
    sync_repo "$APP_REPO" "$APP_BRANCH" "$APP_DIR"
    sync_repo "$WEB_REPO" "$WEB_BRANCH" "$WEB_DIR"
}

automat_running() { pgrep -f "$MAIN_CLASS" >/dev/null; }

stop_automat() {
    pkill -f "$LAUNCHER" 2>/dev/null || true
    pkill -f "$MAIN_CLASS" 2>/dev/null || true
    local i
    for i in $(seq 20); do automat_running || return 0; sleep 1; done
    pkill -9 -f "$MAIN_CLASS" 2>/dev/null || true
}

build_automat() {
    step "Automat bauen (dauert beim ersten Mal einige Minuten)"
    if automat_running; then
        ui_confirm "$TITLE" "Der Automat läuft gerade. Für das Update muss er kurz beendet werden."$'\n\n'"Jetzt beenden?" ||
            cancelled
        stop_automat
    fi
    chmod +x "$APP_DIR/mvnw"
    # Kein Fat-Jar: Klassen + Bibliotheken nebeneinander starten zuverlässiger
    # (Spring und JavaFX vertragen das Zusammenpacken schlecht).
    (cd "$APP_DIR" && run env JAVA_HOME="$JDK_HOME" ./mvnw -B -DskipTests clean compile \
        dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/lib) ||
        fail "Der Automat konnte nicht gebaut werden."
    note "$(find "$APP_DIR/target/lib" -name '*.jar' | wc -l) Bibliotheken"
}

setup_website() {
    step "Website einrichten"
    [[ -x $WEB_DIR/.venv/bin/python ]] || run python3 -m venv "$WEB_DIR/.venv" ||
        fail "Python-Umgebung konnte nicht angelegt werden (Paket python3-venv?)."
    run "$WEB_DIR/.venv/bin/pip" install --upgrade pip || true
    # requests und bcrypt fehlen in älteren Ständen der requirements.txt.
    run "$WEB_DIR/.venv/bin/pip" install -r "$WEB_DIR/requirements.txt" requests bcrypt ||
        fail "Python-Pakete der Website konnten nicht installiert werden."
    note "Python-Umgebung: $WEB_DIR/.venv"
    # Ohne .env startet die Website gar nicht - dann erreicht der Automat sie
    # nicht, bleibt bei "Verbindung wiederherstellen" stehen, und man kommt nie
    # zum HMAC-Bildschirm, der den echten Schlüssel erzeugt. Mit Platzhalter
    # läuft sie schon (nur Anmelden geht noch nicht).
    [[ -f $WEB_DIR/.env ]] || write_env "$PLACEHOLDER" "$PLACEHOLDER"
}

write_launcher() {
    step "Autostart einrichten"
    mkdir -p "$BIN_DIR" "$(dirname "$AUTOSTART_FILE")" "$(dirname "$MENU_FILE")"
    cat >"$LAUNCHER" <<EOF
#!/usr/bin/env bash
# Von install.sh erzeugt. Startet den Automaten und nach einem Absturz neu.
cd "$APP_DIR" || exit 1
mkdir -p "$LOG_DIR"
exec 9>"$LOG_DIR/automat.lock"
flock -n 9 || exit 0   # läuft schon
log="$LOG_DIR/automat.log"
while true; do
    [[ -f \$log && \$(stat -c %s "\$log") -gt 10485760 ]] && mv "\$log" "\$log.1"
    echo "\$(date '+%F %T') Automat startet" >>"\$log"
    "$JDK_HOME/bin/java" -cp "target/classes:target/lib/*" $MAIN_CLASS >>"\$log" 2>&1 && break
    echo "\$(date '+%F %T') Automat beendet mit Code \$? - Neustart in 5 s" >>"\$log"
    sleep 5
done
EOF
    chmod +x "$LAUNCHER"

    local entry
    entry=$(
        cat <<EOF
[Desktop Entry]
Type=Application
Name=Automat
Comment=B.A.M.B.I. Süßigkeitenautomat
Exec="$LAUNCHER"
Icon=$APP_DIR/src/main/resources/image/Logo.png
Terminal=false
Categories=Utility;
X-GNOME-Autostart-enabled=true
EOF
    )
    if [[ $SKIP_SYSTEM == 1 ]]; then
        note "Autostart-Eintrag übersprungen (AUTOMAT_SKIP_SYSTEM=1), Startskript: $LAUNCHER"
        return
    fi
    printf '%s\n' "$entry" >"$AUTOSTART_FILE"
    printf '%s\n' "$entry" >"$MENU_FILE"
    note "Automat: $AUTOSTART_FILE"
}

write_service() {
    if [[ $SKIP_SYSTEM == 1 ]]; then note "Website-Dienst übersprungen (AUTOMAT_SKIP_SYSTEM=1)"; return; fi
    as_root tee "$SERVICE_FILE" >/dev/null <<EOF
[Unit]
Description=B.A.M.B.I. Website
After=network-online.target
Wants=network-online.target
ConditionPathExists=$WEB_DIR/.env

[Service]
Type=simple
User=$USER
WorkingDirectory=$WEB_DIR/src
Environment=PYTHONUNBUFFERED=1
ExecStart=$WEB_DIR/.venv/bin/flask --app main run --host 0.0.0.0 --port $WEBSITE_PORT
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF
    run as_root systemctl daemon-reload
    run as_root systemctl enable "$SERVICE" || fail "Der Website-Dienst konnte nicht aktiviert werden."
    note "Website: systemd-Dienst $SERVICE"
}

restart_website() {
    if [[ $SKIP_SYSTEM == 1 ]]; then return; fi
    run as_root systemctl restart "$SERVICE" || fail "Der Website-Dienst startet nicht (journalctl -u $SERVICE)."
}

kiosk_settings() {
    [[ $SKIP_SYSTEM == 1 ]] && return
    command -v raspi-config >/dev/null || return 0
    if ui_confirm "Kiosk-Einstellungen" "Soll sich der Raspberry Pi beim Hochfahren automatisch anmelden und der Bildschirm nie abschalten?"$'\n\n'"Ohne automatische Anmeldung startet der Automat erst, wenn sich jemand anmeldet."; then
        run as_root raspi-config nonint do_boot_behaviour B4 || note "automatische Anmeldung konnte nicht gesetzt werden"
        run as_root raspi-config nonint do_blanking 1 || note "Bildschirmabschaltung konnte nicht geändert werden"
        note "automatische Anmeldung und Bildschirm dauerhaft an"
    fi
}

start_automat() {
    automat_running && return 0
    if ! has_display; then
        note "Kein Bildschirm in dieser Sitzung - der Automat startet bei der nächsten Anmeldung."
        return 1
    fi
    setsid "$LAUNCHER" >/dev/null 2>&1 </dev/null &
    local i
    for i in $(seq 20); do automat_running && break; sleep 0.5; done
    note "Automat gestartet"
}

# ---------------------------------------------------------------------------
# Verbindung Website <-> Automat (HMAC-Schlüssel)
# ---------------------------------------------------------------------------

hmac_configured() {
    [[ -f $WEB_DIR/.env ]] &&
        grep -q '^HMAC_KEY_ID=.' "$WEB_DIR/.env" &&
        grep -q '^HMAC_SECRET=.' "$WEB_DIR/.env" &&
        ! grep -q -e 'your_very_long' -e "=$PLACEHOLDER\$" "$WEB_DIR/.env"
}

write_env() {  # Name Schlüssel
    local tmp="$WEB_DIR/.env.tmp"
    (
        umask 077
        printf 'HMAC_KEY_ID=%s\nHMAC_SECRET=%s\n' "$1" "$2" >"$tmp"
    )
    mv "$tmp" "$WEB_DIR/.env"
}

# Prüft den Schlüssel gegen den laufenden Automaten.
# 0 = angenommen, 3 = Automat nicht erreichbar, 4 = abgelehnt
check_hmac() {
    (cd "$WEB_DIR/src" && "$WEB_DIR/.venv/bin/python" - "$BACKEND_URL" <<'PY'
import sys
import hmac_client
try:
    r = hmac_client.get(sys.argv[1] + "/course/allCourses", timeout=5)
except Exception:
    sys.exit(3)
sys.exit(0 if r.status_code == 200 else 4)
PY
    )
}

# Nimmt den Schlüssel entgegen, den der Automat per "nc 127.0.0.1 12345" schickt.
receive_secret() {  # Ausgabedatei Wartezeit
    python3 - "$HMAC_PORT" "$2" >"$1" <<'PY'
import socket, sys
port, timeout = int(sys.argv[1]), int(sys.argv[2])
server = socket.socket()
server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
server.bind(("127.0.0.1", port))
server.listen(1)
server.settimeout(timeout)
try:
    conn, _ = server.accept()
except socket.timeout:
    sys.exit(2)
conn.settimeout(5)
data = b""
while True:
    try:
        chunk = conn.recv(65536)
    except socket.timeout:
        break
    if not chunk:
        break
    data += chunk
print(data.decode(errors="replace").strip())
PY
}

wait_for_secret() {  # Name -> Schlüssel auf stdout
    local out="$INSTALL_DIR/.hmac-empfang" pid instructions
    rm -f "$out"
    receive_secret "$out" 900 &
    pid=$!
    instructions="Am Automaten:"$'\n'
    instructions+="  1. Unten rechts auf das Bambi tippen, PIN 33333 und ✓ eingeben"$'\n'
    instructions+="  2. In den Einstellungen auf »HMAC« tippen"$'\n'
    instructions+="  3. Als Namen »$1« eingeben und »Erstellen« tippen"$'\n'
    instructions+="  4. Im Fenster »IP Address« 127.0.0.1 stehen lassen und OK tippen"$'\n\n'
    instructions+="Zeigt der Automat »Verbindung wiederherstellen«, erreicht er Ausgabe, Scanner oder Station nicht - dann erst diese einschalten."$'\n\n'
    instructions+="Warte auf den Schlüssel …"

    if [[ $UI == zenity ]]; then
        (
            while kill -0 "$pid" 2>/dev/null; do echo 0; sleep 1; done
            echo 100
        ) | zenity --progress --pulsate --auto-close --cancel-label Abbrechen --width 520 --title "Schlüssel empfangen" \
            --text "$instructions" 2>/dev/null &
        local dialog=$!
        # "Abbrechen" schließt das Fenster - dann auch nicht mehr lauschen.
        while kill -0 "$pid" 2>/dev/null; do
            kill -0 "$dialog" 2>/dev/null || { kill "$pid" 2>/dev/null; break; }
            sleep 0.5
        done
        wait "$pid" 2>/dev/null || true
        wait "$dialog" 2>/dev/null || true
    else
        printf '\n%s\n(höchstens 15 Minuten; Strg+C bricht ab)\n' "$instructions" >&2
        wait "$pid" || true
    fi
    local secret
    secret=$(cat "$out" 2>/dev/null || true)
    rm -f "$out"
    [[ -n $secret ]] || return 1
    printf '%s' "$secret"
}

setup_hmac() {
    step "Verbindung Website ↔ Automat"
    local intro choice key_id secret status
    intro="Die Website braucht einen Schlüssel, den der Automat erzeugt. Dafür muss der Automat laufen und entsperrt sein."
    if ! automat_running; then
        intro+=$'\n\n'"Der Automat läuft gerade nicht. Starte ihn und lege beim ersten Start das Datenbank-Passwort fest - oder richte die Verbindung später ein."
    fi
    choice=$(ui_choice "Verbindung Website ↔ Automat" "$intro" \
        empfangen "Schlüssel vom Automaten empfangen (empfohlen)" \
        einfuegen "Schlüssel selbst einfügen" \
        spaeter "Später einrichten") || choice=spaeter
    if [[ $choice == spaeter ]]; then
        note "übersprungen - später mit: bash $APP_DIR/install.sh hmac"
        return 1
    fi

    key_id=$(ui_input "Name des Schlüssels" "Unter welchem Namen soll der Schlüssel am Automaten angelegt werden?" website) || return 1
    key_id=${key_id//[[:space:]]/}
    [[ -n $key_id && $key_id != *=* ]] || { ui_error "$TITLE" "Ungültiger Name."; return 1; }

    if [[ $choice == empfangen ]]; then
        secret=$(wait_for_secret "$key_id") || { ui_error "$TITLE" "Es kam kein Schlüssel an."; return 1; }
    else
        secret=$(ui_password "Schlüssel einfügen" "Schlüssel von »$key_id« (lange Zeichenkette hinter »$key_id:«):") || return 1
    fi
    secret=${secret#"$key_id":}
    secret=${secret//[[:space:]]/}
    [[ $secret =~ ^[A-Za-z0-9+/=]{16,}$ ]] || { ui_error "$TITLE" "Das sieht nicht nach einem gültigen Schlüssel aus."; return 1; }

    write_env "$key_id" "$secret"
    status=0
    check_hmac || status=$?
    case $status in
        0) note "Schlüssel »$key_id« vom Automaten angenommen" ;;
        3) note "Schlüssel gespeichert, konnte aber nicht geprüft werden (Automat nicht erreichbar)" ;;
        *)
            ui_error "$TITLE" "Der Automat lehnt den Schlüssel »$key_id« ab. Stimmt der Name?"$'\n\n'"Erneut versuchen mit: bash $APP_DIR/install.sh hmac"
            return 1
            ;;
    esac
    restart_website
}

# ---------------------------------------------------------------------------
# Admin-Zugang der Website
# ---------------------------------------------------------------------------

# Listet die IDs der Lehrkräfte mit dem Standardzugang (oder beendet mit 3,
# wenn der Automat nicht erreichbar ist).
default_admin_ids() {
    (cd "$WEB_DIR/src" && "$WEB_DIR/.venv/bin/python" - "$BACKEND_URL" "$DEFAULT_ADMIN_MAIL" <<'PY'
import json, sys
import hmac_client
try:
    r = hmac_client.get(sys.argv[1] + "/teacher/allTeachers", timeout=5)
except Exception:
    sys.exit(3)
if r.status_code != 200:
    sys.exit(4)
for t in json.loads(r.text)["teachers"]:
    if t.get("mail") == sys.argv[2]:
        print(t["id"])
PY
    )
}

set_admin_login() {  # neue E-Mail; Passwort kommt über ADMIN_PASSWORD (nicht in der Prozessliste)
    (cd "$WEB_DIR/src" && "$WEB_DIR/.venv/bin/python" - "$BACKEND_URL" "$DEFAULT_ADMIN_MAIL" "$1" <<'PY'
import json, os, sys
import bcrypt, hmac_client
backend, old_mail, new_mail = sys.argv[1:4]
hashed = bcrypt.hashpw(os.environ["ADMIN_PASSWORD"].encode(), bcrypt.gensalt()).decode()
teachers = json.loads(hmac_client.get(backend + "/teacher/allTeachers", timeout=5).text)["teachers"]
first = True
for t in teachers:
    if t.get("mail") != old_mail:
        continue
    t["password"] = hashed
    # Mehrere Standardzugänge: alle bekommen das neue Passwort, die E-Mail nur einer.
    if first:
        t["mail"] = new_mail
        first = False
    r = hmac_client.post(backend + "/teacher/modify", json_body=t, timeout=10)
    if r.status_code != 200:
        sys.exit(5)
PY
    )
}

# Anzahl der Lehrkräfte (Exit 3 = Automat nicht erreichbar).
teacher_count() {
    (cd "$WEB_DIR/src" && "$WEB_DIR/.venv/bin/python" - "$BACKEND_URL" <<'PY'
import json, sys
import hmac_client
try:
    r = hmac_client.get(sys.argv[1] + "/teacher/allTeachers", timeout=5)
except Exception:
    sys.exit(3)
if r.status_code != 200:
    sys.exit(4)
print(len(json.loads(r.text)["teachers"]))
PY
    )
}

# Legt den ersten Admin an. Passwort über ADMIN_PASSWORD (nicht in der Prozessliste).
# Exit 6 = es gibt inzwischen schon einen Zugang.
create_first_admin() {  # Vorname Nachname E-Mail Geschlecht
    (cd "$WEB_DIR/src" && "$WEB_DIR/.venv/bin/python" - "$BACKEND_URL" "$@" <<'PY'
import os, sys
import hmac_client
backend, first, last, mail, gender = sys.argv[1:6]
r = hmac_client.post(backend + "/teacher/createFirstAdmin", timeout=10, json_body={
    "firstName": first, "lastName": last, "email": mail, "gender": gender,
    "password": os.environ["ADMIN_PASSWORD"],
})
sys.exit(0 if r.status_code == 200 else 6 if r.status_code == 409 else 5)
PY
    )
}

ask_mail() {  # -> E-Mail auf stdout
    local mail
    while true; do
        mail=$(ui_input "Admin-Zugang" "E-Mail-Adresse für die Anmeldung auf der Website:" "") || return 1
        [[ $mail == *@*.* && $mail != *[[:space:]]* ]] && { printf '%s' "$mail"; return 0; }
        ui_error "Admin-Zugang" "Bitte eine gültige E-Mail-Adresse eingeben."
    done
}

ask_new_password() {  # -> Passwort in NEW_PASSWORD
    local pw2
    while true; do
        NEW_PASSWORD=$(ui_password "Admin-Zugang" "Passwort (mindestens 8 Zeichen):") || return 1
        pw2=$(ui_password "Admin-Zugang" "Passwort wiederholen:") || return 1
        if [[ $NEW_PASSWORD != "$pw2" ]]; then
            ui_error "Admin-Zugang" "Die Passwörter sind nicht gleich."
        elif ((${#NEW_PASSWORD} < 8)); then
            ui_error "Admin-Zugang" "Das Passwort ist zu kurz."
        else
            return 0
        fi
    done
}

setup_first_admin() {
    ui_confirm "Admin-Zugang" "Es gibt noch keinen Zugang zur Website. Wer sie als Erstes öffnet, darf den ersten Admin-Zugang anlegen."$'\n\n'"Jetzt hier anlegen?" ||
        { ADMIN_MISSING=1; note "noch kein Admin-Zugang - die erste Person auf der Website legt ihn an"; return 1; }

    local first last mail gender status=0
    first=$(ui_input "Admin-Zugang" "Vorname:" "") || return 1
    last=$(ui_input "Admin-Zugang" "Nachname:" "") || return 1
    [[ -n ${first// /} && -n ${last// /} ]] || { ui_error "Admin-Zugang" "Vor- und Nachname sind nötig."; return 1; }
    mail=$(ask_mail) || return 1
    # Muss zu Gender.java passen.
    gender=$(ui_choice "Admin-Zugang" "Geschlecht:" \
        FEMALE weiblich MALE männlich NON_BINARY nicht-binär AGENDER agender BIGENDER bigender \
        GENDERFLUID genderfluid GENDERQUEER genderqueer TRANSGENDER transgender CISGENDER cisgender \
        INTERSEX intergeschlechtlich TWO_SPIRIT two-spirit) || return 1
    ask_new_password || return 1
    ADMIN_PASSWORD="$NEW_PASSWORD" create_first_admin "$first" "$last" "$mail" "$gender" || status=$?
    NEW_PASSWORD=""
    case $status in
        0) note "Admin-Zugang angelegt, Anmeldung mit $mail" ;;
        6) note "Inzwischen hat schon jemand auf der Website einen Zugang angelegt" ;;
        *) fail "Der Admin-Zugang konnte nicht angelegt werden." ;;
    esac
}

setup_admin() {
    step "Admin-Zugang der Website"
    if ! hmac_configured; then
        note "übersprungen - zuerst die Verbindung einrichten"
        return 1
    fi
    local count status=0
    count=$(teacher_count) || status=$?
    if ((status == 3)); then
        note "Automat nicht erreichbar - später mit: bash $APP_DIR/install.sh admin"
        return 1
    elif ((status != 0)); then
        note "Lehrkräfte konnten nicht gelesen werden"
        return 1
    fi
    if ((count == 0)); then
        setup_first_admin
        return
    fi

    # Ältere Stände legten einen Standardzugang mit bekanntem Passwort an.
    local ids
    ids=$(default_admin_ids) || { note "Lehrkräfte konnten nicht gelesen werden"; return 1; }
    if [[ -z $ids ]]; then
        note "Admin-Zugang ist eingerichtet"
        return 0
    fi
    ui_confirm "Admin-Zugang" "Die Website hat noch den alten Standardzugang »$DEFAULT_ADMIN_MAIL« mit öffentlich bekanntem Passwort."$'\n\n'"Jetzt eine eigene E-Mail-Adresse und ein Passwort festlegen?" ||
        { note "nicht geändert"; return 1; }
    local mail
    mail=$(ask_mail) || return 1
    ask_new_password || return 1
    ADMIN_PASSWORD="$NEW_PASSWORD" set_admin_login "$mail" || { NEW_PASSWORD=""; fail "Der Admin-Zugang konnte nicht geändert werden."; }
    NEW_PASSWORD=""
    note "Anmeldung auf der Website jetzt mit $mail"
}

# ---------------------------------------------------------------------------
# Befehle
# ---------------------------------------------------------------------------

prepare() {
    [[ $EUID -ne 0 ]] || { echo "Bitte als normaler Benutzer starten (nicht mit sudo) - der Automat läuft unter diesem Benutzer." >&2; exit 1; }
    [[ $INSTALL_DIR != *[[:space:]]* ]] || { echo "Der Installationsordner darf keine Leerzeichen enthalten: $INSTALL_DIR" >&2; exit 1; }
    mkdir -p "$INSTALL_DIR" "$LOG_DIR"
    printf '\n===== %s %s =====\n' "$(date '+%F %T')" "${1:-install}" >>"$LOG"
    choose_ui
}

summary() {
    local ip text
    ip=$(hostname -I 2>/dev/null | awk '{print $1}')
    text="Fertig."$'\n\n'
    text+="Website: http://${ip:-<IP-Adresse>}:$WEBSITE_PORT"$'\n'
    text+="Automat: startet automatisch nach der Anmeldung am Gerät."$'\n\n'
    if ! hmac_configured; then
        text+="Noch offen: Verbindung Website ↔ Automat"$'\n'"  bash $APP_DIR/install.sh hmac"$'\n\n'
    fi
    if ((ADMIN_MISSING)); then
        text+="Noch offen: ersten Admin-Zugang anlegen - einfach die Website öffnen. Bis dahin kann das jede Person im Netzwerk tun."$'\n\n'
    fi
    text+="Beim ersten Start fragt der Automat nach einem neuen Datenbank-Passwort (mindestens 10 Ziffern). Gut merken - ohne es sind die Daten verloren."$'\n\n'
    text+="Aktualisieren: bash $APP_DIR/install.sh"$'\n'
    text+="Protokolle: $LOG_DIR"
    ui_info "$TITLE" "$text"
    printf '\n%s\n' "$text"
}

cmd_install() {
    ui_confirm "$TITLE" "Installiert bzw. aktualisiert Automat und Website in:"$'\n'"  $INSTALL_DIR"$'\n\n'"Danach starten beide automatisch mit dem Gerät. Fortfahren?" ||
        cancelled
    need_root
    install_packages
    ensure_java
    download_sources
    build_automat
    setup_website
    write_launcher
    write_service
    kiosk_settings
    local started=0
    start_automat && started=1
    restart_website
    if hmac_configured; then
        note "Verbindung zum Automaten ist schon eingerichtet"
    elif ((started)); then
        ui_info "Automat" "Der Automat startet jetzt."$'\n\n'"Beim ersten Start fragt er nach einem neuen Datenbank-Passwort (mindestens 10 Ziffern, zweimal eingeben). Leg es jetzt am Automaten fest und warte, bis die Süßigkeiten zu sehen sind."$'\n\n'"Dann hier auf OK."
        setup_hmac || true
    else
        setup_hmac || true
    fi
    setup_admin || true
    summary
}

cmd_hmac() {
    [[ -x $WEB_DIR/.venv/bin/python ]] || fail "Die Website ist noch nicht installiert - zuerst: bash install.sh"
    need_root
    setup_hmac
    setup_admin || true
}

cmd_admin() {
    [[ -x $WEB_DIR/.venv/bin/python ]] || fail "Die Website ist noch nicht installiert - zuerst: bash install.sh"
    setup_admin
}

cmd_uninstall() {
    ui_confirm "$TITLE" "Autostart des Automaten und den Website-Dienst entfernen?"$'\n\n'"Programme und Daten (auch die Datenbank) bleiben in $INSTALL_DIR." ||
        cancelled
    need_root
    step "Deinstallieren"
    stop_automat
    rm -f "$AUTOSTART_FILE" "$MENU_FILE" "$LAUNCHER"
    if [[ $SKIP_SYSTEM != 1 ]]; then
        run as_root systemctl disable --now "$SERVICE" || true
        run as_root rm -f "$SERVICE_FILE"
        run as_root systemctl daemon-reload
    fi
    ui_info "$TITLE" "Autostart und Website-Dienst sind entfernt."$'\n\n'"Die Daten liegen weiter in $INSTALL_DIR. Zum vollständigen Entfernen den Ordner löschen - damit ist auch die Datenbank weg."
}

main() {
    local command=${1:-install}
    prepare "$command"
    case $command in
        install | update) cmd_install ;;
        hmac) cmd_hmac ;;
        admin) cmd_admin ;;
        uninstall) cmd_uninstall ;;
        -h | --help | help) sed -n '2,24p' "$0" | sed 's/^# \{0,1\}//' ;;
        *) echo "Unbekannter Befehl: $command (install, hmac, admin, uninstall)" >&2; exit 1 ;;
    esac
}

# "exit" in derselben Zeile: das Update ersetzt diese Datei, während Bash sie noch liest.
main "$@"; exit $?
