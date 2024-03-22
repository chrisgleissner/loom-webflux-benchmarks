#!/bin/bash

if [[ $# -eq 0 ]] ; then
    echo 'Syntax: benchmark.sh ( loom | webflux )'
    exit 0
fi

approach=$1
duration=60s
users=10000
rate=$users
maxWorkers=$users
maxConnections=$users
url="http://localhost:8080/epoch-millis/$approach?delayMillis=100"
result="results/$approach.html"

printf "\n\n"
echo "Starting server with $approach approach"
cp -f build-$approach.gradle build.gradle
nohup ./gradlew bootrun >build/server.log 2>&1 &
until curl --output /dev/null --silent --head --fail $url; do printf '.'; sleep 1; done

printf "\n\n"
echo "Running benchmark: rate=$rate, max-workers=$maxWorkers, max-connections=$maxConnections, duration=$duration"
for i in {1..2}; do mkdir -p bin && echo "Test iteration #$i..." && echo "GET $url" | vegeta attack -duration=$duration -rate=$rate -max-workers=$maxWorkers -max-connections=$maxConnections | tee bin/results.bin | vegeta report && cat bin/results.bin | vegeta plot --title="$approach" > $result; done

printf "\n\n"
echo "Stopping server"
curl -X POST localhost:8080/actuator/shutdown

open $result
