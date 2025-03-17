# Contacts Plugin Tests

This directory contains unit tests for the Contacts plugin. These tests are designed to verify the functionality of the plugin's components without requiring an Android device or emulator.

## Running the Tests

### From Android Studio

1. Open the project in Android Studio
2. Right-click on the `app/src/test` directory
3. Select "Run Tests in 'test'"

Alternatively, you can run individual test classes by right-clicking on the class file and selecting "Run".

### From Command Line

To run all unit tests:

```bash
./gradlew test
```

To run a specific test class:

```bash
./gradlew test --tests "com.atakmap.android.contacts.plugin.model.ContactTest"
```

To run a specific test method:

```bash
./gradlew test --tests "com.atakmap.android.contacts.plugin.model.ContactTest.testContactCreation"
```

## Test Coverage

The unit tests cover the following components:

- `Contact` model class
- `ContactAdapter` for displaying contacts
- `DatabaseHelper` for database operations
- `ContactsNativeLoader` for loading native libraries

## Instrumentation Tests

Instrumentation tests are located in `app/src/androidTest`. These tests require an Android device or emulator to run.

To run all instrumentation tests:

```bash
./gradlew connectedAndroidTest
```

The instrumentation tests cover:

- `ContactManager` for managing contacts
- `Contacts` main plugin class

## Test Reports

After running the tests, you can find the test reports in:

- Unit tests: `app/build/reports/tests/testDebugUnitTest/index.html`
- Instrumentation tests: `app/build/reports/androidTests/connected/index.html` 