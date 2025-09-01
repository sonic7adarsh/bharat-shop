-- Migration to add inventory reservations support
-- This migration creates the reservations table for managing temporary stock holds during checkout

-- Reservations table for inventory management
CREATE TABLE reservations (
    id BINARY(16) NOT NULL PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    product_variant_id BINARY(16) NOT NULL,
    quantity INT NOT NULL,
    order_id BINARY(16) NULL, -- Nullable until reservation is committed
    expires_at TIMESTAMP NOT NULL,
    status ENUM('ACTIVE', 'COMMITTED', 'RELEASED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Indexes for efficient querying
    INDEX idx_reservation_tenant_id (tenant_id),
    INDEX idx_reservation_variant_id (product_variant_id),
    INDEX idx_reservation_order_id (order_id),
    INDEX idx_reservation_status (status),
    INDEX idx_reservation_expires_at (expires_at),
    INDEX idx_reservation_tenant_variant (tenant_id, product_variant_id),
    INDEX idx_reservation_tenant_status (tenant_id, status),
    INDEX idx_reservation_variant_status (product_variant_id, status),
    INDEX idx_reservation_created_at (created_at),
    
    -- Composite index for cleanup queries
    INDEX idx_reservation_status_expires (status, expires_at),
    
    -- Composite index for active reservations by variant
    INDEX idx_reservation_active_variant (product_variant_id, status, expires_at),
    
    -- Foreign key constraints
    CONSTRAINT fk_reservation_variant 
        FOREIGN KEY (product_variant_id) 
        REFERENCES product_variants(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_reservation_order 
        FOREIGN KEY (order_id) 
        REFERENCES orders(id) 
        ON DELETE SET NULL,
    
    -- Business logic constraints
    CONSTRAINT chk_reservation_quantity_positive 
        CHECK (quantity > 0),
    
    CONSTRAINT chk_reservation_expires_future 
        CHECK (expires_at > created_at),
    
    -- Ensure order_id is set only when status is COMMITTED
    CONSTRAINT chk_reservation_order_status 
        CHECK (
            (status = 'COMMITTED' AND order_id IS NOT NULL) OR 
            (status IN ('ACTIVE', 'RELEASED') AND order_id IS NULL)
        )
);

-- Add reserved_stock column to product_variants if it doesn't exist
-- This tracks the total reserved quantity for each variant
ALTER TABLE product_variants 
ADD COLUMN IF NOT EXISTS reserved_stock INT NOT NULL DEFAULT 0;

-- Add index on reserved_stock for efficient queries
ALTER TABLE product_variants 
ADD INDEX IF NOT EXISTS idx_product_variant_reserved_stock (reserved_stock);

-- Add composite index for stock calculations
ALTER TABLE product_variants 
ADD INDEX IF NOT EXISTS idx_product_variant_stock_calc (id, stock, reserved_stock);

-- Create a view for available stock calculations (optional, for reporting)
CREATE OR REPLACE VIEW product_variant_stock_view AS
SELECT 
    pv.id,
    pv.tenant_id,
    pv.product_id,
    pv.sku,
    pv.stock as total_stock,
    pv.reserved_stock,
    GREATEST(0, pv.stock - pv.reserved_stock) as available_stock,
    (
        SELECT COALESCE(SUM(r.quantity), 0)
        FROM reservations r 
        WHERE r.product_variant_id = pv.id 
        AND r.status = 'ACTIVE' 
        AND r.expires_at > NOW()
        AND r.tenant_id = pv.tenant_id
    ) as active_reservations,
    pv.status,
    pv.created_at,
    pv.updated_at
FROM product_variants pv
WHERE pv.deleted_at IS NULL;

-- Add comments for documentation
ALTER TABLE reservations COMMENT = 'Inventory reservations for managing temporary stock holds during checkout process';
ALTER TABLE reservations MODIFY COLUMN id BINARY(16) COMMENT 'UUID primary key for reservation';
ALTER TABLE reservations MODIFY COLUMN tenant_id VARCHAR(100) COMMENT 'Tenant identifier for multi-tenancy';
ALTER TABLE reservations MODIFY COLUMN product_variant_id BINARY(16) COMMENT 'Reference to product variant being reserved';
ALTER TABLE reservations MODIFY COLUMN quantity INT COMMENT 'Number of items reserved (must be positive)';
ALTER TABLE reservations MODIFY COLUMN order_id BINARY(16) COMMENT 'Order ID when reservation is committed (nullable until committed)';
ALTER TABLE reservations MODIFY COLUMN expires_at TIMESTAMP COMMENT 'Expiration time for automatic cleanup';
ALTER TABLE reservations MODIFY COLUMN status ENUM('ACTIVE', 'COMMITTED', 'RELEASED') COMMENT 'Reservation status: ACTIVE (temporary hold), COMMITTED (converted to order), RELEASED (cancelled/expired)';