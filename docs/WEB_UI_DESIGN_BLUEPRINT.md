# NexPlay — Web Cloud Gaming UI Design Blueprint

> Premium dark gaming interface. Desktop-first. Identity-preserving.
> Competes with GeForce NOW, Xbox Cloud Gaming, Steam Big Picture, PlayStation UI.

---

## Table of Contents

1. [Design Vision](#1-design-vision)
2. [Layout Architecture](#2-layout-architecture)
3. [Navigation](#3-navigation)
4. [Page Structure](#4-page-structure)
5. [Component System](#5-component-system)
6. [Design Tokens](#6-design-tokens)
7. [Animation Rules](#7-animation-rules)
8. [Responsive Rules](#8-responsive-rules)
9. [Cloud Gaming UX](#9-cloud-gaming-ux)
10. [Implementation Priority](#10-implementation-priority)

---

## 1. Design Vision

### 1.1 Product Positioning

NexPlay Web is a **premium cloud gaming platform** — not a dashboard, not an admin panel, not a template.

Competitive references:
- NVIDIA GeForce NOW (desktop client feel)
- Xbox Cloud Gaming (cinematic immersion)
- Steam Big Picture (launcher density)
- PlayStation UI (clean minimalism)

### 1.2 Core Principles

| Principle | Meaning |
|---|---|
| **Immersive** | Dark OLED aesthetic, cinematic layouts, game-first presentation |
| **Performant** | Every animation < 400ms, skeleton loading, lazy everything |
| **Clear** | Information hierarchy first, decoration second |
| **Consistent** | Same tokens, same patterns, every page |
| **Accessible** | Keyboard nav, focus states, contrast ratios, screen reader |

### 1.3 Anti-patterns (What This Is NOT)

- NOT admin dashboard with sidebar tables
- NOT SaaS landing page with hero CTA
- NOT 1:1 copy of the Android app
- NOT neon-overloaded "gamer" aesthetic
- NOT card-heavy grid with no hierarchy

### 1.4 Visual Direction

**Premium Dark Gaming Interface**

```
Background:  Near-black  (#090B0D)
Surface:     Charcoal    (#11161A)
Elevated:    Lighter     (#171D22)
Border:      White @ 8%
Text:        Off-white   (#EEF3F5)
Muted:       Mid-gray    (#98A4AA)
Accent:      Green       (#6AF0A0)
```

Effects (used sparingly):
- **Glassmorphism** — backdrop-blur 8–12px on overlays
- **Soft glow** — accent box-shadow on focus/hover
- **Gradient overlays** — cinematic on hero images only
- **Micro-interactions** — scale, opacity, translate (never bounce/jelly)

---

## 2. Layout Architecture

### 2.1 Desktop Layout (Primary)

```
┌───────────────────────────────────────────────────────┐
│  ┌────────┐  ┌─────────────────────────────────────┐  │
│  │        │  │  Header Bar                         │  │
│  │        │  │  [≡] Search ............. 🔔 [Avatar]│  │
│  │  Side  │  ├─────────────────────────────────────┤  │
│  │  bar   │  │                                     │  │
│  │        │  │  Main Content Area                  │  │
│  │  Home  │  │  (max-width: 1440px, centered)      │  │
│  │  Store │  │                                     │  │
│  │  Lib   │  │  Scrollable                         │  │
│  │  Srch  │  │                                     │  │
│  │  ────  │  │                                     │  │
│  │  Queue │  │                                     │  │
│  │  Hist  │  │                                     │  │
│  │  ════  │  │                                     │  │
│  │  Sets  │  │                                     │  │
│  │  User  │  └─────────────────────────────────────┘  │
│  └────────┘                                           │
└───────────────────────────────────────────────────────┘
```

### 2.2 Layout Dimensions

| Element | Width | Behavior |
|---|---|---|
| Sidebar | 240px expanded / 72px collapsed | Fixed left, always visible |
| Header | 100% minus sidebar | Sticky top, 64px height |
| Content | Fluid, max-width 1440px | Scrollable, padded 32px |
| Sidebar bottom | Fixed bottom | Settings + User profile |

### 2.3 Sidebar

```
┌────────────────┐
│  ◆ NexPlay     │  ← Logo/brand mark
│                │
│  🏠 Home       │  ← Dashboard
│  🛒 Store      │  ← Browse games
│  📚 Library    │  ← Owned/played
│  🔍 Search     │  ← Global search
│                │
│  ────────────  │
│                │
│  ⏳ Queue      │  ← Active queue
│  📊 History    │  ← Play history
│                │
│  ════════════  │  ← Spacer (flex-grow)
│                │
│  ⚙️ Settings   │  ← Settings page
│  👤 Profile    │  ← Avatar + name
└────────────────┘
```

Active state: accent background pill with 12px radius.
Collapsed: 72px, icon only, tooltip on hover.

---

## 3. Navigation

### 3.1 Routes

| Route | Page | Sidebar |
|---|---|---|
| `/` | Home Dashboard | Home |
| `/store` | Game Store/Browse | Store |
| `/library` | Game Library | Library |
| `/search` | Search Results | Search |
| `/queue` | Active Queue | Queue |
| `/history` | Play History | History |
| `/game/:id` | Game Detail | Store |
| `/settings` | Settings | Settings |
| `/settings/:section` | Settings Section | Settings |
| `/stream` | Cloud Gaming Session | (fullscreen overlay) |
| `/profile` | User Profile | Profile |

### 3.2 Header

```
┌────────────────────────────────────────────────────────┐
│ [≡]   🔍 Search games, genres, developers...    🔔  👤 │
│       ──────────────────────────────────────────       │
│       Instant results dropdown                         │
└────────────────────────────────────────────────────────┘
```

- Hamburger: mobile/tablet only
- Search: global, instant, `Ctrl+K` shortcut
- Notifications: bell icon, queue status
- Profile: avatar dropdown (profile, settings, sign out)

### 3.3 Keyboard Shortcuts

| Key | Action |
|---|---|
| `Ctrl+K` | Focus search |
| `Escape` | Close/Back |
| `Space` | Play selected |
| `F` | Toggle fullscreen |
| `←/→` | Navigate carousel |
| `↑/↓` | Navigate grid |
| `Enter` | Select/confirm |
| `Tab` | Focus traversal |

---

## 4. Page Structure

### 4.1 Home Dashboard

```
┌──────────────────────────────────────────────────────┐
│                                                      │
│  ┌──────────────────────────────────────────────┐    │
│  │  HERO / FEATURED GAME                        │    │
│  │                                              │    │
│  │  Title                          [background] │    │
│  │  Description                                 │    │
│  │  Genre · Platform · Rating                   │    │
│  │                                              │    │
│  │  [▶ Play]  [+ Library]  [♡ Favorite]        │    │
│  └──────────────────────────────────────────────┘    │
│                                                      │
│  Continue Playing                     See All →      │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐      │
│  │      │ │      │ │      │ │      │ │      │  →   │
│  │ Game │ │ Game │ │ Game │ │ Game │ │ Game │      │
│  └──────┘ └──────┘ └──────┘ └──────┘ └──────┘      │
│                                                      │
│  Recently Played                    See All →       │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐      │
│  │ Game │ │ Game │ │ Game │ │ Game │ │ Game │  →   │
│  └──────┘ └──────┘ └──────┘ └──────┘ └──────┘      │
│                                                      │
│  Recommended For You                See All →       │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐      │
│  │ Game │ │ Game │ │ Game │ │ Game │ │ Game │  →   │
│  └──────┘ └──────┘ └──────┘ └──────┘ └──────┘      │
│                                                      │
│  Popular Games                      See All →       │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐      │
│  │ Game │ │ Game │ │ Game │ │ Game │ │ Game │  →   │
│  └──────┘ └──────┘ └──────┘ └──────┘ └──────┘      │
│                                                      │
└──────────────────────────────────────────────────────┘
```

**Hero Spec:**
- Height: 480px (desktop), 320px (tablet)
- Full-width bg image with cinematic gradient overlay
- Overlay: `linear-gradient(to right, rgba(9,11,13,0.95) 0%, rgba(9,11,13,0.4) 60%, transparent 100%)`
- Content: left-aligned, 48px padding

**Row Spec:**
- Horizontal scroll with snap
- Card width: 200px (desktop), 160px (tablet)
- Gap: 16px
- Label: bold, section title
- "See All" link on right

### 4.2 Game Library

```
┌──────────────────────────────────────────────────────┐
│  Library                         [Grid ▼] [⚙ Sort]  │
│  ────────────────────────────────────────────────── │
│  [All] [Installed] [Favorites] [Cloud] [Recently]   │
│                                                      │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐      │
│  │      │ │      │ │      │ │      │ │      │      │
│  │      │ │      │ │      │ │      │ │      │      │
│  │      │ │      │ │      │ │      │ │      │      │
│  │  ▶   │ │  ▶   │ │  ▶   │ │  ▶   │ │  ▶   │      │
│  │      │ │      │ │      │ │      │ │      │      │
│  │ Title│ │ Title│ │ Title│ │ Title│ │ Title│      │
│  │ 2h   │ │ 5h   │ │ 12h  │ │ 0.5h │ │ 8h   │      │
│  └──────┘ └──────┘ └──────┘ └──────┘ └──────┘      │
│                                                      │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐      │
│  │ Game │ │ Game │ │ Game │ │ Game │ │ Game │      │
│  └──────┘ └──────┘ └──────┘ └──────┘ └──────┘      │
└──────────────────────────────────────────────────────┘
```

**Grid:** 5 cols (desktop), 4 (laptop), 3 (tablet), 2 (mobile)
**Card:** 3:4 aspect ratio, 16px gap
**Sort:** Name, Last Played, Play Time

**Card States:**
- Default — cover, title, play time
- Hover — scale(1.03), glow border, play button overlay
- Focus — accent border, glow shadow
- Loading — skeleton shimmer
- Unavailable — desaturated, "Maintenance" badge
- In Queue — animated pulse border

### 4.3 Search

```
┌──────────────────────────────────────────────────────┐
│  🔍 Search games, genres, developers...              │
│                                                      │
│  Filters: [Genre ▼] [Platform ▼] [Rating ▼] [Sort ▼]│
│                                                      │
│  Results (12 games)                                  │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐      │
│  │ Game │ │ Game │ │ Game │ │ Game │ │ Game │      │
│  └──────┘ └──────┘ └──────┘ └──────┘ └──────┘      │
└──────────────────────────────────────────────────────┘
```

Instant results (debounced 200ms). Recent searches when empty.
Filter chips: multi-select, removable.

### 4.4 Game Detail

```
┌──────────────────────────────────────────────────────┐
│  ← Back                                             │
│                                                      │
│  ┌──────────────────────────────────────────────┐    │
│  │  HERO BANNER (400px)                         │    │
│  │  Background: game screenshot + blur + overlay│    │
│  └──────────────────────────────────────────────┘    │
│                                                      │
│  ┌──────────┐  Game Title                           │
│  │          │  Developer · Publisher                 │
│  │  Cover   │  ★ 4.5 · Action RPG                   │
│  │  200×280 │  Cloud Available                      │
│  │          │                                       │
│  │          │  [▶ Play]  [+ Library]  [♡ Favorite] │
│  └──────────┘                                       │
│                                                      │
│  About This Game                                     │
│  Lorem ipsum dolor sit amet, consectetur...          │
│                                                      │
│  Screenshots                                         │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐               │
│  │      │ │      │ │      │ │      │               │
│  └──────┘ └──────┘ └──────┘ └──────┘               │
│                                                      │
│  System Requirements                                 │
│  ┌─────────────────┐  ┌─────────────────┐           │
│  │  Minimum        │  │  Recommended    │           │
│  │  OS: Windows 10 │  │  OS: Windows 11 │           │
│  │  RAM: 8 GB      │  │  RAM: 16 GB     │           │
│  │  GPU: GTX 1060  │  │  GPU: RTX 3060  │           │
│  └─────────────────┘  └─────────────────┘           │
│                                                      │
│  Related Games                                       │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐               │
│  │ Game │ │ Game │ │ Game │ │ Game │               │
│  └──────┘ └──────┘ └──────┘ └──────┘               │
└──────────────────────────────────────────────────────┘
```

### 4.5 Settings

```
┌──────────────────────────────────────────────────────┐
│  Settings                                            │
│                                                      │
│  ┌──────────┐  ┌──────────────────────────────────┐  │
│  │ Account  │  │  Account Settings                │  │
│  │ Streaming│  │                                  │  │
│  │ Input    │  │  Display Name: [__________]     │  │
│  │Interface │  │  Email: user@email.com           │  │
│  │ Advanced │  │  Avatar: [Upload]                │  │
│  │ Diagnos. │  │                                  │  │
│  │ About    │  │  ──────────────────────────────  │  │
│  │          │  │                                  │  │
│  │          │  │  Linked Accounts                 │  │
│  │          │  │  NVIDIA: Connected ✓             │  │
│  │          │  │  Google: Connect                 │  │
│  └──────────┘  └──────────────────────────────────┘  │
└──────────────────────────────────────────────────────┘
```

**Sections:** Account, Streaming, Input, Interface, Advanced, Diagnostics, About

---

## 5. Component System

### 5.1 Component Tree

```
src/components/
├── layout/
│   ├── AppShell.tsx            # Sidebar + Header + Content
│   ├── Sidebar.tsx             # Navigation sidebar
│   ├── Header.tsx              # Top header bar
│   ├── ContentArea.tsx         # Main content wrapper
│   └── FullScreen.tsx          # Streaming overlay
│
├── navigation/
│   ├── NavItem.tsx             # Sidebar nav item
│   ├── NavGroup.tsx            # Grouped items
│   ├── SearchBar.tsx           # Global search + dropdown
│   ├── NotificationBell.tsx    # Notification trigger
│   └── UserMenu.tsx            # Profile dropdown
│
├── game/
│   ├── GameCard.tsx            # Library/store card
│   ├── GameCardSmall.tsx       # Row/carousel card
│   ├── GameHero.tsx            # Featured hero
│   ├── GameGrid.tsx            # Grid container
│   ├── GameRow.tsx             # Horizontal scroll row
│   ├── GameDetail.tsx          # Detail page
│   ├── GameScreenshots.tsx     # Screenshot gallery
│   └── GameStatusBadge.tsx     # Status badge
│
├── streaming/
│   ├── VideoPlayer.tsx         # WebRTC video
│   ├── StreamOverlay.tsx       # Controls overlay
│   ├── PerformanceHUD.tsx      # Stats pill
│   ├── QueueView.tsx           # Queue screen
│   ├── ConnectionScreen.tsx    # Connecting state
│   └── SessionControls.tsx     # Mic, ctrl, exit
│
├── ui/
│   ├── Button.tsx              # Primary/secondary/ghost/icon
│   ├── Card.tsx                # Surface card
│   ├── Modal.tsx               # Dialog overlay
│   ├── Toggle.tsx              # Switch toggle
│   ├── Slider.tsx              # Range slider
│   ├── Dropdown.tsx            # Select dropdown
│   ├── Input.tsx               # Text input
│   ├── Badge.tsx               # Status badge
│   ├── Tooltip.tsx             # Hover tooltip
│   ├── Skeleton.tsx            # Loading skeleton
│   ├── Spinner.tsx             # Loading spinner
│   ├── Avatar.tsx              # User avatar
│   ├── Progress.tsx            # Progress bar/circle
│   └── Toast.tsx               # Notification toast
│
└── shared/
    ├── Carousel.tsx             # Horizontal scroll
    ├── Tabs.tsx                 # Tab navigation
    ├── FilterChips.tsx          # Filter chips
    ├── EmptyState.tsx           # Empty state
    └── PageTransition.tsx       # Route transition
```

### 5.2 Button System

| Variant | Use | Style |
|---|---|---|
| **Primary** | Play, Confirm | Accent bg, dark text, glow hover |
| **Secondary** | Add to Library | Border, accent text |
| **Ghost** | Cancel, Back | Transparent, text only |
| **Icon** | Settings, Close | Square, icon only |
| **Danger** | Sign Out, Delete | Red accent, subtle bg |

States: Default → Hover (brighten) → Pressed (scale 0.97) → Disabled (opacity 0.4)
Focus: Accent border + glow shadow
Loading: Spinner replaces label

### 5.3 Card (GameCard)

```
┌─────────────┐
│             │  ← Cover image (3:4)
│             │
│      ▶      │  ← Play button (hover only)
│             │
├─────────────┤
│ Game Title  │  ← 2 lines max
│ 12h played  │  ← muted
│     ♡       │  ← Favorite (hover)
└─────────────┘
```

Hover CSS:
```css
transform: scale(1.03);
box-shadow: 0 8px 32px rgba(106, 240, 160, 0.15);
border: 1px solid rgba(106, 240, 160, 0.3);
transition: all 260ms cubic-bezier(0.2, 0, 0, 1);
```

### 5.4 Toggle, Modal, Skeleton

**Toggle:** Gray track → Accent track on. Knob translates right. 260ms.

**Modal:** Backdrop `black@60%` + `blur(8px)`. Content `#11161A`, 16px radius. Enter: fade+scale(0.95→1).

**Skeleton:** Base `#0D1216`. Shimmer `#1B2228` left→right, 1.5s infinite. Respects `prefers-reduced-motion`.

---

## 6. Design Tokens

### 6.1 Colors

**Surfaces:**
| Token | Value | Usage |
|---|---|---|
| `--np-bg` | `#090B0D` | App background |
| `--np-surface` | `#11161A` | Card/panel |
| `--np-surface-alt` | `#171D22` | Alternate surface |
| `--np-surface-hover` | `#1E252C` | Hover state |
| `--np-border` | `rgba(255,255,255,0.08)` | Default border |
| `--np-border-accent` | `rgba(106,240,160,0.3)` | Accent border |

**Text:**
| Token | Value | Usage |
|---|---|---|
| `--np-text` | `#EEF3F5` | Primary |
| `--np-text-muted` | `#98A4AA` | Secondary |
| `--np-text-dim` | `#5A6A72` | Disabled |
| `--np-text-on-accent` | `#08090C` | On accent bg |

**Accent (user-swappable):**
| Token | Value | Usage |
|---|---|---|
| `--np-accent` | `#6AF0A0` | Primary action |
| `--np-accent-hover` | `#7DF5AE` | Hover |
| `--np-accent-muted` | `rgba(106,240,160,0.15)` | Subtle bg |
| `--np-accent-glow` | `rgba(106,240,160,0.25)` | Glow shadow |

**Accent palette:** green `#6AF0A0` · blue `#8AB4F8` · pink `#FF4FB8` · lime `#C7EF6B` · coral `#FF8D7A` · violet `#C7A4FF`

**Status:**
| Token | Value |
|---|---|
| `--np-status-good` | `#6AF0A0` |
| `--np-status-fair` | `#FFC95A` |
| `--np-status-poor` | `#FF8D7A` |
| `--np-error-bg` | `#33181C` |
| `--np-error-text` | `#FFB8BF` |

**Video Overlay:**
| Token | Value |
|---|---|
| `--np-stream-scrim` | `rgba(0,0,0,0.55)` |
| `--np-panel-over-video` | `rgba(17,22,26,0.96)` |
| `--np-row-rest` | `#1B2228` |
| `--np-row-focused` | `#28323A` |
| `--np-hairline` | `rgba(255,255,255,0.08)` |

### 6.2 Typography

**Font:** Inter Variable (Google Fonts, weights 400–800)
**Numeric:** `font-feature-settings: "tnum, zero"` for FPS/bitrate/latency

| Token | Size | Weight | Line Height | Usage |
|---|---|---|---|---|
| `--np-text-display` | 44px | 800 | 50px | Hero titles |
| `--np-text-h1` | 32px | 700 | 40px | Page titles |
| `--np-text-h2` | 24px | 700 | 30px | Section titles |
| `--np-text-h3` | 20px | 600 | 24px | Subsection |
| `--np-text-body-lg` | 16px | 400 | 24px | Body large |
| `--np-text-body` | 14px | 400 | 20px | Body default |
| `--np-text-body-sm` | 12px | 400 | 17px | Captions |
| `--np-text-label` | 13px | 600 | 16px | Labels |
| `--np-text-mono` | 14px | 400 | 20px | Numbers (tnum) |

### 6.3 Spacing

| Token | Value |
|---|---|
| `--np-space-2xs` | 2px |
| `--np-space-xs` | 4px |
| `--np-space-sm` | 8px |
| `--np-space-md` | 12px |
| `--np-space-lg` | 16px |
| `--np-space-xl` | 24px |
| `--np-space-2xl` | 32px |
| `--np-space-3xl` | 48px |
| `--np-space-4xl` | 64px |
| `--np-page-padding` | 32px |
| `--np-grid-gap` | 16px |

### 6.4 Radius

| Token | Value |
|---|---|
| `--np-radius-xs` | 4px |
| `--np-radius-sm` | 8px |
| `--np-radius-md` | 12px |
| `--np-radius-lg` | 16px |
| `--np-radius-xl` | 24px |
| `--np-radius-full` | 9999px |

### 6.5 Shadows

| Token | Value |
|---|---|
| `--np-shadow-sm` | `0 2px 8px rgba(0,0,0,0.3)` |
| `--np-shadow-md` | `0 4px 16px rgba(0,0,0,0.4)` |
| `--np-shadow-lg` | `0 8px 32px rgba(0,0,0,0.5)` |
| `--np-shadow-glow` | `0 0 24px var(--np-accent-glow)` |

### 6.6 Z-Index

| Layer | Value |
|---|---|
| Base | 0 |
| Dropdown | 100 |
| Sticky | 200 |
| Overlay | 300 |
| Modal | 400 |
| Stream | 500 |
| Toast | 600 |

### 6.7 Breakpoints

| Name | Width | Layout |
|---|---|---|
| Mobile | < 640px | Single column, bottom nav |
| Tablet | 640–1024px | Collapsed sidebar, 3-col |
| Laptop | 1024–1440px | Expanded sidebar, 4-col |
| Desktop | > 1440px | Full layout, 5-col |

---

## 7. Animation Rules

### 7.1 Durations

| Token | Value | Usage |
|---|---|---|
| `--np-duration-fast` | 120ms | Press, toggle, ripple |
| `--np-duration-normal` | 260ms | Hover, focus, tabs |
| `--np-duration-slow` | 420ms | Page transition, modal |
| `--np-duration-slower` | 600ms | Hero entrance |

### 7.2 Easings

| Token | Value | Usage |
|---|---|---|
| `--np-ease-default` | `cubic-bezier(0.2, 0, 0, 1)` | General |
| `--np-ease-decel` | `cubic-bezier(0.05, 0.7, 0.1, 1)` | Entering |
| `--np-ease-accel` | `cubic-bezier(0.3, 0, 0.8, 0.15)` | Exiting |

### 7.3 Animation Catalog

| Animation | Trigger | Duration | Properties |
|---|---|---|---|
| Card hover | Mouse enter | 260ms | scale(1.03), box-shadow |
| Card press | Mouse down | 120ms | scale(0.97) |
| Button hover | Mouse enter | 260ms | bg-color, box-shadow |
| Button press | Mouse down | 120ms | scale(0.97) |
| Toggle | Click | 260ms | translateX, bg-color |
| Page enter | Route change | 420ms | opacity 0→1, translateY 12→0 |
| Page exit | Route change | 260ms | opacity 1→0 |
| Modal enter | Open | 420ms | opacity 0→1, scale 0.95→1 |
| Modal exit | Close | 260ms | opacity 1→0 |
| Carousel scroll | Scroll | 420ms | scroll-behavior: smooth |
| Skeleton shimmer | Loading | 1500ms | bg-position left→right |
| Stagger grid | Page load | 420ms | Sequential fade+slide per card |

**Rule:** All infinite animations (shimmer, pulse, carousel) respect `prefers-reduced-motion: reduce` and stop entirely.

---

## 8. Responsive Rules

### 8.1 Desktop (> 1440px)

- Full sidebar (240px, expanded)
- 5-column game grid
- Hero: 480px height
- Row cards: 200px wide
- Full header with search
- 32px content padding

### 8.2 Laptop (1024–1440px)

- Expanded sidebar (240px)
- 4-column game grid
- Hero: 440px height
- Row cards: 180px wide
- Header with search
- 24px content padding

### 8.3 Tablet (640–1024px)

- Collapsed sidebar (72px, icon only)
- 3-column game grid
- Hero: 320px height
- Row cards: 160px wide
- Compact header
- 20px content padding

### 8.4 Mobile (< 640px)

- Sidebar hidden (drawer, hamburger trigger)
- 2-column game grid
- Hero: 240px height
- Row cards: 140px wide
- Compact header
- 16px content padding
- Bottom navigation bar (5 items: Home, Library, Search, Queue, Profile)
- Touch-friendly: min 44px tap targets

---

## 9. Cloud Gaming UX

### 9.1 Session Flow

```
Game Selected → Preparing → Queue → Connecting → Streaming → End
                  ↓           ↓         ↓            ↓
              Cancel      Cancel   Timeout      Graceful
```

### 9.2 Queue Screen

```
┌──────────────────────────────────────────────────────┐
│                                                      │
│              ⏳ Connecting to Server...               │
│                                                      │
│           Position: #4                                │
│           Estimated Wait: ~3 minutes                 │
│           Region: Southeast Asia                     │
│                                                      │
│           ┌────────────────────────┐                  │
│           │  ████████░░░░░░░░░░░░  │                  │
│           │     40% estimated      │                  │
│           └────────────────────────┘                  │
│                                                      │
│           [Cancel]                                   │
│                                                      │
└──────────────────────────────────────────────────────┘
```

### 9.3 Streaming UI

```
┌──────────────────────────────────────────────────────┐
│  ┌──────────────────────────────────────────────┐    │
│  │  ┌─────┐ FPS: 60  Res: 1080p  Ping: 24ms   │    │
│  │  │ 🟢  │ Bitrate: 35 Mbps   Codec: H.265    │    │
│  │  └─────┘                                    │    │
│  │                                              │    │
│  │                                              │    │
│  │          FULL SCREEN VIDEO PLAYER            │    │
│  │          (WebRTC stream)                     │    │
│  │                                              │    │
│  │                                              │    │
│  │                                              │    │
│  │  ┌──────────────────────────────────────┐    │    │
│  │  │  🎤  🎮  ⚙️  📊  ⛶   Session: 2h  │    │    │
│  │  └──────────────────────────────────────┘    │    │
│  └──────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────┘
```

**Top bar:** Performance stats pill (auto-hide after 5s)
- Green dot = connected, yellow = degraded, red = disconnected
- FPS, Resolution, Latency, Bitrate, Codec, Connection status

**Bottom bar:** Session controls (auto-hide after 5s)
- Microphone toggle
- Controller settings
- Performance overlay toggle
- Session info
- Screenshot
- Exit session

**Auto-hide:** Controls fade after 5s of inactivity. Mouse move or keypress reveals.

### 9.4 Connection States

| State | UI |
|---|---|
| **Preparing** | Spinner + "Setting up your stream..." |
| **Connecting** | Spinner + "Connecting to server..." |
| **Connected** | Video player + overlay controls |
| **Reconnecting** | Overlay: "Reconnecting..." + spinner + retry countdown |
| **Disconnected** | Overlay: "Connection lost" + [Retry] [Exit] |
| **Error** | Overlay: Error message + [Retry] [Exit] [Diagnostics] |

### 9.5 Diagnostics Panel

Side panel (toggle with 📊 button):

```
┌─────────────────────┐
│  Stream Diagnostics  │
│  ────────────────── │
│  Codec: H.265       │
│  Resolution: 1920x  │
│  FPS: 60/60         │
│  Bitrate: 35 Mbps   │
│  Latency: 24ms      │
│  Packet Loss: 0.1%  │
│  Jitter: 2.3ms      │
│  Server: SEA-1      │
│  Session: 1:23:45   │
│  ────────────────── │
│  [Run Speed Test]   │
│  [Export Logs]      │
│  [Report Issue]     │
└─────────────────────┘
```

---

## 10. Implementation Priority

### Phase 1: Foundation (Week 1–2)

**Priority: Critical**

- [ ] Design tokens (CSS custom properties)
- [ ] Typography system (Inter font, scale)
- [ ] AppShell layout (sidebar + header + content)
- [ ] Sidebar navigation (expanded/collapsed)
- [ ] Header bar (search, profile)
- [ ] Button component (all variants)
- [ ] Card component (GameCard)
- [ ] Basic routing (React Router)
- [ ] Dark theme foundation

### Phase 2: Core Pages (Week 3–4)

**Priority: High**

- [ ] Home Dashboard (hero + rows)
- [ ] Game Library (grid + filters)
- [ ] Game Detail page
- [ ] Search page
- [ ] Settings page (skeleton)
- [ ] Carousel/row component
- [ ] Skeleton loading states
- [ ] Page transitions

### Phase 3: Streaming (Week 5–6)

**Priority: High**

- [ ] WebRTC video player
- [ ] Stream overlay controls
- [ ] Performance HUD
- [ ] Queue view
- [ ] Connection states
- [ ] Session controls
- [ ] Fullscreen mode
- [ ] Keyboard shortcuts during stream

### Phase 4: Polish (Week 7–8)

**Priority: Medium**

- [ ] Settings pages (all sections)
- [ ] Toggle, slider, dropdown components
- [ ] Modal system
- [ ] Toast notifications
- [ ] Filter chips
- [ ] Empty states
- [ ] Error states
- [ ] Search instant results

### Phase 5: Advanced (Week 9–10)

**Priority: Low**

- [ ] Responsive optimization (tablet/mobile)
- [ ] Bottom navigation (mobile)
- [ ] Diagnostics panel
- [ ] Keyboard navigation improvements
- [ ] Screen reader support
- [ ] Reduced motion support
- [ ] Performance profiling
- [ ] PWA manifest

### Tech Stack Recommendation

| Layer | Choice | Reason |
|---|---|---|
| **Framework** | Next.js 14+ | SSR, routing, performance |
| **Styling** | Tailwind CSS + CSS variables | Tokens, utility, maintainability |
| **Components** | Radix UI primitives | Accessible, unstyled, composable |
| **Animation** | Framer Motion | Page transitions, layout animations |
| **State** | Zustand | Lightweight, simple |
| **Icons** | Lucide React | Consistent, tree-shakeable |
| **Font** | Inter (Google Fonts) | Variable font, tabular figures |

---

*Last updated: August 2026 — NexPlay Web UI Design Blueprint v1.0*
