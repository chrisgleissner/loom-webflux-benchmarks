#!/bin/bash
# Measures the system load and creates a CSV file.

if [[ $# -ne 2 ]] ; then
    echo 'Syntax: system-measure.sh <outputCsvFilename> <durationInSeconds>'
    exit 0
fi

outputCsvFilename=$1
durationInSeconds=$2
sarOptions="-ur -n TCP,SOCK"

echo "Measuring system for ${durationInSeconds}s..."

# Columns: # hostname	interval	timestamp	CPU	%user	%nice	%system	%iowait	%steal	%idle[...]	kbmemfree	kbavail	kbmemused	%memused	kbbuffers	kbcached	kbcommit	%commit	kbactive	kbinact	kbdirty	totsck	tcpsck	udpsck	rawsck	ip-frag	tcp-tw	active/s	passive/s	iseg/s	oseg/s
sar $sarOptions -o bin/sar.bin 1 "$durationInSeconds" >/dev/null && sadf -Udh bin/sar.bin -- $sarOptions | cut -d ";" -f3,5,7,8,14,23,28-31 > "$outputCsvFilename" && rm bin/sar.bin

echo "Saved $outputCsvFilename"