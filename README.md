# Desktop Autopilot

Small Java 21 desktop utility that nudges the mouse safely. By default it moves 2 pixels every 6 seconds.

The movement is bounded to the current screen's safe area. When the pointer is near an edge, the app reverses direction and clamps the target point so the cursor is not moved outside the visible desktop area.

## Features

- System tray controls: Pause, Resume, Status, Quit
- Profiles: `minimal`, `keep-awake`, `stealth`, `presentation`
- Movement modes: `horizontal`, `vertical`, `diagonal`, `random`
- Configurable interval and pixel step
- Optional idle-only mode based on mouse activity
- Optional working-hours schedule with weekday filtering
- Optional balanced arrow-key nudging for the focused editor/IDE
- Safe screen-boundary clamping

## Build

```sh
mvn clean package
```

## Run

```sh
mvn exec:java
```

Or run the packaged jar:

```sh
java -jar target/mouse-jiggler-1.0.0.jar
```

Press `Ctrl+C` to stop.

On macOS, Java may need Accessibility/Input Monitoring permission before it can move the mouse.

## Examples

```sh
mvn exec:java -Dexec.args="--profile stealth"
mvn exec:java -Dexec.args="--interval 10 --pixels 3 --mode diagonal"
mvn exec:java -Dexec.args="--idle-only --idle-seconds 60"
mvn exec:java -Dexec.args="--schedule --start 09:00 --end 18:00 --weekdays-only"
mvn exec:java -Dexec.args="--keyboard --keyboard-mode alternate"
mvn exec:java -Dexec.args="--keyboard --keyboard-mode horizontal --no-mouse"
```

Keyboard mode sends balanced arrow-key pairs to the currently focused app:

- `horizontal`: Right, then Left
- `vertical`: Down, then Up
- `alternate`: switches between horizontal and vertical pairs

Keep your editor or IDE focused when using keyboard mode. Plain Java cannot reliably detect the active foreground application without OS-specific native integrations.

## Config File

Copy `desktop-autopilot.properties.example` to `desktop-autopilot.properties` and edit it. The app loads that file automatically from the working directory.

You can also pass a custom path:

```sh
mvn exec:java -Dexec.args="--config /path/to/desktop-autopilot.properties"
```

## Test

```sh
mvn test
```
