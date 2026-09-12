# Proposal: Android App Connection Status and Local Mode

## Why
The Android app currently provides no visual indicator to inform the user whether it is successfully connected to the backend server or operating in local mode. Because the backend is often hosted on a private home network accessible only via VPN, users need clear visibility into their connection state and quick access to actions such as connecting, opening VPN settings, configuring connection settings, or manually disconnecting to stay in local mode.

## What Changes
- Add a connection status indicator icon in the main screen `TopAppBar` (in the Material-standard actions slot) reflecting whether the app is connected to the backend or operating in local mode.
- Display intuitive icons: `CloudDone` for Connected and `CloudOff` for Disconnected/Local mode.
- Tapping the icon displays an action menu:
  - When disconnected / local mode:
    1. **Connect now**: Triggers immediate backend reachability check and synchronizes tags if reachable.
    2. **Open VPN Panel**: Launches Android's system VPN settings (`Settings.ACTION_VPN_SETTINGS`).
    3. **Open Connection Settings**: Navigates to the connection/backend settings screen (`SettingsScreen`).
  - When connected:
    1. **Disconnect now**: Puts the app into manual local mode, suppressing network calls.
- In Local Mode, automatic background sync calls to the backend are suppressed, and the app prioritizes and relies strictly on local storage for tags and playback.
- Disable downloading capabilities when in Local Mode: "Download All" and playlist / tag-list / track caching are disabled when the app is in local mode.

## Capabilities

### New Capabilities
- `android-connection-status`: Defines the UI indicator, connection state management (Connected vs Local Mode), reachability detection, action menu (Connect now, Disconnect now, Open VPN Panel, Open Connection Settings), and disabling download actions while in local mode.

### Modified Capabilities

## Impact
- `musicandroid/src/commonMain/kotlin/org/richinet/musicandroid/App.kt`: TopAppBar update to include status icon and dropdown action menu; disable "Download All" in drawer when in local mode.
- `musicandroid/src/commonMain/kotlin/org/richinet/musicandroid/TrackListScreen.kt`: Disable caching buttons ("Cache All" and individual track download) in local mode.
- `musicandroid/src/commonMain/kotlin/org/richinet/musicandroid/SyncScreen.kt`: Disable "Start Download All" when in local mode.
- `musicandroid/src/commonMain/kotlin/org/richinet/musicandroid/Platform.kt` & `androidMain/.../Platform.kt`: Add platform helper to open Android VPN settings (`Settings.ACTION_VPN_SETTINGS`).
- Connection state management & ViewModel: Manage connection mode state (Connected vs Local Mode) and respect local mode across sync and tag loading.
