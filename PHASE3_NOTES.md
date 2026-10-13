# Phase 3 — Customer marketplace (implemented 2026-10-13)

## What works
- Shared in-memory `DemoRepository` (singleton) implements `ListingRepository` +
  `ReservationRepository`: 12 seeded listings / 5 vendors / 6 categories.
- Deals: RecyclerView + `DealAdapter`, case-insensitive search (name/vendor),
  category chips, max-price, min-discount, available-only, 5 sort orders.
  Sorting never mutates repository order. Unavailable/expired cards are dimmed
  and badged but still open details explaining why.
- Detail (`DealDetailActivity`): full info, qty stepper capped at live stock,
  stock rechecked inside `repo.create()` at reserve time; errors shown inline.
- Confirmation dialog shows reference, qty, total, location, deadline, status;
  navigates to history. No payment, no secure QR/PIN (demo reference only).
- History: per-user reservations with cancel (confirm dialog); cancel restores
  stock once; completed/cancelled/expired cannot re-cancel; expiry sweep runs
  on resume (`markExpiredReservations`).
- Home: greeting, top-3 featured by discount, category shortcuts, browse-all.
- Toolbar "Reset demo data" now functional (confirm → reseed → recreate).

## Known limitations (Phase 6+)
- In-memory only: state lost on process restart; no cross-device sync, no real
  concurrency protection. Room migration keeps the same interfaces.
- Reservation `create()` additionally blocks non-APPROVED vendors (suspend/
  pending demo); affected seed listings are INACTIVE so UI stays consistent.
- No instrumented tests yet (no AVD/device); unit coverage is pure-Java rules.

## Interface change (from Phase 2)
- `ReservationRepository.getAll()` → `getAllReservations()`,
  `getById()` → `getReservationById()`: the old names clashed (same erasure)
  with `ListingRepository`, so one class could not implement both. Documented
  in the interface javadoc; covered by tests.
