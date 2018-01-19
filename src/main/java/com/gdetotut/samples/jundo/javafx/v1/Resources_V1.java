package com.gdetotut.samples.jundo.javafx.v1;

import com.gdetotut.samples.jundo.javafx.Resources;

/**
 * String resources for V1
 */
public class Resources_V1 implements Resources {

    private String[] arr = new String[]{
            "color(v1)",
            "radius(v1)",
            "centerX(v1)",
            "centerY(v1)"
    };

    @Override
    public String getString(int resId) {

        if(resId < 0 || resId > arr.length - 1) {
            return "!out_of_index";
        }else {
            return arr[resId];
        }

    }
}
