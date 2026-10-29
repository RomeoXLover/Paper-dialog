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

## What Breaks

Here is exactly what breaks, how the API behaves at runtime, and what was actually hit during development:

### 1. Callback Expiration (`uses(1)` and 5-Minute Lifetime)
- The button action uses:
  ```java
  ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(5)).build()
  ```
- **What breaks**: If a player runs `/task` and leaves the dialog window open on their screen for more than 5 minutes before clicking, Adventure's internal callback registry silently drops the listener. When the player finally clicks a button, nothing happens—no chat message, no console log, no error.
- Because `uses(1)` is set, each button only works once per modal presentation. Duplicate clicks or client replay packets fail silently.

### 2. Dialogs Do Not Survive a Restart or Reload
- Inlined custom click handlers constructed with `DialogAction.customClick(...)` live exclusively in the server's heap memory.
- **What breaks**: If the server restarts or runs `/reload` while a player has the dialog open, the client continues to display the UI, but every button is completely dead. The callback closure on the server is gone. The player must press `Esc` to close the dead modal and run `/task` again to get fresh callbacks.

### 3. What Was Actually Hit During the Build (Real Dev Log)
- **First attempt did not build clean first try**:
  1. **Gradle Metaspace Exhaustion**: On Java 21, resolving `io.papermc.paper:paper-api:1.21.7-R0.1-SNAPSHOT` exceeded Gradle's default 384MB Metaspace (`-XX:MaxMetaspaceSize=384m`). The build hung in severe GC thrashing at 97.4% Metaspace usage. It required adding `gradle.properties` with:
     ```properties
     org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=1024m
     org.gradle.daemon=false
     ```
  2. **Toolchain Auto-Discovery Stall**: Using `java.toolchain { languageVersion = JavaLanguageVersion.of(21) }` stalled the build indefinitely while scanning system Linux directories (`/usr/lib/jvm/...`). We stripped the toolchain resolver and pinned explicit compiler targets:
     ```groovy
     tasks.withType(JavaCompile).configureEach {
         options.release = 21
     }
     ```
  3. **Multiplayer Pause Illusion**: Calling `.pause(true)` on `DialogBase` only freezes single-player worlds. On a live dedicated server, world ticks and hostile mob AI continue ticking while the player reads the dialog. We set `.pause(false)` to prevent any false assumptions about player invulnerability.

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
