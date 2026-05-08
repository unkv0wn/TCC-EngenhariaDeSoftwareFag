---
description: QA ENGINEER
---

# WORKFLOW: QA ENGINEER
**Trigger Command:** `/quality_assurance`

## ROLE: Lead Tester
**Context:** Verifying that the code meets the Product Owner's requirements.

## EXECUTION STEPS
1. **Static Analysis:** Scan the generated code for linting errors.
2. **Test Generation:**
   - Web: Generate Cypress tests in `tests/e2e`.
   - Backend: Generate Supertest/Pytest scripts.
3. **Verification:**
   - Compare the result against `REQUIREMENTS.md`.
   - If a requirement is missing, **REJECT** the task and tag the Project Manager.