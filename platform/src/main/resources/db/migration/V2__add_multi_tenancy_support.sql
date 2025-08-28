-- Migration to add multi-tenancy support to existing tables
-- This script modifies the tenants table to use UUID and adds tenant_id columns

-- First, create a backup of existing data if needed
-- ALTER TABLE tenants RENAME TO tenants_backup;

-- Drop and recreate tenants table with UUID primary key and tenant_id
DROP TABLE IF EXISTS tenants CASCADE;

CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(50) NOT NULL DEFAULT 'system',
    name VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP
);

-- Create indexes for tenants table
CREATE INDEX idx_tenant_code ON tenants(code);
CREATE INDEX idx_tenant_name ON tenants(name);
CREATE INDEX idx_tenant_active ON tenants(active);
CREATE INDEX idx_tenant_tenant_id ON tenants(tenant_id);
CREATE INDEX idx_tenant_deleted ON tenants(deleted);

-- Insert default system tenant
INSERT INTO tenants (id, tenant_id, name, code, description, active) 
VALUES (
    gen_random_uuid(),
    'system',
    'System Tenant',
    'system',
    'Default system tenant for platform management',
    true
);

-- Create trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_tenants_updated_at 
    BEFORE UPDATE ON tenants 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- Add any other platform-specific tables here if they exist
-- Example for a hypothetical platform_users table:
-- ALTER TABLE platform_users ADD COLUMN tenant_id VARCHAR(50) NOT NULL DEFAULT 'system';
-- CREATE INDEX idx_platform_users_tenant_id ON platform_users(tenant_id);

COMMIT;