package com.selfproject.mousejiggler;

import java.awt.AWTException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class SmartMouseJiggler {
    private static final Duration INTERVAL = Duration.ofSeconds(6);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final MouseMover mouseMover;
    private int direction = 1;

    SmartMouseJiggler(MouseMover mouseMover) {
        this.mouseMover = mouseMover;
    }

    public static void main(String[] args) {
        Robot robot;
        try {
            robot = new Robot();
        } catch (AWTException | SecurityException exception) {
            System.err.println("Unable to control the mouse. Allow Java accessibility/input control and run again.");
            System.err.println(exception.getMessage());
            return;
        }

        SmartMouseJiggler jiggler = new SmartMouseJiggler(point -> robot.mouseMove(point.x, point.y));
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "smart-mouse-jiggler");
            thread.setDaemon(false);
            return thread;
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown(executor), "mouse-jiggler-shutdown"));
        executor.scheduleWithFixedDelay(jiggler::jiggleSafely, 0, INTERVAL.toSeconds(), TimeUnit.SECONDS);
        System.out.println("Smart mouse jiggler started. Moving 2 pixels every 6 seconds. Press Ctrl+C to stop.");
    }

    void jiggleSafely() {
        try {
            Point currentPoint = ScreenBounds.pointerLocation();
            Rectangle safeBounds = ScreenBounds.safeBoundsForPointer();
            MovementPlan plan = SafeMouseMovement.next(currentPoint, safeBounds, direction);
            direction = plan.nextDirection();
            mouseMover.move(plan.nextPoint());
            logMove(currentPoint, plan.nextPoint());
        } catch (RuntimeException exception) {
            System.err.println("Mouse jiggle skipped safely: " + exception.getMessage());
        }
    }

    private static void logMove(Point from, Point to) {
        System.out.printf("[%s] moved mouse from (%d,%d) to (%d,%d)%n",
                LocalTime.now().format(TIME_FORMAT), from.x, from.y, to.x, to.y);
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
