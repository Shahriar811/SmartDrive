# SmartDrive ☁️
### 75 GB Unified Multi-Drive Cloud Storage for Android

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-blue.svg)](https://developer.android.com/jetpack/compose)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-green.svg)](https://android.com)
[![License](https://img.shields.io/badge/License-Apache%202.0-lightgrey.svg)](LICENSE)

**SmartDrive** is an advanced native Android application that aggregates the free 15 GB storage tier of up to **5 separate Google Accounts** into a single, unified **75 GB Virtual Storage Pool**. It functions as a cloud-only virtual file system where users can batch-upload files using a smart non-splitting allocation algorithm, stream media directly from Google Drive without touching internal storage, download files on demand, and permanently hard-delete files.

Designed with an elegant **Apple iOS & Oceanic Mint** aesthetic featuring fluid spring micro-interactions and a satisfying Day/Night theme transition.

---

## ✨ Key Features

### 1. 75 GB Unified Virtual Storage Pool
- Merges up to 5 distinct Google Drive accounts into one 75 GB storage ecosystem.
- Live quota queries run in parallel across all connected accounts using Kotlin Coroutines (`async` / `awaitAll`).
- Visualized with a custom **Circular Storage Donut Gauge** showing Free % and Used % sweep arcs with real-time statistics.

### 2. Smart Non-Splitting Batch Upload (Bin-Packing Algorithm)
- **Zero File Splitting:** Files are never fragmented across accounts. If Account 1 has 0.5 GB free and Account 2 has 15 GB free, a 2 GB file is automatically allocated entirely to Account 2.
- **First-Fit-Decreasing Allocation:** Sorts candidate files by size and assigns each to the best-fit Google Drive account based on live contiguous free quota.
- Rejects files exceeding the single-account limit (15 GB) with clear warnings.

### 3. Zero Local Storage Footprint (Pure Cloud Streaming)
- Uploads stream bytes directly from Android's `ContentResolver.openInputStream()` into Google Drive's resumable upload socket without caching files to internal storage (`cacheDir` / `filesDir`).
- **Video Player:** AndroidX Media3 ExoPlayer with `DefaultHttpDataSource` passing `Authorization: Bearer <TOKEN>` headers for smooth HTTP byte-range seeking directly from Google Drive.
- **Photo Viewer:** Coil image viewer with `diskCachePolicy(CachePolicy.DISABLED)` and pinch-to-zoom gestures.
- **Document & Code Reader:** In-memory `PdfRenderer` and scrollable code viewer streaming directly from memory buffers.

### 4. Apple iOS & Oceanic Design Aesthetic
- **Signature Gradient Header:** Deep Oceanic Midnight Navy (`#0F1E36` ➔ `#1B3B6F`) with frosted glass cards.
- **Luminous Turquoise Accent:** Apple Teal (`#2CD5C4`) with diffuse glowing drop shadows.
- **Satisfying Day / Night Shift:** Morphing Sun/Moon toggle button with **360° spring bounce** and a global 450ms fluid color interpolation across all surfaces, cards, and typography.
- **Grid & List View Switcher:** Toggle between 2-column squircle cards with 3D gradient category icons and horizontal list rows.

### 5. Multi-Account OAuth & Android Integration
- Layer 1 Master Auth via **Firebase Authentication** & **Cloud Firestore**.
- Layer 2 Cloud Storage linking using Android's native **`AccountManager`** and Google Play Services Auth (`GoogleAuthUtil`).
- Automatic user-recoverable OAuth consent lifecycle handling.

---

## 🛠️ Tech Stack & Architecture

- **Language:** Kotlin 2.0.21 (Coroutines, Flow, StateFlow)
- **UI Framework:** Jetpack Compose with Material 3
- **Architecture:** MVVM (Model-View-ViewModel) + Clean Architecture
- **Networking:** Retrofit 2 + OkHttp 3 (Drive REST API v3, Resumable Uploads)
- **Media & Streaming:** AndroidX Media3 ExoPlayer, Coil Compose
- **Backend & Database:** Firebase Authentication, Cloud Firestore
- **Authentication:** Android `AccountManager` & Google Play Services Auth

---

## 📱 App Architecture

```
com.shuvo.SmartDrive/
├── data/
│   ├── auth/            # Master Firebase Auth & Multi-Drive OAuth Manager
│   ├── model/           # Unified storage state, file models, categories
│   ├── remote/          # Google Drive REST API v3 Retrofit interface
│   └── repository/      # UnifiedStorageRepository (quota, streaming, bin-packing)
├── ui/
│   ├── screens/         # Dashboard, Account Linker, Auth, Cloud Viewers
│   ├── theme/           # iOS Oceanic Palette, Dynamic Theme, Day/Night Switch
│   └── viewmodel/       # MainStorageViewModel coordinating UI and state
└── MainActivity.kt      # Navigation, lifecycle, and OAuth consent launchers
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug or newer
- JDK 17 or JDK 21 / 23
- Android SDK 35 (Android 15)

### Setup Instructions
1. **Clone the repository:**
   ```bash
   git clone https://github.com/Shahriar811/SmartDrive.git
   cd SmartDrive
   ```

2. **Firebase & Google Services Configuration:**
   - Copy `app/google-services.json.example` to `app/google-services.json` (or download your project's `google-services.json` from the Firebase Console for package `com.shuvo.SmartDrive`).
   - Register the debug certificate SHA-1 in your Firebase Console (*Project settings* ➔ *General* ➔ *Your apps* ➔ *Add fingerprint*):
     ```text
     A6:D6:EB:E1:FF:0D:EE:15:18:4F:B4:ED:08:7A:8A:2C:1A:34:1F:6A
     ```

3. **Build and Run:**
   ```bash
   ./gradlew assembleDebug
   ```

---

## 📄 License

```text
Copyright 2026 Shahriar

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```
