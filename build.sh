#!/bin/sh
set -eu
cd "$(dirname "$0")"
exec ./gradlew --console=plain build installDist
