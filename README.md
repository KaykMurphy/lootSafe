# LootSafe API

REST API for intermediating digital transactions with Pix payments, holding funds until product release, handling mediation in disputes, and receiving Mercado Pago webhook notifications.

The project is designed for buying and selling digital items such as game accounts, virtual currency, and other digital products while keeping sensitive credentials encrypted in the database.

## Main Features

- Digital offer creation and management.
- Automatic platform fee and seller net amount calculation.
- AES/GCM encryption for product login and password.
- Pix payment generation through Mercado Pago.
- Asynchronous Mercado Pago webhook processing.
- Buyer email delivery after payment approval.
- Mediation flow with message history between buyer, seller, and moderator.
- Automatic cancellation of expired pending Pix payments.
- In-memory H2 database for local development.
- PostgreSQL and Flyway-ready structure.

## Stack

- Java 17
- Spring Boot 4.0.6
- Spring Web
- Spring Data JPA
- Spring Security
- Spring Validation
- Spring Mail
- Spring Scheduling / Async
- Maven Wrapper
- H2 Database
- PostgreSQL Driver
- Flyway
- MapStruct
- Lombok
- Mercado Pago Java SDK

## Project Structure

```text
src/main/java/com/lootsafe
|-- config          # Security, encryption, WebSocket, and async executor configuration
|-- controller      # REST endpoints
|-- dto             # API request and response objects
|-- enums           # Domain statuses, categories, and types
|-- exception       # Centralized error handling
|-- mapper          # MapStruct conversions
|-- model           # JPA entities
|-- repository      # Repositories and email contract
|-- security        # API key filter, encryption, and webhook validation
`-- service         # Business rules
```

## Requirements

- JDK 17 installed.
- Maven Wrapper included in the project.
- Mercado Pago token to generate/cancel Pix payments and fetch payment details.
- SMTP app password for sending emails.

## Environment Variables

The application loads variables from the environment and from a root `.env` file.

Create a local `.env` file with:

```properties
LOOTSAFE_CRYPTO_KEY=1234567890123456
LOOTSAFE_ADMIN_API_KEY=replace-this-key
LOOTSAFE_MP_TOKEN=TEST-your-mercado-pago-token
SECRET_KEY=your-webhook-secret
EMAIL_PASSWORD=your-smtp-app-password
```

| Variable | Required | Description |
| --- | --- | --- |
| `LOOTSAFE_CRYPTO_KEY` | Yes | AES key used to encrypt credentials. Must have exactly 16, 24, or 32 bytes. |
| `LOOTSAFE_CRYPTO_KEY_APP` | Optional | Alternative with priority over `LOOTSAFE_CRYPTO_KEY`. |
| `LOOTSAFE_ADMIN_API_KEY` | Yes | Key required in the `X-API-KEY` header for mediation admin routes. |
| `LOOTSAFE_MP_TOKEN` | Yes | Mercado Pago access token. |
| `SECRET_KEY` | Yes | Secret used to validate Mercado Pago webhook signatures. |
| `EMAIL_PASSWORD` | Yes | SMTP password used by Spring Mail. |

> The `.env` file is already listed in `.gitignore` and must not be committed.

## Running Locally

```bash
./mvnw spring-boot:run
```

The API runs by default at:

```text
http://localhost:8080
```

Local H2 console:

```text
http://localhost:8080/h2-console
```

Default H2 credentials:

```text
JDBC URL: jdbc:h2:mem:lootsafedb
User: sa
Password: 1234
```

## Tests

```bash
./mvnw test
```

The current tests validate the Spring context and cover security, API key, webhook, and Mercado Pago test properties.

## Main Flow

1. The seller creates an offer with category, description, amount, product credentials, email, and Pix key.
2. The API calculates the platform fee and seller net amount.
3. The buyer requests Pix generation for the offer.
4. Mercado Pago sends a webhook when the payment is approved.
5. The API validates the webhook signature, fetches the payment, and marks the offer as `PAYMENT_HELD`.
6. The credentials are sent to the buyer by email and the release deadline is set.
7. The payment can be released to the seller or the offer can enter mediation.
8. In mediation, messages can be exchanged and a moderator decides in favor of the buyer or seller.

## Transaction Statuses

| Status | Meaning |
| --- | --- |
| `PENDING_PAYMENT` | Offer created and waiting for payment. |
| `PAYMENT_HELD` | Payment approved and funds held. |
| `IN_MEDIATION` | Offer in dispute. |
| `SETTLED` | Payment settled to the seller. |
| `REFUNDED` | Payment refunded to the buyer. |
| `CANCELLED` | Offer cancelled. |
| `COMPLETED` | Reserved status for flow completion. |

## Endpoints

### Offers

| Method | Route | Description |
| --- | --- | --- |
| `POST` | `/api/offers` | Creates an offer. |
| `GET` | `/api/offers` | Lists offers with pagination. |
| `GET` | `/api/offers/{id}` | Gets an offer by ID. |
| `PUT` | `/api/offers/{id}` | Updates an offer. |
| `DELETE` | `/api/offers/{id}` | Deletes an offer when its status allows it. |
| `POST` | `/api/offers/{id}/generate-pix` | Generates Pix for the buyer. |
| `POST` | `/api/offers/{id}/release-payment` | Tries to release payment to the seller. |
| `POST` | `/api/offers/{id}/mediation` | Opens mediation for an offer with held payment. |
| `POST` | `/api/offers/{id}/messages` | Sends a mediation message. |
| `GET` | `/api/offers/{id}/messages` | Lists the mediation message history. |

### Chat

| Method | Route | Description |
| --- | --- | --- |
| `GET` | `/api/chat/history` | Lists the latest public chat messages. |
| `STOMP` | `/app/chat.send` | Sends a public WebSocket chat message. |
| `STOMP` | `/topic/public` | Public WebSocket chat topic. |

### Mediation

The routes below require this header:

```text
X-API-KEY: <LOOTSAFE_ADMIN_API_KEY>
```

| Method | Route | Description |
| --- | --- | --- |
| `GET` | `/api/mediation/offers` | Lists offers in mediation. |
| `GET` | `/api/mediation/offers/all` | Lists all offers with pagination and sorting. |
| `POST` | `/api/mediation/offers/{id}/resolve` | Resolves a dispute with `BUYER_WINS` or `SELLER_WINS`. |
| `DELETE` | `/api/mediation/offers/{id}/cancel` | Manually cancels an offer. |
| `GET` | `/api/mediation/offers/statistics/profit` | Returns accumulated platform profit. |
| `POST` | `/api/mediation/offers/{id}/simulate-payment` | Simulates an approved payment for local tests. |

### Webhooks

| Method | Route | Description |
| --- | --- | --- |
| `POST` | `/webhooks/mercadopago` | Receives Mercado Pago notifications. |

The webhook expects:

- Header `x-signature`
- Header `x-request-id`
- Query param `data.id`
- JSON body with `type = "payment"`

## Usage Examples

### Create Offer

```bash
curl -X POST http://localhost:8080/api/offers \
  -H "Content-Type: application/json" \
  -d '{
    "productCategory": "GAME_ACCOUNT",
    "description": "Level 80 account with rare items",
    "grossAmount": 150.00,
    "trialPeriodHours": 24,
    "credentialLogin": "account-login",
    "credentialPassword": "account-password",
    "sellerEmail": "seller@example.com",
    "pixKeyType": "EMAIL",
    "pixKey": "seller@example.com"
  }'
```

### Generate Pix For Buyer

```bash
curl -X POST "http://localhost:8080/api/offers/{offerId}/generate-pix?buyerEmail=buyer@example.com&buyerFirstName=Buyer&buyerLastName=Test"
```

### Simulate Approved Payment Locally

```bash
curl -X POST http://localhost:8080/api/mediation/offers/{offerId}/simulate-payment \
  -H "X-API-KEY: $LOOTSAFE_ADMIN_API_KEY"
```

### Open Mediation

```bash
curl -X POST http://localhost:8080/api/offers/{offerId}/mediation
```

### Send Mediation Message

```bash
curl -X POST http://localhost:8080/api/offers/{offerId}/messages \
  -H "Content-Type: application/json" \
  -d '{
    "author": "BUYER",
    "messageText": "The delivered product does not match the listing.",
    "messageType": "CHAT"
  }'
```

### Resolve Dispute

```bash
curl -X POST "http://localhost:8080/api/mediation/offers/{offerId}/resolve?decision=BUYER_WINS" \
  -H "X-API-KEY: $LOOTSAFE_ADMIN_API_KEY"
```

## Database

The current profile uses in-memory H2 with `ddl-auto=update`, intended for local development.

For production, configure:

- PostgreSQL.
- Flyway.
- Versioned migrations in `src/main/resources/db/migration`.
- `spring.jpa.hibernate.ddl-auto` as `validate` or `none`.
- Remove credentials and sensitive values from `application.yml`.

## Important Notes Before Production

- The `/api/mediation/offers/{id}/simulate-payment` endpoint is marked in code as test-only and must be removed before production.
- Automatic Pix transfer to the seller is not fully implemented in `PaymentService.transferToSeller`.
- Mediation routes depend on `X-API-KEY`, but the security policy should be reviewed before exposing the API publicly.
- CORS is limited to `localhost:5173` and `127.0.0.1:5173`.
- The sender email is configured in `application.yml`; move the SMTP username to an environment variable for production.
- Review whether product credentials should appear in API responses in every scenario.

## License

License not defined yet.
