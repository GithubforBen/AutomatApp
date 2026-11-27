# Fehler beim Start der Ausgabe festhalten und anzeigen (siehe main.py).
#
# Schreibt den Fehler nach crash.txt und beantwortet dann jede Anfrage mit
# HTTP 503; /crash liefert den Fehlertext. Nach einem Update läuft die neue
# Version auf Probe - boot.py startet den ESP dann nach 3 Minuten neu und rollt
# nach drei Fehlversuchen zurück. Sonst startet der ESP hier nach 10 Minuten
# neu.

import gc
import sys
import time

RESTART_AFTER_MS = 10 * 60 * 1000


def handle(error):
    gc.collect()
    text = describe(error)
    print(text)
    try:
        with open("crash.txt", "w") as f:
            f.write(text)
    except Exception as e:
        print("crash.txt nicht geschrieben:", e)
    try:
        serve(text)
    except Exception as e:
        print("Fehleranzeige nicht möglich:", e)
    _restart()


def describe(error):
    lines = ["Ausgabe startet nicht:"]
    try:
        with open("ota_version") as f:
            lines.append("Version " + f.read().strip())
    except Exception:
        pass
    try:
        import io
        buf = io.StringIO()
        sys.print_exception(error, buf)
        lines.append(buf.getvalue().strip())
    except Exception:
        lines.append(repr(error))
    try:
        gc.collect()
        lines.append("Freier Arbeitsspeicher: {} Bytes".format(gc.mem_free()))
    except Exception:
        pass
    return "\n".join(lines)


def _connect():
    import network
    wlan = network.WLAN(network.STA_IF)
    wlan.active(True)
    if wlan.isconnected():
        return
    import config
    wlan.connect(config.WLAN_SSID, config.WLAN_PASSWORD)
    started = time.ticks_ms()
    while not wlan.isconnected():
        if time.ticks_diff(time.ticks_ms(), started) > 30000:
            raise OSError("WLAN nicht erreichbar")
        time.sleep_ms(200)
    static_ip = getattr(config, "STATIC_IP", None)
    if static_ip:
        ip, mask, gateway, dns = wlan.ifconfig()
        if ip != static_ip:
            wlan.ifconfig((static_ip, mask, gateway, dns))


def serve(text):
    import socket
    _connect()
    body = text.encode()
    server = socket.socket()
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind(socket.getaddrinfo("0.0.0.0", 80)[0][-1])
    server.listen(2)
    # Kurzer Timeout: so laufen zwischendurch Python-Befehle, und der Timer aus
    # boot.py (Neustart nach der Probezeit) kommt dran.
    server.settimeout(1)
    started = time.ticks_ms()
    print("Fehler unter http://<Ausgabe>/crash")
    while time.ticks_diff(time.ticks_ms(), started) < RESTART_AFTER_MS:
        try:
            client, _ = server.accept()
        except OSError:
            continue
        try:
            client.settimeout(3)
            request = client.recv(512)
            parts = request.split(b" ")
            path = parts[1] if len(parts) > 1 else b"/"
            if path.startswith(b"/crash"):
                status, data = "200 OK", body
            else:
                status, data = "503 Service Unavailable", b"crashed - see /crash"
            client.sendall("HTTP/1.1 {}\r\nContent-Type: text/plain; charset=utf-8\r\nContent-Length: {}\r\nConnection: close\r\n\r\n"
                           .format(status, len(data)).encode() + data)
        except Exception:
            pass
        finally:
            client.close()


def _restart():
    import machine
    machine.reset()
