-- Migration to add product variants and options support
-- This migration creates the variant system and migrates existing products to use default variants

-- Options table (e.g., Size, Color)
CREATE TABLE options (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    description TEXT,
    type ENUM('TEXT', 'COLOR', 'SIZE', 'NUMBER', 'BOOLEAN') NOT NULL DEFAULT 'TEXT',
    sort_order INT DEFAULT 0,
    is_required BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    UNIQUE KEY uk_tenant_name (tenant_id, name),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_type (type),
    INDEX idx_active (is_active),
    INDEX idx_sort_order (sort_order)
);

-- Option Values table (e.g., L, XL, Red, Blue)
CREATE TABLE option_values (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    option_id UUID NOT NULL,
    value VARCHAR(255) NOT NULL,
    display_value VARCHAR(255) NOT NULL,
    color_code VARCHAR(7), -- For color options (hex code)
    sort_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    UNIQUE KEY uk_option_value (option_id, value),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_option_id (option_id),
    INDEX idx_active (is_active),
    INDEX idx_sort_order (sort_order),
    FOREIGN KEY (option_id) REFERENCES options(id) ON DELETE CASCADE
);

-- Product Options junction table (which options are available for a product)
CREATE TABLE product_options (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    product_id UUID NOT NULL,
    option_id UUID NOT NULL,
    is_required BOOLEAN DEFAULT FALSE,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    UNIQUE KEY uk_product_option (product_id, option_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_product_id (product_id),
    INDEX idx_option_id (option_id),
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    FOREIGN KEY (option_id) REFERENCES options(id) ON DELETE CASCADE
);

-- Product Variants table (specific combinations of options with their own SKU, price, stock)
CREATE TABLE product_variants (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    product_id UUID NOT NULL,
    sku VARCHAR(100) NOT NULL,
    barcode VARCHAR(100),
    price DECIMAL(10,2) NOT NULL,
    sale_price DECIMAL(10,2),
    cost_price DECIMAL(10,2),
    stock INT DEFAULT 0,
    weight DECIMAL(8,3),
    dimensions VARCHAR(100), -- JSON string for length, width, height
    attributes JSON, -- Additional variant-specific attributes
    image_id BIGINT, -- Reference to product_images
    is_default BOOLEAN DEFAULT FALSE,
    status ENUM('ACTIVE', 'INACTIVE', 'OUT_OF_STOCK') DEFAULT 'ACTIVE',
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    UNIQUE KEY uk_tenant_sku (tenant_id, sku),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_product_id (product_id),
    INDEX idx_sku (sku),
    INDEX idx_barcode (barcode),
    INDEX idx_price (price),
    INDEX idx_stock (stock),
    INDEX idx_default (is_default),
    INDEX idx_status (status),
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    FOREIGN KEY (image_id) REFERENCES product_images(id) ON DELETE SET NULL
);

-- Product Variant Option Values junction table (which option values make up each variant)
CREATE TABLE product_variant_option_values (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    variant_id UUID NOT NULL,
    option_id UUID NOT NULL,
    option_value_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    UNIQUE KEY uk_variant_option (variant_id, option_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_variant_id (variant_id),
    INDEX idx_option_id (option_id),
    INDEX idx_option_value_id (option_value_id),
    FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE CASCADE,
    FOREIGN KEY (option_id) REFERENCES options(id) ON DELETE CASCADE,
    FOREIGN KEY (option_value_id) REFERENCES option_values(id) ON DELETE CASCADE
);

-- Migrate existing products to have default variants
-- First, create default variants for all existing products
INSERT INTO product_variants (
    tenant_id,
    product_id,
    sku,
    price,
    stock,
    weight,
    dimensions,
    is_default,
    status,
    created_at,
    updated_at
)
SELECT 
    tenant_id,
    id as product_id,
    COALESCE(sku, CONCAT('DEFAULT-', id)) as sku,
    price,
    inventory_quantity as stock,
    weight,
    dimensions,
    TRUE as is_default,
    CASE 
        WHEN status = 'ACTIVE' THEN 'ACTIVE'
        WHEN status = 'INACTIVE' THEN 'INACTIVE'
        ELSE 'INACTIVE'
    END as status,
    created_at,
    updated_at
FROM products
WHERE id NOT IN (
    SELECT DISTINCT product_id 
    FROM product_variants 
    WHERE product_id IS NOT NULL
);

-- Update cart_items to reference variants instead of products directly
-- Add variant_id column to cart_items
ALTER TABLE cart_items 
ADD COLUMN variant_id BIGINT AFTER product_id,
ADD INDEX idx_variant_id (variant_id),
ADD FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE CASCADE;

-- Update existing cart_items to use default variants
UPDATE cart_items ci
JOIN product_variants pv ON ci.product_id = pv.product_id AND pv.is_default = TRUE
SET ci.variant_id = pv.id
WHERE ci.variant_id IS NULL;

-- Update order_items to reference variants instead of products directly
-- Add variant_id column to order_items
ALTER TABLE order_items 
ADD COLUMN variant_id BIGINT AFTER product_id,
ADD INDEX idx_variant_id (variant_id),
ADD FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE SET NULL;

-- Update existing order_items to use default variants
UPDATE order_items oi
JOIN product_variants pv ON oi.product_id = pv.product_id AND pv.is_default = TRUE
SET oi.variant_id = pv.id
WHERE oi.variant_id IS NULL;

-- Add some sample options for demonstration
INSERT INTO options (tenant_id, name, display_name, description, type, sort_order, is_required) VALUES
('system', 'size', 'Size', 'Product size options', 'SIZE', 1, TRUE),
('system', 'color', 'Color', 'Product color options', 'COLOR', 2, FALSE),
('system', 'material', 'Material', 'Product material options', 'TEXT', 3, FALSE);

-- Add some sample option values
INSERT INTO option_values (tenant_id, option_id, value, display_value, sort_order) 
SELECT 
    'system',
    o.id,
    'S',
    'Small',
    1
FROM options o WHERE o.name = 'size' AND o.tenant_id = 'system';

INSERT INTO option_values (tenant_id, option_id, value, display_value, sort_order) 
SELECT 
    'system',
    o.id,
    'M',
    'Medium',
    2
FROM options o WHERE o.name = 'size' AND o.tenant_id = 'system';

INSERT INTO option_values (tenant_id, option_id, value, display_value, sort_order) 
SELECT 
    'system',
    o.id,
    'L',
    'Large',
    3
FROM options o WHERE o.name = 'size' AND o.tenant_id = 'system';

INSERT INTO option_values (tenant_id, option_id, value, display_value, color_code, sort_order) 
SELECT 
    'system',
    o.id,
    'red',
    'Red',
    '#FF0000',
    1
FROM options o WHERE o.name = 'color' AND o.tenant_id = 'system';

INSERT INTO option_values (tenant_id, option_id, value, display_value, color_code, sort_order) 
SELECT 
    'system',
    o.id,
    'blue',
    'Blue',
    '#0000FF',
    2
FROM options o WHERE o.name = 'color' AND o.tenant_id = 'system';

INSERT INTO option_values (tenant_id, option_id, value, display_value, color_code, sort_order) 
SELECT 
    'system',
    o.id,
    'green',
    'Green',
    '#00FF00',
    3
FROM options o WHERE o.name = 'color' AND o.tenant_id = 'system';

-- Add constraint to ensure only one default variant per product
ALTER TABLE product_variants 
ADD CONSTRAINT chk_one_default_per_product 
CHECK (is_default = FALSE OR (
    SELECT COUNT(*) 
    FROM product_variants pv2 
    WHERE pv2.product_id = product_variants.product_id 
    AND pv2.is_default = TRUE 
    AND pv2.id != product_variants.id
) = 0);