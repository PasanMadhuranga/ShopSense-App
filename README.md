# ShopSense – Location-Aware Shopping Reminder App

ShopSense is an Android mobile application developed as part of the **Mobile Computing** continuous assessment. The app helps users remember shopping items by providing **location-aware reminders** based on their daily movements.

Instead of time-based reminders, ShopSense uses **geofencing and real-time location tracking** to suggest nearby stores when the user is likely to shop.

## Features

* Add, edit, delete, and manage a to-buy item list
* Set a home location with a configurable radius
* Automatically detect when the user leaves home using geofencing
* Prompt the user to activate Shopping Mode when leaving home
* Run Shopping Mode as a foreground service for background location tracking
* Suggest nearby relevant stores based on the shopping list
* Provide one-tap navigation to stores using Google Maps
* Offline-first design with local data storage

## How the App Works (High Level)

1. The user adds shopping items and sets a home location.
2. A geofence is registered around the home location.
3. When the user exits the home geofence, the app prompts to activate Shopping Mode.
4. Shopping Mode runs as a foreground service and receives location updates.
5. Nearby stores are searched using the Google Places API based on item categories.
6. Store suggestions are shown via notifications.
7. The user can open Google Maps directly for navigation.

## Architecture Overview

The application follows a **layered architecture**:

* **Presentation Layer**
  Jetpack Compose UI and ViewModel for state-driven UI and event handling.

* **Domain Layer**
  Repository interfaces that define data access contracts.

* **Data Layer**
  Room database for item and category storage, and preferences for home location and shopping mode state.

* **Background Layer**
  Geofence handling, Shopping Mode foreground service, and notification management.

External services such as Google Play Services, Google Places API, and Google Maps are used for location, geofencing, nearby search, and navigation.

## Technologies Used

* Kotlin
* Jetpack Compose
* Room Database
* Foreground Services
* Google Play Services (Geofencing, Fused Location Provider)
* Google Places API
* Google Maps

## Permissions Required

* Location
* Background Location (for geofencing and shopping mode)
* Notifications (Android 13+)

These permissions are required to support background location tracking and user notifications.

## Project Structure

The project is organized into the following main packages:

* `presentation` – UI, ViewModel, and UI-related logic
* `domain` – Repository interfaces and domain models
* `data` – Room database, DAOs, preferences, and repository implementations
* `geofence` – Geofence manager and receivers
* `shopping` – Shopping Mode foreground service and related receivers
* `location` – Location provider abstraction

## How to Run the Project

1. Clone the repository.
2. Open the project in Android Studio.
3. Add your Google Places API key to `local.properties` or `strings.xml`.
4. Build and run the app on an Android device with location services enabled.
