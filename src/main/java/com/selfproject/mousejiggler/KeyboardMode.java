package com.selfproject.mousejiggler;

enum KeyboardMode {
    HORIZONTAL,
    VERTICAL,
    ALTERNATE;

    static KeyboardMode parse(String value) {
        if (value == null || value.isBlank()) {
            return HORIZONTAL;
        }
        return KeyboardMode.valueOf(value.trim().replace('-', '_').toUpperCase());
    }
}
