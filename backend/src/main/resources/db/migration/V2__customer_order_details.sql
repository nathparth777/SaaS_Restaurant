-- Customer-entered table details are independent of QR/table registry records.
ALTER TABLE customer_orders
    ALTER COLUMN table_id DROP NOT NULL;

ALTER TABLE customer_orders
    ADD COLUMN table_number VARCHAR(30),
    ADD COLUMN customer_phone VARCHAR(10),
    ADD COLUMN access_token UUID NOT NULL DEFAULT gen_random_uuid();

CREATE UNIQUE INDEX uq_customer_orders_access_token
    ON customer_orders(access_token);

CREATE INDEX idx_customer_orders_table_number
    ON customer_orders(table_number);
