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
serviceHealthUrl="http://$serviceHost:$servicePort/actuator/health"
serviceApiBaseUrl="http://$serviceHost:$servicePort/$approach"
resultsDir=build/results
resultDir="$resultsDir"/"$scenario"
jvmCsvFilename="$resultDir/$approach"-jvm.csv
latencyCsvFilename="$resultDir/$approach"-latency.csv
systemCsvFilename="$resultDir/$approach"-system.csv
chartFilename="$resultDir/$approach".png

function log() {
  echo "$( date +"%H:%M:%S" )" "$1"
}

function start_service() {
  log "Starting service"
  SPRING_PROFILES_ACTIVE=$approach ./gradlew bootRun 2>&1 &
  until curl --output /dev/null --silent --head --fail "$serviceHealthUrl"; do printf '.'; sleep 1; done
}

function stop_service() {
  log "Stopping service"
  curl -X POST $serviceHost:$servicePort/actuator/shutdown
  while curl --output /dev/null --silent --head --fail "$serviceHealthUrl"; do printf '.'; sleep 1; done
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
  rm -f bin/jvm.csv

  # Start system-measure.sh after some delay to give k6 time to initialize
  (sleep 2 && ./system-measure.sh "$systemCsvFilename" "$durationInSeconds") &
  systemMeasurePid=$!

  load "$durationInSeconds"
  mv bin/jvm.csv "$jvmCsvFilename" && log "Saved $jvmCsvFilename"

  sleep 2
  ./chart.py "$approach: $scenario" "$latencyCsvFilename" "$systemCsvFilename" "$jvmCsvFilename" "$chartFilename"
  verify_chart_exists
  log "Saved $chartFilename"

  # Terminate system-measure.sh if it is misconfigured to run longer than k6
  if ps -p $systemMeasurePid > /dev/null; then
    kill $systemMeasurePid
    log "Terminated system-measure.sh process which may have overrun. Does config/scenarios.csv specify a duration which matches the corresponding k6 duration?"
  fi

  rm -f "$latencyCsvFilename"
  rm -f "$systemCsvFilename"
  rm -f "$jvmCsvFilename"
}

function verify_chart_exists() {
  if ! file "$chartFilename" | grep -q "PNG image data"; then
    log "Chart file $chartFilename does not exist or is not a valid PNG image; terminating"
    exit 1
  fi
}

function load() {
  _durationInSeconds=$1
  k6ConfigFile=config/"$k6Config"
  k6OutputFile=bin/k6.csv

  log "Issuing requests for ${_durationInSeconds}s using ${k6ConfigFile}..."
  k6 run --env DURATION_IN_SECONDS="${_durationInSeconds}" --out csv="$k6OutputFile" --env K6_CSV_TIME_FORMAT="unix_milli" --env DELAY_IN_MILLIS="$delayInMillis" --env SERVICE_API_BASE_URL="$serviceApiBaseUrl" --env VUS="$connections" --env RPS="$requestsPerSecond" "$k6ConfigFile"

  # csv: metric_name,timestamp,metric_value,check,error,error_code,expected_response,group,method,name,proto,scenario,service,status
  cat "$k6OutputFile" | grep http_req_duration | awk -F, '{print $2","$3","$14","$5","$6}' > "$latencyCsvFilename"

  log "Disk use:"
  du -h -d2 bin "$resultsDir"
  
  rm "$k6OutputFile"
  log "Saved $latencyCsvFilename"
}

printf "\n\n"
log "==> Benchmark of $scenario scenario for $approach approach <=="
log "k6Config=$k6Config, delayInMillis=$delayInMillis, connections=$connections, requestsPerSecond=$requestsPerSecond, warmupDurationInSeconds=$warmupDurationInSeconds, testDurationInSeconds=$testDurationInSeconds"

start_service
benchmark_service
stop_service
