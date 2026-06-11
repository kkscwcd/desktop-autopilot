package com.selfproject.mousejiggler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Properties;

record AppConfig(
        boolean enabled,
        Duration interval,
        int pixels,
        MovementMode mode,
        boolean idleOnly,
        Duration idleThreshold,
        boolean scheduleEnabled,
        LocalTime scheduleStart,
        LocalTime scheduleEnd,
        boolean weekdaysOnly,
        boolean trayEnabled,
        boolean logMoves,
        boolean mouseEnabled,
        boolean keyboardEnabled,
        KeyboardMode keyboardMode,
        Profile profile,
        InputMode inputMode,
        boolean keyboardJiggleEnabled,
        int keyboardJiggleMinSeconds,
        int keyboardJiggleMaxSeconds,
        boolean humanPattern,
        boolean preventSleep,
        String jiggleKeys
) {
    private static final Path DEFAULT_CONFIG_PATH = Path.of("desktop-autopilot.properties");

    static AppConfig load(String[] args) {
        Properties properties = new Properties();

        Path configPath = configPath(args);
        if (Files.isRegularFile(configPath)) {
            try (InputStream inputStream = Files.newInputStream(configPath)) {
                properties.load(inputStream);
            } catch (IOException exception) {
                throw new IllegalArgumentException("Unable to read config file " + configPath + ": " + exception.getMessage(), exception);
            }
        }

        applyCommandLineOverrides(properties, args);

        Profile profile = Profile.parse(properties.getProperty("profile"));
        Properties merged = profileDefaults(profile).toProperties();
        merged.putAll(properties);

        return fromProperties(merged, profile);
    }

    private static AppConfig profileDefaults(Profile profile) {
        return switch (profile) {
            case MINIMAL -> new AppConfig(true, Duration.ofSeconds(6), 2, MovementMode.HORIZONTAL,
                    false, Duration.ofSeconds(60), false, LocalTime.of(9, 0), LocalTime.of(18, 0),
                    true, true, true, true, false, KeyboardMode.HORIZONTAL, profile, InputMode.AUTO,
                    false, 60, 90, false, false, "F15,SHIFT,UP,DOWN");
            case KEEP_AWAKE -> new AppConfig(true, Duration.ofSeconds(6), 2, MovementMode.DIAGONAL,
                    false, Duration.ofSeconds(60), true, LocalTime.of(9, 0), LocalTime.of(18, 0),
                    true, true, true, true, false, KeyboardMode.ALTERNATE, profile, InputMode.AUTO,
                    false, 60, 90, true, true, "F15,SHIFT,UP,DOWN");
            case STEALTH -> new AppConfig(true, Duration.ofSeconds(12), 2, MovementMode.RANDOM,
                    true, Duration.ofSeconds(45), true, LocalTime.of(9, 0), LocalTime.of(18, 0),
                    true, true, false, true, false, KeyboardMode.HORIZONTAL, profile, InputMode.AUTO,
                    false, 60, 90, true, true, "F15,SHIFT,UP,DOWN");
            case PRESENTATION -> new AppConfig(false, Duration.ofSeconds(6), 2, MovementMode.HORIZONTAL,
                    false, Duration.ofSeconds(60), false, LocalTime.of(9, 0), LocalTime.of(18, 0),
                    true, true, true, false, false, KeyboardMode.HORIZONTAL, profile, InputMode.AUTO,
                    false, 60, 90, false, false, "F15,SHIFT,UP,DOWN");
        };
    }

    private static AppConfig fromProperties(Properties properties, Profile profile) {
        return new AppConfig(
                bool(properties, "enabled"),
                Duration.ofSeconds(positiveInt(properties, "interval.seconds")),
                positiveInt(properties, "pixels"),
                MovementMode.parse(properties.getProperty("mode")),
                bool(properties, "idle.only"),
                Duration.ofSeconds(positiveInt(properties, "idle.seconds")),
                bool(properties, "schedule.enabled"),
                LocalTime.parse(properties.getProperty("schedule.start")),
                LocalTime.parse(properties.getProperty("schedule.end")),
                bool(properties, "schedule.weekdays.only"),
                bool(properties, "tray.enabled"),
                bool(properties, "log.moves"),
                bool(properties, "mouse.enabled"),
                bool(properties, "keyboard.enabled"),
                KeyboardMode.parse(properties.getProperty("keyboard.mode")),
                profile,
                InputMode.parse(properties.getProperty("input.mode")),
                bool(properties, "keyboard.jiggle.enabled"),
                positiveInt(properties, "keyboard.jiggle.min.seconds"),
                positiveInt(properties, "keyboard.jiggle.max.seconds"),
                bool(properties, "human.pattern"),
                bool(properties, "prevent.sleep"),
                properties.getProperty("jiggle.keys")
        );
    }

    private Properties toProperties() {
        Properties properties = new Properties();
        properties.setProperty("enabled", Boolean.toString(enabled));
        properties.setProperty("interval.seconds", Long.toString(interval.toSeconds()));
        properties.setProperty("pixels", Integer.toString(pixels));
        properties.setProperty("mode", mode.name().toLowerCase(Locale.ROOT));
        properties.setProperty("idle.only", Boolean.toString(idleOnly));
        properties.setProperty("idle.seconds", Long.toString(idleThreshold.toSeconds()));
        properties.setProperty("schedule.enabled", Boolean.toString(scheduleEnabled));
        properties.setProperty("schedule.start", scheduleStart.toString());
        properties.setProperty("schedule.end", scheduleEnd.toString());
        properties.setProperty("schedule.weekdays.only", Boolean.toString(weekdaysOnly));
        properties.setProperty("tray.enabled", Boolean.toString(trayEnabled));
        properties.setProperty("log.moves", Boolean.toString(logMoves));
        properties.setProperty("mouse.enabled", Boolean.toString(mouseEnabled));
        properties.setProperty("keyboard.enabled", Boolean.toString(keyboardEnabled));
        properties.setProperty("keyboard.mode", keyboardMode.name().toLowerCase(Locale.ROOT));
        properties.setProperty("profile", profile.name().toLowerCase(Locale.ROOT));
        properties.setProperty("input.mode", inputMode.name().toLowerCase(Locale.ROOT));
        properties.setProperty("keyboard.jiggle.enabled", Boolean.toString(keyboardJiggleEnabled));
        properties.setProperty("keyboard.jiggle.min.seconds", Integer.toString(keyboardJiggleMinSeconds));
        properties.setProperty("keyboard.jiggle.max.seconds", Integer.toString(keyboardJiggleMaxSeconds));
        properties.setProperty("human.pattern", Boolean.toString(humanPattern));
        properties.setProperty("prevent.sleep", Boolean.toString(preventSleep));
        properties.setProperty("jiggle.keys", jiggleKeys != null ? jiggleKeys : "F15,SHIFT,UP,DOWN");
        return properties;
    }

    private static Path configPath(String[] args) {
        for (int index = 0; index < args.length - 1; index++) {
            if ("--config".equals(args[index])) {
                return Path.of(args[index + 1]);
            }
        }
        return DEFAULT_CONFIG_PATH;
    }

    private static void applyCommandLineOverrides(Properties properties, String[] args) {
        for (int index = 0; index < args.length; index++) {
            String arg = args[index];
            switch (arg) {
                case "--help" -> {
                    printHelpAndExit();
                    return;
                }
                case "--config" -> index++;
                case "--profile" -> properties.setProperty("profile", value(args, ++index, arg));
                case "--interval" -> properties.setProperty("interval.seconds", value(args, ++index, arg));
                case "--pixels" -> properties.setProperty("pixels", value(args, ++index, arg));
                case "--mode" -> properties.setProperty("mode", value(args, ++index, arg));
                case "--idle-only" -> properties.setProperty("idle.only", "true");
                case "--idle-seconds" -> properties.setProperty("idle.seconds", value(args, ++index, arg));
                case "--schedule" -> properties.setProperty("schedule.enabled", "true");
                case "--start" -> properties.setProperty("schedule.start", value(args, ++index, arg));
                case "--end" -> properties.setProperty("schedule.end", value(args, ++index, arg));
                case "--weekdays-only" -> properties.setProperty("schedule.weekdays.only", "true");
                case "--all-days" -> properties.setProperty("schedule.weekdays.only", "false");
                case "--no-tray" -> properties.setProperty("tray.enabled", "false");
                case "--quiet" -> properties.setProperty("log.moves", "false");
                case "--disabled" -> properties.setProperty("enabled", "false");
                case "--no-mouse" -> properties.setProperty("mouse.enabled", "false");
                case "--keyboard" -> properties.setProperty("keyboard.enabled", "true");
                case "--keyboard-mode" -> properties.setProperty("keyboard.mode", value(args, ++index, arg));
                case "--input-mode" -> properties.setProperty("input.mode", value(args, ++index, arg));
                case "--keyboard-jiggle" -> properties.setProperty("keyboard.jiggle.enabled", "true");
                case "--jiggle-min" -> properties.setProperty("keyboard.jiggle.min.seconds", value(args, ++index, arg));
                case "--jiggle-max" -> properties.setProperty("keyboard.jiggle.max.seconds", value(args, ++index, arg));
                case "--jiggle-keys" -> properties.setProperty("jiggle.keys", value(args, ++index, arg));
                case "--human-pattern" -> properties.setProperty("human.pattern", "true");
                case "--prevent-sleep" -> properties.setProperty("prevent.sleep", "true");
                case "--no-prevent-sleep" -> properties.setProperty("prevent.sleep", "false");
                default -> throw new IllegalArgumentException("Unknown option: " + arg);
            }
        }
    }

    private static String value(String[] args, int index, String option) {
        if (index >= args.length || args[index].startsWith("--")) {
            throw new IllegalArgumentException(option + " requires a value");
        }
        return args[index];
    }

    private static boolean bool(Properties properties, String key) {
        return Boolean.parseBoolean(properties.getProperty(key));
    }

    private static int positiveInt(Properties properties, String key) {
        int value = Integer.parseInt(properties.getProperty(key));
        if (value < 1) {
            throw new IllegalArgumentException(key + " must be at least 1");
        }
        return value;
    }

    private static void printHelpAndExit() {
        System.out.println("""
                Desktop Autopilot options:
                  --profile minimal|keep-awake|stealth|presentation
                  --interval seconds
                  --pixels pixels
                  --mode horizontal|vertical|diagonal|random
                  --idle-only --idle-seconds seconds
                  --schedule --start HH:mm --end HH:mm --weekdays-only|--all-days
                  --keyboard --keyboard-mode horizontal|vertical|alternate --no-mouse
                  --input-mode auto|robot|native
                  --keyboard-jiggle [--jiggle-min seconds] [--jiggle-max seconds] [--jiggle-keys F15,SHIFT,UP,DOWN]
                  --human-pattern
                  --prevent-sleep | --no-prevent-sleep
                  --no-tray --quiet --disabled
                  --config path/to/desktop-autopilot.properties
                """);
        System.exit(0);
    }
}
