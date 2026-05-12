package com.selfproject.mousejiggler;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Objects;
import java.util.random.RandomGenerator;

final class SafeMouseMovement {
    static final int STEP_PIXELS = 2;

    private SafeMouseMovement() {
    }

    static MovementPlan next(Point currentPoint, Rectangle safeBounds, int direction) {
        MovementPlan plan = next(currentPoint, safeBounds, STEP_PIXELS, MovementMode.HORIZONTAL, direction, 1,
                RandomGenerator.getDefault());
        return new MovementPlan(plan.nextPoint(), plan.nextXDirection(), plan.nextYDirection());
    }

    static MovementPlan next(
            Point currentPoint,
            Rectangle safeBounds,
            int stepPixels,
            MovementMode mode,
            int xDirection,
            int yDirection,
            RandomGenerator randomGenerator
    ) {
        Objects.requireNonNull(currentPoint, "currentPoint");
        Objects.requireNonNull(randomGenerator, "randomGenerator");

        if (safeBounds == null || safeBounds.isEmpty()) {
            return new MovementPlan(new Point(currentPoint), normalizeDirection(xDirection), normalizeDirection(yDirection));
        }

        int safeXDirection = normalizeDirection(xDirection);
        int safeYDirection = normalizeDirection(yDirection);
        int safeStepPixels = Math.max(1, stepPixels);
        int minX = safeBounds.x;
        int maxX = safeBounds.x + safeBounds.width - 1;
        int minY = safeBounds.y;
        int maxY = safeBounds.y + safeBounds.height - 1;
        int baseX = clamp(currentPoint.x, minX, maxX);
        int baseY = clamp(currentPoint.y, minY, maxY);

        int requestedX = baseX;
        int requestedY = baseY;
        MovementMode safeMode = mode == null ? MovementMode.HORIZONTAL : mode;

        if (safeMode == MovementMode.RANDOM) {
            int xDelta = randomGenerator.nextInt(-safeStepPixels, safeStepPixels + 1);
            int yDelta = randomGenerator.nextInt(-safeStepPixels, safeStepPixels + 1);
            if (xDelta == 0 && yDelta == 0) {
                xDelta = safeXDirection * safeStepPixels;
            }
            requestedX = baseX + xDelta;
            requestedY = baseY + yDelta;
        } else {
            if (safeMode == MovementMode.HORIZONTAL || safeMode == MovementMode.DIAGONAL) {
                requestedX = baseX + safeXDirection * safeStepPixels;
                if (requestedX < minX || requestedX > maxX) {
                    safeXDirection *= -1;
                    requestedX = baseX + safeXDirection * safeStepPixels;
                }
            }
            if (safeMode == MovementMode.VERTICAL || safeMode == MovementMode.DIAGONAL) {
                requestedY = baseY + safeYDirection * safeStepPixels;
                if (requestedY < minY || requestedY > maxY) {
                    safeYDirection *= -1;
                    requestedY = baseY + safeYDirection * safeStepPixels;
                }
            }
        }

        int x = clamp(requestedX, minX, maxX);
        int y = clamp(requestedY, minY, maxY);
        return new MovementPlan(new Point(x, y), safeXDirection, safeYDirection);
    }

    private static int normalizeDirection(int direction) {
        return direction < 0 ? -1 : 1;
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
