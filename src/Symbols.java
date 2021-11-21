import java.util.HashMap;

public class Symbols {
    private HashMap<String, Symbol> symbolHashMap;

    public Symbols() {
        symbolHashMap = new HashMap<>();
    }

    public void addSymbol(String type, int intType, Word word) {
        symbolHashMap.put(word.getContent(), new Symbol(type, intType, word));
    }

    public boolean hasSymbol(Word word) {
        return symbolHashMap.containsKey(word.getContent());
    }

    public Symbol getSymbol(Word word) {
        return symbolHashMap.get(word.getContent());
    }

    public boolean isConst(Word word) {
        return symbolHashMap.get(word.getContent()).getType().equals("const");
    }

    @Override
    public String toString() {
        return symbolHashMap.toString();
    }
}
