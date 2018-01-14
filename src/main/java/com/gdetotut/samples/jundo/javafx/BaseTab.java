package com.gdetotut.samples.jundo.javafx;

import com.gdetotut.jundo.UndoCommand;
import com.gdetotut.jundo.UndoStack;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.hildan.fxgson.FxGson;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Базовый класс для постройки панели.
 * <p>Содержит все основные виджеты и их настройку.
 * <p>Класс наследует маркер {@link Serializable}, так как по дизайну включает сериализуемые субклассы {@link UndoCommand}.
 * Если бы команды были в standalone классе, это не потребовалось бы.
 */
public class BaseTab extends Tab implements Serializable {

    /**
     * "Класс-пакет" для команд над полем {@link #shape}.
     * <p>Оказалось довольно удобно систематизировать общие свойства и поведение таким образом.
     */
    public static class UndoBulk implements Serializable {

        /**
         * Идентификатор стека.
         */
        public static final String IDS_STACK = "com.gdetotut.samples.jundo.javafx";

        /**
         * Ключи для элементов {@link UndoStack#getLocalContexts}.
         */
        public static final String IDS_RES = "res";
        public static final String IDS_COLOR_PICKER = "color_picker";
        public static final String IDS_RADIUS_SLIDER = "radius_slider";
        public static final String IDS_X_SLIDER = "x_slider";
        public static final String IDS_Y_SLIDER = "y_slider";

        /**
         * Ключи для элементов {@link UndoStack#getSubj}, необходимые для сериализации поля {@link #shape}.
         * <p>Так как поле не имеет маркера {@link Serializable}, его прямая сериализация недоступна,
         * и приходится использовать инструменты маппинга. Другим вариантом оказалось бы непосредственная
         * конвертация {@link #shape} в JSON, это на усмотрение.
         */
        public static final String IDS_COLOR = "color";
        public static final String IDS_RADIUS = "radius";
        public static final String IDS_X = "x";
        public static final String IDS_Y = "y";

        /**
         * Базовыу класс команд для удобства.
         * @param <V>
         */
        public static class BaseUndo<V> extends UndoCommand {

            V oldV;
            V newV;

            /**
             * Хранит идентификатор ресурса для вызова конкретной строки.
             */
            final int resId;

            /**
             * @param owner Ссылка на свой {@link UndoStack}
             * @param parent Для команд в "цепочке".
             * @param resId Идентификатор строкового ресурса для {@link #getCaption}
             * @param oldV Текущее значение.
             * @param newV Значение, которое надо присвоить.
             */
            public BaseUndo(@NotNull UndoStack owner, UndoCommand parent, int resId, V oldV, V newV) {
                super(owner, "", parent);
                this.oldV = oldV;
                this.newV = newV;
                this.resId = resId;
            }

            @Override
            public String getCaption() {
                // Техника получения элемента локального контекста.
                // В реальности, конечно, следует проверять на наличие.
                Resources res = (Resources) owner.getLocalContexts().get(IDS_RES);
                return res.getString(resId);
            }
        }

        /**
         * Класс команды изменения цвета {@link #shape}.
         * <p>Тип {@link Color} не содержит маркера {@link Serializable}, и его невозможно напрямую сериализовать
         * в виде поля команды. Поэтому используется техника конвертации в JSON.
         */
        public static class ColorUndo extends BaseUndo<String> {

            public ColorUndo(@NotNull UndoStack owner, UndoCommand parent, int resId, Color oldV, Color newV) {
                super(owner, parent, resId,
                        FxGson.createWithExtras().toJson(oldV),
                        FxGson.createWithExtras().toJson(newV));
            }

            @Override
            protected void doRedo() {
                // Техника получения элемента локального контекста.
                // В реальности, конечно, следует проверять на наличие.
                ColorPicker cp = (ColorPicker) owner.getLocalContexts().get(IDS_COLOR_PICKER);

                Color cl = FxGson.createWithExtras().fromJson(newV, Color.class);
                cp.setValue(cl);
            }

            @Override
            protected void doUndo() {
                // Техника получения элемента локального контекста.
                // В реальности, конечно, следует проверять на наличие.
                ColorPicker cp = (ColorPicker) owner.getLocalContexts().get(IDS_COLOR_PICKER);

                Color cl = FxGson.createWithExtras().fromJson(oldV, Color.class);
                cp.setValue(cl);
            }

        }

        /**
         * Класс команды изменения радиуса {@link #shape}.
         * <p>Тип {@link Double} является сериализуемым, и его можно использовать напрямую.
         * <p>Отличие этого класса - в использовании метода {@link #mergeWith}.
         */
        public static class RadiusUndo extends BaseUndo<Double> {

            /**
             * Конструктор ничем не отличается от конструктора {@link ColorUndo}, за исключением типа значения.
             */
            public RadiusUndo(@NotNull UndoStack owner, UndoCommand parent, int resId, Number oldV, Number newV) {
                super(owner, parent, resId, (Double) oldV, (Double)newV);
            }

            @Override
            protected void doRedo() {
                Slider slider = (Slider)owner.getLocalContexts().get(IDS_RADIUS_SLIDER);
                slider.setValue(newV);
            }

            @Override
            protected void doUndo() {
                Slider slider = (Slider)owner.getLocalContexts().get(IDS_RADIUS_SLIDER);
                slider.setValue(oldV);
            }

            /**
             * Этот идентификатор используется для склейки команд и должен быть уникален для класса в пределах {@link #owner}.
             */
            @Override
            public int id() {
                return 1001;
            }

            /**
             * Склейка тут необходима, так как в момент движения ползунка радиуса события изменения свойства льются
             * непрерывно, и вместо одной команды, как для {@link ColorUndo} мы получаем великое множество, что логически неверно.
             * <p>Поэтому все непрерывные команды этого типа записываются в одну, что дает нам одно redo и одно undo на одно изменение.
             */
            @Override
            public boolean mergeWith(@NotNull UndoCommand cmd) {
                if(cmd instanceof RadiusUndo) {
                    RadiusUndo ruCmd = (RadiusUndo)cmd;
                    newV = ruCmd.newV;
                    return true;
                }
                return false;
            }

        }

        /**
         * Класс команды изменения x-координаты {@link #shape}. Совпадает с классом {@link RadiusUndo}.
         */
        public static class XUndo extends BaseUndo<Double> {

            public XUndo(@NotNull UndoStack owner, UndoCommand parent, int resId, Number oldV, Number newV) {
                super(owner, parent, resId, (Double) oldV, (Double)newV);
            }

            @Override
            protected void doRedo() {
                Slider slider = (Slider)owner.getLocalContexts().get(IDS_X_SLIDER);
                slider.setValue(newV);
            }

            @Override
            protected void doUndo() {
                Slider slider = (Slider)owner.getLocalContexts().get(IDS_X_SLIDER);
                slider.setValue(oldV);
            }

            /**
             * @see {@link RadiusUndo#id}
             */
            @Override
            public int id() {
                return 1002;
            }

            /**
             * @see {@link RadiusUndo#mergeWith}
             */
            @Override
            public boolean mergeWith(@NotNull UndoCommand cmd) {
                if(cmd instanceof XUndo) {
                    XUndo ruCmd = (XUndo)cmd;
                    newV = ruCmd.newV;
                    return true;
                }
                return false;
            }
        }

        /**
         * Класс команды изменения y-координаты {@link #shape}. Совпадает с классом {@link RadiusUndo}.
         */
        public static class YUndo extends BaseUndo<Double> {

            public YUndo(@NotNull UndoStack owner, UndoCommand parent, int resId, Number oldV, Number newV) {
                super(owner, parent, resId, (Double) oldV, (Double)newV);
            }

            @Override
            protected void doRedo() {
                Slider slider = (Slider)owner.getLocalContexts().get(IDS_Y_SLIDER);
                slider.setValue(newV);
            }

            @Override
            protected void doUndo() {
                Slider slider = (Slider)owner.getLocalContexts().get(IDS_Y_SLIDER);
                slider.setValue(oldV);
            }

            /**
             * @see {@link RadiusUndo#id()}
             */
            @Override
            public int id() {
                return 1003;
            }

            /**
             * @see {@link RadiusUndo#mergeWith}
             */
            @Override
            public boolean mergeWith(@NotNull UndoCommand cmd) {
                if(cmd instanceof YUndo) {
                    YUndo ruCmd = (YUndo)cmd;
                    newV = ruCmd.newV;
                    return true;
                }
                return false;
            }

        }

    }

    public Circle shape;
    public final ColorPicker colorPicker = new ColorPicker(Color.RED);
    public final Slider radius = new Slider(10, 200, 40);
    public final Slider centerX = new Slider(0, 400, 200);
    public final Slider centerY = new Slider(0, 400, 200);
    public final Button undoBtn = new Button("Undo2");
    public final Button redoBtn = new Button("Redo2");
    public final Button saveBtn = new Button("Save2");
    public final Button serialBtn = new Button();

    public BaseTab(String text, Circle shape) {
        super(text);
        this.shape = shape;

        Pane pane = new Pane();
        pane.setPrefWidth(400);
        pane.setPrefHeight(300);
        pane.getChildren().add(shape);

        shape.fillProperty().bind(colorPicker.valueProperty());
        shape.radiusProperty().bind(radius.valueProperty());
        shape.centerXProperty().bind(centerX.valueProperty());
        shape.centerYProperty().bind(centerY.valueProperty());

        HBox undoPanel = new HBox(20.0, undoBtn, redoBtn, saveBtn);
        VBox root = new VBox(10.0,
                pane,
                labeled("Color", colorPicker),
                labeled("Radius", radius),
                labeled("X", centerX),
                labeled("Y", centerY),
                undoPanel,
                serialBtn);

        root.setAlignment(Pos.CENTER);
        root.setFillWidth(false);
        setContent(root);
    }

    private static HBox labeled(String labelText, Node node) {
        Label label = new Label(labelText);
        label.setStyle("-fx-font-weight: bold;");
        HBox hbox = new HBox(15, label, node);
        hbox.setAlignment(Pos.CENTER);
        return hbox;
    }

}
