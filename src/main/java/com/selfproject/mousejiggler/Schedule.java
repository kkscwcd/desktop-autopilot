package com.selfproject.mousejiggler;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

final class Schedule {
    private final AppConfig config;

    Schedule(AppConfig config) {
        this.config = config;
    }

    boolean isActive(LocalDateTime now) {
        if (!config.enabled()) {
            return false;
        }
        if (!config.scheduleEnabled()) {
            return true;
        }
        if (config.weekdaysOnly()) {
            DayOfWeek day = now.getDayOfWeek();
            if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
                return false;
            }
        }

        LocalTime current = now.toLocalTime();
        LocalTime start = config.scheduleStart();
        LocalTime end = config.scheduleEnd();
        if (start.equals(end)) {
            return true;
        }
        if (start.isBefore(end)) {
            return !current.isBefore(start) && current.isBefore(end);
        }
        return !current.isBefore(start) || current.isBefore(end);
    }
}
