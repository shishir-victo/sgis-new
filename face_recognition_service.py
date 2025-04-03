#!/usr/bin/env python3
# Mock Face Recognition Service for Attendance System
import os
import logging
import random
from models import Student

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class FaceRecognitionService:
    def __init__(self):
        """Initialize the mock face recognition service"""
        self.known_face_names = []
        self.known_student_ids = []
        self.update_student_encodings()
    
    def update_student_encodings(self):
        """Update the student list from the database"""
        logger.info("Updating student list (mock)")
        students = Student.get_all()
        
        self.known_face_names = []
        self.known_student_ids = []
        
        for student in students:
            self.known_face_names.append(student['name'])
            self.known_student_ids.append(student['student_id'])
            logger.info(f"Added student: {student['name']}")
        
        logger.info(f"Updated student list with {len(self.known_student_ids)} students")
    
    def recognize_faces(self, image_path):
        """
        Mock face recognition that randomly recognizes students
        
        Returns:
            dict: Dictionary with recognized students and unrecognized faces info
        """
        logger.info(f"Processing image: {image_path}")
        
        # Get all students from the database
        students = Student.get_all()
        
        recognized_students = []
        unrecognized_faces = []
        
        # For mock purposes, randomly recognize 60-80% of students
        if students:
            # Randomly decide how many students to recognize
            num_to_recognize = random.randint(max(1, int(len(students) * 0.6)), max(1, int(len(students) * 0.8)))
            
            # Randomly select students to recognize
            students_to_recognize = random.sample(students, min(num_to_recognize, len(students)))
            
            for i, student in enumerate(students_to_recognize):
                # Generate a random confidence score between 0.65 and 0.95
                confidence = round(random.uniform(0.65, 0.95), 2)
                
                recognized_students.append({
                    "student_id": student['student_id'],
                    "name": student['name'],
                    "confidence": float(confidence),
                    "face_index": i,
                    "location": [50 + i*30, 100 + i*20, 150 + i*30, 100 + i*20]  # Mock face location
                })
                
                logger.info(f"Recognized student: {student['name']} (ID: {student['student_id']}) with confidence: {confidence:.2f}")
            
            # Add some random unrecognized faces
            num_unrecognized = random.randint(0, 2)
            for i in range(num_unrecognized):
                confidence = round(random.uniform(0.2, 0.55), 2)
                unrecognized_faces.append({
                    "face_index": len(recognized_students) + i,
                    "confidence": float(confidence),
                    "location": [200, 200, 250, 150]  # Mock face location
                })
                
                logger.info(f"Unrecognized face at position {i} with confidence: {confidence:.2f}")
        
        # Mock processed image path
        output_path = image_path.replace('.jpg', '_processed.jpg')
        # Just create an empty file as a placeholder
        with open(output_path, 'w') as f:
            f.write("")
        
        logger.info(f"Mock processed image saved to: {output_path}")
        logger.info(f"Found {len(recognized_students)} recognized students and {len(unrecognized_faces)} unrecognized faces")
        
        return {
            "recognized_students": recognized_students,
            "unrecognized_faces": unrecognized_faces,
            "processed_image_path": output_path
        }
