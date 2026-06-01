-- ============================================================
-- V4: Granüler scope sistemi (Netflix/e-commerce tarzı)
--
-- ESKİ: api.read / api.write  (çok geniş, anlamsız)
-- YENİ: catalog:read, orders:write, payments:manage ... (resource-based)
--
-- Scope → Kim yapabilir:
--   catalog:read        → Herkes (public, JWT bile gerekmez)
--   catalog:write       → MANAGER, ADMIN
--   orders:read         → USER (kendi siparişleri)
--   orders:write        → USER (sipariş aç/iptal)
--   orders:manage       → MANAGER, ADMIN (tüm siparişler, durum güncelle)
--   cart:read           → USER
--   cart:write          → USER
--   payments:read       → USER (kendi ödemeleri)
--   payments:write      → USER (ödeme başlat)
--   payments:manage     → MANAGER, ADMIN (onay, iade)
--   users:read          → USER (kendi profili)
--   users:write         → USER (profil güncelle)
--   users:manage        → ADMIN (tüm kullanıcılar)
--   notifications:read  → USER
--   notifications:write → servisler arası (internal)
-- ============================================================

-- Kayıtlı OAuth2 client'ları sil (DataInitializer yeni scope'larla yeniden oluşturacak)
DELETE FROM oauth2_authorization_consent;
DELETE FROM oauth2_authorization;
DELETE FROM oauth2_registered_client;

-- Eski scope grant'larını temizle
DELETE FROM user_grants WHERE authority LIKE 'SCOPE_%';

-- ── admin → tam yetki (tüm scope'lar + ROLE_ADMIN) ────────────────
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_catalog:read'       FROM app_users WHERE username = 'admin';
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_catalog:write'      FROM app_users WHERE username = 'admin';
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_orders:read'        FROM app_users WHERE username = 'admin';
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_orders:write'       FROM app_users WHERE username = 'admin';
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_orders:manage'      FROM app_users WHERE username = 'admin';
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_cart:read'          FROM app_users WHERE username = 'admin';
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_cart:write'         FROM app_users WHERE username = 'admin';
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_payments:read'      FROM app_users WHERE username = 'admin';
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_payments:write'     FROM app_users WHERE username = 'admin';
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_payments:manage'    FROM app_users WHERE username = 'admin';
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_users:read'         FROM app_users WHERE username = 'admin';
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_users:write'        FROM app_users WHERE username = 'admin';
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_users:manage'       FROM app_users WHERE username = 'admin';
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_notifications:read' FROM app_users WHERE username = 'admin';
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_notifications:write'FROM app_users WHERE username = 'admin';

-- ── manager kullanıcısı ekle (yoksa) ──────────────────────────────
INSERT INTO app_users (username, password, enabled, email, full_name)
SELECT 'manager', '{noop}manager123', TRUE, 'manager@example.com', 'Store Manager'
WHERE NOT EXISTS (SELECT 1 FROM app_users WHERE username = 'manager');

-- ── manager → mağaza yöneticisi (catalog:write + orders:manage + payments:manage) ──
INSERT INTO user_grants (user_id, authority) SELECT id, 'ROLE_MANAGER'              FROM app_users WHERE username = 'manager' AND NOT EXISTS (SELECT 1 FROM user_grants ug JOIN app_users u ON ug.user_id=u.id WHERE u.username='manager' AND ug.authority='ROLE_MANAGER');
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_catalog:read'        FROM app_users WHERE username = 'manager' AND NOT EXISTS (SELECT 1 FROM user_grants ug JOIN app_users u ON ug.user_id=u.id WHERE u.username='manager' AND ug.authority='SCOPE_catalog:read');
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_catalog:write'       FROM app_users WHERE username = 'manager' AND NOT EXISTS (SELECT 1 FROM user_grants ug JOIN app_users u ON ug.user_id=u.id WHERE u.username='manager' AND ug.authority='SCOPE_catalog:write');
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_orders:read'         FROM app_users WHERE username = 'manager' AND NOT EXISTS (SELECT 1 FROM user_grants ug JOIN app_users u ON ug.user_id=u.id WHERE u.username='manager' AND ug.authority='SCOPE_orders:read');
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_orders:write'        FROM app_users WHERE username = 'manager' AND NOT EXISTS (SELECT 1 FROM user_grants ug JOIN app_users u ON ug.user_id=u.id WHERE u.username='manager' AND ug.authority='SCOPE_orders:write');
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_orders:manage'       FROM app_users WHERE username = 'manager' AND NOT EXISTS (SELECT 1 FROM user_grants ug JOIN app_users u ON ug.user_id=u.id WHERE u.username='manager' AND ug.authority='SCOPE_orders:manage');
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_cart:read'           FROM app_users WHERE username = 'manager' AND NOT EXISTS (SELECT 1 FROM user_grants ug JOIN app_users u ON ug.user_id=u.id WHERE u.username='manager' AND ug.authority='SCOPE_cart:read');
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_cart:write'          FROM app_users WHERE username = 'manager' AND NOT EXISTS (SELECT 1 FROM user_grants ug JOIN app_users u ON ug.user_id=u.id WHERE u.username='manager' AND ug.authority='SCOPE_cart:write');
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_payments:read'       FROM app_users WHERE username = 'manager' AND NOT EXISTS (SELECT 1 FROM user_grants ug JOIN app_users u ON ug.user_id=u.id WHERE u.username='manager' AND ug.authority='SCOPE_payments:read');
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_payments:write'      FROM app_users WHERE username = 'manager' AND NOT EXISTS (SELECT 1 FROM user_grants ug JOIN app_users u ON ug.user_id=u.id WHERE u.username='manager' AND ug.authority='SCOPE_payments:write');
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_payments:manage'     FROM app_users WHERE username = 'manager' AND NOT EXISTS (SELECT 1 FROM user_grants ug JOIN app_users u ON ug.user_id=u.id WHERE u.username='manager' AND ug.authority='SCOPE_payments:manage');
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_users:read'          FROM app_users WHERE username = 'manager' AND NOT EXISTS (SELECT 1 FROM user_grants ug JOIN app_users u ON ug.user_id=u.id WHERE u.username='manager' AND ug.authority='SCOPE_users:read');
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_users:write'         FROM app_users WHERE username = 'manager' AND NOT EXISTS (SELECT 1 FROM user_grants ug JOIN app_users u ON ug.user_id=u.id WHERE u.username='manager' AND ug.authority='SCOPE_users:write');
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_notifications:read'  FROM app_users WHERE username = 'manager' AND NOT EXISTS (SELECT 1 FROM user_grants ug JOIN app_users u ON ug.user_id=u.id WHERE u.username='manager' AND ug.authority='SCOPE_notifications:read');

-- ── user → normal müşteri (alışveriş yapabilir) ───────────────────
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_catalog:read'       FROM app_users WHERE username = 'user';
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_orders:read'        FROM app_users WHERE username = 'user';
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_orders:write'       FROM app_users WHERE username = 'user';
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_cart:read'          FROM app_users WHERE username = 'user';
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_cart:write'         FROM app_users WHERE username = 'user';
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_payments:read'      FROM app_users WHERE username = 'user';
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_payments:write'     FROM app_users WHERE username = 'user';
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_users:read'         FROM app_users WHERE username = 'user';
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_users:write'        FROM app_users WHERE username = 'user';
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_notifications:read' FROM app_users WHERE username = 'user';

-- ── readonly → sadece okuma ────────────────────────────────────────
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_catalog:read'       FROM app_users WHERE username = 'readonly';
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_orders:read'        FROM app_users WHERE username = 'readonly';
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_payments:read'      FROM app_users WHERE username = 'readonly';
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_users:read'         FROM app_users WHERE username = 'readonly';
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_notifications:read' FROM app_users WHERE username = 'readonly';

