public class Word {
    private String identification;
    private String content;
    private String type;
    private int lineNum;

    public Word(String identification, int lineNum) {
        this.identification = identification;
        this.type = new KeyWordMap().getType(this.identification);
        this.content = this.identification;
        this.lineNum = lineNum;
    }

    public Word(char identification, int lineNum) {
        this.identification = String.valueOf(identification);
        this.type = new KeyWordMap().getType(this.identification);
        this.content = this.identification;
        this.lineNum = lineNum;
    }

    public Word(String type, String content, int lineNum) {
        this.type = type;
        this.content = content;
        this.lineNum = lineNum;
    }

    @Override
    public String toString() {
        return type + " " + content;
    }

    public boolean typeEquals(String str) {
        return type.equals(str);
    }

    public boolean typeSymbolizeStmt() {
        return type.equals("IDENFR")
                || type.equals("LBRACE")
                || type.equals("IFTK")
                || type.equals("ELSETK")
                || type.equals("WHILETK")
                || type.equals("BREAKTK")
                || type.equals("CONTINUETK")
                || type.equals("RETURNTK")
                || type.equals("PRINTFTK")
                || type.equals("SEMICN")
                || typeSymbolizeBeginOfExp();
    }

    public boolean typeSymbolizeValidateStmt() {
        return type.equals("IFTK")
                || type.equals("ELSETK")
                || type.equals("WHILETK")
                || type.equals("BREAKTK")
                || type.equals("CONTINUETK")
                || type.equals("RETURNTK")
                || type.equals("PRINTFTK")
                || type.equals("SEMICN");
    }

    public boolean typeSymbolizeBeginOfExp() {
        return type.equals("LPARENT")
                || type.equals("IDENFR")
                || type.equals("INTCON")
                || type.equals("NOT")
                || type.equals("PLUS")
                || type.equals("MINU");
    }

    public boolean typeOfUnary() {
        return type.equals("PLUS")
                || type.equals("MINU")
                || type.equals("NOT");
    }

    public boolean typeOfNotInExp() {
        return type.equals("CONSTTK")
                || type.equals("INTTK")
                || type.equals("BREAKTK")
                || type.equals("CONTINUETK")
                || type.equals("IFTK")
                || type.equals("ELSETK")
                || type.equals("WHILETK")
                || type.equals("GETINTTK")
                || type.equals("PRINTFTK")
                || type.equals("RETURNTK");
    }

    public String getType() {
        return type;
    }

    public int getLineNum() {
        return lineNum;
    }

    public String getContent() {
        return content;
    }

    public int getFormatNum() {
        int n = 0;
        for (int i = 0; i < content.length(); i++) {
            if (i + 1 < content.length()) {
                if (content.charAt(i) == '%' && content.charAt(i + 1) == 'd') {
                    n++;
                }
            }
        }
        return n;
    }

    public boolean isFormatIllegal() {
        for (int i = 1; i < content.length() - 1; i++) {
            char c = content.charAt(i);
            if (!isLegal(c)) {
                if (c == '%' && content.charAt(i + 1) == 'd') {
                    continue;
                }
                return true;
            } else {
                if (c == '\\' && content.charAt(i + 1) != 'n') {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isLegal(char c) {
        return c == 32 || c == 33 || (c >= 40 && c <= 126);
    }
}
