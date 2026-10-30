import java.util.HashMap;

public class Code {

    static final HashMap<String, String> COMP = new HashMap<>();
    static final HashMap<String, String> DEST = new HashMap<>();
    static final HashMap<String, String> JUMP = new HashMap<>();

    static {
        COMP.put("0", "0101010");
        COMP.put("1", "0111111");
        COMP.put("-1", "0111010");
        COMP.put("D", "0001100");
        COMP.put("A", "0110000");
        COMP.put("M", "1110000");
        COMP.put("!D", "0001101");
        COMP.put("!A", "0110001");
        COMP.put("!M", "1110001");
        COMP.put("-D", "0001111");
        COMP.put("-A", "0110011");
        COMP.put("-M", "1110011");
        COMP.put("D+1", "0011111");
        COMP.put("A+1", "0110111");
        COMP.put("M+1", "1110111");
        COMP.put("D-1", "0001110");
        COMP.put("A-1", "0110010");
        COMP.put("M-1", "1110010");
        COMP.put("D+A", "0000010");
        COMP.put("D+M", "1000010");
        COMP.put("D-A", "0010011");
        COMP.put("D-M", "1010011");
        COMP.put("A-D", "0000111");
        COMP.put("M-D", "1000111");
        COMP.put("D&A", "0000000");
        COMP.put("D&M", "1000000");
        COMP.put("D|A", "0010101");
        COMP.put("D|M", "1010101");

        DEST.put("", "000");
        DEST.put("M", "001");
        DEST.put("D", "010");
        DEST.put("MD", "011");
        DEST.put("A", "100");
        DEST.put("AM", "101");
        DEST.put("AD", "110");
        DEST.put("AMD", "111");

        JUMP.put("", "000");
        JUMP.put("JGT", "001");
        JUMP.put("JEQ", "010");
        JUMP.put("JGE", "011");
        JUMP.put("JLT", "100");
        JUMP.put("JNE", "101");
        JUMP.put("JLE", "110");
        JUMP.put("JMP", "111");
    }

    public static String dest(String mnemonico) {
        return DEST.get(mnemonico);
    }

    public static String comp(String mnemonico) {
        return COMP.get(mnemonico);
    }

    public static String jump(String mnemonico) {
        return JUMP.get(mnemonico);
    }
}
