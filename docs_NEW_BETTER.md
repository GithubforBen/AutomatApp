## AutomatApp HTTP API — Expanded Documentation

This document provides an authoritative, expanded description of all HTTP endpoints implemented by the AutomatApp Java
server, along with the external HTTP requests that the desktop application itself expects to call. It is based on the
actual handler implementations in the codebase under `src/main/java/de/schnorrenbergers/automat/server/handler` and the
routes registered in `Server.java`.

Unless otherwise stated, the server listens on port 8000.

### Common Status Codes

- 200 OK — Request was successful
- 400 Bad Request — Missing or invalid data (e.g., resource not found, invalid ID)
- 404 Not Found — Resource or route not found
- 405 Method Not Allowed — Wrong HTTP method used
- 409 Conflict — Conflict with current state
- 410 JSON Error — Server couldn’t parse JSON body

### Response Body Conventions

- Text responses: Many endpoints reply with text/plain carrying a short message.
- JSON responses: Listing endpoints and detailed data endpoints return application/json.
- CSV responses: The export endpoint returns text/plain with semicolon-separated values.
- Standard error payloads from CustomHandler:
    - 400 → "BAD REQUEST: 400"
    - 405 → "Method not allowed"
    - 410 → "Can't parse JSON"

## Server Endpoints (Inbound)

The following endpoints are registered in `Server.java` via `server.createContext()` and are backed by concrete
`HttpHandler` implementations.

### 1) POST /login

Handler: LoginHandler

- Request (application/json):
  {
  "rfid": [int, int, int, int, ...] // raw UID bytes of the card
  }

- Responses:
    - 200 application/json
      {
      "cameIn": boolean, // true if this call registered an entry, false if it registered an exit
      "time": number, // current account balance (Kontostand)
      "name": string // user's full name
      }
    - 405 text/plain
      "Method not allowed" (non-POST)
    - 410 text/plain
      "Can't parse JSON"
    - 501 text/plain
      "There is no user associated with this rfid card. Please register one or start crying." (user not found)

- Long description:
  Authenticates a user by RFID card. The handler expects the raw RFID UID as an integer array, looks up the associated
  user, toggles their attendance state via LoginManager, and responds with a JSON object containing whether the user
  just came in or left, their current balance, and their full name. Use POST only; other methods return 405. Invalid
  JSON yields 410. If no user is associated with the RFID, a 501 response is returned with a descriptive message.

### 2) POST /scanned

Handler: ScannedHandler

- Request (application/json):
  {
  "rfid": [int, int, int, int, ...]  // raw UID bytes from the card reader
  }

- Responses:
    - 200 text/plain
      "success"
    - 410 text/plain
      "Can't parse JSON"

- Long description:
  Receives a raw RFID scan from external hardware and stores it in memory as the “last scanned card”. This is used by
  the UI flow to identify the requesting user before dispensing. The server does not validate the RFID here; it simply
  caches it. Subsequent operations (e.g., balance check and dispense) read this cached value. Make sure to send an int
  array (not strings). Any JSON parsing issue results in 410.

### 3) GET /energetics

Handler: SchonenderHandler

- Request: no body required
- Responses:
    - 200 text/plain
      "200"

- Long description:
  Utility endpoint used to interact with the screen saver logic. A successful call resets the screen saver timeout by
  setting its last activity timestamp. The handler doesn’t enforce a specific HTTP method, but GET is recommended. No
  authentication or payload expected.

### 4) GET /          (root)

Handler: IndexHandler

- Request: no body required
- Responses:
    - 200 text/html
      Embedded index.html content

- Long description:
  Serves the main HTML landing page that ships with the application. Content-type is set to text/html. Useful as a quick
  connectivity and resource sanity check.

### 5) POST /addTeacher

Handler: AddTeatcherHandler

- Request (application/json):
  {
  "firstName": string,
  "lastName": string,
  "rfid": [int, ...],
  "gender": "MALE"|"FEMALE"|"OTHER",
  "birthday": long, // epoch millis
  "address": {
  "nr": int,
  "street": string,
  "city": string,
  "zip": int,
  "country": string
  },
  "email": string,
  "password": string,
  "level": "ADMIN"|"TEACHER"|... // Level enum
  }

- Responses:
    - 200 text/plain
      "Successfully added teacher"
    - 405 text/plain
      "Method not allowed" (non-POST)
    - 410 text/plain
      "Can't parse JSON"

- Long description:
  Creates a teacher together with their address (Wohnort). The handler persists the address first, then the teacher
  within one transaction. Any parsing or validation error results in 410 (JSON error) or 400 (bad request for obviously
  invalid fields). Only POST is accepted.

### 6) POST /addStudent

Handler: AddStudentHandler

- Request (application/json):
  {
  "firstName": string,
  "lastName": string,
  "rfid": [int, ...],
  "gender": "MALE"|"FEMALE"|"OTHER",
  "birthday": long, // epoch millis
  "address": {
  "nr": int,
  "street": string,
  "city": string,
  "zip": int,
  "country": string
  },
  "kurse": [string|number, ...]    // course IDs; looked up from DB
  }

- Responses:
    - 200 text/plain
      "Successfully added student"
    - 405 text/plain
      "Method not allowed" (non-POST)
    - 410 text/plain
      "Can't parse JSON"

- Long description:
  Persists a new student and their address. For each course ID provided in "kurse", the handler loads the course entity
  and associates it. Transactions ensure both address and student are stored. Use POST only; JSON errors -> 410; invalid
  references -> 400/409 depending on the failure.

### 7) POST /addCourse

Handler: AddCourseHandler

- Request (application/json):
  {
  "name": string,
  "tutor": [string|number, ...], // teacher IDs
  "day": "MONDAY"|"TUESDAY"|... // Day enum
  }

- Responses:
    - 200 text/plain
      "Successfully added course: {Course.toString()}"
    - 405 text/plain
      "Method not allowed" (non-POST)
    - 410 text/plain
      "Can't parse JSON"
    - Server error (uncaught exception) if duplicate is detected: runtime error with message "The Course already
      exists!" (HTTP code depends on container; not standardized here)

- Long description:
  Creates a new course (Kurs) with a list of tutors (teachers) and a scheduled day. The handler persists the new course
  and immediately re-queries by name+day to confirm uniqueness. If multiple matches are found, a runtime exception is
  thrown and surfaced as an error. Only POST is supported; malformed JSON returns 410.

### 8) GET /genders

Handler: GenderHandler

- Request: no body
- Responses:
    - 200 text/plain
      Newline-separated Gender enum values

- Long description:
  Utility discovery endpoint for client-side forms. Returns all possible values of the Gender enum, one per line. No
  method check is enforced but GET is recommended.

### 9) GET /days

Handler: DayHandler

- Request: no body
- Responses:
    - 200 text/plain
      Newline-separated Day enum values (in English names)

- Long description:
  Utility discovery endpoint for course scheduling forms. Returns all Day enum constants, one per line. No strict method
  enforcement, but use GET.

### 10) GET /allStudents

Handler: GetAllStudentsHandler

- Request: no body
- Responses:
    - 200 application/json
      {
      "students": [ Student.json, ... ]
      }

- Long description:
  Returns every Student in the database encoded via each entity’s `toJSON()` method. The list is empty if there are no
  records. The handler doesn’t validate method, but this endpoint is intended for GET. Outputs a compact JSON string.

### 11) GET /allTeachers

Handler: GetAllTeatchersHandler

- Request: no body
- Responses:
    - 200 application/json
      {
      "teachers": [ Teacher.json, ... ]
      }

- Long description:
  Returns all Teacher entities. On an empty database, the handler seeds a sample teacher for demonstration and then
  returns the list. Intended for GET. Output key is "teachers".

### 12) GET /allCourses

Handler: GetAllCoursesHandler

- Request: no body
- Responses:
    - 200 application/json
      {
      "courses": [ Kurs.json, ... ]
      }

- Long description:
  Returns all courses with their associated tutors as produced by each Kurs entity’s `toJSON()` method. Intended for
  GET.

### 13) DELETE /deleteStudent

Handler: DeleteStudentHandler

- Request (application/json):
  { "id": long }

- Responses:
    - 200 text/plain
      "Successfully deleted Student"
    - 400 text/plain
      "BAD REQUEST: 400" (invalid or non-existent ID)
    - 405 text/plain
      "Method not allowed" (non-DELETE)
    - 410 text/plain
      "Can't parse JSON"

- Long description:
  Deletes a student by ID. Validates the method (DELETE), parses JSON, checks existence, and removes the entity in a
  transaction. Missing/invalid JSON yields 410; non-existing ID yields 400.

### 14) DELETE /deleteTeacher

Handler: DeleteTeacherHandler

- Request (application/json):
  { "id": long }

- Responses:
    - 200 text/plain
      "Successfully deleted teacher"
    - 400 text/plain
      "BAD REQUEST: 400" (invalid or non-existent ID)
    - 405 text/plain
      "Method not allowed" (non-DELETE)
    - 410 text/plain
      "Can't parse JSON"

- Long description:
  Deletes a teacher by ID, with the same validation and error handling approach as DeleteStudentHandler. Only DELETE is
  accepted.

### 15) DELETE /deleteCourse

Handler: DeleteCourseHandler

- Request (application/json):
  { "id": long }

- Responses:
    - 200 text/plain
      "Successfully deleted Kurs"
    - 400 text/plain
      "BAD REQUEST: 400" (invalid or non-existent ID)
    - 405 text/plain
      "Method not allowed" (non-DELETE)
    - 410 text/plain
      "Can't parse JSON"

- Long description:
  Deletes a course (Kurs) by ID. Only DELETE is accepted. 410 for JSON errors; 400 if the course doesn’t exist.

### 16) POST /modifyStudent

Handler: ModifyStudentHandler

- Request (application/json):
  {
  "id": long, // required
  "gender"?: Gender,
  "firstName"?: string,
  "lastName"?: string,
  "rfid"?: [int, ...],
  "birthday"?: long,
  "wohnort"?: Wohnort.json,
  "kurse"?: [ Kurs.json, ... ]
  }

- Responses:
    - 400 text/plain
      "BAD REQUEST: 400" (invalid/missing ID)
    - 405 text/plain
      "Method not allowed" (non-POST)
    - 410 text/plain
      "Can't parse JSON"
    - 200 text/plain (not currently sent)
      Note: The current implementation prepares updates but does not finalize the transaction/response. Treat as
      experimental.

- Long description:
  Intended to patch Student fields by ID. The handler loads the current Student, applies any provided field updates, and
  was meant to merge the entity. The final transaction/response is currently incomplete and commented out; expect
  changes or extend as needed if you plan to rely on it. Only POST is accepted; 400 if ID invalid/missing.

### 17) POST /modifyTeacher

Handler: ModifyTeatcherHandler

- Request (application/json): Teacher.json

- Responses:
    - 200 text/plain
      "Successfully added teacher" (also for updates)
    - 405 text/plain
      "Method not allowed" (non-POST)
    - 410 text/plain
      "Can't parse JSON"

- Long description:
  Upserts a Teacher from its JSON representation. If the provided ID doesn’t exist, the handler persists a new record (
  including the address). If it exists and values differ, it merges changes for both the address and the teacher. Only
  POST is accepted. 410 for JSON errors. Note: Response text says “added teacher” even for updates.

### 18) POST /modifyCourse

Handler: ModifyCourseHandler

- Request (application/json):
  {
  "id": long, // required
  "name"?: string,
  "day"?: Day,
  "addTeacher"?: long, // teacher ID
  "removeTeacher"?: long // teacher ID
  }

- Responses:
    - 200 text/plain
      "Successfully deleted Kurs" (message text is misleading; it actually updates)
    - 400 text/plain
      "BAD REQUEST: 400" (invalid/missing course/teacher IDs)
    - 405 text/plain
      "Method not allowed" (non-POST)
    - 410 text/plain
      "Can't parse JSON"

- Long description:
  Patches selected fields of a course by ID. Optionally add or remove a teacher (by teacher ID). A transaction merges
  the updated state. Only POST is accepted. Returns 400 for invalid IDs.

### 19) POST /csv

Handler: GetCSVHandler

- Request (application/json):
  { "kurs": long } // pass -1 for “all courses” aggregate

- Responses:
    - 200 text/plain (CSV; semicolon-separated)
      Example:
      Vorname;Nachname;1/10/2025;8/10/2025
      Anna;Muster;Anwesend;Nicht anwesend
    - 405 text/plain
      "Method not allowed" (non-POST)
    - 410 text/plain
      "Can't parse JSON"

- Long description:
  Exports attendance statistics for all students enrolled in the selected course. Dates are aggregated from Statistic
  entries of type 'STUDENT_ATTEND_STATIC'. Each subsequent row has the student’s first and last name followed by
  per-date attendance markers (“Anwesend” or “Nicht anwesend”). Use POST only.

### 20) POST /attendances

Handler: UserAttandanceHandler

- Request (application/json):
  { "id": long } // internal user ID

- Responses:
    - 200 application/json
      {
      "attendances": [
      { "day": 1, "month": 10, "year": 2025, "type": "STUDENT_ATTEND_STATIC" }
      ]
      }
    - 400 text/plain
      "BAD REQUEST: 400" (invalid/missing ID)
    - 405 text/plain
      "Method not allowed" (non-POST)
    - 410 text/plain
      "Can't parse JSON"

- Long description:
  Returns the persisted attendance entries for the user’s account (Konto) behind the given internal ID. Each entry
  includes day, month, year, and a type enum indicating the attendance kind. Only POST is accepted. Invalid IDs result
  in 400; JSON parsing failures result in 410.

## Expected External Requests (Outbound)

The desktop application initiates HTTP requests to an external device/service using `CustomRequest`. These endpoints are
expected to exist in that external service (see `apiDummy/dummy.py` for a reference implementation). They are not served
by this Java server.

From code references in Main, MainController, AdminController, and HelloController:

1) GET /ping
    - Purpose: Connectivity/health check. The app uses it to decide whether to proceed with normal UI or fall back to a
      reconnect screen.
    - Expected response: A short truthy payload (e.g., "1").

2) GET /alarm_off
    - Purpose: Disables the external alarm/buzzer/indicator when the app initializes or when toggled from the admin
      screen.
    - Expected response: "success"

3) GET /alarm_on
    - Purpose: Enables the external alarm; used when toggling from the admin screen.
    - Expected response: "success"

4) POST /dispense
    - Body (application/json):
      { "nr": int, "cost": int, "usr": [int, ...] }
      where nr is the slot/index (0–7), cost is the configured price in “hours”, and usr is the RFID byte array of the
      user who requested dispensing.
    - Purpose: Commands the external machine to dispense an item from the specified compartment.
    - Expected response: "success" on successful dispense, otherwise an error code string (e.g., "error: 235").

5) POST /re-enable
    - Body (application/json):
      { "name": string }
      where name is the configured sweet’s name to re-enable/mark available.
    - Purpose: Re-enables a compartment or item line after it was marked unavailable.
    - Expected response: Free text confirming the action (e.g., "Added: <name>").

Notes:

- All these requests are constructed via `CustomRequest`. `execute()` issues a GET; `executeComplex(json)` issues a POST
  with JSON content-type.
- Timeouts are set to ~1000 ms; failure causes the app to switch to the reconnect screen.

## Implementation Notes and Gotchas

- Method checks: Many read-only endpoints do not strictly check HTTP method and will respond to any method; use the
  documented method for best compatibility.
- JSON parsing: All JSON parsing is done with org.json. Invalid JSON triggers a 410 via CustomHandler.jsonError().
- Entities and JSON: Student, Teacher, and Kurs provide `toJSON()` helpers used by the listing endpoints.
- Modify endpoints: Some response messages are misleading (e.g., modifyCourse responds with "Successfully deleted
  Kurs"). Treat them as implementation quirks rather than the intended behavior.
- Attendance export: CSV endpoint aggregates attendance by date strings built from Java Date fields; consider the legacy
  Date semantics (day/month/year offsets) if you post-process the CSV.

## Changelog (Docs)

- 2025-11-21: First expanded version created as docs_NEW_BETTER.md; includes comprehensive long descriptions and an
  Expected External Requests section.
