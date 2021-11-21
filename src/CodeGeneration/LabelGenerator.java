package CodeGeneration;

public class LabelGenerator {
    private int count = 0;

    public String getLabel(String type) {
        count++;
        return "label_" + type + "_"+ count;
    }
}
