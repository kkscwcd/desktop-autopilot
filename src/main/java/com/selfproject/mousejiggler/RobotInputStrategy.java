package com.selfproject.mousejiggler;

import java.awt.AWTException;
import java.awt.Point;
import java.awt.Robot;

final class RobotInputStrategy implements InputStrategy {
    private final Robot robot;

    RobotInputStrategy() throws AWTException {
        this.robot = new Robot();
    }

    @Override
    public void moveMouse(Point point) {
        robot.mouseMove(point.x, point.y);
    }

    @Override
    public void pressKey(int javaKeyCode) {
        robot.keyPress(javaKeyCode);
        robot.delay(25);
        robot.keyRelease(javaKeyCode);
    }
}
