#!/bin/bash
# Desktop Autopilot launcher
# Usage: ./run.sh [options]   — see --help for all options
#
# Common examples:
#   ./run.sh                                         # default (auto native input, minimal profile)
#   ./run.sh --input-mode native                     # force hardware-level input
#   ./run.sh --keyboard-jiggle                       # enable random key presses (Space/Up/Down/Shift)
#   ./run.sh --profile stealth                       # stealth profile
#   ./run.sh --schedule --start 09:00 --end 18:00    # working-hours only
#   ./run.sh --input-mode native --keyboard-jiggle --profile keep-awake

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$SCRIPT_DIR/target/mouse-jiggler-1.0.0.jar"

if [ ! -f "$JAR" ]; then
    echo "JAR not found. Building first..."
    cd "$SCRIPT_DIR" || exit 1
    MVN=$(find "$HOME/.m2/wrapper/dists" -name "mvn" -type f 2>/dev/null | sort -r | head -1)
    if [ -z "$MVN" ]; then
        MVN=$(command -v mvn 2>/dev/null)
    fi
    if [ -z "$MVN" ]; then
        echo "Maven not found. Run: mvn package"
        exit 1
    fi
    "$MVN" -q package
fi

exec java -jar "$JAR" "$@"
