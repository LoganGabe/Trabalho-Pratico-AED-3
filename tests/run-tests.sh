#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WORK="$(mktemp -d "${TMPDIR:-/tmp}/aed3-tests.XXXXXX")"
trap 'rm -rf "$WORK"' EXIT
mkdir -p "$WORK/classes" "$WORK/work"
(
  cd "$ROOT"
  find src tests -name '*.java' -print > "$WORK/sources.txt"
  javac --release 11 -encoding UTF-8 -d "$WORK/classes" @"$WORK/sources.txt"
)
cd "$WORK/work"
java -Dfile.encoding=UTF-8 -cp "$WORK/classes" JogoDesenvolvedoraTest
java -Dfile.encoding=UTF-8 -cp "$WORK/classes" ArquivoJogoDesenvolvedoraTest
mkdir "$WORK/integracao"
cd "$WORK/integracao"
java -Dfile.encoding=UTF-8 -cp "$WORK/classes" RelacionamentoJogoDesenvolvedoraTest
cd "$WORK/work"
mkdir "$WORK/catalogo"
cd "$WORK/catalogo"
java -Dfile.encoding=UTF-8 -cp "$WORK/classes" CatalogoTest
cd "$WORK/work"
java -Dfile.encoding=UTF-8 -Duser.language=en -Duser.country=US -cp "$WORK/classes" TestSuite
if [[ "${1:-}" == "--interface" ]]; then
  mkdir "$WORK/interface"
  cd "$WORK/interface"
  java -Dfile.encoding=UTF-8 -cp "$WORK/classes" InterfaceGraficaTest
fi
