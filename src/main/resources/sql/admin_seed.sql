-- Admin user seed script for Hercey
-- Run this in MySQL after the admin table is created by Hibernate

-- First, ensure ACTIVE status exists
INSERT INTO status (value) VALUES ('ACTIVE') ON DUPLICATE KEY UPDATE value='ACTIVE';

-- Get the ACTIVE status ID
SET @active_status_id = (SELECT id FROM status WHERE value = 'ACTIVE' LIMIT 1);

-- Insert default admin user (email: admin@hercey.com, password: Admin@123)
INSERT INTO admin (first_name, last_name, email, password, mobile, status_id, created_at, updated_at)
VALUES (
    'System',
    'Admin',
    'admin@hercey.com',
    'Admin@123',
    '0771234567',
    @active_status_id,
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE
    first_name = 'System',
    last_name = 'Admin',
    updated_at = NOW();

-- Query to verify admin was created
SELECT * FROM admin WHERE email = 'admin@hercey.com';

