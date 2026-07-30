# Toast Notifications — Design Spec

## Context

`FrontEnd-Next` has no feedback mechanism today for the outcome of user actions (saving a vehicle, deleting one, a failed login, etc.) beyond closing a modal. This spec defines a self-built (no external library) toast notification system, mounted once at the app root so any screen in the system can trigger one — not limited to the vehicle CRUD flow that motivated the request.

Design decisions below were validated interactively with mockups (visual companion session `.superpowers/brainstorm/396-1785453585/`) and a short Q&A. Nothing here is speculative.

## Visual design

- **Position:** fixed, top-right of the viewport. Matches the convention used by most dashboard tools (Gmail, Linear, Vercel) and doesn't collide with the sidebar (left) or the page footer.
- **Card shell:** white background, `rounded-2xl`, `border border-gray-100`, `shadow-xl shadow-gray-900/10` — identical to the modal/dialog shell already used in `VehicleFormModal` and `DeleteVehicleDialog`. No tinted backgrounds; color is used only where semantically needed.
- **Icon badge:** square, `rounded-lg`, `bg-{token}-50` background with `text-{token}-600` icon color — the same treatment `KpiTile` already uses for its icon square. Not a solid circular badge.
- **Close button:** small `rounded-lg` button, `text-gray-400` default, `hover:bg-gray-100 hover:text-gray-600` — same visual treatment as the "X" in `VehicleFormModal`.
- **Progress bar:** a thin (`~2.5px`) bar along the bottom edge of the card, filled with `bg-{token}-500`, animating from 100% to 0% width over the toast's lifetime. Gives a visual cue of how long is left before auto-dismiss, and is what visually differentiates this from the plainer alternatives considered.
- **Stacking:** newest toast appears closest to the top bar edge; older ones stack below it, `gap-2` between cards.

## Type → color token mapping

The public API exposes three trigger methods: `success`, `error`, `warning`. Internally each maps to a color token from the existing palette (`CLAUDE.md` → Color palette section):

| Method    | Internal type | Color token | Icon  |
|-----------|---------------|-------------|-------|
| `success` | `success`     | `success`   | check |
| `error`   | `danger`      | `danger`    | x     |
| `warning` | `warning`     | `warning`   | !     |

The public method is named `error` (matches how callers think about it — "this failed") while the internal type/token is `danger` (matches the Tailwind token name already defined in `globals.css`). This mapping must be explicit in the implementation (e.g. a `TOAST_METHOD_TO_TOKEN` map) so the two vocabularies don't drift.

`info` is not in scope — add it later if a real use case shows up (YAGNI).

## Public API

```ts
const { success, error, warning } = useToast();

success("Veículo salvo", "ABC-1234 atualizado com sucesso.");
error("Não foi possível excluir", "Tente novamente em instantes.");
warning("Placa já cadastrada"); // description is optional
```

Signature: `(title: string, description?: string) => void`. Chosen over a single `toast({ type, title, description })` call for brevity at call sites — this is the form that will be written dozens of times across the app (every mutation's success/error path).

## Architecture

**Approach: React Context + Provider**, not a module-level store/event-emitter. Every trigger site is inside a React component or hook (form submit handlers, mutation callbacks) — nothing in this codebase today needs to fire a toast from outside the component tree, so the extra complexity of a `useSyncExternalStore`-based global store isn't justified (YAGNI). Revisit this if a future need arises (e.g. a shared fetch wrapper that toasts on any failed request).

- `ToastProvider` wraps `{children}` once in `app/layout.tsx`. Holds `Toast[]` state via `useReducer` with two actions: `ADD` and `REMOVE`.
- `useToast()` (`hooks/useToast.ts`) consumes the context and returns the three bound trigger methods. Throws if called outside `ToastProvider` (should be unreachable since the provider wraps the whole app, but guards against a future refactor breaking that assumption).
- `ToastViewport` is rendered once by `ToastProvider`, fixed-positioned top-right, and renders the current `Toast[]` as `ToastCard`s.
- `ToastCard` owns its own dismiss timer (see Behavior below) and calls `dispatch({ type: "REMOVE", id })` when it expires or the user clicks close.

### Files

```
FrontEnd-Next/
├── components/ui/toast/
│   ├── ToastProvider.tsx    # context, reducer, provider component
│   ├── ToastViewport.tsx    # fixed-position stack container
│   └── ToastCard.tsx        # single toast: badge, title, description, close, progress bar
└── hooks/
    └── useToast.ts          # public hook
```

`ToastProvider` is mounted in `app/layout.tsx`, wrapping `{children}` inside `<body>`.

## Behavior

- **Duration:** fixed 5000ms for every type. No per-type variation — keeps the mental model simple and the implementation uniform.
- **Pause on hover:** while the pointer is over a `ToastCard`, its dismiss timer pauses (tracked via remaining-time state, not just `clearTimeout`) and the progress bar animation pauses with it. On `mouseleave`, both resume from where they left off.
- **Manual close:** the close button dispatches `REMOVE` immediately, bypassing the timer.
- **Stacking limit:** maximum 4 toasts visible at once. When a 5th is added, the oldest (`toasts[0]`) is removed immediately to make room — no queueing, no silent drop of the newest.
- **Route navigation:** since `ToastProvider` lives above the router in the root layout, toasts persist across client-side navigation (e.g. a toast fired right before redirecting still finishes its lifetime on the next page). This is the default and intentional — no special handling needed.
- **Empty description:** when `description` is omitted, the card renders title-only (no empty second line).

## Accessibility

- `ToastViewport` is a live region: `role="status"` + `aria-live="polite"`, so screen readers announce new toasts without interrupting whatever the user is currently doing. `polite` (not `assertive`) is sufficient for this app — nothing here is a critical/blocking alert.
- Close button: `aria-label="Fechar notificação"`.
- Toasts are not focus-trapping and don't steal focus on appearance — they're supplementary feedback, not modal.

## Explicitly out of scope

- **`info` toast type** — no current use case.
- **Module-level/global store** — revisit only if a real need to trigger toasts from outside the component tree appears.
- **Automated tests** — `FrontEnd-Next` has no test runner configured yet (no Vitest/Testing Library, unlike the `frontend/` prototype). This feature ships with manual QA only; setting up a test framework for `FrontEnd-Next` is a separate, unrelated piece of work.
- **Queueing beyond the 4-toast cap** — excess toasts are dropped (oldest first), not queued for later display.
