# IBAN Registry

An offline-first Android app for registering IBAN bank accounts with a description.

## Features

- Add, edit, and delete bank accounts
- Search by description or IBAN
- Organize accounts with folders and tags
- Rearrange accounts in a custom order
- Export and import versioned JSON backups
- Copy an IBAN to the clipboard
- Validate country-specific IBAN lengths and ISO 13616 modulo-97 checksums
- Reject duplicate IBANs after normalizing case and whitespace
- Store data privately on the device with Room

## Requirements

- Android Studio with JDK 17
- Android SDK 35
- An emulator or device running Android 8.0 (API 26) or later

## Run

1. Open this directory in Android Studio.
2. Allow Gradle sync to finish.
3. Select the `app` run configuration and an Android device.
4. Run the app.

From a terminal with `JAVA_HOME` and the Android SDK configured:

```powershell
.\gradlew.bat test lint assembleDebug
```

## Architecture

- `domain` contains the account model, IBAN rules, and repository contract.
- `data/local` contains the Room entity, DAO, and database.
- `data/repository` adapts local storage to the domain repository.
- `ui` contains state-hoisted Compose screens and ViewModels.
- `AppContainer` owns application-scoped dependencies without coupling screens to Room.

The UI depends only on `BankAccountRepository`. A future synchronized implementation can
combine local and remote data sources behind that interface while retaining Room as the
offline source of truth.

## Data and privacy

Accounts are stored only in the app-private Room database. The app does not request network
permissions, contact banks, read balances, or initiate payments. Full IBAN values are not
logged. Android backup is disabled so stored IBANs are not copied into device backups. See
[PRIVACY.md](PRIVACY.md) for the complete privacy policy.

Manual backups use Android's document picker and contain IBANs, descriptions, folders, tags,
and list order as readable JSON. Store exported files somewhere private.

## F-Droid publishing

This project contains only FOSS dependencies, is licensed under the
[MIT License](LICENSE), requests no network access, and includes localized store metadata in
`fastlane/metadata/android/en-US`.

Before submitting the app to the official F-Droid repository:

1. Publish this project in a public Git repository.
2. Replace the placeholder repository URLs and commit in
   `docs/fdroiddata-template.yml`.
3. Tag each release (the current release is `v1.0.1`); every future release must increment
   `versionCode` and have a corresponding changelog file.
4. Copy the template to `metadata/io.github.remmie75.ibanregistry.yml` in a fork of
   `fdroiddata`, run `fdroid lint`, and submit it as a merge request.

F-Droid builds and signs its own APK from the tagged source. Verify the same
unsigned release locally with:

```powershell
.\gradlew.bat clean test lint assembleRelease
```
