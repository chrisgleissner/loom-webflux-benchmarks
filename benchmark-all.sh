#!/bin/bash
# Performs a benchmark of Loom and WebFlux, using the scenarios defined in scenarios.csv.
# Outputs are written to results/$scenario.

approaches="loom-tomcat loom-netty webflux-netty"

function log() {
  echo "$( date +"%H:%M:%S" )" "$1"
}

if [ -z "$1" ]; then
    scenariosFile="config/scenarios.csv"
else
    env="$1"
fi

log "Building service"
 ./gradlew clean build

log "Reading scenarios from $scenarios File"

first_line=true
while IFS=',' read -r scenario k6Config delayInMillis connections requestsPerSecond warmupDurationInSeconds testDurationInSeconds; do
    if [[ -z "$scenario" || $scenario == "#"*  ]]; then
        continue
    fi
    if $first_line; then
        first_line=false
        continue
    fi

    for approach in $approaches; do ./benchmark-scenario.sh "$approach" "$scenario" "$k6Config" "$delayInMillis" "$connections" "$requestsPerSecond" "$warmupDurationInSeconds" "$testDurationInSeconds"; done
done < "$scenariosFile" 

endSeconds=`date +%s`
testDurationInSeconds=$(( $endSeconds - $startSeconds ))

log "Completed Loom and WebFlux benchmark after ${testDurationInSeconds}s"
