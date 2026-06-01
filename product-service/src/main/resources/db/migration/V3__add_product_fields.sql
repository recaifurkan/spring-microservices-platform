-- ============================================================
-- V3: Ürün tablosuna e-ticaret alanları ekle
-- brand, imageUrl, rating, reviewCount, discountPercent, featured
-- ============================================================
ALTER TABLE products ADD COLUMN IF NOT EXISTS brand           VARCHAR(100);
ALTER TABLE products ADD COLUMN IF NOT EXISTS image_url       VARCHAR(600);
ALTER TABLE products ADD COLUMN IF NOT EXISTS rating          FLOAT   NOT NULL DEFAULT 4.0;
ALTER TABLE products ADD COLUMN IF NOT EXISTS review_count    INT     NOT NULL DEFAULT 0;
ALTER TABLE products ADD COLUMN IF NOT EXISTS discount_percent INT    NOT NULL DEFAULT 0;
ALTER TABLE products ADD COLUMN IF NOT EXISTS featured        BOOLEAN NOT NULL DEFAULT FALSE;

-- Mevcut 6 ürünü güncelle
UPDATE products SET brand='TechBrand', rating=4.5, review_count=128, discount_percent=10, featured=TRUE  WHERE name='Laptop Pro 15';
UPDATE products SET brand='Logitech',  rating=4.3, review_count=256, discount_percent=5,  featured=FALSE WHERE name='Wireless Mouse';
UPDATE products SET brand='Corsair',   rating=4.7, review_count=89,  discount_percent=0,  featured=TRUE  WHERE name='Mechanical Keyboard';
UPDATE products SET brand='Anker',     rating=4.1, review_count=312, discount_percent=15, featured=FALSE WHERE name='USB-C Hub';
UPDATE products SET brand='Samsung',   rating=4.8, review_count=67,  discount_percent=20, featured=TRUE  WHERE name='4K Monitor';
UPDATE products SET brand='Logitech',  rating=4.2, review_count=145, discount_percent=8,  featured=FALSE WHERE name='Webcam HD';

