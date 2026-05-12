package com.selfproject.mousejiggler;

import java.awt.Point;
import java.awt.Rectangle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class SafeMouseMovementTest {
    @Test
    void movesTwoPixelsWithinBounds() {
        MovementPlan plan = SafeMouseMovement.next(new Point(10, 10), new Rectangle(0, 0, 100, 100), 1);
        assertEquals(new Point(12, 10), plan.nextPoint(), "moves right by two pixels");
        assertEquals(1, plan.nextDirection(), "keeps direction");
    }

    @Test
    void reversesAtRightEdge() {
        MovementPlan plan = SafeMouseMovement.next(new Point(99, 10), new Rectangle(0, 0, 100, 100), 1);
        assertEquals(new Point(97, 10), plan.nextPoint(), "moves left near right edge");
        assertEquals(-1, plan.nextDirection(), "reverses direction at right edge");
    }

    @Test
    void reversesAtLeftEdge() {
        MovementPlan plan = SafeMouseMovement.next(new Point(0, 10), new Rectangle(0, 0, 100, 100), -1);
        assertEquals(new Point(2, 10), plan.nextPoint(), "moves right near left edge");
        assertEquals(1, plan.nextDirection(), "reverses direction at left edge");
    }

    @Test
    void clampsPointerThatStartsOutsideBounds() {
        MovementPlan plan = SafeMouseMovement.next(new Point(500, -20), new Rectangle(10, 10, 100, 100), 1);
        assertEquals(new Point(109, 10), plan.nextPoint(), "clamps an external pointer into safe bounds");
        assertEquals(-1, plan.nextDirection(), "points back into safe bounds");
    }

    @Test
    void handlesVeryNarrowBounds() {
        MovementPlan plan = SafeMouseMovement.next(new Point(10, 10), new Rectangle(5, 5, 1, 1), 1);
        assertEquals(new Point(5, 5), plan.nextPoint(), "clamps into one-pixel bounds");
        assertEquals(-1, plan.nextDirection(), "still returns a usable direction");
    }
}
