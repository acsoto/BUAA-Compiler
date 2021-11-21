package CodeGeneration;

public class Func {
    private int index;
    private int args;

    public Func(int index, int args) {
        this.index = index;
        this.args = args;
    }

    public int getIndex() {
        return index;
    }

    public int getArgs() {
        return args;
    }
}
