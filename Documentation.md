# AutomatApp API Documentation

The AutomatApp REST API provides access to user management, attendance tracking, and course administration.

**Base URL:** `http://localhost:8000`

## Authentication

All requests must be authenticated via HMAC-SHA256. The server validates the signature against the API key associated
with the provided Key ID.

### Required Headers

| Header | Description |
| :--- | :--- |
| `X-API-KEY-ID` | Unique identifier for the API key |
| `X-TIMESTAMP` | Current Unix timestamp (must be within 30 seconds of server time) |
| `X-NONCE` | A unique string for each request to prevent replay attacks |
| `X-SIGNATURE` | The generated HMAC-SHA256 signature |

### Signing Algorithm

The signature is a **Base64-encoded** HMAC-SHA256 hash of the following concatenated string:

`{keyId}:{timestamp}:{nonce}:{body}`

* **keyId**: The value from `X-API-KEY-ID`.
* **timestamp**: The value from `X-TIMESTAMP`.
* **nonce**: The value from `X-NONCE`.
* **body**: The raw JSON string for POST, PUT, or DELETE requests. For GET requests, use an empty string.

**Note:** The server expects compact JSON without spaces: `{"key":"value"}`.

### Python Integration Example

```python
import base64
import hmac
import hashlib
import time
import secrets
import requests
import json


def make_request(method, endpoint, body=None, key_id="your_key_id", secret="your_secret"):
    url = f"http://localhost:8000{endpoint}"
    timestamp = str(int(time.time()))
    nonce = secrets.token_hex(16)

    # Body must be compact JSON with no spaces
    payload = json.dumps(body, separators=(',', ':')) if body else ""

    # Create signing string
    signing_string = f"{key_id}:{timestamp}:{nonce}:{payload}"

    # Generate signature
    signature = hmac.new(
        secret.encode('utf-8'),
        signing_string.encode('utf-8'),
        hashlib.sha256
    ).digest()
    signature = base64.b64encode(signature).decode('utf-8')

    headers = {
        "X-API-KEY-ID": key_id,
        "X-TIMESTAMP": timestamp,
        "X-NONCE": nonce,
        "X-SIGNATURE": signature,
        "Content-Type": "application/json"
    }

    if method == "POST":
        return requests.post(url, data=payload, headers=headers)
    elif method == "GET":
        return requests.get(url, headers=headers)
    elif method == "DELETE":
        return requests.delete(url, data=payload, headers=headers)
    elif method == "PUT":
        return requests.put(url, data=payload, headers=headers)


# Example: Adding a teacher
teacher_data = {
    "firstName": "Max",
    "lastName": "Mustermann",
    "rfid": [1, 2, 3, 4],
    "gender": "MALE",
    "birthday": 631152000000,
    "address": {
        "nr": 10,
        "street": "Hauptstrasse",
        "city": "Berlin",
        "zip": 10115,
        "country": "Germany"
    },
    "email": "max@example.com",
    "password": "securepassword",
    "level": "NORMAL"
}

response = make_request("POST", "/teacher/add", body=teacher_data)
print(response.status_code, response.text)
```

## Index

### GET /

Returns the main HTML interface.

* **Response (200 OK):** `text/html`

## Energetics

Used to reset the screensaver timer on the hardware terminal.

### GET /energetics

### POST /energetics

Resets the timer.

* **Response (200 OK):** `200` (plain text)

## Login

### POST /login

Authenticates a user via RFID bytes.

* **Request Body:**
  ```json
  {"rfid":[10,20,30,40,50]}
  ```
* **Response (200 OK):**
  ```json
  {
    "cameIn": true,
    "time": 15.5,
    "name": "Max Mustermann",
    "text": [
      "Herzlich Willkommen,",
      "Max Mustermann",
      "im MINT-Zentrum!",
      "Stunden: 15.5"
    ]
  }
  ```
    * `cameIn` — `true` if the user logged in, `false` if they logged out.
    * `time` — The user's current hour balance (Konto), **not** a timestamp.
    * `name` — Full name of the user.
    * `text` — A 4-element array for display on the terminal: greeting (changes based on `cameIn`), truncated name (max
      20 chars), location label, and hours balance string.

## Attendances

### POST /attendances

Retrieves attendance records for a specific user ID.

* **Request Body:**
  ```json
  {"id":123}
  ```
* **Response (200 OK):**
  ```json
  {
    "attendances": [
      {"day":13,"month":3,"year":2026,"type":"NORMAL"},
      {"day":12,"month":3,"year":2026,"type":"AWAY"}
    ]
  }
  ```

## Teacher

### POST /teacher/add

Creates a new teacher. Use `email` and `address` keys.

* **Request Body:**
  ```json
  {
    "firstName": "string",
    "lastName": "string",
    "rfid": [1,2,3,4],
    "gender": "MALE",
    "birthday": 123456789000,
    "address": {
      "nr": 1,
      "street": "string",
      "city": "string",
      "zip": 12345,
      "country": "string"
    },
    "email": "string",
    "password": "string",
    "level": "ADMIN"
  }
  ```
* **Response (200 OK):** `Successfully added teacher`

### GET /teacher/all

Returns all registered teachers.

* **Response (200 OK):**
  ```json
  {
    "teachers": [
      {
        "id": 1,
        "firstName": "Max",
        "lastName": "Mustermann",
        "rfid": [1,2,3,4],
        "gender": "MALE",
        "birthday": 123456789000,
        "wohnort": {
          "id": 5,
          "nr": 10,
          "street": "Strasse",
          "city": "Stadt",
          "zip": 12345,
          "country": "Land"
        },
        "mail": "max@example.com",
        "password": "hashed_password",
        "level": "NORMAL"
      }
    ]
  }
  ```

### DELETE /teacher/delete

Removes a teacher by ID.

* **Request Body:**
  ```json
  {"id":1}
  ```
* **Response (200 OK):** `Successfully deleted teacher`

### POST /teacher/modify

Updates an existing teacher. Requires full JSON object including `id`.

**Note:** Use `mail` and `wohnort` keys (matching the `GET /teacher/all` response), **not** `email` and `address`.

* **Request Body:**
  ```json
  {
    "id": 1,
    "firstName": "Max",
    "lastName": "Updated",
    "rfid": [1,2,3,4],
    "gender": "MALE",
    "birthday": 123456789000,
    "wohnort": {
      "id": 5,
      "nr": 11,
      "street": "Neue Strasse",
      "city": "Stadt",
      "zip": 12345,
      "country": "Land"
    },
    "mail": "max@example.com",
    "password": "hashed_password",
    "level": "NORMAL"
  }
  ```
* **Response (200 OK):** `Successfully added teacher`

## Student

### POST /student/add

Enqueues a student for registration. Use `address` key.

**Warning:** This endpoint only enqueues the student. They are not saved to the database until `/seed/flush` is called
with an RFID tag.

* **Request Body:**
  ```json
  {
    "firstName": "string",
    "lastName": "string",
    "gender": "MALE",
    "birthday": 123456789000,
    "address": {
      "nr": 1,
      "street": "string",
      "city": "string",
      "zip": 12345,
      "country": "string"
    },
    "kurse": [1, 2]
  }
  ```
* **Response (200 OK):** `Successfully added student`

### GET /student/all

Returns all registered students.

* **Response (200 OK):**
  ```json
  {
    "students": [
      {
        "id": 10,
        "firstName": "Student",
        "lastName": "Name",
        "rfid": [5,6,7,8],
        "gender": "MALE",
        "birthday": 123456789000,
        "wohnort": {
          "id": 20,
          "nr": 1,
          "street": "Strasse",
          "city": "Stadt",
          "zip": 12345,
          "country": "Land"
        },
        "hours": [15.5],
        "kurse": [
          {
            "id": 1,
            "name": "Math",
            "day": "MONDTAG",
            "tutor": [
              {
                "id": 1,
                "firstName": "Max",
                "lastName": "Mustermann",
                "rfid": [1,2,3,4],
                "gender": "MALE",
                "birthday": 123456789000,
                "wohnort": {"id":5,"nr":10,"street":"Strasse","city":"Stadt","zip":12345,"country":"Land"},
                "mail": "max@example.com",
                "password": "hashed_password",
                "level": "NORMAL"
              }
            ]
          }
        ]
      }
    ]
  }
  ```

### DELETE /student/delete

Removes a student by ID.

* **Request Body:**
  ```json
  {"id":10}
  ```
* **Response (200 OK):** `Successfully deleted Student`

### POST /student/modify

Performs a partial update on a student. Requires `id`.

* **Request Body:**
  ```json
  {
    "id": 10,
    "firstName": "NewName",
    "lastName": "NewLast"
  }
  ```
* **Supported Fields:** `firstName`, `lastName`, `gender`, `rfid`, `birthday`, `wohnort`, `kurse`.
* **Response (200 OK):** `Successfully modified Student`

## Course (Kurs)

### POST /course/add

Creates a new course.

* **Request Body:**
  ```json
  {
    "name": "Robotics",
    "tutor": ["1", "2"],
    "day": "MONDTAG"
  }
  ```
* **Response (200 OK):** `Successfully added course: Kurs{id=1, name='Robotics', tutor=[...], day=MONDTAG}` (includes
  the created course details)

### GET /course/all

Returns all courses.

* **Response (200 OK):**
  ```json
  {
    "courses": [
      {
        "id": 1,
        "name": "Robotics",
        "day": "MONDTAG",
        "tutor": [
          {
            "id": 1,
            "firstName": "Max",
            "lastName": "Mustermann",
            "rfid": [1,2,3,4],
            "gender": "MALE",
            "birthday": 123456789000,
            "wohnort": {"id":5,"nr":10,"street":"Strasse","city":"Stadt","zip":12345,"country":"Land"},
            "mail": "max@example.com",
            "password": "hashed_password",
            "level": "NORMAL"
          }
        ]
      }
    ]
  }
  ```

### DELETE /course/delete

Removes a course by ID.

* **Request Body:**
  ```json
  {"id":1}
  ```
* **Response (200 OK):** `Successfully deleted Kurs`

### POST /course/modify

Updates a course. Supports partial updates and teacher list modification.

* **Request Body:**
  ```json
  {
    "id": 1,
    "name": "Advanced Robotics",
    "day": "METTWOCH",
    "addTeacher": "3",
    "removeTeacher": "1"
  }
  ```
* **Response (200 OK):** `Successfully deleted Kurs` (Note: server returns this text for modify as well)

## CSV Export

### POST /csv

Generates a CSV export of attendances for a course.

* **Request Body:**
  ```json
  {"kurs": 1}
  ```
* **Parameters:** Use `-1` for `kurs` to export all courses.
* **Response (200 OK):** `text/csv`
    * Separator: Semicolon (`;`)
    * Columns: Vorname, Nachname, [Date Columns...]
    * Values: Anwesend, Nicht anwesend

## Scanned

Used to log raw RFID scans.

### POST /scanned

### PUT /scanned

### GET /scanned

Accepts GET, POST, and PUT methods. GET requests will always return `410` since no body can be provided.

* **Request Body (POST/PUT):**
  ```json
  {"rfid": [1,2,3,4]}
  ```
* **Response (200 OK):** `success`

## Seed Endpoints ⚠️

These endpoints are for internal use and testing.

### POST /seed/flush ⚠️

Assigns an RFID tag to the next student in the addition queue and persists them.

* **Request Body:**
  ```json
  {"rfid": [10,11,12,13]}
  ```
* **Response (200 OK):** `Successfully flushed student`

### POST /seed/attendance ⚠️

Manually adds an attendance record.

* **Request Body:**
  ```json
  {
    "id": 10,
    "day": 13,
    "month": 3,
    "year": 2026,
    "login": 1710324000000,
    "logout": 1710327600000,
    "type": "NORMAL"
  }
  ```
* **Response (200 OK):** `Successfully added attendance`

## Enum Reference Tables

### Gender

<details>
<summary>View all Gender values</summary>

ABINARY, AGENDER, AGENDERFLUID, AGENDERFLUX, GENDERBLANK, GENDERFREE, POLYAGENDER, AMBIGENDER, ANDROGYNE, ANDROGYNOUS,
APORAGENDER, AUTIGENDER, BAKLA, BIGENDER, BINARY, BISSU, BUTCH, CALABAI, CALALAI, CIS, CISGENDER, CIS_FEMALE, CIS_MALE,
CIS_MAN, CIS_WOMAN, DEMI_BOY, DEMIFLUX, DEMIGENDER, DEMI_GIRL, DEMI_GUY, DEMI_MAN, DEMI_WOMAN, DUAL_GENDER, EUNUCH,
FA_AFAFINE, FEMALE, FEMALE_TO_MALE, FEMME, FTM, F14TOMCAT, GENDER_BENDER, GENDER_DIVERSE, GENDER_GIFTED, GENDERFAE,
GENDERFLUID, GENDERFLUX, GENDERFUCK, GENDERLESS, GENDER_NONCONFORMING, GENDERQUEER, GENDER_QUESTIONING, GENDER_VARIANT,
GRAYGENDER, HIJRA, HELICOPTER, INTERGENDER, INTERSEX, IPSOGENDER, KATHOEY, MAHU, MALE, MALE_TO_FEMALE, MAN,
MAN_OF_TRANS_EXPERIENCE, MAVERIQUE, META_GENDER, MTF, MULTIGENDER, MUXE, NEITHER, NEUROGENDER, NEUTROIS, NON_BINARY,
NON_BINARY_TRANSGENDER, OMNIGENDER, OTHER, PANGENDER, PERSON_OF_TRANSGENDERED_EXPERIENCE, POLYGENDER, QUEER, SEKHET,
THIRD_GENDER, TRANS, TRANS_FEMALE, TRANS_MALE, TRANS_MAN, TRANS_PERSON, TRANS_WOMAN, TRANSGENDER, TRANSGENDER_FEMALE,
TRANSGENDER_MALE, TRANSGENDER_MAN, TRANSGENDER_PERSON, TRANSGENDER_WOMAN, TRANSFEMININE, TRANSMASELINE, TRANSSEXUAL,
TRANSSEXUAL_FEMALE, TRANSSEXUAL_MALE, TRANSSEXUAL_MAN, TRANSSEXUAL_PERSON, TRANSSEXUAL_WOMAN, TRAVESTI, TRIGENDER,
TUMTUM, TWO_SPIRIT, VAKASALEWALEWA, WARIA, WINKTE, WOMAN, WOMAN_OF_TRANS_EXPERIENCE, X_GENDER, X_JENDA, XENOGENDER,
YAOI, YAOIGENDER, YURI, YURIGENDER, ZENGENDER
</details>

### Day

* MONDTAG
* DEINSTAG
* METTWOCH
* DÖNNERSTAG
* REINTAG
* SAUFTAG
* SONNDAG

### Level

* NORMAL
* ADMIN

### Attendance Type

* NORMAL
* AWAY
* EXCUSED

## Error Handling

The API uses HTTP status codes to indicate the outcome of a request.

| Status Code | Description |
| :--- | :--- |
| **200 OK** | The request was successful. |
| **400 Bad Request** | Invalid data provided (e.g., ID not found, `id=0`, queue empty). |
| **401 Unauthorized** | HMAC signature validation failed or headers missing. |
| **410 Gone** | Invalid or missing JSON body. Request could not be parsed. |
| **501 Not Implemented** | No user associated with the provided RFID (Login endpoint only). |
