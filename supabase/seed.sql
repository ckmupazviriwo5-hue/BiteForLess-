-- Bite For Less — idempotent demo seed (Phase 6/7)
-- Safe to rerun: every row is upserted by stable ID and flagged is_demo;
-- DO UPDATE applies only to existing demo rows (real rows never touched).
-- Zambian marketplace demo: Lusaka vendors, ZMW prices.
-- No auth users, no fake reservations: catalog data only.
-- Apply ONCE on an empty catalog; to refresh, run demo_cleanup.sql first
-- (reruns reset demo stock/status and keep original deadlines).

-- Categories (ids match the Android demo dataset).
INSERT INTO public.categories (id, name, description, is_active)
VALUES
    ('bakery', 'Bakery', 'Demo category: breads and pastries.', true),
    ('produce', 'Produce', 'Demo category: fruit and vegetables.', true),
    ('dairy', 'Dairy', 'Demo category: milk, yoghurt and cheese.', true),
    ('meals', 'Meals', 'Demo category: ready-to-eat dishes.', true),
    ('beverages', 'Beverages', 'Demo category: juices and drinks.', true),
    ('snacks', 'Snacks', 'Demo category: bites and packs.', true)
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    is_active = EXCLUDED.is_active;

-- Vendors (ownerless demo records; approval via set_vendor_status only).
INSERT INTO public.vendors (id, owner_id, name, description, location, status, is_demo)
VALUES
    ('v1', NULL, 'Mama Chanda''s Bakery', 'Demo vendor: neighbourhood bakery.', 'Matero, Lusaka', 'approved', true),
    ('v2', NULL, 'Soweto Fresh Produce', 'Demo vendor: fresh produce market.', 'Soweto Market, Lusaka', 'approved', true),
    ('v3', NULL, 'Kapiri Kitchen', 'Demo vendor: home-style cooked meals.', 'Kalingalinga, Lusaka', 'approved', true),
    ('v4', NULL, 'Lusaka Dairy Corner', 'Demo vendor: dairy outlet (pending approval).', 'Kabulonga, Lusaka', 'pending', true),
    ('v5', NULL, 'Cairo Road Cafe', 'Demo vendor: late-night cafe (suspended).', 'Cairo Road, Lusaka', 'suspended', true)
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    location = EXCLUDED.location,
    status = EXCLUDED.status,
    is_demo = true
WHERE vendors.is_demo IS TRUE;

-- Listings (ids/prices mirror the Android demo dataset; ZMW).
INSERT INTO public.listings
    (id, vendor_id, category_id, name, description, currency,
     original_price, discounted_price, stock, status,
     deadline, collection_window, location, is_demo)
VALUES
    ('l1', 'v1', 'bakery', 'Sourdough Loaf Bundle',
     'Demo: two day-old sourdough loaves, still fresh for toast.', 'ZMW',
     60.00, 29.00, 8, 'active',
     now() + interval '6 hours', 'Today 16:00-18:00', 'Matero, Lusaka', true),
    ('l2', 'v1', 'bakery', 'Butter Croissants (4-pack)',
     'Demo: morning-batch croissants, flaky and buttery.', 'ZMW',
     80.00, 35.00, 2, 'active',
     now() + interval '4 hours', 'Today 15:00-17:00', 'Matero, Lusaka', true),
    ('l3', 'v2', 'produce', 'Seasonal Veggie Box',
     'Demo: mixed surplus vegetables, great for a week of cooking.', 'ZMW',
     120.00, 59.00, 10, 'active',
     now() + interval '8 hours', 'Today 14:00-19:00', 'Soweto Market, Lusaka', true),
    ('l4', 'v2', 'produce', 'Fruit Salad Cups (2-pack)',
     'Demo: pre-cut fruit cups from this morning.', 'ZMW',
     45.00, 20.00, 0, 'active',
     now() + interval '5 hours', 'Today 14:00-18:00', 'Soweto Market, Lusaka', true),
    ('l5', 'v3', 'meals', 'Chicken Curry + Rice',
     'Demo: hearty lunch portions, cooked fresh at midday.', 'ZMW',
     95.00, 45.00, 6, 'active',
     now() + interval '3 hours', 'Today 12:00-15:00', 'Kalingalinga, Lusaka', true),
    ('l6', 'v3', 'meals', 'Veg Bean Stew',
     'Demo: slow-cooked bean stew, vegan friendly.', 'ZMW',
     70.00, 30.00, 12, 'active',
     now() + interval '3 hours', 'Today 12:00-15:00', 'Kalingalinga, Lusaka', true),
    ('l7', 'v5', 'snacks', 'Cafe Muffin Box (4-pack)',
     'Demo: assorted muffins. Vendor account suspended, listing paused.', 'ZMW',
     55.00, 25.00, 3, 'inactive',
     now() + interval '5 hours', 'Today 13:00-17:00', 'Cairo Road, Lusaka', true),
    ('l8', 'v1', 'bakery', 'Cinnamon Rolls (6-pack)',
     'Demo: yesterday''s cinnamon rolls, still soft inside.', 'ZMW',
     90.00, 40.00, 5, 'active',
     now() - interval '2 hours', 'Was 08:00-10:00', 'Matero, Lusaka', true),
    ('l9', 'v2', 'dairy', 'Fresh Milk 2L',
     'Demo: two 2L bottles nearing best-before, keep refrigerated.', 'ZMW',
     70.00, 55.00, 9, 'active',
     now() + interval '24 hours', 'Tomorrow 09:00-18:00', 'Soweto Market, Lusaka', true),
    ('l10', 'v3', 'snacks', 'Beef Samosas (6-pack)',
     'Demo: crispy samosas, perfect for sharing.', 'ZMW',
     60.00, 27.00, 1, 'active',
     now() + interval '2 hours', 'Today 12:00-14:00', 'Kalingalinga, Lusaka', true),
    ('l11', 'v4', 'dairy', 'Drinking Yoghurt (4-pack)',
     'Demo: mixed-flavour yoghurts. Vendor pending approval.', 'ZMW',
     85.00, 38.00, 7, 'inactive',
     now() + interval '20 hours', 'Tomorrow 09:00-17:00', 'Kabulonga, Lusaka', true),
    ('l12', 'v2', 'beverages', 'Fresh Orange Juice 1L',
     'Demo: squeezed this morning, no added sugar.', 'ZMW',
     50.00, 22.00, 15, 'active',
     now() + interval '10 hours', 'Today 09:00-19:00', 'Soweto Market, Lusaka', true)
ON CONFLICT (id) DO UPDATE
SET vendor_id = EXCLUDED.vendor_id,
    category_id = EXCLUDED.category_id,
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    original_price = EXCLUDED.original_price,
    discounted_price = EXCLUDED.discounted_price,
    stock = EXCLUDED.stock,
    status = EXCLUDED.status,
    collection_window = EXCLUDED.collection_window,
    location = EXCLUDED.location,
    is_demo = true
WHERE listings.is_demo IS TRUE;

-- Sample reviews for the moderation screen (no fake auth accounts).
INSERT INTO public.reviews
    (id, listing_id, reviewer_id, reviewer_label, rating, body, status, is_demo)
VALUES
    ('r1', 'l1', NULL, 'demo-customer-01', 5,
     'Demo review: crusty and fresh, great value.', 'visible', true),
    ('r2', 'l5', NULL, 'demo-customer-02', 4,
     'Demo review: generous portion, pickup was quick.', 'visible', true),
    ('r3', 'l3', NULL, 'demo-customer-01', 3,
     'Demo review: box was good but heavy on potatoes.', 'pending', true),
    ('r4', 'l10', NULL, 'demo-customer-03', 2,
     'Demo review: portion smaller than expected.', 'pending', true)
ON CONFLICT (id) DO UPDATE
SET listing_id = EXCLUDED.listing_id,
    reviewer_label = EXCLUDED.reviewer_label,
    rating = EXCLUDED.rating,
    body = EXCLUDED.body,
    status = EXCLUDED.status,
    is_demo = true
WHERE reviews.is_demo IS TRUE;
