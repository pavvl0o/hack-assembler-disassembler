import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Pattern;

public class Parser {

    public static final int A_INSTRUCTION = 0;
    public static final int C_INSTRUCTION = 1;
    public static final int L_INSTRUCTION = 2;

    private static final Pattern SIMBOLO = Pattern.compile("[A-Za-z_.$:][A-Za-z0-9_.$:]*");
    private static final Pattern NUMERO = Pattern.compile("[0-9]+");
    private static final BigInteger MAXIMO = BigInteger.valueOf(32767);

    private final String[] lineas;
    private int siguiente;
    private int numeroLinea;
    private String actual;

    private int tipo;
    private String simbolo;
    private String destino;
    private String computo;
    private String salto;

    public Parser(String ruta) throws IOException {
        byte[] datos = Files.readAllBytes(Paths.get(ruta));
        String texto = new String(datos, StandardCharsets.UTF_8);
        if (texto.startsWith("﻿")) {
            texto = texto.substring(1);
        }
        lineas = texto.split("\n", -1);
        siguiente = 0;
    }

    private String limpiar(String linea) {
        int comentario = linea.indexOf("//");
        if (comentario != -1) {
            linea = linea.substring(0, comentario);
        }
        return linea.replace("\r", "").replace(" ", "").replace("\t", "");
    }

    public boolean hasMoreLines() {
        // salta las lineas vacias y las de solo comentarios
        while (siguiente < lineas.length) {
            if (!limpiar(lineas[siguiente]).isEmpty()) {
                return true;
            }
            siguiente++;
        }
        return false;
    }

    public void advance() throws ErrorEnsamblador {
        numeroLinea = siguiente + 1;
        actual = limpiar(lineas[siguiente]);
        siguiente++;

        if (actual.startsWith("@")) {
            leerInstruccionA();
        } else if (actual.startsWith("(")) {
            leerEtiqueta();
        } else if (actual.endsWith(")")) {
            error("etiqueta sin paréntesis de apertura '" + actual + "'");
        } else {
            leerInstruccionC();
        }
    }

    public int getNumeroLinea() {
        return numeroLinea;
    }

    private void error(String mensaje) throws ErrorEnsamblador {
        throw new ErrorEnsamblador(numeroLinea, mensaje);
    }

    private void leerInstruccionA() throws ErrorEnsamblador {
        String valor = actual.substring(1);
        if (valor.isEmpty()) {
            error("instrucción A sin valor");
        }

        char primero = valor.charAt(0);
        if (primero >= '0' && primero <= '9') {
            if (!NUMERO.matcher(valor).matches()) {
                error("valor inválido '" + valor + "', un símbolo no puede empezar por dígito");
            }
            if (new BigInteger(valor).compareTo(MAXIMO) > 0) {
                error("valor fuera de rango '" + valor + "', el máximo es 32767");
            }
        } else if (primero == '-') {
            error("valor negativo '" + valor + "' no permitido");
        } else if (!SIMBOLO.matcher(valor).matches()) {
            error("símbolo inválido '" + valor + "'");
        }

        tipo = A_INSTRUCTION;
        simbolo = valor;
    }

    private void leerEtiqueta() throws ErrorEnsamblador {
        if (!actual.endsWith(")")) {
            error("etiqueta sin paréntesis de cierre '" + actual + "'");
        }

        String nombre = actual.substring(1, actual.length() - 1);
        if (nombre.isEmpty()) {
            error("etiqueta vacía");
        }
        if (!SIMBOLO.matcher(nombre).matches()) {
            error("nombre de etiqueta inválido '" + nombre + "'");
        }

        tipo = L_INSTRUCTION;
        simbolo = nombre;
    }

    private void leerInstruccionC() throws ErrorEnsamblador {
        String texto = actual;
        if (contar(texto, '=') > 1) {
            error("la instrucción tiene más de un '='");
        }
        if (contar(texto, ';') > 1) {
            error("la instrucción tiene más de un ';'");
        }

        String dest = "";
        String jump = "";

        int igual = texto.indexOf('=');
        if (igual != -1) {
            dest = texto.substring(0, igual);
            texto = texto.substring(igual + 1);
            if (dest.isEmpty()) {
                error("destino vacío");
            }
            if (!Code.DEST.containsKey(dest)) {
                error("destino inválido '" + dest + "'");
            }
        }

        int puntoComa = texto.indexOf(';');
        if (puntoComa != -1) {
            jump = texto.substring(puntoComa + 1);
            texto = texto.substring(0, puntoComa);
            if (jump.isEmpty()) {
                error("salto vacío");
            }
            if (!Code.JUMP.containsKey(jump)) {
                error("salto inválido '" + jump + "'");
            }
        }

        if (texto.isEmpty()) {
            error("cómputo vacío");
        }
        if (!Code.COMP.containsKey(texto)) {
            if (igual == -1 && puntoComa == -1) {
                error("instrucción no reconocida '" + actual + "'");
            }
            error("cómputo inválido '" + texto + "'");
        }

        tipo = C_INSTRUCTION;
        destino = dest;
        computo = texto;
        salto = jump;
    }

    private int contar(String texto, char c) {
        int total = 0;
        for (int i = 0; i < texto.length(); i++) {
            if (texto.charAt(i) == c) {
                total++;
            }
        }
        return total;
    }

    public int instructionType() {
        return tipo;
    }

    public String symbol() {
        return simbolo;
    }

    public String dest() {
        return destino;
    }

    public String comp() {
        return computo;
    }

    public String jump() {
        return salto;
    }
}
