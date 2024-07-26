#!/bin/bash
# Runs all benchmarks and saves results to build/results.

approaches="loom-tomcat,loom-netty,webflux-netty"
scenariosDir="src/main/resources/scenarios"
relativeScenariosPath="scenarios.csv"
resultsDir=build/results
resultsCsvFile="$resultsDir/results.csv"
resultsPngFile="$resultsDir/results.png"
resultsNettyPngFile="$resultsDir/results-netty.png"
resultsMdFile="$resultsDir/results.md"
keep_csv=false

log() {
  echo "$( date +"%H:%M:%S" )" "$1"
}

print_usage() {
  echo "Usage: $(basename "$0") [OPTION]... [SCENARIO_FILE]"
  echo "Runs benchmarks configured by a scenario file."
  echo
  echo "SCENARIO_FILE:     Scenario configuration CSV file in src/main/resources/scenarios/. Default: scenarios.csv"
  echo
  echo "OPTION:"
  echo "  -a <approaches>  Comma-separated list of approaches to test. Default: loom-tomcat, loom-netty, webflux-netty"
  echo "                   Supported approaches: platform-tomcat, loom-tomcat, loom-netty, webflux-netty"
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

log "Started Loom and WebFlux benchmark"
log "- Scenarios file: $scenariosFile"
log "- Approaches: $approaches"
echo

./src/main/bash/log-system-specs.sh

log "Contents of $scenariosFile:"
cat "$scenariosFile"

tail -n +2 "$scenariosFile" | while IFS=',' read -r scenario k6Config serverProfiles delayCallDepth delayInMillis connections requestsPerSecond warmupDurationInSeconds testDurationInSeconds; do
    if [[ -z "$scenario" || $scenario == "#"* ]]; then
        continue
    fi

    IFS='|' read -ra server_profile_array <<< "$serverProfiles"
    for serverProfile in "${server_profile_array[@]}"; do
        composeFile="src/main/docker/docker-compose-$serverProfile.yaml"
        if [[ -f "$composeFile" ]]; then
            log "Starting Docker container(s) using $composeFile"
            docker compose -f "$composeFile" up -d
        fi
    done

    IFS=',' read -ra approach_array <<< "$approaches"
    for approach in "${approach_array[@]}"; do
      ./src/main/bash/benchmark-scenario.sh -a "$approach" -s "$scenario" -k "$k6Config" -p "$serverProfiles" -d "$delayCallDepth" -m "$delayInMillis" -c "$connections" -r "$requestsPerSecond" -w "$warmupDurationInSeconds" -t "$testDurationInSeconds" -C "$keep_csv"
    done

    for serverProfile in "${server_profile_array[@]}"; do
        composeFile="src/main/docker/docker-compose-$serverProfile.yaml"
        if [[ -f "$composeFile" ]]; then
            log "Stopping Docker containers using $composeFile"
            docker compose -f "$composeFile" down
        fi
    done
done

./src/main/python/results_chart.py -i "$resultsCsvFile" -o "$resultsPngFile"
./src/main/python/results_chart.py -i "$resultsCsvFile" -o "$resultsNettyPngFile" -a "loom-netty,webflux-netty" || true

endSeconds=$( date +%s )
durationInSeconds=$(( endSeconds - startSeconds ))

cat <<EOF > "$resultsMdFile"
# $(basename "$scenariosFile" .csv)

## Test Time

| **Name**                | **Value** |
|-------------------------|-----------|
| **Start Time (UTC)** | $(date -u -d @$startSeconds +"%Y-%m-%d %H:%M:%S") |
| **End Time (UTC)**   | $(date -u -d @$(( $(date +%s) )) +"%Y-%m-%d %H:%M:%S") |
| **Duration (hh:mm:ss)** | $(printf '%02d:%02d:%02d' $((durationInSeconds/3600)) $(( (durationInSeconds%3600)/60 )) $((durationInSeconds%60))) |

## System Specs

| **Name**                | **Value** |
|-------------------------|-----------|
| **Java Version**        | $(java --version | grep "Server") |
| **Python Version**      | $(python3 --version | awk '{print $2}') |
| **OS Version**          | $(grep "PRETTY_NAME" /etc/os-release | cut -d '"' -f 2) |
| **Kernel Version**      | $(uname -r) |
| **CPU Model**           | $(lscpu | grep "Model name" | sed 's/Model name:\s*//') |
| **CPU Cores**           | $(grep -c ^processor /proc/cpuinfo) |
| **RAM**                 | $(free -h | awk '/^Mem:/ {print $2 " total, " $7 " available"}') |
| **Disk**                | $(df -h --total | awk '/^total/ {print $2 " total, " $4 " available"}') |

## Scenarios

**Scenario file:** $scenariosFile

EOF

{
  echo "| Scenario | k6 Config | Server Profiles | Delay Call Depth | Delay (ms) | Connections | Requests per Second | Warmup Duration (s) | Test Duration (s) |"
  echo "|----------|-----------|-----------------|------------------|------------|-------------|---------------------|---------------------|------------------|"
  tail -n +2 "$scenariosFile" | while IFS=',' read -r scenario k6Config serverProfiles delayCallDepth delayInMillis connections requestsPerSecond warmupDurationInSeconds testDurationInSeconds; do
      if [[ -z "$scenario" || $scenario == "#"* ]]; then
          continue
      fi
      echo "| $scenario | $k6Config | $serverProfiles | $delayCallDepth | $delayInMillis | $connections | $requestsPerSecond | $warmupDurationInSeconds | $testDurationInSeconds |"
  done
} >> "$resultsMdFile"
log "Result description saved to $resultsMdFile"

log "Completed Loom and WebFlux benchmark after ${durationInSeconds}s"
