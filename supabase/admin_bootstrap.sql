-- Bite For Less — administrator bootstrap (MANUAL, privileged environment)
-- Run AFTER you have signed up in the app. Replace the email, review, then run.
-- Fails safely when the account is missing or ambiguous. Promotes NOBODY else.

-- 1. Inspect the candidate (expect exactly one row):
-- SELECT id, email, name, role, created_at FROM public.profiles
-- WHERE email = 'you@example.com';

-- 2. Promote exactly that account (uncomment + set email, then run):
-- UPDATE public.profiles
-- SET role = 'admin'
-- WHERE email = 'you@example.com'
--   AND role IS DISTINCT FROM 'admin';
--
-- Expected result: "UPDATE 1". If it says "UPDATE 0", the account does not
-- exist (sign up first) or is already an admin.

-- 3. Verify:
-- SELECT id, email, role FROM public.profiles WHERE email = 'you@example.com';

-- Safety notes:
-- - Run this only in the Supabase SQL editor or another trusted, privileged
--   environment (service role / postgres), never from the Android app.
-- - Never promote by role ('customer' → all customers) or by domain.
-- - The app's public signup trigger always creates 'customer' profiles, and
--   the role guard blocks client-side escalation, so this file is the only
--   supported path to the first admin.
