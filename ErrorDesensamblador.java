public class ErrorDesensamblador extends Exception {

    private final int linea;

    public ErrorDesensamblador(int linea, String mensaje) {
        super(mensaje);
        this.linea = linea;
    }

    public int getLinea() {
        return linea;
    }
}
