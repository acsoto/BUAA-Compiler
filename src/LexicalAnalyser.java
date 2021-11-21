import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class LexicalAnalyser {
    private String code;
    private int lineNum = 1;
    private int index = 0;
    private ArrayList<Word> words = new ArrayList<>();

    public LexicalAnalyser() throws IOException {
        code = new FileProcessor().getCode();
        analyse();
    }


    private Character getChar() {
        if (index < code.length()) {
            char c = code.charAt(index);
            if (c == '\n') {
                lineNum++;
            }
            index++;
            return c;
        } else {
            return null;
        }
    }

    private void unGetChar() {
        index--;
        char c = code.charAt(index);
        if (c == '\n') {
            lineNum--;
        }
    }

    private void analyse() throws IOException {
        // 持续读入字符
        Character c = null;
        while ((c = getChar()) != null) {
            if (c == ' ' || c == '\r' || c == '\t') {
                continue;
            } else if (c == '+' || c == '-' || c == '*' || c == '%') {
                words.add(new Word(c,lineNum));
            } else if (c == '/') {
                analyseSlash();
            } else if (c == '(' || c == ')' || c == '[' || c == ']' || c == '{' || c == '}') {
                words.add(new Word(c,lineNum));
            } else if (c == '>' || c == '<' || c == '=' || c == '!') {
                analyseRelation(c);
            } else if (c == ',' || c == ';') {
                words.add(new Word(c,lineNum));
            } else if (c == '"') {
                analyseCitation();
            } else if (c == '&' || c == '|') {
                analyseLogic(c);
            } else if (Character.isDigit(c)) {
                analyseDigit(c);
            } else if (Character.isLetter(c) || c == '_') {
                analyseLetter(c);
            }

        }
    }

    private void analyseSlash() {
        Character c = getChar();
        if (c == '/') {
            do {
                c = getChar();
                if (c == null || c == '\n') {
                    return;
                    // 判断为//注释，结束分析
                }
            } while (true);
        } else if (c == '*') {
            do {
                c = getChar();
                if (c == null) {
                    return;
                }
                if (c == '*') {
                    c = getChar();
                    if (c == '/') {
                        return;
                        // 判断为/* */注释，直接结束分析
                    } else {
                        unGetChar();
                    }
                }
            } while (true);
        } else {
            words.add(new Word("/",lineNum));
            unGetChar();
        }
    }

    private void analyseRelation(char c) {
        if (c == '=') {
            c = getChar();
            if (c == '=') {
                words.add(new Word("==",lineNum));
            } else {
                unGetChar();
                words.add(new Word("=",lineNum));
                return;
            }
        } else if (c == '<') {
            c = getChar();
            if (c == '=') {
                words.add(new Word("<=",lineNum));
            } else {
                unGetChar();
                words.add(new Word("<",lineNum));
            }
        } else if (c == '>') {
            c = getChar();
            if (c == '=') {
                words.add(new Word(">=",lineNum));
            } else {
                unGetChar();
                words.add(new Word(">",lineNum));
            }
        } else {
            c = getChar();
            if (c == '=') {
                words.add(new Word("!=",lineNum));
            } else {
                unGetChar();
                words.add(new Word("!",lineNum));
            }
        }
    }

    private void analyseCitation() {
        Character c = null;
        StringBuffer buffer = new StringBuffer("");
        while ((c = getChar()) != null) {
            if (c == '"') {
                words.add(new Word("STRCON", "\"" + buffer + "\"",lineNum));
                return;
            } else {
                buffer.append(c);
            }
        }
    }

    private void analyseLogic(char pre) {
        Character c = null;
        if ((c = getChar()) != null) {
            if (pre == '&') {
                if (c == '&') {
                    words.add(new Word("&&",lineNum));
                } else {
                    unGetChar();
                    words.add(new Word("&",lineNum));
                }
            } else {
                if (c == '|') {
                    words.add(new Word("||",lineNum));
                } else {
                    unGetChar();
                    words.add(new Word("|",lineNum));
                }
            }
        }
    }

    private void analyseDigit(char pre) {
        StringBuilder builder = new StringBuilder("" + pre);
        Character c = null;
        while ((c = getChar()) != null) {
            if (Character.isDigit(c)) {
                builder.append(c);
            } else {
                unGetChar();
                words.add(new Word("INTCON", builder.toString(),lineNum));
                return;
            }
        }
    }

    private void analyseLetter(char pre) {
        StringBuilder builder = new StringBuilder("" + pre);
        Character c = null;
        while ((c = getChar()) != null) {
            if (Character.isLetter(c) || c == '_' || Character.isDigit(c)) {
                builder.append(c);
            } else {
                unGetChar();
                if (new KeyWordMap().isKey(builder.toString())) {
                    words.add(new Word(builder.toString(),lineNum));
                } else {
                    words.add(new Word("IDENFR", builder.toString(),lineNum));
                }
                return;
            }
        }
    }

    public void printWords(FileWriter writer) throws IOException {
        for (Word word : words) {
            writer.write(word.toString() + "\n");
        }
        writer.flush();
        writer.close();
    }

    public ArrayList<Word> getWords() {
        return words;
    }
}
