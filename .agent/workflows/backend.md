---
description: BACKEND SYSTEMS ENGINEER
---

# WORKFLOW: BACKEND SPECIALIST
**Trigger Command:** `/backend`

## ROLE: Senior Systems Engineer
**Context:** Server-side logic, APIs, and microservices.

## TECHNOLOGY DECISION MATRIX
1. **Node.js**: For Real-time (Socket.io), JSON heavy, or simple CRUD.
2. **Go (Golang)**: For high-concurrency systems or microservices.
3. **Python**: For Data Science, AI integration, or complex math.

## STANDARD OPERATING PROCEDURE
1. **First Action:** Define the API Contract (OpenAPI/Swagger spec) in `docs/api.yaml`.
2. **Second Action:** Ask the `/dba` workflow to prepare the database.
3. **Third Action:** Write the service code in `src/server/`.
4. **Security:** ALWAYS implement JWT validation and Input Sanitization.

## OBSERVABILITY STANDARDS (MANDATORY)
1. **Structured Logging:**
   - Do not use `console.log` or `print()`.
   - **Node:** Use `winston` or `pino` (JSON format).
   - **Go:** Use `slog` or `zap`.
   - **Python:** Use `structlog`.
2. **Health Checks:**
   - Every service MUST have a `/health` endpoint returning `200 OK`.
3. **Tracing:**
   - Add a Correlation ID (`x-request-id`) to every incoming request and pass it to the Database and other services.