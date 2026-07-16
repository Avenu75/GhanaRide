INSERT INTO users (
    username,
    full_name,
    email,
    password,
    phone_number,
    role,
    account_type,
    email_verified,
    enabled,
    account_locked,
    created_at,
    updated_at,
    password_changed_at
)
SELECT
    'admin',
    'Administrator',
    'admin@ghanaride.local',
    '$2b$12$r2GF9DV5MTfPpyXM4XywKuzBrRI9R6IsJv6JeA/ohbcnRT3lRFUk.',
    '0200000000',
    'ADMIN',
    'ADMIN',
    b'1',
    b'1',
    b'0',
    NOW(),
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE username = 'admin'
);

INSERT INTO wallets (
    user_id,
    balance,
    loyalty_points,
    currency,
    is_active,
    created_at,
    updated_at
)
SELECT
    u.id,
    0.00,
    0.00,
    'GHS',
    b'1',
    NOW(),
    NOW()
FROM users u
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM wallets w WHERE w.user_id = u.id
  );
