import java.io.BufferedWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class HackAssembler {

    private static void primeraPasada(String ruta, SymbolTable tabla) throws IOException, ErrorEnsamblador {
        Parser parser = new Parser(ruta);
        int direccion = 0;

        while (parser.hasMoreLines()) {
            parser.advance();
            if (parser.instructionType() == Parser.L_INSTRUCTION) {
                String etiqueta = parser.symbol();
                if (tabla.esPredefinido(etiqueta)) {
                    throw new ErrorEnsamblador(parser.getNumeroLinea(),
                            "la etiqueta '" + etiqueta + "' es un símbolo predefinido");
                }
                if (tabla.contains(etiqueta)) {
                    throw new ErrorEnsamblador(parser.getNumeroLinea(),
                            "la etiqueta '" + etiqueta + "' ya fue declarada");
                }
                tabla.addEntry(etiqueta, direccion);
            } else {
                direccion++;
            }
        }
    }

    private static ArrayList<String> segundaPasada(String ruta, SymbolTable tabla) throws IOException, ErrorEnsamblador {
        Parser parser = new Parser(ruta);
        ArrayList<String> binario = new ArrayList<>();
        int siguienteVariable = 16;

        while (parser.hasMoreLines()) {
            parser.advance();
            int tipo = parser.instructionType();

            if (tipo == Parser.A_INSTRUCTION) {
                String simbolo = parser.symbol();
                int valor;
                if (Character.isDigit(simbolo.charAt(0))) {
                    valor = Integer.parseInt(simbolo);
                } else {
                    if (!tabla.contains(simbolo)) {
                        tabla.addEntry(simbolo, siguienteVariable);
                        siguienteVariable++;
                    }
                    valor = tabla.getAddress(simbolo);
                }
                binario.add(aBinario(valor));
            } else if (tipo == Parser.C_INSTRUCTION) {
                binario.add("111" + Code.comp(parser.comp()) + Code.dest(parser.dest()) + Code.jump(parser.jump()));
            }
        }
        return binario;
    }

    private static String aBinario(int valor) {
        String bits = Integer.toBinaryString(valor);
        while (bits.length() < 16) {
            bits = "0" + bits;
        }
        return bits;
    }

    private static String quitarExtension(String ruta) {
        int punto = ruta.lastIndexOf('.');
        int barra = Math.max(ruta.lastIndexOf('/'), ruta.lastIndexOf('\\'));
        if (punto > barra) {
            return ruta.substring(0, punto);
        }
        return ruta;
    }

    public static void main(String[] args) {
        try {
            System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // se deja la salida por defecto
        }

        if (args.length != 1) {
            System.out.println("Uso: ./ensamblador.sh <Prog.asm>");
            System.exit(1);
        }

        String rutaEntrada = args[0];
        String rutaSalida = quitarExtension(rutaEntrada) + ".hack";

        if (!new File(rutaEntrada).isFile()) {
            System.out.println("ERROR: no se encontró el archivo '" + rutaEntrada + "'");
            System.exit(1);
        }

        ArrayList<String> binario;
        try {
            SymbolTable tabla = new SymbolTable();
            primeraPasada(rutaEntrada, tabla);
            binario = segundaPasada(rutaEntrada, tabla);
        } catch (ErrorEnsamblador e) {
            System.out.println("ERROR *** Línea " + e.getLinea() + ": " + e.getMessage());
            System.exit(1);
            return;
        } catch (IOException e) {
            System.out.println("ERROR: no se pudo leer el archivo '" + rutaEntrada + "'");
            System.exit(1);
            return;
        }

        try (BufferedWriter salida = Files.newBufferedWriter(Paths.get(rutaSalida), StandardCharsets.UTF_8)) {
            for (String instruccion : binario) {
                salida.write(instruccion);
                salida.newLine();
            }
        } catch (IOException e) {
            System.out.println("ERROR: no se pudo escribir el archivo '" + rutaSalida + "'");
            System.exit(1);
        }
    }
}
