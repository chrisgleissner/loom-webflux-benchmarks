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
benchmarkFailureReason=

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
curlConnectTimeoutInSeconds=1
curlMaxTimeInSeconds=3
# Run sar a few seconds past k6 so its 1-second samples cover the whole load window, including the
# requests that complete during k6's graceful stop. sar timestamps each sample at the END of its
# interval, so an N-second capture only yields samples up to second N-1; without this trailing margin
# the CPU/RAM/socket/throughput panels stop ~1-2 s before the latency/RPS panels. The analysis clips
# the plotted system series back to the load window, so these extra trailing samples never leak into
# the chart or the aggregates.
systemMeasureTrailingSeconds=3

log() {
  echo "$( date +"%H:%M:%S" )" "$1"
}

service_is_running() {
  [[ -n "$servicePid" ]] && ps -p "$servicePid" > /dev/null
}

service_health_check() {
  curl --output /dev/null --silent --head --fail --connect-timeout "$curlConnectTimeoutInSeconds" --max-time "$curlMaxTimeInSeconds" "$serviceHealthUrl"
}

# True when the just-launched JVM died because the port was already taken. This
# is the recoverable startup failure (reclaim + wait for the port, then retry),
# as opposed to a build error or an OOME at startup, where retrying is pointless.
service_log_reports_port_conflict() {
  [[ -s "$serviceLogTmpFile" ]] && \
    grep -qiE 'Port [0-9]+ was already in use|Address already in use|BindException' "$serviceLogTmpFile"
}

post_shutdown() {
  curl -fsS -X POST --connect-timeout "$curlConnectTimeoutInSeconds" --max-time "$curlMaxTimeInSeconds" "$serviceShutdownUrl" >/dev/null 2>&1
}

log_port_owner() {
  ss -H -tanp "sport = :$servicePort" || true
}

# Prints the distinct PIDs (one per line) of processes that currently hold the
# service port via a non-TIME-WAIT socket, or nothing if the port is free or
# its owner is not visible to this user. `ss -p` exposes PIDs for sockets owned
# by the current user without root, which is sufficient because the JVM runs as
# us. This lets us reclaim the port by killing the exact owner instead of every
# java process on the host.
service_port_owner_pids() {
  ss -H -tanp "sport = :$servicePort" 2>/dev/null \
    | awk 'toupper($1) != "TIME-WAIT"' \
    | grep -oE 'pid=[0-9]+' \
    | cut -d= -f2 \
    | sort -u
}

is_service_port_in_use() {
  # Consider the port busy while any non-TIME-WAIT socket is still bound to it.
  # A crashed JVM (e.g. OOME) closes its LISTEN socket early but leaves thousands
  # of abruptly torn-down connections behind; binding a fresh server then fails
  # with EADDRINUSE. TIME-WAIT sockets are reusable (SO_REUSEADDR) and ignored so
  # we do not wait needlessly for them to drain.
  #
  # Count non-TIME-WAIT sockets with awk rather than relying on `grep -v`'s exit
  # status: some grep implementations (notably ugrep, which is often installed
  # as `grep`) return "match found" for an inverted pattern on EMPTY input, which
  # made this predicate report the port as perpetually in use even when free and
  # blocked every service start. awk is portable and unambiguous here.
  local nonTimeWaitSockets
  nonTimeWaitSockets=$(ss -H -tan "sport = :$servicePort" 2>/dev/null \
    | awk 'toupper($1) != "TIME-WAIT" { count++ } END { print count + 0 }')
  (( nonTimeWaitSockets > 0 ))
}

# Returns 0 (true) only if a fresh server could actually bind the service port
# right now. This is the authoritative readiness check; ss-based heuristics are
# NOT, because is_service_port_in_use deliberately ignores TIME-WAIT sockets yet
# a server that does not reuse the address still fails to bind() while they
# linger. An OOME victim under tens of thousands of connections leaves thousands
# of such TIME-WAIT sockets behind, so the port reads "free" via ss while the
# next JVM dies with "Port 8080 was already in use" -- the exact failure that
# aborted the suite. We probe by attempting a real bind WITHOUT SO_REUSEADDR
# (the strictest case): if even that succeeds the embedded server will bind too,
# whatever its socket options. The probe creates only listening sockets and
# never accepts a connection, so it leaves no TIME-WAIT residue of its own.
port_is_bindable() {
  python3 - "$servicePort" <<'PY' >/dev/null 2>&1
import socket, sys

port = int(sys.argv[1])
families = [(socket.AF_INET, "0.0.0.0")]
if socket.has_ipv6:
    families.append((socket.AF_INET6, "::"))

for family, addr in families:
    try:
        sock = socket.socket(family, socket.SOCK_STREAM)
    except OSError:
        # Address family unsupported on this host: it cannot block a bind, skip.
        continue
    try:
        # Intentionally NOT setting SO_REUSEADDR: mirror the strictest server so
        # that a TIME-WAIT-blocked port is reported as not-yet-bindable.
        sock.bind((addr, port))
    except OSError:
        sock.close()
        sys.exit(1)
    sock.close()

sys.exit(0)
PY
}

# Polls until the service port is actually bindable, up to timeoutInSeconds.
# This is how we wait out the TIME-WAIT sockets a crashed JVM leaves behind:
# killing processes cannot clear them (they have no owner), only time can.
wait_for_port_bindable() {
  local timeoutInSeconds=$1
  local waited=0

  if ! port_is_bindable; then
    log "Waiting up to ${timeoutInSeconds}s for port $servicePort to become bindable"
  fi
  while ! port_is_bindable && (( waited < timeoutInSeconds )); do
    sleep 1
    ((waited++))
  done
  port_is_bindable
}

# Returns 0 (true) if the given PID exists AND is in D state (uninterruptible
# sleep, e.g. blocked on a stuck syscall). A D-state process ignores every
# signal, including SIGKILL, and `wait` on it blocks forever; this helper is
# the canonical way to detect the "corrupt JVM" the caller must work around
# instead of waiting for. Always use this before any `wait` on a background
# process so a stuck child cannot freeze the harness.
is_uninterruptible() {
  local pid=$1
  [[ -n "$pid" ]] && ps -o stat= -p "$pid" 2>/dev/null | grep -q '^D'
}

wait_for_service_exit() {
  local timeoutInSeconds=$1
  local waited=0

  while ps -p "$servicePid" > /dev/null && (( waited < timeoutInSeconds )); do
    sleep 1
    ((waited++))
  done
  ! ps -p "$servicePid" > /dev/null
}

wait_for_service_port_release() {
  local timeoutInSeconds=$1
  local waited=0

  while is_service_port_in_use && (( waited < timeoutInSeconds )); do
    sleep 1
    ((waited++))
  done
  ! is_service_port_in_use
}

# Force-frees the service port when it is still bound by a rogue or leftover
# JVM: one that survived its own scenario's cleanup, an OOME victim stuck
# mid-exit, or an orphan from a previous scenario that this fresh process never
# started (so $servicePid does not know about it). Reclamation is keyed off the
# port owner rather than $servicePid precisely so it works across scenarios and
# regardless of any -k/--kill-java flag.
#
# Strategy: SIGKILL the exact PID(s) bound to the port (graceful shutdown was
# already attempted by the caller, and these are processes we want gone now),
# then fall back to `killall -9 java` only when the owner cannot be identified
# (e.g. socket owned by another user). Returns 0 if the port is free on
# completion, 1 otherwise. A PID stuck in uninterruptible (D) sleep cannot be
# killed by anything until the kernel unblocks it; we log that explicitly so the
# failure is diagnosable rather than silent.
reclaim_service_port() {
  is_service_port_in_use || return 0

  log "Port $servicePort is still bound; forcing reclamation"
  log_port_owner

  local -a pids
  mapfile -t pids < <(service_port_owner_pids)
  if (( ${#pids[@]} > 0 )); then
    log "Killing port owner PID(s): ${pids[*]}"
    kill -9 "${pids[@]}" >/dev/null 2>&1 || true
    if wait_for_service_port_release 10; then
      log "Port $servicePort released after killing owner PID(s)"
      return 0
    fi
  fi

  log "Port $servicePort still bound; falling back to 'killall -9 java'"
  killall -9 java >/dev/null 2>&1 || true
  if wait_for_service_port_release 10; then
    log "Port $servicePort released after killall"
    return 0
  fi

  if (( ${#pids[@]} > 0 )) && is_uninterruptible "${pids[0]}"; then
    log "Port owner PID ${pids[0]} is in uninterruptible (D) sleep and cannot be killed until the kernel unblocks it"
  fi
  log "Port $servicePort is still in use after reclamation attempts"
  log_port_owner
  return 1
}

# Makes the service port truly bindable before we launch a JVM on it, recovering
# from both ways a crashed predecessor can poison the port:
#   * a still-live rogue/orphaned JVM holding the LISTEN socket -- killed by
#     reclaim_service_port (keyed off the port owner, so it works across
#     scenarios regardless of any -k/--kill-java flag), and
#   * thousands of TIME-WAIT sockets left by an OOME under load, which no process
#     owns and which only time can drain -- waited out via wait_for_port_bindable.
# Returns 0 once a real bind would succeed, 1 if the port cannot be freed within
# the timeout (e.g. a JVM wedged in uninterruptible D-state).
ensure_port_available() {
  local timeoutInSeconds=${1:-90}

  if port_is_bindable; then
    return 0
  fi

  if is_service_port_in_use; then
    log "Port $servicePort is held by a live process before start; reclaiming"
    reclaim_service_port || true
  else
    log "Port $servicePort is not bindable but owned by no live process; TIME-WAIT sockets from a crashed JVM are draining"
  fi

  wait_for_port_bindable "$timeoutInSeconds"
}

terminate_service() {
  if [[ -z "$servicePid" ]]; then
    return
  fi

  if ps -p "$servicePid" > /dev/null; then
    post_shutdown || true
    if ! wait_for_service_exit 15; then
      kill "$servicePid" >/dev/null 2>&1 || true
      if ! wait_for_service_exit 10; then
        log "Service PID $servicePid did not stop within 10s of SIGTERM; sending SIGKILL"
        kill -9 "$servicePid" >/dev/null 2>&1 || true
        wait_for_service_exit 5 || true
      fi
    fi
  fi

  # Do NOT `wait $servicePid`: a JVM in D state (uninterruptible sleep) will
  # never exit, and `wait` would block the entire script. The port-release
  # poll below already bounds the time we spend on a stuck JVM; if the port
  # is still bound afterwards we force-reclaim it (which targets whichever PID
  # actually owns the port, not just $servicePid).
  if ! wait_for_service_port_release 15; then
    log "Port $servicePort is still in use after stopping service"
    reclaim_service_port || log "Port $servicePort could not be freed; the next scenario's start will retry reclamation before aborting"
  fi
}

build_app_jar() {
  local activeProfile=$1
  local targetJar=$2
  local marker
  local -a builtJars

  log "Building application jar for $activeProfile"
  marker=$(mktemp)
  if ! DEBUG=false GRADLE_USER_HOME="$gradleUserHome" SPRING_PROFILES_ACTIVE="$activeProfile" ./gradlew --no-daemon --gradle-user-home "$gradleUserHome" bootJar --rerun-tasks; then
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
    # Do NOT `wait`: if the subshell's child (sar/sadc) ever gets stuck, the
    # `wait` would block the EXIT trap and prevent the script from exiting.
    # The subshell reaps itself when its child closes stdout.
  fi
  terminate_service
}

# A signal mid-run (Ctrl-C from the operator, or SIGTERM/SIGHUP when a parent
# script or the terminal goes away) must not bypass JVM cleanup and orphan a
# rogue process holding the port. Run the same cleanup, then disable the EXIT
# trap so it does not run a second time, and exit with the conventional
# 128+signal code.
on_signal() {
  log "Received termination signal; cleaning up before exit"
  cleanup_service
  trap - EXIT
  exit 130
}

trap cleanup_service EXIT
trap on_signal INT TERM HUP

# Launches the JVM once and waits up to 120s for it to become healthy.
# Return codes:
#   0  service is healthy
#   2  process exited during startup because the port was already in use
#      (EADDRINUSE) -- recoverable: the caller reclaims/waits for the port and
#      retries
#   1  any other failure (exited for another reason, or never became healthy)
launch_service_once() {
  local appJar=$1

  rm -f "$serviceLogTmpFile"

  local commaSeparatedServerProfiles="${serverProfiles//|/,}"
  local javaMajorVersion
  javaMajorVersion=$(java -version 2>&1 | awk -F '[\".]' '/version/ { print $2; exit }')
  local javaArgs=("--enable-native-access=ALL-UNNAMED" "-Xms2g" "-Xmx2g" "-XX:+ExitOnOutOfMemoryError" "-Djdk.tracePinnedThreads=full")
  if (( javaMajorVersion >= 25 )); then
    javaArgs+=("-XX:+UseCompactObjectHeaders")
  fi

  DEBUG=false SPRING_PROFILES_ACTIVE=$approach${commaSeparatedServerProfiles:+,$commaSeparatedServerProfiles} java "${javaArgs[@]}" -jar "$appJar" > >(tee "$serviceLogTmpFile") 2>&1 &
  servicePid=$!

  local startupTimeoutInSeconds=120
  while (( startupTimeoutInSeconds > 0 )); do
    if service_health_check; then
      log "Started service"
      return 0
    fi
    if ! ps -p "$servicePid" > /dev/null; then
      if service_log_reports_port_conflict; then
        log "Service process exited during startup because port $servicePort was already in use"
        return 2
      fi
      log "Service process exited before becoming healthy"
      tail -n 50 "$serviceLogTmpFile"
      return 1
    fi
    printf '.'
    sleep 1
    ((startupTimeoutInSeconds--))
  done
  log "Service did not become healthy within 120s"
  tail -n 50 "$serviceLogTmpFile"
  return 1
}

# Starts the service, recovering from a port poisoned by a crashed predecessor.
# Before each launch it makes the port genuinely bindable (kill any live owner,
# then wait out TIME-WAIT sockets); a launch that still loses a race for the port
# is retried rather than aborting the whole benchmark suite. Only a port conflict
# is retried -- a build error or an OOME at startup fails fast.
start_service() {
  log "Starting service"
  rm -f "$jvmCsvTmpFile"
  mkdir -p "$gradleUserHome"
  mkdir -p "$tmpDir"

  if [[ "$approach" == "platform-tomcat" || "$approach" == "loom-tomcat" ]]; then
    appJar="$tomcatAppJar"
  else
    appJar="$webfluxAppJar"
  fi

  if [[ ! -s "$appJar" ]]; then
    log "Application jar $appJar does not exist; building it"
    build_app_jar "$approach" "$appJar"
  fi

  local maxStartAttempts=3
  local attempt
  for (( attempt = 1; attempt <= maxStartAttempts; attempt++ )); do
    if ! ensure_port_available 90; then
      log "Port $servicePort could not be made available before start attempt $attempt/$maxStartAttempts"
      log_port_owner
      if (( attempt == maxStartAttempts )); then
        return 1
      fi
      reclaim_service_port || true
      continue
    fi

    launch_service_once "$appJar"
    case $? in
      0) return 0 ;;
      2)
        log "Start attempt $attempt/$maxStartAttempts lost a race for port $servicePort; reclaiming and retrying"
        reclaim_service_port || true
        ;;
      *)
        return 1
        ;;
    esac
  done

  log "Service could not claim port $servicePort after $maxStartAttempts attempts"
  log_port_owner
  return 1
}

stop_service() {
  log "Stopping service"
  terminate_service
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
    if ! load_and_measure_system warmup "$warmupDurationInSeconds"; then
      log "Warmup did not complete cleanly"
    fi
    if ! restart_service_if_needed; then
      benchmarkFailureReason=${benchmarkFailureReason:-"warmup left service unavailable"}
      record_failed_result
      return 0
    fi
  fi
  if ! load_and_measure_system test "$testDurationInSeconds"; then
    benchmarkFailureReason=${benchmarkFailureReason:-"test phase did not produce benchmark artifacts"}
    log "Test phase failed: $benchmarkFailureReason"
    record_failed_result
  fi
}

wait_for_system_csv_file() {
  # system-measure.sh only writes the CSV once sar has finished and sadf has converted the binary, i.e.
  # systemMeasureTrailingSeconds after k6 ends. Wait long enough to cover that trailing capture.
  timeout=$((systemMeasureTrailingSeconds + 12))
  while [[ ! -s "$systemCsvFile" && $timeout -gt 0 ]]; do
    sleep 1
    ((timeout--))
  done
  if [[ ! -s "$systemCsvFile" ]]; then
    log "System metrics file $systemCsvFile does not exist or is empty"
    return 1
  fi
}

load_and_measure_system() {
  phase=$1
  durationInSeconds=$2
  benchmarkFailureReason=
  log "Starting $phase"

  # Remove any CSV left from a previous phase so wait_for_system_csv_file only returns once THIS
  # phase's capture is complete (system-measure.sh writes the CSV at the very end of its run).
  rm -f "$systemCsvFile"

  # Start system-measure.sh together with k6 (no artificial lead) and let it run a few seconds past k6
  # so its samples cover the whole load window (see systemMeasureTrailingSeconds). The analysis clips
  # the plotted system series and the aggregates back to the load window, so the trailing samples only
  # ensure coverage and never leak into the results.
  local systemMeasureSeconds=$((durationInSeconds + systemMeasureTrailingSeconds))
  (./src/main/bash/system-measure.sh "$systemCsvFile" "$systemMeasureSeconds") &
  systemMeasurePid=$!

  if ! load "$durationInSeconds"; then
    [[ -f "$jvmCsvTmpFile" ]] && mv "$jvmCsvTmpFile" "$jvmCsvFile" 2>/dev/null || true
    stop_system_measure_if_running
    return 1
  fi
  if ! mv "$jvmCsvTmpFile" "$jvmCsvFile"; then
    benchmarkFailureReason="JVM metrics file $jvmCsvTmpFile does not exist"
    log "$benchmarkFailureReason"
    return 1
  fi
  log "Saved $jvmCsvFile"
  if ! wait_for_system_csv_file; then
    benchmarkFailureReason="System metrics file $systemCsvFile does not exist or is empty"
    stop_system_measure_if_running
    return 1
  fi

  if [ "$phase" == "test" ]; then
    if ! ./src/main/python/scenario_chart.py "$scenario" "$approach" "$latencyCsvFile" "$systemCsvFile" "$jvmCsvFile" "$chartFile" "$resultsCsvFile"; then
      benchmarkFailureReason="scenario_chart.py failed"
      stop_system_measure_if_running
      return 1
    fi
    if ! verify_chart_results; then
      benchmarkFailureReason="benchmark chart artifacts were not created"
      stop_system_measure_if_running
      return 1
    fi
  fi

  # By design sar runs systemMeasureTrailingSeconds past k6; wait_for_system_csv_file above already
  # blocked until it finished writing the CSV, so it is no longer running here. Stop it defensively in
  # case of an unexpected overrun.
  stop_system_measure_if_running

  if [ "$keep_csv" == "true" ]; then
    log "Keeping CSV files"
  else
    log "Removing CSV files"
    rm -f "$latencyCsvFile"
    rm -f "$systemCsvFile"
    rm -f "$jvmCsvFile"
  fi

  return 0
}

verify_chart_results() {
  if ! file "$chartFile" | grep -q "PNG image data"; then
    log "Chart file $chartFile does not exist or is not a valid PNG image"
    return 1
  fi
  if [ ! -f "$resultsCsvFile" ]; then
    log "Results file $resultsCsvFile does not exist"
    return 1
  fi
  return 0
}

stop_system_measure_if_running() {
  if [[ -n "$systemMeasurePid" ]] && ps -p "$systemMeasurePid" > /dev/null; then
    kill "$systemMeasurePid" >/dev/null 2>&1 || true
    # Do NOT `wait`: see cleanup_service for rationale.
  fi
}

record_failed_result() {
  local requestsError=${1:-1}
  log "Recording failed benchmark result for $scenario / $approach"
  rm -f "$chartFile"
  if ! ./src/main/python/scenario_chart.py --failed "$scenario" "$approach" "$resultsCsvFile" "$requestsError"; then
    log "Failed to record benchmark result for $scenario / $approach"
    exit 1
  fi
}

restart_service_if_needed() {
  if service_is_running; then
    return 0
  fi

  log "Service exited during benchmark phase"
  # Do NOT `wait $servicePid` here: if the crashed JVM landed in D state
  # (uninterruptible sleep), `wait` would block forever and freeze the run.
  # The port-release poll below is bounded and detects the stuck case.
  if ! wait_for_service_port_release 30; then
    log "Port $servicePort was not released after service exit; forcing reclamation"
    if ! reclaim_service_port; then
      log "Port $servicePort is still in use; recording failed result and continuing"
      return 1
    fi
  fi

  log "Restarting service for remaining benchmark phases"
  start_service
}

load() {
  _durationInSeconds=$1
  k6ConfigFile=src/main/resources/scenarios/"$k6Config"

  log "Issuing requests for ${_durationInSeconds}s using ${k6ConfigFile}..."
  k6 run --env DURATION_IN_SECONDS="${_durationInSeconds}" --out csv="$k6OutputTmpFile" --env K6_CSV_TIME_FORMAT="unix_milli" --env DELAY_CALL_DEPTH="$delayCallDepth" --env DELAY_IN_MILLIS="$delayInMillis" --env SERVICE_API_BASE_URL="$serviceApiBaseUrl" --env VUS="$connections" --env RPS="$requestsPerSecond" "$k6ConfigFile" 2>&1 | tee "$k6LogTmpFile"
  k6ExitCode=${PIPESTATUS[0]}
  if [[ ! -s "$k6OutputTmpFile" ]]; then
    benchmarkFailureReason="k6 did not produce CSV output"
    log "$benchmarkFailureReason"
    return 1
  fi
  hasFailedRequests=$(awk -F, '$1 == "http_req_failed" && $3 != 0 { found = 1 } END { print found + 0 }' "$k6OutputTmpFile")
  # csv: metric_name,timestamp,metric_value,check,error,error_code,expected_response,group,method,name,proto,scenario,service,status
  awk -F, '$1 == "http_req_duration" {print $2","$3","$14","$5","$6}' "$k6OutputTmpFile" > "$latencyCsvFile"
  if [[ ! -s "$latencyCsvFile" ]]; then
    benchmarkFailureReason="k6 produced no latency samples"
    log "$benchmarkFailureReason"
    rm -f "$k6OutputTmpFile"
    return 1
  fi

  if (( k6ExitCode != 0 )); then
    log "k6 exited with code $k6ExitCode; continuing with partial results"
  fi
  if (( hasFailedRequests > 0 )); then
    log "k6 recorded failed requests; continuing with error-marked results"
  fi
  if ! service_is_running; then
    log "Service process exited during load; continuing with captured results"
    tail -n 50 "$serviceLogTmpFile"
  fi

  log "Disk use:"
  du -h -d2 bin "$resultsDir"
  df -h

  rm "$k6OutputTmpFile"
  log "Saved $latencyCsvFile"
}

printf "\n\n"
log "==> Benchmark of $scenario scenario for $approach approach <=="
log "k6Config=$k6Config, serverProfiles=$serverProfiles, delayCallDepth=$delayCallDepth, delayInMillis=$delayInMillis, connections=$connections, requestsPerSecond=$requestsPerSecond, warmupDurationInSeconds=$warmupDurationInSeconds, testDurationInSeconds=$testDurationInSeconds"

# A service that cannot start (e.g. its port is still poisoned by a crashed
# predecessor after all reclaim/wait/retry attempts) must NOT abort the whole
# suite: record a failed data point for this approach and let the next approach
# and scenario run. Otherwise a single OOME would forfeit every remaining test.
if start_service; then
  benchmark_service
  stop_service
else
  benchmarkFailureReason=${benchmarkFailureReason:-"service failed to start (port $servicePort could not be claimed)"}
  log "Recording failed result and continuing: $benchmarkFailureReason"
  record_failed_result
fi
capture_log_errors
