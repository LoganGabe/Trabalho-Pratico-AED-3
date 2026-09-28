#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WORK="$(mktemp -d "${TMPDIR:-/tmp}/aed3-tests.XXXXXX")"
trap 'rm -rf "$WORK"' EXIT
mkdir -p "$WORK/classes" "$WORK/work"
find "$ROOT/src" "$ROOT/tests" -name '*.java' -print > "$WORK/sources.txt"
javac -encoding UTF-8 -d "$WORK/classes" @"$WORK/sources.txt"
cd "$WORK/work"
java -Dfile.encoding=UTF-8 -Duser.language=en -Duser.country=US -cp "$WORK/classes" TestSuite
