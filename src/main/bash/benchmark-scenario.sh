#!/bin/bash
# Benchmarks an approach for a specific scenario.

print_usage() {
    echo "Syntax: $(basename "$0") [-h] -a <approach> -s <scenario> -k <k6Config> -p <serverProfiles> -d <delayCallDepth> -m <delayInMillis> -c <connections> [-r <requestsPerSecond>] -w <warmupDurationInSeconds> -t <testDurationInSeconds> -C <keepCsvFiles>"
    echo "Options:"
    echo "  -a approach:                  Approach to test: platform-tomcat, loom-tomcat, loom-netty or webflux-netty"
    echo "  -s <scenario>                 The scenario to benchmark."
    echo "  -k <k6Config>                 Client-side K6 configuration file."
    echo "  -p <serverProfiles>           Server profiles, pipe-separated. If it contains non-empty value foo, then src/main/docker/docker-compose-foo.yaml is used"
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
    p) serverProfiles=$OPTARG ;;
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
scriptDir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repoRoot="$(cd -- "$scriptDir/../../.." && pwd)"
gradleUserHome="${GRADLE_USER_HOME:-$repoRoot/build/gradle-user-home}"
webfluxAppJar="$repoRoot/build/libs/loom-webflux-webflux.jar"
tomcatAppJar="$repoRoot/build/libs/loom-webflux-tomcat.jar"
servicePid=
systemMeasurePid=

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

build_app_jar() {
  local activeProfile=$1
  local targetJar=$2
  local marker
  local -a builtJars

  log "Building application jar for $activeProfile"
  marker=$(mktemp)
  if ! DEBUG=false GRADLE_USER_HOME="$gradleUserHome" SPRING_PROFILES_ACTIVE="$activeProfile" ./gradlew --gradle-user-home "$gradleUserHome" bootJar --rerun-tasks; then
    rm -f "$marker"
    exit 1
  fi

  mapfile -t builtJars < <(find "$repoRoot/build/libs" -maxdepth 1 -type f -name "*.jar" ! -name "*-plain.jar" -newer "$marker" -print | sort)
  rm -f "$marker"
  if (( ${#builtJars[@]} != 1 )); then
    log "Expected one bootJar output for $activeProfile, found ${#builtJars[@]}; terminating"
    printf '%s\n' "${builtJars[@]}"
    exit 1
  fi
  cp "${builtJars[0]}" "$targetJar"
}

cleanup_service() {
  if [[ -n "$systemMeasurePid" ]] && ps -p "$systemMeasurePid" > /dev/null; then
    kill "$systemMeasurePid" >/dev/null 2>&1 || true
    wait "$systemMeasurePid" 2>/dev/null || true
  fi
  if [[ -n "$servicePid" ]] && ps -p "$servicePid" > /dev/null; then
    curl -fsS -X POST "$serviceShutdownUrl" >/dev/null 2>&1 || true
    kill "$servicePid" >/dev/null 2>&1 || true
    wait "$servicePid" 2>/dev/null || true
  fi
}

trap cleanup_service EXIT

start_service() {
  log "Starting service"
  rm -f "$serviceLogTmpFile"
  rm -f "$jvmCsvTmpFile"
  mkdir -p "$gradleUserHome"

  if [[ "$approach" == "platform-tomcat" || "$approach" == "loom-tomcat" ]]; then
    appJar="$tomcatAppJar"
  else
    appJar="$webfluxAppJar"
  fi

  if [[ ! -s "$appJar" ]]; then
    log "Application jar $appJar does not exist; building it"
    build_app_jar "$approach" "$appJar"
  fi

  local commaSeparatedServerProfiles="${serverProfiles//|/,}"
  local javaMajorVersion
  javaMajorVersion=$(java -version 2>&1 | awk -F '[\".]' '/version/ { print $2; exit }')
  local javaArgs=("--enable-native-access=ALL-UNNAMED" "-Xms2g" "-Xmx2g" "-XX:+ExitOnOutOfMemoryError" "-Djdk.tracePinnedThreads=full")
  if (( javaMajorVersion >= 25 )); then
    javaArgs+=("-XX:+UseCompactObjectHeaders")
  fi

  DEBUG=false SPRING_PROFILES_ACTIVE=$approach${commaSeparatedServerProfiles:+,$commaSeparatedServerProfiles} java "${javaArgs[@]}" -jar "$appJar" > >(tee "$serviceLogTmpFile") 2>&1 &
  servicePid=$!

  startupTimeoutInSeconds=120
  while (( startupTimeoutInSeconds > 0 )); do
    if curl --output /dev/null --silent --head --fail "$serviceHealthUrl"; then
      log "Started service"
      return
    fi
    if ! ps -p "$servicePid" > /dev/null; then
      log "Service process exited before becoming healthy"
      tail -n 50 "$serviceLogTmpFile"
      exit 1
    fi
    printf '.'
    sleep 1
    ((startupTimeoutInSeconds--))
  done
  log "Service did not become healthy within 120s"
  tail -n 50 "$serviceLogTmpFile"
  exit 1
}

stop_service() {
  log "Stopping service"
  if ps -p "$servicePid" > /dev/null; then
    curl -fsS -X POST "$serviceShutdownUrl" >/dev/null || true
    while curl --output /dev/null --silent --head --fail "$serviceHealthUrl"; do printf '.'; sleep 1; done
    wait "$servicePid" || true
  fi
  log "Stopped service"
}

capture_log_errors() {
  rm -f "$clientErrorLogFile" "$serviceErrorLogFile"

  grep -Ei -A 2 -e '(^|[[:space:]])(ERROR|ERRO)([[:space:]]|:|$)' "$k6LogTmpFile" | grep -vie "no error" >> "$clientErrorLogFile"
  grep -E -A 2 -e '(^|[[:space:]])ERROR([[:space:]]|:|$)' -e "ThreadContinuation.onPinned" "$serviceLogTmpFile" | grep -vie "no error" >> "$serviceErrorLogFile"

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

wait_for_system_csv_file() {
  timeout=10
  while [[ ! -s "$systemCsvFile" && $timeout -gt 0 ]]; do
    sleep 1
    ((timeout--))
  done
  if [[ ! -s "$systemCsvFile" ]]; then
    log "System metrics file $systemCsvFile does not exist or is empty; terminating"
    exit 1
  fi
}

load_and_measure_system() {
  phase=$1
  durationInSeconds=$2
  log "Starting $phase"

  # Start system-measure.sh after some delay to give k6 time to initialize
  (sleep 2 && ./src/main/bash/system-measure.sh "$systemCsvFile" "$durationInSeconds") &
  systemMeasurePid=$!

  load "$durationInSeconds"
  if ! mv "$jvmCsvTmpFile" "$jvmCsvFile"; then
    log "JVM metrics file $jvmCsvTmpFile does not exist; terminating"
    exit 1
  fi
  log "Saved $jvmCsvFile"
  wait_for_system_csv_file

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
  k6ExitCode=${PIPESTATUS[0]}
  if (( k6ExitCode != 0 )); then
    log "k6 failed with exit code $k6ExitCode; terminating"
    exit "$k6ExitCode"
  fi
  hasFailedRequests=$(awk -F, '$1 == "http_req_failed" && $3 != 0 { found = 1 } END { print found + 0 }' "$k6OutputTmpFile")
  if (( hasFailedRequests > 0 )); then
    log "k6 recorded at least one non-zero http_req_failed sample; terminating"
    exit 1
  fi
  if ! ps -p "$servicePid" > /dev/null; then
    log "Service process exited during load; terminating"
    tail -n 50 "$serviceLogTmpFile"
    exit 1
  fi

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
log "k6Config=$k6Config, serverProfiles=$serverProfiles, delayCallDepth=$delayCallDepth, delayInMillis=$delayInMillis, connections=$connections, requestsPerSecond=$requestsPerSecond, warmupDurationInSeconds=$warmupDurationInSeconds, testDurationInSeconds=$testDurationInSeconds"

start_service
benchmark_service
stop_service
capture_log_errors
