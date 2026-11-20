#!/bin/sh
set -eu
cd "$(dirname "$0")"
mkdir -p build
"${CXX:-c++}" -std=c++17 -Wall -Wextra -Wpedantic src/*.cpp -o build/decaf
