# ClearPath AI — Backend API

Spring Boot backend for **ClearPath AI**, an AI-assisted payment reconciliation and investigation prototype. The backend connects to PayPal Sandbox, verifies PayPal webhook notifications, stores payment and merchant evidence, reconciles records, and requests investigation explanations from Google Gemini.

> **Prototype notice:** This project is for hackathon demonstration using PayPal Sandbox and synthetic merchant data. It is not a production payment, dispute-management, or financial decision system.

## Features

- PayPal Sandbox order creation and payment capture.
- PayPal webhook signature verification using PayPal's verification API.
- Persistence of verified webhook events and investigation records using Spring Data JPA and H2.
- Merchant transaction report submission through a REST API.
- Bulk merchant evidence import from CSV with row-level success and failure results.
- Deterministic reconciliation of PayPal evidence against merchant-reported status, amount, and currency.
- Gemini-assisted investigation summaries, plausible causes, missing evidence, and recommended actions.
- Investigation history for previously generated AI reports.

## Technology

- Java 21
- Spring Boot
- Spring Data JPA
- H2 database
- PayPal REST APIs and Webhooks (Sandbox)
- Google Gemini API (`gemini-3.5-flash-lite` as configured for the prototype)
- Maven Wrapper

## Prerequisites

- Java 21 installed and available on `PATH`.
- A PayPal Developer account with a Sandbox REST app and Sandbox buyer account.
- A Google AI Studio API key for Gemini.
- A public HTTPS tunnel such as ngrok when PayPal needs to deliver webhooks to a locally running application.

## Configuration

The application expects these environment variables. Set them in your IDE's Spring Boot run configuration or in your local shell. **Do not commit real values.**

| Variable | Purpose |
|---|---|
| `PAYPAL_CLIENT_ID` | PayPal Sandbox REST app client ID |
| `PAYPAL_CLIENT_SECRET` | PayPal Sandbox REST app secret |
| `PAYPAL_WEBHOOK_ID` | Webhook ID configured in the PayPal Developer Dashboard |
| `GEMINI_API_KEY` | Google Gemini API key |
| `CLEARPATH_AI_ACCESS_TOKEN` | Shared token used to protect AI investigation endpoints in this prototype |

The prototype's `application.properties` uses the PayPal Sandbox base URL `https://api-m.sandbox.paypal.com` and reads secrets from environment variables. Keep the Gemini key and all other credentials outside source control.

For local development, configure the variables in your IDE run configuration. Alternatively, export them in the current terminal session before starting the service:

```bash
export PAYPAL_CLIENT_ID='YOUR_SANDBOX_CLIENT_ID'
export PAYPAL_CLIENT_SECRET='YOUR_SANDBOX_CLIENT_SECRET'
export PAYPAL_WEBHOOK_ID='YOUR_SANDBOX_WEBHOOK_ID'
export GEMINI_API_KEY='YOUR_GEMINI_API_KEY'
export CLEARPATH_AI_ACCESS_TOKEN='YOUR_LONG_RANDOM_TOKEN'
```

Use your own values; do not paste credentials into README files, screenshots, commits, or public issue trackers.

## Run locally

From the backend project directory:

```bash
./mvnw clean compile
./mvnw spring-boot:run
```

The API is configured for local development at:

```text
http://localhost:8080
```

To build the application:

```bash
./mvnw clean package
```

## API endpoints

The following endpoints are implemented in the current prototype. Request/response fields may evolve as the project develops.

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/api/paypal/health` | Check PayPal Sandbox connectivity/authentication |
| `POST` | `/api/paypal/orders` | Create a PayPal Sandbox order |
| `POST` | `/api/paypal/orders/{orderId}/capture` | Capture an approved order |
| `GET` | `/api/paypal/orders/{orderId}/verify` | Retrieve/verify order information |
| `POST` | `/api/paypal/webhooks` | Receive and verify PayPal webhook notifications |
| `POST` | `/api/clearpath/merchant-reports` | Submit one merchant payment report as JSON |
| `POST` | `/api/clearpath/merchant-reports/bulk` | Import merchant reports from a multipart CSV file (`file` part) |
| `GET` | `/api/clearpath/transactions/{orderId}/timeline` | Retrieve stored PayPal event timeline for an order |
| `GET` | `/api/clearpath/investigations/{orderId}` | Retrieve deterministic investigation/reconciliation evidence |
| `POST` | `/api/clearpath/investigations/{orderId}/ai` | Generate and save a Gemini-assisted investigation report |
| `GET` | `/api/clearpath/investigations/{orderId}/ai/history` | Retrieve saved AI investigation reports |

The AI investigation endpoints use the `X-ClearPath-Token` request header in the current prototype.

### Merchant CSV format

The CSV importer expects these column names (case-insensitive):

```csv
paypalOrderId,merchantOrderId,paymentStatus,amount,currency
PAYPAL_ORDER_ID,MERCHANT-ORDER-001,NOT_RECEIVED,10.00,USD
```

Supported merchant payment statuses are:

- `COMPLETED`
- `NOT_RECEIVED`
- `PENDING`
- `FAILED`

Each merchant report must refer to an order for which ClearPath has stored a verified PayPal order event. A merchant report is unique per PayPal order in the current implementation; attempting to add another report for the same order returns a duplicate/conflict error. The amount must be positive and the currency must be a three-letter code.

Example CSV upload:

```bash
curl -X POST http://localhost:8080/api/clearpath/merchant-reports/bulk \
  -F "file=@$HOME/Downloads/merchant-reports.csv"
```

The API returns the total row count, imported row count, failed row count, and a result for each processed row.

## PayPal webhook setup for local demos

1. Start the backend locally.
2. Start an HTTPS tunnel to port `8080` (for example, using ngrok).
3. In the PayPal Developer Dashboard, configure the Sandbox app's webhook URL as `https://YOUR-TUNNEL-HOST/api/paypal/webhooks`.
4. Subscribe to the event types handled by the application, including `CHECKOUT.ORDER.APPROVED` and `PAYMENT.CAPTURE.COMPLETED`.
5. Set the matching webhook ID as `PAYPAL_WEBHOOK_ID` in the backend environment.
6. Complete a Sandbox payment and confirm the verified event appears in the backend logs and transaction timeline.

A tunnel URL can change when restarted. Never treat a temporary development tunnel as a permanent production endpoint.

## Data and privacy notes

- H2 is used as the prototype database; the configured file-backed database is stored locally under the backend's `data/` directory.
- Use synthetic Sandbox transactions and non-sensitive merchant test data only.
- Do not upload real customer payment details, card data, personal information, or production credentials to this prototype or to third-party AI services.
- Do not commit the H2 database, local environment files, API keys, access tokens, or IDE-specific secrets.

## Current limitations

- PayPal integration uses the Sandbox environment, not live payment processing.
- Merchant data is currently provided through manual CSV upload or the merchant-report API; there is no live merchant system/ledger integration yet.
- H2 is used for the hackathon prototype rather than a production database.
- AI output is advisory. Deterministic reconciliation is the source of truth; Gemini does not independently confirm a root cause or change payment status.
- Production deployment would require stronger authentication and authorization, secret management, rate limiting, observability, resilient persistence, and security review across all endpoints.

## Repository safety

Before publishing this repository, verify that secrets and generated files are excluded from Git:

```text
.env
.env.*
!.env.example
**/data/**
*.mv.db
*.trace.db
.idea/
```

Review `git status` and inspect staged changes before committing. If a secret was ever committed, removing it from the latest file is not sufficient; rotate/revoke that credential and remove it from repository history as appropriate.

## License

Add the license required by your hackathon or project owner before redistributing this code.
