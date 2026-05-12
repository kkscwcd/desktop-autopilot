package com.selfproject.mousejiggler;

import java.awt.Point;

@FunctionalInterface
interface MouseMover {
    void move(Point point);
}
