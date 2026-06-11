# AGENTS.md

## Project Context

This repository is `desktop-autopilot`, a Java 21 Maven desktop utility.

Main goal: keep the desktop active with safe, configurable automation while avoiding risky default behavior.

GitHub remote:

```sh
https://github.com/kkscwcd/desktop-autopilot.git
```

## Current Features

- Safe mouse movement with pluggable input strategy (native or Robot)
- Movement is clamped to the current screen safe bounds
- Movement modes:
  - `horizontal`
  - `vertical`
  - `diagonal`
  - `random`
- Input modes (`--input-mode`):
  - `auto` — tries native first, falls back to Robot with a warning (default)
  - `native` — macOS: `CGEventPost(kCGHIDEventTap, ...)` via JNA; Windows: `SendInput()` via JNA
  - `robot` — original `java.awt.Robot` (software-tagged, cross-platform fallback)
- Profiles:
  - `minimal`
  - `keep-awake`
  - `stealth`
  - `presentation`
- System tray controls:
  - Pause
  - Resume
  - Status
  - Quit
- CLI configuration
- `desktop-autopilot.properties` configuration file support
- Example config file: `desktop-autopilot.properties.example`
- Optional idle-only mode based on mouse movement
- Optional working-hours schedule with weekday filtering
- Optional keyboard arrow-key nudging for the focused app

## Important Defaults

Keyboard mode must stay disabled by default.

Current intended defaults:

```properties
mouse.enabled=true
keyboard.enabled=false
```

Keyboard mode should only turn on when the user explicitly passes `--keyboard` or sets:

```properties
keyboard.enabled=true
```

Reason: Java sends keys to whichever application is currently focused. Without OS-specific native integrations, the app cannot reliably know whether a text editor or IDE is active.

## Build And Test

Use Maven.

```sh
mvn test
mvn package
```

Expected current test count:

```text
16 tests
```

Run the app:

```sh
mvn exec:java
```

Run the jar:

```sh
java -jar target/mouse-jiggler-1.0.0.jar
```

Show CLI options:

```sh
java -jar target/mouse-jiggler-1.0.0.jar --help
```

## Common Run Examples

```sh
mvn exec:java -Dexec.args="--profile stealth"
mvn exec:java -Dexec.args="--interval 10 --pixels 3 --mode diagonal"
mvn exec:java -Dexec.args="--idle-only --idle-seconds 60"
mvn exec:java -Dexec.args="--schedule --start 09:00 --end 18:00 --weekdays-only"
mvn exec:java -Dexec.args="--keyboard --keyboard-mode alternate"
mvn exec:java -Dexec.args="--keyboard --keyboard-mode horizontal --no-mouse"

# Native hardware-level input (macOS / Windows)
mvn exec:java -Dexec.args="--input-mode native"
java -jar target/mouse-jiggler-1.0.0.jar --input-mode native
java -jar target/mouse-jiggler-1.0.0.jar --input-mode robot   # force software fallback
```

## Key Classes

- `SmartMouseJiggler`: main runtime orchestration
- `AppConfig`: CLI and properties config parsing
- `SafeMouseMovement`: screen-safe mouse target calculation
- `ScreenBounds`: current pointer screen safe bounds
- `ActivityTracker`: idle-only behavior based on mouse movement
- `Schedule`: working-hours logic
- `TrayController`: system tray controls
- `KeyboardNudge`: balanced arrow-key nudging
- `MovementMode`: mouse movement modes
- `KeyboardMode`: arrow-key modes
- `Profile`: profile defaults
- `InputMode`: enum — `AUTO`, `ROBOT`, `NATIVE`
- `InputStrategy`: interface — `moveMouse(Point)`, `pressKey(int javaKeyCode)`, `close()`
- `RobotInputStrategy`: `java.awt.Robot` implementation (cross-platform fallback)
- `MacOSInputStrategy`: CoreGraphics JNA implementation — `CGEventPost(kCGHIDEventTap, ...)`
- `WindowsInputStrategy`: User32 JNA implementation — `SendInput()`
- `InputStrategyFactory`: selects the right strategy based on `InputMode` and current OS

## Engineering Notes

- The project now depends on `jna-platform 5.14.0` for native input. This is intentional.
- The fat-jar is built with `maven-shade-plugin` — JNA native libraries are bundled inside the JAR.
- `InputStrategy` is the extension point for new platforms; `RobotInputStrategy` is always the fallback.
- `MacOSInputStrategy` uses `kCGHIDEventTap` (value `0`) so events appear from the HID subsystem, not a Java process. Requires Accessibility permission granted to the JVM in *System Preferences → Privacy & Security → Accessibility*.
- `WindowsInputStrategy` uses `SendInput()` directly. Arrow-key VK codes are identical between Java and Win32, so no mapping is needed. Mouse coordinates are normalized to 0-65535 for `MOUSEEVENTF_ABSOLUTE`.
- `InputMode.AUTO` is the default — tries native, prints a warning and falls back to Robot if native fails (missing permission, wrong OS, etc.).
- Do not add installers yet; the user explicitly said installers are not needed now.
- Keep keyboard automation opt-in and document the focused-app limitation.
- Add or update tests when changing config parsing, scheduling, movement, idle tracking, or keyboard nudging.
- Avoid committing `target/`, `.DS_Store`, or local config files.

## Last Known Remote State

Latest pushed commit after smart controls:

```text
7421905 Add smart autopilot controls
```

Local unpushed changes (not yet merged to remote):
- Native hardware-level input via JNA (`MacOSInputStrategy`, `WindowsInputStrategy`)
- `InputStrategy` abstraction + `InputStrategyFactory`
- `InputMode` config field (`auto` / `robot` / `native`)
- Fat-jar build via `maven-shade-plugin`
