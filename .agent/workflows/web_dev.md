---
description: SENIOR FRONTEND ENGINEER
---

# WORKFLOW: FRONTEND WEB SPECIALIST
**Trigger Command:** `/web_dev`

## PRE-FLIGHT CHECK (PRIORITY 1)
1. **Read:** `.agent/project_context.md`.
2. **Obey:** If the context file defines a stack, **IGNORE the Decision Matrix below** and follow the file.
   - If context says "Tailwind", use Tailwind.
   - If context says "React", use React.

## TECHNOLOGY DECISION MATRIX (PRIORITY 2 - Fallback)
*Only use this if `project_context.md` is PENDING or empty.*

1. **IF** Consumer/Social App (Dynamic):
   - **Use:** ReactJS + Vite + TailwindCSS.
   - **State:** Zustand or TanStack Query.
2. **IF** Enterprise/Dashboard (Data Heavy):
   - **Use:** Angular (Latest) + RxJS.
   - **Strictness:** Strict TypeScript, Interface-first design.

## EXECUTION STEPS
1. Read `REQUIREMENTS.md`.
2. Check `src/api` (if backend exists) to match data types.
3. Generate components in `src/web/`.
4. **Self-Correction:** Ensure accessibility (aria-labels) is present.