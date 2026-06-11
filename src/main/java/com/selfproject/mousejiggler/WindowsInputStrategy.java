package com.selfproject.mousejiggler;

import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;

import java.awt.Point;

/**
 * Uses JNA platform's standard SendInput() to deliver mouse and keyboard events.
 * Bypasses x64 alignment/padding issues and supports multi-monitor setups.
 */
final class WindowsInputStrategy implements InputStrategy {

    private static final int SM_XVIRTUALSCREEN = 76;
    private static final int SM_YVIRTUALSCREEN = 77;
    private static final int SM_CXVIRTUALSCREEN = 78;
    private static final int SM_CYVIRTUALSCREEN = 79;

    private static final int MOUSEEVENTF_MOVE = 0x0001;
    private static final int MOUSEEVENTF_ABSOLUTE = 0x8000;
    private static final int MOUSEEVENTF_VIRTUALDESKTOP = 0x4000;

    WindowsInputStrategy() {
        // Trigger class loading to fail fast if User32 is unavailable
        User32.INSTANCE.getClass();
    }

    @Override
    public void moveMouse(Point point) {
        int xVirtual = User32.INSTANCE.GetSystemMetrics(SM_XVIRTUALSCREEN);
        int yVirtual = User32.INSTANCE.GetSystemMetrics(SM_YVIRTUALSCREEN);
        int wVirtual = User32.INSTANCE.GetSystemMetrics(SM_CXVIRTUALSCREEN);
        int hVirtual = User32.INSTANCE.GetSystemMetrics(SM_CYVIRTUALSCREEN);

        // Fallback to primary monitor metrics if virtual desktop metrics are unavailable
        if (wVirtual <= 0 || hVirtual <= 0) {
            wVirtual = User32.INSTANCE.GetSystemMetrics(0); // SM_CXSCREEN
            hVirtual = User32.INSTANCE.GetSystemMetrics(1); // SM_CYSCREEN
            xVirtual = 0;
            yVirtual = 0;
        }

        // Normalize coordinate to 0-65535 across the virtual desktop coordinates
        int normX = (int) (((long) (point.x - xVirtual) * 65535) / Math.max(1, wVirtual - 1));
        int normY = (int) (((long) (point.y - yVirtual) * 65535) / Math.max(1, hVirtual - 1));

        WinUser.INPUT[] inputs = (WinUser.INPUT[]) new WinUser.INPUT().toArray(1);
        WinUser.INPUT input = inputs[0];

        input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_MOUSE);
        input.input.setType("mi");
        input.input.mi.dx = new WinDef.LONG(normX);
        input.input.mi.dy = new WinDef.LONG(normY);
        input.input.mi.mouseData = new WinDef.DWORD(0);
        input.input.mi.time = new WinDef.DWORD(0);
        input.input.mi.dwExtraInfo = new BaseTSD.ULONG_PTR(0);
        
        int flags = MOUSEEVENTF_MOVE | MOUSEEVENTF_ABSOLUTE | MOUSEEVENTF_VIRTUALDESKTOP;
        input.input.mi.dwFlags = new WinDef.DWORD(flags);

        User32.INSTANCE.SendInput(new WinDef.DWORD(1), inputs, input.size());
    }

    @Override
    public void pressKey(int javaKeyCode) {
        // Java VK codes for arrow keys match Windows Virtual-Key codes exactly
        sendKey((short) javaKeyCode, 0);
        try {
            Thread.sleep(25);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        sendKey((short) javaKeyCode, WinUser.KEYBDINPUT.KEYEVENTF_KEYUP);
    }

    private void sendKey(short vk, int flags) {
        WinUser.INPUT[] inputs = (WinUser.INPUT[]) new WinUser.INPUT().toArray(1);
        WinUser.INPUT input = inputs[0];

        input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
        input.input.setType("ki");
        input.input.ki.wVk = new WinDef.WORD(vk);
        input.input.ki.wScan = new WinDef.WORD(0);
        input.input.ki.time = new WinDef.DWORD(0);
        input.input.ki.dwExtraInfo = new BaseTSD.ULONG_PTR(0);
        input.input.ki.dwFlags = new WinDef.DWORD(flags);

        User32.INSTANCE.SendInput(new WinDef.DWORD(1), inputs, input.size());
    }
}
