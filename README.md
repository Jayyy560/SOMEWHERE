<div align="center">

# 📍 SOMEWHERE

**Discover the world, one drop at a time.**

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-blue.svg?logo=kotlin)](#)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Ready-success?logo=android)](#)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg?logo=android)](#)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM-orange.svg)](#)

</div>

---

## What is Somewhere?
**Somewhere** is a sleek, modern, minimal location-based discovery app built natively for Android. It lets you explore the real world and unlock hidden digital content, photos, and messages left at specific GPS coordinates—called **Drops**.

You can only view a Drop if you physically walk to its location!

---

## ✨ Features

🧭 **AR-Inspired Discovery** 
Real-time compass heading and distance calculation using Fused Location and device rotation sensors to guide you to nearby Drops.

📸 **In-App CameraX Integration** 
Snap photos natively in the app when leaving a Drop without ever leaving the experience.

🔓 **Proximity Unlocking**
Drops remain locked and mysterious until you walk within a tight 5-meter radius (literally *right there*).

💾 **Local First & Offline Ready**
All metadata, images, and coordinates are saved locally using Room database, making the experience insanely fast.

---

## 🛠 Tech Stack

Built 100% in Kotlin using modern Android development practices:

* **UI:** Jetpack Compose & Material 3
* **Architecture:** MVVM (Model-View-ViewModel)
* **Dependency Injection:** Dagger Hilt
* **Database:** Room (SQLite)
* **Location & Sensors:** Google Play Services FusedLocationProviderClient & Hardware Sensors
* **Camera:** AndroidX CameraX (camera2)
* **Image Loading:** Coil

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug (or newer recommended)
- Java `JDK 17`
- Android device or emulator with API Level 26+

### Installation

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Jayyy560/SOMEWHERE.git
   ```

2. **Open the project** in Android Studio.

3. *(Optional)* Let the `gradle` sync complete. If you build from the terminal and don't have the wrapper, you can generate it or just rely on your local gradle:
   ```bash
   chmod +x ./set_java.sh
   source ./set_java.sh
   gradle :app:assembleDebug
   ```

4. **Run the App** on an emulator or a physical device with GPS enabled!

<br />

> **Note on Location Permissions:** The app requires `ACCESS_FINE_LOCATION` and `CAMERA` permissions. Please grant them upon first launch to experience everything.

---
<div align="center">
  <i>Crafted with ❤️ for explorers.</i>
</div>
