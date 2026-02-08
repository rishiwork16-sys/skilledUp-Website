# Live Deployment Guide (AWS Backend + AWS DB + GoDaddy Frontend)

## Target Architecture (simple + production-friendly)
- Frontend (GoDaddy): `https://yourdomain.com` and `https://admin.yourdomain.com` (static files)
- Backend (AWS): EC2 (Docker Compose) + only one public port `8080` (API Gateway)
- Database (AWS): RDS MySQL
- Files (AWS): S3 bucket

## Part A — AWS Database (RDS MySQL)
1. AWS Console → RDS → Create database → MySQL
2. DB instance: `db.t3.micro` (start), Storage as needed
3. Public access: `No` (recommended)
4. VPC + Subnet group: default ok (start)
5. Security group: allow inbound MySQL (3306) only from EC2 security group (not from 0.0.0.0/0)
6. Create DB user/password and note RDS endpoint
7. In the same RDS instance create schemas (one time):
   - `auth_db`, `student_db`, `certificate_db`, `task_db`, `notification_db`, `support_db`, `course_db`, `payment_db`, `career_db`

## Part B — AWS S3 (uploads / certificates / resumes)
1. S3 → Create bucket (same region as backend)
2. Block public access: keep ON (recommended)
3. Backend will use presigned URLs for download/view
4. IAM:
   - Best: give EC2 an IAM Role with S3 access (no keys in files)
   - Minimum permissions: `s3:PutObject`, `s3:GetObject`, `s3:ListBucket` on the bucket

## Part C — AWS Backend on EC2 (Docker Compose)
### 1) EC2 + Security Groups
1. Launch EC2 (Ubuntu 22.04 / Amazon Linux 2023), type `t3.medium` (microservices + JVM need RAM)
2. Security group inbound:
   - 22 (SSH) from your IP
   - 8080 (HTTP API Gateway) from 0.0.0.0/0
3. Attach an IAM Role to EC2 for S3 access (recommended)

### 2) Install Docker
Install Docker + Docker Compose plugin (Ubuntu):
- `sudo apt update`
- `sudo apt install -y docker.io docker-compose-plugin`
- `sudo usermod -aG docker ubuntu`

### 3) Copy project to server
- Option 1: Git clone repo (recommended)
- Option 2: Zip upload + unzip

### 4) Configure environment
1. Go to `deploy/`
2. Copy `.env.example` → `.env`
3. Set values:
   - `YOUR_RDS_ENDPOINT` with actual RDS endpoint
   - `DB_USERNAME`, `DB_PASSWORD`
   - `JWT_SECRET` (minimum 32 characters)
   - `SMTP_USERNAME`, `SMTP_PASSWORD` (email)
   - `FAST2SMS_API_KEY` (SMS OTP, optional)
   - `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET` (payments)
   - `CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://admin.yourdomain.com`
   - `AWS_REGION`, `AWS_BUCKET`
   - If not using EC2 IAM Role, set `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`

### 5) Start all backend containers
From `deploy/` folder:
- `docker compose --env-file .env -f docker-compose.ec2.yml up -d --build`

API base URL:
- `http://EC2_PUBLIC_IP:8080`

## Part D — GoDaddy Frontend Live (React Vite)
You have two frontends:
- `frontend` (user)
- `admin-frontend` (admin)

### 1) Set API base URL for production
In each frontend, set API URL to your live gateway URL:
- Example: `https://api.yourdomain.com` (recommended) OR `http://EC2_IP:8080`

Vite usually uses `.env.production`:
- `VITE_API_BASE_URL=https://api.yourdomain.com/api`
- `VITE_PUBLIC_SITE_URL=https://yourdomain.com` (admin only, optional but recommended)

Then build:
- `npm install`
- `npm run build`

### 2) Upload build to GoDaddy
1. GoDaddy cPanel → File Manager → `public_html/`
2. Upload the `dist/` contents (not the dist folder) into the hosting root
3. SPA routing fix: add `.htaccess` in `public_html/`:
   - Rewrite all routes to `/index.html`

## Part E — DNS (GoDaddy)
Recommended subdomains:
- `api.yourdomain.com` → AWS backend
- `yourdomain.com` → GoDaddy frontend
- `admin.yourdomain.com` → GoDaddy admin frontend

DNS options for API:
- If you use a Load Balancer (ALB): add CNAME `api` → ALB DNS
- If direct EC2: add A record `api` → EC2 Elastic IP

## Part F — Production checklist (must do)
- Remove hardcoded DB passwords and AWS keys from repository configs
- Use HTTPS:
  - Best: ALB + ACM certificate for `api.yourdomain.com`
- Update API Gateway CORS allowedOrigins to your GoDaddy domains
- Turn off `spring.jpa.show-sql` on prod
- Use `ddl-auto=validate` (or migrations) for prod stability
