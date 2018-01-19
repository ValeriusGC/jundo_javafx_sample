# JavaFx example for JUndo - Java's undo library


![gif](https://github.com/ValeriusGC/jundo/blob/master/doc/sample.gif)

[JUndo is a undo/redo library](https://github.com/ValeriusGC/jundo) for implementing undo/redo functionality in Java applications.

It based on the `Command pattern` - idea that all editing in an application is done by creating instances of commands. Commands apply changes to the document and are stored on a command stack. Furthermore, each command knows how to undo its changes to bring the document back to its previous state. As long as the application only uses command objects to change the state of the document, it is possible to undo a sequence of commands by traversing the stack downwards and calling undo on each command in turn. It is also possible to redo a sequence of commands by traversing the stack upwards and calling redo on each command.

This example illustrates library's advanced features.

First af all you should plan the design of your 'undo stack' for specific subject.

#### Step 0. Design...

##### ... for commands

We control properties for `javafx.scene.shape.Circle` instance.

- this class doesn't implement `Serializable` so we do not use it in command's fields. Instead we will store specific controlled properties: `ColorUndo` will store color, `RadiusUndo` will store radius and so on
- commands have caption property that can depends on context (stack can be restored on another locale, for example), so we do not store strings but only string identifiers, and request strings dynamically via local contexts of the stack
- app's widgets `javafx.scene.control.Slider` which change `x`, `y` and `radius` do fire events on every minor changes. But we don't need 100 commands for 100 pixels - only one command for entire change. So we will use commands merging

Here how it looks:

```java
// resId - is a string identifier.
public ColorUndo(@NotNull UndoStack owner, UndoCommand parent, int resId, Color oldV, Color newV) {
    super(owner, parent, resId,
        // Color is not Serializable too, so we convert it to JSON
        FxGson.createWithExtras().toJson(oldV),
        FxGson.createWithExtras().toJson(newV));
    }

@Override
protected void doRedo() {
    // Here how to get local context
    ColorPicker cp = (ColorPicker) owner.getLocalContexts().get(IDS_COLOR_PICKER);
    Color cl = FxGson.createWithExtras().fromJson(newV, Color.class);
    cp.setValue(cl);
}


@Override
protected void doUndo() {
    // Here how to get local context
    ColorPicker cp = (ColorPicker) owner.getLocalContexts().get(IDS_COLOR_PICKER);
    Color cl = FxGson.createWithExtras().fromJson(oldV, Color.class);
    cp.setValue(cl);
}

@Override
public int id() {
    // Here how to set unique id for merging. 
    // The same for XUndo (return 1002) and YUndo (return 1003).
    return 1001; 
}

@Override
public boolean mergeWith(@NotNull UndoCommand cmd) {
    // Here how to merge for RadiusUndo.
    // The same for XUndo and YUndo.
    if(cmd instanceof RadiusUndo) {
        RadiusUndo ruCmd = (RadiusUndo)cmd;
        newV = ruCmd.newV;
        return true;
    }
    return false;
}

@Override
public String getCaption() {
    // Here how to get local context
    Resources res = (Resources) owner.getLocalContexts().get(IDS_RES);
    return res.getString(resId);
}
```

##### ... for undo stack

Widgets and resources are parts of Scene and obviously depend on local memory addressing. So we will use them as local contexts.


#### Step 1. Do instance of the the stack and set the events watcher


```java
stack = new UndoStack(tab.shape, null);
stack.getLocalContexts().put(BaseTab.UndoBulk.IDS_RES, new Resources_V1());
stack.getLocalContexts().put(BaseTab.UndoBulk.IDS_COLOR_PICKER, tab.colorPicker);
stack.getLocalContexts().put(BaseTab.UndoBulk.IDS_RADIUS_SLIDER, tab.radius);
stack.getLocalContexts().put(BaseTab.UndoBulk.IDS_X_SLIDER, tab.centerX);
stack.getLocalContexts().put(BaseTab.UndoBulk.IDS_Y_SLIDER, tab.centerY);

stack.setWatcher(this);
```
#### Step 2. Commands and stack linking

We use widget and stack events.

```java
//  Link create commands to the events of property
tab.shape.fillProperty().addListener(
    (observable, oldValue, newValue)
        -> stack.push(new BaseTab.UndoBulk.ColorUndo(
            stack, null, 0, (Color)oldValue, (Color)newValue)
));

//  Link stack methods to the app actions
tab.undoBtn.setOnAction(event -> stack.undo());
tab.redoBtn.setOnAction(event -> stack.redo());
tab.saveBtn.setOnAction(event -> stack.setClean());

// Handler of one of stack events
@Override
public void indexChanged(int idx) {
    tab.undoBtn.setDisable(!stack.canUndo());
    tab.redoBtn.setDisable(!stack.canRedo());
    tab.saveBtn.setDisable(stack.isClean());
    tab.undoBtn.setText("undo: " + stack.undoCaption());
    tab.redoBtn.setText("redo: " + stack.redoCaption());
}
```

#### Step 3. Save the stack

Here demonstrates how to work with the non-serializable subject. We just save specific values in the map.
**Very important question: For what we should save subject's state? The fact is the stack has history of changes from start till 'that point of time'. And in new place we should refresh that subject exactly to 'that point of time'.**

```java
private void serialize() throws IOException {
    try {
        String store = UndoPacket
            .make(stack, IDS_STACK, 1)
            .onStore(new UndoPacket.OnStore() {
                @Override
                public Serializable handle(Object subj) {
                    Map<String, Object> props = new HashMap<>();
                        Gson fxGson = FxGson.createWithExtras();
                        props.put("color", FxGson.createWithExtras().toJson(tab.shape.getFill()));
                        props.put("radius", FxGson.createWithExtras().toJson(tab.shape.getRadius()));
                        props.put("x", FxGson.createWithExtras().toJson(tab.shape.getCenterX()));
                        props.put("y", FxGson.createWithExtras().toJson(tab.shape.getCenterY()));
                        return fxGson.toJson(props);
                    }
            })
            .zipped(true)
            .store();

        // Simply store in file
        Files.write(Paths.get("./undo.txt"), store.getBytes());
    } catch (Exception e) {
        System.err.println(e.getLocalizedMessage());
    }
}
```

#### Step 4. Restore the stack in another time another place. Continue using as usual

**See, that we not only restore stack but migrate our subject's properties to the new version of it!**

```java
// Get string
String store = new String(Files.readAllBytes(Paths.get("./undo.txt")));

stack = UndoPacket
        // Check whether we got appropriate stack
        .peek(store, subjInfo -> IDS_STACK.equals(subjInfo.id))
        // Manual restoring (because we store non-serializable type)
        .restore((processedSubj, subjInfo) -> {
            // First, manual tune for restoring types from string
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
        })
        .stack((stack, subjInfo) -> {
            // Restore new local contexts
            stack.getLocalContexts().put(BaseTab.UndoBulk.IDS_RES, new Resources_V2());
            stack.getLocalContexts().put(BaseTab.UndoBulk.IDS_COLOR_PICKER, tab.colorPicker);
            stack.getLocalContexts().put(BaseTab.UndoBulk.IDS_RADIUS_SLIDER, tab.radius);
            stack.getLocalContexts().put(BaseTab.UndoBulk.IDS_X_SLIDER, tab.centerX);
            stack.getLocalContexts().put(BaseTab.UndoBulk.IDS_Y_SLIDER, tab.centerY);
        });

// Process case when we don't restore stack
if(null == stack)
    stack = new UndoStack(tab.shape, null);
// Restore watcher
stack.setWatcher(this);
```

Next connection to app's widgets and actions - as in **Step 2. Commands and stack linking**.
**Voila!**

As you see if you take time for design you get simple and elegant undo system.

- - -

==The library has a lot of tests with using techniques. See them in the code.==