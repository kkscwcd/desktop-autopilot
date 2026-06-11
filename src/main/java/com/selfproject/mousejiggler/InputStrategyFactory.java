package com.selfproject.mousejiggler;

import java.awt.AWTException;

final class InputStrategyFactory {

    private InputStrategyFactory() {}

    static InputStrategy create(InputMode mode) {
        return switch (mode) {
            case ROBOT  -> createRobot();
            case NATIVE -> createNative();
            case AUTO   -> tryNativeOrFallback();
        };
    }

    private static InputStrategy createNative() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) {
            return new MacOSInputStrategy();
        }
        if (os.contains("win")) {
            return new WindowsInputStrategy();
        }
        throw new UnsupportedOperationException(
                "Native input is only supported on macOS and Windows. Current OS: " + os);
    }

    private static InputStrategy tryNativeOrFallback() {
        try {
            InputStrategy native_ = createNative();
            System.out.println("Input mode: native (hardware-level events)");
            return native_;
        } catch (Exception | UnsatisfiedLinkError e) {
            System.err.println("Native input unavailable (" + e.getMessage() + "), falling back to Robot.");
            return createRobot();
        }
    }

    private static InputStrategy createRobot() {
        try {
            System.out.println("Input mode: Robot (software-level events)");
            return new RobotInputStrategy();
        } catch (AWTException e) {
            throw new IllegalStateException(
                    "Unable to initialise Robot input: " + e.getMessage(), e);
        }
    }
}
