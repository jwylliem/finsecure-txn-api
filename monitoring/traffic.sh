#!/usr/bin/env bash
# usage: monitoring/traffic.sh <url> <api-key> <amount> <count>
for i in $(seq 1 "${4:-500}"); do
  curl -s -o /dev/null -w '%{http_code}\n' -X POST "$1/validate" -H "X-API-Key: $2" -H 'Content-Type: application/json' \
    -d "{\"id\":\"T$i\",\"fromAccount\":\"111\",\"toAccount\":\"222\",\"amount\":${3:-100},\"currency\":\"SGD\"}"
  sleep 0.2
done | sort | uniq -c
