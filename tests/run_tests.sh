#!/usr/bin/env sh
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RUN="$ROOT/run.sh"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
pass=0
fail=0

check() {
  name="$1"
  ok="$2"
  if [ "$ok" = 1 ]; then
    pass=$((pass + 1))
  else
    fail=$((fail + 1))
    echo "FAIL $name"
  fi
}

for src in "$ROOT"/tests/scanner/*.dcf; do
  name="scanner/$(basename "$src")"
  expected="${src%.dcf}.out"
  "$RUN" -t scan "$src" > "$TMP/out" 2> "$TMP/err"
  code=$?
  ok=1
  case "$(basename "$src")" in
    legal-*) [ "$code" -eq 0 ] && [ ! -s "$TMP/err" ] || ok=0 ;;
    illegal-*) [ "$code" -ne 0 ] && [ -s "$TMP/err" ] || ok=0 ;;
  esac
  if [ -f "$expected" ] && ! diff -u "$expected" "$TMP/out" > "$TMP/diff"; then
    ok=0
    cat "$TMP/diff"
  fi
  check "$name" "$ok"
done

for src in "$ROOT"/tests/parser/*.dcf; do
  name="parser/$(basename "$src")"
  "$RUN" -t parse "$src" > "$TMP/out" 2> "$TMP/err"
  code=$?
  ok=1
  case "$(basename "$src")" in
    legal-*) [ "$code" -eq 0 ] && [ ! -s "$TMP/out" ] && [ ! -s "$TMP/err" ] || ok=0 ;;
    illegal-*) [ "$code" -ne 0 ] && [ -s "$TMP/err" ] || ok=0 ;;
  esac
  check "$name" "$ok"
done

echo "Пройдено: $pass, провалено: $fail"
[ "$fail" -eq 0 ]
