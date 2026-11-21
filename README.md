# Restaurant QR Ordering

A single-restaurant QR-based ordering application with two React frontends and one Spring Boot API.

## Project structure

- `apps/customer-app` — public menu and customer ordering experience
- `apps/manager-app` — restaurant operations dashboard
- `backend` — Java 21, Spring Boot and PostgreSQL API

## Current status

The customer app includes an interactive menu, cart and checkout form. Customers enter their table number, name and 10-digit phone number at checkout. Checkout submits orders to the Spring Boot API; the backend validates menu availability and calculates the authoritative total. A demo menu is inserted by Flyway for a fresh database. Razorpay checkout is now wired into customer checkout. The backend creates Razorpay Orders using server-calculated totals, verifies the checkout signature and payment status server-side, and accepts signed payment webhooks. A successful captured payment moves the restaurant order to `PENDING_ACCEPTANCE`. The manager app provides authenticated viewing of incoming orders and customer/table details.

## Run the customer app

Set `VITE_API_BASE_URL` only if the backend is not running at `http://localhost:8080` (for example, create `apps/customer-app/.env.local`).

```bash
cd apps/customer-app
npm install
npm run dev
```

## Run the manager app

```bash
cd apps/manager-app
npm install
npm run dev -- --port 5174
```

## Run the backend

For local manager access, the defaults are username `manager` and password `change-me-now`. Override these with `MANAGER_USERNAME` and `MANAGER_PASSWORD`; never use the defaults in a deployed environment. Flyway applies the database migrations, including customer-entered table number/phone fields and demo menu data.

Requirements: Java 21 and PostgreSQL. Configure `DB_URL` (default `jdbc:postgresql://localhost:5432/restaurant_db`), `DB_USERNAME` (default `postgres`) and `DB_PASSWORD`, then run:

```bash
cd backend
./gradlew bootRun
```

## Product direction

The MVP starts with one restaurant, a shared backend and one PostgreSQL database. Customers scan one common restaurant QR code (the same QR for every table), browse the menu, enter their table number and contact details at checkout, and submit an order. The manager app authenticates separately and displays incoming orders. Order status actions, customer tracking, and menu management remain the next development steps.

Payment success must be verified by a trusted gateway webhook or by the manager against the merchant payment record; a customer-provided success screen or UTR alone is not proof of payment.

## Razorpay setup (test mode first)

1. Create or sign in to your Razorpay account and generate **Test Mode** API keys in the Razorpay Dashboard.
2. Configure these environment variables for the backend (never commit real values):
   - `RAZORPAY_KEY_ID`
   - `RAZORPAY_KEY_SECRET`
   - `RAZORPAY_WEBHOOK_SECRET`
3. Start the backend and customer app as described above. The customer checkout opens Razorpay Checkout after the backend creates the restaurant order and Razorpay Order.
4. Configure a Razorpay webhook pointing to `https://YOUR_PUBLIC_API_HOST/api/public/payments/razorpay/webhook`. Subscribe to `payment.captured` and `payment.failed`, and use the same webhook secret in `RAZORPAY_WEBHOOK_SECRET`.
5. Test successful, failed, dismissed, and delayed webhook payment flows before switching to Live Mode.

The API secret and webhook secret are backend-only. The customer app receives only the public Razorpay key ID. A customer checkout callback is not trusted by itself: the backend validates the signature, fetches the payment from Razorpay, checks amount/currency/order association, and only treats a captured payment as successful. Signed webhooks provide a second server-to-server confirmation path.
