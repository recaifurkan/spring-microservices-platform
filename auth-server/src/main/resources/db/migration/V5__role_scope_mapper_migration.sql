-- ============================================================
-- V5: RoleScopeMapper geçişi
--
-- ESKİ YAKLAŞIM (V4):
--   user_grants tablosunda hem ROLE_ hem SCOPE_ kayıtları vardı.
--   Her kullanıcıya tek tek SCOPE_catalog:read, SCOPE_orders:write...
--   gibi 10-15 satır grant ekleniyordu.
--   → Yeni scope eklenince hem migration hem tüm servisler güncellenmek zorundaydı.
--
-- YENİ YAKLAŞIM (V5 sonrası):
--   user_grants tablosunda sadece ROLE_ kayıtları tutulur.
--   Hangi rolün hangi scope'ları kazandırdığı auth-server'daki
--   RoleScopeMapper.java'da tek merkezde tanımlanır.
--   Token customizer bu map'i kullanarak JWT'ye scope'ları yazar.
--   → Yeni rol/scope eklemek = sadece RoleScopeMapper'a 1 case eklemek.
--
-- KULLANICI → ROL EŞLEMESİ:
--   admin        → ROLE_ADMIN           (tüm yetkiler)
--   manager      → ROLE_MANAGER         (mağaza yöneticisi)
--   user         → ROLE_USER            (standart müşteri)
--   readonly     → ROLE_USER            (standart müşteri, sadece okuma için ayrı akış yok artık)
--   premium_user → ROLE_PREMIUM_USER    (ileride: premium özellikler)
-- ============================================================

-- 1) Tüm SCOPE_ grant'larını kaldır — artık RoleScopeMapper yönetecek
DELETE FROM user_grants WHERE authority LIKE 'SCOPE_%';

-- 2) Eksik ROLE_ grant'larını tamamla (idempotent)

-- admin → ROLE_ADMIN
INSERT INTO user_grants (user_id, authority)
SELECT u.id, 'ROLE_ADMIN'
FROM app_users u
WHERE u.username = 'admin'
  AND NOT EXISTS (
    SELECT 1 FROM user_grants ug WHERE ug.user_id = u.id AND ug.authority = 'ROLE_ADMIN'
  );

-- manager → ROLE_MANAGER
INSERT INTO user_grants (user_id, authority)
SELECT u.id, 'ROLE_MANAGER'
FROM app_users u
WHERE u.username = 'manager'
  AND NOT EXISTS (
    SELECT 1 FROM user_grants ug WHERE ug.user_id = u.id AND ug.authority = 'ROLE_MANAGER'
  );

-- user → ROLE_USER
INSERT INTO user_grants (user_id, authority)
SELECT u.id, 'ROLE_USER'
FROM app_users u
WHERE u.username = 'user'
  AND NOT EXISTS (
    SELECT 1 FROM user_grants ug WHERE ug.user_id = u.id AND ug.authority = 'ROLE_USER'
  );

-- readonly → ROLE_USER  (RoleScopeMapper'da okuma scope'larına zaten sahip)
INSERT INTO user_grants (user_id, authority)
SELECT u.id, 'ROLE_USER'
FROM app_users u
WHERE u.username = 'readonly'
  AND NOT EXISTS (
    SELECT 1 FROM user_grants ug WHERE ug.user_id = u.id AND ug.authority = 'ROLE_USER'
  );

-- 3) Premium kullanıcı örneği — ileride ROLE_PREMIUM_USER'ı test etmek için
INSERT INTO app_users (username, password, enabled, email, full_name)
SELECT 'premium', '{noop}premium123', TRUE, 'premium@example.com', 'Premium User'
WHERE NOT EXISTS (SELECT 1 FROM app_users WHERE username = 'premium');

INSERT INTO user_grants (user_id, authority)
SELECT u.id, 'ROLE_PREMIUM_USER'
FROM app_users u
WHERE u.username = 'premium'
  AND NOT EXISTS (
    SELECT 1 FROM user_grants ug WHERE ug.user_id = u.id AND ug.authority = 'ROLE_PREMIUM_USER'
  );

-- ============================================================
-- Sonuç: user_grants tablosu artık çok daha temiz
--
-- admin    | ROLE_ADMIN
-- manager  | ROLE_MANAGER
-- user     | ROLE_USER
-- readonly | ROLE_USER
-- premium  | ROLE_PREMIUM_USER
--
-- Yeni bir rol eklemek için:
--   1. RoleScopeMapper.java'ya case ekle          ← tek değişiklik
--   2. Yeni kullanıcıya ROLE_xxx grant'ı ver      ← DB insert
--   3. Hiçbir servise dokunma                     ← servisler SCOPE_xxx kontrol eder
-- ============================================================

