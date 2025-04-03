# AWS Cost Estimation for Attendance System Deployment

This document provides a rough cost estimation for deploying the Student Attendance System on AWS services as described in the AWS_DEPLOYMENT_GUIDE.md. These are approximate costs based on AWS's pricing as of April 2023. Actual costs may vary based on usage patterns, data volume, and any AWS pricing changes.

## Assumptions

For this estimation, we'll assume:
- A school with 1,000 students
- 50 classes
- Attendance is taken once daily per class on weekdays (20 days/month)
- Each student photo is approximately 500KB
- Each classroom photo is approximately 2MB
- The application is accessed primarily during school hours (8 AM - 4 PM)

## Cost Breakdown

### 1. AWS Elastic Beanstalk (EC2)

Elastic Beanstalk itself doesn't cost extra, but you pay for the underlying resources:

- **t3.small instance** (2 vCPU, 2 GB RAM):
  - $0.0208 per hour × 24 hours × 30 days = ~$15/month
  - With auto-scaling (avg. 2 instances during peak hours):
    - ~$22/month

### 2. Amazon S3 Storage

- **Student photos**:
  - 1,000 students × 500KB = 500MB
  - $0.023 per GB/month × 0.5GB = ~$0.01/month

- **Classroom photos**:
  - 50 classes × 20 days × 2MB = 2GB/month
  - $0.023 per GB/month × 2GB = ~$0.05/month

- **S3 API Requests**:
  - PUT requests: 50 classes × 20 days = 1,000 requests/month
  - GET requests: ~10,000 requests/month (for viewing)
  - ~$0.05/month for requests

### 3. Amazon DynamoDB

- **Students table**:
  - 1,000 items × 2KB per item = 2MB storage
  - 5 RCU (read capacity units) and 5 WCU (write capacity units)
  - Storage: ~$0.25/month
  - Provisioned capacity: ~$4.75/month

- **Classes table**:
  - 50 items × 1KB per item = 50KB storage
  - 5 RCU and 5 WCU
  - Storage: Negligible
  - Provisioned capacity: ~$4.75/month

- **Attendance table**:
  - 50 classes × 20 days × avg. 20 students per class = 20,000 records/month
  - 20,000 items × 1KB per item = 20MB storage
  - 10 RCU and 10 WCU (higher to handle attendance recording)
  - Storage: ~$0.25/month
  - Provisioned capacity: ~$9.50/month

### 4. Data Transfer

- **Inbound** (free): 0
- **Outbound**:
  - Estimated 50GB/month (app usage, image downloads)
  - First GB free, then $0.09/GB up to 10TB
  - ~$4.41/month

### 5. Other Services

- **CloudWatch** (basic monitoring): ~$0.50/month
- **Route 53** (if using custom domain): $0.50/month + $12/year for domain

## Total Estimated Monthly Cost

| Service | Estimated Cost |
|---------|----------------|
| Elastic Beanstalk (EC2) | $22.00 |
| S3 Storage | $0.11 |
| DynamoDB | $19.50 |
| Data Transfer | $4.41 |
| CloudWatch | $0.50 |
| Route 53 | $1.50 |
| **Total** | **~$48.02/month** |

## Cost Optimization Strategies

1. **Use EC2 Reserved Instances**:
   - For a 1-year commitment, you could save ~30% on EC2 costs
   - For a 3-year commitment, you could save ~40-60% on EC2 costs

2. **DynamoDB On-Demand Capacity**:
   - If usage patterns are unpredictable, on-demand might be more cost-effective
   - Only pay for what you use, with no minimum capacity

3. **Auto Scaling Schedules**:
   - Scale down to 1 instance during non-school hours and weekends
   - This could reduce EC2 costs by ~40%

4. **S3 Lifecycle Policies**:
   - Move older classroom photos to Infrequent Access tier after 30 days
   - Archive to Glacier after 90 days
   - This would provide minimal savings given the small storage volume

5. **CloudFront for Content Delivery**:
   - If your application is accessed globally, CloudFront can reduce data transfer costs
   - Improves application performance with caching

## Additional Considerations

- **AWS Free Tier**:
  - If this is a new AWS account, some services might be free for the first 12 months
  - EC2: 750 hours of t2.micro per month (not sufficient for production)
  - S3: 5GB of storage
  - DynamoDB: 25GB of storage and 25 WCU/RCU

- **Scaling Costs**:
  - The above estimates are for 1,000 students
  - Costs would scale roughly linearly with student population

- **Development/Testing Environments**:
  - Consider using smaller instances for development
  - Turn off non-production environments when not in use

- **AWS Organizations and Consolidated Billing**:
  - If deploying multiple applications, consolidated billing can help optimize costs

## Conclusion

The estimated cost of approximately $48/month represents a basic deployment suitable for a school with 1,000 students. This could be significantly reduced through optimization strategies and leveraging the AWS Free Tier for the first year. 

For more precise estimation, consider using the [AWS Pricing Calculator](https://calculator.aws.amazon.com/) with your specific usage patterns.