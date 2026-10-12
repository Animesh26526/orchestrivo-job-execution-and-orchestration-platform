-- V5__user_full_name.sql
-- Additive schema migration for Stage 4.2: Full name field for user registration

ALTER TABLE users
    ADD COLUMN full_name VARCHAR(255) NULL AFTER email;
