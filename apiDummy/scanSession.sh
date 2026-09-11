#!/usr/bin/env bash
# Startet eine tmux-Session zum Simulieren von Kartenscans.
#
#   links   : interaktiver Scan-Simulator (scanSim.py)
#   rechts  : Live-Log der Website, damit man sieht was hinten ankommt
#
# Beenden: "q" im Simulator, danach  tmux kill-session -t scan
set -euo pipefail

SESSION="scan"
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG="${SCAN_LOG:-}"

if ! command -v tmux >/dev/null 2>&1; then
	echo "tmux ist nicht installiert:  sudo apt install tmux" >&2
	echo "Ohne tmux geht es auch direkt:  python3 $HERE/scanSim.py" >&2
	exit 1
fi

if tmux has-session -t "$SESSION" 2>/dev/null; then
	echo "Session '$SESSION' läuft schon - hänge mich dran."
	exec tmux attach -t "$SESSION"
fi

tmux new-session -d -s "$SESSION" -n scanner "python3 '$HERE/scanSim.py'; echo; echo '[Simulator beendet - Enter zum Schliessen]'; read"

# Zweites Fenster nur, wenn ein Log angegeben wurde (SCAN_LOG=/pfad/zur/website.log).
if [[ -n "$LOG" && -f "$LOG" ]]; then
	tmux split-window -h -t "$SESSION:scanner" "tail -f '$LOG'"
	tmux select-pane -t "$SESSION:scanner.0"
fi

exec tmux attach -t "$SESSION"
