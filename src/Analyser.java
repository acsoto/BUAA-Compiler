import java.io.FileReader;
import java.io.IOException;

public class Analyser {

    public Analyser() throws IOException {
        new LexicalAnalyser().printWords(new FileProcessor().getWriter());
    }


}
