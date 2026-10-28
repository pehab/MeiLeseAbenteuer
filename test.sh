#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p build/tests
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -d build/tests app/src/main/java/de/meiapps/leseabenteuer/Learning.java tests/LearningTest.java
java -cp build/tests de.meiapps.leseabenteuer.LearningTest
