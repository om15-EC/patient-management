INSERT INTO users (id, email, password, role, enabled, created_at, updated_at)
VALUES
    (
        gen_random_uuid(),
        'admin@example.com',
        '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',  -- password: "password"
        'ROLE_ADMIN',
        TRUE,
        NOW(),
        NOW()
    ),
    (
        gen_random_uuid(),
        'user@example.com',
        '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',  -- password: "password"
        'ROLE_USER',
        TRUE,
        NOW(),
        NOW()
    );