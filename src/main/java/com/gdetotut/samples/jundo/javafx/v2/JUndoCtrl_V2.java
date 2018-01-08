package com.gdetotut.samples.jundo.javafx.v2;

import com.gdetotut.jundo.UndoSerializer;
import com.gdetotut.jundo.UndoSerializer.SubjInfo;
import com.gdetotut.jundo.UndoStack;
import com.gdetotut.jundo.UndoWatcher;
import com.gdetotut.samples.jundo.javafx.BaseCtrl;
import com.gdetotut.samples.jundo.javafx.BaseTab;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.scene.paint.Color;
import org.hildan.fxgson.FxGson;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static com.gdetotut.samples.jundo.javafx.BaseTab.UndoBulk.IDS_STACK;


/**
 * Контроллер для работы с JUndo на вкладке V2.
 */
public class JUndoCtrl_V2 extends BaseCtrl implements UndoWatcher{

    private final BaseTab tab;

    private final UndoStack stack;

    public JUndoCtrl_V2(BaseTab tab) throws IOException, ClassNotFoundException {
        this.tab = tab;

        String s = new String(Files.readAllBytes(Paths.get("./undo.txt")));
        // Восстановление стека из строки.
        UndoSerializer serializer = UndoSerializer.deserialize(s, (subjAsString, subjInfo) -> {
            if(subjInfo.id.equals(IDS_STACK)) {
                Type type = new TypeToken<HashMap<String, Object>>(){}.getType();
                HashMap<String, Object> c = new Gson().fromJson(subjAsString, type);
                return c;
            }
            // Следует возвращать null, если стек не нашего типа.
            // Это выставит свойство UndoSerializer#isExpected в false.
            return null;
        });

        // Проверим на соответствие стека нашим ожиданиям и на версию
        if(serializer.asExpected()) {
            stack = serializer.getStack();
            if(serializer.subjInfo.version == 1) {
                // Миграция свойств на новую версию
                Map<String, Object> map = (Map<String, Object>)stack.getSubj();
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
        }else{
            stack = new UndoStack(tab.shape, null);
        }

        // Подключение локальных контекстов на нужные места
        stack.getLocalContexts().put(BaseTab.UndoBulk.IDS_RES, new Resources_V2());
        stack.getLocalContexts().put(BaseTab.UndoBulk.IDS_COLOR_PICKER, tab.colorPicker);
        stack.getLocalContexts().put(BaseTab.UndoBulk.IDS_RADIUS_SLIDER, tab.radius);
        stack.getLocalContexts().put(BaseTab.UndoBulk.IDS_X_SLIDER, tab.centerX);
        stack.getLocalContexts().put(BaseTab.UndoBulk.IDS_Y_SLIDER, tab.centerY);
        stack.setWatcher(this);

        // Подключение слушателей свойств к созданию команд.
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


        indexChanged(stack.getIdx());

        tab.undoBtn.setOnAction(event -> stack.undo());
        tab.redoBtn.setOnAction(event -> stack.redo());
    }

    private void save() {
        // implement save action here
    }

    @Override
    public void indexChanged(int idx) {
        tab.undoBtn.setDisable(!stack.canUndo());
        tab.redoBtn.setDisable(!stack.canRedo());

        tab.undoBtn.setText("undo: " + stack.undoCaption());
        tab.redoBtn.setText("redo: " + stack.redoCaption());
    }
}
