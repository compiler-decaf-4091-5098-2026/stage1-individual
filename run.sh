#!/bin/bash
# запуск компилятора: ./run.sh -t scan файл.decaf
DIR="$(cd "$(dirname "$0")" && pwd)"
exec java -Dfile.encoding=UTF-8 -cp "$DIR/out" Main "$@"
