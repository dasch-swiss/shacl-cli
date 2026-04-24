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
  local name="$1" shapes_fixture="$2" data_fixture="$3" mount_opts="${4:-}" report_path="${5:-/data/report.ttl}"
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

  docker run --rm -v "$tmp:/data${mount_opts}" "$IMAGE" \
    validate --shacl /data/shapes.ttl --data /data/data.ttl --report "$report_path" \
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
      _assert "$name" "exit non-zero"             "[ $rc -ne 0 ]"                      || scenario_ok=0
      _assert "$name" "stderr non-empty"          "[ -s '$stderr_f' ]"                 || scenario_ok=0
      ;;
    bad-mount-readonly | bad-mount-wrong-target)
      # bad-mount-readonly:    inputs mount correctly but the bind is read-only,
      #                        so writing the report fails with "Read-only file
      #                        system".
      # bad-mount-wrong-target: inputs mount correctly but the report path points
      #                         outside any bind mount — FileOutputStream fails
      #                         with "No such file or directory".
      #
      # Both assert the fix for the Band C silent-write failure: exit non-zero
      # AND a real diagnostic on stderr (previously just an empty ANSI-colored
      # line went to stdout and nothing to stderr).
      _assert "$name" "exit non-zero"             "[ $rc -ne 0 ]"                      || scenario_ok=0
      _assert "$name" "stderr non-empty"          "[ -s '$stderr_f' ]"                 || scenario_ok=0
      _assert "$name" "FileNotFoundException on stderr" \
                                                  "grep -q 'FileNotFoundException' '$stderr_f'" || scenario_ok=0
      _assert "$name" "no report written"         "[ ! -f '$tmp/report.ttl' ]"         || scenario_ok=0
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
run_scenario conforming         shapes.ttl         data-conforming.ttl
run_scenario non-conforming     shapes.ttl         data-nonconforming.ttl
# invalid-input: the SUT here is the invalid SHACL file; data-conforming.ttl
# is a throwaway valid data argument, not part of the assertion.
run_scenario invalid-input      shapes-invalid.ttl data-conforming.ttl
# bad-mount-readonly: valid inputs, but the bind mount is read-only so the
# report write fails. The `:ro` mount option is passed as the 4th argument.
run_scenario bad-mount-readonly    shapes.ttl data-conforming.ttl :ro
# bad-mount-wrong-target: valid inputs at /data, but --report points outside
# any mount (`/elsewhere/report.ttl`). Simulates the user mounting the output
# volume at a path that doesn't match their --report argument. The 5th arg
# overrides the report path.
run_scenario bad-mount-wrong-target shapes.ttl data-conforming.ttl "" /elsewhere/report.ttl

exit "$FAIL"
