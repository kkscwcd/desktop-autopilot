package com.selfproject.mousejiggler;

import java.awt.event.KeyEvent;
import java.util.random.RandomGenerator;

/**
 * Presses a randomly chosen key from a realistic pool (Space, Up, Down, Shift)
 * to simulate natural keyboard activity. Fired on its own random interval,
 * independently of the mouse jiggle cycle.
 */
final class KeyboardJiggle {

    private static final int[] KEY_POOL = {
        KeyEvent.VK_SPACE,
        KeyEvent.VK_UP,
        KeyEvent.VK_DOWN,
        KeyEvent.VK_SHIFT
    };

    private final KeyPresser keyPresser;

    KeyboardJiggle(KeyPresser keyPresser) {
        this.keyPresser = keyPresser;
    }

    int pressRandom(RandomGenerator random) {
        int keyCode = KEY_POOL[random.nextInt(KEY_POOL.length)];
        keyPresser.press(keyCode);
        return keyCode;
    }
}
