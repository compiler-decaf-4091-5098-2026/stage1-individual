#!/usr/bin/env sh
set -eu
root=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
if [ ! -f "$root/build/classes/decaf/Main.class" ]; then
    sh "$root/build.sh"
fi
exec java -Dfile.encoding=UTF-8 -cp "$root/build/classes" decaf.Main "$@"
