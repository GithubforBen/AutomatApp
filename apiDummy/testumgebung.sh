#!/usr/bin/env bash
# Startet und stoppt die lokale Testumgebung.
#
#   apiDummy/testumgebung.sh start    Backend, Website und Testpult starten
#   apiDummy/testumgebung.sh stop     alles beenden
#   apiDummy/testumgebung.sh status   was läuft gerade?
#
# Das Testpult (http://localhost:8070) spielt die Geräte auf 8081-8083 und
# ersetzt damit die drei dummyExternal.py-Instanzen.
#
# Pfade lassen sich per Umgebungsvariable überschreiben:
#   WEBSITE_SRC   (~/WebstormProjects/automatWebsiteTwo/src)
#   TESTENV_LOGS  (/tmp/automat-testenv)

set -u

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WEBSITE_SRC="${WEBSITE_SRC:-$HOME/WebstormProjects/automatWebsiteTwo/src}"
LOGS="${TESTENV_LOGS:-/tmp/automat-testenv}"

# name:port:startverzeichnis:befehl
SERVICES=(
    "testpult:8070:$APP_DIR/apiDummy:python3 testpult.py"
    "website:8080:$WEBSITE_SRC:python3 main.py"
    "backend:8000:$APP_DIR:./mvnw -q javafx:run"
)
DEVICE_PORTS=(8081 8082 8083)

field() { cut -d: -f"$2" <<<"$1"; }

port_open() { ss -ltn 2>/dev/null | grep -qE "[:.]$1\b"; }

pids_on_port() { ss -ltnp 2>/dev/null | grep -E "[:.]$1\b" | grep -oE 'pid=[0-9]+' | cut -d= -f2 | sort -u; }

start_service() {
    local entry=$1 name port dir cmd
    name=$(field "$entry" 1); port=$(field "$entry" 2)
    dir=$(field "$entry" 3); cmd=$(field "$entry" 4-)
    if port_open "$port"; then
        echo "  $name läuft schon (Port $port)"
        return
    fi
    # Eigene Prozessgruppe, damit "stop" auch Kindprozesse (JVM, Flask-Reloader) erwischt.
    # setsid muss selbst der Hintergrundjob sein, sonst landet in $! die PID der
    # umgebenden Subshell statt der des Dienstes.
    (cd "$dir" || exit 1; setsid nohup $cmd >"$LOGS/$name.log" 2>&1 </dev/null & echo $! >"$LOGS/$name.pid")
    echo "  $name gestartet, Log: $LOGS/$name.log"
}

wait_for() {
    local name=$1 port=$2 seconds=$3
    for _ in $(seq "$seconds"); do
        port_open "$port" && return 0
        sleep 1
    done
    echo "  !! $name ist nach ${seconds}s nicht erreichbar, siehe $LOGS/$name.log"
    return 1
}

cmd_start() {
    mkdir -p "$LOGS"
    for port in "${DEVICE_PORTS[@]}"; do
        if port_open "$port" && ! port_open 8070; then
            echo "  Port $port ist belegt (alte Dummies?), beende sie"
            kill $(pids_on_port "$port") 2>/dev/null
        fi
    done
    sleep 0.5
    echo "Starte Testumgebung"
    for entry in "${SERVICES[@]}"; do start_service "$entry"; done
    wait_for testpult 8070 10 && wait_for website 8080 20
    echo "  warte auf das Backend (dauert rund eine Minute)"
    wait_for backend 8000 300 || return 1
    echo
    echo "Fertig."
    echo "  Testpult  http://localhost:8070"
    echo "  Website   http://localhost:8080"
}

cmd_stop() {
    echo "Stoppe Testumgebung"
    for entry in "${SERVICES[@]}"; do
        local name port pidfile
        name=$(field "$entry" 1); port=$(field "$entry" 2); pidfile="$LOGS/$name.pid"
        if [[ -f $pidfile ]]; then
            kill -- -"$(cat "$pidfile")" 2>/dev/null
            rm -f "$pidfile"
        fi
        # Auch Prozesse erwischen, die nicht über dieses Skript gestartet wurden.
        local left
        left=$(pids_on_port "$port")
        [[ -n $left ]] && kill $left 2>/dev/null
        echo "  $name beendet"
    done
    for port in "${DEVICE_PORTS[@]}"; do
        local left
        left=$(pids_on_port "$port")
        [[ -n $left ]] && kill $left 2>/dev/null
    done
}

cmd_status() {
    for entry in "${SERVICES[@]}"; do
        local name port
        name=$(field "$entry" 1); port=$(field "$entry" 2)
        if port_open "$port"; then echo "  läuft   $name ($port)"; else echo "  aus     $name ($port)"; fi
    done
}

case "${1:-start}" in
    start) cmd_start ;;
    stop) cmd_stop ;;
    restart) cmd_stop; sleep 2; cmd_start ;;
    status) cmd_status ;;
    *) echo "Aufruf: $0 {start|stop|restart|status}"; exit 1 ;;
esac
