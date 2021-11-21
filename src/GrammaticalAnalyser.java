import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class GrammaticalAnalyser {
    private ArrayList<Word> words;
    private int index;
    private Word curWord;
    private ArrayList<String> grammar;

    public GrammaticalAnalyser(ArrayList<Word> words) {
        this.words = words;
        index = 0;
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
        Word word = getNextWord();
        while (word.typeEquals("LBRACK")) {
            getWord(); //[
            analyseConstExp(getExp());
            getWord(); //]
            if (curWord.typeEquals("RBRACK")) {
                // TODO: 2021/10/11
            } else {
                error();
            }
            word = getNextWord();
        }
        getWord();//=
        analyseConstInitVal();
        grammar.add("<ConstDef>");
    }

    private void analyseFuncDef() {
        analyseFuncType();
        getWord(); //Ident
        if (curWord.typeEquals("IDENFR")) {
            getWord();//(
            if (curWord.typeEquals("LPARENT")) {
                Word word = getNextWord();
                if (!word.typeEquals("RPARENT")) {
                    analyseFuncFParams();
                }
                getWord();//)
            } else {
                error();
            }
        } else {
            error();
        }
        analyseBlock();
        grammar.add("<FuncDef>");
    }

    private void analyseMainFuncDef() {
        getWord();//int
        getWord();//main
        getWord();//(
        getWord();//)
        analyseBlock();
        grammar.add("<MainFuncDef>");
    }

    private void analyseBlock() {
        getWord();//{
        Word word = getNextWord();
        while (word.typeEquals("CONSTTK") || word.typeEquals("INTTK") || word.typeSymbolizeStmt()) {
            if (word.typeEquals("CONSTTK") || word.typeEquals("INTTK")) {
                analyseBlockItem();
            } else {
                analyseStmt();
            }
            word = getNextWord();
        }
        getWord();//}
        grammar.add("<Block>");
    }

    private void analyseBlockItem() {
        Word word = getNextWord();
        if (word.typeEquals("CONSTTK") || word.typeEquals("INTTK")) {
            analyseDecl();
        } else {
            analyseStmt();
        }
    }

    private void analyseStmt() {
        Word word = getNextWord();
        if (word.typeEquals("IDENFR")) {
            ArrayList<Word> exp = getExp();
            if (!getNextWord().typeEquals("SEMICN")) {
                analyseLVal(exp);
                getWord();//=
                if (getNextWord().typeEquals("GETINTTK")) {
                    getWord();//getint
                    getWord();//(
                    getWord();//)
                    getWord();//;
                } else {
                    analyseExp(getExp());//
                    getWord(); //;
                }
            } else {
                analyseExp(exp);
                getWord();//;
            }
        } else if (word.typeSymbolizeExp()) {
            analyseExp(getExp());
            getWord();//;
        } else if (word.typeEquals("LBRACE")) {
            analyseBlock();
        } else if (word.typeEquals("IFTK")) {
            getWord();//if
            getWord();//(
            analyseCond();
            getWord();//)
            analyseStmt();
            word = getNextWord();
            if (word.typeEquals("ELSETK")) {
                getWord(); //else
                analyseStmt();
            }
        } else if (word.typeEquals("WHILETK")) {
            getWord();//while
            getWord();//(
            analyseCond();
            getWord();//)
            analyseStmt();
        } else if (word.typeEquals("BREAKTK")) {
            getWord();//break
            getWord();//;
        } else if (word.typeEquals("CONTINUETK")) {
            getWord();//continue
            getWord();//;
        } else if (word.typeEquals("RETURNTK")) {
            getWord();//return
            word = getNextWord();
            if (word.typeSymbolizeExp()) {
                analyseExp(getExp());
            }
            getWord();//;
        } else if (word.typeEquals("PRINTFTK")) {
            getWord();//printf
            getWord();//(
            getWord();//STRCON
            word = getNextWord();
            while (word.typeEquals("COMMA")) {
                getWord();//,
                analyseExp(getExp());
                word = getNextWord();
            }
            getWord();//)
            getWord();//;
        } else if (word.typeEquals("SEMICN")) {
            getWord();//;
        }
        grammar.add("<Stmt>");
    }

    private void analyseFuncFParams() {
        analyseFuncFParam();
        Word word = getNextWord();
        while (word.typeEquals("COMMA")) {
            getWord();//,
            analyseFuncFParam();
            word = getNextWord();
        }
        grammar.add("<FuncFParams>");
    }

    private void analyseFuncFParam() {
        getWord();//void|int
        getWord();//Ident
        Word word = getNextWord();
        if (word.typeEquals("LBRACK")) {
            getWord();//[
            getWord();//]
            word = getNextWord();
            while (word.typeEquals("LBRACK")) {
                getWord();//[
                analyseConstExp(getExp());
                getWord();//]
                word = getNextWord();
            }
        }
        grammar.add("<FuncFParam>");
    }

    private void analyseFuncType() {
        getWord(); // void|int
        grammar.add("<FuncType>");
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
        getWord();//;
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
        getWord();//;
        grammar.add("<VarDecl>");
    }

    private void analyseVarDef() {
        getWord();//Ident
        Word word = getNextWord();
        while (word.typeEquals("LBRACK")) {
            getWord();//[
            analyseConstExp(getExp());
            getWord();//]
            word = getNextWord();
        }
        if (word.typeEquals("ASSIGN")) {
            getWord();//=
            analyseInitVal();
        }
        grammar.add("<VarDef>");
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

    private void analyseExp(ArrayList<Word> exp) {
        analyseAddExp(exp);
        grammar.add("<Exp>");
    }

    private void analyseCond() {
        analyseLOrExp(getExp());
        grammar.add("<Cond>");
    }

    private void analyseFuncRParams(ArrayList<Word> exp) {
        Exps exps = divideExp(exp, new ArrayList<>(Arrays.asList("COMMA")));
        int j = 0;
        for (ArrayList<Word> exp1 : exps.getWords()) {
            analyseExp(exp1);
            if (j < exps.getSymbols().size()) {
                grammar.add(exps.getSymbols().get(j++).toString());
            }
        }
        grammar.add("<FuncRParams>");
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

    private void analyseLVal(ArrayList<Word> exp) {
        grammar.add(exp.get(0).toString());//Ident
        if (exp.size() > 1) {
            ArrayList<Word> exp1 = new ArrayList<>();
            int flag = 0;
            for (int i = 1; i < exp.size(); i++) {
                Word word = exp.get(i);
                if (word.typeEquals("LBRACK")) {
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
        }
        grammar.add("<LVal>");
    }

    private void analyseNumber(Word word) {
        grammar.add(word.toString());
        grammar.add("<Number>");
    }

    private void analysePrimaryExp(ArrayList<Word> exp) {
        Word word = exp.get(0);
        if (word.typeEquals("LPARENT")) {
            //remove ( )
            grammar.add(exp.get(0).toString());
            analyseExp(new ArrayList<>(exp.subList(1, exp.size() - 1)));
            grammar.add(exp.get(exp.size() - 1).toString());
        } else if (word.typeEquals("IDENFR")) {
            analyseLVal(exp);
        } else if (word.typeEquals("INTCON")) {
            analyseNumber(exp.get(0));
        } else {
            error();
        }
        grammar.add("<PrimaryExp>");
    }

    private void analyseUnaryExp(ArrayList<Word> exp) {
        Word word = exp.get(0);
        if (word.typeEquals("PLUS") || word.typeEquals("MINU") || word.typeEquals("NOT")) {
            //remove UnaryOp
            analyseUnaryOp(exp.get(0));
            analyseUnaryExp(new ArrayList<>(exp.subList(1, exp.size())));
        } else if (exp.size() == 1) {
            analysePrimaryExp(exp);
        } else {
            if (exp.get(0).typeEquals("IDENFR") && exp.get(1).typeEquals("LPARENT")) {
                //remove Ident ( )
                grammar.add(exp.get(0).toString());
                grammar.add(exp.get(1).toString());
                if (exp.size() > 3) {
                    analyseFuncRParams(new ArrayList<>(exp.subList(2, exp.size() - 1)));
                }
                grammar.add(exp.get(exp.size() - 1).toString());
            } else {
                analysePrimaryExp(exp);
            }
        }
        grammar.add("<UnaryExp>");
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

    private void analyseMulExp(ArrayList<Word> exp) {
        Exps exps = divideExp(exp, new ArrayList<>(Arrays.asList("MULT", "DIV", "MOD")));
        int j = 0;
        for (ArrayList<Word> exp1 : exps.getWords()) {
            analyseUnaryExp(exp1);
            grammar.add("<MulExp>");
            if (j < exps.getSymbols().size()) {
                grammar.add(exps.getSymbols().get(j++).toString());
            }
        }
    }

    private void analyseAddExp(ArrayList<Word> exp) {
        Exps exps = divideExp(exp, new ArrayList<>(Arrays.asList("PLUS", "MINU")));
        int j = 0;
        for (ArrayList<Word> exp1 : exps.getWords()) {
            analyseMulExp(exp1);
            grammar.add("<AddExp>");
            if (j < exps.getSymbols().size()) {
                grammar.add(exps.getSymbols().get(j++).toString());
            }
        }
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
        Word word = getNextWord();
        while (true) {
            if (word.typeEquals("SEMICN") || word.typeEquals("ASSIGN") || word.typeEquals("RBRACE")) {
                break;
            }
            if (word.typeEquals("COMMA") && !inFunc) {
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
            word = getNextWord();
        }
        return exp;
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
}
