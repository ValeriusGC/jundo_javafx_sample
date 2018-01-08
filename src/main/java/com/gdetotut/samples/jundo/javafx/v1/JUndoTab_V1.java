package com.gdetotut.samples.jundo.javafx.v1;

import com.gdetotut.samples.jundo.javafx.BaseTab;
import javafx.scene.control.TabPane;
import javafx.scene.shape.Circle;

/**
 * Во вкладке V1 в качестве {@link #shape} используется простой {@link Circle}
 */
public class JUndoTab_V1 extends BaseTab {

//    private final JUndoCtrl_V1 ctrl;

    public JUndoTab_V1(String text, TabPane tabPane) {
        super(text, new Circle());
        new JUndoCtrl_V1(this, tabPane);
        serialBtn.setText("Serialize");
    }
}
