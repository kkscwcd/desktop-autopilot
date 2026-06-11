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
- Movement clamped to current screen safe bounds
- Movement modes: `horizontal`, `vertical`, `diagonal`, `random`
- Input modes (`--input-mode`):
  - `auto` — tries native first, falls back to Robot with a warning (default)
  - `native` — macOS: `CGEventPost(kCGHIDEventTap, ...)` via JNA; Windows: `SendInput()` via JNA
  - `robot` — `java.awt.Robot` (software-tagged, cross-platform fallback)
- Human-pattern simulation (`--human-pattern`): ±20% interval variation, 50–150% step size, ±2px micro-jitter, 10% random skip per tick
- System wake lock (`--prevent-sleep`): macOS `IOPMAssertion` (caffeinate equivalent), Windows `SetThreadExecutionState`
- Profiles: `minimal`, `keep-awake` (human+sleep on), `stealth` (human+sleep on), `presentation`
- System tray: Pause / Resume / Status / Quit
- CLI + `desktop-autopilot.properties` file configuration
- Idle-only mode (waits for user to stop moving mouse before jiggling)
- Working-hours schedule with weekday filtering
- Optional keyboard arrow-key nudging (`--keyboard`, off by default)
- Hardware keyboard jiggle (`--keyboard-jiggle`): presses a random key from a configurable pool at a random 60–90 s interval, independent of the mouse cycle
  - Default key pool: `F15, SHIFT, UP, DOWN` — F15 is the cleanest (does nothing visible in any app)
  - Configurable via `--jiggle-keys F15,SHIFT,UP,DOWN` or `jiggle.keys=` in config

## Important Defaults

Keyboard nudge (`--keyboard`) must stay **disabled** by default — it sends keys to whatever app is focused.

```properties
mouse.enabled=true
keyboard.enabled=false
keyboard.jiggle.enabled=false
human.pattern=false
prevent.sleep=false
```

`keep-awake` and `stealth` profiles override `human.pattern=true` and `prevent.sleep=true` automatically.

## Build And Test

Maven is in `~/.m2/wrapper/dists/`. Use the full path if `mvn` is not on PATH:

```sh
~/.m2/wrapper/dists/apache-maven-3.9.12-bin/*/bin/mvn package
~/.m2/wrapper/dists/apache-maven-3.9.12-bin/*/bin/mvn test
```

Or use the launcher (builds if JAR is missing):

```sh
./run.sh
```

Expected test count: **16 tests**

## Common Run Examples

```sh
# Recommended demo command — all new features active
./run.sh --input-mode native --keyboard-jiggle --human-pattern --prevent-sleep

# Profiles
./run.sh --profile stealth          # human-pattern + prevent-sleep on by default
./run.sh --profile keep-awake

# Mouse only
./run.sh --interval 10 --pixels 3 --mode diagonal
./run.sh --idle-only --idle-seconds 60

# Schedule
./run.sh --schedule --start 09:00 --end 18:00 --weekdays-only

# Keyboard nudge (arrow keys to focused app — use carefully)
./run.sh --keyboard --keyboard-mode alternate
./run.sh --keyboard --keyboard-mode horizontal --no-mouse

# Keyboard jiggle
./run.sh --keyboard-jiggle
./run.sh --keyboard-jiggle --jiggle-min 60 --jiggle-max 90
./run.sh --keyboard-jiggle --jiggle-keys F15,SHIFT

# Input mode
./run.sh --input-mode native
./run.sh --input-mode robot         # force software fallback

# Human pattern + wake lock standalone
./run.sh --human-pattern --prevent-sleep
```

## Key Classes

| Class | Role |
|---|---|
| `SmartMouseJiggler` | Main runtime orchestration, scheduler, human-pattern logic |
| `AppConfig` | CLI + properties file parsing (23 config fields) |
| `SafeMouseMovement` | Screen-safe mouse target calculation |
| `ScreenBounds` | Current pointer screen safe bounds |
| `ActivityTracker` | Idle-only behaviour via position polling |
| `Schedule` | Working-hours window logic |
| `TrayController` | System tray icon + menu |
| `KeyboardNudge` | Balanced arrow-key nudging (existing `--keyboard` feature) |
| `KeyboardJiggle` | Random key from configurable pool; F15 sentinel `VK_F15_NATIVE = -15` |
| `MovementMode` | Mouse movement mode enum |
| `KeyboardMode` | Arrow-key mode enum |
| `InputMode` | Enum: `AUTO`, `ROBOT`, `NATIVE` |
| `InputStrategy` | Interface: `moveMouse(Point)`, `pressKey(int)`, `close()` |
| `RobotInputStrategy` | `java.awt.Robot` fallback |
| `MacOSInputStrategy` | CoreGraphics JNA — `CGEventPost(kCGHIDEventTap, ...)` |
| `WindowsInputStrategy` | User32 JNA — `SendInput()` with virtual desktop coords |
| `InputStrategyFactory` | Auto-selects strategy by OS + InputMode |
| `WakeLock` | Interface: `acquire()`, `release()` |
| `MacOSWakeLock` | IOKit JNA — `IOPMAssertionCreateWithName("PreventUserIdleDisplaySleep")` |
| `WindowsWakeLock` | Kernel32 JNA — `SetThreadExecutionState(ES_CONTINUOUS|ES_DISPLAY_REQUIRED)` |
| `WakeLockFactory` | Auto-selects wake lock by OS; no-op if `prevent.sleep=false` |
| `Profile` | Profile enum + default config values |

## Engineering Notes

- **Dependency**: `jna-platform 5.14.0` — intentional. Do not remove.
- **Fat JAR**: built with `maven-shade-plugin`. Signature files (`*.SF`, `*.DSA`, `*.RSA`) are stripped to prevent `SecurityException` when signed JNA jars are merged.
- **`InputStrategy` extension point**: add new platforms by implementing `InputStrategy`; `RobotInputStrategy` is always the fallback.
- **macOS keyboard fix**: `CGEventCreateKeyboardEvent` uses `CGEventSourceCreate(kCGEventSourceStateHIDSystemState)` as source (null source silently fails on macOS 12+). The `keyDown` parameter is `byte`, not `boolean` — JNA marshals `boolean` as 4-byte int; CoreGraphics expects 1-byte C `bool`.
- **macOS CGKeyCode mappings**: Space=49, Shift=56, Left=123, Right=124, Down=125, Up=126, F15=113.
- **Windows coordinate normalisation**: mouse coords normalised to 0–65535 across the virtual desktop using `SM_XVIRTUALSCREEN`/`SM_CXVIRTUALSCREEN`; falls back to primary monitor if virtual metrics are zero.
- **F15 sentinel**: `KeyboardJiggle.VK_F15_NATIVE = -15`. Each strategy maps it to the platform key code (macOS CGKeyCode 113, Windows 0x7E, Java `KeyEvent.VK_F15`).
- **Human-pattern variable interval**: when `human.pattern=true`, mouse jiggle switches from `scheduleWithFixedDelay` to self-rescheduling (`scheduleNextMouseTick`) with ±20% random variation per tick.
- **Wake lock lifecycle**: `acquire()` in `start()`, `release()` in `stop()` (called by shutdown hook).
- Do not add installers; not needed.
- Keep keyboard nudge opt-in; document focused-app limitation.
- Add or update tests when changing config parsing, scheduling, movement, idle tracking, or keyboard nudging.
- Avoid committing `target/`, `.DS_Store`, or local config files.

## AppConfig Fields (23 total)

| Property | CLI flag | Default |
|---|---|---|
| `enabled` | `--disabled` | `true` |
| `interval.seconds` | `--interval N` | `6` |
| `pixels` | `--pixels N` | `2` |
| `mode` | `--mode horizontal\|...` | `horizontal` |
| `idle.only` | `--idle-only` | `false` |
| `idle.seconds` | `--idle-seconds N` | `60` |
| `schedule.enabled` | `--schedule` | `false` |
| `schedule.start` | `--start HH:mm` | `09:00` |
| `schedule.end` | `--end HH:mm` | `18:00` |
| `schedule.weekdays.only` | `--weekdays-only` | `true` |
| `tray.enabled` | `--no-tray` | `true` |
| `log.moves` | `--quiet` | `true` |
| `mouse.enabled` | `--no-mouse` | `true` |
| `keyboard.enabled` | `--keyboard` | `false` |
| `keyboard.mode` | `--keyboard-mode horizontal\|...` | `horizontal` |
| `profile` | `--profile minimal\|...` | `minimal` |
| `input.mode` | `--input-mode auto\|robot\|native` | `auto` |
| `keyboard.jiggle.enabled` | `--keyboard-jiggle` | `false` |
| `keyboard.jiggle.min.seconds` | `--jiggle-min N` | `60` |
| `keyboard.jiggle.max.seconds` | `--jiggle-max N` | `90` |
| `human.pattern` | `--human-pattern` | `false` (true in keep-awake/stealth) |
| `prevent.sleep` | `--prevent-sleep` / `--no-prevent-sleep` | `false` (true in keep-awake/stealth) |
| `jiggle.keys` | `--jiggle-keys F15,SHIFT,...` | `F15,SHIFT,UP,DOWN` |

## Last Known Remote State

Latest pushed commit:

```text
7421905 Add smart autopilot controls
```

Local unpushed changes (not yet pushed to remote):
- Native hardware-level input via JNA (`MacOSInputStrategy`, `WindowsInputStrategy`)
- `InputStrategy` abstraction + `InputStrategyFactory` + `InputMode`
- Hardware keyboard jiggle (`KeyboardJiggle`) with configurable key pool + F15 support
- Human-pattern simulation (variable interval, step size, micro-jitter, random skip)
- System wake lock (`MacOSWakeLock`, `WindowsWakeLock`, `WakeLockFactory`)
- Fat JAR via `maven-shade-plugin` with signature stripping
- `run.sh` terminal launcher (auto-builds if JAR missing)
- Startup log shows all active features explicitly
