from machine import Pin
import time
import socket
import network
import ujson

# WLAN-Zugangsdaten und HMAC-Schlüssel stehen in .env (nicht eingecheckt, siehe .env.example).
import env
ENV = env.load()

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
    

def step(num: int):
    print("Start stepping")
    DIR.value(direction)
    time.sleep(2)
    wasOne = 0
    for x in range(66050):
        STEP.on()
        time.sleep(1/200)
        STEP.off()
        time.sleep(1/200)
        print(getSense(num).value())
        if getSense(num).value() == 1:
            wasOne = wasOne + 1
        elif wasOne >= 3:
            print(f"Stopped spinning:", num);
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
    return

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
    

def dispense(x: int):
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
    step(x);
    

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
    
    
def make_http(text = str):
    http_response = """HTTP/1.1 200 OK
        Content-Type: text/plain; charset=utf-8
        Content-Length: """+str(len(text))+"""
        Cache-Control: no-cache
        Connection: close

        """+text;
    return http_response;
    

def listen(port):
    connection = socket.socket();
    connection.bind(("0.0.0.0", 80));
    connection.listen(128);
    
    #try:
    while True:
        client = connection.accept()[0]
        request = client.recv(1024)
        request = request.decode()
        print(request)
            
        method = request.split()[0]
        path = request.split()[1]
        print(method=="POST")
        print(path=="/dispense")
        print(1)
            
        if method == "GET" and path == "/ping":
            print("Ben ist kacke")
            client.send(make_http("Ben ist ein Arschloch!"));
            client.close();
        
        elif method == "POST" and path == '/dispense':
            print(3)
            print('POST', connection)
                #conn, addr = connection.accept()
                #data = conn.recv(1024).decode("ascii")
                #print(data)
                #print(ujson.loads(request[request.find("{"):])["nr"])
            dispense(ujson.loads(request[request.find("{"):])["nr"])
            client.send(make_http("Ben ist ein süßiiii <3 <3 Und ich will etwas von ihm <3"));
            client.close();
            
        else:
            print("ERROR");

    #except Exception as e:
    #    print("Error:");
    #    print(e);
    
    #finally:
    #    connection.close();


def main():
    connect_wlan();
    
    listen(80);


if __name__ == "__main__":
    main();
