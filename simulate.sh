#!/usr/bin/env bash
#
# simulate.sh — boots the service, simulates the assignment by replaying every
# line of input.txt through POST /api/v1/loads, captures each 200 response,
# and diffs the captured stream against output.txt.
#
# Exits 0 on a match, 1 otherwise.
#
# Usage: ./simulate.sh
#

set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
INPUT="$ROOT/input.txt"
EXPECTED="$ROOT/output.txt"
ACTUAL_DIR="$ROOT/build/replay"
ACTUAL="$ACTUAL_DIR/actual-output.txt"
APP_LOG="$ACTUAL_DIR/app.log"
BODY_TMP="$(mktemp)"
APP_URL="http://localhost:8080"
PORT=8080
READY_TIMEOUT=180

mkdir -p "$ACTUAL_DIR"
: > "$ACTUAL"
: > "$APP_LOG"

# ─── Pre-flight checks ──────────────────────────────────────────────────────
[[ -f "$INPUT" ]]    || { echo "missing $INPUT"; exit 1; }
[[ -f "$EXPECTED" ]] || { echo "missing $EXPECTED"; exit 1; }
if lsof -i:"$PORT" >/dev/null 2>&1; then
    echo "port $PORT already in use — stop the running app and retry"
    exit 1
fi

# ─── Cleanup on exit ────────────────────────────────────────────────────────
APP_PID=""
cleanup() {
    rc=$?
    if [[ -n "$APP_PID" ]] && kill -0 "$APP_PID" 2>/dev/null; then
        echo
        echo "stopping app (pid $APP_PID)..."
        kill "$APP_PID" 2>/dev/null || true
        # kill the gradle daemon process too if it spawned a child
        pkill -P "$APP_PID" 2>/dev/null || true
        wait "$APP_PID" 2>/dev/null || true
    fi
    rm -f "$BODY_TMP"
    exit $rc
}
trap cleanup EXIT INT TERM

# ─── Start the app ──────────────────────────────────────────────────────────
echo "starting app via ./gradlew bootRun (log → $APP_LOG)..."
./gradlew bootRun --no-daemon >"$APP_LOG" 2>&1 &
APP_PID=$!

# ─── Wait for readiness ─────────────────────────────────────────────────────
echo -n "waiting for 'Started VelocityApplication'"
seconds=0
until grep -q 'Started VelocityApplication' "$APP_LOG" 2>/dev/null; do
    if ! kill -0 "$APP_PID" 2>/dev/null; then
        echo
        echo "app exited before becoming ready, last log lines:"
        tail -30 "$APP_LOG"
        exit 1
    fi
    if (( seconds >= READY_TIMEOUT )); then
        echo
        echo "timed out after ${READY_TIMEOUT}s. last log lines:"
        tail -30 "$APP_LOG"
        exit 1
    fi
    echo -n "."
    sleep 1
    seconds=$((seconds + 1))
done
echo " up after ${seconds}s"

# ─── Replay ─────────────────────────────────────────────────────────────────
echo "replaying $(wc -l <"$INPUT" | tr -d ' ') lines from input.txt..."
total=0
accepted=0
declined=0
duplicates=0

while IFS= read -r line; do
    [[ -z "$line" ]] && continue
    total=$((total + 1))

    status=$(curl -sS -o "$BODY_TMP" -w "%{http_code}" \
        -X POST "$APP_URL/api/v1/loads" \
        -H 'Content-Type: application/json' \
        -d "$line" \
        --max-time 10)

    case "$status" in
        200)
            body=$(cat "$BODY_TMP")
            printf '%s\n' "$body" >>"$ACTUAL"
            if [[ "$body" == *'"accepted":true'* ]]; then
                accepted=$((accepted + 1))
            else
                declined=$((declined + 1))
            fi
            ;;
        204)
            duplicates=$((duplicates + 1))
            ;;
        *)
            echo
            echo "unexpected status $status on input line $total:"
            echo "  $line"
            cat "$BODY_TMP"
            exit 1
            ;;
    esac

    if (( total % 100 == 0 )); then
        echo "  $total processed (accepted=$accepted declined=$declined duplicates=$duplicates)"
    fi
done <"$INPUT"

echo
echo "replay done — $total inputs, $accepted accepted, $declined declined, $duplicates duplicates"
echo "actual output written to: $ACTUAL"

# ─── Diff ───────────────────────────────────────────────────────────────────
# output.txt uses CRLF line endings (authored on Windows). We strip CRs from
# both sides so the comparison is line-ending-agnostic.
echo
echo "comparing actual vs expected (line-ending-agnostic)..."
expected_lines=$(wc -l <"$EXPECTED" | tr -d ' ')
actual_lines=$(wc -l <"$ACTUAL" | tr -d ' ')

EXPECTED_NORM="$ACTUAL_DIR/expected-normalised.txt"
ACTUAL_NORM="$ACTUAL_DIR/actual-normalised.txt"
tr -d '\r' <"$EXPECTED" >"$EXPECTED_NORM"
tr -d '\r' <"$ACTUAL"   >"$ACTUAL_NORM"

if diff -q "$EXPECTED_NORM" "$ACTUAL_NORM" >/dev/null 2>&1; then
    echo "PASS — output matches output.txt ($expected_lines lines)"
    exit 0
else
    echo "FAIL — output differs"
    echo "  expected: $expected_lines lines"
    echo "  actual:   $actual_lines lines"
    echo
    echo "first diff hunks:"
    diff "$EXPECTED_NORM" "$ACTUAL_NORM" | head -40
    exit 1
fi
