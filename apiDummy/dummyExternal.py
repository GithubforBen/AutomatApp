import flask
import threading
from flask import request, jsonify

# We will run one Flask app that can handle multiple "virtual" hosts or ports if needed,
# but for simplicity of a single file execution, we'll implement it to be able to start multiple instances
# or just one instance that provides all endpoints if they were to share a host.
# The user asked for it to be able to parse ping requests for website/ausgabe/scanner/station.

app = flask.Flask(__name__)


@app.route("/ping")
def ping():
    return "1"


@app.route("/re-enable")
def re_enable():
    print(request.json["name"])
    return "Added:" + request.json["name"]


@app.route("/fill", methods=["POST"])
def fill():
    print(request.json["name"] + ":" + str(request.json["nr"]))
    return "Added:" + request.json["name"] + ":" + str(request.json["nr"])


@app.route("/mint")
def mint():
    return jsonify({"da": 100, "weg": 150})


@app.route("/sweets")
def sweets():
    return jsonify({
        "0": {"name": "Mentos", "hours": 2},
        "1": {"name": "Duplo", "hours": 2},
        "2": {"name": "Kinder", "hours": 2},
        "3": {"name": "Maoam", "hours": 2},
        "4": {"name": "Smarties", "hours": 2},
        "5": {"name": "Haribo", "hours": 2},
        "6": {"name": "Brause", "hours": 2},
        "7": {"name": "Stats", "hours": -1}
    })


@app.route("/dispense", methods=["POST"])
def dispense():
    data = request.json
    print(f"Dispensing: {data}")
    if data and "nr" in data:
        return "success"
    return "error: 235"


@app.route("/alarm_on")
def alarm_on():
    print("Alarm ON")
    return "success"


@app.route("/alarm_off")
def alarm_off():
    print("Alarm OFF")
    return "success"


def run_app(port):
    app.run(port=port, host='0.0.0.0')


if __name__ == "__main__":
    import sys

    # Default ports for the four components if running all from this script
    # Website: 8080 (from config.yaml)
    # Dispenser: 8081
    # Scanner: 8082
    # Station: 8083

    ports = [8080, 8081, 8082, 8083]

    if len(sys.argv) > 1:
        # Run a single instance on specified port
        port = int(sys.argv[1])
        print(f"Starting dummy on port {port}...")
        app.run(port=port, host='0.0.0.0')
    else:
        # Run multiple instances in threads
        print(
            "Starting multiple dummy instances for Website (8080), Dispenser (8081), Scanner (8082), Station (8083)...")
        threads = []
        for port in ports:
            t = threading.Thread(target=run_app, args=(port,))
            t.daemon = True
            t.start()
            threads.append(t)

        print("All dummies started. Press Ctrl+C to stop.")
        try:
            for t in threads:
                t.join()
        except KeyboardInterrupt:
            print("Stopping...")
