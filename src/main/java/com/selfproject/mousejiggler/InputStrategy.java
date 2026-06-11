package com.selfproject.mousejiggler;

import java.awt.Point;

interface InputStrategy {
    void moveMouse(Point point);
    void pressKey(int javaKeyCode);
    default void close() {}
}
