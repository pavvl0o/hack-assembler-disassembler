import java.util.HashMap;
import java.util.HashSet;

public class SymbolTable {

    private final HashMap<String, Integer> tabla = new HashMap<>();
    private final HashSet<String> predefinidos = new HashSet<>();

    public SymbolTable() {
        tabla.put("SP", 0);
        tabla.put("LCL", 1);
        tabla.put("ARG", 2);
        tabla.put("THIS", 3);
        tabla.put("THAT", 4);
        tabla.put("SCREEN", 16384);
        tabla.put("KBD", 24576);
        for (int i = 0; i < 16; i++) {
            tabla.put("R" + i, i);
        }
        predefinidos.addAll(tabla.keySet());
    }

    public void addEntry(String simbolo, int direccion) {
        tabla.put(simbolo, direccion);
    }

    public boolean contains(String simbolo) {
        return tabla.containsKey(simbolo);
    }

    public int getAddress(String simbolo) {
        return tabla.get(simbolo);
    }

    public boolean esPredefinido(String simbolo) {
        return predefinidos.contains(simbolo);
    }
}
