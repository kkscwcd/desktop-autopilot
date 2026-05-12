package com.selfproject.mousejiggler;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.Toolkit;

final class ScreenBounds {
    private ScreenBounds() {
    }

    static Rectangle safeBoundsForPointer() {
        PointerInfo pointerInfo = MouseInfo.getPointerInfo();
        GraphicsDevice device = pointerInfo == null
                ? GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                : pointerInfo.getDevice();
        return safeBounds(device.getDefaultConfiguration());
    }

    static Point pointerLocation() {
        PointerInfo pointerInfo = MouseInfo.getPointerInfo();
        if (pointerInfo == null) {
            Rectangle bounds = safeBoundsForPointer();
            return new Point(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
        }
        return pointerInfo.getLocation();
    }

    private static Rectangle safeBounds(GraphicsConfiguration configuration) {
        Rectangle bounds = new Rectangle(configuration.getBounds());
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration);

        bounds.x += insets.left;
        bounds.y += insets.top;
        bounds.width -= insets.left + insets.right;
        bounds.height -= insets.top + insets.bottom;

        if (bounds.width <= 0 || bounds.height <= 0) {
            return new Rectangle(configuration.getBounds());
        }
        return bounds;
    }
}
