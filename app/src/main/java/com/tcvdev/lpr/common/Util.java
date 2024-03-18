package com.tcvdev.lpr.common;

import android.graphics.Paint;
import android.graphics.Rect;

public class Util {

    public static String getValidString(char[] text, int length) {


        int i = 0;
        for (i = 0; i < length; i++) {

            if (text[i] == 0) {

                break;
            }

        }

        String string = String.copyValueOf(text, 0, i);
        return  string;
    }


    public static Rect getTextBounds(Paint paint, String text) {

        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);

        return bounds;
    }


    public static String getTextFromSecond(int second) {

        String strTime = "";
        int hr = second / 3600;
        int min = (second % 3600) / 60;
        int sec = second % 60;

        if (hr > 0) {
            strTime = String.format("%02d:%02d:%02d", hr, min, sec);
        } else {
            strTime = String.format("%02d:%02d", min, sec);
        }
        return strTime;
    }
}
