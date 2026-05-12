package com.selfproject.mousejiggler;

import java.util.random.RandomGenerator;

final class FixedRandomGenerator implements RandomGenerator {
    private final int value;

    FixedRandomGenerator(int value) {
        this.value = value;
    }

    @Override
    public long nextLong() {
        return value;
    }

    @Override
    public int nextInt(int origin, int bound) {
        return Math.max(origin, Math.min(bound - 1, value));
    }
}
