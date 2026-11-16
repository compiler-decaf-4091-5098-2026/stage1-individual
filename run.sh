#!/bin/sh
set -eu
project_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
if [ ! -x "$project_dir/build/install/decaf/bin/decaf" ]; then
    echo "Build the project first: ./build.sh" >&2
    exit 2
fi
exec "$project_dir/build/install/decaf/bin/decaf" "$@"
