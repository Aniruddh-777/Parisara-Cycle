# Parisara-Cycle Setup

## Run Without Manual API Setup

The app now runs out of the box with no Firebase project, no Google Maps key, and no Directions key.

Default no-key mode uses:

- Local email/password session stored on-device
- Local danger-zone storage
- Local demo buddy markers
- A Compose-rendered keyless map surface
- Local route generation and CO2 calculation
- Mock pit-stops

Build:

```powershell
.\gradlew.bat assembleDebug --no-daemon
```

If dependencies are already cached:

```powershell
.\gradlew.bat assembleDebug --offline --no-daemon
```

## Optional Google Maps And Directions

If you later want real Google routing, add API keys to root `local.properties`:

```properties
MAPS_API_KEY=your_maps_android_key
DIRECTIONS_API_KEY=your_directions_key
```

Enable these APIs in Google Cloud:

- Maps SDK for Android
- Directions API

The app automatically falls back to local route generation when no usable key exists.

## Optional Firebase

If you later want real Firebase sync:

1. Create a Firebase Android app with package:

```text
com.example.parisaracycle
```

2. Put the downloaded config here:

```text
app/google-services.json
```

3. Enable:
   - Authentication > Email/Password
   - Firestore Database
   - Realtime Database

Without `google-services.json`, the app uses the local repositories automatically.

## Permissions

The manifest includes:

```text
ACCESS_FINE_LOCATION
ACCESS_COARSE_LOCATION
ACCESS_NETWORK_STATE
INTERNET
```

Location permission is optional for the demo. If granted, the app can use the device location as the route source and for live location updates.

## Feature Locations

- Compose app shell: `MainActivity.kt`, `ui/ParisaraCycleApp.kt`
- Screens: `ui/screens/LoginScreen.kt`, `MapScreen.kt`, `StatsScreen.kt`, `ProfileScreen.kt`
- Repositories: `data/repository/`
- Local CO2 stats: `data/repository/EcoStatsRepository.kt`
- Keyless map UI: `ui/screens/MapScreen.kt`
