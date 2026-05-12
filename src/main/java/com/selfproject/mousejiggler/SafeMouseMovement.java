package com.selfproject.mousejiggler;

import java.awt.Point;
import java.awt.Rectangle;

final class SafeMouseMovement {
    static final int STEP_PIXELS = 2;

    private SafeMouseMovement() {
    }

    static MovementPlan next(Point currentPoint, Rectangle safeBounds, int direction) {
        if (safeBounds == null || safeBounds.isEmpty()) {
            return new MovementPlan(new Point(currentPoint), normalizeDirection(direction));
        }

        int safeDirection = normalizeDirection(direction);
        int minX = safeBounds.x;
        int maxX = safeBounds.x + safeBounds.width - 1;
        int y = clamp(currentPoint.y, safeBounds.y, safeBounds.y + safeBounds.height - 1);
        int requestedX = currentPoint.x + safeDirection * STEP_PIXELS;

        if (requestedX < minX || requestedX > maxX) {
            safeDirection *= -1;
            requestedX = currentPoint.x + safeDirection * STEP_PIXELS;
        }

        int x = clamp(requestedX, minX, maxX);
        return new MovementPlan(new Point(x, y), safeDirection);
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
