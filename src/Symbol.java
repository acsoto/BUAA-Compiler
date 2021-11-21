public class Symbol {
    private String type;
    private int intType;
    private String content;
    private int area = 0;

    public Symbol(String type, int intType, Word word) {
        this.type = type;
        this.intType = intType;
        this.content = word.getContent();
    }

    public String getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public int getArea() {
        return area;
    }

    public int getIntType() {
        return intType;
    }

    @Override
    public String toString() {
        return content;
    }
}
