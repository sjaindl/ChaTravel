#!/usr/bin/env bash
set -euo pipefail

# Ports to free (8080 = netty, 9090 = grpc)
PORTS=(9090 8080)

# Kill processes bound to the ports
for p in "${PORTS[@]}"; do
  pids=$(lsof -ti tcp:$p || true)
  if [[ -n "${pids}" ]]; then
    echo "Killing PIDs on port $p: ${pids}"
    kill -9 ${pids} || true
  fi
done

# Kill by names (fallbacks)
pkill -f 'MainKt' || true         # your Kotlin server process
pkill -f 'io.grpc.netty' || true  # gRPC server
killall mongod

echo "Cleanup done."
