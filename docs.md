## General Information

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
  "rfid": [
    integer_array
  ]
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
  "firstName": "string",
  "lastName": "string",
  "rfid": [
    integer_array
  ],
  "gender": "ENUM_VALUE",
  "birthday": long_timestamp,
  "address": {
    "nr": integer,
    "street": "string",
    "city": "string",
    "zip": integer,
    "country": "string"
  },
  "email": "string",
  "password": "string",
  "level": "ENUM_VALUE"
}
```

**Output:**

- Success: "Successfully added teacher"
- Error: "Can't parse JSON"

**Description:** Adds a new teacher to the system with their personal data, address, and authentication details.

---

### 3. Add Student Endpoint

**URL:** `/addStudent`

**Method:** POST

**Input:**

```json
{
  "firstName": "string",
  "lastName": "string",
  "rfid": [
    integer_array
  ],
  "gender": "ENUM_VALUE",
  "birthday": long_timestamp,
  "address": {
    "nr": integer,
    "street": "string",
    "city": "string",
    "zip": integer,
    "country": "string"
  },
  "kurse": [
    string_array_of_course_ids
  ]
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
  "name": "string",
  "tutor": [
    string_array_of_teacher_ids
  ],
  "day": "DAY_ENUM_VALUE"
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
      student_object
    },
    {
      student_object
    },
    ...
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
      teacher_object
    },
    {
      teacher_object
    },
    ...
  ]
}
```

- Or "No Teachers found" if no teachers are registered

**Description:** Retrieves a complete list of all teachers in the system with their details.

---

### 7. Scanned Card Endpoint

**URL:** `/scanned`

**Method:** POST

**Input:**

```json
{
  "rfid": [
    integer_array
  ]
}
```

**Output:**

- Success: "success"

**Description:** Handles raw RFID card scan data from card readers, setting the last scanned card in the system. (
Automat login)

---

### 8. Screen Saver Control Endpoint

**URL:** `/energetics`

**Method:** GET

**Input:** None

**Output:**

- Success: "200"

**Description:** Resets the screen saver timer, preventing activation or disabling an active screen saver.

---

### 9. Gender List Endpoint

**URL:** `/genders`

**Method:** GET

**Input:** None

**Output:** A newline-separated list of all available gender types in the system.

**Description:** Provides all supported gender options for user registration forms.

---

### 10. Day List Endpoint

**URL:** `/days`

**Method:** GET

**Input:** None

**Output:** A newline-separated list of all days of the week (in German).

**Description:** Provides all supported days of the week for course scheduling.

---

### 11. Index Endpoint

**URL:** `/`

**Method:** GET

**Input:** None

**Output:** HTML content of the main application page.

**Description:** Serves the main HTML interface for the application.

## Additional Information

- All responses with 200 OK status include a text message indicating success or the requested data.
- JSON parsing errors result in a 410 status code with message "Can't parse JSON".
- Method not allowed errors return a 405 status code with message "Method not allowed".
- The API operates on port 8000 by default.
- Authentication is handled via RFID scanning rather than traditional username/password.
