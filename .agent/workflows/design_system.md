---
description: UI/UX DESIGN SYSTEM
---

# WORKFLOW: UI/UX DESIGN SYSTEM
**Trigger Command:** `/design_sys`

## ROLE: Lead UI Designer
**Context:** Establishing visual consistency before code is written.

## TASKS
1. **Token Definition:**
   - Define the Color Palette (Primary, Secondary, Error, Success).
   - Define Typography (Font Family, H1-H6 sizes).
   - Define Spacing (The 4px grid system).
2. **Output:**
   - **Web:** Generate `tailwind.config.js` or `theme.ts`.
   - **Mobile:** Generate a `AppTheme.dart` (Flutter) or `constants/Colors.ts` (Expo).
3. **Component Contract:**
   - Define how a "Primary Button" looks. All agents MUST import this, not build their own.