package com.selfproject.mousejiggler;

enum Profile {
    MINIMAL,
    KEEP_AWAKE,
    STEALTH,
    PRESENTATION;

    static Profile parse(String value) {
        if (value == null || value.isBlank()) {
            return MINIMAL;
        }
        return Profile.valueOf(value.trim().replace('-', '_').toUpperCase());
    }
}
