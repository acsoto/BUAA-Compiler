import CodeGeneration.PCodeExecutor;

import java.io.IOException;
import java.util.Scanner;

public class Analyser {

    private LexicalAnalyser lexicalAnalyser;
    private GrammaticalAnalyser grammaticalAnalyser;
    private PCodeExecutor pCodeExecutor;
    private FileProcessor fileProcessor;
    private Scanner scanner;

    public Analyser() throws IOException {
        scanner = new Scanner(System.in);
        fileProcessor = new FileProcessor();
        lexicalAnalyser = new LexicalAnalyser();
        grammaticalAnalyser = new GrammaticalAnalyser(lexicalAnalyser.getWords());
//        grammaticalAnalyser.printPCode();
        pCodeExecutor = new PCodeExecutor(grammaticalAnalyser.getCodes(), fileProcessor.getWriter(), scanner);
        pCodeExecutor.run();
        pCodeExecutor.print();
    }


}
