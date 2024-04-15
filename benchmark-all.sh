#!/bin/bash
# Performs a benchmark of Loom and WebFlux, using the scenarios defined in test-scenarios.csv.
# Outputs are written to results/$scenario.

echo "Started Loom and WebFlux benchmark"
startSeconds=`date +%s`

first_line=true
while IFS=',' read -r scenario k6Config delayInMillis connections requestsPerSecond warmupDurationInSeconds testDurationInSeconds; do
    if [[ -z "$scenario" ]]; then
        continue
    fi
    if $first_line; then
        first_line=false
        continue
    fi
    for approach in loom webflux; do ./benchmark-scenario.sh "$approach" "$scenario" "$k6Config" "$delayInMillis" "$connections" "$requestsPerSecond" "$warmupDurationInSeconds" "$testDurationInSeconds"; done
done < test-config/test-scenarios.csv

endSeconds=`date +%s`
testDurationInSeconds=$(( $endSeconds - $startSeconds ))

echo "Completed Loom and WebFlux benchmark after ${testDurationInSeconds}s"
