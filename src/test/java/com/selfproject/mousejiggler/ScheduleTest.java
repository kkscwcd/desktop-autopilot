package com.selfproject.mousejiggler;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ScheduleTest {
    @Test
    void inactiveOutsideDaytimeSchedule() {
        Schedule schedule = new Schedule(config(true, LocalTime.of(9, 0), LocalTime.of(18, 0), false));

        assertTrue(schedule.isActive(LocalDateTime.of(2026, 5, 12, 9, 0)));
        assertFalse(schedule.isActive(LocalDateTime.of(2026, 5, 12, 18, 0)));
    }

    @Test
    void supportsOvernightSchedule() {
        Schedule schedule = new Schedule(config(true, LocalTime.of(22, 0), LocalTime.of(6, 0), false));

        assertTrue(schedule.isActive(LocalDateTime.of(2026, 5, 12, 23, 0)));
        assertTrue(schedule.isActive(LocalDateTime.of(2026, 5, 13, 5, 59)));
        assertFalse(schedule.isActive(LocalDateTime.of(2026, 5, 13, 12, 0)));
    }

    @Test
    void weekdaysOnlySkipsWeekendWhenScheduleIsEnabled() {
        Schedule schedule = new Schedule(config(true, LocalTime.of(9, 0), LocalTime.of(18, 0), true));

        assertFalse(schedule.isActive(LocalDateTime.of(2026, 5, 16, 10, 0)));
    }

    private static AppConfig config(boolean scheduleEnabled, LocalTime start, LocalTime end, boolean weekdaysOnly) {
        return new AppConfig(true, Duration.ofSeconds(6), 2, MovementMode.HORIZONTAL,
                false, Duration.ofSeconds(60), scheduleEnabled, start, end, weekdaysOnly,
                false, true, true, false, KeyboardMode.HORIZONTAL, Profile.MINIMAL);
    }
}
