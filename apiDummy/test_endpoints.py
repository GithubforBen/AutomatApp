import base64
import hashlib
import hmac
import json
import os
import random
import requests
import secrets
import shutil
import sys
import time

# ── Configuration ──────────────────────────────────────────────────────────────
BASE_URL = "http://localhost:8000"
KEY_ID = "token"
SECRET = "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"
# ── Shared test state ──────────────────────────────────────────────────────────
_state = {
    "teacher_id": None,
    "teacher_email": None,
    "student_id": None,
    "course_id": None,
    "suffix": secrets.token_hex(4),
}

_seed_state = {
    "teacher_ids": [],
    "course_ids": [],
    "student_ids": [],
}


# ── HMAC helpers ───────────────────────────────────────────────────────────────

def generate_hmac_signature(key_id, timestamp, nonce, secret, body=""):
    """Generate HMAC-SHA256 signature."""
    data = f"{key_id}:{timestamp}:{nonce}:{body}"
    signature = hmac.new(
        secret.encode("utf-8"),
        data.encode("utf-8"),
        hashlib.sha256,
    ).digest()
    return base64.b64encode(signature).decode("utf-8")


def send_authenticated_request(method, endpoint, body=None):
    """Send an HMAC-authenticated request. Returns the requests.Response."""
    url = f"{BASE_URL}{endpoint}"
    timestamp = str(int(time.time()))
    nonce = secrets.token_hex(16)

    body_str = json.dumps(body, separators=(",", ":")) if body is not None else ""

    signature = generate_hmac_signature(KEY_ID, timestamp, nonce, SECRET, body_str)

    headers = {
        "X-API-KEY-ID": KEY_ID,
        "X-TIMESTAMP": timestamp,
        "X-NONCE": nonce,
        "X-SIGNATURE": signature,
        "Content-Type": "application/json",
    }

    method_upper = method.upper()
    if method_upper == "GET":
        response = requests.get(url, headers=headers)
    elif method_upper == "POST":
        response = requests.post(url, headers=headers, data=body_str)
    elif method_upper == "PUT":
        response = requests.put(url, headers=headers, data=body_str)
    elif method_upper == "DELETE":
        response = requests.delete(url, headers=headers, data=body_str)
    else:
        raise ValueError(f"Unsupported method: {method}")

    assert response.status_code != 401, f"Unauthorized access to {endpoint} (401)"
    return response


def _safe_json(response):
    try:
        return response.json()
    except Exception:
        return None


def _send_raw(method, endpoint, raw_body):
    """Send a request with a raw (non-JSON-serialised) body string."""
    url = f"{BASE_URL}{endpoint}"
    timestamp = str(int(time.time()))
    nonce = secrets.token_hex(16)
    signature = generate_hmac_signature(KEY_ID, timestamp, nonce, SECRET, raw_body)
    headers = {
        "X-API-KEY-ID": KEY_ID,
        "X-TIMESTAMP": timestamp,
        "X-NONCE": nonce,
        "X-SIGNATURE": signature,
        "Content-Type": "application/json",
    }
    if method.upper() == "POST":
        return requests.post(url, headers=headers, data=raw_body)
    elif method.upper() == "GET":
        return requests.get(url, headers=headers)
    raise ValueError(f"Unsupported raw method: {method}")


# ── Lookup helpers ─────────────────────────────────────────────────────────────

def _get_teachers():
    response = send_authenticated_request("GET", "/teacher/all")
    if response.status_code != 200:
        return []
    data = _safe_json(response)
    if not data:
        return []
    return data.get("teachers", [])


def _find_teacher_id_by_email(email):
    for t in _get_teachers():
        mail = t.get("mail") or t.get("email")
        if mail == email:
            return t.get("id")
    return None


def _get_students():
    response = send_authenticated_request("GET", "/student/all")
    if response.status_code != 200:
        return []
    data = _safe_json(response)
    if not data:
        return []
    return data.get("students", [])


def _find_student_id_by_name(first, last):
    for s in _get_students():
        if s.get("firstName") == first and s.get("lastName") == last:
            return s.get("id")
    return None


def _get_courses():
    response = send_authenticated_request("GET", "/course/all")
    if response.status_code != 200:
        return []
    data = _safe_json(response)
    if not data:
        return []
    return data.get("courses", [])


def _find_course_id_by_name(name):
    for c in _get_courses():
        if c.get("name") == name:
            return c.get("id")
    return None


SEED_MARKER = "SEEDTEST_"

GERMAN_FIRST_NAMES = [
    "Anna", "Lena", "Marie", "Sophie", "Laura", "Lea", "Julia", "Lara", "Emma", "Mia",
    "Paul", "Max", "Leon", "Lukas", "Felix", "Jonas", "Tim", "Tom", "Niklas", "Jan",
    "Finn", "Moritz", "Ben", "Erik", "Karl", "Hannah", "Lisa", "Sarah", "Amelie", "Emilia",
    "Clara", "Noah", "Elias",
]

GERMAN_LAST_NAMES = [
    "Müller", "Schmidt", "Schneider", "Fischer", "Weber", "Meyer", "Wagner", "Becker", "Schulz", "Hoffmann",
    "Koch", "Richter", "Wolf", "Schröder", "Neumann", "Schwarz", "Braun", "Zimmermann", "Krüger", "Hartmann",
    "Lange", "Werner", "Lehmann", "Peters", "Scholz", "König", "Huber", "Kaiser", "Fuchs", "Herrmann",
    "Graf", "Vogel", "Friedrich",
]

GERMAN_STREETS = [
    "Hauptstraße", "Bahnhofstraße", "Schulstraße", "Gartenstraße", "Dorfstraße",
    "Bergstraße", "Kirchstraße", "Waldstraße", "Ringstraße", "Lindenstraße",
]

GERMAN_CITIES = [
    "Berlin", "München", "Hamburg", "Köln", "Frankfurt",
    "Stuttgart", "Düsseldorf", "Dortmund", "Essen", "Leipzig",
]

DAYS = ["MONDTAG", "DEINSTAG", "METTWOCH", "DÖNNERSTAG", "REINTAG", "SAUFTAG", "SONNDAG"]

COURSE_NAMES = ["Mathematik", "Deutsch", "Englisch", "Physik", "Informatik", "Kunst", "Sport"]

GENDERS_SUBSET = ["MALE", "FEMALE", "NON_BINARY"]


def _random_rfid():
    """Generate a random 4-byte RFID value."""
    return [random.randint(0, 255) for _ in range(4)]


def wipe_test_data():
    global _seed_state

    teacher_deleted = 0
    course_deleted = 0
    student_deleted = 0

    for student in _get_students():
        first_name = student.get("firstName") or ""
        if first_name.startswith(SEED_MARKER):
            sid = student.get("id")
            if sid is None:
                continue
            resp = send_authenticated_request("DELETE", "/student/delete", {"id": sid})
            if resp.status_code == 200:
                student_deleted += 1

    for course in _get_courses():
        name = course.get("name") or ""
        if name.startswith(SEED_MARKER):
            cid = course.get("id")
            if cid is None:
                continue
            resp = send_authenticated_request("DELETE", "/course/delete", {"id": cid})
            if resp.status_code == 200:
                course_deleted += 1

    for teacher in _get_teachers():
        first_name = teacher.get("firstName") or ""
        if first_name.startswith(SEED_MARKER):
            tid = teacher.get("id")
            if tid is None:
                continue
            resp = send_authenticated_request("DELETE", "/teacher/delete", {"id": tid})
            if resp.status_code == 200:
                teacher_deleted += 1

    _seed_state["teacher_ids"] = []
    _seed_state["course_ids"] = []
    _seed_state["student_ids"] = []

    print(f"🧹 Wiped seeded data: teachers={teacher_deleted}, courses={course_deleted}, students={student_deleted}")


def seed_test_data():
    global _seed_state

    print("🌱 Seeding test data...")
    _seed_state = {
        "teacher_ids": [],
        "course_ids": [],
        "student_ids": [],
    }

    wipe_test_data()

    for i in range(12):
        first_name = f"{SEED_MARKER}{GERMAN_FIRST_NAMES[i]}"
        last_name = GERMAN_LAST_NAMES[i]
        teacher_body = {
            "firstName": first_name,
            "lastName": last_name,
            "rfid": _random_rfid(),
            "gender": GENDERS_SUBSET[i % len(GENDERS_SUBSET)],
            "birthday": random.randint(0, 946684800000),
            "address": {
                "nr": random.randint(1, 100),
                "street": random.choice(GERMAN_STREETS),
                "city": random.choice(GERMAN_CITIES),
                "zip": random.randint(10000, 99999),
                "country": "Deutschland",
            },
            "email": f"{SEED_MARKER.lower()}{GERMAN_FIRST_NAMES[i].lower()}.{last_name.lower()}@automat.test",
            "password": f"{GERMAN_FIRST_NAMES[i].lower()}123",
            "level": "ADMIN" if i < 2 else "NORMAL",
        }
        send_authenticated_request("POST", "/teacher/add", teacher_body)

    seeded_teachers = [
        t for t in _get_teachers()
        if (t.get("firstName") or "").startswith(SEED_MARKER)
    ]
    _seed_state["teacher_ids"] = [t.get("id") for t in seeded_teachers if t.get("id") is not None]
    print(f"  • Teachers created: {len(_seed_state['teacher_ids'])}")

    teacher_id_strings = [str(tid) for tid in _seed_state["teacher_ids"]]
    if teacher_id_strings:
        for i, day in enumerate(DAYS):
            tutor_count = min(len(teacher_id_strings), random.randint(1, 2))
            course_body = {
                "name": f"{SEED_MARKER}{COURSE_NAMES[i]}",
                "tutor": random.sample(teacher_id_strings, tutor_count),
                "day": day,
            }
            send_authenticated_request("POST", "/course/add", course_body)

    seeded_courses = [
        c for c in _get_courses()
        if (c.get("name") or "").startswith(SEED_MARKER)
    ]
    _seed_state["course_ids"] = [c.get("id") for c in seeded_courses if c.get("id") is not None]
    print(f"  • Courses created: {len(_seed_state['course_ids'])}")

    student_name_to_rfid = {}
    course_id_strings = [str(cid) for cid in _seed_state["course_ids"]]
    for i in range(32):
        first_name = f"{SEED_MARKER}{GERMAN_FIRST_NAMES[i % len(GERMAN_FIRST_NAMES)]}"
        last_name = GERMAN_LAST_NAMES[(i + 5) % len(GERMAN_LAST_NAMES)]
        student_rfid = _random_rfid()
        student_name_to_rfid[(first_name, last_name)] = student_rfid

        student_body = {
            "firstName": first_name,
            "lastName": last_name,
            "gender": GENDERS_SUBSET[i % len(GENDERS_SUBSET)],
            "birthday": random.randint(1104537600000, 1325376000000),
            "address": {
                "nr": random.randint(1, 100),
                "street": random.choice(GERMAN_STREETS),
                "city": random.choice(GERMAN_CITIES),
                "zip": random.randint(10000, 99999),
                "country": "Deutschland",
            },
            "kurse": random.sample(course_id_strings,
                                   random.randint(1, min(3, len(course_id_strings)))) if course_id_strings else [],
        }
        send_authenticated_request("POST", "/student/add", student_body)
        send_authenticated_request("POST", "/seed/flush", {"rfid": student_rfid})

    seeded_students = [
        s for s in _get_students()
        if (s.get("firstName") or "").startswith(SEED_MARKER)
    ]
    _seed_state["student_ids"] = [s.get("id") for s in seeded_students if s.get("id") is not None]
    print(f"  • Students created: {len(_seed_state['student_ids'])}")

    login_created = 0
    for student in seeded_students:
        sid = student.get("id")
        first_name = student.get("firstName") or ""
        last_name = student.get("lastName") or ""
        if sid is None:
            continue
        student_rfid = student_name_to_rfid.get((first_name, last_name))
        if not student_rfid:
            continue
        resp = send_authenticated_request("POST", "/login", {"rfid": student_rfid})
        if resp.status_code == 200:
            login_created += 1
    print(f"  • Student login/Konto triggered: {login_created}")

    attendance_count = 0
    for sid in _seed_state["student_ids"]:
        for _ in range(random.randint(2, 3)):
            day = random.randint(1, 28)
            month = random.randint(1, 3)
            login_ms = int(time.mktime((2026, month, day, 8, 0, 0, 0, 0, -1)) * 1000)
            logout_ms = int(time.mktime((2026, month, day, 14, 0, 0, 0, 0, -1)) * 1000)
            attendance_body = {
                "id": sid,
                "day": day,
                "month": month,
                "year": 126,
                "login": login_ms,
                "logout": logout_ms,
                "type": "NORMAL",
            }
            resp = send_authenticated_request("POST", "/seed/attendance", attendance_body)
            if resp.status_code == 200:
                attendance_count += 1
    print(f"  • Attendance records created: {attendance_count}")

    print(
        "✔ Seeding complete! "
        f"(teachers={len(_seed_state['teacher_ids'])}, "
        f"courses={len(_seed_state['course_ids'])}, "
        f"students={len(_seed_state['student_ids'])})"
    )


class ProgressBar:
    """
    Persistent bottom-of-terminal progress bar.
    Uses ANSI scroll region to keep the last line reserved for the bar while
    normal output scrolls above it – similar to apt / dpkg output.
    """

    def __init__(self, total: int):
        self.total = total
        self.done = 0
        self.passed = 0
        self.failed = 0
        self.current_test = ""
        self._rows, self._cols = self._terminal_size()
        sys.stdout.write(f"\033[1;{self._rows - 1}r")  # set scroll region to rows-1
        sys.stdout.write("\033[s")  # save cursor
        sys.stdout.flush()
        self._draw()

    @staticmethod
    def _terminal_size():
        size = shutil.get_terminal_size((80, 24))
        return size.lines, size.columns

    def update(self, test_name: str, passed: bool):
        self.done += 1
        if passed:
            self.passed += 1
        else:
            self.failed += 1
        self.current_test = test_name
        self._draw()

    def set_current(self, test_name: str):
        """Update the bar to show which test is currently running."""
        self.current_test = test_name
        self._draw()

    def _draw(self):
        cols = self._cols
        pct = int(self.done / self.total * 100) if self.total else 0
        bar_width = max(cols - 40, 10)
        filled = int(bar_width * self.done / self.total) if self.total else 0
        bar = "█" * filled + "░" * (bar_width - filled)

        status = f" {pct:3d}% [{bar}] {self.done}/{self.total}  ✓{self.passed} ✗{self.failed}"
        status = status[:cols].ljust(cols)

        sys.stdout.write("\033[s")  # save cursor
        sys.stdout.write(f"\033[{self._rows};1H")  # goto last row
        sys.stdout.write(f"\033[0;36m{status}\033[0m")  # draw bar (cyan)
        sys.stdout.write("\033[u")  # restore cursor
        sys.stdout.flush()

    def finish(self):
        """Restore scroll region and clear the bar line."""
        sys.stdout.write(f"\033[1;{self._rows}r")  # restore full scroll region
        sys.stdout.write(f"\033[{self._rows};1H")  # goto last row
        sys.stdout.write("\033[2K")  # clear line
        sys.stdout.write(f"\033[{self._rows - 1};1H")  # position cursor above old bar
        sys.stdout.flush()


def _drain_add_queue():
    """Drain stale entries from the server's student-add queue."""
    for _ in range(50):
        resp = send_authenticated_request("POST", "/seed/flush", {"rfid": [0, 0, 0, 0]})
        if resp.status_code != 200:
            break


def setup_test_data():
    """Create a test teacher and a test student that all tests can reference."""
    _drain_add_queue()
    suffix = _state["suffix"]

    email = f"test.teacher.{suffix}@automat.test"
    _state["teacher_email"] = email
    teacher_body = {
        "firstName": "TestTeacher",
        "lastName": suffix,
        "rfid": [250, 250, 250, 250],
        "gender": "FEMALE",
        "birthday": 946684800000,
        "address": {
            "nr": 1,
            "street": "Test Street",
            "city": "Teststadt",
            "zip": 10000,
            "country": "Germany",
        },
        "email": email,
        "password": "testpassword123",
        "level": "ADMIN",
    }
    resp = send_authenticated_request("POST", "/teacher/add", teacher_body)
    assert resp.status_code == 200, f"Setup: teacher/add failed ({resp.status_code}): {resp.text}"

    teacher_id = _find_teacher_id_by_email(email)
    assert teacher_id is not None, "Setup: could not find created teacher by email"
    _state["teacher_id"] = teacher_id

    student_rfid = [251, 251, 251, 251]
    student_body = {
        "firstName": "TestStudent",
        "lastName": suffix,
        "gender": "MALE",
        "birthday": 1009843200000,
        "address": {
            "nr": 2,
            "street": "Student Alley",
            "city": "Teststadt",
            "zip": 20000,
            "country": "Germany",
        },
        "kurse": [],
    }
    resp = send_authenticated_request("POST", "/student/add", student_body)
    assert resp.status_code == 200, f"Setup: student/add failed ({resp.status_code}): {resp.text}"

    # Flush the student from the add-queue into the database (student/add only enqueues)
    resp = send_authenticated_request("POST", "/seed/flush", {"rfid": student_rfid})
    assert resp.status_code == 200, f"Setup: seed/flush failed ({resp.status_code}): {resp.text}"

    student_id = _find_student_id_by_name("TestStudent", suffix)
    assert student_id is not None, "Setup: could not find created student by name"
    _state["student_id"] = student_id


def teardown_test_data():
    """Delete the test teacher and test student created during setup."""
    errors = []

    if _state.get("course_id"):
        try:
            resp = send_authenticated_request("DELETE", "/course/delete", {"id": _state["course_id"]})
            if resp.status_code != 200:
                errors.append(f"course/delete returned {resp.status_code}: {resp.text}")
        except Exception as e:
            errors.append(f"course/delete error: {e}")

    if _state.get("student_id"):
        try:
            resp = send_authenticated_request("DELETE", "/student/delete", {"id": _state["student_id"]})
            if resp.status_code != 200:
                errors.append(f"student/delete returned {resp.status_code}: {resp.text}")
        except Exception as e:
            errors.append(f"student/delete error: {e}")

    if _state.get("teacher_id"):
        try:
            resp = send_authenticated_request("DELETE", "/teacher/delete", {"id": _state["teacher_id"]})
            if resp.status_code != 200:
                errors.append(f"teacher/delete returned {resp.status_code}: {resp.text}")
        except Exception as e:
            errors.append(f"teacher/delete error: {e}")

    if errors:
        print(f"\n⚠  Teardown warnings: {'; '.join(errors)}")


def test_index():
    """GET / – should return HTML."""
    resp = send_authenticated_request("GET", "/")
    assert resp.status_code == 200, f"Expected 200, got {resp.status_code}"
    assert "text/html" in resp.headers.get("Content-Type", ""), "Expected text/html content type"


# ── Energetics ─────────────────────────────────────────────────────────────────

def test_energetics_get():
    """GET /energetics – should return '200'."""
    resp = send_authenticated_request("GET", "/energetics")
    assert resp.status_code == 200, f"Expected 200, got {resp.status_code}"
    assert resp.text == "200", f"Expected body '200', got '{resp.text}'"


def test_energetics_post():
    """POST /energetics – should return 200."""
    resp = send_authenticated_request("POST", "/energetics")
    assert resp.status_code == 200, f"Expected 200, got {resp.status_code}"


# ── Login ──────────────────────────────────────────────────────────────────────

def test_login_student():
    """POST /login – with student RFID."""
    data = {"rfid": [99, 253, 101, 0, 251]}
    resp = send_authenticated_request("POST", "/login", data)
    if resp.status_code == 200:
        body = resp.json()
        for key in ("cameIn", "time", "name", "text"):
            assert key in body, f"Response missing '{key}'"
    else:
        assert resp.status_code == 501, f"Expected 200 or 501, got {resp.status_code}"


def test_login_teacher():
    """POST /login – with teacher RFID."""
    data = {"rfid": [0, 0, 0, 0, 0]}
    resp = send_authenticated_request("POST", "/login", data)
    if resp.status_code == 200:
        body = resp.json()
        for key in ("cameIn", "time", "name", "text"):
            assert key in body, f"Response missing '{key}'"
    else:
        assert resp.status_code == 501, f"Expected 200 or 501, got {resp.status_code}"


def test_login_invalid_json():
    """POST /login – with invalid JSON body → expect 410."""
    resp = _send_raw("POST", "/login", "invalid json")
    assert resp.status_code != 401, f"HMAC verification failed: {resp.text}"
    assert resp.status_code == 410, f"Expected 410, got {resp.status_code}"


def test_login_missing_body():
    """POST /login – with empty body → expect 410."""
    resp = _send_raw("POST", "/login", "")
    assert resp.status_code != 401, f"HMAC verification failed: {resp.text}"
    assert resp.status_code == 410, f"Expected 410, got {resp.status_code}"


# ── Attendances ────────────────────────────────────────────────────────────────

def test_attendances():
    """POST /attendances – for a known user id."""
    data = {"id": _state["student_id"] or 1}
    resp = send_authenticated_request("POST", "/attendances", data)
    if resp.status_code == 200:
        body = resp.json()
        assert "attendances" in body, "Response missing 'attendances'"
    else:
        assert resp.status_code in (200, 400), f"Unexpected status {resp.status_code}"


def test_attendances_missing_body():
    """POST /attendances – with no body → expect 410."""
    resp = _send_raw("POST", "/attendances", "")
    assert resp.status_code != 401, f"HMAC verification failed: {resp.text}"
    assert resp.status_code == 410, f"Expected 410, got {resp.status_code}"


# ── Teacher endpoints ──────────────────────────────────────────────────────────

def test_teacher_all():
    """GET /teacher/all – should return list of teachers."""
    resp = send_authenticated_request("GET", "/teacher/all")
    assert resp.status_code == 200, f"Expected 200, got {resp.status_code}"
    body = resp.json()
    assert "teachers" in body, "Response missing 'teachers'"
    assert isinstance(body["teachers"], list), "'teachers' is not a list"


def test_teacher_add_and_delete():
    """POST /teacher/add + DELETE /teacher/delete – lifecycle test."""
    suffix = secrets.token_hex(4)
    email = f"lifecycle.teacher.{suffix}@automat.test"
    data = {
        "firstName": "Lifecycle",
        "lastName": "Teacher",
        "rfid": [201, 201, 201, 201],
        "gender": "MALE",
        "birthday": 946684800000,
        "address": {
            "nr": 99,
            "street": "Lifecycle St",
            "city": "Teststadt",
            "zip": 99999,
            "country": "Germany",
        },
        "email": email,
        "password": "lifecycle123",
        "level": "NORMAL",
    }
    resp = send_authenticated_request("POST", "/teacher/add", data)
    assert resp.status_code == 200, f"teacher/add failed: {resp.status_code} {resp.text}"

    tid = _find_teacher_id_by_email(email)
    assert tid is not None, "Teacher not found after add"

    resp = send_authenticated_request("DELETE", "/teacher/delete", {"id": tid})
    assert resp.status_code == 200, f"teacher/delete failed: {resp.status_code} {resp.text}"


def test_teacher_modify():
    """POST /teacher/modify – modify the setup teacher's first name."""
    tid = _state["teacher_id"]
    assert tid is not None, "No test teacher available (setup failed?)"

    teachers = _get_teachers()
    teacher = None
    for t in teachers:
        if t.get("id") == tid:
            teacher = t
            break
    assert teacher is not None, f"Could not find teacher id={tid} in /teacher/all"

    # Send the full teacher JSON as-is from toJSON (fromJSON reads the same keys)
    mod = dict(teacher)
    mod["firstName"] = "ModifiedTeacher"
    resp = send_authenticated_request("POST", "/teacher/modify", mod)
    assert resp.status_code == 200, f"teacher/modify failed: {resp.status_code} {resp.text}"

    teachers_after = _get_teachers()
    found = False
    for t in teachers_after:
        if t.get("id") == tid:
            revert = dict(t)
            revert["firstName"] = "TestTeacher"
            send_authenticated_request("POST", "/teacher/modify", revert)
            found = True
            break
    assert found, "Teacher not found after modify"


# ── Student endpoints ──────────────────────────────────────────────────────────

def test_student_all():
    """GET /student/all – should return list of students."""
    resp = send_authenticated_request("GET", "/student/all")
    assert resp.status_code == 200, f"Expected 200, got {resp.status_code}"
    body = resp.json()
    assert "students" in body, "Response missing 'students'"
    assert isinstance(body["students"], list), "'students' is not a list"


def test_student_add_and_delete():
    """POST /student/add + DELETE /student/delete – lifecycle test."""
    suffix = secrets.token_hex(4)
    data = {
        "firstName": "Lifecycle",
        "lastName": f"Student{suffix}",
        "gender": "FEMALE",
        "birthday": 1009843200000,
        "address": {
            "nr": 77,
            "street": "Lifecycle Ave",
            "city": "Teststadt",
            "zip": 77777,
            "country": "Germany",
        },
        "kurse": [],
    }
    resp = send_authenticated_request("POST", "/student/add", data)
    assert resp.status_code == 200, f"student/add failed: {resp.status_code} {resp.text}"

    resp = send_authenticated_request("POST", "/seed/flush", {"rfid": [202, 202, 202, 202]})
    assert resp.status_code == 200, f"seed/flush failed: {resp.status_code} {resp.text}"

    sid = _find_student_id_by_name("Lifecycle", f"Student{suffix}")
    assert sid is not None, "Student not found after add"

    resp = send_authenticated_request("DELETE", "/student/delete", {"id": sid})
    assert resp.status_code == 200, f"student/delete failed: {resp.status_code} {resp.text}"


def test_student_modify():
    """POST /student/modify – modify the setup student's first name."""
    sid = _state["student_id"]
    assert sid is not None, "No test student available (setup failed?)"

    mod = {
        "id": sid,
        "firstName": "ModifiedStudent",
    }
    resp = send_authenticated_request("POST", "/student/modify", mod)
    assert resp.status_code == 200, f"student/modify failed: {resp.status_code} {resp.text}"

    revert = {"id": sid, "firstName": "TestStudent"}
    send_authenticated_request("POST", "/student/modify", revert)


# ── Course endpoints ───────────────────────────────────────────────────────────

def test_course_all():
    """GET /course/all – should return list of courses."""
    resp = send_authenticated_request("GET", "/course/all")
    assert resp.status_code == 200, f"Expected 200, got {resp.status_code}"
    body = resp.json()
    assert "courses" in body, "Response missing 'courses'"
    assert isinstance(body["courses"], list), "'courses' is not a list"


def test_course_add_and_delete():
    """POST /course/add + DELETE /course/delete – lifecycle test."""
    tid = _state["teacher_id"]
    assert tid is not None, "No test teacher available for course"

    course_name = f"TestCourse_{_state['suffix']}"
    data = {
        "name": course_name,
        "tutor": [str(tid)],
        "day": "MONDTAG",
    }
    resp = send_authenticated_request("POST", "/course/add", data)
    assert resp.status_code == 200, f"course/add failed: {resp.status_code} {resp.text}"

    cid = _find_course_id_by_name(course_name)
    assert cid is not None, "Course not found after add"

    resp = send_authenticated_request("DELETE", "/course/delete", {"id": cid})
    assert resp.status_code == 200, f"course/delete failed: {resp.status_code} {resp.text}"


def test_course_modify():
    """POST /course/modify – create a course, modify it, then delete it."""
    tid = _state["teacher_id"]
    assert tid is not None, "No test teacher available for course"

    course_name = f"ModCourse_{_state['suffix']}"
    data = {
        "name": course_name,
        "tutor": [str(tid)],
        "day": "DEINSTAG",
    }
    resp = send_authenticated_request("POST", "/course/add", data)
    assert resp.status_code == 200, f"course/add for modify test failed: {resp.status_code} {resp.text}"

    cid = _find_course_id_by_name(course_name)
    assert cid is not None, "Course not found for modify"

    mod = {"id": cid, "name": f"Modified_{course_name}", "day": "METTWOCH"}
    resp = send_authenticated_request("POST", "/course/modify", mod)
    assert resp.status_code == 200, f"course/modify failed: {resp.status_code} {resp.text}"

    resp = send_authenticated_request("DELETE", "/course/delete", {"id": cid})
    assert resp.status_code == 200, f"course/delete after modify failed: {resp.status_code} {resp.text}"


# ── CSV export ─────────────────────────────────────────────────────────────────

def test_csv_export():
    """POST /csv – export CSV for all courses."""
    data = {"kurs": -1}
    resp = send_authenticated_request("POST", "/csv", data)
    assert resp.status_code == 200, f"Expected 200, got {resp.status_code}"
    ct = resp.headers.get("Content-Type", "")
    assert "text/csv" in ct, f"Expected text/csv content type, got '{ct}'"


def test_csv_invalid_json():
    """POST /csv – with invalid JSON → expect 410."""
    resp = _send_raw("POST", "/csv", "not json")
    assert resp.status_code != 401, f"HMAC verification failed: {resp.text}"
    assert resp.status_code == 410, f"Expected 410, got {resp.status_code}"


# ── Scanned endpoint ──────────────────────────────────────────────────────────

def test_scanned_post():
    """POST /scanned – send RFID data."""
    data = {"rfid": [1, 2, 3, 4]}
    resp = send_authenticated_request("POST", "/scanned", data)
    assert resp.status_code == 200, f"Expected 200, got {resp.status_code}"
    assert "success" in resp.text.lower(), f"Expected 'success' in body, got '{resp.text}'"


def test_scanned_get():
    """GET /scanned – without body → expect 410 (body required)."""
    resp = send_authenticated_request("GET", "/scanned")
    assert resp.status_code == 410, f"Expected 410, got {resp.status_code}"


def test_scanned_put():
    """PUT /scanned – send RFID data via PUT."""
    data = {"rfid": [5, 6, 7, 8]}
    resp = send_authenticated_request("PUT", "/scanned", data)
    assert resp.status_code == 200, f"Expected 200, got {resp.status_code}"
    assert "success" in resp.text.lower(), f"Expected 'success' in body, got '{resp.text}'"


def test_scanned_missing_body():
    """POST /scanned – without body → expect 410."""
    resp = _send_raw("POST", "/scanned", "")
    assert resp.status_code != 401, f"HMAC verification failed: {resp.text}"
    assert resp.status_code == 410, f"Expected 410, got {resp.status_code}"


# ══════════════════════════════════════════════════════════════════════════════
#  Test runner
# ══════════════════════════════════════════════════════════════════════════════

TESTS = [
    ("GET /", test_index),
    ("GET /energetics", test_energetics_get),
    ("POST /energetics", test_energetics_post),
    ("POST /login (student)", test_login_student),
    ("POST /login (teacher)", test_login_teacher),
    ("POST /login (invalid json)", test_login_invalid_json),
    ("POST /login (missing body)", test_login_missing_body),
    ("POST /attendances", test_attendances),
    ("POST /attendances (missing body)", test_attendances_missing_body),
    ("GET /teacher/all", test_teacher_all),
    ("POST /teacher/add + DELETE", test_teacher_add_and_delete),
    ("POST /teacher/modify", test_teacher_modify),
    ("GET /student/all", test_student_all),
    ("POST /student/add + DELETE", test_student_add_and_delete),
    ("POST /student/modify", test_student_modify),
    ("GET /course/all", test_course_all),
    ("POST /course/add + DELETE", test_course_add_and_delete),
    ("POST /course/modify", test_course_modify),
    ("POST /csv", test_csv_export),
    ("POST /csv (invalid json)", test_csv_invalid_json),
    ("POST /scanned", test_scanned_post),
    ("GET /scanned (no body)", test_scanned_get),
    ("PUT /scanned", test_scanned_put),
    ("POST /scanned (missing body)", test_scanned_missing_body),
]


def run_all_tests():
    total = len(TESTS)
    results = []

    print("=" * 60)
    print("  AutomatApp – API Endpoint Tests")
    print("=" * 60)
    print()

    try:
        print("⏳ Setting up test data (teacher + student) ...")
        setup_test_data()
        print("✔  Setup complete.")
        seed_test_data()
    except Exception as e:
        print(f"✗  Setup FAILED: {e}")
        print("   Cannot proceed without test data. Aborting.")
        return

    print()

    bar = ProgressBar(total)

    for name, fn in TESTS:
        bar.set_current(name)
        passed = False
        error = None
        try:
            fn()
            passed = True
        except AssertionError as e:
            error = str(e)
        except requests.exceptions.ConnectionError as e:
            error = f"Connection error: {e}"
        except Exception as e:
            error = f"{type(e).__name__}: {e}"

        results.append((name, passed, error))

        if passed:
            print(f"  \033[32m✓\033[0m {name}")
        else:
            print(f"  \033[31m✗\033[0m {name}  —  {error}")

        bar.update(name, passed)

    bar.finish()

    print()
    print("⏳ Tearing down test data ...")
    teardown_test_data()
    print("✔  Teardown complete.")
    answer = input("\n🗑  Delete seeded data? [y/N]: ").strip().lower()
    if answer in ("y", "yes"):
        wipe_test_data()
    else:
        print("Seeded data kept in database.")

    passed_count = sum(1 for _, p, _ in results if p)
    failed_count = sum(1 for _, p, _ in results if not p)

    print()
    print("=" * 60)
    print(f"  Results: \033[32m{passed_count} passed\033[0m, \033[31m{failed_count} failed\033[0m  (total {total})")
    print("=" * 60)

    if failed_count:
        print()
        print("  Failed tests:")
        for name, p, err in results:
            if not p:
                print(f"    \033[31m✗\033[0m {name}")
                print(f"      {err}")
        print()

    raise SystemExit(1 if failed_count else 0)


if __name__ == "__main__":
    run_all_tests()
