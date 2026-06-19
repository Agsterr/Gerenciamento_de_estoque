-- Migration to remove Stripe-related columns from tables
ALTER TABLE subscriptions DROP COLUMN IF EXISTS stripe_subscription_id;
ALTER TABLE subscriptions DROP COLUMN IF EXISTS stripe_customer_id;

ALTER TABLE plans DROP COLUMN IF EXISTS stripe_price_id;
ALTER TABLE plans DROP COLUMN IF EXISTS stripe_product_id;

ALTER TABLE payments DROP COLUMN IF EXISTS stripe_payment_intent_id;
ALTER TABLE payments DROP COLUMN IF EXISTS stripe_invoice_id;
