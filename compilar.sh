#!/bin/sh
set -e
raiz=$(CDPATH= cd -- "$(dirname "$0")" && pwd)
mkdir -p "$raiz/bin"
fontes=$(find "$raiz/src" -name '*.java' | sort)
# Java 17: o display entra no grupo multicast pela API com NetworkInterface.
javac --release 17 -encoding UTF-8 -d "$raiz/bin" $fontes
echo "Compilado em $raiz/bin"
