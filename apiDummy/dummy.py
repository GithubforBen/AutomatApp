import flask;

app = flask.Flask(__name__);

@app.route("/sweets")
def sweets():
    return {"5": {"name": "Haribo", "hours": 2},
            "4": {"name": "Smarties", "hours": 2,}, 
            "7": {"name": "Stats", "hours": -1}, 
            "6": {"name": "Brause", "hours": 2},
            "1": {"name": "Dublo", "hours": 2}, 
            "0": {"name": "Mentos", "hours": 2}, 
            "3": {"name": "Maoam", "hours": 2}, 
            "2": {"name": "Kinder", "hours": 2}}

@app.route("/alarm_on", methods=["POST"])
def alarm_on():
    return "success";

@app.route("/alarm_off", methods=["POST"])
def alarm_off():
        return "success";

@app.route("/dispense", methods=["POST"])
def dispense():
    print(flask.request.json["nr"])
    if flask.request.json["nr"] in range(0, 8):
        return "success";
    else:
        return "error: 235";

@app.route("/ping")
def ping():
    return "1"

@app.route("/fill" methods=["POST"])
def fill():
    json = flask.request.json
    print(json["name"] + ":" + json["amount"])
    return "Added:" + json["name"] + ":" + json["amount"]

@app.route("/mint")
def mint():
    return '{"da": 10, "weg": 100}'

if __name__ == "__main__":
    app.run()

