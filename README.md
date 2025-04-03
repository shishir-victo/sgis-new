# Student Attendance System with Facial Recognition

A comprehensive student attendance management system featuring:
- Android mobile application for teachers
- Facial recognition technology to automate attendance marking
- Classroom photo processing to identify present students
- JSON-based storage system for student, class, and attendance data

## Features

- **Facial Recognition**: Automatically detect and mark student attendance from classroom photos
- **Manual Attendance**: Option to manually mark attendance for individual students
- **Attendance Reports**: Generate reports by class, date, or individual student
- **Class Management**: Create and manage multiple class records
- **Student Management**: Add, delete, and view student records with photos

## Technical Details

- **Backend**: Flask REST API (Python)
- **Frontend**: Android native application (Java)
- **Storage**: File-based JSON storage for student, class, and attendance records
- **Authentication**: Simple auth system for teacher login (to be implemented)
- **Image Processing**: Simplified mock facial recognition for the prototype

## Setup Instructions

### Backend

1. Install required packages:
   ```
   pip install flask flask-cors pymysql sqlalchemy numpy
   ```

2. Run the Flask server:
   ```
   python app.py
   ```

3. API will be available at `http://localhost:5000`

### Android App

1. Open the Android project in Android Studio
2. Configure the API endpoint in `RetrofitClient.java` if necessary
3. Build and run the application on an Android device or emulator

## API Endpoints

- GET `/api/students` - Get all students
- POST `/api/students` - Add a new student with photo
- DELETE `/api/students/<student_id>` - Delete a student
- GET `/api/classes` - Get all classes
- POST `/api/classes` - Add a new class
- POST `/api/attendance/take` - Process classroom photo and mark attendance
- POST `/api/attendance/manual` - Manually mark attendance for a student
- GET `/api/attendance/report/<class_id>/<date>` - Get attendance report for a class on a specific date
- GET `/api/attendance/student/<student_id>` - Get attendance report for a specific student

## AWS Deployment

See the following files for AWS deployment information:
- `AWS_DEPLOYMENT_GUIDE.md` - Step-by-step deployment instructions
- `AWS_COST_ESTIMATION.md` - Estimated costs for AWS infrastructure
- `AWS_SECURITY_BEST_PRACTICES.md` - Security recommendations for deployment