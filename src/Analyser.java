import java.io.IOException;

public class Analyser {

    private LexicalAnalyser lexicalAnalyser;
    private GrammaticalAnalyser grammaticalAnalyser;

    public Analyser() throws IOException {
        lexicalAnalyser = new LexicalAnalyser();
        grammaticalAnalyser = new GrammaticalAnalyser(lexicalAnalyser.getWords());
        grammaticalAnalyser.printWords(new FileProcessor().getWriter());
    }


}
