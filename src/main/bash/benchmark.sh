#!/bin/bash
# Performs a benchmark of Loom and WebFlux. Outputs are written to build/results/$scenario.

approaches="loom-tomcat,loom-netty,webflux-netty"
scenariosDir="src/main/resources/scenarios"
relativeScenariosPath="scenarios.csv"
resultsDir=build/results
resultsCsvFile="$resultsDir/results.csv"
resultsPngFile="$resultsDir/results.png"
keep_csv=false

log() {
  echo "$( date +"%H:%M:%S" )" "$1"
}

print_usage() {
  echo "Usage: $(basename "$0") [-h] [-a <approaches>] [-C] [FILE]"
  echo "  FILE: Scenario CSV file path relative to 'src/main/resources/scenarios/' folder. Default: scenarios.csv"
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
  relativeScenariosPath="$1"
fi

scenariosFile="$scenariosDir/$relativeScenariosPath"

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
./src/main/bash/log-system-specs.sh

log "Contents of $scenariosFile:"
cat "$scenariosFile"

first_line=true
while IFS=',' read -r scenario k6Config delayCallDepth delayInMillis connections requestsPerSecond warmupDurationInSeconds testDurationInSeconds; do
    if [[ -z "$scenario" || $scenario == "#"* || $first_line == true ]]; then
        first_line=false
        continue
    fi

    IFS=',' read -ra approach_array <<< "$approaches"
    for approach in "${approach_array[@]}"; do
      ./src/main/bash/benchmark-scenario.sh -a "$approach" -s "$scenario" -k "$k6Config" -d "$delayCallDepth" -m "$delayInMillis" -c "$connections" -r "$requestsPerSecond" -w "$warmupDurationInSeconds" -t "$testDurationInSeconds" -C "$keep_csv"
    done
done < "$scenariosFile"

./src/main/python/results_chart.py -i "$resultsCsvFile" -o "$resultsPngFile"

endSeconds=$( date +%s )
testDurationInSeconds=$(( endSeconds - startSeconds ))

log "Completed Loom and WebFlux benchmark after ${testDurationInSeconds}s"
