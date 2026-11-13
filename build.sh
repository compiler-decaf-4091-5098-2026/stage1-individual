#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
if [ ! -x node_modules/.bin/tsc ]; then
  npm install --no-audit --no-fund
fi
npm run build --silent
