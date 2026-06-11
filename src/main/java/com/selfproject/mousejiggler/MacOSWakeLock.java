package com.selfproject.mousejiggler;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

/**
 * Holds an IOPMAssertion to prevent the display from sleeping.
 * Equivalent to running `caffeinate -d` in the background.
 * Requires no special entitlements — any app can hold a display assertion.
 */
final class MacOSWakeLock implements WakeLock {

    private static final int kIOPMAssertionLevelOn    = 255;
    private static final int kCFStringEncodingUTF8    = 0x08000100;

    interface CoreFoundation extends Library {
        CoreFoundation INSTANCE = Native.load("CoreFoundation", CoreFoundation.class);
        Pointer CFStringCreateWithCString(Pointer allocator, String cStr, int encoding);
        void CFRelease(Pointer cf);
    }

    interface IOKit extends Library {
        IOKit INSTANCE = Native.load("IOKit", IOKit.class);
        int IOPMAssertionCreateWithName(Pointer assertionType, int level,
                                        Pointer assertionName, IntByReference assertionID);
        int IOPMAssertionRelease(int assertionID);
    }

    private int assertionID = 0;

    MacOSWakeLock() {
        // Fail fast if IOKit is unavailable
        IOKit.INSTANCE.getClass();
    }

    @Override
    public void acquire() {
        Pointer type = CoreFoundation.INSTANCE.CFStringCreateWithCString(
                null, "PreventUserIdleDisplaySleep", kCFStringEncodingUTF8);
        Pointer name = CoreFoundation.INSTANCE.CFStringCreateWithCString(
                null, "Desktop Autopilot active", kCFStringEncodingUTF8);
        try {
            IntByReference idRef = new IntByReference();
            int result = IOKit.INSTANCE.IOPMAssertionCreateWithName(type, kIOPMAssertionLevelOn, name, idRef);
            if (result == 0) {
                assertionID = idRef.getValue();
            } else {
                System.err.println("Warning: sleep prevention assertion failed (IOReturn=" + result + ")");
            }
        } finally {
            if (type != null) CoreFoundation.INSTANCE.CFRelease(type);
            if (name != null) CoreFoundation.INSTANCE.CFRelease(name);
        }
    }

    @Override
    public void release() {
        if (assertionID != 0) {
            IOKit.INSTANCE.IOPMAssertionRelease(assertionID);
            assertionID = 0;
        }
    }
}
