package com.gdetotut.samples.jundo.javafx.v2;

import com.gdetotut.samples.jundo.javafx.BaseTab;
import javafx.scene.shape.Circle;

import java.io.IOException;

/**
 * Во вкладке V2 в качестве {@link #shape} используется наследник {@link Circle} для демонстрации миграции субъекта стека.
 */
public class JUndoTab_V2 extends BaseTab {

//    private final JUndoCtrl_V2 ctrl;

    public JUndoTab_V2(String text) throws Exception {
        super(text, new Circle_V2());
        new JUndoCtrl_V2(this);
        serialBtn.setVisible(false);
    }
}
