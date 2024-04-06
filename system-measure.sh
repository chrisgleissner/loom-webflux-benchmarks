#!/bin/bash

if [[ $# -ne 2 ]] ; then
    echo 'Syntax: system-measure.sh ( loom | webflux ) <duration-in-seconds>'
    exit 0
fi

approach=$1
durationInSeconds=$2
saropts="-ur -n TCP,SOCK"
outputFile=results/$approach-system.csv

echo "Measuring system using $approach approach for $durationInSeconds seconds..."
sar $saropts -o sar.bin 1 $durationInSeconds >/dev/null && sadf -Udh sar.bin -- $saropts | cut -d ";" -f3,5,7,8,13,23,28-31 > $outputFile && rm sar.bin
echo "Updated $outputFile"