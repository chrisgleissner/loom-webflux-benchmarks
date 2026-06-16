#!/bin/bash
# Measures the system load and creates a CSV file.

if [[ $# -ne 2 ]] ; then
    echo 'Syntax: system-measure.sh <outputCsvFilename> <durationInSeconds>'
    exit 0
fi

outputCsvFilename=$1
durationInSeconds=$2
sarOptions="-ur -n TCP,SOCK,DEV --iface=lo"
sarBinFile=bin/sar.bin
sadfErrorFile=$(mktemp)

function cleanup() {
  rm -f "$sadfErrorFile"
}

trap cleanup EXIT

function log() {
  echo "$( date +"%H:%M:%S" )" "$1"
}

log "Measuring system for ${durationInSeconds}s..."
mkdir -p bin "$(dirname "$outputCsvFilename")"
rm -f "$sarBinFile"
# Columns: # hostname;interval;timestamp;CPU;%user;%nice;%system;%iowait;%steal;%idle[...];kbmemfree;kbavail;kbmemused;%memused;kbbuffers;kbcached;kbcommit;%commit;kbactive;kbinact;kbdirty;IFACE;rxpck/s;txpck/s;rxkB/s;txkB/s;rxcmp/s;txcmp/s;rxmcst/s;%ifutil[...];totsck;tcpsck;udpsck;rawsck;ip-frag;tcp-tw;active/s;passive/s;iseg/s;oseg/s
if ! sar $sarOptions -o "$sarBinFile" 1 "$durationInSeconds" >/dev/null; then
  echo "Failed to capture system metrics with sar" >&2
  exit 1
fi
if ! sadf -Udh "$sarBinFile" -- $sarOptions 2>"$sadfErrorFile" | cut -d ";" -f3,5,7,8,14,23-26,32,37-40 > "$outputCsvFilename"; then
  cat "$sadfErrorFile" >&2
  exit 1
fi
grep -v "End of system activity file unexpected" "$sadfErrorFile" >&2 || true
if [[ ! -s "$outputCsvFilename" ]]; then
  echo "System metrics CSV is empty: $outputCsvFilename" >&2
  exit 1
fi
log "Saved $outputCsvFilename"
