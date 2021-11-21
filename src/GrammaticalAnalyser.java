import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class GrammaticalAnalyser {
    private ArrayList<Word> words;
    private int index = 0;
    private Word curWord;
    private ArrayList<String> grammar;

    private HashMap<Integer, Symbols> symbols = new HashMap<>();
    private HashMap<String, Function> functions = new HashMap<>();
    private ArrayList<Error> errors = new ArrayList<>();
    private int area = -1;
    private boolean needReturn = false;
    private int whileFlag = 0;

    public GrammaticalAnalyser(ArrayList<Word> words) {
        this.words = words;
        grammar = new ArrayList<>();
        analyseCompUnit();
    }

    private void getWord() {
        curWord = words.get(index);
        grammar.add(curWord.toString());
        index++;
    }

    private void getWordWithoutAddToGrammar() {
        curWord = words.get(index);
        index++;
    }

    private Word getNextWord() {
        return words.get(index);
    }

    private Word getNext2Word() {
        return words.get(index + 1);
    }

    private Word getNext3Word() {
        return words.get(index + 2);
    }

    private void analyseCompUnit() {
        addArea();

        Word word = getNextWord();
        while (word.typeEquals("CONSTTK") || (
                word.typeEquals("INTTK") && getNext2Word().typeEquals("IDENFR") && !getNext3Word().typeEquals("LPARENT"))) {

            analyseDecl();
            word = getNextWord();
        }
        while (word.typeEquals("VOIDTK") || (
                (word.typeEquals("INTTK") && !getNext2Word().typeEquals("MAINTK")))) {
            analyseFuncDef();
            word = getNextWord();
        }
        if (word.typeEquals("INTTK") && getNext2Word().typeEquals("MAINTK")) {
            analyseMainFuncDef();
        } else {
            error();
        }

        removeArea();
        grammar.add("<CompUnit>");
    }

    private void analyseDecl() {
        Word word = getNextWord();
        if (word.typeEquals("CONSTTK")) {
            analyseConstDecl();
        } else if (word.typeEquals("INTTK")) {
            analyseVarDecl();
        } else {
            error();
        }
    }

    private void analyseConstDef() {
        getWord(); //Ident
        Word ident = curWord;
        if (hasSymbolInThisArea(curWord)) {
            error("b");
        }
        int intType = 0;
        Word word = getNextWord();
        while (word.typeEquals("LBRACK")) {
            intType++;
            getWord(); //[
            analyseConstExp(getExp());
            checkBrack(); //]
            word = getNextWord();
        }
        addSymbol(ident, "const", intType);
        getWord();//=
        analyseConstInitVal();
        grammar.add("<ConstDef>");
    }

    private void analyseFuncDef() {
        Function function = null;
        ArrayList<Integer> paras = new ArrayList<>();
        String returnType = analyseFuncType();
        getWord(); //Ident
        if (functions.containsKey(curWord.getContent())) {
            error("b");
        }
        function = new Function(curWord, returnType);

        addArea();

        getWord();//(
        Word word = getNextWord();
        if (word.typeEquals("VOIDTK") || word.typeEquals("INTTK")) { //changed from !word.typeEquals("RPARENT")
            paras = analyseFuncFParams();
        }
        checkParent(); //)
        function.setParas(paras);
        functions.put(function.getContent(), function);
        needReturn = function.getReturnType().equals("int");
        boolean isReturn = analyseBlock(true);
        if (needReturn && !isReturn) {
            error("g");
        }

        removeArea();

        grammar.add("<FuncDef>");
    }

    private void analyseMainFuncDef() {
        getWord();//int
        getWord();//main
        if (functions.containsKey(curWord.getContent())) {
            error("b");
        } else {
            Function function = new Function(curWord, "int");
            function.setParas(new ArrayList<>());
            functions.put("main", function);
        }
        getWord();//(
        checkParent(); //)
        needReturn = true;
        boolean isReturn = analyseBlock(false);
        if (needReturn && !isReturn) {
            error("g");
        }
        grammar.add("<MainFuncDef>");
    }

    private boolean analyseBlock(boolean fromFunc) {
        getWord();//{
        if (!fromFunc) {
            addArea();
        }
        Word word = getNextWord();
        boolean isReturn = false;
        while (word.typeEquals("CONSTTK") || word.typeEquals("INTTK") || word.typeSymbolizeStmt()) {
            if (word.typeEquals("CONSTTK") || word.typeEquals("INTTK")) {
                isReturn = analyseBlockItem();
            } else {
                isReturn = analyseStmt();
            }
            word = getNextWord();
        }
        getWord();//}
        if (!fromFunc) {
            removeArea();
        }
        grammar.add("<Block>");
        return isReturn;
    }

    private boolean analyseBlockItem() {
        Word word = getNextWord();
        boolean isReturn = false;
        if (word.typeEquals("CONSTTK") || word.typeEquals("INTTK")) {
            analyseDecl();
        } else {
            isReturn = analyseStmt();
        }
        return isReturn;
    }

    private void checkSemicn() {
        if (getNextWord().typeEquals("SEMICN")) {
            getWord();//;
        } else {
            error("i");
        }
    }

    private boolean analyseStmt() {
        boolean isReturn = false;
        Word word = getNextWord();
        if (word.typeEquals("IDENFR")) {
            ArrayList<Word> exp = getExp();
            if (getNextWord().typeEquals("ASSIGN")) {  //changed from ! eq "SEMICN"
                analyseLVal(exp);
                if (isConst(word)) {
                    error("h", word.getLineNum());
                }
                getWord();//=
                if (getNextWord().typeEquals("GETINTTK")) {
                    getWord();//getint
                    getWord();//(
                    checkParent();//)
                    checkSemicn(); //;
                } else {
                    analyseExp(getExp());//
                    checkSemicn(); //;
                }
            } else {
                analyseExp(exp);
                checkSemicn();//;
            }
        } else if (word.typeSymbolizeBeginOfExp()) {
            analyseExp(getExp());
            checkSemicn();//;
        } else if (word.typeEquals("LBRACE")) {
            analyseBlock(false);
        } else if (word.typeEquals("IFTK")) {
            getWord();//if
            getWord();//(
            analyseCond();
            checkParent(); //)
            analyseStmt();
            word = getNextWord();
            if (word.typeEquals("ELSETK")) {
                getWord(); //else
                analyseStmt();
            }
        } else if (word.typeEquals("WHILETK")) {
            getWord();//while
            whileFlag++;
            getWord();//(
            analyseCond();
            checkParent(); //)
            analyseStmt();
            whileFlag--;
        } else if (word.typeEquals("BREAKTK")) {
            getWord();//break
            if (whileFlag == 0) {
                error("m");
            }
            checkSemicn(); //;
        } else if (word.typeEquals("CONTINUETK")) {
            getWord();//continue
            if (whileFlag == 0) {
                error("m");
            }
            checkSemicn(); //;
        } else if (word.typeEquals("RETURNTK")) {
            getWord();//return
            isReturn = true;
            if (getNextWord().typeSymbolizeBeginOfExp()) {
                if (!needReturn) {
                    error("f");
                }
                analyseExp(getExp());
            }
            checkSemicn();//;
        } else if (word.typeEquals("PRINTFTK")) {
            getWord();//printf
            Word printftk = curWord;
            getWord();//(
            getWord();//STRCON
            Word strcon = curWord;
            word = getNextWord();
            int para = 0;
            while (word.typeEquals("COMMA")) {
                getWord();//,
                analyseExp(getExp());
                para++;
                word = getNextWord();
            }
            if (strcon.isFormatIllegal()) {
                error("a", strcon.getLineNum());
            }
            if (para != strcon.getFormatNum()) {
                error("l", printftk.getLineNum());
            }
            checkParent(); //)
            checkSemicn(); //;
        } else if (word.typeEquals("SEMICN")) {
            getWord();//;
        }
        grammar.add("<Stmt>");
        return isReturn;
    }

    private ArrayList<Integer> analyseFuncFParams() {
        ArrayList<Integer> paras = new ArrayList<>();
        int paraType = analyseFuncFParam();
        paras.add(paraType);
        Word word = getNextWord();
        while (word.typeEquals("COMMA")) {
            getWord();//,
            paraType = analyseFuncFParam();
            paras.add(paraType);
            word = getNextWord();
        }
        grammar.add("<FuncFParams>");
        return paras;
    }

    private int analyseFuncFParam() {
        int paraType = 0;
        getWord();//void|int
        getWord();//Ident
        Word ident = curWord;
        if (hasSymbolInThisArea(curWord)) {
            error("b");
        }
        Word word = getNextWord();
        if (word.typeEquals("LBRACK")) {
            paraType++;
            getWord();//[
            checkBrack();//]
            word = getNextWord();
            while (word.typeEquals("LBRACK")) {
                paraType++;
                getWord();//[
                analyseConstExp(getExp());
                checkBrack();//]
                word = getNextWord();
            }
        }
        addSymbol(ident, "para", paraType);
        grammar.add("<FuncFParam>");
        return paraType;
    }

    private String analyseFuncType() {
        getWord(); // void|int
        grammar.add("<FuncType>");
        return curWord.getContent();
    }

    private void analyseConstInitVal() {
        Word word = getNextWord();
        if (word.typeEquals("LBRACE")) {
            getWord();//{
            word = getNextWord();
            if (!word.typeEquals("RBRACE")) {
                analyseConstInitVal();
                Word word1 = getNextWord();
                while (word1.typeEquals("COMMA")) {
                    getWord();//,
                    analyseConstInitVal();
                    word1 = getNextWord();
                }
            }
            getWord();//}
        } else {
            analyseConstExp(getExp());
        }
        grammar.add("<ConstInitVal>");
    }

    private void analyseBType() {
        //
    }

    private void analyseConstDecl() {
        getWord(); // const
        getWord(); // int
        if (curWord.typeEquals("INTTK")) {
            analyseBType();
        } else {
            error();
        }
        analyseConstDef();
        Word word = getNextWord();
        while (word.typeEquals("COMMA")) {
            getWord(); //,
            analyseConstDef();
            word = getNextWord();
        }
        if (getNextWord().typeEquals("SEMICN")) {
            getWord();//;
        } else {
            error("i");
        }
        grammar.add("<ConstDecl>");
    }

    private void analyseVarDecl() {
        getWord();//int
        analyseVarDef();
        Word word = getNextWord();
        while (word.typeEquals("COMMA")) {
            getWord();//,
            analyseVarDef();
            word = getNextWord();
        }
        if (getNextWord().typeEquals("SEMICN")) {
            getWord();//;
        } else {
            error("i");
        }
        grammar.add("<VarDecl>");
    }

    private void analyseVarDef() {
        getWord();//Ident
        Word ident = curWord;
        if (hasSymbolInThisArea(curWord)) {
            error("b");
        }
        int intType = 0;
        Word word = getNextWord();
        while (word.typeEquals("LBRACK")) {
            intType++;
            getWord();//[
            analyseConstExp(getExp());
            checkBrack(); //]
            word = getNextWord();
        }
        if (word.typeEquals("ASSIGN")) {
            getWord();//=
            analyseInitVal();
        }
        addSymbol(ident, "var", intType);
        grammar.add("<VarDef>");
    }

    private void checkBrack() {
        if (getNextWord().typeEquals("RBRACK")) {
            getWord();//]
        } else {
            error("k");
        }
    }

    private void checkParent() {
        if (getNextWord().typeEquals("RPARENT")) {
            getWord();//)
        } else {
            error("j");
        }
    }

    private void analyseInitVal() {
        Word word = getNextWord();
        if (word.typeEquals("LBRACE")) {
            getWord();//{
            word = getNextWord();
            if (!word.typeEquals("RBRACK")) {
                analyseInitVal();
                Word word1 = getNextWord();
                while (word1.typeEquals("COMMA")) {
                    getWord();//,
                    analyseInitVal();
                    word1 = getNextWord();
                }
            }
            getWord();//}
        } else {
            analyseExp(getExp());
        }
        grammar.add("<InitVal>");
    }

    private int analyseExp(ArrayList<Word> exp) {
        int intType = analyseAddExp(exp);
        grammar.add("<Exp>");
        return intType;
    }

    private void analyseCond() {
        analyseLOrExp(getExp());
        grammar.add("<Cond>");
    }

    private void analyseFuncRParams(Word ident, ArrayList<Word> exp, ArrayList<Integer> paras) {
        ArrayList<Integer> rparas = new ArrayList<>();
        Exps exps = divideExp(exp, new ArrayList<>(Arrays.asList("COMMA")));
        int j = 0;
        for (ArrayList<Word> exp1 : exps.getWords()) {
            int intType = analyseExp(exp1);
            rparas.add(intType);
            if (j < exps.getSymbols().size()) {
                grammar.add(exps.getSymbols().get(j++).toString());
            }
        }
        if (paras != null) {
            checkParasMatchRParas(ident, paras, rparas);
        }
        grammar.add("<FuncRParams>");
    }

    private void checkParasMatchRParas(Word ident, ArrayList<Integer> paras, ArrayList<Integer> rparas) {
        if (paras.size() != rparas.size()) {
            error("d", ident.getLineNum());
        } else {
            for (int i = 0; i < paras.size(); i++) {
                if (!paras.get(i).equals(rparas.get(i))) {
                    error("e", ident.getLineNum());
                }
            }
        }
    }

    private void analyseRelExp(ArrayList<Word> exp) {
        Exps exps = divideExp(exp, new ArrayList<>(Arrays.asList("LSS", "LEQ", "GRE", "GEQ")));
        int j = 0;
        for (ArrayList<Word> exp1 : exps.getWords()) {
            analyseAddExp(exp1);
            grammar.add("<RelExp>");
            if (j < exps.getSymbols().size()) {
                grammar.add(exps.getSymbols().get(j++).toString());
            }
        }
    }

    private void analyseEqExp(ArrayList<Word> exp) {
        Exps exps = divideExp(exp, new ArrayList<>(Arrays.asList("EQL", "NEQ")));
        int j = 0;
        for (ArrayList<Word> exp1 : exps.getWords()) {
            analyseRelExp(exp1);
            grammar.add("<EqExp>");
            if (j < exps.getSymbols().size()) {
                grammar.add(exps.getSymbols().get(j++).toString());
            }
        }
    }

    private void analyseLAndExp(ArrayList<Word> exp) {
        Exps exps = divideExp(exp, new ArrayList<>(Arrays.asList("AND")));
        int j = 0;
        for (ArrayList<Word> exp1 : exps.getWords()) {
            analyseEqExp(exp1);
            grammar.add("<LAndExp>");
            if (j < exps.getSymbols().size()) {
                grammar.add(exps.getSymbols().get(j++).toString());
            }
        }
    }

    private void analyseLOrExp(ArrayList<Word> exp) {
        Exps exps = divideExp(exp, new ArrayList<>(Arrays.asList("OR")));
        int j = 0;
        for (ArrayList<Word> exp1 : exps.getWords()) {
            analyseLAndExp(exp1);
            grammar.add("<LOrExp>");
            if (j < exps.getSymbols().size()) {
                grammar.add(exps.getSymbols().get(j++).toString());
            }
        }
    }

    private int analyseLVal(ArrayList<Word> exp) {
        int intType = 0;
        Word ident = exp.get(0);//Ident
        if (!hasSymbol(ident)) {
            error("c", ident.getLineNum());
        }
        grammar.add(ident.toString());
        if (exp.size() > 1) {
            ArrayList<Word> exp1 = new ArrayList<>();
            int flag = 0;
            for (int i = 1; i < exp.size(); i++) {
                Word word = exp.get(i);
                if (word.typeEquals("LBRACK")) {
                    intType++;
                    flag++;
                    if (flag == 1) {
                        grammar.add(word.toString());
                        exp1 = new ArrayList<>();
                    } else {
                        exp1.add(word);
                    }
                } else if (word.typeEquals("RBRACK")) {
                    flag--;
                    if (flag == 0) {
                        analyseExp(exp1);
                        grammar.add(word.toString());
                    } else {
                        exp1.add(word);
                    }
                } else {
                    exp1.add(word);
                }
            }
            if (flag > 0) {
                analyseExp(exp1);
                error("k", exp.get(exp.size() - 1).getLineNum()); // not sure
            }
        }
        grammar.add("<LVal>");
        if (hasSymbol(ident)) {
            return getSymbol(ident).getIntType() - intType;
        } else {
            return 0;
        }
    }

    private void analyseNumber(Word word) {
        grammar.add(word.toString());
        grammar.add("<Number>");
    }

    private int analysePrimaryExp(ArrayList<Word> exp) {
        int intType = 0;
        Word word = exp.get(0);
        if (word.typeEquals("LPARENT")) {
            //remove ( )
            grammar.add(exp.get(0).toString());
            analyseExp(new ArrayList<>(exp.subList(1, exp.size() - 1)));
            grammar.add(exp.get(exp.size() - 1).toString());
        } else if (word.typeEquals("IDENFR")) {
            intType = analyseLVal(exp);
        } else if (word.typeEquals("INTCON")) {
            analyseNumber(exp.get(0));
        } else {
            error();
        }
        grammar.add("<PrimaryExp>");
        return intType;
    }

    private int analyseUnaryExp(ArrayList<Word> exp) {
        int intType = 0;
        Word word = exp.get(0);
        if (word.typeEquals("PLUS") || word.typeEquals("MINU") || word.typeEquals("NOT")) {
            //UnaryOp UnaryExp
            analyseUnaryOp(exp.get(0)); //remove UnaryOp
            analyseUnaryExp(new ArrayList<>(exp.subList(1, exp.size())));
        } else if (exp.size() == 1) {
            //PrimaryExp
            intType = analysePrimaryExp(exp);
        } else {
            if (exp.get(0).typeEquals("IDENFR") && exp.get(1).typeEquals("LPARENT")) {
                // Ident '(' [FuncRParams] ')'
                Word ident = exp.get(0);
                ArrayList<Integer> paras = null;
                if (!hasFunction(ident)) {
                    error("c", ident.getLineNum());
                } else {
                    paras = getFunction(ident).getParas();
                }
                if (!exp.get(exp.size() - 1).typeEquals("RPARENT")) {
                    exp.add(new Word(")", curWord.getLineNum()));
                    error("j");
                }
                //Ident ( )
                grammar.add(exp.get(0).toString());
                grammar.add(exp.get(1).toString());
                if (exp.size() > 3) {
                    analyseFuncRParams(ident, new ArrayList<>(exp.subList(2, exp.size() - 1)), paras);
                } else {
                    if (paras != null) {
                        if (paras.size() != 0) {
                            error("d", ident.getLineNum());
                        }
                    }
                }
                grammar.add(exp.get(exp.size() - 1).toString());

                if (hasFunction(ident)) {
                    if (getFunction(ident).getReturnType().equals("void")) {
                        intType = -1;
                    }
                }
            } else {
                //PrimaryExp
                intType = analysePrimaryExp(exp);
            }
        }
        grammar.add("<UnaryExp>");
        return intType;
    }

    private void analyseUnaryOp(Word word) {
        grammar.add(word.toString());
        grammar.add("<UnaryOp>");
    }

    private Exps divideExp(ArrayList<Word> exp, ArrayList<String> symbol) {
        ArrayList<ArrayList<Word>> exps = new ArrayList<>();
        ArrayList<Word> exp1 = new ArrayList<>();
        ArrayList<Word> symbols = new ArrayList<>();
        boolean unaryFlag = false;
        int flag1 = 0;
        int flag2 = 0;
        for (int i = 0; i < exp.size(); i++) {
            Word word = exp.get(i);
            if (word.typeEquals("LPARENT")) {
                flag1++;
            }
            if (word.typeEquals("RPARENT")) {
                flag1--;
            }
            if (word.typeEquals("LBRACK")) {
                flag2++;
            }
            if (word.typeEquals("RBRACK")) {
                flag2--;
            }
            if (symbol.contains(word.getType()) && flag1 == 0 && flag2 == 0) {
                //UnaryOp
                if (word.typeOfUnary()) {
                    if (!unaryFlag) {
                        exp1.add(word);
                        continue;
                    }
                }
                exps.add(exp1);
                symbols.add(word);
                exp1 = new ArrayList<>();
            } else {
                exp1.add(word);
            }
            unaryFlag = word.typeEquals("IDENFR") || word.typeEquals("RPARENT") || word.typeEquals("INTCON") || word.typeEquals("RBRACK");
        }
        exps.add(exp1);
        return new Exps(exps, symbols);
    }

    private int analyseMulExp(ArrayList<Word> exp) {
        int intType = 0;
        Exps exps = divideExp(exp, new ArrayList<>(Arrays.asList("MULT", "DIV", "MOD")));
        int j = 0;
        for (ArrayList<Word> exp1 : exps.getWords()) {
            intType = analyseUnaryExp(exp1);
            grammar.add("<MulExp>");
            if (j < exps.getSymbols().size()) {
                grammar.add(exps.getSymbols().get(j++).toString());
            }
        }
        return intType;
    }

    private int analyseAddExp(ArrayList<Word> exp) {
        int intType = 0;
        Exps exps = divideExp(exp, new ArrayList<>(Arrays.asList("PLUS", "MINU")));
        int j = 0;
        for (ArrayList<Word> exp1 : exps.getWords()) {
            intType = analyseMulExp(exp1);
            grammar.add("<AddExp>");
            if (j < exps.getSymbols().size()) {
                grammar.add(exps.getSymbols().get(j++).toString());
            }
        }
        return intType;
    }


    private void analyseConstExp(ArrayList<Word> exp) {
        analyseAddExp(exp);
        grammar.add("<ConstExp>");
    }


    private ArrayList<Word> getExp() {
        ArrayList<Word> exp = new ArrayList<>();
        boolean inFunc = false;
        int funcFlag = 0;
        int flag1 = 0;
        int flag2 = 0;
        Word preWord = null;
        Word word = getNextWord();
        while (true) {
            if (word.typeEquals("SEMICN") || word.typeEquals("ASSIGN") || word.typeEquals("RBRACE") ||
                    word.typeSymbolizeValidateStmt()) {
                break;
            }
            if (word.typeEquals("COMMA") && !inFunc) {
                break;
            }
            if (preWord != null) {
                if ((preWord.typeEquals("INTCON") || preWord.typeEquals("IDENFR")) && (word.typeEquals("INTCON") || word.typeEquals("IDENFR"))) {
                    break;
                }
                if ((preWord.typeEquals("RPARENT") || preWord.typeEquals("RBRACK")) && (word.typeEquals("INTCON") || word.typeEquals("IDENFR"))) {
                    break;
                }
                if (flag1 == 0 && flag2 == 0) {
                    if (preWord.typeEquals("INTCON") && word.typeEquals("LBRACK")) {
                        break;
                    }
                    if (preWord.typeEquals("INTCON") && word.typeEquals("LBRACE")) {
                        break;
                    }
                }
            }
            if (word.typeOfNotInExp()) {
                break;
            }
            if (word.typeEquals("IDENFR")) {
                if (getNext2Word().typeEquals("LPARENT")) {
                    inFunc = true;
                }
            }
            if (word.typeEquals("LPARENT")) {
                flag1++;
                if (inFunc) {
                    funcFlag++;
                }
            }
            if (word.typeEquals("RPARENT")) {
                flag1--;
                if (inFunc) {
                    funcFlag--;
                    if (funcFlag == 0) {
                        inFunc = false;
                    }
                }
            }
            if (word.typeEquals("LBRACK")) {
                flag2++;
            }
            if (word.typeEquals("RBRACK")) {
                flag2--;
            }
            if (flag1 < 0) {
                break;
            }
            if (flag2 < 0) {
                break;
            }
            getWordWithoutAddToGrammar();
            exp.add(curWord);
            preWord = word;
            word = getNextWord();
        }
        System.out.println(exp);
        return exp;
    }

    private void error(String type) {
        errors.add(new Error(curWord.getLineNum(), type));
        System.out.println(curWord.getLineNum() + " " + type);
    }

    private void error(String type, int lineNum) {
        errors.add(new Error(lineNum, type));
        System.out.println(lineNum + " " + type);
    }


    private void error() {
        //
    }

    public void printWords(FileWriter writer) throws IOException {
        for (String str : grammar) {
            writer.write(str + "\n");
        }
        writer.flush();
        writer.close();
    }

    public void printErrors(FileWriter writer) throws IOException {
        errors.sort(new Comparator<Error>() {
            @Override
            public int compare(Error e1, Error e2) {
                return e1.getN() - e2.getN();
            }
        });
        for (Error error : errors) {
            writer.write(error + "\n");
        }
        writer.flush();
        writer.close();
    }

    private void addArea() {
        area++;
        symbols.put(area, new Symbols());
    }

    private void removeArea() {
        symbols.remove(area);
        area--;
    }

    private boolean isConst(Word word) {
        for (Symbols s : symbols.values()) {
            if (s.hasSymbol(word)) {
                if (s.isConst(word)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasSymbol(Word word) {
        for (Symbols s : symbols.values()) {
            if (s.hasSymbol(word)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSymbolInThisArea(Word word) {
        return symbols.get(area).hasSymbol(word);
    }

    private boolean hasFunction(Word word) {
        return functions.containsKey(word.getContent());
    }

    private void addSymbol(Word word, String type, int intType) {
        symbols.get(area).addSymbol(type, intType, word);
    }

    private Symbol getSymbol(Word word) {
        Symbol symbol = null;
        for (Symbols s : symbols.values()) {
            if (s.hasSymbol(word)) {
                symbol = s.getSymbol(word);
            }
        }
        return symbol;
    }


    private Function getFunction(Word word) {
        return functions.getOrDefault(word.getContent(), null);
    }

}
