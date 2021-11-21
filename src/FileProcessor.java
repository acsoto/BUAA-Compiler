import java.io.*;

public class FileProcessor {
    private FileReader reader;
    private String code;
    private FileWriter writer;

    public FileProcessor() throws IOException {
        reader = new FileReader(new File("testfile.txt"));
        code = transferFileToCode();
        writer = new FileWriter(new File("error.txt"));
    }

    public FileReader getReader() {
        return reader;
    }

    private String transferFileToCode() throws IOException {
        BufferedReader bf = new BufferedReader(reader);
        StringBuffer buffer = new StringBuffer();
        String s = null;
        while ((s = bf.readLine()) != null) {
            buffer.append(s).append("\n");
        }
        return buffer.toString();
    }

    public String getCode() {
        return code;
    }

    public FileWriter getWriter() {
        return writer;
    }
}
