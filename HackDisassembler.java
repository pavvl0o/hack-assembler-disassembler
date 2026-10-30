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
import java.util.HashMap;
import java.util.TreeSet;

public class HackDisassembler {

    private static ArrayList<String> leerInstrucciones(String ruta) throws IOException, ErrorDesensamblador {
        byte[] datos = Files.readAllBytes(Paths.get(ruta));
        String texto = new String(datos, StandardCharsets.UTF_8);
        if (texto.startsWith("﻿")) {
            texto = texto.substring(1);
        }

        String[] lineas = texto.split("\n", -1);
        int total = lineas.length;
        // el salto de linea final es opcional
        if (lineas[total - 1].isEmpty()) {
            total--;
        }

        ArrayList<String> instrucciones = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            String linea = lineas[i];
            if (linea.endsWith("\r")) {
                linea = linea.substring(0, linea.length() - 1);
            }
            validar(linea, i + 1);
            instrucciones.add(linea);
        }
        return instrucciones;
    }

    private static void validar(String linea, int numero) throws ErrorDesensamblador {
        if (linea.length() != 16) {
            throw new ErrorDesensamblador(numero,
                    "la instrucción tiene " + linea.length() + " caracteres, deben ser 16");
        }

        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (c != '0' && c != '1') {
                throw new ErrorDesensamblador(numero, "carácter inválido '" + c + "'");
            }
        }

        if (linea.charAt(0) == '1') {
            if (!linea.substring(1, 3).equals("11")) {
                throw new ErrorDesensamblador(numero, "los bits 14 y 13 de la instrucción C deben ser 11");
            }
            if (!Decoder.COMP.containsKey(linea.substring(3, 10))) {
                throw new ErrorDesensamblador(numero,
                        "código de cómputo inválido '" + linea.substring(3, 10) + "'");
            }
        }
    }

    private static ArrayList<String> desensamblar(ArrayList<String> instrucciones) {
        int n = instrucciones.size();
        TreeSet<Integer> destinos = new TreeSet<>();
        boolean[] conEtiqueta = new boolean[n];

        for (int i = 1; i < n; i++) {
            String actual = instrucciones.get(i);
            String anterior = instrucciones.get(i - 1);
            if (!Decoder.esInstruccionA(actual) && Decoder.tieneSalto(actual)
                    && Decoder.esInstruccionA(anterior) && Decoder.valorA(anterior) <= n) {
                destinos.add(Decoder.valorA(anterior));
                conEtiqueta[i - 1] = true;
            }
        }

        // las etiquetas se numeran en orden de direccion
        HashMap<Integer, String> nombres = new HashMap<>();
        int contador = 0;
        for (int direccion : destinos) {
            nombres.put(direccion, "L" + contador);
            contador++;
        }

        ArrayList<String> salida = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String bits = instrucciones.get(i);
            if (nombres.containsKey(i)) {
                salida.add("(" + nombres.get(i) + ")");
            }

            if (Decoder.esInstruccionA(bits)) {
                int valor = Decoder.valorA(bits);
                if (conEtiqueta[i]) {
                    salida.add("@" + nombres.get(valor));
                } else {
                    salida.add("@" + valor);
                }
            } else {
                salida.add(Decoder.instruccionC(bits));
            }
        }

        if (nombres.containsKey(n)) {
            salida.add("(" + nombres.get(n) + ")");
        }
        return salida;
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
            System.out.println("Uso: ./desensamblador.sh <Prog.hack>");
            System.exit(1);
        }

        String rutaEntrada = args[0];
        String rutaSalida = quitarExtension(rutaEntrada) + "Dis.asm";

        if (!new File(rutaEntrada).isFile()) {
            System.out.println("ERROR: no se encontró el archivo '" + rutaEntrada + "'");
            System.exit(1);
        }

        ArrayList<String> instrucciones;
        try {
            instrucciones = leerInstrucciones(rutaEntrada);
        } catch (ErrorDesensamblador e) {
            System.out.println("ERROR *** Línea " + e.getLinea() + ": " + e.getMessage());
            System.exit(1);
            return;
        } catch (IOException e) {
            System.out.println("ERROR: no se pudo leer el archivo '" + rutaEntrada + "'");
            System.exit(1);
            return;
        }

        ArrayList<String> programa = desensamblar(instrucciones);

        try (BufferedWriter salida = Files.newBufferedWriter(Paths.get(rutaSalida), StandardCharsets.UTF_8)) {
            for (String linea : programa) {
                salida.write(linea);
                salida.newLine();
            }
        } catch (IOException e) {
            System.out.println("ERROR: no se pudo escribir el archivo '" + rutaSalida + "'");
            System.exit(1);
        }
    }
}
