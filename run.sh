#!/usr/bin/env sh
set -eu
node "$(dirname "$0")/dist/index.js" "$@"
