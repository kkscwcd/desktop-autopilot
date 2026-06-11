package com.selfproject.mousejiggler;

import java.awt.Point;
import java.awt.Rectangle;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.random.RandomGenerator;

public final class SmartMouseJiggler {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final MouseMover mouseMover;
    private final AppConfig config;
    private final Schedule schedule;
    private final ActivityTracker activityTracker;
    private final KeyboardNudge keyboardNudge;
    private final KeyboardJiggle keyboardJiggle;
    private final RandomGenerator randomGenerator;
    private ScheduledExecutorService executor;
    private TrayController trayController;
    private WakeLock wakeLock;
    private volatile boolean paused;
    private volatile String lastStatus = "Starting";
    private int xDirection = 1;
    private int yDirection = 1;

    SmartMouseJiggler(MouseMover mouseMover, KeyPresser keyPresser, AppConfig config) {
        this.mouseMover = mouseMover;
        this.config = config;
        this.schedule = new Schedule(config);
        this.activityTracker = new ActivityTracker();
        this.keyboardNudge = new KeyboardNudge(keyPresser);
        this.keyboardJiggle = new KeyboardJiggle(keyPresser, KeyboardJiggle.parseKeyPool(config.jiggleKeys()));
        this.randomGenerator = RandomGenerator.getDefault();
    }

    public static void main(String[] args) {
        AppConfig config;
        try {
            config = AppConfig.load(args);
        } catch (RuntimeException exception) {
            System.err.println("Invalid configuration: " + exception.getMessage());
            System.err.println("Run with --help for available options.");
            return;
        }

        InputStrategy strategy;
        try {
            strategy = InputStrategyFactory.create(config.inputMode());
        } catch (Exception | UnsatisfiedLinkError exception) {
            System.err.println("Unable to initialise input strategy: " + exception.getMessage());
            System.err.println("Grant Accessibility permission to the JVM and run again.");
            return;
        }

        SmartMouseJiggler jiggler = new SmartMouseJiggler(
                strategy::moveMouse,
                strategy::pressKey,
                config
        );
        final InputStrategy finalStrategy = strategy;
        Runtime.getRuntime().addShutdownHook(new Thread(finalStrategy::close, "input-strategy-close"));

        jiggler.start();

        String keyboardStatus = config.keyboardEnabled()
                ? config.keyboardMode().name().toLowerCase()
                : "off";
        String jiggleStatus = config.keyboardJiggleEnabled()
                ? "on (" + config.keyboardJiggleMinSeconds() + "-" + config.keyboardJiggleMaxSeconds() + "s random)"
                : "off";
        System.out.printf(
                "Desktop Autopilot started. Profile=%s, input=%s, mouse=%s, keyboard=%s," +
                " keyboard-jiggle=%s, human-pattern=%s, prevent-sleep=%s, interval=%ds.%n" +
                "Press Ctrl+C to stop.%n",
                config.profile(), config.inputMode().name().toLowerCase(),
                config.mouseEnabled() ? config.mode() : "off",
                keyboardStatus, jiggleStatus,
                config.humanPattern() ? "on" : "off",
                config.preventSleep() ? "on" : "off",
                config.interval().toSeconds());
    }

    void start() {
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "smart-mouse-jiggler");
            thread.setDaemon(false);
            return thread;
        });

        trayController = TrayController.install(this, config);
        wakeLock = WakeLockFactory.create(config.preventSleep());
        wakeLock.acquire();

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "mouse-jiggler-shutdown"));

        if (config.humanPattern()) {
            // Variable interval: self-reschedule after each tick
            scheduleNextMouseTick(0);
        } else {
            executor.scheduleWithFixedDelay(this::jiggleSafely, 0, config.interval().toSeconds(), TimeUnit.SECONDS);
        }

        if (config.keyboardJiggleEnabled()) {
            scheduleNextKeyboardJiggle();
        }
    }

    // ── Human-pattern mouse scheduling ──────────────────────────────────────

    private void scheduleNextMouseTick(long firstDelayMs) {
        long delayMs;
        if (firstDelayMs == 0) {
            delayMs = 0; // fire immediately on first run
        } else {
            long baseMs = config.interval().toMillis();
            long variationMs = baseMs / 5; // ±20% of base interval
            delayMs = baseMs - variationMs + randomGenerator.nextLong(variationMs * 2 + 1);
        }
        executor.schedule(this::humanJiggleSafely, Math.max(0, delayMs), TimeUnit.MILLISECONDS);
    }

    private void humanJiggleSafely() {
        try {
            jiggleSafely();
        } finally {
            scheduleNextMouseTick(config.interval().toMillis());
        }
    }

    // ── Core jiggle tick ────────────────────────────────────────────────────

    void jiggleSafely() {
        try {
            if (paused) {
                updateStatus("Paused");
                return;
            }
            if (!schedule.isActive(LocalDateTime.now())) {
                updateStatus("Outside configured schedule");
                return;
            }

            Point currentPoint = ScreenBounds.pointerLocation();
            if (config.idleOnly() && !activityTracker.userIsIdle(currentPoint, config.idleThreshold())) {
                updateStatus("Waiting for idle mouse");
                return;
            }

            // Human pattern: 10% chance to skip this tick (simulates a natural micro-pause)
            if (config.humanPattern() && randomGenerator.nextInt(10) == 0) {
                updateStatus("Active");
                return;
            }

            Point movedPoint = currentPoint;
            if (config.mouseEnabled()) {
                movedPoint = moveMouse(currentPoint);
            }
            if (config.keyboardEnabled()) {
                keyboardNudge.pressBalancedArrowKeys(config.keyboardMode());
            }
            activityTracker.recordAutopilotMove(movedPoint);
            updateStatus("Active");
            if (config.logMoves()) {
                logActivity(currentPoint, movedPoint, config.keyboardEnabled());
            }
        } catch (RuntimeException exception) {
            updateStatus("Skipped safely: " + exception.getMessage());
            System.err.println("Autopilot action skipped safely: " + exception.getMessage());
        }
    }

    private Point moveMouse(Point currentPoint) {
        Rectangle safeBounds = ScreenBounds.safeBoundsForPointer();

        // Human pattern: vary step size 50%–150% of configured pixels
        int stepPixels = config.pixels();
        if (config.humanPattern()) {
            stepPixels = Math.max(1, (int) (stepPixels * (0.5 + randomGenerator.nextDouble())));
        }

        MovementPlan plan = SafeMouseMovement.next(
                currentPoint, safeBounds, stepPixels, config.mode(), xDirection, yDirection, randomGenerator
        );
        xDirection = plan.nextXDirection();
        yDirection = plan.nextYDirection();

        Point target = plan.nextPoint();

        // Human pattern: micro-jitter ±2px on final target
        if (config.humanPattern() && safeBounds != null && !safeBounds.isEmpty()) {
            int jx = randomGenerator.nextInt(5) - 2; // -2 to +2
            int jy = randomGenerator.nextInt(5) - 2;
            int tx = Math.max(safeBounds.x, Math.min(safeBounds.x + safeBounds.width - 1, target.x + jx));
            int ty = Math.max(safeBounds.y, Math.min(safeBounds.y + safeBounds.height - 1, target.y + jy));
            target = new Point(tx, ty);
        }

        mouseMover.move(target);
        return target;
    }

    // ── Keyboard jiggle scheduling ──────────────────────────────────────────

    private void scheduleNextKeyboardJiggle() {
        int min = config.keyboardJiggleMinSeconds();
        int range = Math.max(1, config.keyboardJiggleMaxSeconds() - min + 1);
        long delaySecs = min + randomGenerator.nextInt(range);
        executor.schedule(this::keyboardJiggleSafely, delaySecs, TimeUnit.SECONDS);
    }

    private void keyboardJiggleSafely() {
        try {
            if (!paused && schedule.isActive(LocalDateTime.now())) {
                int keyCode = keyboardJiggle.pressRandom(randomGenerator);
                if (config.logMoves()) {
                    String keyName = keyCode == KeyboardJiggle.VK_F15_NATIVE ? "F15"
                            : String.format("0x%02X", keyCode);
                    System.out.printf("[%s] keyboard jiggle key=%s%n",
                            LocalTime.now().format(TIME_FORMAT), keyName);
                }
            }
        } catch (RuntimeException exception) {
            System.err.println("Keyboard jiggle skipped: " + exception.getMessage());
        } finally {
            scheduleNextKeyboardJiggle();
        }
    }

    // ── Lifecycle ───────────────────────────────────────────────────────────

    boolean isPaused() {
        return paused;
    }

    void setPaused(boolean paused) {
        this.paused = paused;
        updateStatus(paused ? "Paused" : "Active");
    }

    String status() {
        return lastStatus;
    }

    void stop() {
        if (executor != null) {
            shutdown(executor);
        }
        if (wakeLock != null) {
            wakeLock.release();
        }
    }

    private void updateStatus(String status) {
        lastStatus = status;
        if (trayController != null) {
            trayController.update();
        }
    }

    private static void logActivity(Point from, Point to, boolean keyboardEnabled) {
        String keyboardStatus = keyboardEnabled ? ", pressed balanced arrow keys" : "";
        System.out.printf("[%s] moved mouse from (%d,%d) to (%d,%d)%s%n",
                LocalTime.now().format(TIME_FORMAT), from.x, from.y, to.x, to.y, keyboardStatus);
    }

    private static void shutdown(ScheduledExecutorService executor) {
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                System.err.println("Mouse jiggler did not stop cleanly before timeout.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
