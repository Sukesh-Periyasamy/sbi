# Android Development Environment Summary (AnteClick)

## System

| Item | Value |
|------|-------|
| OS | Windows 11 |
| Java (JDK) | 21.0.10 (Android Studio bundled JBR) |
| Java Path | `C:\Program Files\Android\Android Studio\jbr` |

## Gradle & Build

| Item | Value |
|------|-------|
| Gradle Version | 8.7 |
| Android Gradle Plugin (AGP) | 8.5.2 |
| Kotlin Plugin | 2.3.21 |
| Gradle Embedded Kotlin | 1.9.22 |

## Android Configuration

| Item | Value |
|------|-------|
| compileSdk | 35 |
| targetSdk | 35 |
| minSdk | 31 |
| Java Compatibility | 11 |
| Kotlin JVM Target | 11 |
| UI Framework | Jetpack Compose |

## Project Structure

| Item | Value |
|------|-------|
| Project Name | AnteClick |
| Application ID | `com.anteclick.app` |

## Build Status

**Command executed:**
```
.\gradlew.bat :app:assembleDebug
```

**Result:** BUILD SUCCESSFUL

**Warning:** AGP 8.5.2 was tested up to compileSdk 34. Project uses compileSdk 35. Warning only; build succeeds.

## Important Files

| File | Purpose |
|------|---------|
| `gradle/wrapper/gradle-wrapper.properties` | Gradle Wrapper (uses Gradle 8.7) |
| `gradle/libs.versions.toml` | Version Catalog (AGP 8.5.2, Kotlin 2.3.21) |
| `local.properties` | API keys, backend URL, keystore path |
| `keystore.jks` | Release signing key (NEVER commit to git) |

## For New Laptop Migration

### Install:
- Android Studio
- Android SDK Platform 35
- Android SDK Platform Tools (ADB)
- Android Emulator (optional)

### Copy:
- Entire project folder
- `keystore.jks` (critical for Play Store updates)
- `local.properties` (if it contains API keys or backend URLs)

### No need to copy:
- `.gradle/`
- Gradle downloads
- Android Studio caches

Gradle 8.7 and dependencies will be downloaded automatically when the project is opened.

## Temporary Java Setup (Terminal)

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

## Verification Commands

```bash
java -version
.\gradlew.bat -v
.\gradlew.bat :app:assembleDebug
adb devices
```

## Build Commands

```bash
# Debug build (for testing)
.\gradlew.bat :app:assembleDebug

# Install on connected device/emulator
.\gradlew.bat :app:installDebug

# Release build (for Play Store)
.\gradlew.bat :app:assembleRelease

# Release bundle (AAB for Play Store upload)
.\gradlew.bat :app:bundleRelease

# Run tests
.\gradlew.bat :app:test
```

## APK Output Locations

| Build Type | Path |
|-----------|------|
| Debug APK | `app/build/outputs/apk/debug/app-debug.apk` |
| Release APK | `app/build/outputs/apk/release/app-release.apk` |
| Release AAB | `app/build/outputs/bundle/release/app-release.aab` |
