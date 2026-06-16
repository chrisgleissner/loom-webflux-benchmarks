#!/bin/bash
# Runs all benchmarks and saves results to build/results.

approaches="loom-tomcat,loom-netty,webflux-netty"
scenariosDir="src/main/resources/scenarios"
relativeScenariosPath="scenarios-default.csv"
resultsDir=build/results
resultsCsvFile="$resultsDir/results.csv"
resultsPngFile="$resultsDir/results.png"
resultsNettyPngFile="$resultsDir/results-netty.png"
keep_csv=false
warmupDurationInSecondsOverride=
testDurationInSecondsOverride=
scriptDir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repoRoot="$(cd -- "$scriptDir/../../.." && pwd)"
gradleUserHome="${GRADLE_USER_HOME:-$repoRoot/build/gradle-user-home}"
webfluxAppJar="$repoRoot/build/libs/loom-webflux-webflux.jar"
tomcatAppJar="$repoRoot/build/libs/loom-webflux-tomcat.jar"

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

composeStartupTimeoutInSeconds=120

# Starts the Docker container(s) for each server profile that has a compose file.
# Waits until the containers report healthy and aborts with diagnostics if any
# fail to start (e.g. host port already in use), so we never run the benchmark
# against a database that never came up.
start_server_profiles() {
  local -n _profiles=$1
  local serverProfile composeFile
  for serverProfile in "${_profiles[@]}"; do
    composeFile="src/main/docker/docker-compose-$serverProfile.yaml"
    [[ -f "$composeFile" ]] || continue
    log "Starting Docker container(s) using $composeFile"
    if ! docker compose -f "$composeFile" up -d --wait --wait-timeout "$composeStartupTimeoutInSeconds"; then
      log "Docker container(s) from $composeFile did not start cleanly; diagnostics follow"
      docker compose -f "$composeFile" ps || true
      docker compose -f "$composeFile" logs --tail 50 || true
      docker compose -f "$composeFile" down --remove-orphans || true
      return 1
    fi
  done
}

# Stops the Docker container(s) for each server profile that has a compose file.
stop_server_profiles() {
  local -n _profiles=$1
  local serverProfile composeFile
  for serverProfile in "${_profiles[@]}"; do
    composeFile="src/main/docker/docker-compose-$serverProfile.yaml"
    [[ -f "$composeFile" ]] || continue
    log "Stopping Docker container(s) using $composeFile"
    docker compose -f "$composeFile" down
  done
}

print_usage() {
  echo "Usage: $(basename "$0") [OPTION]... [SCENARIO_FILE]"
  echo "Runs benchmarks configured by a scenario file."
  echo
  echo "SCENARIO_FILE:     Scenario configuration CSV file in src/main/resources/scenarios/. Default: scenarios-default.csv"
  echo
  echo "OPTION:"
  echo "  -a <approaches>  Comma-separated list of approaches to test. Default: loom-tomcat, loom-netty, webflux-netty"
  echo "                   Supported approaches: platform-tomcat, loom-tomcat, loom-netty, webflux-netty"
  echo "  -C               Keep CSV files used to create chart. Default: false"
  echo "  -w <seconds>     Override warmupDurationInSeconds from the scenario CSV for all scenarios. Default: value from CSV"
  echo "  -t <seconds>     Override testDurationInSeconds from the scenario CSV for all scenarios. Default: value from CSV"
  echo "  -h               Print this help"
}

while getopts 'ha:Cw:t:' opt; do
  case "$opt" in
    h)
      print_usage
      exit 0
      ;;
    a) approaches="$OPTARG" ;;
    C) keep_csv=true ;;
    w) warmupDurationInSecondsOverride="$OPTARG" ;;
    t) testDurationInSecondsOverride="$OPTARG" ;;
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

cd "$repoRoot" || exit 1

scenariosFile="$scenariosDir/$relativeScenariosPath"

if [ ! -f "$scenariosFile" ]; then
  log "Scenario file $scenariosFile does not exist; terminating"
  exit 1
fi

log "Checking benchmark host tuning"
if ! "$scriptDir/tune-benchmark-host.sh"; then
  log "Benchmark host tuning check failed; terminating"
  exit 1
fi

startSeconds=$( date +%s )

log "Started Loom and WebFlux benchmark"
log "- Scenarios file: $scenariosFile"
log "- Approaches: $approaches"
log "- warmupDurationInSeconds override: ${warmupDurationInSecondsOverride:-(from CSV)}"
log "- testDurationInSeconds override: ${testDurationInSecondsOverride:-(from CSV)}"
echo

log "Building application jars"
mkdir -p "$gradleUserHome"
build_app_jar webflux-netty "$webfluxAppJar"
build_app_jar loom-tomcat "$tomcatAppJar"

./src/main/bash/log-system-specs.sh

log "Contents of $scenariosFile:"
cat "$scenariosFile"

while IFS=',' read -r scenario k6Config serverProfiles delayCallDepth delayInMillis connections requestsPerSecond warmupDurationInSeconds testDurationInSeconds; do
    if [[ -z "$scenario" || $scenario == "#"* ]]; then
        continue
    fi

    IFS='|' read -ra server_profile_array <<< "$serverProfiles"
    if ! start_server_profiles server_profile_array; then
        log "Aborting: Docker dependencies for scenario $scenario failed to start"
        exit 1
    fi

    IFS=',' read -ra approach_array <<< "$approaches"
    effectiveWarmupDurationInSeconds=${warmupDurationInSecondsOverride:-$warmupDurationInSeconds}
    effectiveTestDurationInSeconds=${testDurationInSecondsOverride:-$testDurationInSeconds}
    for approach in "${approach_array[@]}"; do
      if ! ./src/main/bash/benchmark-scenario.sh -a "$approach" -s "$scenario" -k "$k6Config" -p "$serverProfiles" -d "$delayCallDepth" -m "$delayInMillis" -c "$connections" -r "$requestsPerSecond" -w "$effectiveWarmupDurationInSeconds" -t "$effectiveTestDurationInSeconds" -C "$keep_csv"; then
        stop_server_profiles server_profile_array
        exit 1
      fi
    done

    stop_server_profiles server_profile_array
done < <(tail -n +2 "$scenariosFile")

./src/main/python/results_chart.py -i "$resultsCsvFile" -o "$resultsPngFile"
./src/main/python/results_chart.py -i "$resultsCsvFile" -o "$resultsNettyPngFile" -a "loom-netty,webflux-netty" || true

endSeconds=$( date +%s )
./src/main/bash/generate-results-markdown.sh "$scenariosFile" "$resultsDir" "$approaches" "$startSeconds" "$endSeconds" "$warmupDurationInSecondsOverride" "$testDurationInSecondsOverride"

durationInSeconds=$(( endSeconds - startSeconds ))
log "Completed Loom and WebFlux benchmark after ${durationInSeconds}s"
