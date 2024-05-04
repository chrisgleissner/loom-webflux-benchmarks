#!/bin/bash
# Performs a benchmark of Loom and WebFlux, using the scenarios defined in scenarios.csv.
# Outputs are written to results/$scenario.

approaches="loom-tomcat,loom-netty,webflux-netty"
scenariosFile="config/scenarios.csv"
keep_csv=false

function log() {
  echo "$( date +"%H:%M:%S" )" "$1"
}

function print_usage() {
  echo "Usage: $(basename "$0") [-h] [-a <approaches>] [-C] [FILE]"
  echo "  FILE: Scenario CSV file. Default: config/scenarios.csv"
  echo "  -a <approaches>: Comma-separated list of approaches to test. Default: loom-tomcat,loom-netty,webflux-netty"
  echo "                   Supported approaches: platform-tomcat,loom-tomcat,loom-netty,webflux-netty"
  echo "  -C               Keep CSV files used to create chart. Default: false"
  echo "  -h               Print this help"
}

while getopts 'ha:C' opt; do
  case "$opt" in
    h)
      print_usage
      exit 0
      ;;
    a) approaches="$OPTARG" ;;
    C) keep_csv=true ;;
    \?)
      echo "Invalid option: -$OPTARG"
      print_usage
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
while IFS=',' read -r scenario k6Config delayCallDepth delayInMillis connections requestsPerSecond warmupDurationInSeconds testDurationInSeconds; do
    if [[ -z "$scenario" || $scenario == "#"*  ]]; then
        continue
    fi
    if $first_line; then
        first_line=false
        continue
    fi

    IFS=',' read -ra approach_array <<< "$approaches"
    for approach in "${approach_array[@]}"; do ./benchmark-scenario.sh -a "$approach" -s "$scenario" -k "$k6Config" -d "$delayCallDepth" -m "$delayInMillis" -c "$connections" -r "$requestsPerSecond" -w "$warmupDurationInSeconds" -t "$testDurationInSeconds" -C "$keep_csv"; done
done < "$scenariosFile"

endSeconds=$( date +%s )
testDurationInSeconds=$(( endSeconds - startSeconds ))

log "Completed Loom and WebFlux benchmark after ${testDurationInSeconds}s"
