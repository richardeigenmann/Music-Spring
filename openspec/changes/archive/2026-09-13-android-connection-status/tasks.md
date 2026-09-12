# Tasks: Android Connection Status and Local Mode

## 1. Platform Support

- [x] 1.1 Add `openVpnSettings` to `Platform.kt` interface and implement it in `androidMain` using `android.provider.Settings.ACTION_VPN_SETTINGS` with fallback handling. Verify the method signature and Android intent handling.
- [x] 1.2 Wire the platform actions into Koin dependency injection in `AndroidKoin.kt`. Verify successful compilation.

## 2. Connection State Management

- [x] 2.1 Extend `SettingsRepository` to persist the manual offline/local mode preference. Verify preference read/write functions.
- [x] 2.2 Implement `ConnectionManager` with reachability checks against `/api/version` and `ConnectionState` tracking (`CONNECTED`, `LOCAL_MODE`, `CONNECTING`). Verify state transitions on connect and disconnect.
- [x] 2.3 Register `ConnectionManager` in Koin configuration. Verify resolution in the DI container.

## 3. Top App Bar & UI Actions

- [x] 3.1 Add the connection status icon (`CloudDone` for connected, `CloudOff` for local mode) to the `TopAppBar` actions in `App.kt`. Verify correct icon rendering per state.
- [x] 3.2 Implement the action `DropdownMenu` offering "Connect now", "Open VPN Panel", and "Open Connection Settings" when disconnected/local, and "Disconnect now" when connected. Verify menu triggering and action dispatching.

## 4. Local Mode Enforcement & Download Disabling

- [x] 4.1 Update `TrackViewModel` and sync operations to respect local mode by avoiding remote calls when disconnected/local and serving tags directly from the local database. Verify offline tag retrieval.
- [x] 4.2 Disable "Download All" in `App.kt` drawer and `SyncScreen.kt` when in local mode. Verify UI reflects disabled state.
- [x] 4.3 Disable "Cache All" and individual track download buttons in `TrackListScreen.kt` when in local mode. Verify buttons are disabled when disconnected.
- [x] 4.4 Compile the Android application via `./gradlew :musicandroid:app:compileDebugKotlin` and `./gradlew :musicandroid:app:assembleDebug` to verify there are no compilation errors or broken dependencies.
