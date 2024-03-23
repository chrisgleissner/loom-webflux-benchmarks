#!/bin/bash

if [[ $# -eq 0 ]] ; then
    echo 'Syntax: benchmark.sh ( loom | webflux )'
    exit 0
fi

testIterationDuration=60s # Duration of a test iteration. Each approach undergoes two test iterations.
delayMillis=100 # Server-side delay in milliseconds before a response is returned.
connections=5000 # Number of connections.
workers=5000 # Number of workers, i.e. users.
totalRate=5000 # Requests/s across all connections.

approach=$1 # The service approach that is being tested, either loom or webflux.
serviceHost=localhost # Server host which runs Spring Boot application under test.
servicePort=8080
serviceUrl="http://$serviceHost:$servicePort/epoch-millis/$approach?delayMillis=$delayMillis"
serviceHeapSize=512m
result="results/$approach.html"

# Start service
printf "\n\n"
echo "Starting service with $approach approach"
echo "Service URL: $serviceUrl"
cp -f build-$approach.gradle build.gradle
nohup ./gradlew bootrun  >build/server.log 2>&1 &
until curl --output /dev/null --silent --head --fail $serviceUrl; do printf '.'; sleep 1; done

# Benchmark service
printf "\n\n"
echo "Running benchmark: totalRate=$totalRate/s, connections=$connections, workers=$workers, delayMillis=$delayMillis, testIterationDuration=$testIterationDuration"
for i in {1..2}; do mkdir -p bin && echo "Test iteration #$i started at $( date )..." && echo "GET $serviceUrl" | vegeta attack -duration=$testIterationDuration -rate=$totalRate -workers=$workers -max-workers=$workers -connections=$connections -max-connections=$connections | tee bin/results.bin | vegeta report && cat bin/results.bin | vegeta plot --title="$approach" > $result; done

# Stop service
printf "\n\n"
echo "Stopping service"
curl -X POST $serviceHost:$servicePort/actuator/shutdown

# Open benchmark result in browser
open $result
