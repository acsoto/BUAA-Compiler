public class Word {
    private String identification;
    private String content;
    private String type;

    public Word(String identification) {
        this.identification = identification;
        this.type = new KeyWordMap().getType(this.identification);
        this.content = this.identification;
    }

    public Word(char identification) {
        this.identification = String.valueOf(identification);
        this.type = new KeyWordMap().getType(this.identification);
        this.content = this.identification;
    }

    public Word(String type, String content) {
        this.type = type;
        this.content = content;
    }

    @Override
    public String toString() {
        return type + " " + content;
    }
}
