# Paper-dialog (Paper 1.21.7 Dialog API)

A minimal Minecraft Paper plugin demonstrating and testing the official **Paper 1.21.7 Dialog API** on **Java 21**.

---

## Features

- **Native Dialog API**: Uses real server-driven modal forms (`io.papermc.paper.dialog.Dialog`, `DialogBase`, `ActionButton`, `MultiActionType`, and `Player#showDialog`).
- **Fully Configurable**: All dialog properties (Title, Body lines, Button text, Widths, Chat messages, and Console logs) can be edited live via `config.yml` or directly in code.
- **Adventure MiniMessage Support**: Format colors and styles easily using tags like `<gold>`, `<bold>`, `<green>`, `<aqua>`, `<light_purple>`, etc.
- **Turnkey Build**: Includes complete Gradle 8.5 wrapper (`gradlew`), build files, and a one-step `build.sh` script.

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

After modifying `config.yml`, restart the server or reload plugins.

---

### Option B: Edit Directly in Java Source Code

If you prefer hardcoding or adding more complex logic, the dialog definition is located in:
[`src/main/java/com/example/dialogtest/TaskCommand.java`](src/main/java/com/example/dialogtest/TaskCommand.java)

#### 1. Changing the Title & Body Text
In `openTaskDialog(Player player)` around line 50:
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

#### 2. Changing the Action Buttons
In `openTaskDialog(Player player)` around line 68:
```java
final ActionButton button1 = ActionButton.builder(Component.text("Custom Button 1", NamedTextColor.GREEN))
        .width(180) // Set custom width
        .action(DialogAction.customClick((response, audience) -> {
            audience.sendMessage(Component.text("Custom response to chat!"));
            plugin.getLogger().info(player.getName() + " clicked Custom Button 1");
        }, callbackOptions))
        .build();
```

#### 3. Changing Layout and Number of Columns
In `openTaskDialog(Player player)` around line 112:
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

Compiled JAR will be placed at:
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
