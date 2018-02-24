package com.gdetotut.samples.jundo.javacon;

import com.gdetotut.jundo.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.*;
import java.util.function.Function;

import static com.gdetotut.samples.jundo.javacon.UndoMacrosSample.*;
import static com.gdetotut.samples.jundo.javacon.UndoMacrosSample.Side.ResId.*;

/**
 * Пример демонстрирует технику реализации параметризованных {@link UndoCommand} и макросов на их основе.
 * <p>Клиент может использовать макросы повторно с разными параметрами.
 * <p>Кроме этого, в примере активно используется техника локальных контекстов применимо к строковым ресурсам
 * и использующимся функциям. Это позволяет гибко привязываться к новому окружению после упаковки/распаковки стеков.
 *
 * В коде используется 2 "документа" {@link Doc}, каждый из которых в процессе работы с параметризованной командой
 * создает по макросу.
 * <p>Макросы размещаются в общую карту {@link Macros}, и используются повторно каждым документом.
 * Важной характеристикой макросов является их независимость от породившего стека, таким образом макрос может
 * использоваться повторно другими стеками.
 *
 * <p>Во второй части примера происходит распаковка стека и демонстрация привязки к новому контексту -
 * другая локализация, другие функции обратного вызова.
 */
public class UndoMacrosSample {

    public static void main(String[] args) throws Exception {

        // Do some work on A-side
        SideEn sideE = new SideEn();
        System.out.println("--- E-side (ENG) ---");
        System.out.println(sideE.print());
        System.out.println("--- 1. Fill docs with results ---");
        sideE.doingWork();
        System.out.println(sideE.print());
        System.out.println("--- 2. Apply all macros to every doc ---");
        sideE.applyMacros();
        System.out.println(sideE.print());


        System.out.println("--- 3. Pack and move to R-side... ---");
        Map<String, String> packets = sideE.pack();
        //
        SideRu sideR = new SideRu();
        sideR.restore(packets);
        System.out.println("\n--- R-сторона (RUS) ---");
        System.out.println(sideR.print());
        System.out.println("\n--- 4. Работаем с переданными макросами: ---");
        sideR.doingWorkAgain();
        System.out.println(sideR.print());

        System.out.println("\n--- 5. Очистим: ---");
        sideR.clearDocs();
        System.out.println(sideR.print());

        System.out.println("\n--- 6. И снова макросами: ---");
        sideR.doingWorkAgain();
        System.out.println(sideR.print());
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * Utility class for handy using of locale resources.
     */
    static class Res {
        public final TreeMap<Integer, String> items = new TreeMap<>();
    }

    /**
     * Utility class for handy using of macros.
     */
    static class Macros implements Serializable {
        public final TreeMap<String, UndoMacro> items = new TreeMap<>();
    }

    /**
     * Псевдодокумент, содержащий результаты работы арифметических методов.
     *
     * Sample class for macros example.
     *
     */
    public static class Doc {

        final String id;
        public final List<String> text = new ArrayList<>();

        Doc(String id) {
            this.id = id;
        }

        /**
         * Adds substring to current position.
         */
        public void add(String s) {
            String cur = text.size() != 0 ? text.remove(text.size() - 1) : "";
            cur += s;
            text.add(cur);
        }

        /**
         * Removes substring from current position
         */
        public void remove(String s) {
            String cur = text.remove(text.size() - 1);
            cur = cur.substring(0, cur.length() - s.length());
            text.add(cur);
        }

        /**
         * Adds new line
         */
        public void addLine() {
            text.add("");
        }

        /**
         * Removes last line.
         */
        public void removeLine() {
            if(text.size() > 0) {
                text.remove(text.size() - 1);
            }
        }

        /**
         * Resets current text with value.
         */
        public void reset(List<String> value) {
            text.clear();
            text.addAll(value);
        }

        /**
         * Prints content of object to string.
         */
        public String print() {
            StringBuilder builder = new StringBuilder();
            builder.append(id).append("\n");
            for (String s : text) {
                builder.append(s).append("\n");
            }
            return builder.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Doc that = (Doc) o;
            return Objects.equals(text, that.text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(text);
        }

    }

    /**
     * Base class for concept of so-called double-siding of {@link Doc} representation.
     *
     * <p>First side named {@link SideEn} uses english localization when second one named {@link SideRu}
     * uses russian one.
     */
    abstract public static class Side {

        // Some identifiers for local contexts and repacking procedure.

        /**
         * The squaring method identifier.
         */
        static final String FUN_SQUARE = "square";

        /**
         * The cubing method identifier.
         */
        static final String FUN_CUBE = "cube";

        /**
         * The {@link Res} list identifier.
         */
        static final String RES = "res";

        /**
         * The {@link Macros} list identifier.
         */
        static final String MACROS = "macros";

        /**
         * The delimiter constant.
         */
        static final String DELIM = "~~!:!:!:!~~";

        // ~Some identifiers for local contexts and repacking procedure.

        /**
         * Идентификаторы строковых ресурсов в локализациях.
         *
         * String resources identifiers for locales.
         */
        enum ResId {
            RI_Name,
            RI_Docs,
            RI_DocName,
            RI_SqPrompt,
            RI_CbPrompt,
            RI_Result,
            RI_FunSqName,
            RI_FunCbName
        }


        /**
         * {@link Doc} ids for two docs.
         */
        static String[] dids = new String[]{"Doc_1", "Doc_2"};

        /**
         * Stack identifiers for manipulates stacks in the list.
         */
        static String[] sids = new String[] {"stackSquaringDoc", "stackCubingDoc"};

        /**
         * Stack identifiers for manipulates macro in the list.
         */
        static String[] mids = new String[]{"make square", "make cube"};

        /**
         * Stack map for manipulates stacks.
         */
        Map<String, UndoStack> stackMap = new TreeMap<>();

        /**
         * So-called resource map holder.
         */
        Res res = new Res();

        /**
         * So-called macro map holder.
         */
        Macros macros = new Macros();

        /**
         * Holds packed stack.
         */
        String packet;

        /**
         * Map for stacks manipulating.
         */
        Map<String, String> packets = new HashMap<>();

        /**
         * List for docs manipulating.
         */
        static List<Doc> docs = new ArrayList<>();

        /**
         * @return Locale string by its identifier.
         */
        String getPromptById(int id){
            return res.items.get(id);
        }

        /**
         * Squaring method. Is used in {@link UndoCommand} and macros for parameterized access.
         */
        String square(int promptId) {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            Integer param = null;
            boolean success = false;
            while (!success) {
                System.out.print(getPromptById(promptId) + ": ");
                try {
                    param = Integer.parseInt(br.readLine());
                    success = true;
                } catch (IOException e) {
                    System.err.println(e.getLocalizedMessage());
                }
            }
            return String.format("%d^2 = %d", param, param * param);
        }

        /**
         * Cubing method. Is used in {@link UndoCommand} and macros for parameterized access.
         */
        String cube(int promptId) {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            Integer param = null;
            boolean success = false;
            while (!success) {
                System.out.print(getPromptById(promptId) + ": ");
                try {
                    param = Integer.parseInt(br.readLine());
                    success = true;
                } catch (IOException e) {
                    System.err.println(e.getLocalizedMessage());
                }
            }
            return String.format("%d^3 = %d", param, param * param * param);
        }

        /**
         * Forces printing the entire object content.
         */
        String print() {
            StringBuilder builder = new StringBuilder();
            builder.append(String.format("%s : %s", res.items.get(RI_Name.ordinal()), getClass().getCanonicalName()));
            if (docs.size() > 0) {
                builder.append("\n--- ").append(res.items.get(RI_Docs.ordinal())).append(" ---\n");
                int i = 0;
                for (Doc doc : docs) {
                    builder
                            .append(res.items.get(RI_DocName.ordinal())).append(" #").append(i++).append(": ")
                            .append(doc.print()).append("\n");
                }
            }
            return builder.toString();
        }

    }

    /**
     * Represents so-called first or 'english' side of the sample.
     * <p>Has methods for data manipulating and stacks packing.
     */
    static class SideEn extends Side {

        {
            res.items.put(RI_Name.ordinal(), "Side A.");
            res.items.put(RI_Docs.ordinal(), "Side A. Documents");
            res.items.put(RI_DocName.ordinal(), "Side A. Document");
            res.items.put(RI_SqPrompt.ordinal(), "Side A. Enter Integer for square");
            res.items.put(RI_CbPrompt.ordinal(), "Side A. Enter Integer for cube");
            res.items.put(RI_Result.ordinal(), "(on A-side)");
            res.items.put(RI_FunSqName.ordinal(), "make square");
            res.items.put(RI_FunCbName.ordinal(), "make cube");
        }

        /**
         * Makes first manipulating, creates macros.
         */
        void doingWork() throws Exception {
            createSquare();
            createCube();
        }

        /**
         * Creates docs, stacks and fills them with initial data.
         */
        private UndoStack createDoc(int idx) {
            // Помещаем документ "SummingDoc" в лист
            Doc doc = new Doc(dids[idx]);
            docs.add(doc);
            // Creates stack, its contexts and puts it all together.
            UndoStack stack = new UndoStack(doc);
            stack.getLocalContexts().put(RES, res);
            stack.getLocalContexts().put(FUN_SQUARE, (Function<Integer, String>) this::square);
            stack.getLocalContexts().put(FUN_CUBE, (Function<Integer, String>) this::cube);
            stackMap.put(sids[idx], stack);
            return stack;
        }

        /**
         * Creates first commands for docs[0] and macro[0] with them.
         */
        private void createSquare() throws Exception {

            // Create doc & stack
            int idx = 0;
            UndoStack stack = createDoc(idx);

            // create first undoes & macro
            stack.beginMacro(mids[idx]);
            stack.push(new DocCommands.AddLine("line"));
            stack.push(new DocCommands.AddString("string", RI_FunSqName.ordinal()));
            stack.push(new DocCommands.CalcFunc<Integer>("square",
                    FUN_SQUARE, RI_SqPrompt.ordinal(), RI_Result.ordinal()));
            stack.endMacro();
            // Puts macro it the global list.
            macros.items.put(mids[idx], stack.getMacro(0));

        }

        /**
         * Creates the commands for docs[1] and macro[1] with them.
         */
        private void createCube() throws Exception {
            // Create doc & stack
            int idx = 1;
            UndoStack stack = createDoc(idx);

            // create first undoes & macro
            stack.beginMacro(mids[idx]);
            stack.push(new DocCommands.AddLine("line"));
            stack.push(new DocCommands.AddString("string", RI_FunCbName.ordinal()));
            stack.push(new DocCommands.CalcFunc<Integer>("cube",
                    FUN_CUBE, RI_CbPrompt.ordinal(), RI_Result.ordinal()));
            stack.endMacro();
            // Puts macro it the global list.
            macros.items.put(mids[idx], stack.getMacro(0));
        }

        /**
         * Applies standalone saved macros to the docs.
         */
        void applyMacros() throws Exception {
            for (UndoStack stack: stackMap.values()) {
                for (UndoMacro macro : macros.items.values() ) {
                    stack.push(macro);
                }
            }
        }

        /**
         * @return Packs and returns stacks as a map.
         */
        Map<String, String> pack() throws Exception {
            for (int i = 0; i < docs.size(); ++i) {
                packet = UndoPacket
                        // Good practice to pass identifier and version to UndoPacket
                        .make(stackMap.get(sids[i]), sids[i], 1)
                        // As the subject has non-serializable type we save it as a proxy-string.
                        .onStore(subj -> String.join(DELIM, ((Doc) subj).text))
                        // Pass macros as an extra parameter.
                        .extra(MACROS, macros)
                        .zipped(true)
                        .store();
                packets.put(sids[i], packet);
            }
            return packets;
        }

    }

    /**
     * Represents so-called second or 'russian' side of the sample.
     * <p>Has methods for data manipulating and stacks packing.
     */
    static class SideRu extends Side {

        {
            res.items.put(RI_Name.ordinal(), "Сторона Б. ");
            res.items.put(RI_Docs.ordinal(), "Сторона Б. Документы");
            res.items.put(RI_DocName.ordinal(), "Сторона Б. Документ");
            res.items.put(RI_SqPrompt.ordinal(), "Сторона Б. Введите значение для квадрата");
            res.items.put(RI_CbPrompt.ordinal(), "Сторона Б. Введите значение для куба");
            res.items.put(RI_Result.ordinal(), "(на Б-стороне)");
            res.items.put(RI_FunSqName.ordinal(), "квадратим");
            res.items.put(RI_FunCbName.ordinal(), "кубим");
        }

        /**
         * Restores map of string as a map of local stacks.
         * <p>Meanwhile adjusts them.
         */
        void restore(Map<String, String> packets) throws CreatorException {
            docs.clear();
            for (int i = 0; i < packets.size(); ++i) {
                final int idx = i;
                packet = packets.get(Side.sids[idx]);
                UndoStack stack = UndoPacket
                        // This check allows eliminate further unpacking if packet is wrong.
                        .peek(packet, subjInfo -> subjInfo.id.equals(Side.sids[idx]))
                        .restore((processedSubj, subjInfo) -> {
                            // Restore macros.
                            Object o = subjInfo.extras.get(MACROS);
                            if (o != null && o instanceof Macros) {
                                Macros src = (Macros) o;
                                for (Map.Entry<String, UndoMacro> es: src.items.entrySet()) {
                                    macros.items.put(es.getKey(), es.getValue());
                                }
                            }
                            // Work only if version is correct.
                            if (subjInfo.version == 1) {
                                // Restore subject from proxy-string.
                                String s = (String) processedSubj;
                                List<String> text = Arrays.asList(s.split(DELIM));
                                Doc doc = new Doc(dids[idx]);
                                doc.reset(text);
                                return doc;
                            }
                            // Return null if something is wrong...
                            return null;
                            // ...and use default creator.
                        }, () -> new UndoStack(new Doc(dids[idx])))
                        .prepare((stackBack, subjInfo, result) -> {
                            if (result.code != UndoPacket.UnpackResult.UPR_Success) {
                                // Make message for client if something was wrong.
                                System.err.println(result.code + " <- " + result.msg);
                            }
                            // Adjust just recreated stack.
                            stackBack.getLocalContexts().put(FUN_SQUARE, (Function<Integer, String>) this::square);
                            stackBack.getLocalContexts().put(FUN_CUBE, (Function<Integer, String>) this::cube);
                            stackBack.getLocalContexts().put(RES, res);
                        });
                // Put the stack to the map.
                stackMap.put(sids[idx], stack);
                // Place doc to the list.
                docs.add((Doc) stack.getSubj());
            }
        }

        /**
         * Applies macros to the stacks once more.
         */
        void doingWorkAgain() throws Exception {
            for (UndoStack stack: stackMap.values()) {
                for (UndoMacro macro : macros.items.values() ) {
                    stack.push(macro);
                }
            }
        }

        /**
         * Rolls docs back to the empty state.
         */
        void clearDocs() throws Exception {
            for (UndoStack stack: stackMap.values()) {
                while (stack.getIdx() > 0) {
                    stack.undo();
                }
            }
        }

    }

}


/**
 * Common class for all {@link Doc} undo commands.
 *
 * Команды для {@link Doc}
 */
class DocCommands implements Serializable {

    /**
     * Adds new line to {@link Doc}
     */
    public static class AddLine extends UndoCommand {

        AddLine(String caption) {
            super(caption);
        }

        @Override
        protected void doRedo() {
            Object o = getOwner().getSubj();
            if (o != null && o instanceof Doc) {
                Doc doc = (Doc) getOwner().getSubj();
                doc.addLine();
            }
        }

        @Override
        protected void doUndo() {
            Object o = getOwner().getSubj();
            if (o != null && o instanceof Doc) {
                Doc doc = (Doc) getOwner().getSubj();
                doc.removeLine();
            }
        }
    }

    /**
     * Adds string to current line of text.
     */
    public static class AddString extends UndoCommand {

        private final Integer resId;
        private String text;
        private boolean init = false;

        AddString(String caption, int resId) {
            super(caption);
            this.resId = resId;
        }

        @Override
        protected void doRedo() {
            Object o = getOwner().getSubj();
            if (o == null || !(o instanceof Doc)) {
                return;
            }

            Doc doc = (Doc) getOwner().getSubj();
            if (init) {
                doc.add(String.valueOf(text));
            } else {
                // Here we use local context for correct localization
                Object o2 = getOwner().getLocalContexts().get("res");
                if (o2 != null && o2 instanceof Res) {
                    Res res = (Res) o2;
                    text = String.format("%s: ", res.items.get(resId));
                } else {
                    text = String.format("%s: ", String.valueOf(resId));
                }
                doc.add(text);
                init = true;
            }
        }

        @Override
        protected void doUndo() {
            Object o = getOwner().getSubj();
            if (o == null || !(o instanceof Doc) || !init) {
                return;
            }
            Doc doc = (Doc) getOwner().getSubj();
            doc.remove(text);
        }
    }

    /**
     * Uses outer callback method with parameters as source of value.
     * <p>The method available as one of local contexts.
     *
     * @param <R> type of return value.
     */
    public static class CalcFunc<R extends Serializable> extends UndoCommand {

        private final String funcId;
        private final Integer promptId;
        private final Integer prefixId;
        private String text;
        private boolean init = false;

        CalcFunc(String caption, String funcId, Integer promptId, Integer prefixId) {
            super(caption);
            this.funcId = funcId;
            this.promptId = promptId;
            this.prefixId = prefixId;
        }

        @Override
        protected void doRedo() {
            Object o = getOwner().getSubj();
            if (o == null || !(o instanceof Doc)) {
                return;
            }

            Doc doc = (Doc) getOwner().getSubj();
            if (init) {
                doc.add(String.valueOf(text));
            } else {
                // Here we use local contexts for appropriate objects:
                // 1. Local callback for the parameterized using.
                Object f = getOwner().getLocalContexts().get(funcId);
                if (f != null && f instanceof Function) {
                    Function<Integer, R> fn = (Function<Integer, R>) f;
                    R val = fn.apply(promptId);
                    // 2. Local locale resource.
                    Object o2 = getOwner().getLocalContexts().get("res");
                    if (o2 != null && o2 instanceof Res) {
                        Res res = (Res) o2;
                        text = String.format("%s: %s", res.items.get(prefixId), String.valueOf(val));
                    } else {
                        text = String.valueOf(val);
                    }
                    doc.add(text);
                    init = true;
                }
            }
        }

        @Override
        protected void doUndo() {
            Object o = getOwner().getSubj();
            if (o == null || !(o instanceof Doc) || !init) {
                return;
            }
            Doc doc = (Doc) getOwner().getSubj();
            doc.remove(text);
        }
    }

}


