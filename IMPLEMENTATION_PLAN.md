# IMPLEMENTATION_PLAN.md — Bite For Less Android (proposal, Phase 1)

> Status: PROPOSED — no feature work started. Awaiting your review of `PROJECT_AUDIT.md` + this plan before Phase 2.
> Constraints honoured: local-only, no backend/cloud DB/auth/API/payments, no push/commit/deploy without approval.

## 1. Approved starting point (from audit)

Keep: Gradle 9.5.0 + AGP 9.3.1 + Java 11 + `compileSdk 36.1 / target 36 / min 24`, Material3 dark theme + food-orange identity, `Splash → Login/Signup → Main` flow shape, package `com.example.biteforlessfoodbridge` for now.

## 2. Proposed package structure (adapt, don't restructure for its own sake)

```
com.example.biteforlessfoodbridge/
  model/        // FoodListing, Vendor, Reservation(+Status), User(+Role), Review, NotificationItem, Category
  repository/   // interfaces: ListingRepository, ReservationRepository, VendorRepository, UserRepository, ReviewRepository, NotificationRepository
  data/         // Room: AppDatabase, DAOs, entities; DemoDataSeeder; DemoDataReset; SessionManager (SharedPreferences, demo role only)
  ui/
    splash/     // SplashActivity (migrated to AndroidX SplashScreen, no fixed delay)
    auth/       // LoginActivity, SignupActivity (validated, simulated roles)
    customer/   // HomeFragment, DealsFragment, DealDetailActivity/Fragment, ReservationsFragment, ReservationDetail, ProfileFragment
    vendor/     // VendorDashboardFragment/Activity, ListingEditDialog/Activity, VendorReservations
    admin/      // AdminDashboard, VendorDirectory, CustomerDirectory, ListingModeration, ReservationOverview, ReviewModeration, Analytics, ActivityLog
    common/     // adapters (DealAdapter…), dialogs, views, BottomNav MainActivity
  util/         // Validation, PriceFormat, DateTimeUtil, QuantityRules, ReservationRules
```

## 3. Data & business rules (local-only)

- One Room database, one DAO per entity; all screens go through repositories — no duplicate in-memory copies.
- Entities (draft): `Category(id,name)`, `Vendor(id,name,location,status[PENDING|APPROVED|SUSPENDED])`, `FoodListing(id,vendorId,name,desc,category,origPrice,discPrice,qty,status[ACTIVE|INACTIVE],expiryEpoch,collectionWindow,location,imageKey)`, `Reservation(id,listingId,qty,status[RESERVED|COMPLETED|CANCELLED|EXPIRED],createdAt,pickupCode,demoPin)`, `User(id,name,email,role[CUSTOMER|VENDOR|ADMIN])`, `Review(id,listingId,rating,text,status[VISIBLE|HIDDEN|PENDING])`, `ActivityEvent(id,type,text,at)`, notifications derived from reservation events.
- Reservation invariants: create decrements stock; cancel/expiry restores **once** (guarded by status transition check inside a Room `@Transaction`); complete never restores; repeated cancel/complete is a no-op; qty must be ≥1 and ≤ available; expired/inactive/zero-stock listings block reservation with a clear message.
- Seed ~8–12 listings across 3–5 vendors + categories + reviews; `Reset demo data` reseeds to known state (destructive, with confirm dialog).
- Document in-app + README that local storage cannot sync devices, enforce server auth, or guarantee global inventory.

## 4. Phase-by-phase build (each phase: implement → build → test → report)

- **Phase 2 — Foundation & design**: theme/tokens (`colors/dimens/styles`), reusable layouts (food card, section header, empty/error states), `MainActivity` + BottomNavigation + Fragments skeleton, `model` classes, `util` (validation/price/date), AndroidX SplashScreen, simulated role entry. Entry criteria: plan approved + design-reference question answered.
- **Phase 3 — Customer marketplace**: deals list (RecyclerView + adapter), search (name/vendor), filter (category/price/discount/availability), sort, detail (prices/savings/qty/vendor/window/expiry), qty selector, reserve-local, confirmation, history (active/completed/cancelled/expired), cancel-eligible, profile edit (local demo).
- **Phase 4 — Vendor dashboard**: metrics (listings/reservations/pickups/est. sales), CRUD listings + validation, stock/reservation status, reservation list + simulated confirm/pickup, waste-reduction summary. Same inventory as customer.
- **Phase 5 — Admin dashboard**: overview metrics, vendor approve/suspend, customer directory, listing management, reservation overview, review/complaint moderation, category/status management, activity log, analytics from local data. Phone-friendly cards/dialogs.
- **Phase 6 — Functionalization & persistence**: wire all screens to Room repositories, transactions, validation, cancel/expiry/pickup transitions, restart persistence, reset-demo-data, rotation-safe state (ViewModel/SavedState where justified).
- **Phase 7 — Testing & APK**: `assembleDebug`, `testDebugUnitTest`, targeted instrumented tests; emulator/device install; doc exact APK path; `BUILD_RUNBOOK.md` (open in Studio, reproduce build, reset demo). Debug APK only unless signing config is supplied + approved.

## 5. Testing strategy (new tests, not just defaults)

Unit (`app/src/test`): price/discount math, validation (email/price/qty/deadline), reservation rules (sufficient/insufficient stock, cancel-restore-once, double-cancel no-op, complete-no-restore, expiry), vendor-edit propagation logic, reset behaviour.
Instrumented (`androidTest`): startup/nav, deal → reserve → history → cancel flow, rotation smoke, list→detail, admin/vendor action reflected in customer view (via shared Repo/DB).
Matrix: build success, invalid forms, insufficient stock, repeated actions, expiry/availability, restart persistence, screen sizes, no-crash core flows.

## 6. What is explicitly NOT in these phases

Backend, Firebase/Supabase, network APIs, real auth, payments, signed release, website repo changes, pushes/commits/deploys. Pickup QR/PIN (if added) labelled DEMO, not secure proof.

## 7. Milestone reporting

Per phase: features, files created/changed, commands + actual results, limitations, missing tools, next stage. No claim of “complete” until implemented + tested.

## 8. Decisions needed from you (see Phase-1 questions)

1. Persistence: Room (recommended) vs. simpler file/JSON store?
2. Navigation: single-Activity + Fragments + BottomNav (recommended) vs. keep multiple Activities?
3. Roles: simulated role picker on login (recommended) vs. separate demo logins?
4. Design: keep/extend black+orange (recommended) vs. you supply a reference?
5. Package/applicationId: keep `com.example.*` (recommended for now) vs. rename now?
