#!/bin/bash
set -e
mkdir -p out
javac -d out $(find src/main/java/decaf -name "*.java")