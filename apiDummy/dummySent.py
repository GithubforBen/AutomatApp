import requests;

# beispiel für die request, die an den Server gehen soll
def dispense(n: int):
    r = requests.post("http://127.0.0.1:8000/dispense", json={"nr": n});
    print(r.text)

def scanned(id: list[int]):
    r = requests.post("http://127.0.0.1:8000/scanned", json=id)
    print(r.text)


def strom():
    r = requests.post("http://127.0.0.1:8000/energetics")
    print(r.text)

#time in seconds
scanned({"name": "Ben Schnorri","time": 3600*2,"rfid": [99, 179, 107, 0, 187]});#if time is equal to -2147483648 sweets will be dispensed

while True:
    s = input("was willst du machen")
    if s == "s":
        strom()
    elif s == "b":
        scanned({"name": "Ben Schnorrenberger","time": 3600*2,"rfid": [99, 179, 107, 0, 187]});#if time is equal to -2147483648 sweets will be dispensed
    elif s == "d":
        scanned({"name": "David Glänzel","time": 3600,"rfid": [99, 179, 107, 2  , 187]});#if time is equal to -2147483648 sweets will be dispensed
    elif s == "a":
        scanned({"name": "Benjamin Schnorrenberger-Glänzel","time": 3600,"rfid": [99, 179, 107, 2  , 187]});#if time is equal to -2147483648 sweets will be dispensed
    else:
        print(s + " ist keine Option")

