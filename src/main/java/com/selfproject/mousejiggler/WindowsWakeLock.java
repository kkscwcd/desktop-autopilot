package com.selfproject.mousejiggler;

import com.sun.jna.Native;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

/**
 * Calls SetThreadExecutionState to prevent Windows from turning off the display
 * or entering sleep. Equivalent to calling PowerSetActiveScheme programmatically.
 * No elevation or special permissions required.
 */
final class WindowsWakeLock implements WakeLock {

    // Flags for SetThreadExecutionState
    private static final int ES_CONTINUOUS       = 0x80000000;
    private static final int ES_SYSTEM_REQUIRED  = 0x00000001;
    private static final int ES_DISPLAY_REQUIRED = 0x00000002;

    interface Kernel32Ex extends StdCallLibrary {
        Kernel32Ex INSTANCE = Native.load("kernel32", Kernel32Ex.class, W32APIOptions.DEFAULT_OPTIONS);
        int SetThreadExecutionState(int esFlags);
    }

    WindowsWakeLock() {
        Kernel32Ex.INSTANCE.getClass();
    }

    @Override
    public void acquire() {
        // ES_CONTINUOUS keeps the state until explicitly reset;
        // ES_DISPLAY_REQUIRED and ES_SYSTEM_REQUIRED prevent screen-off and sleep.
        Kernel32Ex.INSTANCE.SetThreadExecutionState(
                ES_CONTINUOUS | ES_DISPLAY_REQUIRED | ES_SYSTEM_REQUIRED);
    }

    @Override
    public void release() {
        // Pass only ES_CONTINUOUS with no other flags to revert to system defaults.
        Kernel32Ex.INSTANCE.SetThreadExecutionState(ES_CONTINUOUS);
    }
}
