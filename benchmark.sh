#!/bin/bash

if [[ $# -eq 0 ]] ; then
    echo 'Syntax: benchmark.sh ( loom | webflux )'
    exit 0
fi

testIterationDurationSeconds=60 # Duration of a test iteration. Each approach undergoes two test iterations.
delayMillis=100 # Server-side delay in milliseconds before a response is returned.
connections=5000 # Number of connections.
totalRate=$connections # Requests/s across all connections.

approach=$1 # The service approach that is being tested, either loom or webflux.
serviceHost=localhost # Server host which runs Spring Boot application under test.
servicePort=8080
serviceUrl="http://$serviceHost:$servicePort/epoch-millis/$approach?delayMillis=$delayMillis"
latencyResult="results/$approach-latency.html"

function start_service() {
  printf "\n\n"
  echo "Starting service with $approach approach"
  echo "Service URL: $serviceUrl"
  cp -f build-$approach.gradle build.gradle
  nohup ./gradlew bootrun  >build/server.log 2>&1 &
  until curl --output /dev/null --silent --head --fail $serviceUrl; do printf '.'; sleep 1; done
}

function stop_service() {
  printf "\n\n"
  echo "Stopping service"
  curl -X POST $serviceHost:$servicePort/actuator/shutdown
  while curl --output /dev/null --silent --head --fail $serviceUrl; do printf '.'; sleep 1; done
}

function benchmark_service() {
  printf "\n\n"
  echo "Running benchmark: totalRate=$totalRate/s, connections=$connections, delayMillis=$delayMillis, testIterationDuration=${testIterationDurationSeconds}s"
  mkdir -p bin

  echo
  echo "Warmup started at $( date )..."
  perform_requests

  echo
  echo "Test started at $( date )..."
  ./system-measure.sh $approach $testIterationDurationSeconds &
  perform_requests
  ./system-chart.py $approach results/$approach-system.csv results/$approach-system.png
}

function perform_requests() {
  echo "GET $serviceUrl" | vegeta attack -duration=${testIterationDurationSeconds}s -rate=$totalRate -connections=$connections -max-connections=$connections | tee bin/results.bin | vegeta report
  vegeta plot --title="$approach" < bin/results.bin > "$latencyResult"
}

start_service
benchmark_service
stop_service
# open $latencyResult
