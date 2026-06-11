package com.selfproject.mousejiggler;

enum InputMode {
    AUTO,   // try native first, fall back to Robot
    ROBOT,  // java.awt.Robot (default, software-level)
    NATIVE; // platform native APIs (hardware-level appearance)

    static InputMode parse(String value) {
        if (value == null || value.isBlank()) {
            return AUTO;
        }
        return InputMode.valueOf(value.trim().toUpperCase());
    }
}
