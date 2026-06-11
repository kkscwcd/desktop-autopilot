package com.selfproject.mousejiggler;

final class WakeLockFactory {

    private static final WakeLock NO_OP = new WakeLock() {
        public void acquire() {}
        public void release() {}
    };

    private WakeLockFactory() {}

    static WakeLock create(boolean enabled) {
        if (!enabled) {
            return NO_OP;
        }
        String os = System.getProperty("os.name", "").toLowerCase();
        try {
            if (os.contains("mac")) {
                return new MacOSWakeLock();
            }
            if (os.contains("win")) {
                return new WindowsWakeLock();
            }
        } catch (Exception | UnsatisfiedLinkError e) {
            System.err.println("Sleep prevention unavailable (" + e.getMessage() + "), continuing without it.");
        }
        return NO_OP;
    }
}
