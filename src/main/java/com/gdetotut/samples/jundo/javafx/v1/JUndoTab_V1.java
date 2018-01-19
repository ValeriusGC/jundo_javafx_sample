package com.gdetotut.samples.jundo.javafx.v1;

import com.gdetotut.samples.jundo.javafx.BaseTab;
import javafx.scene.control.TabPane;
import javafx.scene.shape.Circle;

/**
 * Here {@link #shape} is as simple {@link Circle}
 */
public class JUndoTab_V1 extends BaseTab {

    public JUndoTab_V1(String text, TabPane tabPane) {
        super(text, new Circle());
        new JUndoCtrl_V1(this, tabPane);
        serialBtn.setText("Store stack and migrate to V2");
    }
}
