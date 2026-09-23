# Nova Android App

Nova is a native Android assistant starter project.

## Included
- Chat-style interface
- Text-to-speech
- Speech recognition
- Flashlight control
- Battery status
- Volume control
- Vibration
- Open Settings
- Open Camera
- Open browser
- Creator information
- Help screen
- GitHub Actions APK build workflow

## Before building
Open `MainActivity.kt` and replace:
- `creatorName`
- `creatorTitle`
- `creatorBio`

with your real creator information.

The app uses Android system APIs and permissions. It does not attempt to bypass Android security or access protected private data.

## Build
Use Android Studio or another maintained Gradle/Android build environment.

The GitHub Actions workflow in `.github/workflows/build-apk.yml` can build a debug APK in the cloud after the project is placed in a GitHub repository.
