package CodeGeneration;

public class Var {
    private int index;
    private int dimension = 0;
    private int dim1;
    private int dim2;

    public Var(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public int getDim1() {
        return dim1;
    }

    public int getDim2() {
        return dim2;
    }

    public void setDim1(int dim1) {
        this.dim1 = dim1;
    }

    public void setDim2(int dim2) {
        this.dim2 = dim2;
    }

    public int getDimension() {
        return dimension;
    }

    public void setDimension(int dimension) {
        this.dimension = dimension;
    }
}
