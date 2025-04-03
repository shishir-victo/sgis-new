# AWS Deployment Guide for Attendance System

This document outlines the steps to deploy the Attendance System (with facial recognition for student attendance) on AWS services.

## Architecture Overview

The deployment architecture consists of:

1. **Backend API Server**: Flask application hosted on AWS Elastic Beanstalk
2. **File Storage**: AWS S3 for storing student photos and classroom images
3. **Database**: AWS DynamoDB for storing attendance records, student information, and class data
4. **Android App**: Mobile client that communicates with the backend API

## Prerequisites

- AWS Account with administrator access
- AWS CLI installed and configured locally
- Git repository with the application code
- Android app built and ready for distribution

## Step 1: Create S3 Buckets for File Storage

1. **Create Buckets**:
   ```bash
   # Create bucket for student photos
   aws s3 mb s3://attendance-system-student-photos --region <your-region>
   
   # Create bucket for classroom photos
   aws s3 mb s3://attendance-system-classroom-photos --region <your-region>
   ```

2. **Configure CORS for S3 Buckets**:
   Create a `cors.json` file:
   ```json
   {
     "CORSRules": [
       {
         "AllowedHeaders": ["*"],
         "AllowedMethods": ["GET", "PUT", "POST", "DELETE"],
         "AllowedOrigins": ["*"],
         "ExposeHeaders": ["ETag"]
       }
     ]
   }
   ```

   Apply CORS configuration:
   ```bash
   aws s3api put-bucket-cors --bucket attendance-system-student-photos --cors-configuration file://cors.json
   aws s3api put-bucket-cors --bucket attendance-system-classroom-photos --cors-configuration file://cors.json
   ```

## Step 2: Create DynamoDB Tables

```bash
# Create Students Table
aws dynamodb create-table \
  --table-name Students \
  --attribute-definitions \
    AttributeName=student_id,AttributeType=S \
  --key-schema \
    AttributeName=student_id,KeyType=HASH \
  --provisioned-throughput \
    ReadCapacityUnits=5,WriteCapacityUnits=5 \
  --region <your-region>

# Create Classes Table
aws dynamodb create-table \
  --table-name Classes \
  --attribute-definitions \
    AttributeName=id,AttributeType=S \
  --key-schema \
    AttributeName=id,KeyType=HASH \
  --provisioned-throughput \
    ReadCapacityUnits=5,WriteCapacityUnits=5 \
  --region <your-region>

# Create Attendance Table
aws dynamodb create-table \
  --table-name Attendance \
  --attribute-definitions \
    AttributeName=id,AttributeType=S \
  --key-schema \
    AttributeName=id,KeyType=HASH \
  --provisioned-throughput \
    ReadCapacityUnits=5,WriteCapacityUnits=5 \
  --region <your-region>
```

## Step 3: Update Application Code for AWS Services

Update the application to use AWS services instead of file-based storage:

1. **Update models.py** to use DynamoDB instead of local JSON files
2. **Update utils.py** to use S3 for file storage instead of local filesystem
3. **Create a configuration file** for AWS service endpoints and credentials

### Example AWS Configuration (aws_config.py)

```python
import os

# AWS Configuration
AWS_REGION = os.environ.get('AWS_REGION', 'us-east-1')
DYNAMODB_ENDPOINT = os.environ.get('DYNAMODB_ENDPOINT', None)  # Use AWS service endpoint by default
S3_ENDPOINT = os.environ.get('S3_ENDPOINT', None)  # Use AWS service endpoint by default

# S3 Buckets
STUDENT_PHOTOS_BUCKET = 'attendance-system-student-photos'
CLASSROOM_PHOTOS_BUCKET = 'attendance-system-classroom-photos'

# DynamoDB Tables
STUDENTS_TABLE = 'Students'
CLASSES_TABLE = 'Classes'
ATTENDANCE_TABLE = 'Attendance'
```

### Example S3 Utility Functions

```python
import boto3
from aws_config import AWS_REGION, S3_ENDPOINT, STUDENT_PHOTOS_BUCKET, CLASSROOM_PHOTOS_BUCKET

# Initialize S3 client
s3 = boto3.client(
    's3',
    region_name=AWS_REGION,
    endpoint_url=S3_ENDPOINT
)

def upload_student_photo(photo_data, student_id):
    """
    Upload student photo to S3
    """
    key = f"{student_id}.jpg"
    s3.put_object(
        Bucket=STUDENT_PHOTOS_BUCKET,
        Key=key,
        Body=photo_data,
        ContentType='image/jpeg'
    )
    return f"s3://{STUDENT_PHOTOS_BUCKET}/{key}"

def upload_classroom_photo(photo_data, class_id, date):
    """
    Upload classroom photo to S3
    """
    key = f"{class_id}/{date.replace('-', '_')}.jpg"
    s3.put_object(
        Bucket=CLASSROOM_PHOTOS_BUCKET,
        Key=key,
        Body=photo_data,
        ContentType='image/jpeg'
    )
    return f"s3://{CLASSROOM_PHOTOS_BUCKET}/{key}"
```

### Example DynamoDB Models

```python
import boto3
import time
from datetime import datetime
from boto3.dynamodb.conditions import Key
from aws_config import AWS_REGION, DYNAMODB_ENDPOINT, STUDENTS_TABLE, CLASSES_TABLE, ATTENDANCE_TABLE

# Initialize DynamoDB client
dynamodb = boto3.resource(
    'dynamodb',
    region_name=AWS_REGION,
    endpoint_url=DYNAMODB_ENDPOINT
)

# Initialize tables
students_table = dynamodb.Table(STUDENTS_TABLE)
classes_table = dynamodb.Table(CLASSES_TABLE)
attendance_table = dynamodb.Table(ATTENDANCE_TABLE)

class Student:
    @staticmethod
    def get_all():
        """Get all students"""
        response = students_table.scan()
        return response.get('Items', [])

    @staticmethod
    def get(student_id):
        """Get a specific student by ID"""
        response = students_table.get_item(Key={'student_id': student_id})
        return response.get('Item')

    @staticmethod
    def get_by_class(class_id):
        """Get all students in a specific class"""
        # Note: This is a scan with filter which isn't efficient for large datasets
        # Consider using a global secondary index in production
        response = students_table.scan(
            FilterExpression=Key('class_id').eq(class_id)
        )
        return response.get('Items', [])

    @staticmethod
    def create(name, student_id, class_id, photo_path):
        """Create a new student"""
        timestamp = datetime.now().isoformat()
        item = {
            'student_id': student_id,
            'name': name,
            'class_id': class_id,
            'photo_path': photo_path,
            'created_at': timestamp
        }
        students_table.put_item(Item=item)
        return item
    
    # Other methods (update, delete) would follow the same pattern
```

## Step 4: Deploy to Elastic Beanstalk

1. **Initialize Elastic Beanstalk Application**:
   ```bash
   eb init -p python-3.8 attendance-system --region <your-region>
   ```

2. **Create an environment**:
   ```bash
   eb create attendance-system-production
   ```

3. **Set environment variables**:
   ```bash
   eb setenv \
     AWS_REGION=<your-region> \
     STUDENT_PHOTOS_BUCKET=attendance-system-student-photos \
     CLASSROOM_PHOTOS_BUCKET=attendance-system-classroom-photos
   ```

4. **Deploy your application**:
   ```bash
   eb deploy
   ```

## Step 5: Create IAM Role for Elastic Beanstalk

1. Create a role with permissions for:
   - S3 (GetObject, PutObject, DeleteObject)
   - DynamoDB (full access to your tables)

2. Attach the role to your Elastic Beanstalk environment

## Step 6: Update Android App Configuration

Update the Android app's `RetrofitClient.java` to point to your Elastic Beanstalk endpoint:

```java
public class RetrofitClient {
    // Change this to your Elastic Beanstalk endpoint
    private static final String BASE_URL = "http://attendance-system-production.us-east-1.elasticbeanstalk.com/api/";
    private static RetrofitClient instance;
    private Retrofit retrofit;

    private RetrofitClient() {
        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public static synchronized RetrofitClient getInstance() {
        if (instance == null) {
            instance = new RetrofitClient();
        }
        return instance;
    }

    public ApiService getApi() {
        return retrofit.create(ApiService.class);
    }
}
```

## Step 7: Set Up CloudWatch for Monitoring

```bash
# Enable CloudWatch logs for your Elastic Beanstalk environment
eb setenv --environment attendance-system-production EnableCWLogging=true
```

## Step 8: Configure Auto Scaling

1. **Go to the Elastic Beanstalk Console**
2. **Select your environment**
3. **Navigate to Configuration > Capacity**
4. **Set up Auto Scaling group**:
   - Min instances: 1
   - Max instances: 4
   - Scaling triggers based on CPU utilization or network traffic

## Security Considerations

1. **Secure S3 Buckets**:
   ```bash
   aws s3api put-bucket-policy --bucket attendance-system-student-photos --policy file://bucket-policy.json
   ```

2. **Enable HTTPS**:
   - Create an SSL certificate in AWS Certificate Manager
   - Configure your Elastic Beanstalk environment to use HTTPS

3. **Set Up AWS WAF** (Web Application Firewall):
   - Create rules to protect against common web vulnerabilities
   - Associate WAF with your Elastic Beanstalk environment

## Cost Optimization

1. **DynamoDB Capacity Mode**:
   - Consider using On-Demand Capacity Mode for unpredictable workloads
   - Use Provisioned Capacity with Auto Scaling for predictable workloads

2. **S3 Storage Classes**:
   - Use S3 Lifecycle policies to move older classroom photos to Infrequent Access tier

3. **Elastic Beanstalk**:
   - Use smaller instance types in development environments
   - Scale down instances during non-school hours

## Maintenance and Operations

1. **Backup Strategy**:
   - Enable DynamoDB Point-in-Time Recovery
   - Enable S3 bucket versioning

2. **Regular Updates**:
   - Set up a CI/CD pipeline for regular updates
   - Use Blue/Green deployment for zero-downtime updates

3. **Monitoring**:
   - Set up CloudWatch alarms for key metrics
   - Configure SNS notifications for alarms

## Disaster Recovery

1. **Data Backup**:
   - Configure DynamoDB backups to S3
   - Set up cross-region replication for S3 buckets

2. **Recovery Plan**:
   - Document steps to restore from backups
   - Test recovery procedures regularly

## Compliance Considerations

1. **Data Privacy**:
   - Ensure student data is encrypted at rest and in transit
   - Implement appropriate access controls

2. **Data Retention**:
   - Configure data retention policies according to educational regulations
   - Set up automated cleanup of old data

## Conclusion

This deployment guide provides a foundation for deploying the Attendance System on AWS. Adapt the specifics based on your organization's requirements and AWS best practices. Always test thoroughly in a staging environment before deploying to production.