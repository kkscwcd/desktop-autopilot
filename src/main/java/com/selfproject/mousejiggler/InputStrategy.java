package com.selfproject.mousejiggler;

import java.awt.Point;

interface InputStrategy {
    void moveMouse(Point point);
    void pressKey(int javaKeyCode);
    void click(Point point);
    default void close() {}
}
