# None More Black (n̈mb)

**Version:** 11.0  
**Author:** Jerry Kindall  
**Package:** `com.engyrus.nonemoreblack`  
**Target:** Android TV / Google TV (`minSdk 28`, `targetSdk 34`)

## Purpose

**None More Black** lets you easily make your TV screen pure black (`#000000`) while listening to music from apps like Spotify, YouTube, or TuneIn. On OLED TVs, displaying true `#000000` turns off individual pixels—helping **save power** and **prevent OLED burn-in** from static album art, player UIs, or menus. For maximum convenience, use [tvQuickActions](https://play.google.com/store/apps/details?id=dev.vodik7.tvquickactions&hl=en_US) or a similar app to assign it to a button on your remote.

> [!NOTE]
> **This app does not turn the screen off.** It displays a full-screen, pure-black window while keeping the streaming device awake (`FLAG_KEEP_SCREEN_ON`). Because the TV and streamer remain powered on, background audio and HDMI/CEC volume controls continue working uninterrupted.
> 
> If your TV does High Dynamic Range (HDR) video, as most do these days, make sure *Match content dynamic rage* is turned on in your streaming box's Display & Sound settings to make sure you get truly black.

## Its best features are what it *doesn't* do

- **Instant Toggle (`singleInstance`):** Launching the app a second time (for example, via a mapped remote button in **tvQuickActions**) immediately closes it (`finishAndRemoveTask()`) and returns you to your previous app.
- **Zero-Animation Transitions:** All open, close, enter, and exit animations are disabled in `themes.xml` and `MainActivity.kt` (`overrideActivityTransition` on API 34+, `overridePendingTransition(0, 0)` on API 28–33) for an instant on/off blackout.
- **True OLED Black & Immersive Fullscreen:** Pure `#000000` window and view background, SDR color mode (`COLOR_MODE_DEFAULT`), no action bar/title/shadows, and hidden system bars.
- **Audio & Volume Friendly:** Requests no audio focus and creates no `MediaSession`. Volume keys (`VOLUME_UP`, `VOLUME_DOWN`, `VOLUME_MUTE`) pass through to system/CEC volume control without dismissing the black screen.
- **Backup Exit & Clean Task Lifecycle:** Pressing `DPAD_CENTER`, `ENTER`, `BACK`, `ESCAPE`, or `Home` (`onStop()`) cleanly destroys the task, and `excludeFromRecents` / `autoRemoveFromRecents` / `noHistory` keep it out of Recent Apps.

## The Name

Yes, it's a *This Is Spin̈al Tap* reference. "It's like, how much more black could this be? And the answer is none. None more black." There are a couple more nods to that classic mockumentary in the app.

## Project Structure

```text
nonemoreblack/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── gradle/wrapper/
│   ├── gradle-wrapper.jar
│   └── gradle-wrapper.properties
├── README.md
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/engyrus/nonemoreblack/MainActivity.kt
        └── res/
            ├── drawable/
            │   ├── ic_launcher.xml
            │   └── tv_banner.xml
            ├── layout/activity_main.xml
            └── values/
                ├── colors.xml
                ├── strings.xml
                └── themes.xml
```

## Building the APK

### Android Studio
1. Open the `nonemoreblack` folder in Android Studio and let Gradle sync.
2. Build a debug APK via **Build → Build Bundle(s) / APK(s) → Build APK(s)** (`app/build/outputs/apk/debug/app-debug.apk`), or generate a signed release APK/AAB via **Build → Generate Signed Bundle / APK**.

### Command Line (Windows PowerShell)

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat assembleDebug
```

## Sideloading via ADB

1. Enable **Developer options** and **USB/Network debugging** on your Google TV / Android TV device.
2. Connect and install the APK:

```powershell
adb connect <DEVICE_IP>:5555
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

3. Optionally, map a remote button in **tvQuickActions** (or a similar app) to launch **None More Black** (`com.engyrus.nonemoreblack`). Press that button once to black out the screen; press again (or, really, almost any other key) to reveal the app behind the blackout curtain. You can also simply launch **n̈mb** from your launcher whenever you need to blank the screen.