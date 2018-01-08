package com.gdetotut.samples.jundo.javafx.v2;

import com.gdetotut.samples.jundo.javafx.Resources;

public class Resources_V2 implements Resources {

    private final String IDS_COLOR = "color(v2)";
    private final String IDS_R = "radius(v2)";
    private final String IDS_X = "centerX(v2)";
    private final String IDS_Y = "centerY(v2)";

    @Override
    public String getString(int resId) {
        switch (resId) {
            case 0:
                return IDS_COLOR;
            case 1:
                return IDS_R;
            case 2:
                return IDS_X;
            case 3:
                return IDS_Y;
            default:
                return "???";
        }

    }
}
