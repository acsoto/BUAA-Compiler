import java.util.ArrayList;

public class Exps {
    private ArrayList<ArrayList<Word>> words;
    private ArrayList<Word> symbols;

    public Exps(ArrayList<ArrayList<Word>> words, ArrayList<Word> symbols) {
        this.words = words;
        this.symbols = symbols;
    }

    public ArrayList<ArrayList<Word>> getWords() {
        return words;
    }

    public ArrayList<Word> getSymbols() {
        return symbols;
    }

}
