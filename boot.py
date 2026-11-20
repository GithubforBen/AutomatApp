from machine import Pin, RTC
import time
import socket
import network
import ujson
import hmac
import hashlib
import binascii
import os
import ntptime

# WLAN-Zugangsdaten und HMAC-Schlüssel stehen in .env (nicht eingecheckt).
# Format: KEY=wert pro Zeile, siehe esp/ausgabe/.env.example.
def load_env(path=".env"):
    values = {}
    with open(path) as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                key, value = line.split("=", 1)
                values[key.strip()] = value.strip().strip('"').strip("'")
    return values


ENV = load_env()

A = Pin(2 ,Pin.OUT)#2-13
B = Pin(15, Pin.OUT)#15-4
C = Pin(0, Pin.OUT)#0-2
STEP = Pin(13, Pin.OUT)
DIR = Pin(4, Pin.OUT)
ALWAYS_ON = Pin(12, Pin.OUT)

direction = 0
ALWAYS_ON.value(1)

SENSE_1 = Pin(14, Pin.IN)
SENSE_2 = Pin(27, Pin.IN)
SENSE_3 = Pin(26, Pin.IN)
SENSE_4 = Pin(25, Pin.IN)
SENSE_5 = Pin(32, Pin.IN)
SENSE_6 = Pin(33, Pin.IN)
SENSE_7 = Pin(34, Pin.IN)

mapping = [
    [0,0,0],#A;B;C
    [1,0,0],
    [0,1,0],
    [1,1,0],
    [0,0,1],
    [1,0,1],
    [0,1,1],
    [1,1,1]
]

A.value(0)
B.value(0)
C.value(0)

def getSense(num: int):
    if num == 1:
        return SENSE_1
    elif num == 2:
        return SENSE_2
    elif num == 3:
        return SENSE_3
    elif num == 4:
        return SENSE_4
    elif num == 5:
        return SENSE_5
    elif num == 6:
        return SENSE_6
    elif num == 7:
        return SENSE_7
    else:
        raise Exception("Wieso hast du es noch nicht wie der Elektriker gemacht??!")


def step(num: int) -> bool:
    print("Start stepping")
    DIR.value(direction)
    time.sleep(2)
    wasOne = 0
    success = False
    for x in range(750):
        STEP.on()
        time.sleep(1/200)
        STEP.off()
        time.sleep(1/200)
        print(getSense(num).value())
        if getSense(num).value() == 1:
            wasOne = wasOne + 1
        elif wasOne >= 3:
            print(f"Stopped spinning:", num);
            success = True
            break
        else:
            wasOne = 0
    for x in range(4):
        STEP.on()
        time.sleep(1/200)
        STEP.off()
        time.sleep(1/200)
    STEP.value(0)
    DIR.value(0)
    time.sleep(1)
    ALWAYS_ON.value(0)
    A.value(0)
    B.value(0)
    C.value(0)
    ALWAYS_ON.value(1)
    return success

b = 0
rev = 0
z = False
zah = 1
#while True:
while False:
    #x = str(zah)
    x = int(input("Welcher motor?:"))
    zah = zah + 1
    if zah >= 8:
        zah = 1
    print(x)
    print(zah)
    y = mapping[x]
    A.value(y[0]);
    B.value(y[1]);
    C.value(y[2]);
    step(x);


def dispense(x: int) -> bool:
    x += 1
    if x >= 8:
        x = 1
    if x <= 0:
        raise Exception("IDK BRO - just die or something")
    print(x)
    y = mapping[x]
    A.value(y[0]);
    B.value(y[1]);
    C.value(y[2]);
    return step(x);


def connect_wlan():
    wlan = network.WLAN(network.WLAN.IF_STA);
    wlan.active(False);
    time.sleep(0.1)
    wlan.active(True);
    wlan.connect(ENV["WLAN_SSID"], ENV["WLAN_PASSWORD"]);
    print(network.STAT_IDLE)
    print(network.STAT_CONNECTING)
    print(network.STAT_WRONG_PASSWORD)
    print(network.STAT_NO_AP_FOUND)
    #print(network.WLAN.STAT_CONNECT_FAIL)
    print(network.STAT_GOT_IP)

    while not wlan.isconnected():
        print(wlan.status())
        time.sleep(1)
        pass;
    print(wlan.ipconfig(addr4=("192.168.188.202", "255.255.255.0")));
    time.sleep(1)
    print(wlan.ifconfig());

    # Sync the RTC so generate_header()'s timestamp matches the server's clock within
    # MAX_SKEW_SECONDS. Without this, time.time() counts seconds since boot from the
    # MicroPython epoch (2000-01-01), and every HMAC request would fail. If NTP fails here
    # (e.g. MINTSeeheim blocks outbound NTP), the RTC gets corrected later from the
    # X-Unix-Time header of the server's /ping request (see listen()).
    sync_time()


def sync_time():
    """Tries to sync the RTC via NTP, attempting several servers (incl. the WiFi gateway,
    which often doubles as a local NTP server) before giving up."""
    hosts = ["pool.ntp.org", "de.pool.ntp.org", "time.google.com"]
    try:
        hosts.append(network.WLAN(network.WLAN.IF_STA).ifconfig()[2])
    except Exception:
        pass
    for host in hosts:
        for attempt in range(3):
            try:
                ntptime.host = host
                ntptime.settime()
                print("NTP time synced via", host, ":", time.time())
                return True
            except Exception as e:
                print("NTP sync via", host, "failed (attempt", attempt + 1, "):", e)
                time.sleep(1)
    print("NTP sync failed for all hosts; waiting for time from /ping")
    return False


def set_time_from_unix(unix_ts: int):
    """Sets the RTC from a Unix timestamp, e.g. received via the X-Unix-Time header of /ping."""
    tm = time.localtime(unix_ts - 946684800)
    RTC().datetime((tm[0], tm[1], tm[2], tm[6], tm[3], tm[4], tm[5], 0))
    print("Time synced from /ping:", time.time())


def make_http(text = str):
    http_response = """HTTP/1.1 200 OK
        Content-Type: text/plain; charset=utf-8
        Content-Length: """+str(len(text))+"""
        Cache-Control: no-cache
        Connection: close

        """+text;
    return http_response;


def generate_random_string(length: int) -> str:
    chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    return "".join(chars[b % len(chars)] for b in os.urandom(length))


def generate_header(msg: str) -> bytes:
    key_id: str = ENV["HMAC_KEY_ID"];
    key: bytes = ENV["HMAC_KEY"].encode()
    # Converts MicroPython epoch (2000-01-01) seconds to Unix epoch (1970-01-01) seconds.
    # Requires the RTC to be synced, either via sync_time() in connect_wlan() or from the
    # X-Unix-Time header of /ping (see set_time_from_unix() and listen()).
    timestamp: int = time.time() + 946684800;
    nonce: str = generate_random_string(2**4);
    data: bytes = f"{key_id}:{timestamp}:{nonce}:{msg}".encode();
    signature: str = binascii.b2a_base64(hmac.new(key, data, hashlib.sha256).digest()).decode()[:-1]	;

    return {
        "X-API-KEY-ID": key_id,
        "X-TIMESTAMP": str(timestamp),
        "X-NONCE": nonce,
        "X-SIGNATURE": signature,
        "Content-Type": "application/json"
    };


# Learned from the source address of the GET /ping request the Java backend sends on
# startup (and from the "ping" button in Settings) - see the "/ping" branch in listen().
# None until the first ping arrives.
SERVER_HOST = None
SERVER_PORT = 8000


def send_dispense_failed(nr: int):
    """Reports to the Java backend that the dispense for slot `nr` did not produce a stop signal,
    so the corresponding Sweet gets deactivated until refilled."""
    if SERVER_HOST is None:
        print("Can't report dispense failure: server host not yet known (no /ping received)")
        return
    body = '{"nr":' + str(nr) + '}'
    headers = generate_header(body)
    try:
        addr = socket.getaddrinfo(SERVER_HOST, SERVER_PORT)[0][-1]
        s = socket.socket()
        s.connect(addr)
        lines = ["POST /dispense/failed HTTP/1.1", "Host: " + SERVER_HOST + ":" + str(SERVER_PORT)]
        for key, value in headers.items():
            lines.append(key + ": " + value)
        lines.append("Content-Length: " + str(len(body)))
        lines.append("Connection: close")
        lines.append("")
        lines.append(body)
        s.send("\r\n".join(lines).encode())
        print(s.recv(1024))
        s.close()
    except Exception as e:
        print("Failed to report dispense failure:", e)


def listen(port):
    connection = socket.socket();
    connection.bind(("0.0.0.0", 80));
    connection.listen(128);

    while True:
        client, addr = connection.accept()
        try:
            request = client.recv(1024)
            request = request.decode()
            print(request)

            method = request.split()[0]
            path = request.split()[1]
            print(method=="POST")
            print(path=="/dispense")
            print(1)

            if method == "GET" and path == "/ping":
                global SERVER_HOST
                SERVER_HOST = addr[0]
                print("Learned server host from /ping:", SERVER_HOST)
                for line in request.split("\r\n"):
                    if line.lower().startswith("x-unix-time:"):
                        try:
                            set_time_from_unix(int(line.split(":", 1)[1].strip()))
                        except Exception as e:
                            print("Failed to sync time from /ping:", e)
                client.send(make_http("Ben ist ein Arschloch!"));
                client.close();

            elif method == "POST" and path == '/dispense':
                print(3)
                print('POST', connection)
                nr = ujson.loads(request[request.find("{"):])["nr"]
                # Acknowledge immediately: dispense() can take much longer than the
                # Java client's read timeout, so the result is reported separately below.
                client.send(make_http("Ben ist ein süßiiii <3 <3 Und ich will etwas von ihm <3"));
                client.close();
                success = dispense(nr)
                if not success:
                    send_dispense_failed(nr)
            else:
                print("ERROR");
                client.close();
        except Exception as e:
            print("Error:");
            print(e);
            try:
                client.close();
            except Exception:
                pass


def main():
    connect_wlan();
    listen(80);


if __name__ == "__main__":
    main();

