#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
classes=$(mktemp -d)
trap 'rm -rf "$classes"' EXIT
src=app/src/main/java/dev/kolmapper
javac -encoding UTF-8 -d "$classes" "$src"/{AnalogInput,BackupCodec,ContactPolicy,FocusRules,InputLedger,OverlayPlacement,ProfileRules,RepeatState,SessionRules,TouchStartPlan,HoldPath,BadgePosition}.java tests/*Test.java
for test in tests/*Test.java; do
  name=$(basename "$test" .java)
  java -cp "$classes" "dev.kolmapper.$name"
done
