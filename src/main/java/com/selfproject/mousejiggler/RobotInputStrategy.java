package com.selfproject.mousejiggler;

import java.awt.AWTException;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;

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
    public void click(Point point) {
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(50);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    @Override
    public void pressKey(int javaKeyCode) {
        // Map F15 sentinel to Java's VK_F15 (0xF002)
        int vk = (javaKeyCode == KeyboardJiggle.VK_F15_NATIVE) ? java.awt.event.KeyEvent.VK_F15 : javaKeyCode;
        robot.keyPress(vk);
        robot.delay(25);
        robot.keyRelease(vk);
    }
}
