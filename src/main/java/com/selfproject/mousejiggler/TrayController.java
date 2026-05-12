package com.selfproject.mousejiggler;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;

final class TrayController {
    private final SmartMouseJiggler jiggler;
    private final TrayIcon trayIcon;
    private final MenuItem pauseItem;

    private TrayController(SmartMouseJiggler jiggler, TrayIcon trayIcon, MenuItem pauseItem) {
        this.jiggler = jiggler;
        this.trayIcon = trayIcon;
        this.pauseItem = pauseItem;
    }

    static TrayController install(SmartMouseJiggler jiggler, AppConfig config) {
        if (!config.trayEnabled() || !SystemTray.isSupported()) {
            return null;
        }

        PopupMenu menu = new PopupMenu();
        MenuItem pauseItem = new MenuItem("Pause");
        MenuItem statusItem = new MenuItem("Status");
        MenuItem quitItem = new MenuItem("Quit");

        TrayIcon trayIcon = new TrayIcon(icon(), "Desktop Autopilot", menu);
        trayIcon.setImageAutoSize(true);
        TrayController controller = new TrayController(jiggler, trayIcon, pauseItem);

        pauseItem.addActionListener(event -> controller.togglePause());
        statusItem.addActionListener(event -> trayIcon.displayMessage("Desktop Autopilot", jiggler.status(), TrayIcon.MessageType.INFO));
        quitItem.addActionListener(event -> {
            jiggler.stop();
            SystemTray.getSystemTray().remove(trayIcon);
            System.exit(0);
        });

        menu.add(pauseItem);
        menu.add(statusItem);
        menu.addSeparator();
        menu.add(quitItem);

        try {
            SystemTray.getSystemTray().add(trayIcon);
            return controller;
        } catch (AWTException exception) {
            System.err.println("Tray unavailable: " + exception.getMessage());
            return null;
        }
    }

    void update() {
        pauseItem.setLabel(jiggler.isPaused() ? "Resume" : "Pause");
        trayIcon.setToolTip(jiggler.status());
    }

    private void togglePause() {
        jiggler.setPaused(!jiggler.isPaused());
        update();
    }

    private static BufferedImage icon() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(33, 150, 243));
            graphics.fillOval(1, 1, 14, 14);
            graphics.setColor(Color.WHITE);
            graphics.fillOval(5, 4, 3, 3);
            graphics.fillOval(9, 4, 3, 3);
            graphics.drawLine(5, 10, 11, 10);
        } finally {
            graphics.dispose();
        }
        return image;
    }
}
