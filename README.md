# Desktop Autopilot

A Java 21 desktop utility that keeps your computer active using hardware-level mouse and keyboard simulation. Moves undetectably — events appear to the OS as if they originated from physical hardware (HID subsystem), so presence indicators in Teams, Zoom, and Slack stay green.

Movement is bounded to the current screen's safe area. When the cursor is near an edge the app reverses direction automatically.

## Features

- **Hardware-level input** — macOS: `CGEventPost(kCGHIDEventTap)` via JNA; Windows: `SendInput()` via JNA; falls back to `java.awt.Robot`
- **Smooth mouse movement** — cursor glides through 5 intermediate positions instead of teleporting
- **Mouse click** — periodic left-click at a random interval to keep chat apps active
- **Keyboard jiggle** — presses a random key (F15, Shift, Up, Down) at a random interval; F15 does nothing visible in any app
- **Human-pattern simulation** — ±20% interval variation, 50–150% step size, ±2 px micro-jitter, 10% random skip
- **System wake lock** — prevents display sleep (macOS `IOPMAssertion`; Windows `SetThreadExecutionState`)
- **Profiles** — `minimal`, `keep-awake`, `stealth`, `presentation`
- **Movement modes** — `horizontal`, `vertical`, `diagonal`, `random`
- **Working-hours schedule** — start/end time with optional weekday-only filter
- **Idle-only mode** — waits for the user to stop moving the mouse before acting
- **System tray** — Pause / Resume / Status / Quit
- **Config file** — `desktop-autopilot.properties` loaded from the working directory

---

## Quick Start

### macOS

```sh
./run.sh
```

### Windows

```bat
run.bat
```

Both scripts build the fat JAR automatically on first run if it is missing.

> **macOS**: grant Accessibility / Input Monitoring permission to the JVM in  
> System Settings → Privacy & Security → Accessibility.

---

## All Options

| Flag | Argument | Default | Description |
|---|---|---|---|
| `--profile` | `minimal` `keep-awake` `stealth` `presentation` | `minimal` | Load a preset profile (see Profiles below) |
| `--input-mode` | `auto` `native` `robot` | `auto` | Input strategy. `native` = hardware-level; `robot` = software fallback |
| `--interval` | seconds | `6` | How often to jiggle the mouse |
| `--pixels` | px | `2` | Mouse step size in pixels |
| `--mode` | `horizontal` `vertical` `diagonal` `random` | `horizontal` | Movement direction |
| `--smooth` | — | off | Animate cursor through 5 positions (~60 ms) instead of teleporting |
| `--mouse-click` | — | off | Enable periodic left-click |
| `--click-min` | seconds | `120` | Minimum interval between clicks |
| `--click-max` | seconds | `300` | Maximum interval between clicks |
| `--keyboard-jiggle` | — | off | Enable random key presses from the jiggle key pool |
| `--jiggle-min` | seconds | `60` | Minimum interval between key presses |
| `--jiggle-max` | seconds | `90` | Maximum interval between key presses |
| `--jiggle-keys` | `F15,SHIFT,UP,DOWN` | `F15,SHIFT,UP,DOWN` | Comma-separated key pool (F15, SPACE, SHIFT, UP, DOWN, LEFT, RIGHT) |
| `--human-pattern` | — | off | Vary interval, step size, add micro-jitter, occasionally skip ticks |
| `--prevent-sleep` | — | off | Prevent display/system sleep via OS wake lock |
| `--no-prevent-sleep` | — | — | Explicitly disable sleep prevention |
| `--idle-only` | — | off | Only act when user mouse is idle |
| `--idle-seconds` | seconds | `60` | Idle threshold before acting |
| `--schedule` | — | off | Restrict to a working-hours window |
| `--start` | `HH:mm` | `09:00` | Schedule window start time |
| `--end` | `HH:mm` | `18:00` | Schedule window end time |
| `--weekdays-only` | — | on | Skip weekends |
| `--all-days` | — | — | Include weekends |
| `--keyboard` | — | off | Balanced arrow-key nudging to the focused app |
| `--keyboard-mode` | `horizontal` `vertical` `alternate` | `horizontal` | Arrow-key mode for `--keyboard` |
| `--no-mouse` | — | — | Disable mouse movement (use with `--keyboard`) |
| `--no-tray` | — | — | Disable system tray icon |
| `--quiet` | — | — | Suppress move/click/key log output |
| `--disabled` | — | — | Start without acting (useful with tray for manual control) |
| `--config` | path | `desktop-autopilot.properties` | Custom config file path |

---

## Profiles

| Profile | What changes from defaults |
|---|---|
| `minimal` | Mouse only, no sleep prevention, no human pattern |
| `keep-awake` | Schedule on, diagonal moves, human-pattern on, prevent-sleep on, smooth on |
| `stealth` | Idle-only, random moves, human-pattern on, prevent-sleep on, smooth on, logs off |
| `presentation` | Mouse and keyboard disabled — tray only |

---

## Example Commands

### macOS — `./run.sh [options]`
### Windows — `run.bat [options]`

```sh
# Default — minimal mouse jiggle, 2px every 6s
./run.sh

# Force hardware-level input explicitly
./run.sh --input-mode native

# Recommended: hardware input + keyboard jiggle + all evasion features
./run.sh --input-mode native --keyboard-jiggle --human-pattern --prevent-sleep --smooth --mouse-click

# Profile shortcuts (keep-awake and stealth include human-pattern + prevent-sleep + smooth)
./run.sh --profile keep-awake
./run.sh --profile stealth
./run.sh --profile presentation

# Smooth movement (cursor glides instead of teleporting)
./run.sh --smooth

# Mouse click every 2–5 minutes
./run.sh --mouse-click
./run.sh --mouse-click --click-min 60 --click-max 120

# Keyboard jiggle (random key press every 60–90 s)
./run.sh --keyboard-jiggle
./run.sh --keyboard-jiggle --jiggle-min 45 --jiggle-max 75
./run.sh --keyboard-jiggle --jiggle-keys F15,SHIFT

# Faster/slower movement
./run.sh --interval 10 --pixels 5 --mode diagonal
./run.sh --interval 3 --pixels 1 --mode random

# Only act when user is idle (mouse not moved for 60 s)
./run.sh --idle-only
./run.sh --idle-only --idle-seconds 30

# Working-hours schedule only (09:00–18:00 weekdays)
./run.sh --schedule
./run.sh --schedule --start 08:30 --end 17:30
./run.sh --schedule --start 09:00 --end 18:00 --all-days

# Prevent screen sleep without any other features
./run.sh --prevent-sleep

# Human-pattern simulation only
./run.sh --human-pattern

# Keyboard arrow-key nudging (sends keys to focused app — keep editor focused)
./run.sh --keyboard
./run.sh --keyboard --keyboard-mode alternate
./run.sh --keyboard --keyboard-mode horizontal --no-mouse

# Run with a custom config file
./run.sh --config ~/my-autopilot.properties

# Quiet mode (no logging, just runs silently)
./run.sh --quiet

# Show all options
./run.sh --help
```

---

## Config File

Copy the example and edit it:

```sh
cp desktop-autopilot.properties.example desktop-autopilot.properties
```

The app loads `desktop-autopilot.properties` automatically from the working directory. CLI flags override any property in the file.

Key properties:

```properties
input.mode=native
mouse.smooth=true
mouse.click.enabled=true
mouse.click.min.seconds=120
mouse.click.max.seconds=300
keyboard.jiggle.enabled=true
keyboard.jiggle.min.seconds=60
keyboard.jiggle.max.seconds=90
jiggle.keys=F15,SHIFT,UP,DOWN
human.pattern=true
prevent.sleep=true
```

---

## Build

Requires Java 21 and Maven 3.9+.

```sh
# macOS / Linux
mvn clean package

# Or use the Maven wrapper (auto-detected by run.sh / run.bat)
~/.m2/wrapper/dists/apache-maven-*/*/bin/mvn clean package
```

The fat JAR is written to `target/mouse-jiggler-1.0.0.jar`.

## Test

```sh
mvn test
```

---

## Permissions

**macOS** — grant Accessibility permission to the JVM (or your terminal app):  
System Settings → Privacy & Security → Accessibility → add Java / your terminal.

Without this, native input silently falls back to `java.awt.Robot` (software-tagged events).

**Windows** — no special permissions required. `SendInput()` works from any normal user process.
