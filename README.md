# Paper-dialog (Paper 1.21.7 Dialog API)

[![Build](https://github.com/RomeoXLover/Paper-dialog/actions/workflows/build.yml/badge.svg)](https://github.com/RomeoXLover/Paper-dialog/actions/workflows/build.yml)

A minimal Minecraft Paper plugin demonstrating and testing the official **Paper 1.21.7 Dialog API** on **Java 21**.

---

## Features

- **Native Dialog API**: Uses real server-driven modal forms (`io.papermc.paper.dialog.Dialog`, `DialogBase`, `ActionButton`, `MultiActionType`, and `Player#showDialog`).
- **Fully Configurable**: All dialog properties (Title, Body lines, Button text, Widths, Chat messages, and Console logs) can be edited live via `config.yml` or directly in code.
- **Adventure MiniMessage Support**: Format colors and styles easily using tags like `<gold>`, `<bold>`, `<green>`, `<aqua>`, `<light_purple>`, etc.
- **CI Verified**: Automated GitHub Actions workflow compiling with Java 21 and Gradle 8.5.

---

## What Breaks (Real-World Quirks & Technical Gotchas)

These are the exact runtime behaviors, memory limits, and build issues observed when compiling and running this plugin:

### 1. Callback Lifetime & Expiration
- **The Issue**: Action button callbacks registered via `DialogAction.customClick(callback, options)` use `ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(5)).build()`.
- **What Breaks**:
  - If a player opens the dialog with `/task` and leaves it open on their screen for more than 5 minutes before clicking, the callback silently expires in Adventure's callback registry. Clicking any button results in a dead click with no chat message and no server log.
  - The `uses(1)` limit ensures single execution per button instance. If the client sends duplicate packets or tries to reuse the button, it will not fire a second time.

### 2. Server Restarts & Reloads Drop Callbacks
- **The Issue**: Inlined dialogs constructed via `Dialog.create(...)` register ephemeral custom click listeners in the server JVM's heap memory.
- **What Breaks**:
  - Callbacks do not survive a server restart or `/reload`. If a player is looking at the `/task` modal when the server restarts or reloads, the screen remains visible on the client, but clicking any button does nothing because the corresponding callback reference in memory was destroyed.
  - The dialog must be dismissed (Esc) and reopened with `/task` to establish fresh callbacks.

### 3. Gradle Build Daemon Metaspace Exhaustion
- **The Issue Hit During Build**: When running Gradle 8.5 with Java 21 against `paper-api:1.21.7-R0.1-SNAPSHOT`, the default Gradle daemon heap and Metaspace (`-XX:MaxMetaspaceSize=384m`) were overwhelmed.
- **What Broke**: The daemon entered a severe GC thrashing state at 97.4% Metaspace utilization, taking over 10 minutes to resolve dependencies and compile classes.
- **The Fix**: `gradle.properties` must explicitly define:
  ```properties
  org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=1024m
  org.gradle.daemon=false
  ```

### 4. Toolchain Discovery Latency
- **The Issue Hit During Build**: Using Gradle's `java.toolchain` configuration caused the compiler to hang while scanning common Linux directory trees (`/usr/lib/jvm/openjdk-21`, `/usr/lib/jvm/openjdk-17`) for installed toolchains.
- **The Fix**: Removed toolchain auto-discovery from `build.gradle` and replaced it with direct compiler targets:
  ```groovy
  java {
      sourceCompatibility = JavaVersion.VERSION_21
      targetCompatibility = JavaVersion.VERSION_21
  }
  tasks.withType(JavaCompile).configureEach {
      options.encoding = 'UTF-8'
      options.release = 21
  }
  ```

### 5. Single-Player Pause vs Dedicated Server
- **The Issue**: `DialogBase.builder().pause(true)` is designed to pause single-player worlds when opening menus.
- **What Breaks**: On a dedicated Paper multiplayer server, the server tick loop never pauses. Entities will continue moving and hostile mobs can still attack the player while the dialog is active. We explicitly set `.pause(false)` to match server reality.

### 6. Escape Key vs Cancel Events
- **The Issue**: `canCloseWithEscape(true)` allows dismissing the dialog with Esc.
- **What Breaks**: Pressing Escape does not trigger an action button or emit a cancellation callback unless an explicit `exitAction` is registered on `MultiActionType`. The dialog simply disappears client-side.

---

## How to Customize the Dialog

### Option A: Edit via `config.yml` (Recommended — No Recompilation Required)

Once installed, the plugin generates `plugins/PaperDialogTest/config.yml`. You can modify every aspect of the dialog directly:

```yaml
dialog:
  # Title displayed in the modal header
  title: "<gold><bold>Task Selection</bold></gold>"

  # Exactly two body lines displayed above the action buttons
  body:
    line1: "Please select an operational task to perform:"
    line2: "Clicking any button will execute the selected action."

  # Action buttons configuration (up to 3 buttons)
  buttons:
    button1:
      # Label on the button
      label: "<green>Task Alpha</green>"
      # Width of the button (1 to 1024)
      width: 150
      # Chat message sent to the clicking player
      message: "<green>[Task] You executed Task Alpha!</green>"
      # Text logged to server console
      console_log: "clicked: Task Alpha"

    button2:
      label: "<aqua>Task Beta</aqua>"
      width: 150
      message: "<aqua>[Task] You executed Task Beta!</aqua>"
      console_log: "clicked: Task Beta"

    button3:
      label: "<light_purple>Task Gamma</light_purple>"
      width: 150
      message: "<light_purple>[Task] You executed Task Gamma!</light_purple>"
      console_log: "clicked: Task Gamma"
```

Restart the server or reload plugins to apply config changes.

---

### Option B: Edit Directly in Java Source Code

If you prefer hardcoding or adding more complex logic, the dialog definition is located in:
[`src/main/java/com/example/dialogtest/TaskCommand.java`](src/main/java/com/example/dialogtest/TaskCommand.java)

#### 1. Changing Title & Body Text
In `openTaskDialog(Player player)`:
```java
final DialogBase base = DialogBase.builder(Component.text("Your Custom Title", NamedTextColor.GOLD))
        .canCloseWithEscape(true)
        .pause(false)
        .afterAction(DialogBase.DialogAfterAction.CLOSE)
        .body(List.of(
                DialogBody.plainMessage(Component.text("First line of body text")),
                DialogBody.plainMessage(Component.text("Second line of body text"))
        ))
        .inputs(List.of())
        .build();
```

#### 2. Changing Action Buttons
```java
final ActionButton button1 = ActionButton.builder(Component.text("Custom Button 1", NamedTextColor.GREEN))
        .width(180)
        .action(DialogAction.customClick((response, audience) -> {
            audience.sendMessage(Component.text("Custom response to chat!"));
            plugin.getLogger().info(player.getName() + " clicked Custom Button 1");
        }, callbackOptions))
        .build();
```

#### 3. Changing Layout Columns
```java
// 1 = vertical stack, 2 or 3 = grid layout
final MultiActionType type = DialogType.multiAction(List.of(button1, button2, button3))
        .columns(1)
        .build();
```

---

## Project Structure

```text
Paper-dialog/
├── .github/
│   └── workflows/
│       └── build.yml                     # GitHub Actions CI build workflow
├── build.gradle                          # Dependencies and compiler options
├── settings.gradle                       # Project name
├── gradle.properties                     # JVM & memory flags for compiler
├── build.sh                              # Automated build script
├── gradlew                               # Unix Gradle wrapper executable
├── gradlew.bat                           # Windows Gradle wrapper script
├── README.md                             # Documentation & customization guide
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar            # Gradle wrapper binary
│       └── gradle-wrapper.properties     # Gradle 8.5 distribution spec
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── example/
        │           └── dialogtest/
        │               ├── DialogTestPlugin.java   # Main plugin entrypoint
        │               └── TaskCommand.java        # /task command & Dialog API builder
        └── resources/
            ├── plugin.yml                # Plugin metadata & command registration
            └── config.yml                # Default editable configuration
```

---

## How to Build

From the project root:

```bash
chmod +x build.sh
./build.sh
```

Or using Gradle directly:
```bash
./gradlew build
```

Compiled JAR output:
```text
build/libs/paper-dialog-test-1.0.0-SNAPSHOT.jar
```

---

## How to Install and Test

1. Put `paper-dialog-test-1.0.0-SNAPSHOT.jar` into your Paper 1.21.7 server's `plugins/` directory.
2. Start the server. Confirm `[PaperDialogTest] PaperDialogTest plugin enabled successfully!` in logs.
3. In-game, run `/task`.
4. A native Dialog API window opens with your title, 2 lines of text, and 3 buttons.
5. Click any button to receive the configured chat message and verify console output.
