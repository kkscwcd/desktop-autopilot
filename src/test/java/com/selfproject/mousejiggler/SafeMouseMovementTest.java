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
        assertEquals(1, plan.nextXDirection(), "keeps direction");
    }

    @Test
    void reversesAtRightEdge() {
        MovementPlan plan = SafeMouseMovement.next(new Point(99, 10), new Rectangle(0, 0, 100, 100), 1);
        assertEquals(new Point(97, 10), plan.nextPoint(), "moves left near right edge");
        assertEquals(-1, plan.nextXDirection(), "reverses direction at right edge");
    }

    @Test
    void reversesAtLeftEdge() {
        MovementPlan plan = SafeMouseMovement.next(new Point(0, 10), new Rectangle(0, 0, 100, 100), -1);
        assertEquals(new Point(2, 10), plan.nextPoint(), "moves right near left edge");
        assertEquals(1, plan.nextXDirection(), "reverses direction at left edge");
    }

    @Test
    void clampsPointerThatStartsOutsideBounds() {
        MovementPlan plan = SafeMouseMovement.next(new Point(500, -20), new Rectangle(10, 10, 100, 100), 1);
        assertEquals(new Point(107, 10), plan.nextPoint(), "clamps an external pointer and nudges inward");
        assertEquals(-1, plan.nextXDirection(), "points back into safe bounds");
    }

    @Test
    void handlesVeryNarrowBounds() {
        MovementPlan plan = SafeMouseMovement.next(new Point(10, 10), new Rectangle(5, 5, 1, 1), 1);
        assertEquals(new Point(5, 5), plan.nextPoint(), "clamps into one-pixel bounds");
        assertEquals(-1, plan.nextXDirection(), "still returns a usable direction");
    }

    @Test
    void movesDiagonallyWithinBounds() {
        MovementPlan plan = SafeMouseMovement.next(
                new Point(10, 10),
                new Rectangle(0, 0, 100, 100),
                3,
                MovementMode.DIAGONAL,
                1,
                -1,
                new FixedRandomGenerator(0)
        );

        assertEquals(new Point(13, 7), plan.nextPoint(), "moves on both axes");
        assertEquals(1, plan.nextXDirection(), "keeps x direction");
        assertEquals(-1, plan.nextYDirection(), "keeps y direction");
    }

    @Test
    void randomMovementIsClampedInsideBounds() {
        MovementPlan plan = SafeMouseMovement.next(
                new Point(99, 99),
                new Rectangle(0, 0, 100, 100),
                5,
                MovementMode.RANDOM,
                1,
                1,
                new FixedRandomGenerator(5)
        );

        assertEquals(new Point(99, 99), plan.nextPoint(), "random target is clamped");
    }
}
