# Dokan Hisab

An offline-first, bilingual (Bangla / English) shop accounting app for Android. Track daily income and expenses, view an Excel-style ledger, see monthly trends, and export reports.

## Features

- Daily income / expense entry with an inline calculator (`250+150`, `120*3`)
- Automatic category suggestions from description and past entries
- Ledger table with search, period and type filters, running balance
- Dashboard with daily, monthly and yearly summaries and a 6-month trend chart
- Monthly PDF report, CSV export, and database backup / restore
- Biometric app lock
- Optional assistant chat (bring your own Gemini or OpenRouter API key in Settings)
- Bangla and English UI, multiple colour palettes, light and dark themes

## Build

Requirements: Android Studio (recent stable) and JDK 11+.

1. Open the project folder in Android Studio and let Gradle sync.
2. Run the `app` configuration on an emulator or device, or build from the command line:

```
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

### Release signing

Release builds are signed only when these environment variables are set:

```
KEYSTORE_PATH=/path/to/release.jks
STORE_PASSWORD=...
KEY_ALIAS=...
KEY_PASSWORD=...
```

## Assistant API keys

API keys are entered by the user inside the app (Settings) and stored on the device. Nothing is bundled into the APK.
