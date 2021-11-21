public class Error {
    private int n;
    private String type;

    public Error(int n, String type) {
        this.n = n;
        this.type = type;
    }

    public int getN() {
        return n;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return n+" "+type;
    }
}
