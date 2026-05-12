package com.selfproject.mousejiggler;

enum MovementMode {
    HORIZONTAL,
    VERTICAL,
    DIAGONAL,
    RANDOM;

    static MovementMode parse(String value) {
        if (value == null || value.isBlank()) {
            return HORIZONTAL;
        }
        return MovementMode.valueOf(value.trim().replace('-', '_').toUpperCase());
    }
}
