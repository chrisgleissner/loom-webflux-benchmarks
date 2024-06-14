#!/bin/bash
docker compose up -d
./src/main/bash/benchmark.sh "$@"
docker compose down
