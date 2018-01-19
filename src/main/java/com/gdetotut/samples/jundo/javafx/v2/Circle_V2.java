package com.gdetotut.samples.jundo.javafx.v2;

import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeType;

/**
 * New subject has other version.
 */
public class Circle_V2 extends Circle {

    public Circle_V2() {
        init();
    }

    void init() {
        this.setStroke(Color.GREEN);
        this.setStrokeWidth(6);
        this.setStrokeType(StrokeType.CENTERED);
    }
}
