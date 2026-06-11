package com.selfproject.mousejiggler;

import com.sun.jna.platform.win32.WinUser;
import java.lang.reflect.Field;

public class InspectJNA {
    public static void main(String[] args) {
        System.out.println("WinUser.MOUSEINPUT fields:");
        for (Field field : WinUser.MOUSEINPUT.class.getFields()) {
            System.out.println(field.getName() + " : " + field.getType().getName());
        }
    }
}
