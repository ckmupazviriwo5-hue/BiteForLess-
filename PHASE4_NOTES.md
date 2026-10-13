# Phase 4 — Vendor dashboard (implemented 2026-10-13)

## What works
- Demo vendor identity: explicit `SessionManager` mapping (vendor role → seeded
  `v1` Ubuntu Bakery via `requireDemoVendorId()`). Labelled simulated, not security.
- Dashboard (`VendorDashboardFragment`): identity header + 6 real metric cards
  (`VendorStats.compute` over shared repo: est. sales, pickups, awaiting pickup,
  active/total listings, stock awaiting, reservations) + food-rescue line
  (meals collected, customer savings). Add-listing + reservations entry points.
- Listing CRUD (`ListingFormActivity`, reusable create/edit): name, description,
  category spinner, prices with live discount preview, qty, location, window,
  deadline spinner (keep-current on edit), active switch. Inline errors +
  repository recheck at save; Cancel never mutates.
- Repository guards (`DemoRepository`, same interfaces + additions): ownership
  checks on update/status/complete; create validates vendor/category/price/qty/
  location/deadline; `updateListing` refuses stock below outstanding RESERVED qty;
  `setListingStatus` deactivation blocks new reservations, history untouched;
  `completeForVendor` enforces own-vendor + RESERVED-only, never restores stock.
- Reservations (`VendorReservationsActivity`): own-listing reservations with
  All/Reserved/Completed/Cancelled/Expired filter, customer demo id, simulated
  pickup (confirm dialog, labelled demo).
- Customer screens read the same records: vendor creates/edits/deactivations
  and completions are visible in marketplace + histories (verified by tests and
  onResume refreshes).

## Known limitations (Phase 5/6)
- No listing delete UI (protects reservation history; `delete()` reserved).
- No image upload (placeholder only); in-memory state (Phase 6 Room).
- No instrumented tests (no AVD/device).

## Model note
- No model changes needed: `ListingStatus` covers deactivation; sales/pickups
  derive from existing reservation fields. Smallest-change principle kept.
