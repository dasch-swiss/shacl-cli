#!/usr/bin/env bash
# Integration test for the shacl-cli docker image.
#
# Runs three scenarios against `docker run` from a plain Linux shell, mirroring
# how dsp-tools consumes the container. Any failure exits non-zero.
#
# Env:
#   SHACL_CLI_IMAGE  image tag to test (default: daschswiss/shacl-cli:latest)

set -euo pipefail

IMAGE="${SHACL_CLI_IMAGE:-daschswiss/shacl-cli:latest}"
FIXTURES_DIR="$(cd "$(dirname "$0")/fixtures" && pwd)"
FAIL=0

command -v docker >/dev/null 2>&1 || {
  echo "ERROR: docker not found on PATH" >&2
  exit 2
}

# Pattern matches Jena's default Turtle serialization of xsd:boolean (bare
# `true` / `false` with whitespace separator). Resilient to prefix-expanded
# forms because the value side is what we match on, not the subject/predicate.
CONFORMS_TRUE_RE='sh:conforms[[:space:]]+true'
CONFORMS_FALSE_RE='sh:conforms[[:space:]]+false'

run_scenario() {
  local name="$1" shapes_fixture="$2" data_fixture="$3"
  local tmp stdout_f stderr_f rc=0
  tmp="$(mktemp -d)"
  stdout_f="$(mktemp)"
  stderr_f="$(mktemp)"
  # Ensure temp files are removed even if set -e trips mid-function.
  trap 'rm -rf "$tmp" "$stdout_f" "$stderr_f"' RETURN

  # Copy fixtures into a fresh bind-mount dir so the container's uid 1001 can
  # read them and write report.ttl back.
  cp "$FIXTURES_DIR/$shapes_fixture" "$tmp/shapes.ttl"
  cp "$FIXTURES_DIR/$data_fixture"   "$tmp/data.ttl"
  chmod 777 "$tmp"

  docker run --rm -v "$tmp:/data" "$IMAGE" \
    validate --shacl /data/shapes.ttl --data /data/data.ttl --report /data/report.ttl \
    >"$stdout_f" 2>"$stderr_f" || rc=$?

  # Assert per-scenario expectations.
  local scenario_ok=1
  case "$name" in
    conforming)
      _assert "$name" "exit 0"                    "[ $rc -eq 0 ]"                               || scenario_ok=0
      _assert "$name" "stdout log line present"   "grep -q 'Validation report written to' '$stdout_f'" || scenario_ok=0
      _assert "$name" "report file written"       "[ -s '$tmp/report.ttl' ]"                    || scenario_ok=0
      _assert "$name" "sh:conforms true"          "grep -qE '$CONFORMS_TRUE_RE' '$tmp/report.ttl'"     || scenario_ok=0
      ;;
    non-conforming)
      _assert "$name" "exit 0"                    "[ $rc -eq 0 ]"                               || scenario_ok=0
      _assert "$name" "stdout log line present"   "grep -q 'Validation report written to' '$stdout_f'" || scenario_ok=0
      _assert "$name" "report file written"       "[ -s '$tmp/report.ttl' ]"                    || scenario_ok=0
      _assert "$name" "sh:conforms false"         "grep -qE '$CONFORMS_FALSE_RE' '$tmp/report.ttl'"    || scenario_ok=0
      ;;
    invalid-input)
      # NOTE: The container currently emits diagnostics to stdout rather than
      # stderr (see dsp-tools silent-failure report). This assertion is
      # intentionally permissive — it accepts a diagnostic on either stream —
      # so it catches the *pathological* case (truly silent non-zero exit) but
      # tolerates the known channel-routing wart. Tighten to `stderr non-empty`
      # once the logging channel fix ships.
      _assert "$name" "exit non-zero"             "[ $rc -ne 0 ]"                                                       || scenario_ok=0
      _assert "$name" "diagnostic on some stream" "[ -s '$stderr_f' ] || [ -s '$stdout_f' ]"                            || scenario_ok=0
      ;;
    *)
      echo "INTERNAL: unknown scenario $name" >&2
      scenario_ok=0
      ;;
  esac

  if [ "$scenario_ok" -eq 1 ]; then
    echo "[PASS] $name"
  else
    echo "[FAIL] $name"
    echo "  rc=$rc"
    echo "  stdout:"; sed 's/^/    /' "$stdout_f" | head -20
    echo "  stderr:"; sed 's/^/    /' "$stderr_f" | head -20
    if [ -s "$tmp/report.ttl" ]; then
      echo "  report:"; sed 's/^/    /' "$tmp/report.ttl" | head -20
    fi
    FAIL=1
  fi

  # tmp files cleaned up by RETURN trap set above.
}

_assert() {
  local scenario="$1" label="$2" cmd="$3"
  if eval "$cmd"; then
    return 0
  else
    echo "  [$scenario] assertion failed: $label" >&2
    return 1
  fi
}

echo "Using image: $IMAGE"
run_scenario conforming     shapes.ttl         data-conforming.ttl
run_scenario non-conforming shapes.ttl         data-nonconforming.ttl
# invalid-input: the SUT here is the invalid SHACL file; data-conforming.ttl
# is a throwaway valid data argument, not part of the assertion.
run_scenario invalid-input  shapes-invalid.ttl data-conforming.ttl

exit "$FAIL"
