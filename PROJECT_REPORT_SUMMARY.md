# ParisaraCycle Project Summary

## Project Title

**ParisaraCycle - Green Commuter Guide Android Application**

## Project Overview

ParisaraCycle is a Kotlin-based Android application designed to support bicycle commuters. The app helps cyclists plan routes, report unsafe road conditions, find basic pit-stops, track environmental benefits, and optionally share their live location with nearby riders.

The project is built so it can run in two modes:

- **Local demo mode:** Works without Firebase, Google Maps, or Directions API keys.
- **Cloud/API-ready mode:** Can be connected later to Firebase Authentication, Firestore, Firebase Realtime Database, Google Maps, and Google Directions.

This makes the application suitable for demonstration, academic reporting, and future real-world expansion.

## Problem Statement

Cyclists often face problems such as unsafe roads, potholes, blocked paths, lack of nearby support points, and limited awareness of the environmental impact of cycling. ParisaraCycle addresses these issues by providing a mobile platform where riders can view cycling-related map information, report danger zones, calculate route distance, track CO2 savings, and connect with nearby riders.

## Main Objectives

- Encourage eco-friendly commuting through cycling.
- Help cyclists identify safer and more useful routes.
- Allow users to report danger zones such as potholes, blocked paths, and dangerous intersections.
- Show nearby pit-stops such as cycle repair points and water points.
- Track daily and monthly distance cycled and estimated CO2 saved.
- Provide an optional buddy system for nearby rider visibility.
- Keep the app usable without mandatory backend or paid API setup.

## Technology Stack

- **Language:** Kotlin
- **Platform:** Android
- **UI Framework:** Jetpack Compose
- **Design System:** Material 3
- **Architecture Pattern:** MVVM-style separation using UI screens, ViewModels, repositories, models, and utilities
- **Build System:** Gradle Kotlin DSL
- **Location Services:** Google Play Services Location
- **Optional Backend:** Firebase Authentication, Cloud Firestore, Firebase Realtime Database
- **Optional Routing:** Google Directions API
- **Optional Map Support:** Google Maps SDK / Maps Compose
- **Local Storage:** Android SharedPreferences

## Project Structure

The project follows a single Android app module structure.

- `MainActivity.kt`: Starts the Compose app and creates the application container.
- `ui/ParisaraCycleApp.kt`: Main Compose app shell, login flow, bottom navigation, and screen switching.
- `ui/screens/`: Contains Login, Map, Stats, and Profile screens.
- `ui/components/`: Contains reusable UI components such as the bottom navigation bar.
- `viewmodel/`: Contains ViewModels for authentication, map behavior, and eco-stats.
- `data/model/`: Defines app data models such as user, danger zone, pit-stop, route result, buddy location, and eco-stats.
- `data/repository/`: Contains data access and business logic for authentication, location, routes, danger zones, pit-stops, buddy locations, and eco-stats.
- `data/firebase/`: Detects Firebase availability and provides Firebase clients when configured.
- `utils/`: Contains helper classes for permissions, distance calculation, CO2 formatting, date keys, and polyline decoding.
- `docs/`: Contains existing user guide, internship diary, and internship key point documents in HTML/PDF format.

## Application Flow

When the app starts, `MainActivity` initializes `AppContainer` and launches `ParisaraCycleApp`. The app first checks the authentication state. If no user is signed in, the Login screen is shown. After sign-in or registration, the user can access three main tabs through the bottom navigation bar:

- **Map:** Route planning, danger-zone reporting, pit-stop markers, buddy markers, and layer filters.
- **Stats:** Daily and monthly distance and CO2 savings.
- **Profile:** User information, location permission action, live-location sharing toggle, and sign out.

## Major Features

### 1. Authentication

The app provides login and registration using an email and password. In local demo mode, the user session is stored on-device using SharedPreferences. If Firebase is configured by adding `google-services.json`, the same authentication flow can use Firebase Email/Password Authentication.

### 2. Keyless Map Screen

The Map screen uses a Compose-rendered local map surface when API keys are not available. It displays road-like lines, cycle paths, current location, destination marker, route line, danger markers, pit-stops, and nearby buddy markers. This allows the project to be demonstrated without Google Maps billing or setup.

### 3. Route Planning

Users can select a destination by tapping the map and then generate a route. If a valid Google Directions or Maps API key is available, the app can request a bicycling route from Google Directions. If no key is configured, it generates a local route between source and destination.

The route distance is calculated and used for eco-stat tracking.

### 4. Danger Zone Reporting

Users can long-press the map to report a danger zone. Supported danger types are:

- Pothole
- Dangerous Intersection
- Blocked Path

In local mode, danger zones are stored in SharedPreferences. If Firebase is configured, the danger-zone records are stored in Firestore with latitude, longitude, type, timestamp, and user ID.

### 5. Pit-stop Finder

The app shows nearby pit-stop markers for:

- Cycle repair points
- Water points

The current implementation uses generated mock pit-stop data around the current map center, making the feature available without a Places API dependency.

### 6. Eco-stats

The app tracks the environmental benefit of cycling. When a route is generated, the route distance is added to the user's statistics. The app uses this formula:

**1 km cycled = 120 g CO2 saved**

The Stats screen displays:

- Today's cycled distance
- Today's CO2 saved
- This month's cycled distance
- This month's CO2 saved
- Current daily average based on monthly CO2 savings

Stats are stored locally using SharedPreferences.

### 7. Buddy System

The Profile screen includes a live-location sharing switch. When enabled, the app can publish the rider's location and show nearby riders on the Map screen. In local demo mode, it displays demo buddy markers near the user's location. With Firebase Realtime Database configured, it can synchronize live rider locations.

The app filters visible buddies by excluding the active user, checking recent timestamps, and showing riders within approximately 5 km of the current location.

### 8. Map Layer Filters

The Map screen includes filter chips for:

- Danger zones
- Pit-stops
- Buddies
- Route

These filters let users control which map elements are visible.

## Architecture Summary

ParisaraCycle uses a clean separation of responsibilities:

- **UI layer:** Jetpack Compose screens display data and forward user actions.
- **ViewModel layer:** Holds UI state, handles user actions, and coordinates repositories.
- **Repository layer:** Handles authentication, route generation, local/cloud storage, location updates, and stats.
- **Model layer:** Defines structured data used across the app.
- **Utility layer:** Provides reusable calculations and permission helpers.

This structure improves maintainability because UI code is separated from business logic and data handling.

## Data Storage and Backend Design

The app is designed with local fallback support:

- Local authentication uses SharedPreferences.
- Local danger zones are stored as JSON in SharedPreferences.
- Local eco-stats are stored in SharedPreferences with daily and monthly keys.
- Local buddy mode generates demo nearby riders.
- Local route generation works without network requests.

For future deployment, Firebase can be enabled:

- Firebase Authentication for real user login.
- Firestore for shared danger-zone reports.
- Firebase Realtime Database for live buddy location sharing.

## Permissions

The app declares these Android permissions:

- `ACCESS_FINE_LOCATION`
- `ACCESS_COARSE_LOCATION`
- `ACCESS_NETWORK_STATE`
- `INTERNET`

Location permission is used for current location and live-location sharing. The app still works in demo mode if location permission is not granted.

## Environmental Calculation

The project uses a simple CO2-saving estimation:

`CO2 saved = distance in km * 120 grams`

This provides a visible environmental metric that encourages cycling and supports the green commuting purpose of the project.

## Build and Setup

The app can be built from the project root using:

```powershell
.\gradlew.bat assembleDebug --no-daemon
```

If dependencies are already cached, it can also be built offline:

```powershell
.\gradlew.bat assembleDebug --offline --no-daemon
```

Optional API keys can be added to `local.properties`:

```properties
MAPS_API_KEY=your_maps_android_key
DIRECTIONS_API_KEY=your_directions_key
```

Firebase can be enabled by adding `app/google-services.json` and enabling Email/Password Authentication, Firestore, and Realtime Database in Firebase.

## Existing Documentation

The `docs/` directory already contains:

- User guide
- Internship key points
- Internship diary

These files support demonstration, reporting, and presentation of the project.

## Project Outcomes

- A working Android application structure was created using Kotlin and Jetpack Compose.
- The app supports login, route selection, danger-zone reporting, pit-stop markers, eco-stat tracking, and live-location sharing.
- The project can run without external API configuration, which makes it easy to demonstrate.
- The codebase is structured for future expansion with Firebase and Google APIs.
- The project supports sustainable mobility by helping cyclists and showing CO2 savings.

## Limitations

- The current map surface is locally rendered in demo mode and is not a full Google Maps replacement.
- Pit-stop data is mock/generated and not fetched from a real Places service.
- Local authentication is suitable for demonstration but not for production security.
- Real-time buddy sharing requires Firebase Realtime Database configuration.
- Accurate real-world bicycle routing requires a valid Google Directions API key.

## Future Enhancements

- Integrate a fully configured Google Maps view for production map display.
- Use Google Directions or another routing provider for real bicycle route data.
- Add real pit-stop discovery through a places database or API.
- Add route safety scoring based on reported danger zones.
- Add user profiles, ride history, and achievements.
- Add admin moderation for reported danger zones.
- Add notifications for nearby hazards.
- Improve testing with unit tests and UI tests.

## Conclusion

ParisaraCycle is a practical green commuting Android application focused on cyclist safety, convenience, and environmental awareness. It combines route planning, danger-zone reporting, pit-stop support, CO2 tracking, and nearby rider visibility in a single mobile app. The project is suitable for academic reporting because it demonstrates Android development, MVVM architecture, location-based logic, local storage, optional cloud integration, and sustainable technology goals.

