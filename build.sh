#!/bin/bash
# сборка компилятора: классы попадают в папку out
set -e
cd "$(dirname "$0")"
rm -rf out
mkdir -p out
javac -encoding UTF-8 -d out *.java
echo "сборка завершена"
