#!/bin/bash
# Performs a benchmark of Loom and WebFlux, using the scenarios defined in scenarios.csv.
# Outputs are written to results/$scenario.

approaches="loom-tomcat,loom-netty,webflux-netty"
scenariosFile="config/scenarios.csv"

function log() {
  echo "$( date +"%H:%M:%S" )" "$1"
}

while getopts 'ha:' opt; do
  case "$opt" in
    h)
      echo "Usage: $(basename "$0") [-h] [-a APPROACHES] [FILE]"
      echo "  FILE: Scenario CSV file. Default: config/scenarios.csv"
      echo "  -a APPROACHES: Comma-separated list of approaches. Default: loom-tomcat,loom-netty,webflux-netty"
      echo "                 Supported approaches: platform-tomcat,loom-tomcat,loom-netty,webflux-netty"
      echo "  -h: Shows this help"
      exit 0
      ;;
    a)
      approaches="$OPTARG"
      ;;
    \?)
      echo "Invalid option: -$OPTARG"
      exit 1
      ;;
  esac
done
shift "$(( OPTIND -1 ))"

if [ -n "$1" ]; then
  scenariosFile="$1"
fi

if [ ! -f "$scenariosFile" ]; then
  log "Scenario file $scenariosFile does not exist; terminating"
  exit 1
fi

startSeconds=$( date +%s )

echo
log "Started Loom and WebFlux benchmark"
log "- Scenarios file: $scenariosFile"
log "- Approaches: $approaches"
echo

./log-system-specs.sh

first_line=true
while IFS=',' read -r scenario k6Config delayInMillis connections requestsPerSecond warmupDurationInSeconds testDurationInSeconds; do
    if [[ -z "$scenario" || $scenario == "#"*  ]]; then
        continue
    fi
    if $first_line; then
        first_line=false
        continue
    fi

    IFS=',' read -ra approach_array <<< "$approaches"
    for approach in "${approach_array[@]}"; do ./benchmark-scenario.sh "$approach" "$scenario" "$k6Config" "$delayInMillis" "$connections" "$requestsPerSecond" "$warmupDurationInSeconds" "$testDurationInSeconds"; done
done < "$scenariosFile"

endSeconds=$( date +%s )
testDurationInSeconds=$(( endSeconds - startSeconds ))

log "Completed Loom and WebFlux benchmark after ${testDurationInSeconds}s"
