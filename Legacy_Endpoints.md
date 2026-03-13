# Legacy Endpoint Aliases

These endpoints are preserved for backward compatibility. They behave identically to their current equivalents. New integrations should use the current endpoints documented in Documentation.md.

### Teacher
| Legacy Endpoint | Method | Current Endpoint | Notes |
| :--- | :--- | :--- | :--- |
| POST /teacher/addTeacher | POST | /teacher/add | |
| GET /teacher/allTeachers | GET | /teacher/all | |
| DELETE /teacher/deleteTeacher | DELETE | /teacher/delete | |
| POST /teacher/modifyTeacher | POST | /teacher/modify | |

### Student
| Legacy Endpoint | Method | Current Endpoint | Notes |
| :--- | :--- | :--- | :--- |
| POST /student/addStudent | POST | /student/add | |
| GET /student/allStudents | GET | /student/all | |
| DELETE /student/deleteStudent | DELETE | /student/delete | |
| POST /student/modifyStudent | POST | /student/modify | |

### Course
| Legacy Endpoint | Method | Current Endpoint | Notes |
| :--- | :--- | :--- | :--- |
| POST /course/addCourse | POST | /course/add | |
| GET /course/allCourses | GET | /course/all | |
| DELETE /course/deleteCourse | DELETE | /course/delete | |
| POST /course/modifyCourse | POST | /course/modify | |

All legacy endpoints accept the same request bodies and return the same responses as their current counterparts. Authentication requirements are identical.
