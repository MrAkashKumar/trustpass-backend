# TrustPass backend

Spring Boot 4 REST API for agent authority, policy decisions, verified consent, execution gating, and tamper-evident auditing.

## Local start

```bash
mvn spring-boot:run
```

The API is available at `http://localhost:8080/api/v1`. Health is at `/actuator/health`.

## Configuration

| Environment variable | Purpose | Local default |
| --- | --- | --- |
| `TRUSTPASS_JWT_SECRET` | HMAC JWT signing key, minimum 32 characters | development-only value |
| `TRUSTPASS_CORS_ORIGINS` | Comma-separated frontend origins | `http://localhost:3000` |
| `TRUSTPASS_ADMIN_PASSWORD` | Development administrator password | `Admin123!` |
| `TRUSTPASS_APPROVER_PASSWORD` | Development approver password | `Approve123!` |
| `TRUSTPASS_AUDITOR_PASSWORD` | Development auditor password | `Audit123!` |
| `OPENAI_ENABLED` | Enable live OpenAI risk adapter | `false` |
| `OPENAI_API_KEY` | Backend-only OpenAI API key | none |
| `OPENAI_MODEL` | Risk classification model | `gpt-5.4-nano` |
| `ELEVENLABS_ENABLED` | Enable live voice calls | `false` |
| `ELEVENLABS_API_KEY` | Backend-only ElevenLabs key | none |
| `ELEVENLABS_AGENT_ID` | ElevenLabs agent identifier | none |
| `ELEVENLABS_PHONE_NUMBER_ID` | Configured outbound number ID | none |
| `ELEVENLABS_WEBHOOK_SECRET` | HMAC webhook secret | none |

Live integrations are opt-in. With no provider credentials, deterministic local adapters keep the complete workflow demonstrable.

## Main endpoints

- `POST /api/v1/auth/login`
- `GET|POST /api/v1/agents`
- `GET|POST /api/v1/policies`
- `GET|POST /api/v1/approvals`
- `GET /api/v1/approvals/{id}`
- `POST /api/v1/approvals/{id}/decision`
- `POST /api/v1/approvals/{id}/execute`
- `GET /api/v1/audit-events`
- `GET /api/v1/dashboard`
- `POST /api/v1/webhooks/elevenlabs` (HMAC signed, public ingress)

Use `Authorization: Bearer <token>` for every endpoint except login, health, and the signed webhook.

## Production notes

Run with the `prod` profile and supply all secrets externally. Replace H2 with PostgreSQL, use schema migrations, connect enterprise OIDC, and implement real execution adapters. Do not use development credentials or the development JWT fallback in any deployed environment.

