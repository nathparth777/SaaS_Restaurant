ALTER TABLE payments
    ADD COLUMN gateway_order_id VARCHAR(100),
    ADD COLUMN gateway_payment_id VARCHAR(100),
    ADD COLUMN gateway_signature VARCHAR(255);

CREATE UNIQUE INDEX uq_payments_gateway_order_id
    ON payments(gateway_order_id)
    WHERE gateway_order_id IS NOT NULL;

CREATE UNIQUE INDEX uq_payments_gateway_payment_id
    ON payments(gateway_payment_id)
    WHERE gateway_payment_id IS NOT NULL;

CREATE TABLE processed_payment_webhooks (
    event_id VARCHAR(150) PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
