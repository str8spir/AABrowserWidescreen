# <img src="https://github.com/user-attachments/assets/fa4252fa-b71e-4c87-9b93-d8ad832434cc" width="48" height="48" style="vertical-align: bottom;" /> AA Browser

[![Android](https://img.shields.io/badge/Android-15%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://www.android.com/)
[![License](https://img.shields.io/badge/License-GPLv3-blue.svg?style=for-the-badge)](https://www.gnu.org/licenses/gpl-3.0)
[<img src="https://github.com/user-attachments/assets/1551eaef-432d-4634-875c-f085870d00a1" alt="Get it on Obtainium" height="40">](https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22com.kododake.aabrowser%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2FSaveEditors%2FAABrowser%22%2C%22author%22%3A%22SaveEditors%22%2C%22name%22%3A%22AABrowser%22%2C%22preferredApkIndex%22%3A0%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Afalse%2C%5C%22fallbackToOlderReleases%5C%22%3Atrue%2C%5C%22filterReleaseTitlesByRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22filterReleaseNotesByRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22verifyLatestTag%5C%22%3Afalse%2C%5C%22sortMethodChoice%5C%22%3A%5C%22date%5C%22%2C%5C%22useLatestAssetDateAsReleaseDate%5C%22%3Afalse%2C%5C%22releaseTitleAsVersion%5C%22%3Afalse%2C%5C%22trackOnly%5C%22%3Afalse%2C%5C%22versionExtractionRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22matchGroupToUse%5C%22%3A%5C%22%5C%22%2C%5C%22versionDetection%5C%22%3Atrue%2C%5C%22releaseDateAsVersion%5C%22%3Afalse%2C%5C%22useVersionCodeAsOSVersion%5C%22%3Afalse%2C%5C%22apkFilterRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22invertAPKFilter%5C%22%3Afalse%2C%5C%22autoApkFilterByArch%5C%22%3Atrue%2C%5C%22appName%5C%22%3A%5C%22%5C%22%2C%5C%22appAuthor%5C%22%3A%5C%22%5C%22%2C%5C%22shizukuPretendToBeGooglePlay%5C%22%3Afalse%2C%5C%22allowInsecure%5C%22%3Afalse%2C%5C%22exemptFromBackgroundUpdates%5C%22%3Afalse%2C%5C%22skipUpdateNotifications%5C%22%3Afalse%2C%5C%22about%5C%22%3A%5C%22%5C%22%2C%5C%22refreshBeforeDownload%5C%22%3Afalse%2C%5C%22includeZips%5C%22%3Afalse%2C%5C%22zippedApkFilterRegEx%5C%22%3A%5C%22%5C%22%7D%22%2C%22overrideSource%22%3Anull%7D)

[![Android](https://img.shields.io/badge/Android-15%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://www.android.com/)
[![License](https://img.shields.io/badge/License-GPLv3-blue.svg?style=for-the-badge)](https://www.gnu.org/licenses/gpl-3.0)

# AABrowser (Widescreen Navigation Edition)

**The ultimate WebView browser experience for Android Auto, optimized for ultra-widescreen head units.**
This version is specifically enhanced for displays like BMW iDrive 7/8/9, Mercedes MBUX, and other wide aspect ratio systems.

## 🧬 Project Lineage
This repository is a specialized fork focused on navigation ergonomics and widescreen scaling.
* **This Fork:** Widescreen Navigation Edition
* **Forked From:** [SaveEditors/AABrowser](https://github.com/SaveEditors/AABrowser)
* **Original Project:** [kododake/AABrowser](https://github.com/kododake/AABrowser)

---

## ✨ Features

### 🚀 Widescreen Edition Exclusives (New & Recent)
- 🚥 **5-Button Integrated Command Dock:** A horizontal navigation bar replacing the single button for rapid control.
    - **Toggle/Crop:** Adjust view heights for wide displays.
    - **FS (FullScreen):** One-tap Immersive Mode (hides Android Status/Nav bars).
    - **R (Reset):** Instant page reload and scale reset "Panic Button."
    - **(+) Zoom In:** Incremental page magnification.
    - **(-) Zoom Out:** Incremental page reduction.
- 🏎️ **BMW iDrive Optimization:** - **Rotary Zoom:** Use the iDrive rotary controller to zoom video elements (1.0x to 2.0x).
    - **Touchpad Mouse:** Use the iDrive controller's touchpad to move a virtual cursor and click.
- 📺 **Widevideo Crop-to-Fill:** Uses `object-fit: cover` logic to scale video content to fill ultra-wide displays.
- 🔒 **Document Overflow Lock:** Prevents erratic page scrolling while in Zoom/Crop mode to keep content centered.

### 🌐 Core Browser Functionality
- 🎬 **Immersive Media:** Fullscreen DRM-protected video support (Widevine L3) for streaming while parked.
- 🎨 **Theming:** Toggle between Light mode and a true-black AMOLED Dark mode.
- 🌓 **Beta Dark Pages:** Forces WebView to darken supported web pages when Dark mode is active.
- 🏠 **Home & Start Page:** Custom dashboard with six configurable quick-link slots and local icon caching.
- 🗂️ **Tab Management:** Open multiple tabs, switch via an in-app manager, and optionally restore sessions on launch.
- 🧭 **Persistent URL Bar:** Optional compact address bar always visible above the page.
- 🔎 **Global Display Scale:** Adjust UI and content scale together with custom percentage presets.
- 🔄 **Desktop Mode:** Seamlessly toggle between mobile and desktop site rendering.

---

## 🆕 Recent Enhancements Summary
- Implemented the **5-button horizontal Navigation Dock**.
- Fixed critical startup crashes related to menu layout on high-resolution screens.
- Added **Immersive Mode** support to utilize 100% of widescreen real estate.
- Improved **Video Restoration** system to preserve page styles after exiting crop mode.
- Added default quick links for YouTube, Twitch, Kick, and more.

---

## 📱 Quick Start & Safety 🚦

> [!CAUTION]
> **Stationary Use Only:** This app is intended for use only while the vehicle is safely parked. **DO NOT** attempt to use this app while driving.

#### 🛠️ How to Enable Unknown Sources on Android Auto
1. Open **Android Auto Settings** on your phone.
2. Scroll to the bottom and **tap "Version" 10 times**.
3. Tap the **three-dot menu (⋮)** -> **Developer settings**.
4. Enable **Unknown sources**.

---

## 🤝 Credits & Contributors

A massive thanks to the developers who made this tool possible:

- **[kododake](https://github.com/kododake):** Original Project Lead.
- **[SaveEditors](https://github.com/SaveEditors):** For the critical updates to tabs, themes, and start-page architecture.
- **[cmacrowther](https://github.com/cmacrowther):** Design improvements.
- **[jigneshbhavani](https://github.com/jigneshbhavani):** Microphone support.

---

## 🛡️ Privacy
This app is designed with a **Transparency-First** policy. No URL tracking, no Big Tech trackers, and only anonymous, self-hosted analytics are used to improve app stability.
