# Restaurant QR Ordering

A single-restaurant QR-based ordering application with two React frontends and one Spring Boot API.

## Project structure

- `apps/customer-app` — public menu and customer ordering experience
- `apps/manager-app` — restaurant operations dashboard
- `backend` — Java 21, Spring Boot and PostgreSQL API

## Current status

The customer app includes an interactive menu, cart and checkout form. Customers enter their table number, name and 10-digit phone number at checkout. Checkout submits orders to the Spring Boot API; the backend validates menu availability and calculates the authoritative total. A demo menu is inserted by Flyway for a fresh database. Payment initiation and verification are not connected yet. The manager app provides authenticated viewing of incoming orders and customer/table details.

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

The MVP starts with one restaurant, a shared backend and one PostgreSQL database. Customers scan one common restaurant QR code (the same QR for every table), browse the menu, enter their table number and contact details at checkout, and submit an order. The manager app authenticates separately and displays incoming orders. Payment verification, order status actions, customer tracking, and menu management are future steps.

Payment success must be verified by a trusted gateway webhook or by the manager against the merchant payment record; a customer-provided success screen or UTR alone is not proof of payment.
