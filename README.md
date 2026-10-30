# Proyecto 3 – Ensamblador y desensamblador Hack

Organización de computadores 2026-2 – Universidad EAFIT
Docente: José Luis Montoya Pareja

## Integrantes

- Laura Santamaria Espinosa
- Pablo Alvarez Restrepo

## Compilación y ejecución

### Lenguaje y versión

- **Java**, JDK 8 o superior (probado con JDK 21).
- No se usan librerías externas, solo la librería estándar de Java.

### Dependencias y cómo instalarlas

1. **Git for Windows** (incluye Git Bash): descargar desde https://git-scm.com e instalar con las opciones por defecto.
2. **JDK de Java**: descargar el instalador `.msi` de la versión LTS desde https://adoptium.net. Durante la instalación activar las opciones *Set JAVA_HOME* y *Add to PATH*.
3. Verificar en Git Bash que los comandos quedaron disponibles:

```bash
java -version
javac -version
```

### Comandos

Todos los comandos se ejecutan en **Git Bash** desde la carpeta `proyecto3/`:

```bash
./compilar.sh
./ensamblador.sh pruebas/ensamblador/Prog.asm
./desensamblador.sh pruebas/desensamblador/Prog.hack
```

- `./compilar.sh` compila el código fuente con `javac` y deja los `.class` en la carpeta `bin/` (que no se sube al repositorio). Se debe ejecutar una vez antes de usar los otros dos scripts y cada vez que se modifique un `.java`.
- `./ensamblador.sh <ruta>` traduce `Prog.asm` y genera `Prog.hack` en la misma carpeta del archivo de entrada.
- `./desensamblador.sh <ruta>` traduce `Prog.hack` y genera `ProgDis.asm` en la misma carpeta del archivo de entrada.

Los scripts reciben la ruta relativa o absoluta del archivo y funcionan con rutas que contienen espacios, por ejemplo:

```bash
./ensamblador.sh "/c/Users/Usuario/OneDrive/Mis pruebas/Max.asm"
```

Si la entrada tiene un error, se imprime una línea con el formato `ERROR *** Línea N: <mensaje>`, no se genera el archivo de salida y el programa termina con código de salida 1. Si no hay errores, el código de salida es 0.

## Estructura del código

```
proyecto3/
├── compilar.sh             compila ensamblador y desensamblador
├── ensamblador.sh          ejecuta el ensamblador
├── desensamblador.sh       ejecuta el desensamblador
├── ensamblador/
│   ├── HackAssembler.java      programa principal, hace las dos pasadas
│   ├── Parser.java             lee el archivo, limpia cada línea y valida la sintaxis
│   ├── Code.java               tablas dest, comp y jump a binario
│   ├── SymbolTable.java        tabla de símbolos (predefinidos, etiquetas, variables)
│   └── ErrorEnsamblador.java   excepción con el número de línea del error
├── desensamblador/
│   ├── HackDisassembler.java   programa principal: valida, genera etiquetas y escribe el .asm
│   ├── Decoder.java            tablas de binario a mnemónico
│   └── ErrorDesensamblador.java
├── pruebas/                programas válidos y casos de error (ver pruebas/README.md)
└── DOCUMENTACION/
    └── INFORME.md          informe técnico
```

El ensamblador sigue la arquitectura propuesta en clase (Parser, Code, SymbolTable y un módulo principal). El detalle del diseño está en [DOCUMENTACION/INFORME.md](DOCUMENTACION/INFORME.md).
