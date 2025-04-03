#!/usr/bin/env python3
# Attendance System - Flask API Backend
import os
import logging
import base64
from flask import Flask, request, jsonify, render_template, send_from_directory
from flask_cors import CORS
from models import init_db, Student, Attendance, Class
from face_recognition_service import FaceRecognitionService
from attendance_service import AttendanceService
from utils import save_uploaded_image, decode_base64_image

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Initialize Flask app
app = Flask(__name__)
CORS(app)  # Enable CORS for all routes

# Initialize database
init_db()

# Initialize services
face_recognition_service = FaceRecognitionService()
attendance_service = AttendanceService(face_recognition_service)

# Create necessary directories if they don't exist
os.makedirs('uploads/student_photos', exist_ok=True)
os.makedirs('uploads/classroom_photos', exist_ok=True)

@app.route('/')
def index():
    """Render the main page - useful for testing the API"""
    return render_template('index.html')

@app.route('/api/students', methods=['GET'])
def get_students():
    """Get all students"""
    try:
        students = Student.get_all()
        return jsonify({"success": True, "students": students})
    except Exception as e:
        logger.error(f"Error fetching students: {str(e)}")
        return jsonify({"success": False, "error": str(e)}), 500

@app.route('/api/students', methods=['POST'])
def add_student():
    """Add a new student with photo"""
    try:
        data = request.json
        name = data.get('name')
        student_id = data.get('student_id')
        class_id = data.get('class_id')
        photo_base64 = data.get('photo')
        
        if not all([name, student_id, class_id, photo_base64]):
            return jsonify({"success": False, "error": "Missing required fields"}), 400
        
        # Save the base64 image
        photo_path = decode_base64_image(photo_base64, f"uploads/student_photos/{student_id}.jpg")
        
        # Add student to database
        student = Student.create(name=name, student_id=student_id, class_id=class_id, photo_path=photo_path)
        
        # Update face recognition model
        face_recognition_service.update_student_encodings()
        
        return jsonify({"success": True, "student": student})
    except Exception as e:
        logger.error(f"Error adding student: {str(e)}")
        return jsonify({"success": False, "error": str(e)}), 500

@app.route('/api/students/<student_id>', methods=['DELETE'])
def delete_student(student_id):
    """Delete a student"""
    try:
        success = Student.delete(student_id)
        if success:
            # Update face recognition model
            face_recognition_service.update_student_encodings()
            return jsonify({"success": True})
        return jsonify({"success": False, "error": "Student not found"}), 404
    except Exception as e:
        logger.error(f"Error deleting student: {str(e)}")
        return jsonify({"success": False, "error": str(e)}), 500

@app.route('/api/classes', methods=['GET'])
def get_classes():
    """Get all classes"""
    try:
        classes = Class.get_all()
        return jsonify({"success": True, "classes": classes})
    except Exception as e:
        logger.error(f"Error fetching classes: {str(e)}")
        return jsonify({"success": False, "error": str(e)}), 500

@app.route('/api/classes', methods=['POST'])
def add_class():
    """Add a new class"""
    try:
        data = request.json
        name = data.get('name')
        
        if not name:
            return jsonify({"success": False, "error": "Missing class name"}), 400
        
        class_obj = Class.create(name=name)
        return jsonify({"success": True, "class": class_obj})
    except Exception as e:
        logger.error(f"Error adding class: {str(e)}")
        return jsonify({"success": False, "error": str(e)}), 500

@app.route('/api/take_attendance', methods=['POST'])
def take_attendance():
    """Process classroom photo and mark attendance"""
    try:
        data = request.json
        class_id = data.get('class_id')
        photo_base64 = data.get('photo')
        date = data.get('date')
        
        if not all([class_id, photo_base64, date]):
            return jsonify({"success": False, "error": "Missing required fields"}), 400
        
        # Save the classroom photo
        photo_path = decode_base64_image(photo_base64, f"uploads/classroom_photos/class_{class_id}_{date.replace('-', '_')}.jpg")
        
        # Process attendance
        attendance_results = attendance_service.process_attendance(class_id, photo_path, date)
        
        return jsonify({
            "success": True, 
            "attendance": attendance_results['attendance'],
            "recognized_students": attendance_results['recognized_students'],
            "unrecognized_faces": attendance_results['unrecognized_faces']
        })
    except Exception as e:
        logger.error(f"Error processing attendance: {str(e)}")
        return jsonify({"success": False, "error": str(e)}), 500

@app.route('/api/manual_attendance', methods=['POST'])
def manual_attendance():
    """Manually mark attendance for a student"""
    try:
        data = request.json
        student_id = data.get('student_id')
        class_id = data.get('class_id')
        date = data.get('date')
        status = data.get('status', True)  # Default to present
        
        if not all([student_id, class_id, date]):
            return jsonify({"success": False, "error": "Missing required fields"}), 400
        
        # Update attendance record
        attendance = Attendance.update_or_create(
            student_id=student_id,
            class_id=class_id,
            date=date,
            status=status
        )
        
        return jsonify({"success": True, "attendance": attendance})
    except Exception as e:
        logger.error(f"Error marking manual attendance: {str(e)}")
        return jsonify({"success": False, "error": str(e)}), 500

@app.route('/api/attendance_report', methods=['GET'])
def get_attendance_report():
    """Get attendance report for a class on a specific date"""
    try:
        class_id = request.args.get('class_id')
        date = request.args.get('date')
        
        if not all([class_id, date]):
            return jsonify({"success": False, "error": "Missing required fields"}), 400
        
        attendance_records = Attendance.get_by_class_and_date(class_id, date)
        
        # Fetch student details for each attendance record
        detailed_records = []
        for record in attendance_records:
            student = Student.get(record['student_id'])
            if student:
                detailed_records.append({
                    **record,
                    'student_name': student['name']
                })
        
        return jsonify({"success": True, "attendance_records": detailed_records})
    except Exception as e:
        logger.error(f"Error fetching attendance report: {str(e)}")
        return jsonify({"success": False, "error": str(e)}), 500

@app.route('/api/student_attendance_report', methods=['GET'])
def get_student_attendance_report():
    """Get attendance report for a specific student"""
    try:
        student_id = request.args.get('student_id')
        
        if not student_id:
            return jsonify({"success": False, "error": "Missing student ID"}), 400
        
        attendance_records = Attendance.get_by_student(student_id)
        
        return jsonify({"success": True, "attendance_records": attendance_records})
    except Exception as e:
        logger.error(f"Error fetching student attendance report: {str(e)}")
        return jsonify({"success": False, "error": str(e)}), 500

@app.route('/uploads/<path:filename>')
def uploaded_file(filename):
    """Serve uploaded files"""
    return send_from_directory('uploads', filename)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
