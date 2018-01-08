package com.gdetotut.samples.jundo.javafx.v1;

import com.gdetotut.jundo.UndoSerializer;
import com.gdetotut.jundo.UndoStack;
import com.gdetotut.jundo.UndoWatcher;
import com.gdetotut.samples.jundo.javafx.BaseCtrl;
import com.gdetotut.samples.jundo.javafx.BaseTab;
import com.gdetotut.samples.jundo.javafx.v2.JUndoTab_V2;
import com.google.gson.Gson;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.TabPane;
import javafx.scene.paint.Color;
import org.hildan.fxgson.FxGson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static com.gdetotut.samples.jundo.javafx.BaseTab.UndoBulk.IDS_STACK;

/**
 * Контроллер для работы с JUndo на вкладке V1
 */
public class JUndoCtrl_V1 extends BaseCtrl implements UndoWatcher{


    private final BaseTab tab;

    private final UndoStack stack;

    public JUndoCtrl_V1(BaseTab tab, TabPane tabPane) {
        this.tab = tab;

        stack = new UndoStack(tab.shape, null);
        // Назначение локальных контекстов
        stack.getLocalContexts().put(BaseTab.UndoBulk.IDS_RES, new Resources_V1());
        stack.getLocalContexts().put(BaseTab.UndoBulk.IDS_COLOR_PICKER, tab.colorPicker);
        stack.getLocalContexts().put(BaseTab.UndoBulk.IDS_RADIUS_SLIDER, tab.radius);
        stack.getLocalContexts().put(BaseTab.UndoBulk.IDS_X_SLIDER, tab.centerX);
        stack.getLocalContexts().put(BaseTab.UndoBulk.IDS_Y_SLIDER, tab.centerY);
        // Назначение обработчика событий UndoStack
        stack.setWatcher(this);

        // Привязка создания команд к событиям пропертей
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
        // ~

        // Вызов обработчика событий для настройки виджетов.
        // В данный момент стек пуст, и индекс равен 0
        indexChanged(stack.getIdx());

        // Настройка undo/redo команд стека
        tab.undoBtn.setOnAction(event -> stack.undo());
        tab.redoBtn.setOnAction(event -> stack.redo());
        // ~

        // Сериализация с последующим переходом на закладку V2.
        tab.serialBtn.setOnAction(event -> {
            try {
                serialize();
                if(tabPane.getTabs().size() > 1) {
                    tabPane.getTabs().remove(1);
                }
                tabPane.getTabs().add(new JUndoTab_V2("JUndo_V2"));
                tabPane.getSelectionModel().select(1);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Сериализация демонстрирует технику работы с несериализуемым субъектом стека.
     * Мы просто сохраняем нужные нам свойства в виде карты.
     */
    private void serialize() throws IOException {
        String s = "";

        // Мы сохраняем идентификатор стека и номер версии.
        UndoSerializer serializer = new UndoSerializer(IDS_STACK, 1, stack);
        try {
            s = UndoSerializer.serialize(serializer, true, subj -> {
                Map<String, Object> props = new HashMap<>();
                Gson fxGson = FxGson.createWithExtras();
                props.put("color", FxGson.createWithExtras().toJson(tab.shape.getFill()));
                props.put("radius", FxGson.createWithExtras().toJson(tab.shape.getRadius()));
                props.put("x", FxGson.createWithExtras().toJson(tab.shape.getCenterX()));
                props.put("y", FxGson.createWithExtras().toJson(tab.shape.getCenterY()));
                return fxGson.toJson(props);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Для простоты стек сохраняется в файле в корне проекта.
        Files.write(Paths.get("./undo.txt"), s.getBytes());
    }

    private void save() {
        // implement save action here
    }

    /**
     * Обработчик одного из событий стека {@link UndoWatcher}
     * @param idx
     */
    @Override
    public void indexChanged(int idx) {
        tab.undoBtn.setDisable(!stack.canUndo());
        tab.redoBtn.setDisable(!stack.canRedo());
        tab.undoBtn.setText("undo: " + stack.undoCaption());
        tab.redoBtn.setText("redo: " + stack.redoCaption());
    }
}