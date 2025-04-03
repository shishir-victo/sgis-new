#!/usr/bin/env python3
# Attendance Service for the Attendance System
import logging
from datetime import datetime
from models import Student, Attendance

logger = logging.getLogger(__name__)

class AttendanceService:
    def __init__(self, face_recognition_service):
        """Initialize the attendance service"""
        self.face_recognition_service = face_recognition_service
    
    def process_attendance(self, class_id, image_path, date=None):
        """
        Process attendance from a classroom photo
        
        Args:
            class_id (str): The ID of the class
            image_path (str): Path to the classroom photo
            date (str): Date for the attendance record (default: today)
        
        Returns:
            dict: Dictionary with attendance results
        """
        if date is None:
            date = datetime.now().strftime('%Y-%m-%d')
        
        logger.info(f"Processing attendance for class {class_id} on {date}")
        
        # Get all students in the class
        class_students = Student.get_by_class(class_id)
        student_ids = [s['student_id'] for s in class_students]
        
        # Recognize faces in the image
        recognition_results = self.face_recognition_service.recognize_faces(image_path)
        recognized_students = recognition_results['recognized_students']
        unrecognized_faces = recognition_results['unrecognized_faces']
        
        # Record attendance for recognized students
        attendance_records = []
        present_student_ids = set()
        
        for student in recognized_students:
            student_id = student['student_id']
            if student_id in student_ids:
                # Create attendance record (present)
                try:
                    attendance_record = Attendance.update_or_create(
                        student_id=student_id,
                        class_id=class_id,
                        date=date,
                        status=True  # Present
                    )
                    attendance_records.append(attendance_record)
                    present_student_ids.add(student_id)
                    logger.info(f"Marked student {student_id} as present")
                except Exception as e:
                    logger.error(f"Error marking attendance for student {student_id}: {str(e)}")
        
        # Mark absent for students not recognized in the photo
        for student_id in student_ids:
            if student_id not in present_student_ids:
                try:
                    attendance_record = Attendance.update_or_create(
                        student_id=student_id,
                        class_id=class_id,
                        date=date,
                        status=False  # Absent
                    )
                    attendance_records.append(attendance_record)
                    logger.info(f"Marked student {student_id} as absent")
                except Exception as e:
                    logger.error(f"Error marking absence for student {student_id}: {str(e)}")
        
        logger.info(f"Processed attendance for {len(attendance_records)} students")
        
        return {
            "attendance": attendance_records,
            "recognized_students": recognized_students,
            "unrecognized_faces": unrecognized_faces,
            "processed_image_path": recognition_results.get('processed_image_path')
        }
    
    def manual_attendance(self, student_id, class_id, date, status):
        """
        Manually mark attendance for a student
        
        Args:
            student_id (str): The ID of the student
            class_id (str): The ID of the class
            date (str): Date for the attendance record
            status (bool): True for present, False for absent
        
        Returns:
            dict: The attendance record
        """
        logger.info(f"Manually marking student {student_id} as {'present' if status else 'absent'}")
        
        try:
            attendance_record = Attendance.update_or_create(
                student_id=student_id,
                class_id=class_id,
                date=date,
                status=status
            )
            return attendance_record
        except Exception as e:
            logger.error(f"Error manually marking attendance: {str(e)}")
            raise
