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
                || typeSymbolizeExp();
    }

    public boolean typeSymbolizeExp() {
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

    public String getType() {
        return type;
    }

    public int getLineNum() {
        return lineNum;
    }
}
