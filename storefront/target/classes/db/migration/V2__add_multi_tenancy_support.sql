-- Migration to add multi-tenancy support to storefront tables
-- This script modifies the products table to use UUID and adds tenant_id columns

-- First, create a backup of existing data if needed
-- ALTER TABLE products RENAME TO products_backup;
-- ALTER TABLE product_images RENAME TO product_images_backup;

-- Drop and recreate products table with UUID primary key and tenant_id
DROP TABLE IF EXISTS product_images CASCADE;
DROP TABLE IF EXISTS products CASCADE;

CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    discount_price DECIMAL(10,2),
    category VARCHAR(100) NOT NULL,
    brand VARCHAR(100),
    sku VARCHAR(50) NOT NULL,
    stock_quantity INTEGER NOT NULL DEFAULT 0,
    featured BOOLEAN NOT NULL DEFAULT false,
    active BOOLEAN NOT NULL DEFAULT true,
    rating DECIMAL(3,2),
    review_count INTEGER DEFAULT 0,
    meta_title VARCHAR(500),
    meta_description VARCHAR(1000),
    slug VARCHAR(200),
    weight DECIMAL(8,3),
    dimensions VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP
);

-- Create product_images table
CREATE TABLE product_images (
    product_id UUID NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- Create indexes for products table
CREATE INDEX idx_product_category ON products(category);
CREATE INDEX idx_product_brand ON products(brand);
CREATE INDEX idx_product_sku ON products(sku);
CREATE INDEX idx_product_active ON products(active);
CREATE INDEX idx_product_featured ON products(featured);
CREATE INDEX idx_product_tenant ON products(tenant_id);
CREATE INDEX idx_product_deleted ON products(deleted);
CREATE INDEX idx_product_created_at ON products(created_at);

-- Create unique constraint on SKU per tenant
CREATE UNIQUE INDEX idx_product_sku_tenant ON products(sku, tenant_id) WHERE deleted = false;

-- Create trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_products_updated_at 
    BEFORE UPDATE ON products 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- Add any other storefront-specific tables here if they exist
-- Example for orders, cart_items, etc.
-- ALTER TABLE orders ADD COLUMN tenant_id VARCHAR(50) NOT NULL;
-- CREATE INDEX idx_orders_tenant_id ON orders(tenant_id);

COMMIT;