#!/usr/bin/env bash
set -e

rm -rf out
mkdir -p out

javac -d out src/*.java