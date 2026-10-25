#!/usr/bin/env sh
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
mkdir -p build/classes
javac --release 17 -encoding UTF-8 -Xlint:all -Werror -d build/classes src/decaf/*.java
