#!/usr/bin/env python3
# Utility functions for the Attendance System
import os
import base64
import logging
from datetime import datetime

logger = logging.getLogger(__name__)

def save_uploaded_image(file, directory, filename=None):
    """
    Save an uploaded image file
    
    Args:
        file: The uploaded file object
        directory (str): Directory to save the file in
        filename (str, optional): Custom filename
    
    Returns:
        str: Path to the saved file
    """
    if not os.path.exists(directory):
        os.makedirs(directory, exist_ok=True)
    
    if filename is None:
        # Generate a filename based on timestamp
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        filename = f"{timestamp}.jpg"
    
    file_path = os.path.join(directory, filename)
    file.save(file_path)
    
    logger.info(f"Saved uploaded image to {file_path}")
    
    return file_path

def decode_base64_image(base64_string, output_path):
    """
    Decode a base64 image string and save it to a file
    
    Args:
        base64_string (str): Base64 encoded image
        output_path (str): Path to save the image
    
    Returns:
        str: Path to the saved image
    """
    try:
        # Remove data URL prefix if present
        if "base64," in base64_string:
            base64_string = base64_string.split("base64,")[1]
        
        # Decode the base64 string
        image_data = base64.b64decode(base64_string)
        
        # Ensure the directory exists
        os.makedirs(os.path.dirname(output_path), exist_ok=True)
        
        # Save the image
        with open(output_path, "wb") as f:
            f.write(image_data)
        
        logger.info(f"Decoded base64 image and saved to {output_path}")
        
        return output_path
    except Exception as e:
        logger.error(f"Error decoding base64 image: {str(e)}")
        raise
