public class Symbol {
    private String type;
    private int intType;
    private String content;
    private int areaID;

    public Symbol(String type, int intType, Word word,int areaID) {
        this.type = type;
        this.intType = intType;
        this.content = word.getContent();
        this.areaID = areaID;
    }

    public String getType() {
        return type;
    }

    public int getIntType() {
        return intType;
    }

    public int getAreaID() {
        return areaID;
    }

    @Override
    public String toString() {
        return content;
    }
}
