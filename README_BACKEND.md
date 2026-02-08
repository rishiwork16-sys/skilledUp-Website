# Backend (Production-ready)

## What’s included
- Spring Boot microservices + API Gateway
- Docker deployment files (`deploy/`)
- No frontend code tracked in this repo

## Before deploy
1. Copy env template:
   - `deploy/.env.example` → `deploy/.env`
2. Fill required values:
   - `*_DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
   - `JWT_SECRET` (min 32 chars)
   - `AWS_REGION`, `AWS_BUCKET` (+ optional access keys if not using IAM Role)
   - `SMTP_USERNAME`, `SMTP_PASSWORD` (notifications)
   - `RAZORPAY_*` (payments)
   - `CORS_ALLOWED_ORIGINS` (your domains)

## Deploy (EC2)
From `deploy/`:
- `docker compose --env-file .env -f docker-compose.ec2.yml up -d --build`

## Local build (sanity)
From repo root:
- `mvn -DskipTests package`
