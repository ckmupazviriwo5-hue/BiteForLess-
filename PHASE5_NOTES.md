# Phase 5 — Admin dashboard (implemented 2026-10-13)

## What works
- Dashboard (`AdminDashboardFragment`): 6 real metric cards (`AdminStats.compute`:
  vendors, approved/suspended, active/total listings, reservations, meals
  awaiting, est. sales) + platform summary line. Entry points to 6 screens.
- Vendors (`AdminVendorsActivity`): directory with status filter, listing counts,
  detail dialog, Approve/Suspend/Reactivate with confirmations. Canonical
  `Vendor.setStatus` via `setVendorStatus` + activity log. Suspension preserves
  reservations and stock; only new business is gated.
- Repository gates: `createListing`/`updateListing`/activation require APPROVED
  vendor; customer `create()` already gated; completing existing pickups stays
  allowed after suspension (explicit policy: fulfil obligations, no restock).
- Listings (`AdminListingsActivity`): search + category + new availability buckets
  (`ListingQuery.applyForAdmin`: Bookable/Paused/Sold out/Expired, priority
  order, tested). Details via customer screen; safe deactivate/reactivate via
  `setListingStatusByAdmin` (activation refused for unapproved vendors).
- Reservations (`AdminReservationsActivity`): view-only overview, search
  (ref/food/vendor) + status filter. No admin cancellation this phase —
  lifecycle rules apply; documented limitation.
- Customers (`AdminCustomersActivity`): distinct demo IDs from reservation
  records only (counts, completed, last activity) + honest empty state.
- Reviews (`AdminReviewsActivity`): 4 seeded sample reviews (2 visible, 2
  pending), hide/show/approve with confirmations; records preserved;
  `getVisibleReviewsForListing` is the customer-safe view (no customer review
  display exists yet — hidden exclusion enforced at repository level, tested).
- Activity log (`AdminActivityLogActivity` + `ActivityEvent`): newest-first,
  id/type/time/actor; records real vendor/listing/review actions only —
  never pre-seeded; cleared on demo reset.
- All admin changes visible in vendor/customer views (same singleton records).

## Known limitations (Phase 6+)
- No admin reservation cancellation (would need a validated domain op).
- No customer-facing review display yet (repository filter ready).
- In-memory state; no instrumented tests (no AVD/device).
