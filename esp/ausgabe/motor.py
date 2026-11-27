# Motor, Multiplexer und Lichtschranken der Ausgabe.
#
# Eigene Datei, damit main.py klein bleibt: MicroPython übersetzt jede Datei
# beim Start komplett im Arbeitsspeicher - ein zu großes main.py startet auf
# dem ESP32 nicht (MemoryError) und das Update wird zurückgerollt.
#
# main.py importiert diese Datei als Erstes: beim Import werden die Pins sofort
# gesetzt (siehe unten).

# Zuerst die Motor-Pins: Beim Start ziehen die Boot-Pins GPIO 0 und 15 (C, B)
# hoch, damit ist ein Fach ausgewählt und hat Haltestrom. Das soll nur so kurz
# wie möglich dauern - also vor allen anderen Imports (config/.env, ota,
# signing), die Zeit brauchen und auch fehlschlagen können.
import time
from machine import Pin
from time import sleep_us

A = Pin(2, Pin.OUT)  # 2-13
B = Pin(15, Pin.OUT)  # 15-4
C = Pin(0, Pin.OUT)  # 0-2
STEP = Pin(13, Pin.OUT)
DIR = Pin(4, Pin.OUT)
ALWAYS_ON = Pin(12, Pin.OUT)

direction = 0
ALWAYS_ON.value(1)


def idle_pins():
    """Kein Fach ausgewählt (A/B/C = 0) - die Inverter ziehen alle Enable-Pins
    hoch, kein Motor hat Haltestrom. Mehrfach aufrufen schadet nicht."""
    STEP.value(0)
    DIR.value(0)
    A.value(0)
    B.value(0)
    C.value(0)
    ALWAYS_ON.value(1)


def release_motors():
    """Haltestrom aus für alle Motoren.

    Der Multiplexer verbindet nur den gewählten Kanal; die Eingänge der
    Inverter an den anderen Kanälen hängen dann offen und behalten ihre
    Ladung. Ein Eingang, der einmal hoch war (z.B. beim Booten, wenn die
    Boot-Pins einen Kanal wählen), hält seinen Motor sonst weiter aktiv.
    Deshalb jeden Kanal 1-7 einmal mit ALWAYS_ON = 0 wählen (Eingang entladen,
    Enable bleibt hoch) und erst dann Kanal 0 - der hat keinen Motor."""
    ALWAYS_ON.value(0)
    for bits in ((1, 1, 1), (0, 1, 1), (1, 0, 1), (0, 0, 1), (1, 1, 0), (0, 1, 0), (1, 0, 0)):
        A.value(bits[0])
        B.value(bits[1])
        C.value(bits[2])
        sleep_us(50)
    idle_pins()


release_motors()



def log(*args):
    print("[ausgabe]", *args)


# ---------------------------------------------------------------------------
# Motor und Lichtschranken (unverändert aus dem bisherigen boot.py)
# ---------------------------------------------------------------------------

SENSES = {
    1: Pin(14, Pin.IN),
    2: Pin(27, Pin.IN),
    3: Pin(26, Pin.IN),
    4: Pin(25, Pin.IN),
    5: Pin(32, Pin.IN),
    6: Pin(33, Pin.IN),
    7: Pin(34, Pin.IN),
}

mapping = [
    [0, 0, 0],  # A;B;C
    [1, 0, 0],
    [0, 1, 0],
    [1, 1, 0],
    [0, 0, 1],
    [1, 0, 1],
    [0, 1, 1],
    [1, 1, 1]
]


def step(num):
    """Dreht Fach num, bis die Lichtschranke eine Süßigkeit gesehen hat.
    Gibt zurück, ob das geklappt hat."""
    sense = SENSES[num]
    DIR.value(direction)
    time.sleep(2)
    was_one = 0
    success = False
    # Fach 6 dreht nur halb so weit und danach ein Stück weiter (Eigenheit dieses Geräts).
    for x in range(750 // 2 if num == 6 else 750):
        STEP.on()
        time.sleep(1 / 200)
        STEP.off()
        time.sleep(1 / 200)
        if sense.value() == 1:
            was_one = was_one + 1
        elif was_one >= 3:
            log("Lichtschranke ausgelöst (Motor", num, ")")
            success = True
            break
        else:
            was_one = 0
    for x in range(750 // 2 if num == 6 else 4):
        STEP.on()
        time.sleep(1 / 200)
        STEP.off()
        time.sleep(1 / 200)
    STEP.value(0)
    DIR.value(0)
    time.sleep(1)
    release_motors()
    return success


def dispense(x):
    x += 1
    if x >= 8:
        x = 1
    y = mapping[x]
    A.value(y[0])
    B.value(y[1])
    C.value(y[2])
    return step(x)
