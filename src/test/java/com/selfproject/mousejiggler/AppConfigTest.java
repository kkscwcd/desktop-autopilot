package com.selfproject.mousejiggler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AppConfigTest {
    @Test
    void parsesCommandLineOverrides() {
        AppConfig config = AppConfig.load(new String[]{
                "--profile", "stealth",
                "--interval", "10",
                "--pixels", "4",
                "--mode", "vertical",
                "--idle-only",
                "--schedule",
                "--start", "08:30",
                "--end", "17:45",
                "--all-days",
                "--no-tray",
                "--quiet",
                "--keyboard",
                "--keyboard-mode", "alternate",
                "--no-mouse"
        });

        assertEquals(Profile.STEALTH, config.profile());
        assertEquals(10, config.interval().toSeconds());
        assertEquals(4, config.pixels());
        assertEquals(MovementMode.VERTICAL, config.mode());
        assertTrue(config.idleOnly());
        assertTrue(config.scheduleEnabled());
        assertFalse(config.weekdaysOnly());
        assertFalse(config.trayEnabled());
        assertFalse(config.logMoves());
        assertTrue(config.keyboardEnabled());
        assertEquals(KeyboardMode.ALTERNATE, config.keyboardMode());
        assertFalse(config.mouseEnabled());
    }

    @Test
    void appliesProfileDefaultsWhenNotOverridden() {
        AppConfig config = AppConfig.load(new String[]{"--profile", "stealth"});

        assertEquals(Profile.STEALTH, config.profile());
        assertEquals(12, config.interval().toSeconds());
        assertEquals(MovementMode.RANDOM, config.mode());
        assertTrue(config.idleOnly());
        assertFalse(config.logMoves());
    }
}
