#!/bin/bash
# Benchmarks an approach for a specific scenario.

print_usage() {
    echo "Syntax: $(basename "$0") [-h] -a <approach> -s <scenario> -k <k6Config> -p <serverProfile> -d <delayCallDepth> -m <delayInMillis> -c <connections> [-r <requestsPerSecond>] -w <warmupDurationInSeconds> -t <testDurationInSeconds> -C <keepCsvFiles>"
    echo "Options:"
    echo "  -a approach:                  Approach to test: platform-tomcat, loom-tomcat, loom-netty or webflux-netty"
    echo "  -s <scenario>                 The scenario to benchmark."
    echo "  -k <k6Config>                 Client-side K6 configuration file."
    echo "  -p <serverProfile>            Server profile. If it contains non-empty value foo, then src/main/docker/docker-compose-foo.yaml is used"
    echo "                                  to start/stop Docker containers before/after each scenario, and 'foo' is appended to the server's Spring profiles."
    echo "  -d <delayCallDepth>           The delay call depth. If > 0, the service calls itself recursively the specified number of times before delaying."
    echo "  -m <delayInMillis>            The delay in milliseconds."
    echo "  -c <connections>              The number of connections, i.e. virtual users."
    echo "  -r <requestsPerSecond>        The number of requests per second across all connections."
    echo "  -w <warmupDurationInSeconds>  The duration of the warmup phase in seconds."
    echo "  -t <testDurationInSeconds>    The duration of the test phase in seconds."
    echo "  -C <keepCsvFiles>             Keep CSV files used to create chart. Can be true or false."
    echo "  -h                            Print this help"
    exit 1
}

while getopts "h:a:s:p:k:d:m:c:r:w:t:C:" opt; do
  case ${opt} in
    a) approach=$OPTARG ;;
    s) scenario=$OPTARG ;;
    k) k6Config=$OPTARG ;;
    p) serverProfile=$OPTARG ;;
    d) delayCallDepth=$OPTARG ;;
    m) delayInMillis=$OPTARG ;;
    c) connections=$OPTARG ;;
    r) requestsPerSecond=$OPTARG ;;
    w) warmupDurationInSeconds=$OPTARG ;;
    t) testDurationInSeconds=$OPTARG ;;
    C) keep_csv=$OPTARG ;;
    h)
      print_usage
      exit 0
      ;;
    \? )
      echo "Invalid option: $OPTARG" 1>&2
      print_usage
      exit 1
      ;;
    : )
      echo "Invalid option: $OPTARG requires an argument" 1>&2
      print_usage
      exit 1
      ;;
  esac
done
shift $((OPTIND -1))

if [ -z "$approach" ] || [ -z "$scenario" ] || [ -z "$k6Config" ] || [ -z "$delayCallDepth" ] || [ -z "$delayInMillis" ] || [ -z "$connections" ] || [ -z "$warmupDurationInSeconds" ] || [ -z "$testDurationInSeconds" ]; then
  echo "All arguments are required"
  print_usage
fi

serviceHost=localhost
servicePort=8080
serviceHealthUrl="http://$serviceHost:$servicePort/actuator/health"
serviceShutdownUrl="http://$serviceHost:$servicePort/actuator/shutdown"
serviceApiBaseUrl="http://$serviceHost:$servicePort/$approach"

resultsDir=build/results
resultDir="$resultsDir"/"$scenario"
jvmCsvFile="$resultDir/$approach"-jvm.csv
latencyCsvFile="$resultDir/$approach"-latency.csv
systemCsvFile="$resultDir/$approach"-system.csv
chartFile="$resultDir/$approach".png
resultsCsvFile="$resultsDir/results.csv"
clientErrorLogFile="$resultDir/$approach"-client-error.log
serviceErrorLogFile="$resultDir/$approach"-service-error.log

tmpDir=bin
jvmCsvTmpFile="$tmpDir"/jvm.csv
k6OutputTmpFile="$tmpDir"/k6.csv
k6LogTmpFile="$tmpDir"/k6.log
serviceLogTmpFile="$tmpDir"/service.log

log() {
  echo "$( date +"%H:%M:%S" )" "$1"
}

start_service() {
  log "Starting service"
  rm -f "$serviceLogTmpFile"

  SPRING_PROFILES_ACTIVE=$approach${serverProfile:+,$serverProfile} ./gradlew bootRun > >(tee "$serviceLogTmpFile") 2>&1 &
  until curl --output /dev/null --silent --head --fail "$serviceHealthUrl"; do printf '.'; sleep 1; done
  log "Started service"
}

stop_service() {
  log "Stopping service"
  curl -X POST "$serviceShutdownUrl"
  while curl --output /dev/null --silent --head --fail "$serviceHealthUrl"; do printf '.'; sleep 1; done
  log "Stopped service"
}

capture_log_errors() {
  rm -f "$clientErrorLogFile" "$serviceErrorLogFile"

  grep -i -A 2 -e "\bERROR\b" "$k6LogTmpFile" | grep -vie "no error" >> "$clientErrorLogFile"
  grep -i -A 2 -e "\bERROR\b" -e "ThreadContinuation.onPinned" "$serviceLogTmpFile" | grep -vie "no error" >> "$serviceErrorLogFile"

  # Check and remove empty log files
  for logFile in "$serviceErrorLogFile" "$clientErrorLogFile"; do
    [[ ! -s "$logFile" ]] && rm -f "$logFile"
  done

  rm -f "$k6LogTmpFile" "$serviceLogTmpFile"
}

benchmark_service() {
  mkdir -p "$tmpDir"
  mkdir -p "$resultDir"
  if (( warmupDurationInSeconds > 0 )); then
    load_and_measure_system warmup "$warmupDurationInSeconds"
  fi
  load_and_measure_system test "$testDurationInSeconds"
}

load_and_measure_system() {
  phase=$1
  durationInSeconds=$2
  log "Starting $phase"
  rm -f "$jvmCsvTmpFile"

  # Start system-measure.sh after some delay to give k6 time to initialize
  (sleep 2 && ./src/main/bash/system-measure.sh "$systemCsvFile" "$durationInSeconds") &
  systemMeasurePid=$!

  load "$durationInSeconds"
  mv "$jvmCsvTmpFile" "$jvmCsvFile" && log "Saved $jvmCsvFile"
  sleep 2

  if [ "$phase" == "test" ]; then
    ./src/main/python/scenario_chart.py "$scenario" "$approach" "$latencyCsvFile" "$systemCsvFile" "$jvmCsvFile" "$chartFile" "$resultsCsvFile"
    verify_chart_results
  fi

  # Terminate system-measure.sh if it is misconfigured to run longer than k6
  if ps -p $systemMeasurePid > /dev/null; then
    kill $systemMeasurePid
    log "Terminated system-measure.sh process which may have overrun. Does the scenario specify a duration which matches the corresponding k6 duration?"
  fi

  if [ "$keep_csv" == "true" ]; then
    log "Keeping CSV files"
  else
    log "Removing CSV files"
    rm -f "$latencyCsvFile"
    rm -f "$systemCsvFile"
    rm -f "$jvmCsvFile"
  fi
}

verify_chart_results() {
  if ! file "$chartFile" | grep -q "PNG image data"; then
    log "Chart file $chartFile does not exist or is not a valid PNG image; terminating"
    exit 1
  fi
  if [ ! -f "$resultsCsvFile" ]; then
    log "Results file $resultsCsvFile does not exist; terminating"
    exit 1
  fi
}

load() {
  _durationInSeconds=$1
  k6ConfigFile=src/main/resources/scenarios/"$k6Config"

  log "Issuing requests for ${_durationInSeconds}s using ${k6ConfigFile}..."
  k6 run --env DURATION_IN_SECONDS="${_durationInSeconds}" --out csv="$k6OutputTmpFile" --env K6_CSV_TIME_FORMAT="unix_milli" --env DELAY_CALL_DEPTH="$delayCallDepth" --env DELAY_IN_MILLIS="$delayInMillis" --env SERVICE_API_BASE_URL="$serviceApiBaseUrl" --env VUS="$connections" --env RPS="$requestsPerSecond" "$k6ConfigFile" 2>&1 | tee "$k6LogTmpFile"

  # csv: metric_name,timestamp,metric_value,check,error,error_code,expected_response,group,method,name,proto,scenario,service,status
  # shellcheck disable=SC2002
  cat "$k6OutputTmpFile" | grep http_req_duration | awk -F, '{print $2","$3","$14","$5","$6}' > "$latencyCsvFile"

  log "Disk use:"
  du -h -d2 bin "$resultsDir"
  df -h

  rm "$k6OutputTmpFile"
  log "Saved $latencyCsvFile"
}

printf "\n\n"
log "==> Benchmark of $scenario scenario for $approach approach <=="
log "k6Config=$k6Config, serverProfile=$serverProfile, delayCallDepth=$delayCallDepth, delayInMillis=$delayInMillis, connections=$connections, requestsPerSecond=$requestsPerSecond, warmupDurationInSeconds=$warmupDurationInSeconds, testDurationInSeconds=$testDurationInSeconds"

start_service
benchmark_service
stop_service
capture_log_errors
