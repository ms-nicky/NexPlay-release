# NexPlay — Cloud Gaming UI & UX Blueprint

> A comprehensive reference for the full user experience, screen map, component
> inventory, session lifecycle, and design tokens of the NexPlay Android client.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Tech Stack & Design System](#2-tech-stack--design-system)
3. [App Entry & Navigation](#3-app-entry--navigation)
4. [Screen Map](#4-screen-map)
5. [Component Inventory](#5-component-inventory)
6. [Cloud Gaming Session Flow](#6-cloud-gaming-session-flow)
7. [Input Handling](#7-input-handling)
8. [State Management & Data Flow](#8-state-management--data-flow)
9. [API Integration](#9-api-integration)
10. [Error Handling & Recovery](#10-error-handling--recovery)
11. [Design Tokens](#11-design-tokens)
12. [Performance Considerations](#12-performance-considerations)
13. [Accessibility](#13-accessibility)

---

## 1. Project Overview

| Item | Detail |
|---|---|
| **Package** | `com.nexplay` |
| **Architecture** | MVVM — single `NexPlayViewModel` + Jetpack Compose |
| **Language** | Kotlin, 100% Compose (no XML layouts) |
| **Min SDK** | 21 (Android 5.0) |
| **Target SDK** | 37 |
| **Total Kotlin files** | 53 |
| **Lines of code** | ~24,658 (all Kotlin), ~33,704 (all source) |
| **UI source lines** | ~16,338 Kotlin + ~5,297 XML |
| **Key screens file** | `NexPlayScreens.kt` — 15,155 lines (all `@Composable` functions) |
| **Key streaming file** | `Streaming.kt` — 8,173 lines (WebRTC, codec probing, input routing) |
| **Key API file** | `GfnApi.kt` — 3,325 lines (auth, catalog, sessions) |
| **Key ViewModel** | `NexPlayViewModel.kt` — 3,788+ lines |
| **Remote repo** | `https://github.com/ms-nicky/NexPlay-release` |

### Core File Map

```
android/app/src/main/java/com/nexplay/
├── NexPlayApplication.kt          # App entry — initializes FileLog, OkHttp
├── NexPlayViewModel.kt            # ViewModel — sessions, queue, recovery, controller
├── NexPlayScreens.kt              # ALL @Composable screens (~200 functions)
├── NexPlayButtons.kt              # Shared button components
├── NexPlaySettingsScreens.kt      # Settings screen composables
├── NexPlaySettingsPanels.kt       # Settings panel composables
├── NexPlaySettingsControls.kt     # Reusable settings controls (toggle, dropdown, etc.)
├── NexPlayAnalytics.kt            # Firebase Analytics wrappers
├── Streaming.kt                   # WebRTC + codec probing + input handling
├── GfnApi.kt                      # GFN API, auth, catalog, session management
├── Models.kt                      # All data classes
├── Persistence.kt                 # SharedPreferences + JSON serialization
├── Diagnostics.kt                 # HTTP diagnostics, paste upload
├── YouTubeLiveBroadcastService.kt # YouTube Live streaming service
├── StreamFileLogger.kt            # Debug file logging to Downloads folder
├── LocalServer.kt                 # Local HTTP server for multicast streaming
├── AvdHelper.kt                   # Android emulator detection
├── NetFeedbackTest.kt             # Network diagnostics page
├── QosFeedback.kt                 # QoS feedback page
├── Perfscale.kt                   # Per-session performance overlay
├── PerfscaleGlobal.kt             # Global performance overlay
├── PdfGenerator.kt                # PDF report generation
├── NetworkTest.kt                 # Network test page
└── ui/
    ├── theme/
    │   ├── Color.kt               # NexPlayPalette — 18 named colors
    │   ├── Motion.kt              # Duration tokens + easing curves
    │   ├── Spacing.kt             # Spacing tokens (xs–4xl)
    │   ├── Shape.kt               # Radius tokens (xs–xl)
    │   └── Type.kt                # Inter Variable font, type scale
    └── animation/
        └── EntranceAnimations.kt  # staggeredEntrance() + popInEntrance()
```

---

## 2. Tech Stack & Design System

### Dependencies

| Library | Purpose |
|---|---|
| Jetpack Compose BOM `2025.01.01` | Core UI |
| Material3 `1.3.1` | Material Design 3 |
| Navigation Compose `2.9.1` | Navigation |
| Coil `3.2.0` (Compose) | Image loading |
| DataStore `1.1.9` | Preferences |
| RootEncoder `2.5.7` | RTMP/SRT/RTSP streaming |
| Gson `2.13.3` | JSON serialization |
| OkHttp `4.12.0` | HTTP client |
| AGP `9.2.1` | Android Gradle Plugin |
| Kotlin `2.3.10` | Language |
| NDK `28.2` | Native (WebRTC) |
| CMake `3.22.1` | Native build |

### Theme System

**File:** `ui/theme/` — 5 files

- **Color.kt** — `NexPlayPalette` object with 18 named colors, all derived from `Color(0xFF6C5CE7)` accent
- **Type.kt** — Inter Variable font (loaded via `Font` from `res/font/inter_variable.ttf`), `Typography` scale with 6 text styles (`titleLarge` 20sp → `labelSmall` 10sp)
- **Motion.kt** — `DurationFast` 150ms, `DurationStandard` 250ms, `DurationEmphasized` 400ms; `FastEaseOut`, `StandardEaseInOut`, `EmphasizedEasing` (custom cubic-bezier)
- **Spacing.kt** — `Spacing` composable + `LocalSpacing` compositionLocal; xs 4dp → 4xl 80dp
- **Shape.kt** — `NexPlayShape` composable + `LocalShape` compositionLocal; xs 4dp → xl 24dp

**Theme entry point:** `NexPlayTheme` composable in `ui/theme/NexPlayTheme.kt` provides `MaterialTheme`, `LocalSpacing`, `LocalShape`, plus window insets provider.

---

## 3. App Entry & Navigation

### Entry Point

- `NexPlayApplication` (extends `Application`) — initializes `FileLog.init(this)` + sets global `OkHttpClient`
- Main activity: `MainActivity` with `setContent { NexPlayTheme { NexPlayApp() } }`
- `AndroidManifest.xml` uses `Theme.NexPlay`, `android:windowSoftInputMode="adjustResize"`
- Deep link: `nexplay://` scheme

### Navigation Graph

```
NavHost("settings")  ← START
│
├── "settings"              → NexPlaySettingsScreen
│
├── "login"                 → LoginScreen
│
├── "main"                  → NexPlayMainScreen (bottom nav: Library, Search, Play History)
│   ├── "library/{userId}"  → Library (with Play & Continue row)
│   ├── "search/{userId}"   → Search
│   └── "playHistory/{userId}" → Play History
│
├── "profile/{userId}"      → Profile screen
│
├── "game/{gameId}"         → Game detail screen
│   └── nested: "gameDetailQueue/{gameId}" → In-game queue overlay
│
├── "networkTest"           → NetworkTest page
├── "netFeedbackTest"       → NetFeedbackTest page
├── "diagnostics"           → Diagnostics page
│
└── "streaming"             → StreamingScreen (WebRTC session)
```

### Navigation Helpers

- `NavBackStackEntry.isInGameDestination()` — checks current route
- `NavBackStackEntry.isProfileDestination()` — checks profile route
- `NavController.navigateToSettings()` — clears backstack to settings
- `NavController.navigateToMain(userId)` — clears backstack to main
- `NavController.navigateToLogin()` — clears backstack to login

---

## 4. Screen Map

### 4.1 Settings (`NexPlaySettingsScreens.kt`)

**SettingsScreen** — entry with 7 categories:

| Section | Fields |
|---|---|
| **Network** | Multicast toggle |
| **Servers** | SDK server selector, Enable Bug Report toggle |
| **Input** | Touchscreen controller toggle |
| **Streaming** | Hardware decoding, Render effect, Scaling, Bitrate, FPS |
| **Debug** | Logcat Logger toggle, Microphone toggle |
| **Look** | Theme (system/light/dark), Colour (green/purple/blue/red) |
| **About** | License, Privacy Policy links |

**Perfscale** (performance overlay controls):
- Enable/Disable toggle, Position selector (4 corners), Scale slider (50–200%)
- Stats toggles: FPS, Resolution, Bitrate, Codec, Server FPS, Network latency, GPU/CPU usage, Ping
- Back button saves settings

**QuickSettingsPanel** — draggable overlay during streaming with MediaSession, Go Live, Audio, Orientation, Screenshot, Volume, and ESC buttons.

### 4.2 Login (`LoginScreen`)

- Email input with validation (contains `@`)
- Login button → `viewModel.login(email)` → navigates to `main` on success
- Back button → navigates to `settings`
- "Or" divider between email input and other options (if any)

### 4.3 Main — Library/Search/Play History (`NexPlayMainScreen`)

**Bottom Navigation:** Library, Search, Play History

#### Library
- **Play & Continue row** (horizontal scroll): Recent games with continue/resume
- **Popular games row**: Grid of GameCards
- **Quick launch row**: Recently played games
- Each GameCard: gradient overlay, staggered entrance animation, pop-in animation

#### Search
- Search bar with keyboard submit
- Search results in grid layout
- Empty state: "Game not found"

#### Play History
- List of past sessions with timestamps
- Pull to refresh

### 4.4 Game Detail (`GameDetailScreen`)

- Hero image (full-width, `contentScale = FillMaxWidth`, 280dp height)
- Game title (headlineSmall)
- Play button (filled, 120dp min width)
- Related games row (horizontal scroll)
- Queue screen: `GameDetailQueueScreen` with queue status, wait time, cancel button

### 4.5 Profile (`ProfileScreen`)

- User avatar (coiled image)
- User display name
- Sign out button

### 4.6 Streaming (`StreamingScreen`)

Full-screen WebRTC streaming with:

- **Controls overlay** (auto-hide after 3s): Back button, Toggle controls button
- **Control menu** (tap toggle button): Screenshot, Escape, Virtual mouse, Performance overlay toggle, Microphone toggle, YouTube Live, YouTube viewer count, Session info, FPS stats, Server stats
- **WebRTC view** (`WebRTCView` composable)
- **TSP client** for input forwarding

### 4.7 Network/Diagnostics Pages

- **NetworkTest** — Network connectivity test
- **NetFeedbackTest** — Network feedback diagnostics
- **Diagnostics** — HTTP diagnostics with paste upload
- **QosFeedback** — QoS feedback page
- **NetStatsOverlay** — Network stats overlay
- **PerfscaleOverlay** — Performance overlay (configurable position + stats)

---

## 5. Component Inventory

### Shared Buttons (`NexPlayButtons.kt`)

| Component | Description |
|---|---|
| `NexPlayButton` | Standard button with loading state, controller focus support |
| `NexPlayIconButton` | Icon-only button |
| `NexPlayTextButton` | Text-only button |
| `NexPlayOutlinedButton` | Outlined variant |
| `NexPlayBackButton` | Back navigation button |

### Settings Controls (`NexPlaySettingsControls.kt`)

| Component | Description |
|---|---|
| `NexPlayToggle` | Toggle switch with label |
| `NexPlayDropdown` | Dropdown selector |
| `NexPlaySlider` | Slider with label and value |
| `NexPlayRadioGroup` | Radio button group |
| `NexPlayClickableRow` | Clickable list item |
| `NexPlaySectionHeader` | Section title |

### Game Cards

- **Gradient overlay**: `Brush.verticalGradient` from transparent → `surfaceColor.copy(alpha = 0.8f)` at 40% → full `surfaceColor` at 100%
- **Staggered entrance animation**: `Modifier.staggeredEntrance()` — fade + slide up, 300ms duration, 80ms delay per item
- **Pop-in animation**: `Modifier.popInEntrance()` — scale from 0.8f → 1.0f with bounce easing
- **Focus glow**: On controller focus, animated border glow using `BorderStroke` + `animateColorAsState`

### Hero Image

- Full-width, 280dp height
- `contentScale = FillMaxWidth`
- Gradient overlay at bottom for text readability

---

## 6. Cloud Gaming Session Flow

### Complete Lifecycle

```
┌─────────────────────────────────────────────────────────────┐
│                    SESSION LIFECYCLE                         │
│                                                             │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐              │
│  │  IDLE    │───→│QUEUED    │───→│CONNECTING│              │
│  └──────────┘    └──────────┘    └──────────┘              │
│       │               │               │                     │
│       │               │               ▼                     │
│       │               │         ┌──────────┐               │
│       │               │         │ CONNECTED│               │
│       │               │         └──────────┘               │
│       │               │               │                     │
│       │               ▼               ▼                     │
│       │         ┌──────────┐   ┌──────────┐               │
│       │         │CANCELLED │   │ RECOVERY │               │
│       │         └──────────┘   └──────────┘               │
│       │                                  │                  │
│       ▼                                  ▼                  │
│  ┌──────────┐                    ┌──────────┐              │
│  │ ENDED    │◄───────────────────│ FAILED   │              │
│  └──────────┘                    └──────────┘              │
└─────────────────────────────────────────────────────────────┘
```

### State Fields in NexPlayViewModel

| Field | Type | Description |
|---|---|---|
| `uiState` | `NexPlayUiState` | Current screen state (Loading/Success/Error) |
| `isInQueue` | `Boolean` | Whether user is in queue |
| `isStreaming` | `Boolean` | Whether streaming is active |
| `isConnected` | `Boolean` | Whether WebRTC is connected |
| `currentSessionId` | `String?` | Active session ID |
| `queuePosition` | `Int` | Position in queue |
| `estimatedWaitTime` | `String?` | Human-readable wait time |
| `gameTitle` | `String?` | Current game title |
| `gameThumbnailUrl` | `String?` | Current game thumbnail |
| `recoveryState` | `RecoveryState?` | Session recovery state |
| `connectionState` | `ConnectionState` | WebRTC connection state |
| `error` | `String?` | Current error message |

### Session Flow Steps

1. **Game Selection** → `viewModel.playGame(gameId)`
2. **Queue Entry** → `viewModel.enterQueue(gameId)` → `isInQueue = true`
3. **Queue Wait** → `queuePosition`, `estimatedWaitTime` updated via polling/WebSocket
4. **Session Start** → `viewModel.startSession(sessionData)` → `isStreaming = true`
5. **WebRTC Connect** → ICE/SDP exchange → `isConnected = true`
6. **Streaming** → Input handling, codec adaptation, performance monitoring
7. **Session End** → `viewModel.endSession()` → cleanup

### Recovery Flow

- Triggered when WebRTC disconnects unexpectedly
- `RecoveryState` tracks: `isRecovering`, `retryCount`, `maxRetries`, `lastError`
- `createFreshRecoverySession()` — creates new recovery session (avoids stale state)
- Exponential backoff: 2s → 4s → 8s → 16s → 32s → 60s (max)
- Max retries: 5 before failure

---

## 7. Input Handling

### Touch Input

- **Single touch** → Virtual gamepad (if enabled)
- **Multi-touch** → Mouse emulation (2-finger = scroll, 3-finger = right-click)
- **Finger Mouse gesture** — preserved from upstream, allows mouse control via touch
- Touch session recovery for disconnections

### Controller Input

- Android `InputDevice` detection via `InputDeviceCompat`
- Button mapping: `KEYCODE_BUTTON_A`, `KEYCODE_BUTTON_B`, etc.
- `isPhysicalGamepad` check for hardware controllers
- **Select button on TV** — mapped to escape/back (NexPlay addition)

### Mouse Input

- **MouseMotionAccumulator** — accumulates mouse motion deltas for precision
- External mouse detection → bypasses virtual gamepad
- Relative mouse mode for FPS games

### Keyboard Input

- `KeyEvent` handling for controller buttons
- D-pad mapping to arrow keys
- Function keys mapped to game actions

### Input Routing (Streaming.kt)

```
Touch/Input Event
    │
    ▼
InputRouter
    │
    ├── Physical Controller → NativeControllerHandler
    │
    ├── Touch Screen → VirtualControllerHandler
    │
    ├── Mouse → MouseMotionAccumulator → MouseHandler
    │
    └── Keyboard → KeyboardHandler
```

---

## 8. State Management & Data Flow

### ViewModel Pattern

```
NexPlayViewModel
    │
    ├── Auth State (login status, user ID, token)
    │
    ├── Session State (current game, queue, streaming)
    │
    ├── Recovery State (retry logic, backoff)
    │
    └── Settings State (via Persistence)
```

### State Flow

1. **User Action** → Composable calls `viewModel.method()`
2. **ViewModel** → Updates `MutableState` fields
3. **Compose** → Recomposes affected composables
4. **Side Effects** → `LaunchedEffect` for API calls, animations

### Data Persistence (`Persistence.kt`)

| Storage | Content |
|---|---|
| `SharedPreferences` | Auth token, user ID, settings |
| `DataStore` | User preferences (theme, color, streaming settings) |
| `FileLog` | Debug logs to Downloads folder |
| **Google Drive** | Cross-device sync of settings, controller mappings, auth, persistent settings |

---

## 9. API Integration

### GFN API (`GfnApi.kt`)

| Endpoint | Method | Purpose |
|---|---|---|
| `/auth/login` | POST | Email login |
| `/auth/refresh` | POST | Token refresh |
| `/games` | GET | Game catalog |
| `/games/{id}` | GET | Game detail |
| `/sessions` | POST | Start session |
| `/sessions/{id}` | GET | Session status |
| `/sessions/{id}/cancel` | POST | Cancel session |
| `/sessions/{id}/end` | POST | End session |
| `/queue/join` | POST | Join queue |
| `/queue/status` | GET | Queue position |
| `/queue/leave` | POST | Leave queue |
| `/streaming/sdp` | POST | WebRTC SDP exchange |
| `/streaming/ice` | POST | ICE candidate exchange |

### Auth Flow

1. User enters email → `viewModel.login(email)`
2. API returns `authToken` + `userId`
3. Stored in `Persistence` (SharedPreferences + Google Drive)
4. Token auto-refreshed on 401 responses
5. Logout clears all stored data

### Catalog Caching

- Game catalog cached locally via `Persistence`
- Cache TTL: 1 hour
- Force refresh available via pull-to-refresh

---

## 10. Error Handling & Recovery

### Error States

| Error Type | User Message | Recovery |
|---|---|---|
| Network timeout | "Connection timed out" | Auto-retry (3x) |
| Auth expired | "Session expired" | Auto-refresh token |
| Queue full | "Queue is full" | Wait or cancel |
| Session failed | "Session failed to start" | Retry |
| WebRTC disconnect | "Connection lost" | Recovery flow |
| Codec not supported | "Codec not supported" | Fallback to compatible |

### Recovery Mechanism

- **Automatic retry** with exponential backoff
- **Session recovery** — attempts to rejoin same session
- **Fresh session creation** — `createFreshRecoverySession()` for stale sessions
- **User notification** — error snackbar with retry action

### Error Display

- `Snackbar` with action button
- `ErrorDialog` for critical errors
- `StreamErrorOverlay` during streaming

---

## 11. Design Tokens

### Colors (`ui/theme/Color.kt` — NexPlayPalette)

| Token | Hex | Usage |
|---|---|---|
| `accent` | `#6C5CE7` | Primary brand color (purple) |
| `accentLight` | `#A29BFE` | Light accent |
| `accentDark` | `#5A4BD1` | Dark accent |
| `surface` | `#1A1A2E` | Background |
| `surfaceLight` | `#2D2D44` | Card background |
| `surfaceDark` | `#0F0F1A` | Deep background |
| `onSurface` | `#FFFFFF` | Text on surface |
| `onSurfaceVariant` | `#B0B0B0` | Secondary text |
| `error` | `#FF6B6B` | Error state |
| `success` | `#51CF66` | Success state |
| `warning` | `#FFD43B` | Warning state |

### Spacing (`ui/theme/Spacing.kt`)

| Token | Value |
|---|---|
| `xs` | 4dp |
| `sm` | 8dp |
| `md` | 16dp |
| `lg` | 24dp |
| `xl` | 32dp |
| `2xl` | 48dp |
| `3xl` | 64dp |
| `4xl` | 80dp |

### Shape (`ui/theme/Shape.kt`)

| Token | Value |
|---|---|
| `xs` | 4dp |
| `sm` | 8dp |
| `md` | 12dp |
| `lg` | 16dp |
| `xl` | 24dp |

### Typography (`ui/theme/Type.kt`)

| Style | Size | Weight |
|---|---|---|
| `displayLarge` | 57sp | Bold |
| `headlineLarge` | 32sp | SemiBold |
| `headlineMedium` | 28sp | SemiBold |
| `titleLarge` | 22sp | Medium |
| `titleMedium` | 16sp | Medium |
| `bodyLarge` | 16sp | Regular |
| `bodyMedium` | 14sp | Regular |
| `labelLarge` | 14sp | Medium |
| `labelMedium` | 12sp | Medium |
| `labelSmall` | 10sp | Medium |

### Motion (`ui/theme/Motion.kt`)

| Token | Duration | Easing |
|---|---|---|
| `DurationFast` | 150ms | `FastEaseOut` |
| `DurationStandard` | 250ms | `StandardEaseInOut` |
| `DurationEmphasized` | 400ms | `EmphasizedEasing` |

### Entrance Animations (`ui/animation/EntranceAnimations.kt`)

| Modifier | Effect | Duration | Delay |
|---|---|---|---|
| `Modifier.staggeredEntrance()` | Fade + slide up | 300ms | 80ms per item |
| `Modifier.popInEntrance()` | Scale 0.8→1.0 + bounce | 300ms | 80ms per item |

---

## 12. Performance Considerations

### Rendering

- **Hardware decoding** enabled by default (toggle in settings)
- **Render effect** options (blur, etc.)
- **Scaling** options (fit, fill, stretch)
- **FPS overlay** for monitoring

### Memory

- Coil image caching with memory + disk cache
- Catalog data cached locally
- Session state cleaned up on exit

### Network

- **Adaptive bitrate** based on network conditions
- **Codec probing** — tries hardware codecs first, falls back to software
- **Exynos HEVC detection** — special handling for Samsung Exynos devices
- **Multicast streaming** option for local network

### Startup

- **LazyColumn** for all lists (virtual scrolling)
- **LaunchedEffect** for deferred loading
- **remember** for expensive computations

---

## 13. Accessibility

### Current Support

- **Content descriptions** on all images
- **Semantic properties** on interactive elements
- **Focus indicators** for controller/keyboard navigation
- **Touch target sizes** ≥ 48dp (Material3 default)
- **High contrast** theme options (green, purple, blue, red)

### Controller Navigation

- All buttons support focus traversal
- D-pad navigation between elements
- Visual focus indicator (animated border glow)

---

## Appendix: Key Files Quick Reference

| File | Lines | Purpose |
|---|---|---|
| `NexPlayScreens.kt` | 15,155 | All screen composables |
| `Streaming.kt` | 8,173 | WebRTC + input handling |
| `NexPlayViewModel.kt` | 3,788 | ViewModel + state management |
| `GfnApi.kt` | 3,325 | GFN API + auth |
| `Models.kt` | 1,056 | Data classes |
| `Persistence.kt` | 656 | Local storage |
| `NexPlaySettingsScreens.kt` | 924 | Settings screens |
| `NexPlaySettingsPanels.kt` | 1,575 | Settings panels |
| `NexPlaySettingsControls.kt` | 1,394 | Reusable controls |
| `NexPlayButtons.kt` | 1,073 | Shared buttons |
| `NexPlayAnalytics.kt` | 212 | Analytics wrappers |
| `EntranceAnimations.kt` | 167 | Animation modifiers |
| `ui/theme/Color.kt` | 38 | Color tokens |
| `ui/theme/Motion.kt` | 22 | Motion tokens |
| `ui/theme/Spacing.kt` | 22 | Spacing tokens |
| `ui/theme/Shape.kt` | 36 | Shape tokens |
| `ui/theme/Type.kt` | 34 | Typography |

---

*Last updated: August 2026 — NexPlay v1.4.0 (versionCode 78)*
