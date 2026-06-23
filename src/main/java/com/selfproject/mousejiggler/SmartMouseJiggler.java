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
    private static final int SMOOTH_STEPS = 5;   // intermediate positions per smooth move
    private static final int SMOOTH_STEP_MS = 12; // delay between each intermediate step

    private final MouseMover mouseMover;
    private final MouseClicker mouseClicker;
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

    SmartMouseJiggler(MouseMover mouseMover, KeyPresser keyPresser, MouseClicker mouseClicker, AppConfig config) {
        this.mouseMover = mouseMover;
        this.mouseClicker = mouseClicker;
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
                strategy::click,
                config
        );
        final InputStrategy finalStrategy = strategy;
        Runtime.getRuntime().addShutdownHook(new Thread(finalStrategy::close, "input-strategy-close"));

        jiggler.start();

        String keyboardStatus = config.keyboardEnabled()
                ? config.keyboardMode().name().toLowerCase() : "off";
        String jiggleStatus = config.keyboardJiggleEnabled()
                ? "on (" + config.keyboardJiggleMinSeconds() + "-" + config.keyboardJiggleMaxSeconds() + "s)" : "off";
        String clickStatus = config.mouseClickEnabled()
                ? "on (" + config.mouseClickMinSeconds() + "-" + config.mouseClickMaxSeconds() + "s)" : "off";
        System.out.printf(
                "Desktop Autopilot started.%n" +
                "  Profile=%s  input=%s  interval=%ds%n" +
                "  mouse=%s  smooth=%s  click=%s%n" +
                "  keyboard=%s  jiggle=%s%n" +
                "  human-pattern=%s  prevent-sleep=%s%n" +
                "Press Ctrl+C to stop.%n",
                config.profile(), config.inputMode().name().toLowerCase(), config.interval().toSeconds(),
                config.mouseEnabled() ? config.mode() : "off",
                config.mouseSmooth() ? "on" : "off", clickStatus,
                keyboardStatus, jiggleStatus,
                config.humanPattern() ? "on" : "off",
                config.preventSleep() ? "on" : "off");
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
            scheduleNextMouseTick(0);
        } else {
            executor.scheduleWithFixedDelay(this::jiggleSafely, 0, config.interval().toSeconds(), TimeUnit.SECONDS);
        }
        if (config.keyboardJiggleEnabled()) {
            scheduleNextKeyboardJiggle();
        }
        if (config.mouseClickEnabled()) {
            scheduleNextMouseClick();
        }
    }

    // ── Human-pattern mouse scheduling ──────────────────────────────────────

    private void scheduleNextMouseTick(long firstDelayMs) {
        long delayMs;
        if (firstDelayMs == 0) {
            delayMs = 0;
        } else {
            long baseMs = config.interval().toMillis();
            long variationMs = baseMs / 5;
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
            if (paused) { updateStatus("Paused"); return; }
            if (!schedule.isActive(LocalDateTime.now())) { updateStatus("Outside configured schedule"); return; }

            Point currentPoint = ScreenBounds.pointerLocation();
            if (config.idleOnly() && !activityTracker.userIsIdle(currentPoint, config.idleThreshold())) {
                updateStatus("Waiting for idle mouse");
                return;
            }

            // Human pattern: 10% chance to skip a tick (natural micro-pause)
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
            int jx = randomGenerator.nextInt(5) - 2;
            int jy = randomGenerator.nextInt(5) - 2;
            int tx = Math.max(safeBounds.x, Math.min(safeBounds.x + safeBounds.width - 1, target.x + jx));
            int ty = Math.max(safeBounds.y, Math.min(safeBounds.y + safeBounds.height - 1, target.y + jy));
            target = new Point(tx, ty);
        }

        if (config.mouseSmooth()) {
            smoothMove(currentPoint, target);
        } else {
            mouseMover.move(target);
        }
        return target;
    }

    /**
     * Animates movement from {@code from} to {@code to} via {@value SMOOTH_STEPS} intermediate
     * positions, pausing {@value SMOOTH_STEP_MS} ms between each step. This makes the cursor
     * visibly travel across the screen rather than teleporting, which looks far more natural
     * to any observer and to monitoring software that tracks cursor velocity.
     */
    private void smoothMove(Point from, Point to) {
        for (int i = 1; i <= SMOOTH_STEPS; i++) {
            int x = from.x + (to.x - from.x) * i / SMOOTH_STEPS;
            int y = from.y + (to.y - from.y) * i / SMOOTH_STEPS;
            mouseMover.move(new Point(x, y));
            if (i < SMOOTH_STEPS) {
                try {
                    Thread.sleep(SMOOTH_STEP_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    // ── Mouse click scheduling ───────────────────────────────────────────────

    private void scheduleNextMouseClick() {
        int min = config.mouseClickMinSeconds();
        int range = Math.max(1, config.mouseClickMaxSeconds() - min + 1);
        long delaySecs = min + randomGenerator.nextInt(range);
        executor.schedule(this::mouseClickSafely, delaySecs, TimeUnit.SECONDS);
    }

    private void mouseClickSafely() {
        try {
            if (!paused && schedule.isActive(LocalDateTime.now())) {
                Point currentPoint = ScreenBounds.pointerLocation();
                mouseClicker.click(currentPoint);
                if (config.logMoves()) {
                    System.out.printf("[%s] mouse click at (%d,%d)%n",
                            LocalTime.now().format(TIME_FORMAT), currentPoint.x, currentPoint.y);
                }
            }
        } catch (RuntimeException exception) {
            System.err.println("Mouse click skipped: " + exception.getMessage());
        } finally {
            scheduleNextMouseClick();
        }
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

    boolean isPaused() { return paused; }

    void setPaused(boolean paused) {
        this.paused = paused;
        updateStatus(paused ? "Paused" : "Active");
    }

    String status() { return lastStatus; }

    void stop() {
        if (executor != null) shutdown(executor);
        if (wakeLock != null) wakeLock.release();
    }

    private void updateStatus(String status) {
        lastStatus = status;
        if (trayController != null) trayController.update();
    }

    private static void logActivity(Point from, Point to, boolean keyboardEnabled) {
        String kb = keyboardEnabled ? ", pressed balanced arrow keys" : "";
        System.out.printf("[%s] moved mouse from (%d,%d) to (%d,%d)%s%n",
                LocalTime.now().format(TIME_FORMAT), from.x, from.y, to.x, to.y, kb);
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
