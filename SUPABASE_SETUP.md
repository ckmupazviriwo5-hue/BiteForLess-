# SUPABASE_SETUP.md — Bite For Less backend (Phase 6)

## 1. Prerequisites

- Supabase CLI 2.120.0 (downloaded from GitHub releases to
  `%LOCALAPPDATA%\supabase-cli` — outside the repo; add it to PATH per shell:
  `$env:Path = "$env:LOCALAPPDATA\supabase-cli;" + $env:Path`).
- No Docker in this environment → `supabase start` / `supabase test db` cannot
  run locally. Migrations are validated by `db push --dry-run` + SQL review.

## 2. Project link (needs interactive login first)

```powershell
supabase login            # opens a browser — paste nothing into source
supabase link --project-ref ktvlytdmjqkcdeahgdqy
supabase projects list    # verify the dashboard project appears
```

Verify the project reports reference `ktvlytdmjqkcdeahgdqy` before pushing.

## 3. Inspect before migrating (remote may not be empty)

```powershell
supabase db pull          # inspect remote schema into supabase/migrations
supabase migration list   # compare history
```

If `db pull` reveals existing tables, reconcile — never reset or drop them.

## 4. Apply migrations (non-destructive, authorized)

```powershell
supabase db push --dry-run   # review every statement first
supabase db push             # apply after approval
```

Order: `..._schema.sql` (tables) → `..._logic.sql` (triggers/RPC) →
`..._rls.sql` (policies). Never run `supabase db reset --linked`.

## 5. App configuration (no secrets in git)

`local.properties` (gitignored) holds the public values:

```properties
supabase.url=https://ktvlytdmjqkcdeahgdqy.supabase.co
supabase.anonKey=<publishable key from API settings>
```

Confirm the URL in the dashboard under Project Settings → API (the dashboard
URL itself is NOT the REST URL). Gradle turns these into
`BuildConfig.SUPABASE_URL / SUPABASE_ANON_KEY`; empty = demo mode.
Only the publishable (anon) key ever ships in the app — never service-role.

## 6. Seed + bootstrap (manual, reviewed)

```powershell
# After migrations are verified:
supabase db execute --file supabase/seed.sql
# First admin (edit email in file first):
#   paste supabase/admin_bootstrap.sql into the SQL editor after review
# Cleanup demo rows only (never automatic):
#   supabase db execute --file supabase/demo_cleanup.sql
```

## 7. Future schema changes

New migration per change (`supabase migration new <name>`), review dry-run,
push. Remote reset is destructive and forbidden as a deploy step.
