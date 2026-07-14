<div align="center">

# 📍 SOMEWHERE

**A Spatial Canvas for the Physical World.**

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-blue.svg?logo=kotlin)](#)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Ready-success?logo=android)](#)
[![ARCore](https://img.shields.io/badge/AR-Google%20ARCore-EA4335?logo=google)](#)
[![Supabase](https://img.shields.io/badge/Backend-Supabase%20PostGIS-3ECF8E?logo=supabase)](#)
[![AI](https://img.shields.io/badge/AI-Google%20Gemini-8E75B2?logo=google)](#)

</div>

<br/>

**Somewhere** is a sleek, hyper-immersive location-based social discovery app built natively for Android. It transforms the physical world into a digital canvas where you can leave and discover hidden digital content, photos, and messages—called **Drops**—permanently locked to real-world coordinates.

To experience a Drop, you must physically travel to its exact location in the real world.

---

## ✨ The Experience

### 🧭 Proximity-Based Discovery
Drops are tied to absolute GPS coordinates and remain securely locked. To uncover the content inside, you must physically walk into the Drop's tight 5-meter radius. The world is your map.

### 🕶️ True AR Spatial Tracking
Powered by Google ARCore, Drops aren't just flat overlays on a screen—they are physical anchors planted in 3D space. As you move your phone, the Drops track flawlessly with 6 Degrees of Freedom (6DoF) and perspective scaling.

### 🎧 Spatial Audio & Granular Haptics
Designed to feel like a premium video game. Somewhere features a continuously shifting generative ambient soundscape, spatialized audio cues that get louder as you approach a Drop, and granular tactile haptics that ground every interaction in physical reality.

### 🧠 Gemini-Powered AI 
- **The Whisperer**: Tap the "Whisper" button to invoke Google Gemini. The AI analyzes your exact GPS location, time of day, and environment to generate and plant mysterious, localized lore drops permanently into the world.
- **Place Summaries**: AI reads the digital footprint of all nearby drops and generates a poetic summary of the area's hidden vibe.

### 📸 Native In-App Capture
Integrated directly with AndroidX CameraX, allowing you to snap and drop photos natively without ever leaving the immersive AR experience.

---

## 🛠 Architecture & Tech Stack

Somewhere is engineered for zero-latency discovery using a Local-First architecture synced with a Realtime edge backend.

- **Frontend UI:** 100% Kotlin & Jetpack Compose (Edge-to-Edge immersive mode)
- **AR Engine:** Google ARCore & OpenGL Matrix Math
- **Camera:** AndroidX CameraX
- **Architecture:** MVVM (Model-View-ViewModel) + Clean Architecture
- **Dependency Injection:** Dagger Hilt
- **Local Persistence:** Room (SQLite) with encrypted DataStore
- **Backend Edge:** Supabase (PostgreSQL + PostGIS spatial queries)
- **AI Integration:** Google Gemini Pro/Flash API
- **Image Loading:** Coil

---

## 🚀 Key Technical Highlights

#### 📡 Real-Time Spatial Sync (PostGIS)
The app leverages Supabase RPCs and PostGIS spatial math (`ST_DWithin`) to instantly fetch and filter Drops within your physical radius.

#### 🎥 Bulletproof AR Fallbacks
If a device lacks ARCore hardware support, or if tracking is temporarily lost in a dark room, the app seamlessly falls back to a custom **2.5D compass-driven HUD** powered by the Fused Location Provider and hardware rotation vectors.

#### 🔋 Battery-Optimized State Management
Location polling, AR tracking, and network syncing are aggressively lifecycle-aware, pausing instantly when the app is backgrounded to preserve battery and thermal performance.

---

## 📦 Getting Started

### Prerequisites
- Android Studio Ladybug (or newer recommended)
- Java `JDK 17`
- Android device or emulator with API Level 26+ (Physical device heavily recommended for AR and GPS)

### Installation

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Jayyy560/SOMEWHERE.git
   ```

2. **Open the project** in Android Studio.

3. *(Optional)* Let the `gradle` sync complete. If you build from the terminal:
   ```bash
   chmod +x ./set_java.sh
   source ./set_java.sh
   ./gradlew assembleDebug
   ```

4. **Run the App** on a physical Android device. Walk outside and start dropping!

> **Note on Permissions:** The app requires `ACCESS_FINE_LOCATION`, `CAMERA`, and `RECORD_AUDIO` (for future features) permissions. Please grant them upon first launch to experience the AR canvas.

---
<div align="center">
  <i>The physical world is just the first layer.</i>
</div>
