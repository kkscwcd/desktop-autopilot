package com.selfproject.mousejiggler;

import java.awt.event.KeyEvent;
import java.util.random.RandomGenerator;

/**
 * Presses a randomly chosen key from a configurable pool to simulate natural
 * keyboard activity. Fired on its own random interval, independently of the
 * mouse jiggle cycle.
 *
 * F15 is the recommended default: it is a real key that exists on older Apple
 * keyboards and does nothing visible in any application, making it the cleanest
 * choice for undetectable activity simulation.
 */
final class KeyboardJiggle {

    /** Sentinel for F15 — not a standard Java VK code; each InputStrategy maps it to the real platform key. */
    static final int VK_F15_NATIVE = -15;

    static final int[] DEFAULT_KEY_POOL = {
        VK_F15_NATIVE,        // F15 — no visible effect in any app (cleanest)
        KeyEvent.VK_SHIFT,    // Shift alone is safe; no character typed
        KeyEvent.VK_UP,
        KeyEvent.VK_DOWN,
    };

    private final KeyPresser keyPresser;
    private final int[] keyPool;

    KeyboardJiggle(KeyPresser keyPresser, int[] keyPool) {
        this.keyPresser = keyPresser;
        this.keyPool = (keyPool != null && keyPool.length > 0) ? keyPool : DEFAULT_KEY_POOL;
    }

    int pressRandom(RandomGenerator random) {
        int keyCode = keyPool[random.nextInt(keyPool.length)];
        keyPresser.press(keyCode);
        return keyCode;
    }

    /** Parses a comma-separated key name list, e.g. {@code "F15,SHIFT,UP,DOWN"}. */
    static int[] parseKeyPool(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_KEY_POOL;
        }
        String[] parts = value.split(",");
        int[] codes = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            codes[i] = parseKeyName(parts[i].trim());
        }
        return codes;
    }

    private static int parseKeyName(String name) {
        return switch (name.toUpperCase()) {
            case "F15"    -> VK_F15_NATIVE;
            case "SPACE"  -> KeyEvent.VK_SPACE;
            case "SHIFT"  -> KeyEvent.VK_SHIFT;
            case "UP"     -> KeyEvent.VK_UP;
            case "DOWN"   -> KeyEvent.VK_DOWN;
            case "LEFT"   -> KeyEvent.VK_LEFT;
            case "RIGHT"  -> KeyEvent.VK_RIGHT;
            default -> throw new IllegalArgumentException("Unknown key name: '" + name
                    + "'. Supported: F15, SPACE, SHIFT, UP, DOWN, LEFT, RIGHT");
        };
    }
}
