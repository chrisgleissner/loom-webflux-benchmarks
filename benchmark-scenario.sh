#!/bin/bash
# Performs a benchmark of Loom and WebFlux for a single scenario.

if [[ $# -ne 7 ]] ; then
    echo 'Syntax: benchmark-scenario.sh <approach> <scenario> <delayInMillis> <connections> <requestsPerSecond> <testDurationInSeconds>'
    exit 1
fi

approach=$1
scenario=$2
delayInMillis=$3
connections=$4
requestsPerSecond=$5
warmupDurationInSeconds=$6
testDurationInSeconds=$7

serviceHost=localhost
servicePort=8080
serviceUrl="http://$serviceHost:$servicePort/epoch-millis/$approach?delayInMillis=$delayInMillis"
resultDir=results/"$scenario"
latencyCsvFilename="$resultDir/$approach"-latency.csv
systemCsvFilename="$resultDir/$approach"-system.csv

function start_service() {
  echo "Starting service to listen at $serviceUrl"
  cp -f build-"$approach".gradle build.gradle
  nohup ./gradlew bootrun  >build/server.log 2>&1 &
  until curl --output /dev/null --silent --head --fail "$serviceUrl"; do printf '.'; sleep 1; done
}

function stop_service() {
  echo "Stopping service"
  curl -X POST $serviceHost:$servicePort/actuator/shutdown
  while curl --output /dev/null --silent --head --fail "$serviceUrl"; do printf '.'; sleep 1; done
}

function benchmark_service() {
  mkdir -p bin
  mkdir -p "$resultDir"
  load_and_measure_system warmup "$warmupDurationInSeconds"
  sleep 2
  load_and_measure_system test "$testDurationInSeconds"
}

function load_and_measure_system() {
  phase=$1
  durationInSeconds=$2
  echo
  echo "Starting $phase at $( date -Is )"
  ./system-measure.sh "$systemCsvFilename" "$durationInSeconds" &
  loadWithVegeta "$durationInSeconds"
  ./chart.py "$approach ($scenario): delay=${delayInMillis}ms, connections=$connections, requests=$requestsPerSecond/s" "$latencyCsvFilename" "$systemCsvFilename" "$resultDir"/"$approach".png
  rm "$latencyCsvFilename"
  rm "$systemCsvFilename"
}

function loadWithVegeta() {
  _durationInSeconds=$1
  echo
  echo "Issuing requests for ${_durationInSeconds}s with vegeta..."
  echo "GET $serviceUrl" | vegeta attack -duration="${_durationInSeconds}"s -rate="$requestsPerSecond" -connections="$connections" -max-connections="$connections" | tee bin/results.bin | vegeta report
  vegeta encode --to csv < bin/results.bin | awk -F, '{print $1","$2","$3}' > "$latencyCsvFilename"
  echo "Saved $latencyCsvFilename"
}

printf "\n\n"
echo "Starting $scenario for $approach: delayInMillis=$delayInMillis, connections=$connections, requestsPerSecond=$requestsPerSecond, warmupDurationInSeconds=$warmupDurationInSeconds, testDurationInSeconds=$testDurationInSeconds"

start_service
benchmark_service
stop_service
