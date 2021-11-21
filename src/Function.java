import java.util.ArrayList;

public class Function {
    private String type;
    private String content;
    private String returnType;
    private ArrayList<Integer> paras;

    public Function(Word word, String returnType) {
        this.type = word.getType();
        this.content = word.getContent();
        this.returnType = returnType;
    }

    public ArrayList<Integer> getParas() {
        return paras;
    }

    public void setParas(ArrayList<Integer> paras) {
        this.paras = paras;
    }

    public String getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public String getReturnType() {
        return returnType;
    }

    public int getParaNum() {
        return paras.size();
    }
}
