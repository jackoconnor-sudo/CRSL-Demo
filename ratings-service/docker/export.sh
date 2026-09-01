#!/bin/sh
# Stand-in for the batch team's export helper. Writes an empty file so that the
# container image has the same shape as production.
set -e
FORMAT="csv"
DESK="credit"
while [ $# -gt 0 ]; do
  case "$1" in
    --format) FORMAT="$2"; shift 2 ;;
    --desk) DESK="$2"; shift 2 ;;
    *) shift ;;
  esac
done
OUT="/var/northgate/exports/${DESK}-$(date +%Y%m%d).${FORMAT}"
: > "$OUT"
echo "wrote $OUT"
