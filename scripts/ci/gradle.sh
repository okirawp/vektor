#!/usr/bin/env bash
# Keep Gradle's exit status and add a short, searchable Actions summary on failure.
set -euo pipefail
mkdir -p ci-output
log=ci-output/gradle.log
if ./gradlew --console=plain "$@" 2>&1 | tee "$log"; then exit 0; else status=${PIPESTATUS[0]}; fi
if [[ -n ${GITHUB_STEP_SUMMARY:-} ]]; then
  {
    echo '### Gradle hatası'
    echo '~~~text'
    awk '/^\* What went wrong:/{show=1;next} /^\* Try:/{exit} show{print; if(++n==9)exit}' "$log"
    grep -m 3 -E 'error:| > .*FAILED|Could not resolve|Exception in thread' "$log" || true
    echo '~~~'
    echo 'Tam çıktı çalıştırmanın Gradle adımında ve tanı dosyalarında.'
  } >>"$GITHUB_STEP_SUMMARY"
fi
exit "$status"
