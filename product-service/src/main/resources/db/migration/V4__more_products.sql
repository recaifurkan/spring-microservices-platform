-- ============================================================
-- V4: Geniş e-ticaret kataloğu (6 kategori, 30+ ürün)
-- ============================================================
INSERT INTO products (name, description, price, stock, category, brand, rating, review_count, discount_percent, featured, created_by) VALUES

-- ── ELEKTRONİK ──────────────────────────────────────────────────────────────
('iPhone 15 Pro',
 'A17 Pro çip, 48MP kamera sistemi, titanyum tasarım. Güçlü ProMotion ekran ile kusursuz deneyim.',
 79999.99, 35, 'Elektronik', 'Apple', 4.9, 1247, 0, TRUE, 'system'),

('Samsung Galaxy S24 Ultra',
 '200MP kamera, S Pen, 12GB RAM, 256GB depolama. Galaxy AI yapay zeka özellikleri dahil.',
 64999.99, 42, 'Elektronik', 'Samsung', 4.7, 834, 5, TRUE, 'system'),

('MacBook Air M3',
 'M3 çip ile 18 saate kadar pil ömrü. 8GB RAM, 256GB SSD. Hafif ve taşınabilir.',
 54999.99, 28, 'Elektronik', 'Apple', 4.8, 456, 0, TRUE, 'system'),

('AirPods Pro 2. Nesil',
 'Aktif gürültü engelleme, Adaptif Şeffaflık modu, MagSafe şarj kutusu. 30 saat toplam pil.',
 7499.99, 120, 'Elektronik', 'Apple', 4.7, 2341, 10, FALSE, 'system'),

('Sony WH-1000XM5',
 'Endüstrinin en iyi gürültü engelleme teknolojisi. 30 saat pil, MultiPoint bağlantı.',
 8999.99, 65, 'Elektronik', 'Sony', 4.8, 1567, 15, FALSE, 'system'),

('iPad Air M2',
 '11 inç Liquid Retina, M2 çip, Wi-Fi 6E. Çalışma ve eğlence için mükemmel tablet.',
 29999.99, 55, 'Elektronik', 'Apple', 4.6, 623, 8, FALSE, 'system'),

('Samsung 65" QLED 4K TV',
 'Quantum HDR, Dolby Atmos, Gaming Hub. Oyun ve film izleme mütevazı bir deneyim sunar.',
 32999.99, 20, 'Elektronik', 'Samsung', 4.5, 345, 20, TRUE, 'system'),

('DJI Mini 4 Pro Drone',
 '4K/60fps kamera, 20 km iletişim menzili, 34 dakika uçuş süresi. Katlanabilir tasarım.',
 21999.99, 18, 'Elektronik', 'DJI', 4.6, 234, 0, FALSE, 'system'),

-- ── GİYİM & MODA ─────────────────────────────────────────────────────────────
('Slim Fit Denim Pantolon',
 '% 98 pamuk, % 2 elastan içerikli. Beş cep tasarımı, kolay bakım. Birden fazla renk seçeneği.',
 899.99, 200, 'Giyim & Moda', 'Levi''s', 4.2, 1456, 0, FALSE, 'system'),

('Oversize Kapüşonlu Sweatshirt',
 'Yumuşak % 100 pamuk. Unisex tasarım, 5 renk seçeneği. Ön kanguru cebi.',
 649.99, 350, 'Giyim & Moda', 'Zara', 4.4, 892, 20, TRUE, 'system'),

('Deri Bikers Ceket',
 'Gerçek inek derisi, YKK fermuar, iç cepli. Klasik motosiklet stili, günlük kullanım.',
 3999.99, 45, 'Giyim & Moda', 'Pull&Bear', 4.3, 267, 15, FALSE, 'system'),

('Spor Koşu Taytı',
 'Hızlı kuruyucu kumaş, derin cepler. Reflektif detaylar, gece görünürlüğü. 4 renk.',
 549.99, 180, 'Giyim & Moda', 'Nike', 4.6, 1023, 0, FALSE, 'system'),

('Linen Yazlık Gömlek',
 '% 100 keten kumaş, nefes alabilir. Gevşek kesim, casual stil. Beyaz, bej, mavi.',
 449.99, 230, 'Giyim & Moda', 'Mango', 4.1, 543, 10, FALSE, 'system'),

('Yün Karışımlı Palto',
 '60% yün 40% polyester. Double-breasted, tam astar, kış için ideal. 3 renk.',
 2799.99, 60, 'Giyim & Moda', 'Massimo Dutti', 4.5, 312, 25, TRUE, 'system'),

-- ── SPOR & OUTDOOR ──────────────────────────────────────────────────────────
('Nike Air Max 270',
 '270° Air yastıklama teknolojisi. Nefes alabilir mesh üst, hafif taban. 6 renk seçeneği.',
 3299.99, 85, 'Spor & Outdoor', 'Nike', 4.7, 2134, 10, TRUE, 'system'),

('Yoga & Pilates Mat Pro',
 '6mm TPE ekstra kalın. Kaymaz yüzey, çevre dostu malzeme. Taşıma kayışı hediye.',
 499.99, 150, 'Spor & Outdoor', 'Manduka', 4.5, 876, 0, FALSE, 'system'),

('Adjustable Dumbbell Seti 40kg',
 'Hızlı ayarlı sistem, 2.5kg''dan 20kg''a 15 farklı ağırlık. Kompakt depolama.',
 3799.99, 30, 'Spor & Outdoor', 'Bowflex', 4.4, 456, 5, FALSE, 'system'),

('Akıllı Fitness Bilekliği',
 '7 gün pil, kalp ritmi ölçümü, SpO2, 100+ spor modu, GPS. Su geçirmez IP68.',
 2499.99, 95, 'Spor & Outdoor', 'Fitbit', 4.3, 1234, 12, FALSE, 'system'),

('Bisiklet Kaskı MTB',
 'CPSC ve CE onaylı. 18 havalandırma kanalı, LED stop lambası, ayarlanabilir vizör.',
 899.99, 70, 'Spor & Outdoor', 'Giro', 4.6, 345, 0, FALSE, 'system'),

-- ── KİTAP ────────────────────────────────────────────────────────────────────
('Sapiens: İnsan Türünün Kısa Tarihi',
 'Yuval Noah Harari imzalı. BilişselDevrim''den Yapay Zeka çağına insanlığın yolculuğu. Ciltli.',
 189.99, 500, 'Kitap', 'Yuval Noah Harari', 4.9, 8934, 0, TRUE, 'system'),

('Atomik Alışkanlıklar',
 'James Clear. Küçük değişiklikler, büyük farklar. Kötü alışkanlıklardan kurtulmanın bilimi.',
 129.99, 450, 'Kitap', 'James Clear', 4.8, 6721, 0, TRUE, 'system'),

('Dune',
 'Frank Herbert. Bilim kurgunun başyapıtı. Çöl gezegeni Arrakis''in efsanesi. Yeni çeviri, ciltli.',
 149.99, 200, 'Kitap', 'Frank Herbert', 4.7, 3456, 5, FALSE, 'system'),

('Suç ve Ceza',
 'Fyodor Dostoyevski. Rus edebiyatının zirvesi. Yeni çeviri ve açıklamalı notlar içerir.',
 99.99, 300, 'Kitap', 'Dostoyevski', 4.7, 2341, 0, FALSE, 'system'),

-- ── EV & YAŞAM ──────────────────────────────────────────────────────────────
('Xiaomi Robot Süpürge X20 Pro',
 'LiDAR navigasyon, 7000PA emiş gücü, App kontrolü, sesli komut. 3 saatte tam şarj.',
 12999.99, 25, 'Ev & Yaşam', 'Xiaomi', 4.5, 789, 25, TRUE, 'system'),

('Philips Hava Fritözü 7L',
 '7 litre kapasiteli, dijital dokunmatik ekran, 9 pişirme modu. Neredeyse sıfır yağ.',
 2999.99, 80, 'Ev & Yaşam', 'Philips', 4.6, 1234, 10, FALSE, 'system'),

('DeLonghi Tam Otomatik Kahve Makinesi',
 'Entegre değirmen, 20 bar pompa, latte art özelliği. Cappuccino, espresso, americano.',
 14999.99, 20, 'Ev & Yaşam', 'DeLonghi', 4.7, 456, 5, TRUE, 'system'),

('Philips Hue Akıllı Ampul Set 4''lü',
 'Uygulama ve ses kontrolü, 16 milyon renk, Zigbee 3.0. Philips Hue Bridge gerektirir.',
 1299.99, 100, 'Ev & Yaşam', 'Philips', 4.3, 678, 0, FALSE, 'system'),

-- ── KOZMETİK & KİŞİSEL BAKIM ─────────────────────────────────────────────
('CeraVe Nemlendrici Krem 454g',
 '3 esansiyel seram, hyalüronik asit, MVE teknolojisi. Kuru ve çok kuru cilt için. Parfümsüz.',
 449.99, 300, 'Kozmetik & Kişisel Bakım', 'CeraVe', 4.8, 4521, 0, TRUE, 'system'),

('La Roche-Posay Anthelios SPF 50+',
 'Ultra hafif doku, nemlendirir ve korur. UVA/UVB filtreOlar. Hassas cilt için test edildi.',
 429.99, 250, 'Kozmetik & Kişisel Bakım', 'La Roche-Posay', 4.7, 2341, 0, FALSE, 'system'),

('The Ordinary Niacinamide % 10',
 'Gözenek görünümünü azaltır, cilt tonunu eşitler. Vegan ve cruelty-free formül.',
 249.99, 400, 'Kozmetik & Kişisel Bakım', 'The Ordinary', 4.6, 5678, 15, FALSE, 'system'),

('Pantene Pro-V Saç Bakım Seti',
 'Şampuan 400ml + saç kremi 400ml + saç maskesi 200ml. Keratin ve biotin takviyeli.',
 599.99, 180, 'Kozmetik & Kişisel Bakım', 'Pantene', 4.3, 1234, 20, FALSE, 'system');

