package com.gdetotut.samples.jundo.javafx.v2;

import com.gdetotut.jundo.UndoPacket;
import com.gdetotut.jundo.UndoStack;
import com.gdetotut.jundo.UndoWatcher;
import com.gdetotut.samples.jundo.javafx.BaseCtrl;
import com.gdetotut.samples.jundo.javafx.BaseTab;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.scene.paint.Color;
import org.hildan.fxgson.FxGson;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

import static com.gdetotut.samples.jundo.javafx.BaseTab.UndoBulk.IDS_STACK;


/**
 * Controller for  {@link JUndoTab_V2}
 */
public class JUndoCtrl_V2 extends BaseCtrl implements UndoWatcher{

    private final BaseTab tab;

    private UndoStack stack;

    public JUndoCtrl_V2(BaseTab tab) throws Exception {
        this.tab = tab;

        String store = new String(Files.readAllBytes(Paths.get("./undo.txt")));

        stack = UndoPacket
                // Check whether we got appropriate stack
                .peek(store, subjInfo -> IDS_STACK.equals(subjInfo.id))
                // Manual restoring (because we store non-serializable type)
                .restore((processedSubj, subjInfo) -> {
                    Type type = new TypeToken<HashMap<String, Object>>(){}.getType();
                    HashMap<String, Object> map = new Gson().fromJson((String) processedSubj, type);
                    if(subjInfo.version == 1) {
                        // Second - migration from V1 to V2!
                        Gson fxGson = FxGson.createWithExtras();
                        Color c = fxGson.fromJson(map.get("color").toString(), Color.class);
                        tab.colorPicker.setValue(c);
                        Double r = fxGson.fromJson(map.get("radius").toString(), Double.class);
                        tab.radius.setValue(r);
                        Double x = fxGson.fromJson(map.get("x").toString(), Double.class);
                        tab.centerX.setValue(x);
                        Double y = fxGson.fromJson(map.get("y").toString(), Double.class);
                        tab.centerY.setValue(y);
                    }
                    return map;
                }).stack((stack, subjInfo) -> {
                    // Restore new local contexts
                    stack.getLocalContexts().put(BaseTab.UndoBulk.IDS_RES, new Resources_V2());
                    stack.getLocalContexts().put(BaseTab.UndoBulk.IDS_COLOR_PICKER, tab.colorPicker);
                    stack.getLocalContexts().put(BaseTab.UndoBulk.IDS_RADIUS_SLIDER, tab.radius);
                    stack.getLocalContexts().put(BaseTab.UndoBulk.IDS_X_SLIDER, tab.centerX);
                    stack.getLocalContexts().put(BaseTab.UndoBulk.IDS_Y_SLIDER, tab.centerY);
                });

        if(null == stack)
            stack = new UndoStack(tab.shape, null);
        stack.setWatcher(this);

        // Link commands creation to widget listeners
        tab.shape.fillProperty().addListener(
                (observable, oldValue, newValue)
                        -> stack.push(new BaseTab.UndoBulk.ColorUndo(
                        stack, null, 0, (Color)oldValue, (Color)newValue)
                ));
        tab.shape.radiusProperty().addListener(
                (observable, oldValue, newValue)
                        -> stack.push(new BaseTab.UndoBulk.RadiusUndo(
                        stack, null, 1, oldValue, newValue)));

        tab.shape.centerXProperty().addListener(
                (observable, oldValue, newValue)
                        -> stack.push(new BaseTab.UndoBulk.XUndo(
                        stack, null, 2, oldValue, newValue)));

        tab.shape.centerYProperty().addListener(
                (observable, oldValue, newValue)
                        -> stack.push(new BaseTab.UndoBulk.YUndo(
                        stack, null, 3, oldValue, newValue)));


        // Initial call of event handler.
        // At this moment stack is empty, index is 0
        indexChanged(stack.getIdx());

        // Link stack to widget actions
        tab.undoBtn.setOnAction(event -> stack.undo());
        tab.redoBtn.setOnAction(event -> stack.redo());
        tab.saveBtn.setOnAction(event -> stack.setClean());
    }

    private void save() {
        // implement save action here
    }

    @Override
    public void indexChanged(int idx) {
        tab.undoBtn.setDisable(!stack.canUndo());
        tab.redoBtn.setDisable(!stack.canRedo());
        tab.saveBtn.setDisable(stack.isClean());
        tab.undoBtn.setText("undo: " + stack.undoCaption());
        tab.redoBtn.setText("redo: " + stack.redoCaption());
    }

    @Override
    public void cleanChanged(boolean clean) {
        tab.saveBtn.setDisable(clean);
        System.out.println("cleanChanged: " + clean);
    }

    @Override
    public void canUndoChanged(boolean canUndo) {
        System.out.println("canUndoChanged: " + canUndo);
    }

    @Override
    public void canRedoChanged(boolean canRedo) {
        System.out.println("canRedoChanged: " + canRedo);
    }

    @Override
    public void undoTextChanged(String undoText) {
        System.out.println("undoTextChanged: " + undoText);
    }

    @Override
    public void redoTextChanged(String redoText) {
        System.out.println("redoTextChanged: " + redoText);
    }
}
