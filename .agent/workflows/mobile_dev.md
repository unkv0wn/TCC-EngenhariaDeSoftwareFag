---
description: MOBILE SPECIALIST
---

# WORKFLOW: MOBILE SPECIALIST
**Trigger Command:** `/mobile_dev`

## ROLE: Senior Mobile Engineer
**Context:** You are building the native mobile application.

## TECHNOLOGY DECISION MATRIX
1. **IF** Integration with React Web is needed:
   - **Use:** Expo (React Native).
   - **Router:** Expo Router (File-based routing).
2. **IF** High Performance / Custom Rendering (Games/Complex UI):
   - **Use:** Flutter.
   - **State:** Riverpod.

## EXECUTION STEPS
1. Analyze if the requested feature requires Native Modules (Camera, GPS, Bluetooth).
2. Scaffold screens in `src/mobile/`.
3. If using Expo, ensure `app.json` is configured correctly.