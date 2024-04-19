#!/bin/bash
# Performs a benchmark of Loom and WebFlux for a single scenario.

if [[ $# -ne 8 ]] ; then
    echo 'Syntax: benchmark-scenario.sh <approach> <scenario> <k6Config> <delayInMillis> <connections> <requestsPerSecond> <testDurationInSeconds>'
    exit 1
fi

approach=$1
scenario=$2
k6Config=$3
delayInMillis=$4
connections=$5
requestsPerSecond=$6
warmupDurationInSeconds=$7
testDurationInSeconds=$8

serviceHost=localhost
servicePort=8080
serviceUrl="http://$serviceHost:$servicePort/epoch-millis/$approach?delayInMillis=$delayInMillis"
resultDir=results/"$scenario"
latencyCsvFilename="$resultDir/$approach"-latency.csv
systemCsvFilename="$resultDir/$approach"-system.csv

function log() {
  echo "$( date +"%H:%M:%S" )" "$1"
}

function start_service() {
  log "Starting service to listen at $serviceUrl"
  cp -f build-"$approach".gradle build.gradle
  ./gradlew bootrun 2>&1 &
  until curl --output /dev/null --silent --head --fail "$serviceUrl"; do printf '.'; sleep 1; done
}

function stop_service() {
  log "Stopping service"
  curl -X POST $serviceHost:$servicePort/actuator/shutdown
  while curl --output /dev/null --silent --head --fail "$serviceUrl"; do printf '.'; sleep 1; done
}

function benchmark_service() {
  mkdir -p bin
  mkdir -p "$resultDir"
  if (( warmupDurationInSeconds > 0 )); then
    load_and_measure_system warmup "$warmupDurationInSeconds"
  fi
  load_and_measure_system test "$testDurationInSeconds"
}

function load_and_measure_system() {
  phase=$1
  durationInSeconds=$2
  log "Starting $phase"
  sleep 2 && ./system-measure.sh "$systemCsvFilename" "$durationInSeconds" &
  load "$durationInSeconds"
  sleep 2
  log "Creating chart for approach $approach and scenario $scenario"
  ./chart.py "$approach: $scenario" "$latencyCsvFilename" "$systemCsvFilename" "$resultDir"/"$approach".png
  # rm "$latencyCsvFilename"
  # rm "$systemCsvFilename"
}

function load() {
  _durationInSeconds=$1
  log "Issuing requests for ${_durationInSeconds}s..."

  k6OutputFile=bin/k6.csv
  if [[ $k6Config == "" || $k6Config == "k6.js" ]]; then
    k6 run --vus "$connections" --duration "${_durationInSeconds}"s --rps "$requestsPerSecond" --out csv="$k6OutputFile" --env K6_CSV_TIME_FORMAT="unix_milli" --env SERVICE_URL="$serviceUrl" config/k6.js
  else
    k6 run --out csv="$k6OutputFile" --env K6_CSV_TIME_FORMAT="unix_milli" --env SERVICE_URL="$serviceUrl" config/"$k6Config"
  fi

  # csv: metric_name,timestamp,metric_value,check,error,error_code,expected_response,group,method,name,proto,scenario,service,status
  cat "$k6OutputFile" | grep http_req_duration | awk -F, '{print $2","$3","$14}' > "$latencyCsvFilename"

  log "Saved $latencyCsvFilename"
}

printf "\n\n"
log "Starting $scenario for $approach: k6Config=$k6Config, delayInMillis=$delayInMillis, connections=$connections, requestsPerSecond=$requestsPerSecond, warmupDurationInSeconds=$warmupDurationInSeconds, testDurationInSeconds=$testDurationInSeconds"

start_service
benchmark_service
stop_service
