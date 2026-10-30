#!/bin/bash

DIR="$(cd "$(dirname "$0")" && pwd)"

if [ ! -f "$DIR/bin/desensamblador/HackDisassembler.class" ]; then
    echo "Primero ejecute ./compilar.sh"
    exit 1
fi

java -cp "$DIR/bin/desensamblador" HackDisassembler "$@"
