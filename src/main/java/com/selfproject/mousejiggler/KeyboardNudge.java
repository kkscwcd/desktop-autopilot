package com.selfproject.mousejiggler;

import java.awt.event.KeyEvent;

final class KeyboardNudge {
    private final KeyPresser keyPresser;
    private boolean horizontalNext = true;

    KeyboardNudge(KeyPresser keyPresser) {
        this.keyPresser = keyPresser;
    }

    void pressBalancedArrowKeys(KeyboardMode mode) {
        KeyboardMode safeMode = mode == null ? KeyboardMode.HORIZONTAL : mode;
        if (safeMode == KeyboardMode.ALTERNATE) {
            pressPair(horizontalNext ? KeyboardMode.HORIZONTAL : KeyboardMode.VERTICAL);
            horizontalNext = !horizontalNext;
            return;
        }
        pressPair(safeMode);
    }

    private void pressPair(KeyboardMode mode) {
        if (mode == KeyboardMode.VERTICAL) {
            press(KeyEvent.VK_DOWN);
            press(KeyEvent.VK_UP);
            return;
        }
        press(KeyEvent.VK_RIGHT);
        press(KeyEvent.VK_LEFT);
    }

    private void press(int keyCode) {
        keyPresser.press(keyCode);
    }
}
