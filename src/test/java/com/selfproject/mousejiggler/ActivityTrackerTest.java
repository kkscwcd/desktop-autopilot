package com.selfproject.mousejiggler;

import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ActivityTrackerTest {
    @Test
    void unchangedMouseBecomesIdleAfterThreshold() {
        MutableClock clock = new MutableClock();
        ActivityTracker tracker = new ActivityTracker(clock);

        assertFalse(tracker.userIsIdle(new Point(10, 10), Duration.ofSeconds(30)));
        clock.advance(Duration.ofSeconds(31));

        assertTrue(tracker.userIsIdle(new Point(10, 10), Duration.ofSeconds(30)));
    }

    @Test
    void userMouseMoveResetsIdleTimer() {
        MutableClock clock = new MutableClock();
        ActivityTracker tracker = new ActivityTracker(clock);

        tracker.userIsIdle(new Point(10, 10), Duration.ofSeconds(30));
        clock.advance(Duration.ofSeconds(31));
        assertTrue(tracker.userIsIdle(new Point(10, 10), Duration.ofSeconds(30)));

        assertFalse(tracker.userIsIdle(new Point(11, 10), Duration.ofSeconds(30)));
    }

    private static final class MutableClock extends Clock {
        private Instant instant = Instant.parse("2026-05-12T00:00:00Z");

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
