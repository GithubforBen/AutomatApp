import base64
import hashlib
import hmac
import json
import requests
import secrets
import time

# Configuration
BASE_URL = "http://localhost:8000"
KEY_ID = "pwop"  # Replace with your actual HMAC token ID from database
SECRET = "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"  # Full SECRET from hmac_test.py


def generate_hmac_signature(key_id, timestamp, nonce, secret, body=""):
    """Generate HMAC-SHA256 signature"""
    data = f"{key_id}:{timestamp}:{nonce}:{body}"
    signature = hmac.new(
        secret.encode('utf-8'),
        data.encode('utf-8'),
        hashlib.sha256
    ).digest()
    return base64.b64encode(signature).decode('utf-8')


# Global offset for time sync
TIME_OFFSET = 0


def sync_time():
    """Attempt to sync time with server"""
    global TIME_OFFSET
    print("\n--- Syncing time with server ---")
    try:
        # Send a request with invalid headers just to get server's current time from error response
        url = f"{BASE_URL}/"
        response = requests.get(url)
        if response.status_code == 401:
            try:
                server_time_str = response.json().get('timestamp')
                if server_time_str:
                    # Parse server time "2026-02-27T15:13:14.358Z"
                    import datetime
                    # Strip Z and nanoseconds if present
                    t_parts = server_time_str.replace('Z', '').split('.')
                    t_str = t_parts[0]

                    # Use UTC for parsing to avoid local timezone issues
                    server_dt = datetime.datetime.strptime(t_str, "%Y-%m-%dT%H:%M:%S").replace(
                        tzinfo=datetime.timezone.utc)
                    server_ts = int(server_dt.timestamp())
                    client_ts = int(time.time())
                    TIME_OFFSET = server_ts - client_ts
                    print(f"Server time: {server_time_str} ({server_ts})")
                    print(f"Client time: {client_ts}")
                    print(f"Offset: {TIME_OFFSET}")
            except Exception as e:
                print(f"Failed to parse server time: {e}")
    except Exception as e:
        print(f"Failed to sync time: {e}")


def send_authenticated_request(method, endpoint, body=None):
    """Send authenticated HMAC request and verify not unauthorized"""
    url = f"{BASE_URL}{endpoint}"
    # Use synced time
    timestamp = str(int(time.time()) + TIME_OFFSET)
    nonce = secrets.token_hex(16)

    body_str = json.dumps(body, separators=(',', ':')) if body is not None else ""

    signature = generate_hmac_signature(KEY_ID, timestamp, nonce, SECRET, body_str)

    headers = {
        "X-API-KEY-ID": KEY_ID,
        "X-TIMESTAMP": timestamp,
        "X-NONCE": nonce,
        "X-SIGNATURE": signature,
        "Content-Type": "application/json"
    }

    if method.upper() == "GET":
        response = requests.get(url, headers=headers)
    elif method.upper() == "POST":
        response = requests.post(url, headers=headers, data=body_str)
    else:
        raise ValueError(f"Unsupported method: {method}")

    assert response.status_code != 401, f"Unauthorized access to {endpoint} (401)"
    return response


def test_index():
    """Test GET / - Index endpoint"""
    print("\n--- Testing GET / (Index) ---")
    response = send_authenticated_request("GET", "/")
    print(f"Status: {response.status_code}")
    print(f"Content-Type: {response.headers.get('Content-Type')}")
    print(f"Body length: {len(response.text)} bytes")
    assert response.status_code == 200, "Index should return 200"
    assert "text/html" in response.headers.get('Content-Type', ''), "Index should return HTML"


def test_energetics():
    """Test GET/POST /energetics - Energetics endpoint"""
    print("\n--- Testing GET /energetics ---")
    response = send_authenticated_request("GET", "/energetics")
    print(f"Status: {response.status_code}")
    print(f"Body: {response.text}")
    assert response.status_code == 200, "Energetics GET should return 200"
    assert response.text == "200", "Energetics should return '200'"

    print("\n--- Testing POST /energetics ---")
    response = send_authenticated_request("POST", "/energetics")
    print(f"Status: {response.status_code}")
    print(f"Body: {response.text}")
    assert response.status_code == 200, "Energetics POST should return 200"


def test_login():
    """Test POST /login - Login endpoint"""
    print("\n--- Testing POST /login ---")
    data = {
        "rfid": [100, 100, 100, 100]
    }
    response = send_authenticated_request("POST", "/login", data)
    print(f"Status: {response.status_code}")
    print(f"Body: {response.text}")

    if response.status_code == 200:
        json_response = response.json()
        print(f"Parsed JSON: {json.dumps(json_response, indent=2)}")
        assert "cameIn" in json_response, "Response should contain 'cameIn'"
        assert "time" in json_response, "Response should contain 'time'"
        assert "name" in json_response, "Response should contain 'name'"
        assert "text" in json_response, "Response should contain 'text'"
    elif response.status_code == 501:
        print("No user associated with RFID (expected if no data)")


def test_attendances():
    """Test POST /attendances - Get attendances for user"""
    print("\n--- Testing POST /attendances ---")
    data = {"id": 1}
    response = send_authenticated_request("POST", "/attendances", data)
    print(f"Status: {response.status_code}")
    print(f"Body: {response.text}")

    if response.status_code == 200:
        json_response = response.json()
        print(f"Parsed JSON: {json.dumps(json_response, indent=2)}")
        assert "attendances" in json_response, "Response should contain 'attendances'"


def test_teacher_all():
    """Test GET /teacher/all - Get all teachers"""
    print("\n--- Testing GET /teacher/all ---")
    response = send_authenticated_request("GET", "/teacher/all")
    print(f"Status: {response.status_code}")
    print(f"Body: {response.text[:200]}...")

    if response.status_code == 200:
        json_response = response.json()
        assert "teachers" in json_response, "Response should contain 'teachers'"
        print(f"Number of teachers: {len(json_response['teachers'])}")


def test_teacher_add():
    """Test POST /teacher/add - Add a teacher"""
    print("\n--- Testing POST /teacher/add ---")
    data = {
        "firstName": "Jane",
        "lastName": "Smith",
        "rfid": [200, 200, 200, 200],
        "gender": "FEMALE",
        "birthday": 946684800000,  # 2000-01-01 in milliseconds
        "address": {
            "nr": 42,
            "street": "Main Street",
            "city": "Springfield",
            "zip": 12345,
            "country": "USA"
        },
        "email": "jane.smith@school.com",
        "password": "password123",
        "level": "TEACHER"
    }
    response = send_authenticated_request("POST", "/teacher/add", data)
    print(f"Status: {response.status_code}")
    print(f"Body: {response.text}")


def test_student_all():
    """Test GET /student/all - Get all students"""
    print("\n--- Testing GET /student/all ---")
    response = send_authenticated_request("GET", "/student/all")
    print(f"Status: {response.status_code}")
    print(f"Body: {response.text[:200]}...")

    if response.status_code == 200:
        json_response = response.json()
        assert "students" in json_response, "Response should contain 'students'"
        print(f"Number of students: {len(json_response['students'])}")


def test_student_add():
    """Test POST /student/add - Add a student"""
    print("\n--- Testing POST /student/add ---")
    data = {
        "firstName": "John",
        "lastName": "Doe",
        "gender": "MALE",
        "birthday": 1009843200000,  # 2002-01-01 in milliseconds
        "address": {
            "nr": 10,
            "street": "Student Ave",
            "city": "Testville",
            "zip": 54321,
            "country": "Germany"
        },
        "kurse": []
    }
    response = send_authenticated_request("POST", "/student/add", data)
    print(f"Status: {response.status_code}")
    print(f"Body: {response.text}")


def test_course_all():
    """Test GET /course/all - Get all courses"""
    print("\n--- Testing GET /course/all ---")
    response = send_authenticated_request("GET", "/course/all")
    print(f"Status: {response.status_code}")
    print(f"Body: {response.text[:200]}...")

    if response.status_code == 200:
        json_response = response.json()
        assert "courses" in json_response, "Response should contain 'courses'"
        print(f"Number of courses: {len(json_response['courses'])}")


def test_course_add():
    """Test POST /course/add - Add a course"""
    print("\n--- Testing POST /course/add ---")
    data = {
        "name": "Mathematics 101",
        "tutor": ["1"],  # Assuming teacher with ID 1 exists
        "day": "MONDAY"
    }
    response = send_authenticated_request("POST", "/course/add", data)
    print(f"Status: {response.status_code}")
    print(f"Body: {response.text}")


def test_csv_export():
    """Test POST /csv - Export CSV"""
    print("\n--- Testing POST /csv ---")
    data = {"kurs": -1}  # -1 for all courses
    response = send_authenticated_request("POST", "/csv", data)
    print(f"Status: {response.status_code}")
    print(f"Content-Type: {response.headers.get('Content-Type')}")
    print(f"Body (first 200 chars): {response.text[:200]}")

    if response.status_code == 200:
        assert "text/csv" in response.headers.get('Content-Type', ''), "CSV should return text/csv"


def test_invalid_json():
    """Test endpoints with invalid JSON"""
    print("\n--- Testing POST /login with invalid JSON ---")
    # Custom sending for invalid JSON test
    url = f"{BASE_URL}/login"
    timestamp = str(int(time.time()) + TIME_OFFSET)
    nonce = secrets.token_hex(16)
    body_str = "invalid json"
    signature = generate_hmac_signature(KEY_ID, timestamp, nonce, SECRET, body_str)
    headers = {
        "X-API-KEY-ID": KEY_ID,
        "X-TIMESTAMP": timestamp,
        "X-NONCE": nonce,
        "X-SIGNATURE": signature,
        "Content-Type": "application/json"
    }
    response = requests.post(url, headers=headers, data=body_str)
    print(f"Status: {response.status_code}")
    print(f"Body: {response.text}")
    assert response.status_code != 401, f"HMAC verification failed for /login with invalid JSON: {response.text}"
    assert response.status_code == 410, "Invalid JSON should return 410"


def test_missing_body():
    """Test endpoints with missing body"""
    print("\n--- Testing POST /attendances with no body ---")
    url = f"{BASE_URL}/attendances"
    timestamp = str(int(time.time()) + TIME_OFFSET)
    nonce = secrets.token_hex(16)
    body_str = ""  # No body
    signature = generate_hmac_signature(KEY_ID, timestamp, nonce, SECRET, body_str)
    headers = {
        "X-API-KEY-ID": KEY_ID,
        "X-TIMESTAMP": timestamp,
        "X-NONCE": nonce,
        "X-SIGNATURE": signature,
        "Content-Type": "application/json"
    }
    response = requests.post(url, headers=headers)  # No data parameter
    print(f"Status: {response.status_code}")
    print(f"Body: {response.text}")
    assert response.status_code != 401, f"HMAC verification failed for /attendances with no body: {response.text}"
    assert response.status_code == 410, "Missing body should return 410"


def run_all_tests():
    """Run all test cases"""
    sync_time()
    print("=" * 60)
    print("Starting Spring Boot Endpoint Tests")
    print("=" * 60)

    tests = [
        ("Index", test_index),
        ("Energetics", test_energetics),
        ("Login", test_login),
        ("Attendances", test_attendances),
        ("Teacher All", test_teacher_all),
        ("Teacher Add", test_teacher_add),
        ("Student All", test_student_all),
        ("Student Add", test_student_add),
        ("Course All", test_course_all),
        ("Course Add", test_course_add),
        ("CSV Export", test_csv_export),
        ("Invalid JSON", test_invalid_json),
        ("Missing Body", test_missing_body),
    ]

    passed = 0
    failed = 0

    for name, test_func in tests:
        try:
            test_func()
            passed += 1
            print(f"✓ {name} passed")
        except AssertionError as e:
            failed += 1
            print(f"✗ {name} failed: {e}")
        except requests.exceptions.RequestException as e:
            failed += 1
            print(f"✗ {name} failed: Connection error - {e}")
        except Exception as e:
            failed += 1
            print(f"✗ {name} failed: {e}")

    print("\n" + "=" * 60)
    print(f"Test Results: {passed} passed, {failed} failed")
    print("=" * 60)


if __name__ == "__main__":
    run_all_tests()
