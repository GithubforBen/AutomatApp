# Kleine HTTP-Hilfen der Ausgabe (eigene Datei, damit main.py klein bleibt -
# siehe motor.py).

import socket


def http_post(host, port, path, body, headers, timeout=5):
    """Schickt einen POST und gibt den HTTP-Status zurück."""
    sock = socket.socket()
    sock.settimeout(timeout)
    try:
        sock.connect(socket.getaddrinfo(host, port)[0][-1])
        data = body.encode()
        head = "POST {} HTTP/1.1\r\nHost: {}:{}\r\n".format(path, host, port)
        for key in headers:
            head += "{}: {}\r\n".format(key, headers[key])
        head += "Content-Length: {}\r\nConnection: close\r\n\r\n".format(len(data))
        sock.sendall(head.encode() + data)
        return int(sock.recv(64).split()[1])
    finally:
        sock.close()


def respond(client, status, body, content_type="text/plain; charset=utf-8"):
    reason = {200: "OK", 400: "Bad Request", 404: "Not Found"}.get(status, "OK")
    data = body.encode()
    head = "HTTP/1.1 {} {}\r\nContent-Type: {}\r\nContent-Length: {}\r\nConnection: close\r\n\r\n".format(
        status, reason, content_type, len(data))
    client.sendall(head.encode() + data)


def read_request(client):
    """Liest Kopf und (per Content-Length) den ganzen Körper - der kann in
    einem zweiten Netzwerkpaket kommen."""
    data = b""
    while data.find(b"\r\n\r\n") < 0 and len(data) < 8192:
        chunk = client.recv(512)
        if not chunk:
            break
        data += chunk
    end = data.find(b"\r\n\r\n")
    head = data[:end] if end >= 0 else data
    body = data[end + 4:] if end >= 0 else b""
    lines = head.decode().split("\r\n")
    parts = lines[0].split()
    headers = {}
    for line in lines[1:]:
        if ":" in line:
            key, value = line.split(":", 1)
            headers[key.strip().lower()] = value.strip()
    length = int(headers.get("content-length", "0"))
    while len(body) < length:
        chunk = client.recv(512)
        if not chunk:
            break
        body += chunk
    method = parts[0] if parts else ""
    path = parts[1].split("?")[0] if len(parts) > 1 else ""
    return method, path, headers, body
