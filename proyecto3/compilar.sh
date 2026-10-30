#!/bin/bash

DIR="$(cd "$(dirname "$0")" && pwd)"

if ! command -v javac > /dev/null 2>&1; then
    echo "No se encontró javac, instale el JDK"
    exit 1
fi

rm -rf "$DIR/bin"
mkdir -p "$DIR/bin/ensamblador" "$DIR/bin/desensamblador"

javac -encoding UTF-8 -d "$DIR/bin/ensamblador" "$DIR"/ensamblador/*.java || exit 1
javac -encoding UTF-8 -d "$DIR/bin/desensamblador" "$DIR"/desensamblador/*.java || exit 1

echo "Compilación terminada"
