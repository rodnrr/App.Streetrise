# Movable Floating Notification Bubble & Firebase FCM Fix

Transform the static Always-On Community Chat widget into a compact, draggable floating notification bubble with magnetic edge snapping so it never obstructs maps or resource cards, and resolve the Firebase FCM API key initialization error.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following key decisions were confirmed through interactive clarification and will guide the implementation:

- **Confirmed Decision 1 (Movement & Physics)**: Free drag across the screen with smooth spring-based magnetic snapping to the nearest left or right edge when released, keeping the center clear.
- **Confirmed Decision 2 (Visual Style)**: Compact circular bubble (56dp diameter) featuring an icon, live pulsing ring, and unread notification badge, replacing the wider static card.
- **Confirmed Decision 3 (Touch Discrimination)**: Tapping cleanly opens the full Community Chat sheet, while dragging moves the bubble smoothly across screen boundaries with safe edge clamping.
- **Confirmed Decision 4 (Firebase FCM Fix)**: Initialize Firebase with a properly formatted Google API key (`AIza...`) and graceful fallback handling so unconfigured or development environments never crash or log `IllegalArgumentException`.

---

## 1. Overview & Core Concept

### What It Does
- **Floating Movable Bubble**: A circular floating button representing the StreetRise Community Chat that users can drag anywhere on screen. When released, it smoothly glides and docks to the nearest screen edge (left or right) to stay out of the way of map pins, search inputs, and resource cards.
- **One-Tap Community Access**: Tapping the bubble expands the full StreetRise Community Chat sheet. Users can view alerts, post messages, react to helpful notices, and toggle Always-On mode.
- **Robust Firebase & Token Management**: Ensures Firebase Messaging initializes cleanly without throwing `IllegalArgumentException: Please set a valid API key`, generating a local fallback token when remote credentials are unavailable.

### Target Audience & Persona
- **Unhoused neighbors & community members**: Needing continuous access to peer questions, food updates, and open bed notices without interface elements blocking interactive maps.
- **Outreach volunteers & service navigators**: Multitasking between checking service details and monitoring live field communications.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Browsing with Draggable Bubble**: While viewing the Web Map or Offline Guide, the user sees a compact 56dp circular bubble docked against the screen edge.
2. **Repositioning**: If the bubble blocks a map pin or button, the user drags it freely across the screen. On release, it magnetically springs to the closest edge (left or right margin).
3. **Opening Chat**: A tap (without drag movement) expands the full Community Chat sheet with smooth slide-up animation.
4. **Toggling Always-On**: The user can toggle Always-On mode off from the top bar chip, the chat sheet header switch, or the About screen settings card, hiding the floating bubble whenever desired.

### Visual Identity & Theme
- **Bubble Container**: 56dp circular surface with `MaterialTheme.colorScheme.primaryContainer`, bordered with `MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)` and elevated with an 8dp drop shadow.
- **Live Indicator**: Pulsing emerald green beacon (animated scale 0.85x to 1.25x) indicating live community activity.
- **Unread Badge**: Elevated crimson badge (`MaterialTheme.colorScheme.error`) positioned at the top-right corner of the circle displaying the unread count.
- **Touch & Motion Feedback**: Ripple effect on tap, elevation lift during drag, and M3 spring physics (`Spring.DampingRatioMediumBouncy`, `Spring.StiffnessLow`) when snapping to edges.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Gesture Discrimination (`pointerInput` with `detectDragGestures`)**:
  - *Chosen Approach*: Track drag displacement offset; if cumulative drag is below touch slop threshold (e.g. < 8dp), treat as a click to open chat. If above threshold, update drag offset and trigger edge snap on release.
  - *Why*: Eliminates accidental chat openings while dragging and prevents "stuck" states on Android touchscreens.
- **Decision 2: Safe Inset Clamping**:
  - *Chosen Approach*: Clamp vertical drag offset between top status bar padding (60dp) and bottom navigation bar padding (80dp).
  - *Why*: Prevents users from dragging the bubble under the system notification bar or underneath the bottom navigation bar.
- **Decision 3: Firebase Initialization Resilience**:
  - *Chosen Approach*: Provide a valid format API key string (`AIza...`) in `FirebaseOptions` and wrap token retrieval in safe try-catch blocks with graceful device fallback.
  - *Why*: Eliminates the fatal `IllegalArgumentException` thrown by `FirebaseInstallations.preConditionChecks` while keeping push listener compatibility active for the WebView bridge.

---

## 4. Technical Architecture & Data Strategy

### System Component Diagram

```
┌──────────────────────────────────────────────────────────────┐
│                    MainActivity / Root Box                   │
│                                                              │
│  ┌─────────────────────────┐    ┌─────────────────────────┐  │
│  │ Top Quick Toggle Chip   │    │  Screen Content         │  │
│  │ (Always-On ON/OFF)      │    │  (Web Map / Guide /     │  │
│  └──────────┬──────────────┘    │   Saved / Providers)    │  │
│             │                   └─────────────────────────┘  │
│             ▼                                                │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ MovableFloatingChatBubble                              │  │
│  │  - PointerInput Drag Detection                         │  │
│  │  - Edge Snap Spring Physics (Animatable X/Y)           │  │
│  │  - Pulsing Live Beacon & Unread Badge                  │  │
│  └──────────────────────────┬─────────────────────────────┘  │
│                             │ tap                            │
│                             ▼                                │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ CommunityChatSheet (Animated Slide-Up Sheet)           │  │
│  │  - Always-On Toggle Switch                             │  │
│  │  - Message Stream, Reactions & Quick Prompts           │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ Firebase Initialization & FCM Graceful Token Fallback  │  │
│  └────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

### Component State & Properties
- `CommunityChatViewModel`:
  - `isAlwaysOn: StateFlow<Boolean>`: Controls whether the floating bubble is active.
  - `isChatSheetOpen: StateFlow<Boolean>`: Controls full chat sheet visibility.
  - `unreadCount: StateFlow<Int>`: Increments on new messages while sheet is closed.
- `MovableFloatingChatBubble`:
  - `offsetX: Animatable<Float, AnimationVector1D>`: Horizontal position in pixels, animating to left margin (16dp) or right margin (`screenWidth - bubbleWidth - 16dp`).
  - `offsetY: Animatable<Float, AnimationVector1D>`: Vertical position in pixels, clamped within safe boundaries.

---

## 5. Implementation Steps

1. **Fix Firebase Initialization (`MainActivity.kt`)**:
   - Provide a valid Google API key in `FirebaseOptions.Builder().setApiKey(...)`.
   - Wrap `FirebaseMessaging.getInstance().token` with safe exception handling and fallback token generation.
2. **Build `MovableFloatingChatBubble` (`CommunityChatComponents.kt`)**:
   - Create draggable circular bubble with `BoxWithConstraints` to calculate screen bounds.
   - Implement gesture handling with `detectDragGestures` and `Animatable` edge snapping.
   - Add pulsing green live beacon and unread badge overlay.
3. **Integrate in `MainActivity.kt`**:
   - Replace the static widget with the new `MovableFloatingChatBubble`.
4. **Verification**:
   - Run `compile_applet` to ensure error-free compilation and clean build output.
