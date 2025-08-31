-- Initialize BharatShop database
-- This script runs when MySQL container starts for the first time

-- Create additional databases if needed
CREATE DATABASE IF NOT EXISTS bharatshop_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Grant permissions
GRANT ALL PRIVILEGES ON bharatshop.* TO 'bharatshop'@'%';
GRANT ALL PRIVILEGES ON bharatshop_test.* TO 'bharatshop'@'%';

-- Create additional user for read-only access (for monitoring/reporting)
CREATE USER IF NOT EXISTS 'bharatshop_readonly'@'%' IDENTIFIED BY 'readonly123';
GRANT SELECT ON bharatshop.* TO 'bharatshop_readonly'@'%';

-- Flush privileges
FLUSH PRIVILEGES;

-- Log initialization
SELECT 'BharatShop database initialization completed' AS message;