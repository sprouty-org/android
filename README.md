# Sprouty Mobile — Android Client

![Kotlin](https://img.shields.io/badge/Kotlin-2.2.21-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Architecture](https://img.shields.io/badge/Architecture-MVVM-blue?style=for-the-badge&logo=android&logoColor=white)
![Firebase](https://img.shields.io/badge/Auth-Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)

This repository contains the source code for the **Sprouty** Android application. The app serves as the central hub for users to manage their botanical garden, view real-time sensor telemetry, and receive AI-driven plant care insights.

---

## Technical Architecture

The application follows the **MVVM (Model-ViewModel-View)** architectural pattern to ensure a clean separation of concerns and a reactive user interface.

### Project Structure Analysis

Based on the project hierarchy, the source code is organized as follows:

* **data/database**: Implements local persistence using **Room Database**. Key files include `AppDatabase.kt` and `PlantDao` for structured offline storage.
* **data/model**: Contains core data entities such as `Plant.kt` and `PlantApiDataModels.kt` for API serialization.
* **data/network**: Manages remote service communication via **Retrofit 3.0**. It includes `AuthInterceptor` for JWT management and `MyFcmService` for Firebase Cloud Messaging.
* **ui**: Divided by feature sets including `garden`, `loading`, `login`, `register`, and `settings`. Each module utilizes dedicated Activities and ViewModels (e.g., `PlantViewModel.kt`).
* **util**: Provides utility classes for `auth` (Firebase and JWT handling), `limiters` (Action Rate Limiting), `storage` (SharedPreferences), and `network` configuration.



---

## Core Functionalities

* **Secure Authentication Pipeline**: Leverages Firebase SDK for identity management. It implements a token-exchange flow where a Firebase OIDC token is traded for a backend-signed JWT.
* **Botanical Inventory**: Provides a comprehensive overview of the user's garden, fetching data from the Plant Service and caching it locally for offline visibility.
* **Reactive Telemetry Monitoring**: Receives push notifications via **Firebase Cloud Messaging (FCM)** regarding soil moisture and temperature thresholds.
* **Optimized Image Loading**: Uses a dual-library approach with **Glide** and **Coil** to handle high-resolution plant imagery and sensor-captured photos efficiently.

---

## Technical Stack

* **Language**: Kotlin 2.2.21
* **Networking**: Retrofit 3.0 & OkHttp 5.3.2
* **Local Storage**: Room 2.8.4 & Shared Preferences
* **Dependency Management**: Gradle Version Catalog (libs.versions.toml)
* **Asynchronous Execution**: Kotlin Coroutines 1.10.2
* **UI Framework**: Material 3 (1.4.0) with a mix of XML and Jetpack Compose Interop

---

## Installation and Development

### Prerequisites
* Android Studio (Ladybug or newer recommended)
* JDK 21
* A valid `google-services.json` file in the `/app` directory

### Setup
1. **Clone the repository**:
   git clone [https://github.com/sprouty-org/android](https://github.com/sprouty-org/android)
   
2. **Synchronize Gradle:** Open the project and allow the IDE to sync dependencies from the version catalog.

3. **Configuration:** Ensure the backend URL in NetworkModule matches your current deployment (default: sprouty.duckdns.org).

## API Integration
The mobile client communicates primarily with the Gateway Service. All requests to protected endpoints are intercepted by AuthInterceptor, which injects the required Authorization: Bearer <JWT> header retrieved from secure storage.

**Author:** David Muhič

**Project:** Sprouty — Smart Plant Companion

**Course:** PRPO 2025/2026   
