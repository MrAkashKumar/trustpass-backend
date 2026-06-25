# TrustPass Backend

Spring Boot REST API for the TrustPass AI human approval layer.

TrustPass sits between autonomous AI agents and real-world execution. The backend receives proposed agent actions, evaluates risk and policy, asks for verified human consent when needed, gates execution, and records a tamper-evident audit trail.

## What This Backend Does

- Registers AI agents with delegated authority limits.
- Defines enterprise policies for travel, purchases, vendor payments, contracts, and other action types.
- Scores proposed actions with a deterministic local risk adapter or optional OpenAI adapter.
- Decides whether an action is denied, auto-approved, or paused for human approval.
- Captures human consent with identity-verification metadata.
- Executes approved actions through an execution adapter.
- Writes every important transition to a SHA-256 hash-chained audit log.
- Supports optional ElevenLabs voice approval callbacks.

## Tech Stack

- Java 21
- Spring Boot 4.0.6
- Spring Web
- Spring Security + JWT resource server
- Spring Data JPA
- H2 file database for local development
- Maven
- Optional OpenAI risk assessment adapter
- Optional ElevenLabs approval notification and webhook adapter

## Backend Flow

```text
AI agent submits action
        |
        v
Risk assessment
        |
        v
Policy engine
        |
        +--> Deny
        |
        +--> Auto-approve
        |
        +--> Require human consent
                    |
                    v
            Identity verified approval
                    |
                    v
              Execution adapter
                    |
                    v
          Hash-chained audit event
```

## Requirements

- Java 21+
- Maven 3.9+

## Run Locally

From this directory:

```bash
mvn spring-boot:run
```

The API starts on:

```text
http://localhost:8080/api/v1
```

Health check:

```text
http://localhost:8080/actuator/health
```

The local database is stored at:

```text
trustpass-backend/data/trustpass.mv.db
```

## Demo Accounts

These users are created in memory for local development.

| Role | Username | Password |
| --- | --- | --- |
| Admin | `admin@trustpass.local` | `Admin123!` |
| Approver | `approver@trustpass.local` | `Approve123!` |
| Auditor | `auditor@trustpass.local` | `Audit123!` |

Use the admin account when testing the full create, approve, execute, and audit flow.

## Demo Data

On first startup, the backend seeds sample agents, policies, and approval requests unless data already exists.

Seeded examples include:

- Atlas Travel Agent
- Ledger Finance Agent
- Nova Procurement Agent
- Corporate travel policy
- Routine purchases policy
- Vendor payment policy
- Singapore to Tokyo flight approval for `SGD 850`

Disable seed data with:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--trustpass.seed.enabled=false
```

## Authentication

Login returns a JWT access token.

```bash
curl -s http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin@trustpass.local",
    "password": "Admin123!"
  }'
```

Example response:

```json
{
  "accessToken": "eyJ...",
  "expiresAt": "2026-06-25T10:00:00Z",
  "username": "admin@trustpass.local",
  "roles": ["ADMIN", "APPROVER", "AUDITOR"]
}
```

Use the token on protected endpoints:

```bash
Authorization: Bearer <accessToken>
```

External AI agents should not use human demo accounts. Enable the agent-client credential and call the `/api/v1/agent-actions` endpoints instead:

```bash
TRUSTPASS_AGENT_CLIENT_ENABLED=true
TRUSTPASS_AGENT_CLIENT_ID=demo-travel-agent
TRUSTPASS_AGENT_API_KEY=local-agent-key-change-me
```

Agent requests use:

```http
X-TrustPass-Client-Id: demo-travel-agent
X-TrustPass-Api-Key: local-agent-key-change-me
Idempotency-Key: <stable-external-request-id>
```

## Quick API Tour

Set a token:

```bash
TOKEN=$(curl -s http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin@trustpass.local","password":"Admin123!"}' \
  | jq -r .accessToken)
```

List agents:

```bash
curl http://localhost:8080/api/v1/agents \
  -H "Authorization: Bearer $TOKEN"
```

Create a travel approval request:

```bash
AGENT_ID=$(curl -s http://localhost:8080/api/v1/agents \
  -H "Authorization: Bearer $TOKEN" \
  | jq -r '.[] | select(.type=="TRAVEL") | .id' | head -n 1)

APPROVAL_ID=$(curl -s http://localhost:8080/api/v1/approvals \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"agentId\": \"$AGENT_ID\",
    \"actionType\": \"TRAVEL_BOOKING\",
    \"summary\": \"Book Singapore to Tokyo flight\",
    \"description\": \"AI travel assistant found a policy-compliant Friday 8 PM flight.\",
    \"target\": \"Demo Airline SIN-HND\",
    \"amount\": 850,
    \"currency\": \"SGD\"
  }" \
  | jq -r .id)
```

Approve the request:

```bash
curl -X POST http://localhost:8080/api/v1/approvals/$APPROVAL_ID/decision \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "decision": "APPROVE",
    "comment": "Approved for customer workshop travel."
  }'
```

Execute the approved action:

```bash
curl -X POST http://localhost:8080/api/v1/approvals/$APPROVAL_ID/execute \
  -H "Authorization: Bearer $TOKEN"
```

Verify the audit chain:

```bash
curl http://localhost:8080/api/v1/audit-events/verify \
  -H "Authorization: Bearer $TOKEN"
```

## Main Endpoints

| Method | Endpoint | Purpose | Auth |
| --- | --- | --- | --- |
| `POST` | `/api/v1/auth/login` | Issue JWT token | Public |
| `GET` | `/api/v1/dashboard` | Summary metrics and recent approvals | Authenticated |
| `GET` | `/api/v1/agents` | List agents | Authenticated |
| `GET` | `/api/v1/agents/{id}` | Get one agent | Authenticated |
| `POST` | `/api/v1/agents` | Register an agent | Admin |
| `PUT` | `/api/v1/agents/{id}` | Update agent authority/status | Admin |
| `GET` | `/api/v1/policies` | List policies | Authenticated |
| `POST` | `/api/v1/policies` | Create policy | Admin |
| `PUT` | `/api/v1/policies/{id}` | Update policy | Admin |
| `GET` | `/api/v1/approvals` | List approval requests | Authenticated |
| `GET` | `/api/v1/approvals/{id}` | Get approval request | Authenticated |
| `POST` | `/api/v1/approvals` | Submit proposed action | Admin or Approver |
| `POST` | `/api/v1/approvals/{id}/decision` | Approve or reject | Admin or Approver |
| `POST` | `/api/v1/approvals/{id}/execute` | Execute approved action | Admin |
| `POST` | `/api/v1/agent-actions` | Submit an exact agent action with payload hash | Agent client |
| `GET` | `/api/v1/agent-actions/{id}` | Poll agent action approval state | Agent client |
| `POST` | `/api/v1/agent-actions/{id}/execution-result` | Report provider execution reference | Agent client |
| `GET` | `/api/v1/audit-events` | List audit events | Admin or Auditor |
| `GET` | `/api/v1/audit-events/verify` | Verify hash chain | Admin or Auditor |
| `POST` | `/api/v1/webhooks/elevenlabs` | Voice decision webhook | Public, HMAC signed |
| `GET` | `/actuator/health` | Health check | Public |

## Domain Values

Action types:

```text
PURCHASE
TRAVEL_BOOKING
VENDOR_PAYMENT
CONTRACT_SIGNING
HIRING
INSURANCE_CLAIM
DATA_EXPORT
OTHER
```

Agent types:

```text
PROCUREMENT
FINANCE
TRAVEL
HR
INSURANCE
GENERAL
```

Approval statuses:

```text
PENDING
AUTO_APPROVED
APPROVED
REJECTED
DENIED
EXECUTED
EXPIRED
```

Decision values:

```text
APPROVE
REJECT
```

Approval channels:

```text
WEB
VOICE
ANY
```

## Configuration

| Environment variable | Purpose | Local default |
| --- | --- | --- |
| `TRUSTPASS_JWT_SECRET` | HMAC JWT signing key. Must be at least 32 characters. | `trustpass-development-secret-change-me-now-2026` |
| `TRUSTPASS_CORS_ORIGINS` | Comma-separated frontend origins. | `http://localhost:3000` |
| `TRUSTPASS_ADMIN_PASSWORD` | Local admin password. | `Admin123!` |
| `TRUSTPASS_APPROVER_PASSWORD` | Local approver password. | `Approve123!` |
| `TRUSTPASS_AUDITOR_PASSWORD` | Local auditor password. | `Audit123!` |
| `TRUSTPASS_AGENT_CLIENT_ENABLED` | Enable server-to-server agent integration credentials. | `false` |
| `TRUSTPASS_AGENT_CLIENT_ID` | External agent client identifier. | none |
| `TRUSTPASS_AGENT_API_KEY` | External agent API key. | none |
| `OPENAI_ENABLED` | Enable live OpenAI risk adapter. | `false` |
| `OPENAI_API_KEY` | Backend-only OpenAI API key. | none |
| `OPENAI_MODEL` | Risk classification model. | `gpt-5.4-nano` |
| `OPENAI_BASE_URL` | OpenAI API host. | `https://api.openai.com` |
| `OPENAI_ORGANIZATION_ID` | Optional OpenAI organization header. | none |
| `OPENAI_PROJECT_ID` | Optional OpenAI project header. | none |
| `ELEVENLABS_ENABLED` | Enable live ElevenLabs notification adapter. | `false` |
| `ELEVENLABS_API_KEY` | Backend-only ElevenLabs API key. | none |
| `ELEVENLABS_AGENT_ID` | ElevenLabs agent identifier. | none |
| `ELEVENLABS_PHONE_NUMBER_ID` | ElevenLabs outbound phone number ID. | none |
| `ELEVENLABS_WEBHOOK_SECRET` | HMAC webhook verification secret. | none |
| `ELEVENLABS_BASE_URL` | ElevenLabs API host. | `https://api.elevenlabs.io` |

See [`../docs/DEVELOPER_INTEGRATION.md`](../docs/DEVELOPER_INTEGRATION.md) for the full external-agent contract. Live providers are opt-in. With no external credentials, deterministic local adapters keep the complete workflow demoable.

## Docker

Build and run the backend container from the repository root:

```bash
docker compose up --build backend
```

Run the full frontend and backend stack:

```bash
docker compose up --build
```

Backend port:

```text
8080
```

Frontend port:

```text
3000
```

## Project Structure

```text
trustpass-backend/
  pom.xml
  Dockerfile
  src/main/java/com/trustpass/
    agent/          Agent registration and authority limits
    agentaction/    Server-to-server agent action API
    approval/       Approval request lifecycle, risk and policy outcomes
    audit/          SHA-256 hash-chained audit ledger
    auth/           Login and JWT token issuing
    config/         Security, properties, demo data
    dashboard/      Summary metrics API
    integration/    Risk, notification, execution, and webhook adapters
    policy/         Enterprise policy model and API
    shared/         Common API response and exception helpers
  src/main/resources/
    application.yml
    application-prod.yml
```

## Testing

Run the backend test suite:

```bash
mvn test
```

The current tests cover application startup and policy-engine behavior.

## Production Notes

Before deploying:

- Set `SPRING_PROFILES_ACTIVE=prod`.
- Replace local H2 with an external database through `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD`.
- Add schema migrations before using `spring.jpa.hibernate.ddl-auto=validate`.
- Set a strong `TRUSTPASS_JWT_SECRET`.
- Replace in-memory users with enterprise identity or OIDC.
- Store provider credentials in a secret manager.
- Keep OpenAI and ElevenLabs credentials backend-only.
- Replace simulated execution adapters with real booking, finance, procurement, or workflow integrations.
- Do not use the development passwords or default JWT secret in production.

## Troubleshooting

If login fails, confirm the app is running and the username/password match the local demo accounts.

If every protected endpoint returns `401`, refresh the JWT token and confirm the `Authorization: Bearer <token>` header is present.

If the app fails on startup with a JWT secret error, set `TRUSTPASS_JWT_SECRET` to a value with at least 32 characters.

If seeded data does not appear, the H2 database already has data. Stop the app and remove the local `data/` directory to reseed from scratch.
