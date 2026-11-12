# ClearPath AI — Frontend Dashboard

React + Vite frontend for **ClearPath AI**, an AI-assisted payment reconciliation and investigation prototype. The dashboard lets a user look up a PayPal Sandbox order, inspect its verified webhook timeline, upload merchant transaction evidence from CSV, and view Gemini-assisted investigation reports and their history.

> **Prototype notice:** This frontend is intended for a hackathon demonstration using PayPal Sandbox and synthetic merchant data. It is not a production financial application.

## Features

- Dark-themed financial investigation dashboard.
- PayPal order lookup and verified event timeline.
- Merchant evidence import from CSV through the backend bulk-import endpoint.
- Display of CSV import totals and row-level results (when returned by the backend).
- Reconciliation result and transaction evidence display.
- Gemini-generated investigation report with executive summary, plausible causes, missing evidence, recommended actions, customer explanation, and evidence limitations.
- Saved investigation history for the selected PayPal order.

## Technology

- React
- Vite
- JavaScript and CSS
- Fetch API for backend REST calls

## Prerequisites

- Node.js and npm versions supported by the Vite version in `package.json`.
- The ClearPath AI Spring Boot backend running locally on port `8080`.
- A configured backend `CLEARPATH_AI_ACCESS_TOKEN` matching the frontend proxy token.

Check your installed versions:

```bash
node -v
npm -v
```

## Install dependencies

From the frontend project directory:

```bash
npm install
```

## Local configuration

Create a file named `.env.local` in the frontend project root:

```dotenv
CLEARPATH_AI_ACCESS_TOKEN=YOUR_LONG_RANDOM_TOKEN
```

Use the same token configured as `CLEARPATH_AI_ACCESS_TOKEN` in the backend. The Vite development proxy reads this variable and adds the `X-ClearPath-Token` header to proxied API requests. This variable deliberately does not use the `VITE_` prefix because it is consumed by the development server configuration rather than exposed to browser code.

Do not commit `.env.local` or share its contents. Ensure it is covered by `.gitignore`.

## Run locally

Start the Spring Boot backend first, then run the frontend:

```bash
npm run dev
```

The development server is configured to use:

```text
http://localhost:5174
```

The Vite proxy forwards requests beginning with `/api` to:

```text
http://127.0.0.1:8080
```

This avoids hard-coding the backend host throughout the React components. Keep the backend running while using the dashboard.

## Build for production

Create a frontend build:

```bash
npm run build
```

To preview the generated build locally (if the package scripts include the standard Vite preview script):

```bash
npm run preview
```

The current project is configured for local development. A production deployment requires configuring the correct backend API origin, secure user authentication, and production-grade handling of credentials; do not expose the development proxy token in a public frontend bundle.

## Demo workflow

1. Start the Spring Boot backend and the Vite frontend.
2. Open `http://localhost:5174`.
3. Enter a PayPal Sandbox order ID that has verified webhook events stored in ClearPath.
4. Select a merchant CSV file and click **Upload CSV**. The CSV should use the backend's expected columns:

   ```csv
   paypalOrderId,merchantOrderId,paymentStatus,amount,currency
   PAYPAL_ORDER_ID,MERCHANT-ORDER-001,NOT_RECEIVED,10.00,USD
   ```

5. Confirm the import result. Use an order without an existing merchant report; the backend rejects duplicate reports for the same PayPal order.
6. Click **Load evidence** to refresh the selected order's evidence and timeline if needed.
7. Click **Run AI investigation** to generate a report.
8. Review the reconciliation result, AI findings, and saved investigation history.

The merchant status values accepted by the backend are `COMPLETED`, `NOT_RECEIVED`, `PENDING`, and `FAILED`.

## Important implementation notes

- The frontend consumes the local Spring Boot API through the Vite `/api` proxy.
- The current merchant integration is CSV/API-based; merchant records are not yet fetched from a live merchant ledger or payment platform.
- The AI report is generated from backend investigation evidence. The deterministic reconciliation result remains authoritative; AI output is advisory.
- The dashboard uses synthetic Sandbox transaction data for the hackathon demonstration.

## Repository safety

Before publishing this repository, confirm that `.env.local`, API keys, tokens, local database files, and IDE settings are excluded from Git. For example:

```text
.env
.env.*
!.env.example
node_modules/
dist/
.DS_Store
.idea/
```

Review staged files before every push:

```bash
git status
```

## License

Add the license required by your hackathon or project owner before redistributing this code.
