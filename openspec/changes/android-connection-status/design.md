# Design: Android Connection Status and Local Mode

## Context
The Music-Spring Android application communicates with a backend that frequently lives on a private network accessible via a VPN tunnel. Previously, the app did not present its connection status on the main screen, and attempts to refresh or sync without VPN would hang or trigger timeout exceptions. Furthermore, features such as "Download All" or caching tag lists / tracks would attempt network requests that fail when offline.

See `proposal.md` for motivation and `specs/android-connection-status/spec.md` for functional requirements.

## Goals / Non-Goals

**Goals:**
- Provide a clear, real-time indicator in the `TopAppBar` actions area showing whether the app is connected to the backend or operating in local mode.
- Provide quick actions on tap: "Connect now", "Open VPN Panel", "Open Connection Settings" (when disconnected/local) and "Disconnect now" (when connected).
- Implement proactive backend reachability checking via `/api/version` rather than relying solely on raw OS internet capability.
- In Local Mode, suppress network requests and seamlessly serve content from the local database and local device storage.
- Explicitly disable download actions ("Download All", tag list "Cache All", and individual track caching) while in Local Mode to prevent invalid network tasks.

**Non-Goals:**
- Managing the VPN tunnel lifecycle automatically from within the app (opening the Android system VPN panel delegates management to the user and their VPN app).

## Decisions

### 1. Connection Management Architecture
- **Decision**: Introduce a `ConnectionManager` component managed by Koin that holds the unified connection state (`CONNECTED`, `LOCAL_MODE`, `CONNECTING`).
- **Rationale**: Keeps connection determination, reachability checks, and state exposure centralized.
- **Reachability check**: Uses `ApiService.getVersion()` with a lightweight 2.5-second timeout to confirm the backend is responding over the active network/VPN.

### 2. Manual Local Mode / Disconnect Persistence
- **Decision**: Persist the user's manual disconnect preference in `SettingsRepository` (DataStore) so that if the user explicitly chooses "Disconnect now", the app stays in local mode across screen navigations until "Connect now" is chosen.
- **Rationale**: Prevents unexpected background network stalls when the user purposefully wants to work offline.

### 3. Material Design UI Placement
- **Decision**: Place the status icon in the `actions` slot of the Material 3 `TopAppBar` on the main screen, using `CloudDone` for Connected and `CloudOff` for Local Mode.
- **Rationale**: Complies with Material 3 app bar guidelines for interactive status actions and provides consistent anchoring for the dropdown menu.

### 4. Disabling Downloads in Local Mode
- **Decision**: Observe `ConnectionManager.connectionState` in `App.kt` (navigation drawer), `SyncScreen.kt`, and `TrackListScreen.kt`. When in `LOCAL_MODE`:
  - "Download All" drawer item is shown disabled / non-clickable with visual indication or warning.
  - `SyncScreen.kt` disables "Start Download All" button with an informative notice ("Disabled in Local Mode").
  - `TrackListScreen.kt` disables the "Cache All" action and track item download buttons when in `LOCAL_MODE`.
- **Rationale**: Prevents users from enqueueing doomed download requests to a local download manager/worker when the backend cannot be reached.

### 5. Platform-Specific VPN Launch
- **Decision**: Define a platform abstraction `PlatformActions` (or `VpnLauncher`) in common code and implement it in `androidMain` using `android.provider.Settings.ACTION_VPN_SETTINGS` with `FLAG_ACTIVITY_NEW_TASK`.
- **Rationale**: Keeps common Compose Multiplatform UI decoupled from Android-specific `Intent` APIs.

## Risks / Trade-offs

- **[Risk]**: Devices or custom ROMs where `Settings.ACTION_VPN_SETTINGS` is not handled.
  - **Mitigation**: Wrap the `startActivity` call in a `try-catch (e: ActivityNotFoundException)` and fall back to general wireless or system settings (`Settings.ACTION_SETTINGS`).
- **[Risk]**: Frequent health checks causing battery or network drain.
  - **Mitigation**: Check reachability on demand (app startup, network change callback, and user-initiated "Connect now"), rather than tight continuous polling.
