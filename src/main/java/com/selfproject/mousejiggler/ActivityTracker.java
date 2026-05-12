package com.selfproject.mousejiggler;

import java.awt.Point;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

final class ActivityTracker {
    private final Clock clock;
    private Instant lastUserActivity;
    private Point lastObservedPoint;
    private Point lastAutopilotPoint;

    ActivityTracker() {
        this(Clock.systemUTC());
    }

    ActivityTracker(Clock clock) {
        this.clock = clock;
        this.lastUserActivity = Instant.now(clock);
    }

    boolean userIsIdle(Point currentPoint, Duration threshold) {
        if (lastAutopilotPoint != null && lastAutopilotPoint.equals(currentPoint)) {
            return isIdle(threshold);
        }

        if (lastObservedPoint == null || !lastObservedPoint.equals(currentPoint)) {
            lastUserActivity = Instant.now(clock);
            lastObservedPoint = new Point(currentPoint);
            lastAutopilotPoint = null;
        }
        return isIdle(threshold);
    }

    void recordAutopilotMove(Point point) {
        lastAutopilotPoint = new Point(point);
        lastObservedPoint = new Point(point);
    }

    private boolean isIdle(Duration threshold) {
        return !lastUserActivity.plus(threshold).isAfter(Instant.now(clock));
    }
}
