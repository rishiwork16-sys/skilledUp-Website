-- LOR Feature Database Migration
-- Create lor_requests table in student_db

CREATE TABLE IF NOT EXISTS lor_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    enrollment_id BIGINT NOT NULL,
    request_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    eligibility_checked BOOLEAN NOT NULL DEFAULT FALSE,
    task_completion_percent INT,
    total_tasks INT,
    completed_tasks INT,
    lor_url VARCHAR(500),
    unique_lor_id VARCHAR(50) UNIQUE,
    admin_remarks TEXT,
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMP NULL,
    rejected_at TIMESTAMP NULL,
    generated_at TIMESTAMP NULL,
    
    CONSTRAINT fk_lor_student FOREIGN KEY (student_id) 
        REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_lor_enrollment FOREIGN KEY (enrollment_id) 
        REFERENCES internship_program_enrollments(id) ON DELETE CASCADE,
    CONSTRAINT uq_lor_student_enrollment UNIQUE (student_id, enrollment_id),
    
    INDEX idx_lor_status (request_status),
    INDEX idx_lor_student (student_id),
    INDEX idx_lor_enrollment (enrollment_id),
    INDEX idx_lor_unique_id (unique_lor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add comments for documentation
ALTER TABLE lor_requests 
    COMMENT = 'Stores Letter of Recommendation requests with auto-approval support';
