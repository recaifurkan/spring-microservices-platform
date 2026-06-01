-- ============================================================
-- Başlangıç kullanıcı verileri
--
-- Scope/Role modeli (Endüstri Standardı):
--   SCOPE_api.read  → Okuma API erişimi  (OAuth2 scope — client yetkilendirmesi)
--   SCOPE_api.write → Yazma API erişimi  (OAuth2 scope — client yetkilendirmesi)
--   ROLE_USER       → Normal kullanıcı   (RBAC role  — user yetkilendirmesi)
--   ROLE_ADMIN      → Sistem yöneticisi  (RBAC role  — user yetkilendirmesi)
--
-- Kullanıcılar:
--   admin    / admin123   → api.read + api.write + ROLE_ADMIN
--   user     / user123    → api.read + api.write + ROLE_USER
--   readonly / read123    → api.read              + ROLE_USER  (sadece okuma)
-- ============================================================

INSERT INTO app_users (username, password, enabled, email, full_name) VALUES
    ('admin',    '{noop}admin123',   TRUE, 'admin@example.com',    'Admin User'),
    ('user',     '{noop}user123',    TRUE, 'user@example.com',     'Regular User'),
    ('readonly', '{noop}read123',    TRUE, 'readonly@example.com', 'Read Only User');

-- ── admin → tam yetki ──────────────────────────────────────────────
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_api.read'  FROM app_users WHERE username = 'admin';
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_api.write' FROM app_users WHERE username = 'admin';
INSERT INTO user_grants (user_id, authority) SELECT id, 'ROLE_ADMIN'      FROM app_users WHERE username = 'admin';

-- ── user → okuma + yazma, normal kullanıcı ────────────────────────
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_api.read'  FROM app_users WHERE username = 'user';
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_api.write' FROM app_users WHERE username = 'user';
INSERT INTO user_grants (user_id, authority) SELECT id, 'ROLE_USER'       FROM app_users WHERE username = 'user';

-- ── readonly → yalnızca okuma ─────────────────────────────────────
INSERT INTO user_grants (user_id, authority) SELECT id, 'SCOPE_api.read' FROM app_users WHERE username = 'readonly';
INSERT INTO user_grants (user_id, authority) SELECT id, 'ROLE_USER'      FROM app_users WHERE username = 'readonly';
