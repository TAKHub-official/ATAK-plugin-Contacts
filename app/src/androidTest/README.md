# Contacts Plugin Instrumentation Tests

This directory contains instrumentation tests for the Contacts plugin. These tests require an Android device or emulator to run, as they test the integration of the plugin with the Android system.

## Running the Tests

### From Android Studio

1. Open the project in Android Studio
2. Connect an Android device or start an emulator
3. Right-click on the `app/src/androidTest` directory
4. Select "Run Tests in 'androidTest'"

Alternatively, you can run individual test classes by right-clicking on the class file and selecting "Run".

### From Command Line

To run all instrumentation tests:

```bash
./gradlew connectedAndroidTest
```

To run a specific test class:

```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.atakmap.android.contacts.plugin.ContactManagerTest
```

## Test Coverage

The instrumentation tests cover the following components:

- `ContactManager` for managing contacts
- `Contacts` main plugin class

These tests verify that the plugin integrates correctly with the Android system and ATAK.

## Test Reports

After running the tests, you can find the test reports in:

- `app/build/reports/androidTests/connected/index.html`

## Troubleshooting

If you encounter issues running the instrumentation tests:

1. Make sure you have a device or emulator connected
2. Check that the device has ATAK installed
3. Ensure the device has the necessary permissions granted
4. Check the logcat output for any errors

## Adding New Tests

When adding new instrumentation tests:

1. Create a new test class in the appropriate package
2. Add the `@RunWith(AndroidJUnit4.class)` annotation
3. Add the test to the `ContactsInstrumentationTestSuite` class 