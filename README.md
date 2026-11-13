# Restaurant QR Ordering

A single-restaurant QR-based ordering application with two React frontends and one Spring Boot API.

## Project structure

- `apps/customer-app` — public menu and customer ordering experience
- `apps/manager-app` — restaurant operations dashboard
- `backend` — Java 21, Spring Boot and PostgreSQL API

## Current status

The customer app includes an interactive menu prototype with category filtering, search, item quantities and a cart drawer. Menu content is sample data; checkout, payment verification and API integration are not connected yet.

## Run the customer app

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

Requirements: Java 21 and PostgreSQL. Configure `DB_URL` (default `jdbc:postgresql://localhost:5432/restaurant_db`), `DB_USERNAME` (default `postgres`) and `DB_PASSWORD`, then run:

```bash
cd backend
./gradlew bootRun
```

## Product direction

The MVP starts with one restaurant, a shared backend and one PostgreSQL database. Customers scan a table QR code, browse the menu, create an order and track its progress. Managers authenticate separately to review orders, verify payments and manage the menu.

Payment success must be verified by a trusted gateway webhook or by the manager against the merchant payment record; a customer-provided success screen or UTR alone is not proof of payment.
