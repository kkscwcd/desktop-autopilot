package com.selfproject.mousejiggler;

import org.junit.jupiter.api.Test;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class KeyboardNudgeTest {
    @Test
    void sendsBalancedHorizontalArrowPair() {
        List<Integer> keys = new ArrayList<>();
        KeyboardNudge keyboardNudge = new KeyboardNudge(keys::add);

        keyboardNudge.pressBalancedArrowKeys(KeyboardMode.HORIZONTAL);

        assertEquals(List.of(KeyEvent.VK_RIGHT, KeyEvent.VK_LEFT), keys);
    }

    @Test
    void alternatesBetweenHorizontalAndVerticalPairs() {
        List<Integer> keys = new ArrayList<>();
        KeyboardNudge keyboardNudge = new KeyboardNudge(keys::add);

        keyboardNudge.pressBalancedArrowKeys(KeyboardMode.ALTERNATE);
        keyboardNudge.pressBalancedArrowKeys(KeyboardMode.ALTERNATE);

        assertEquals(List.of(KeyEvent.VK_RIGHT, KeyEvent.VK_LEFT, KeyEvent.VK_DOWN, KeyEvent.VK_UP), keys);
    }
}
