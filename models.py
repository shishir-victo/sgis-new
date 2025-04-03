#!/usr/bin/env python3
# Database models for the Attendance System
import os
import json
import time
from datetime import datetime

# File paths for data storage
DATA_DIR = 'data'
STUDENTS_FILE = os.path.join(DATA_DIR, 'students.json')
CLASSES_FILE = os.path.join(DATA_DIR, 'classes.json')
ATTENDANCE_FILE = os.path.join(DATA_DIR, 'attendance.json')

def init_db():
    """Initialize the database files if they don't exist"""
    os.makedirs(DATA_DIR, exist_ok=True)
    
    # Create empty JSON files if they don't exist
    for file_path in [STUDENTS_FILE, CLASSES_FILE, ATTENDANCE_FILE]:
        if not os.path.exists(file_path):
            with open(file_path, 'w') as f:
                json.dump([], f)

class Student:
    @staticmethod
    def get_all():
        """Get all students"""
        if os.path.exists(STUDENTS_FILE):
            with open(STUDENTS_FILE, 'r') as f:
                return json.load(f)
        return []
    
    @staticmethod
    def get(student_id):
        """Get a specific student by ID"""
        students = Student.get_all()
        for student in students:
            if student['student_id'] == student_id:
                return student
        return None
    
    @staticmethod
    def get_by_class(class_id):
        """Get all students in a specific class"""
        students = Student.get_all()
        return [s for s in students if s['class_id'] == class_id]
    
    @staticmethod
    def create(name, student_id, class_id, photo_path):
        """Create a new student"""
        students = Student.get_all()
        
        # Check if student ID already exists
        for student in students:
            if student['student_id'] == student_id:
                raise ValueError(f"Student ID {student_id} already exists")
        
        new_student = {
            'id': len(students) + 1,
            'name': name,
            'student_id': student_id,
            'class_id': class_id,
            'photo_path': photo_path,
            'created_at': datetime.now().isoformat()
        }
        
        students.append(new_student)
        
        with open(STUDENTS_FILE, 'w') as f:
            json.dump(students, f, indent=4)
        
        return new_student
    
    @staticmethod
    def update(student_id, data):
        """Update a student's information"""
        students = Student.get_all()
        updated = False
        
        for i, student in enumerate(students):
            if student['student_id'] == student_id:
                students[i].update(data)
                students[i]['updated_at'] = datetime.now().isoformat()
                updated = True
                break
        
        if updated:
            with open(STUDENTS_FILE, 'w') as f:
                json.dump(students, f, indent=4)
            return students[i]
        return None
    
    @staticmethod
    def delete(student_id):
        """Delete a student"""
        students = Student.get_all()
        initial_count = len(students)
        
        students = [s for s in students if s['student_id'] != student_id]
        
        if len(students) < initial_count:
            with open(STUDENTS_FILE, 'w') as f:
                json.dump(students, f, indent=4)
            return True
        return False

class Class:
    @staticmethod
    def get_all():
        """Get all classes"""
        if os.path.exists(CLASSES_FILE):
            with open(CLASSES_FILE, 'r') as f:
                return json.load(f)
        return []
    
    @staticmethod
    def get(class_id):
        """Get a specific class by ID"""
        classes = Class.get_all()
        for cls in classes:
            if cls['id'] == class_id:
                return cls
        return None
    
    @staticmethod
    def create(name):
        """Create a new class"""
        classes = Class.get_all()
        
        new_class = {
            'id': str(int(time.time())),  # Use timestamp as ID
            'name': name,
            'created_at': datetime.now().isoformat()
        }
        
        classes.append(new_class)
        
        with open(CLASSES_FILE, 'w') as f:
            json.dump(classes, f, indent=4)
        
        return new_class
    
    @staticmethod
    def update(class_id, data):
        """Update a class"""
        classes = Class.get_all()
        updated = False
        
        for i, cls in enumerate(classes):
            if cls['id'] == class_id:
                classes[i].update(data)
                classes[i]['updated_at'] = datetime.now().isoformat()
                updated = True
                break
        
        if updated:
            with open(CLASSES_FILE, 'w') as f:
                json.dump(classes, f, indent=4)
            return classes[i]
        return None
    
    @staticmethod
    def delete(class_id):
        """Delete a class"""
        classes = Class.get_all()
        initial_count = len(classes)
        
        classes = [c for c in classes if c['id'] != class_id]
        
        if len(classes) < initial_count:
            with open(CLASSES_FILE, 'w') as f:
                json.dump(classes, f, indent=4)
            return True
        return False

class Attendance:
    @staticmethod
    def get_all():
        """Get all attendance records"""
        if os.path.exists(ATTENDANCE_FILE):
            with open(ATTENDANCE_FILE, 'r') as f:
                return json.load(f)
        return []
    
    @staticmethod
    def get_by_class_and_date(class_id, date):
        """Get attendance records for a specific class on a specific date"""
        records = Attendance.get_all()
        return [r for r in records if r['class_id'] == class_id and r['date'] == date]
    
    @staticmethod
    def get_by_student(student_id):
        """Get all attendance records for a specific student"""
        records = Attendance.get_all()
        return [r for r in records if r['student_id'] == student_id]
    
    @staticmethod
    def create(student_id, class_id, date, status=True):
        """Create a new attendance record"""
        records = Attendance.get_all()
        
        # Check if record already exists
        for record in records:
            if (record['student_id'] == student_id and 
                record['class_id'] == class_id and 
                record['date'] == date):
                raise ValueError("Attendance record already exists")
        
        new_record = {
            'id': str(int(time.time())),  # Use timestamp as ID
            'student_id': student_id,
            'class_id': class_id,
            'date': date,
            'status': status,  # True = present, False = absent
            'created_at': datetime.now().isoformat()
        }
        
        records.append(new_record)
        
        with open(ATTENDANCE_FILE, 'w') as f:
            json.dump(records, f, indent=4)
        
        return new_record
    
    @staticmethod
    def update_or_create(student_id, class_id, date, status=True):
        """Update an existing attendance record or create a new one"""
        records = Attendance.get_all()
        
        # Try to find and update existing record
        for i, record in enumerate(records):
            if (record['student_id'] == student_id and 
                record['class_id'] == class_id and 
                record['date'] == date):
                records[i]['status'] = status
                records[i]['updated_at'] = datetime.now().isoformat()
                
                with open(ATTENDANCE_FILE, 'w') as f:
                    json.dump(records, f, indent=4)
                
                return records[i]
        
        # Create new record if not found
        return Attendance.create(student_id, class_id, date, status)
    
    @staticmethod
    def delete(attendance_id):
        """Delete an attendance record"""
        records = Attendance.get_all()
        initial_count = len(records)
        
        records = [r for r in records if r['id'] != attendance_id]
        
        if len(records) < initial_count:
            with open(ATTENDANCE_FILE, 'w') as f:
                json.dump(records, f, indent=4)
            return True
        return False
