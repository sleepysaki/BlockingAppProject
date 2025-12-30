This is a Kotlin Multiplatform project targeting Android, Web.

* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./composeApp/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
    folder is the appropriate location.

### Build and Run Android Application

To build and run the development version of the Android app, use the run configuration from the run widget
in your IDE’s toolbar or build it directly from the terminal:
- on macOS/Linux
  ```shell
  ./gradlew :composeApp:assembleDebug
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:assembleDebug
  ```

### Build and Run Web Application

To build and run the development version of the web app, use the run configuration from the run widget
in your IDE's toolbar or run it directly from the terminal:
- for the Wasm target (faster, modern browsers):
  - on macOS/Linux
    ```shell
    ./gradlew :composeApp:wasmJsBrowserDevelopmentRun
    ```
  - on Windows
    ```shell
    .\gradlew.bat :composeApp:wasmJsBrowserDevelopmentRun
    ```
- for the JS target (slower, supports older browsers):
  - on macOS/Linux
    ```shell
    ./gradlew :composeApp:jsBrowserDevelopmentRun
    ```
  - on Windows
    ```shell
    .\gradlew.bat :composeApp:jsBrowserDevelopmentRun
    ```

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform),
[Kotlin/Wasm](https://kotl.in/wasm/)…

We would appreciate your feedback on Compose/Web and Kotlin/Wasm in the public Slack channel [#compose-web](https://slack-chats.kotlinlang.org/c/compose-web).
If you face any issues, please report them on [YouTrack](https://youtrack.jetbrains.com/newIssue?project=CMP).




---

1. Core Architecture & Frameworks
Kotlin & Jetpack Compose: Used as the primary programming language and modern UI toolkit for building a reactive, state-driven user interface.

MVVM Architecture (Model-View-ViewModel): Ensures a clean separation of concerns between business logic (ViewModels) and UI components (Composables).

Kotlin Coroutines & Flow: Managed asynchronous tasks, such as background location updates and app list retrieval, ensuring smooth main-thread performance.

2. Advanced Blocking Mechanisms
Android Accessibility Service: The core engine of the app. It monitors system-level events (TYPE_WINDOW_STATE_CHANGED) to detect which application is moving to the foreground.

Global Action Execution: When a violation (time or location) is detected, the service triggers GLOBAL_ACTION_HOME to immediately minimize the restricted app.

UsageStatsManager: Leveraged to query the system for precise, real-time foreground usage data of any specific package.

3. Geofencing & Location Awareness
Google Fused Location Provider API: Integrated for high-accuracy location tracking with low power consumption.

Haversine/Spherical Distance Calculation: Utilized Location.distanceBetween to calculate the proximity between the user's current coordinates and the defined "Target Zone".

Real-time Geofencing Logic: Implemented a background callback loop that updates the isInsideTargetZone state every 5 seconds.

4. Data Storage & Persistence Strategy
SharedPreferences: Used for fast, lightweight persistent storage of user-defined limits (limit_pkg) and schedule timestamps (sched_from/to).

Singleton Pattern (FakeLocalDatabase): Centralized the management of the "Blocked App List" to ensure consistency between the UI settings and the background service.

5. Security & System Integration
Dynamic Whitelisting: Hardcoded a protection layer to exclude critical system packages (Dialer, Settings, SystemUI) and the app itself from being blocked.

Usage Access & Accessibility Permissions: Implemented high-level system permission requests to allow the app to "read" other app's statuses.

Package Manager Integration: Extracted application metadata (icons and labels) directly from the Android system to populate the control dashboard.
