## Purpose

Provides visual indicators for backend connectivity and local mode in the Android application, with direct access to connection management actions including VPN settings, while preventing network-dependent operations like downloads during local mode.

## ADDED Requirements

### Requirement: Connection status indicator in TopAppBar
The system SHALL display an indicator icon in the main screen `TopAppBar` actions area representing the current connection state.

#### Scenario: Display connected status
- **WHEN** the app successfully communicates with the backend
- **THEN** the system SHALL display the connected icon (`CloudDone`).

#### Scenario: Display local mode status
- **WHEN** the app cannot reach the backend or is manually disconnected
- **THEN** the system SHALL display the local mode icon (`CloudOff`).

### Requirement: Connection actions menu
The system SHALL display an action menu when the connection status icon is tapped.

#### Scenario: Disconnected menu options
- **WHEN** the status icon is tapped while disconnected or in local mode
- **THEN** the system SHALL present the options: "Connect now", "Open VPN Panel", and "Open Connection Settings".

#### Scenario: Connected menu options
- **WHEN** the status icon is tapped while connected to the backend
- **THEN** the system SHALL present the option: "Disconnect now".

### Requirement: Manual connect and disconnect
The system SHALL allow the user to manually control connection mode.

#### Scenario: User selects Disconnect now
- **WHEN** the user selects "Disconnect now"
- **THEN** the system SHALL transition to local mode and suppress automatic background network calls to the backend.

#### Scenario: User selects Connect now
- **WHEN** the user selects "Connect now"
- **THEN** the system SHALL clear manual local mode, verify backend reachability, and synchronize tags if reachable.

### Requirement: Open VPN Panel
The system SHALL launch the Android system VPN settings when requested.

#### Scenario: User selects Open VPN Panel
- **WHEN** the user selects "Open VPN Panel"
- **THEN** the system SHALL launch the Android system VPN settings (`android.provider.Settings.ACTION_VPN_SETTINGS`).

### Requirement: Open Connection Settings
The system SHALL navigate to the application connection settings screen.

#### Scenario: User selects Open Connection Settings
- **WHEN** the user selects "Open Connection Settings"
- **THEN** the system SHALL navigate to the Settings screen.

### Requirement: Local mode data access
The system SHALL rely strictly on local storage when operating in local mode.

#### Scenario: Tag retrieval in local mode
- **WHEN** the app loads tags while in local mode
- **THEN** the system SHALL load tags from the local database without attempting network calls to the backend.

### Requirement: Disable downloads in local mode
The system SHALL disable download and caching features when operating in local mode.

#### Scenario: Download all disabled in local mode
- **WHEN** the app is in local mode
- **THEN** the "Download All" feature in the drawer and sync screen SHALL be disabled.

#### Scenario: Tag list caching disabled in local mode
- **WHEN** the user views a tag list while in local mode
- **THEN** the "Cache All" button and individual track download buttons SHALL be disabled.
