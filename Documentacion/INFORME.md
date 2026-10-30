# Informe técnico – Proyecto 3: Ensamblador y desensamblador Hack

Organización de computadores 2026-2 – Universidad EAFIT

## 1. Integrantes y aportes

- Laura Santamaria Espinosa
- Pablo Alvarez Restrepo

El proyecto se desarrolló de forma colaborativa. Ambos integrantes participaron en todas las partes: el diseño, el ensamblador, el desensamblador, el manejo de errores, las pruebas y la documentación.

## 2. Arquitectura de la solución

La solución está escrita en Java y tiene dos programas independientes: el ensamblador (carpeta `ensamblador/`) y el desensamblador (carpeta `desensamblador/`). Cada uno se compila por separado en `bin/` y se ejecuta con su script.

### 2.1 Módulos del ensamblador

Se siguió la arquitectura propuesta en clase:

| Módulo | Responsabilidad |
|--------|-----------------|
| `HackAssembler` | Programa principal. Crea la tabla de símbolos, hace las dos pasadas y escribe el `.hack`. |
| `Parser` | Lee el archivo, quita comentarios y espacios, identifica el tipo de instrucción (`A_INSTRUCTION`, `C_INSTRUCTION`, `L_INSTRUCTION`), la separa en sus campos y valida la sintaxis. |
| `Code` | Contiene las tablas de `dest`, `comp` y `jump` y devuelve el código binario de cada campo. |
| `SymbolTable` | Guarda la relación símbolo → dirección. |
| `ErrorEnsamblador` | Excepción que guarda el número de línea y el mensaje del error. |

El `Parser` implementa la API vista en clase: `hasMoreLines()`, `advance()`, `instructionType()`, `symbol()`, `dest()`, `comp()` y `jump()`.

Cada línea se limpia así:

1. Se corta desde `//` hasta el final de la línea.
2. Se eliminan todos los espacios, tabulaciones y `\r` (por eso funciona con CRLF y con instrucciones como `AMD = M - 1 ; JNE`).
3. Si la línea queda vacía, `hasMoreLines()` la salta.

Después `advance()` clasifica la línea por su primer carácter:

- Si empieza por `@` es una instrucción A.
- Si empieza por `(` es una etiqueta.
- En cualquier otro caso se trata como instrucción C.

### 2.2 Flujo de las dos pasadas

```
             Prog.asm
                │
   ┌────────────▼─────────────┐
   │ Inicialización           │  SymbolTable con R0..R15, SP, LCL, ARG,
   │                          │  THIS, THAT, SCREEN y KBD
   └────────────┬─────────────┘
   ┌────────────▼─────────────┐
   │ Primera pasada           │  Recorre todo el archivo:
   │                          │  - valida la sintaxis de cada línea
   │                          │  - cuenta instrucciones A y C (dirección ROM)
   │                          │  - agrega cada (ETIQUETA) con la dirección
   │                          │    de la siguiente instrucción
   └────────────┬─────────────┘
   ┌────────────▼─────────────┐
   │ Segunda pasada           │  Vuelve a leer desde el inicio:
   │                          │  - @número  → binario directo
   │                          │  - @símbolo → busca en la tabla; si no existe
   │                          │    es variable y se le asigna 16, 17, ...
   │                          │  - dest=comp;jump → "111" + comp + dest + jump
   └────────────┬─────────────┘
                │ (solo si no hubo errores)
             Prog.hack
```

En la primera pasada las etiquetas no aumentan el contador de direcciones, porque no generan código. Por eso una etiqueta toma la dirección de la siguiente instrucción real.

Las instrucciones binarias se acumulan en una lista. El archivo de salida solo se crea al final, cuando las dos pasadas terminaron sin errores. Así se cumple que ante un error no se genera el archivo.

### 2.3 Módulos del desensamblador

| Módulo | Responsabilidad |
|--------|-----------------|
| `HackDisassembler` | Lee y valida las líneas, busca los destinos de salto, asigna etiquetas y escribe `ProgDis.asm`. |
| `Decoder` | Tablas inversas (binario → mnemónico) y funciones para leer cada campo de una instrucción. |
| `ErrorDesensamblador` | Excepción con número de línea. |

El desensamblador trabaja en tres pasos:

1. **Lectura y validación.** Se valida cada línea en orden y se guardan las instrucciones en una lista.
2. **Búsqueda de destinos de salto.** Se calculan las etiquetas (sección 4).
3. **Generación del `.asm`.** Se escribe cada instrucción:
   - Instrucción A: `@` seguido del valor en decimal.
   - Instrucción C: `dest=comp;jump`, omitiendo `dest=` o `;jump` cuando son `000`.

## 3. Tabla de símbolos

### 3.1 Estructura de datos

Se usa un `HashMap<String, Integer>`, donde la llave es el nombre del símbolo y el valor es la dirección. Se escogió porque las operaciones `contains`, `addEntry` y `getAddress` son O(1) en promedio. Esto es importante en programas grandes como `Pong.asm`, que tiene más de 27 000 instrucciones y cientos de símbolos.

Además se guarda un `HashSet<String>` con los nombres predefinidos. Sirve para diferenciar dos errores distintos:

- Una etiqueta que usa un nombre predefinido, por ejemplo `(SCREEN)`.
- Una etiqueta declarada dos veces.

En los dos casos el símbolo ya está en el `HashMap`, así que sin el `HashSet` no se podrían distinguir.

Métodos (API vista en clase):

| Método | Descripción |
|--------|-------------|
| `addEntry(símbolo, dirección)` | Agrega el par a la tabla. |
| `contains(símbolo)` | Indica si el símbolo ya existe. |
| `getAddress(símbolo)` | Devuelve la dirección del símbolo. |
| `esPredefinido(símbolo)` | Indica si es uno de los 23 símbolos predefinidos. |

### 3.2 Contenido de la tabla

| Tipo | Cuándo se agrega | Valor |
|------|------------------|-------|
| Predefinidos | En el constructor | `SP`=0, `LCL`=1, `ARG`=2, `THIS`=3, `THAT`=4, `R0`…`R15`=0…15, `SCREEN`=16384, `KBD`=24576 |
| Etiquetas | Primera pasada, al encontrar `(NOMBRE)` | Dirección ROM de la siguiente instrucción |
| Variables | Segunda pasada, la primera vez que aparece `@símbolo` y no está en la tabla | 16, 17, 18, … en orden de aparición |

### 3.3 Asignación de variables

En la segunda pasada se usa un contador `siguienteVariable` que empieza en 16. Cuando aparece `@x`:

- Si `x` empieza por dígito, es una constante y se convierte directamente.
- Si `x` ya está en la tabla (predefinido, etiqueta o variable vista antes), se usa su dirección.
- Si no está, es una variable nueva. Se agrega con el valor de `siguienteVariable` y el contador aumenta en 1.

Como todas las etiquetas se cargan en la primera pasada, una etiqueta que se usa antes de declararse (por ejemplo `@FIN` antes de `(FIN)`) nunca se confunde con una variable.

Ejemplo con `Prueba2.asm`:

| Símbolo | Tipo | Valor |
|---------|------|-------|
| `CICLO` | etiqueta | 4 |
| `GUARDAR` | etiqueta | 18 |
| `FIN` | etiqueta | 22 |
| `i` | variable | 16 |
| `suma` | variable | 17 |
| `R0`, `R1` | predefinidos | 0, 1 |

## 4. Generación de etiquetas en el desensamblador

### 4.1 Algoritmo

Sea `n` el número de instrucciones del programa.

1. Se recorren las instrucciones desde `i = 1` hasta `n − 1`. La instrucción `i` cumple la regla 9.1 si se dan las tres condiciones:
   - es una instrucción C con salto (bits `j1j2j3` distintos de `000`);
   - la instrucción `i − 1` es una instrucción A;
   - el valor `v` de esa instrucción A cumple `v ≤ n`.

   Cuando se cumplen, `v` se agrega a un conjunto de destinos y se marca la instrucción `i − 1` como "usa etiqueta".
2. El conjunto de destinos es un `TreeSet<Integer>`. Esto evita repetidos (si varios saltos llegan a la misma dirección se crea una sola etiqueta) y deja las direcciones ordenadas de menor a mayor.
3. Se recorre el `TreeSet` y se asignan los nombres `L0`, `L1`, `L2`, … en orden de dirección. Estos nombres son símbolos válidos, no se repiten y no coinciden con ningún predefinido. Se guardan en un `HashMap<Integer, String>` (dirección → nombre).
4. Al escribir el programa, antes de la instrucción `k` se escribe `(Lx)` si `k` es un destino. Si algún destino es igual a `n`, su etiqueta se escribe al final del archivo.
5. Las instrucciones A marcadas se escriben como `@Lx`. Las demás instrucciones A se dejan en decimal, aunque tengan el mismo valor que un destino.

Con esto se cumple la condición de corrección. `(Lx)` queda justo antes de la instrucción `v`, así que al ensamblar `@Lx` se obtiene otra vez el valor `v`. Las etiquetas no ocupan dirección, por lo que las demás instrucciones conservan su posición.

### 4.2 Ejemplo (Prueba4.hack, n = 19)

Instrucciones relevantes del binario:

| Dir. | Binario | Instrucción |
|------|---------|-------------|
| 6 | `0000000000010001` | `@17` |
| 7 | `1110001100000110` | `D;JLE` |
| 15 | `0000000000000100` | `@4` |
| 16 | `1110101010000111` | `0;JMP` |
| 17 | `0000000000010001` | `@17` |
| 18 | `1110101010000111` | `0;JMP` |

Las instrucciones 7, 16 y 18 son saltos precedidos por instrucciones A con valores 17, 4 y 17. Todos son ≤ 19. Destinos ordenados: {4, 17}, así que 4 → `L0` y 17 → `L1`. Las instrucciones `@16` (dirección 2) y `@0` no van antes de un salto y se quedan en decimal.

Salida obtenida:

```
@16384
D=A
@16
M=D
(L0)
@0
D=M
@L1
D;JLE
@16
A=M
M=-1
@16
M=M+1
@0
MD=M-1
@L0
0;JMP
(L1)
@L1
0;JMP
```

Es igual a `Prueba4Dis_ejemplo.asm`. Al volver a ensamblarla se obtiene exactamente `Prueba4.hack`.

## 5. Manejo de errores

Los dos programas imprimen una sola línea con el formato `ERROR *** Línea N: <mensaje>`. Después terminan con `System.exit(1)` y no crean el archivo de salida. Si no hay errores, terminan con código 0.

### 5.1 Cálculo del número de línea

- El archivo se lee completo y se divide con `split("\n", -1)`, de modo que cada posición del arreglo es una línea física del archivo.
- El número de línea es `índice + 1`. Así se cuentan desde 1 e incluyen comentarios y líneas en blanco, porque esas líneas también ocupan una posición en el arreglo aunque el `Parser` las salte.
- El `\r` de los archivos CRLF se quita después de dividir, por lo que no cambia el conteo.

### 5.2 Primer error en orden de líneas

En el ensamblador, toda la validación ocurre en la **primera pasada**, que recorre el archivo de arriba hacia abajo. `advance()` valida la sintaxis de cada línea y `HackAssembler` revisa las etiquetas predefinidas o repetidas en el mismo recorrido.

Ninguna regla de error depende de información que aparezca más adelante. Por eso la primera excepción que se lanza corresponde siempre al primer error del archivo. Por ejemplo, si la línea 2 tiene `D=D*A` y la línea 3 repite una etiqueta, se reporta la línea 2.

En el desensamblador cada línea se valida mientras se lee, también en orden.

### 5.3 Detección por categoría – ensamblador

| Categoría | Cómo se detecta | Ejemplo de mensaje |
|-----------|-----------------|--------------------|
| Instrucción A | Si después de `@` no hay nada, error. Si empieza por dígito, todo debe ser dígitos (regex `[0-9]+`) y el valor ≤ 32767; se compara con `BigInteger` para que números muy largos no desborden un `int`. Si empieza por `-`, valor negativo. Si no, debe cumplir la regex de símbolo `[A-Za-z_.$:][A-Za-z0-9_.$:]*`. | `valor fuera de rango '32768', el máximo es 32767` |
| Etiquetas | Si empieza por `(`, debe terminar en `)`, el nombre no puede ser vacío y debe cumplir la regex de símbolo. Una línea que termina en `)` sin empezar por `(` también es error. En la primera pasada se verifica con `SymbolTable` que no sea predefinida ni esté repetida. | `la etiqueta 'SCREEN' es un símbolo predefinido` |
| Destino | La parte antes del `=` no puede ser vacía y debe ser una llave de la tabla `DEST` (`M`, `D`, `MD`, `A`, `AM`, `AD`, `AMD`). Cualquier otra combinación, como `DM` o `ADM`, no está en la tabla. | `destino inválido 'DM'` |
| Cómputo | La parte entre `=` y `;` no puede ser vacía y debe ser una de las 28 llaves de `COMP`. Así se rechazan `D*A`, `A+D`, `M+D` y `M+M`. | `cómputo inválido 'A+D'` |
| Salto | La parte después del `;` no puede ser vacía y debe estar en `JUMP`. Como `HashMap` distingue mayúsculas, `jmp` es error. | `salto inválido 'jmp'` |
| Estructura | Se cuentan los `=` y `;` y no puede haber más de uno de cada uno. Si la línea no tiene ni `=` ni `;` y no es un cómputo válido, es texto que no es instrucción. | `la instrucción tiene más de un '='` |

### 5.4 Detección por categoría – desensamblador

| Categoría | Cómo se detecta | Ejemplo de mensaje |
|-----------|-----------------|--------------------|
| Longitud | `linea.length() != 16`. Una línea vacía en medio del archivo tiene longitud 0. Los espacios al final cuentan como caracteres. Solo se permite que el último salto de línea del archivo sea opcional (si el último elemento del `split` es vacío, se descarta). | `la instrucción tiene 17 caracteres, deben ser 16` |
| Caracteres | Se recorre cada carácter y debe ser `0` o `1`. | `carácter inválido '2'` |
| Bits 14–13 | Si el bit 15 es `1`, los dos siguientes deben ser `11`. | `los bits 14 y 13 de la instrucción C deben ser 11` |
| Cómputo | Si es instrucción C, los 7 bits `a c1…c6` deben existir en la tabla `COMP` del `Decoder`. Los campos `dest` y `jump` no se validan porque sus 8 combinaciones de 3 bits son todas válidas. | `código de cómputo inválido '0000001'` |

## 6. Pruebas realizadas y resultados

Todas las pruebas se ejecutaron con los scripts `ensamblador.sh` y `desensamblador.sh`. Algunas se hicieron desde una carpeta con espacios en el nombre para verificar ese requisito.

### 6.1 Programas válidos – ensamblador

| Programa | Qué se probó | Resultado |
|----------|--------------|-----------|
| `Prueba1.asm` | Programa sin símbolos | `.hack` idéntico a `Prueba1.hack` |
| `Prueba2.asm` | Etiquetas, variables y comentarios al final de la línea | `.hack` idéntico a `Prueba2.hack` |
| `Prueba2.asm` con CRLF | Fin de línea de Windows | Mismo resultado que con LF |
| `Add`, `MaxL`, `RectL`, `PongL` | Programas oficiales sin símbolos | Se ensamblan sin errores |
| `Max`, `Rect`, `Pong` | Programas oficiales con símbolos | Producen el mismo `.hack` que su versión `L` |
| `Pong.asm` | Programa grande (27 483 instrucciones) | Idéntico al `.hack` de referencia, en menos de 1 segundo |
| Línea `AMD = M - 1 ; JNE` | Espacios dentro de la instrucción | Se acepta y se traduce correctamente |
| [Programas propios en `pruebas/ensamblador/`] | [completar] | [completar] |

### 6.2 Programas válidos – desensamblador

| Programa | Resultado |
|----------|-----------|
| `Prueba3.hack` | `Prueba3Dis.asm` igual a `Prueba3Dis_ejemplo.asm`, con etiquetas L0, L1 y L2. Al reensamblarlo da el mismo binario. |
| `Prueba4.hack` | `Prueba4Dis.asm` igual a `Prueba4Dis_ejemplo.asm`. Al reensamblarlo da el mismo binario. |
| `Add.hack`, `Max.hack`, `Rect.hack` | Desensamblar → ensamblar da archivos idénticos (Max genera 3 etiquetas y Rect 2). |
| `Pong.hack` | Desensamblar → ensamblar da un archivo idéntico. Se generan 290 etiquetas. |
| `.hack` con CRLF y sin salto de línea final | Se acepta y se desensambla bien. |
| [Programas propios en `pruebas/desensamblador/`] | [completar] |

### 6.3 Casos de error

Se probaron todos los ejemplos de las tablas 10.1 y 10.2 de la guía. Cada línea inválida se puso en la línea 4 de un archivo que tiene comentario y línea en blanco antes, para verificar que el número de línea los cuenta. En todos los casos:

- se imprimió `ERROR *** Línea 4: ...`;
- el código de salida fue 1;
- no se generó el archivo de salida.

| Programa | Casos probados |
|----------|----------------|
| Ensamblador | `@`, `@32768`, `@-1`, `@1abc`, `@VAR#1`, `@LOOP)`, `(LOOP`, `LOOP)`, `()`, `(1LOOP)`, `(SCREEN)`, etiqueta duplicada, `X=D`, `DM=D+1`, `ADM=M`, `MA=1`, `MM=0`, `=D`, `D=`, `;JMP`, `D=D*A`, `D=A+D`, `M=M+D`, `D=M+M`, `D;`, `D;JXX`, `0;jmp`, `D=M=A`, `D;JGT;JMP`, `HOLA MUNDO` |
| Desensamblador | Líneas de 15 y 17 caracteres, línea con espacio al final, línea vacía en medio, `0000000000000201`, `1010110000010000`, `1110000001010000` |

También se probó que, con dos errores en el mismo archivo, se reporta el de la línea menor.

Los casos de prueba propios y su resultado esperado están en [`pruebas/README.md`](../pruebas/README.md).

## 7. Análisis (RA4)

### 7.1 ¿Qué información del programa original se pierde al ensamblar?

El binario solo guarda lo que la CPU necesita para ejecutar. Se pierde:

- **Comentarios, líneas en blanco, espacios e indentación.** No generan código.
- **Nombres de las etiquetas.** `(LOOP)` desaparece y `@LOOP` queda solo como el número de la dirección ROM.
- **Nombres de las variables.** `@i` y `@suma` quedan como `@16` y `@17`.
- **Qué símbolo predefinido se usó.** `@R0`, `@SP` y `@0` producen el mismo binario.
- **Ceros a la izquierda.** `@0007` y `@7` dan lo mismo.
- **Para qué sirve cada número.** No queda registro de si una instrucción A carga una constante, una dirección de RAM o una dirección de salto.

### 7.2 ¿Por qué el desensamblado no es único?

Porque la traducción ensamblador → binario no es inyectiva: muchos programas distintos producen el mismo `.hack`. Por ejemplo:

- Las etiquetas pueden llamarse como se quiera (`L0`, `LOOP`, `FIN`…).
- Una instrucción A puede escribirse con el número o con cualquier símbolo que tenga ese valor (`@0`, `@R0`, `@SP`).
- Se pueden agregar comentarios, espacios o etiquetas extra.

Al desensamblar hay que escoger uno de todos esos programas posibles. Por eso la condición de corrección no compara el `.asm` con el original, sino que pide que **al ensamblarlo se obtenga el mismo binario**.

### 7.3 ¿Por qué el binario no permite distinguir una variable de una constante o de un símbolo predefinido?

Una instrucción A en binario es solo un `0` seguido de 15 bits con un número. Las tres cosas producen exactamente esos mismos bits:

- la primera variable (`@i` → 16);
- la constante `@16`, que podría usarse para hacer `D=A`;
- `@R0`, que es lo mismo que `@SP` o `@0`.

La diferencia entre variable, constante y predefinido solo existe en el código fuente y en la tabla de símbolos del ensamblador, y esa tabla no se guarda en el `.hack`.

Mirando las instrucciones siguientes se podría intentar adivinar: si después viene `M=...`, probablemente es una dirección de RAM; si viene `D=A`, probablemente es una constante. Pero no es seguro, porque el mismo valor puede usarse de las dos formas.

### 7.4 ¿Qué saltos no puede etiquetar la regla de la sección 9.1 y por qué?

La regla solo mira la instrucción inmediatamente anterior al salto. Por eso no etiqueta:

1. **Saltos con la dirección calculada en tiempo de ejecución.** Por ejemplo, el retorno de funciones que genera el compilador en `Pong.asm`:
   ```
   @R15
   A=M
   0;JMP
   ```
   La instrucción anterior al salto es `A=M`, no una instrucción A. El destino depende del valor en RAM, que no se conoce sin ejecutar el programa.
2. **Saltos donde el `@destino` no está justo antes.** Por ejemplo `@FIN`, luego `D=D-1` (que no cambia A) y luego `D;JGT`. El salto sí va a `FIN`, pero la regla no lo reconoce porque entre las dos instrucciones hay otra.
3. **Saltos a direcciones mayores que `n`.** Están fuera del programa y no hay dónde poner la etiqueta.
4. **Un salto en la instrucción 0**, porque no tiene instrucción anterior.
5. **Direcciones de código que se usan como datos.** Por ejemplo, cuando el compilador guarda una dirección de retorno con `@RET_ADDRESS` y `D=A` para meterla en la pila. Ahí no hay un salto inmediatamente después, así que el valor queda en decimal aunque en el programa original era una etiqueta.

En todos estos casos el programa desensamblado sigue siendo correcto, porque se conserva el número. Lo único que se pierde es la legibilidad de la etiqueta. Para recuperar esas etiquetas habría que analizar el flujo del programa, lo que se sale de la regla pedida.
