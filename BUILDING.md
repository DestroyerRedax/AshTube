# Building AshTube

## Requirements

- JDK 17
- Android SDK Platform 35 and Build-Tools 35.x
- Android SDK command-line tools or Android Studio
- Gradle 8.7, used once to generate the project wrapper

The required tools were not installed in the development environment when this skeleton was created, so no APK has been produced or claimed as verified yet.

## Create the Gradle wrapper

From the project directory, after installing JDK 17 and Gradle 8.7:

```bash
gradle wrapper --gradle-version 8.7
```

This creates the Gradle wrapper files, including `gradlew` and `gradle/wrapper/gradle-wrapper.jar`. Commit those files to the project repository.

## Build a debug APK

Set `ANDROID_HOME` to your Android SDK location, then run:

```bash
./gradlew assembleDebug
```

The initial APK output will be:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## First launch check

Install the debug APK on an Android 8.0+ device or emulator. It should open to an AshTube foundation screen. No network, media provider, login, file, or notification permission is requested by this initial skeleton.
