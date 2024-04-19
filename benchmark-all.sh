#!/bin/bash
# Performs a benchmark of Loom and WebFlux, using the scenarios defined in scenarios.csv.
# Outputs are written to results/$scenario.

function log() {
  echo "$( date +"%H:%M:%S" )" "$1"
}

log "Started Loom and WebFlux benchmark"
startSeconds=`date +%s`
./log-system-specs.sh

first_line=true
while IFS=',' read -r scenario k6Config delayInMillis connections requestsPerSecond warmupDurationInSeconds testDurationInSeconds; do
    if [[ -z "$scenario" ]]; then
        continue
    fi
    if $first_line; then
        first_line=false
        continue
    fi
    for approach in loom-tomcat loom-netty webflux-netty; do ./benchmark-scenario.sh "$approach" "$scenario" "$k6Config" "$delayInMillis" "$connections" "$requestsPerSecond" "$warmupDurationInSeconds" "$testDurationInSeconds"; done
done < config/scenarios.csv

endSeconds=`date +%s`
testDurationInSeconds=$(( $endSeconds - $startSeconds ))

log "Completed Loom and WebFlux benchmark after ${testDurationInSeconds}s"
