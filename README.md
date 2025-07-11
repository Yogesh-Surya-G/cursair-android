<div align="center">
  <!-- Consider adding an assets/android_logo.png to your repo and use:
  <img src="assets/android_logo.png" alt="Cursair Android Logo" width="60" height="60" align="center"> 
  For now, using the desktop logo as a placeholder concept -->
  <img src="https://raw.githubusercontent.com/Yogesh-Surya-G/cursair-desktop/main/assets/foreground.png" alt="Cursair Logo" width="60" height="60" align="center"> 
  
  <h1 align="center">𝗖𝘂𝗿𝘀𝗮𝗶𝗿 (Android)</h1>

  <p align="center">
    The Android companion app for Cursair Desktop. Transforms your smartphone into a wireless mouse with secure, low-latency control.
    <br />
    <a href="#features"><strong>Explore Features »</strong></a>
    ·
    <a href="#installation">Installation</a>
    ·
    <a href="https://github.com/Yogesh-Surya-G/cursair-android/issues">Report Bug</a>
    ·
    <a href="https://github.com/Yogesh-Surya-G/cursair-android/issues">Request Feature</a>
  </p>

  <p align="center">
    <img src="https://img.shields.io/github/license/Yogesh-Surya-G/cursair-android" alt="License">
    <img src="https://img.shields.io/github/stars/Yogesh-Surya-G/cursair-android" alt="Stars">
    <img src="https://img.shields.io/github/issues/Yogesh-Surya-G/cursair-android" alt="Issues">
    <img src="https://img.shields.io/github/last-commit/Yogesh-Surya-G/cursair-android" alt="Last Commit">
    <!-- Optional: <img src="https://img.shields.io/github/v/release/Yogesh-Surya-G/cursair-android" alt="Latest Release"> -->
    <!-- Optional: <img src="https://img.shields.io/github/downloads/Yogesh-Surya-G/cursair-android/total" alt="Total Downloads"> -->
  </p>
</div>

---

## 🎯 Overview

Cursair for Android is the mobile companion to the [Cursair Desktop application](https://github.com/Yogesh-Surya-G/cursair-desktop). It leverages your Android device's motion sensors to provide intuitive and wireless mouse control for your computer. Using a secure QR code connection and the UDP protocol for ultra-low latency, it enables seamless cursor control.

## ✨ Features

- 📱 **Sensor-Driven Control:** Utilizes gyroscope and accelerometer for precise cursor movement.
    - **Horizontal (DX):** Smooth rotational input from the gyroscope.
    - **Vertical (DY):** "Snap-to-Zero" mechanism via linear accelerometer for crisp vertical stops.
- 🔒 **Secure QR Code Pairing:** Connects seamlessly with Cursair Desktop via a one-time QR scan.
- ⚡ **Ultra-Low Latency:** Communicates movement data over UDP for real-time responsiveness.
- 🌐 **Local Network Operation:** Works over your local WiFi network.
- 💨 **Lightweight & Efficient:** Designed for minimal resource usage on your Android device.
- 🛠️ **Modern Android Development:** Built with Kotlin, Jetpack Compose, and Coroutines.

## 🛠️ Technology Stack

### Android Application
- 🤖 **Kotlin:** Primary programming language.
- 🎨 **Jetpack Compose:** For building the user interface.
- 🧭 **Android Sensor Framework:** Accessing gyroscope and linear accelerometer.
- 🌊 **Kotlin Coroutines & Flow:** For asynchronous operations and data streaming.
- 🌐 **UDP Sockets (java.net):** For network communication with the desktop client.
- 📷 **CameraX with ML Kit Barcode Scanning:** For QR code scanning (based on typical modern Android implementation).

### Network Protocol
Cursair for Android uses UDP (User Datagram Protocol) to send movement data to the Cursair Desktop application, offering:
- ⚡ **Low Latency:** Minimal overhead for real-time cursor input.
- 🎯 **Fast Transmission:** No connection establishment required for each packet.
- 📊 **Efficient Packets:** Small data packets (JSON strings) for `dx` and `dy` values.
- 🔄 **Continuous Updates:** Streams sensor data at a high frequency (currently ~60 FPS based on `PACKET_SEND_INTERVAL_MS = 16L`).

## 📋 Prerequisites

- Android Device:
    - Android 7.0 (Nougat) or higher (Current `minSdk` is 24).
    - Functional gyroscope and accelerometer sensors.
- Computer and Android device on the same WiFi network.
- [Cursair Desktop Application](https://github.com/Yogesh-Surya-G/cursair-desktop) installed and running on your computer.

## 🚀 Getting Started

### Installation

1.  **Download the APK:**
    *   Navigate to the [**Releases** page](https://github.com/Yogesh-Surya-G/cursair-android/releases) of this repository (releases will be added soon).
    *   Download the latest `Cursair-Android-vX.X.X.apk` file once available.
2.  **Install on your Android Device:**
    *   Transfer the downloaded APK file to your Android device.
    *   Open a file manager app on your device, locate the APK, and tap on it.
    *   You might need to **enable "Install from Unknown Sources"** in your Android settings if prompted.

### How it Works (Usage)

1.  💻 Launch the **Cursair Desktop application** on your computer. It will display a QR code.
2.  📱 Open the **Cursair Android app** on your smartphone.
3.  📸 Tap the "Scan QR Code" button in the Android app and point your phone's camera at the QR code displayed on your computer screen.
4.  🤝 Once the connection is established, the app will indicate it's connected (e.g., UI changes or a status message).
5.  ✨ Move your phone to control the cursor on your desktop wirelessly!
6.  🛑 To stop, use the "Disconnect" button in the Android app or close the Cursair Desktop application.

## 🛠️ Building from Source (for Developers)

### Prerequisites

*   [Android Studio Giraffe | 2022.3.1 Patch 2](https://developer.android.com/studio) or newer (based on your current project files).
*   Android SDK Platform 34 (current `targetSdk` and `compileSdk`).
*   Java Development Kit (JDK) 17 (as specified in your `build.gradle.kts` `jvmTarget`).
*   An Android device or emulator for testing.

### Steps

1.  **Clone the repository:**
    bash git clone https://github.com/Yogesh-Surya-G/cursair-android.git cd cursair-android
2.  **Open in Android Studio:**
    *   Launch Android Studio.
    *   Select "Open" and navigate to the cloned `cursair-android` repository folder.
3.  **Gradle Sync:** Allow Android Studio to sync the project and download all dependencies. This should happen automatically.
4.  **Build & Run:**
    *   Select your target device or emulator.
    *   Click the "Run 'app'" button (▶️) or go to **Build > Make Project**.

## 🤝 Contributing

Contributions are what make the open-source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

1.  Fork the Project
2.  Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3.  Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4.  Push to the Branch (`git push origin feature/AmazingFeature`)
5.  Open a Pull Request

Please ensure your code adheres to the project's coding style and includes tests where applicable.

## 📄 License

Distributed under the MIT License. See `LICENSE.md` for more information.
**(Note: Please add a `LICENSE.md` file with the MIT License text to your repository root.)**

## 💬 Contact / Support

Yogesh Surya G - [yogesh.gorrepati30@gmail.com](mailto:yogesh.gorrepati30@gmail.com)

Project Link (Desktop): [https://github.com/Yogesh-Surya-G/cursair-desktop](https://github.com/Yogesh-Surya-G/cursair-desktop)
Project Link (Android): [https://github.com/Yogesh-Surya-G/cursair-android](https://github.com/Yogesh-Surya-G/cursair-android)

Report an Android App Bug: [https://github.com/Yogesh-Surya-G/cursair-android/issues](https://github.com/Yogesh-Surya-G/cursair-android/issues)
Request an Android App Feature: [https://github.com/Yogesh-Surya-G/cursair-android/issues](https://github.com/Yogesh-Surya-G/cursair-android/issues)

---

<div align="center">
  Made with ❤️ by <a href="https://github.com/Yogesh-Surya-G">Yogesh Surya G</a>
</div>
