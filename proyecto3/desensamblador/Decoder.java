import java.util.HashMap;

public class Decoder {

    static final HashMap<String, String> COMP = new HashMap<>();
    static final HashMap<String, String> DEST = new HashMap<>();
    static final HashMap<String, String> JUMP = new HashMap<>();

    static {
        COMP.put("0101010", "0");
        COMP.put("0111111", "1");
        COMP.put("0111010", "-1");
        COMP.put("0001100", "D");
        COMP.put("0110000", "A");
        COMP.put("1110000", "M");
        COMP.put("0001101", "!D");
        COMP.put("0110001", "!A");
        COMP.put("1110001", "!M");
        COMP.put("0001111", "-D");
        COMP.put("0110011", "-A");
        COMP.put("1110011", "-M");
        COMP.put("0011111", "D+1");
        COMP.put("0110111", "A+1");
        COMP.put("1110111", "M+1");
        COMP.put("0001110", "D-1");
        COMP.put("0110010", "A-1");
        COMP.put("1110010", "M-1");
        COMP.put("0000010", "D+A");
        COMP.put("1000010", "D+M");
        COMP.put("0010011", "D-A");
        COMP.put("1010011", "D-M");
        COMP.put("0000111", "A-D");
        COMP.put("1000111", "M-D");
        COMP.put("0000000", "D&A");
        COMP.put("1000000", "D&M");
        COMP.put("0010101", "D|A");
        COMP.put("1010101", "D|M");

        DEST.put("000", "");
        DEST.put("001", "M");
        DEST.put("010", "D");
        DEST.put("011", "MD");
        DEST.put("100", "A");
        DEST.put("101", "AM");
        DEST.put("110", "AD");
        DEST.put("111", "AMD");

        JUMP.put("000", "");
        JUMP.put("001", "JGT");
        JUMP.put("010", "JEQ");
        JUMP.put("011", "JGE");
        JUMP.put("100", "JLT");
        JUMP.put("101", "JNE");
        JUMP.put("110", "JLE");
        JUMP.put("111", "JMP");
    }

    public static boolean esInstruccionA(String bits) {
        return bits.charAt(0) == '0';
    }

    public static int valorA(String bits) {
        return Integer.parseInt(bits.substring(1), 2);
    }

    public static boolean tieneSalto(String bits) {
        return !bits.substring(13, 16).equals("000");
    }

    public static String instruccionC(String bits) {
        String destino = DEST.get(bits.substring(10, 13));
        String computo = COMP.get(bits.substring(3, 10));
        String salto = JUMP.get(bits.substring(13, 16));

        String texto = computo;
        if (!destino.isEmpty()) {
            texto = destino + "=" + texto;
        }
        if (!salto.isEmpty()) {
            texto = texto + ";" + salto;
        }
        return texto;
    }
}
