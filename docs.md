## General Information

Important note

This document is kept for reference. A newer, expanded, and authoritative API document is available in
docs_NEW_BETTER.md. It includes long-form descriptions for every endpoint and a dedicated section listing the external
requests the desktop app sends (Expected External Requests).

Highlights of changes in the new docs:

- Clarifies that POST /login expects an object with id (long) and time (epoch millis), not an RFID array.
- Documents the POST /attendances endpoint for fetching a user’s attendance entries.
- Adds a full “Expected External Requests” section covering /ping, /alarm_on, /alarm_off, /dispense, and /re-enable used
  by the app via CustomRequest.

This documentation covers all HTTP API endpoints in the AutomatApp project. The API serves various functionalities
including user management, course administration, and authentication.

## Error Codes

| Status Code | Description                                                                          |
|-------------|--------------------------------------------------------------------------------------|
| 200         | OK - Request was successful                                                          |
| 400         | Bad Request - The request could not be understood or was missing required parameters |
| 404         | Not Found - The requested resource could not be found                                |
| 405         | Method Not Allowed - The HTTP method is not supported for this endpoint              |
| 409         | Conflict - The request could not be completed due to a conflict with current state   |
| 410         | JSON Error - Could not parse JSON in the request                                     |

## Endpoints

### 1. Login Endpoint

**URL:** `/login`

**Method:** POST

**Input:**

```json
{
  "rfid": [1, 2, 3, 4]
}
```

- `rfid`: An array of integers representing the RFID card data

**Output:**

- Success: "User came in" (when user enters)
- Success: "User left" (when user exits)

**Description:** Handles user login/logout based on RFID card scanning. Records attendance time for billing purposes.

---

### 2. Add Teacher Endpoint

**URL:** `/addTeacher`

**Method:** POST

**Input:**

```json
{
  "firstName": "John",
  "lastName": "Doe",
  "rfid": [1, 2, 3, 4],
  "gender": "MALE",
  "birthday": 1609459200000,
  "address": {
    "nr": 123,
    "street": "Main Street",
    "city": "Berlin",
    "zip": 10115,
    "country": "Germany"
  },
  "email": "john.doe@example.com",
  "password": "securePassword123",
  "level": "ADMIN"
}
```

**Output:**

- Success: "Successfully added teacher"
- Error: "Can't parse JSON"

**Description:** Registers a new teacher with personal information, address, and authentication details.

---

### 3. Add Student Endpoint

**URL:** `/addStudent`

**Method:** POST

**Input:**

```json
{
  "firstName": "Jane",
  "lastName": "Smith",
  "rfid": [5, 6, 7, 8],
  "gender": "FEMALE",
  "birthday": 1609459200000,
  "address": {
    "nr": 456,
    "street": "School Avenue",
    "city": "Berlin",
    "zip": 10115,
    "country": "Germany"
  },
  "kurse": ["1", "2", "3"]
}
```

**Output:**

- Success: "Successfully added student"
- Error: "Can't parse JSON"

**Description:** Registers a new student with personal information, address, and their assigned courses.

---

### 4. Add Course Endpoint

**URL:** `/addCourse`

**Method:** POST

**Input:**

```json
{
  "name": "Mathematics 101",
  "tutor": ["1", "2"],
  "day": "MONDAY"
}
```

**Output:**

- Success: "Successfully added course: {course_details}"
- Error: "The Course already exists!"
- Error: "Can't parse JSON"

**Description:** Creates a new course with name, assigned tutors, and scheduled day of the week.

---

### 5. Get All Students Endpoint

**URL:** `/allStudents`

**Method:** GET

**Input:** None

**Output:**

```json
{
  "students": [
    {
      "id": 1,
      "firstName": "Jane",
      "lastName": "Smith",
      "rfid": [5, 6, 7, 8],
      "gender": "FEMALE",
      "birthday": 1609459200000,
      "wohnort": {
        "id": 1,
        "nr": 456,
        "street": "School Avenue",
        "city": "Berlin",
        "zip": 10115,
        "country": "Germany"
      }
    }
  ]
}
```

- Or "No Students found" if no students are registered

**Description:** Retrieves a complete list of all students in the system with their details.

---

### 6. Get All Teachers Endpoint

**URL:** `/allTeachers`

**Method:** GET

**Input:** None

**Output:**

```json
{
  "users": [
    {
      "id": 1,
      "firstName": "John",
      "lastName": "Doe",
      "rfid": [1, 2, 3, 4],
      "gender": "MALE",
      "birthday": 1609459200000,
      "wohnort": {
        "id": 1,
        "nr": 123,
        "street": "Main Street",
        "city": "Berlin",
        "zip": 10115,
        "country": "Germany"
      },
      "mail": "john.doe@example.com",
      "password": "encryptedPassword123",
      "level": "ADMIN"
    }
  ]
}
```

- Or "No Teachers found" if no teachers are registered

**Description:** Retrieves a complete list of all teachers in the system with their details.

---

### 7. Get All Courses Endpoint

**URL:** `/allCourses`

**Method:** GET

**Input:** None

**Output:**

```json
{
  "courses": [
    {
      "id": 1,
      "name": "Mathematics 101",
      "day": "MONDAY",
      "tutor": [
        {
          "id": 1,
          "firstName": "John",
          "lastName": "Doe",
          "rfid": [1, 2, 3, 4],
          "gender": "MALE",
          "birthday": 1609459200000,
          "wohnort": {
            "id": 1,
            "nr": 123,
            "street": "Main Street",
            "city": "Berlin",
            "zip": 10115,
            "country": "Germany"
          },
          "mail": "john.doe@example.com",
          "password": "encryptedPassword123",
          "level": "ADMIN"
        }
      ]
    }
  ]
}
```

- Or empty array if no courses are registered

**Description:** Retrieves a complete list of all courses in the system with their details.

---

### 8. Scanned Card Endpoint

**URL:** `/scanned`

**Method:** POST

**Input:**

```json
{
  "rfid": [1, 2, 3, 4]
}
```

**Output:**

- Success: "success"

**Description:** Handles raw RFID card scan data from card readers, setting the last scanned card in the system. (
Automat login)

---

### 9. Screen Saver Control Endpoint

**URL:** `/energetics`

**Method:** GET

**Input:** None

**Output:**

- Success: "200"

**Description:** Resets the screen saver timer, preventing activation or disabling an active screen saver.

---

### 10. Gender List Endpoint

**URL:** `/genders`

**Method:** GET

**Input:** None

**Output:** A newline-separated list of all available gender types in the system.

**Description:** Provides all supported gender options for user registration forms.

---

### 11. Day List Endpoint

**URL:** `/days`

**Method:** GET

**Input:** None

**Output:** A newline-separated list of all days of the week (in German).

**Description:** Provides all supported days of the week for course scheduling.

---

### 12. Delete Student Endpoint

**URL:** `/deleteStudent`

**Method:** DELETE

**Input:**

```json
{
  "id": 1
}
```

**Output:**

- Success: "Successfully deleted Student"
- Error: "Can't parse JSON"
- Error: "Bad Request" (if student ID doesn't exist)

**Description:** Deletes a student from the system based on the provided ID.

---

### 13. Delete Teacher Endpoint

**URL:** `/deleteTeacher`

**Method:** DELETE

**Input:**

```json
{
  "id": 1
}
```

**Output:**

- Success: "Successfully deleted teacher"
- Error: "Can't parse JSON"
- Error: "Bad Request" (if teacher ID doesn't exist)

**Description:** Deletes a teacher from the system based on the provided ID.

---

### 14. Delete Course Endpoint

**URL:** `/deleteCourse`

**Method:** DELETE

**Input:**

```json
{
  "id": 1
}
```

**Output:**

- Success: "Successfully deleted Kurs"
- Error: "Can't parse JSON"
- Error: "Bad Request" (if course ID doesn't exist)

**Description:** Deletes a course from the system based on the provided ID.

---

### 15. Modify Student Endpoint

**URL:** `/modifyStudent`

**Method:** POST

**Input:**

```json
{
  "id": 1,
  "gender": "MALE"
}
```

**Output:**

- Success: "Successfully deleted Kurs" (Note: This appears to be an incorrect message in the implementation)
- Error: "Can't parse JSON"
- Error: "Bad Request" (if student ID doesn't exist)

**Description:** Updates student information based on the provided data. Note: This endpoint appears to be incomplete in
the implementation.

---

### 16. Modify Teacher Endpoint

**URL:** `/modifyTeacher`

**Method:** POST

**Input:**

```json
{
  "id": 1,
  "firstName": "John",
  "lastName": "Doe",
  "rfid": [1, 2, 3, 4],
  "gender": "MALE",
  "birthday": 1609459200000,
  "address": {
    "id": 1,
    "nr": 123,
    "street": "Main Street",
    "city": "Berlin",
    "zip": 10115,
    "country": "Germany"
  },
  "email": "john.doe@example.com",
  "password": "securePassword123",
  "level": "ADMIN"
}
```

**Output:**

- Success: "Successfully added teacher" (Note: This appears to be an incorrect message in the implementation)
- Error: "Can't parse JSON"

**Description:** Updates teacher information or creates a new teacher if the ID doesn't exist.

---

### 17. Modify Course Endpoint

**URL:** `/modifyCourse`

**Method:** POST

**Input:**

```json
{
  "id": 1,
  "name": "Advanced Mathematics",
  "day": "TUESDAY",
  "addTeacher": "2",
  "removeTeacher": "3"
}
```

**Output:**

- Success: "Successfully deleted Kurs" (Note: This appears to be an incorrect message in the implementation)
- Error: "Can't parse JSON"
- Error: "Bad Request" (if course ID or teacher ID doesn't exist)

**Description:** Updates course information based on the provided data. Can modify the name, day, and add or remove
teachers.

---

### 18. CSV Export Endpoint

**URL:** `/csv`

**Method:** POST

**Input:**

```json
{
  "kurs": 1
}
```

**Output:** CSV data with student attendance information.

**Description:** Generates a CSV file with attendance data for students in the specified course. The CSV includes
student names and attendance dates, with each row representing a student and each column representing a date.

---

### 19. Index Endpoint

**URL:** `/`

**Method:** GET

**Input:** None

**Output:** HTML content of the main application page.

**Description:** Serves the main HTML interface for the application.

## Additional Information

- All responses with 200 OK status include a text message indicating success or the requested data.
- JSON parsing errors result in a 410 status code with message "Can't parse JSON".
- Documentation completely vibecoded with Claude Code
- Method not allowed errors return a 405 status code with message "Method not allowed".
- The API operates on port 8000 by default.
- Authentication is handled via RFID scanning rather than traditional username/password.