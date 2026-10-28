# CampusConnect-Android

Jetpack Compose Android app for the CampusConnect student super-platform.

This repository contains the Android app and the CampusConnect API in `backend/`.

## Run the Backend Locally

From a terminal at the repository root:

```powershell
Copy-Item backend/.env.example backend/.env
Set-Location backend
npm ci
docker compose up --build
```

The API is available at `http://localhost:3000/api/v1` and Swagger at `http://localhost:3000/api/docs`. The Android emulator is configured to reach the host API through `10.0.2.2`.

## Tech Stack

| Layer | Choice |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Adaptive layout | `WindowSizeClass` — phone / tablet / desktop |
| Navigation | `navigation-compose` |
| State | `ViewModel` + `StateFlow` + `collectAsStateWithLifecycle` |
| Images | Coil 2 async image loading |
| Async | Kotlin Coroutines |
| Theme | Dark-mode-first · Purple/Black palette |
| Min SDK | 26 (Android 8.0) |

---

## Design System

### Colour Palette

| Token | Hex | Use |
|---|---|---|
| `DeepRoyalPurple` | `#7C3AED` | Primary actions, CTAs |
| `ElectricPurple` | `#A855F7` | Secondary, selected states |
| `NeonViolet` | `#C084FC` | Accents, highlights |
| `RichBlack` | `#0A0A0A` | Background |
| `DarkCharcoal` | `#121212` | Surface / bottom nav |
| `SurfaceDark` | `#1A1A2E` | Elevated cards |
| `TextPrimary` | `#FFFFFF` | Body text |
| `TextSecondary` | `#B3B3B3` | Captions, metadata |

### Adaptive Navigation

| Screen width | Navigation chrome |
|---|---|
| Compact (phones) | `NavigationBar` — pinned at bottom |
| Medium (tablets) | `NavigationRail` — left sidebar |
| Expanded (desktop) | `PermanentNavigationDrawer` — always-open drawer |

---

## Project Structure

```
app/src/main/java/com/campusconnect/app/
├── CampusConnectApp.kt              # Application class
├── MainActivity.kt                  # Entry point, WindowSizeClass
├── data/
│   └── model/
│       └── Models.kt                # Post, User, Story, FeedState, ReactionType
└── ui/
    ├── theme/
    │   ├── Color.kt                 # Full brand palette
    │   ├── Type.kt                  # Typography scale
    │   ├── Shape.kt                 # Shape tokens
    │   └── Theme.kt                 # MaterialTheme wrapper
    ├── components/
    │   ├── GlassmorphicCard.kt      # Glassmorphism card + GlassPill
    │   ├── AnimatedGradient.kt      # Animated purple radial glow
    │   ├── SkeletonLoader.kt        # Shimmer skeletons (post + stories)
    │   ├── StoryRow.kt              # Stories with gradient rings
    │   └── PostCard.kt              # Full post card with media grid,
    │                                  reactions, action bar, reaction picker
    ├── screens/
    │   └── feed/
    │       ├── FeedViewModel.kt     # StateFlow state, pagination,
    │       │                          optimistic reactions + bookmarks
    │       └── FeedScreen.kt        # LazyColumn feed, TopAppBar,
    │                                  pull-to-refresh, FAB, error state
    └── navigation/
        ├── NavigationItem.kt        # Sealed nav destinations
        ├── AdaptiveNavigation.kt    # Compact/Medium/Expanded switcher
        └── CampusConnectNavGraph.kt # NavHost with all routes
```

---

## Screens Built — Phase 1

### Social Feed (`FeedScreen`)
- Pinned `TopAppBar` with logo + search + notification badge
- Stories row — gradient rings, online dots, "Add Story" tile
- `LazyColumn` with infinite scroll (loads next page at T-3 items)
- Pull-to-refresh with purple indicator
- Skeleton shimmer loading state
- Error state with retry button
- `ExtendedFloatingActionButton` — create post

### PostCard component
- Glassmorphic dark card surface with purple border glow
- Author avatar (image or initials fallback), verified badge, timestamp
- Community/group pill tag
- Expandable text (truncates at 4 lines, "Read more" toggle)
- Media grid (1-up full-width · 2-up side-by-side · 3+ with overflow badge)
- Reaction summary bar (top emoji + counts)
- Action bar: **React** (long press = reaction picker) · Comment · Share · Bookmark
- Animated reaction picker popup with spring-scale hover
- Spring-animated button press scale
- Optimistic reaction toggle (immediate UI, no network round-trip)

---

## Running Locally

1. Open the project in **Android Studio Hedgehog** or later
2. Select a device — emulator (Pixel 7 API 34) or physical device
3. Click **Run ▶**

The feed, post creation, comments, authentication, events, and accommodation screens use the backend API. Debug builds default to the local emulator backend (`10.0.2.2:3000`). The API URL can be overridden with `CAMPUSCONNECT_API_BASE_URL` or `-PcampusConnectApiBaseUrl`.

Release builds require `CAMPUSCONNECT_API_BASE_URL` to be set to an HTTPS URL ending in `/api/v1/`. For example, in PowerShell:

```powershell
$env:CAMPUSCONNECT_API_BASE_URL = "https://api.your-domain.example/api/v1/"
.\gradlew.bat :app:bundleRelease
```

The WebSocket URL is derived from the same API host and uses `wss://` automatically. Cleartext HTTP access is limited to local hosts in debug builds; release builds reject HTTP API URLs.

---

## Implemented Workflow

- Register or log in, publish a text post, see it in the feed, open its detail, and add comments.
- Stories are not backed by the API and are intentionally empty.
- Bookmarks are currently local-only and do not persist between app launches.

## Remaining Screens

- [ ] User profile detail
- [ ] Event detail
- [ ] Edit profile
- [ ] Media attachments in the post composer

---

CampusConnect © 2025 — Private & Confidential

## CampusConnect Web

A responsive website companion is in [`web/`](web/README.md). To preview it locally, run `py -m http.server 8080` from that directory and open `http://localhost:8080`. It runs as a static site with no build step; connect the API by setting its `/api/v1` base URL in `web/app.js` and allowing the site origin in the backend `CORS_ORIGINS` setting. See the web README for deployment and current feature coverage.
