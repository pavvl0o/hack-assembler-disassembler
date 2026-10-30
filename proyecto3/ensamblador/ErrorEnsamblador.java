public class ErrorEnsamblador extends Exception {

    private final int linea;

    public ErrorEnsamblador(int linea, String mensaje) {
        super(mensaje);
        this.linea = linea;
    }

    public int getLinea() {
        return linea;
    }
}
