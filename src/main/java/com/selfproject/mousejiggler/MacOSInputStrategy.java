package com.selfproject.mousejiggler;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.awt.Point;
import java.awt.event.KeyEvent;

/**
 * Posts events via CGEventPost(kCGHIDEventTap, ...) so the OS treats them as
 * originating from the HID subsystem rather than a software process.
 * Requires the Accessibility permission granted to the JVM in System Preferences.
 */
final class MacOSInputStrategy implements InputStrategy {

    // CGEventTapLocation
    private static final int kCGHIDEventTap = 0;

    // CGEventType
    private static final int kCGEventMouseMoved = 5;

    // CGMouseButton
    private static final int kCGMouseButtonLeft = 0;

    // CGEventSourceStateID — HIDSystemState makes events appear hardware-generated
    private static final int kCGEventSourceStateHIDSystemState = 1;

    interface CoreGraphics extends Library {
        CoreGraphics INSTANCE = Native.load("CoreGraphics", CoreGraphics.class);

        // source=null for mouse works; keyboard needs a real source (see pressKey)
        Pointer CGEventCreateMouseEvent(Pointer source, int mouseType, CGPoint.ByValue position, int mouseButton);

        // keyDown is C bool (1 byte) — use byte, not boolean, to avoid JNA's 4-byte int marshalling
        Pointer CGEventCreateKeyboardEvent(Pointer source, short virtualKey, byte keyDown);

        // Creates a source associated with the given event state (HID, session, etc.)
        Pointer CGEventSourceCreate(int stateID);

        void CGEventPost(int tap, Pointer event);
        void CFRelease(Pointer cf);
    }

    @Structure.FieldOrder({"x", "y"})
    public static class CGPoint extends Structure {
        public double x;
        public double y;

        public static class ByValue extends CGPoint implements Structure.ByValue {
            public ByValue(double x, double y) {
                this.x = x;
                this.y = y;
            }
        }
    }

    // Map java.awt.event.KeyEvent VK codes → macOS CGKeyCode (HIToolbox/Events.h)
    private static short toCGKeyCode(int vk) {
        return switch (vk) {
            case KeyEvent.VK_SPACE -> 49;
            case KeyEvent.VK_SHIFT -> 56;
            case KeyEvent.VK_LEFT  -> 123;
            case KeyEvent.VK_RIGHT -> 124;
            case KeyEvent.VK_DOWN  -> 125;
            case KeyEvent.VK_UP    -> 126;
            default -> throw new IllegalArgumentException("Unsupported key code for native input: " + vk);
        };
    }

    MacOSInputStrategy() {
        // Trigger load — throws UnsatisfiedLinkError if CoreGraphics is unavailable
        CoreGraphics.INSTANCE.getClass();
    }

    @Override
    public void moveMouse(Point point) {
        CGPoint.ByValue position = new CGPoint.ByValue(point.x, point.y);
        Pointer event = CoreGraphics.INSTANCE.CGEventCreateMouseEvent(
                null, kCGEventMouseMoved, position, kCGMouseButtonLeft);
        if (event == null) {
            return;
        }
        try {
            CoreGraphics.INSTANCE.CGEventPost(kCGHIDEventTap, event);
        } finally {
            CoreGraphics.INSTANCE.CFRelease(event);
        }
    }

    @Override
    public void pressKey(int javaKeyCode) {
        short cgKey = toCGKeyCode(javaKeyCode);

        // A null source is unreliable for keyboard events on macOS 12+.
        // Creating a source tied to kCGEventSourceStateHIDSystemState makes the
        // events appear as if they originated from the HID driver.
        Pointer source = CoreGraphics.INSTANCE.CGEventSourceCreate(kCGEventSourceStateHIDSystemState);
        try {
            Pointer press   = CoreGraphics.INSTANCE.CGEventCreateKeyboardEvent(source, cgKey, (byte) 1);
            Pointer release = CoreGraphics.INSTANCE.CGEventCreateKeyboardEvent(source, cgKey, (byte) 0);
            try {
                if (press != null) {
                    CoreGraphics.INSTANCE.CGEventPost(kCGHIDEventTap, press);
                }
                try {
                    Thread.sleep(25);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if (release != null) {
                    CoreGraphics.INSTANCE.CGEventPost(kCGHIDEventTap, release);
                }
            } finally {
                if (press != null)   CoreGraphics.INSTANCE.CFRelease(press);
                if (release != null) CoreGraphics.INSTANCE.CFRelease(release);
            }
        } finally {
            if (source != null) CoreGraphics.INSTANCE.CFRelease(source);
        }
    }
}
