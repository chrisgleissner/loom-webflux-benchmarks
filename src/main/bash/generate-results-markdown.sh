#!/bin/bash
# Generate results Markdown file with benchmark setup and results.

if [ "$#" -ne 5 ]; then
    echo "Usage: $0 <scenarios_file> <results_dir> <approaches> <start_seconds> <end_seconds>"
    exit 1
fi

scenarios_file="$1"
results_dir="$2"
approaches="$3"
start_seconds="$4"
end_seconds="$5"
results_md_file="$results_dir/results.md"
duration=$((end_seconds - start_seconds))

format_date() {
    local seconds=$1
    date -u -d @"$seconds" +"%Y-%m-%d %H:%M:%S"
}

format_duration() {
    local seconds=$1
    printf '%02d:%02d:%02d' $((seconds / 3600)) $(( (seconds % 3600) / 60 )) $((seconds % 60))
}

table_row() {
    local name=$1
    local value=$2
    echo "| **$name** | $value |"
}

markdown="# $(basename "$scenarios_file" .csv)\n"

markdown+="\n## Test Time\n\n"
markdown+="| **Name**                | **Value** |\n"
markdown+="|-------------------------|-----------|\n"
markdown+="$(table_row "Start (UTC)" "$(format_date "$start_seconds")")\n"
markdown+="$(table_row "End (UTC)" "$(format_date "$end_seconds")")\n"
markdown+="$(table_row "Duration (hh:mm:ss)" "$(format_duration "$duration")")\n"

markdown+="\n## System Specs\n\n"
markdown+="| **Name**                | **Value** |\n"
markdown+="|-------------------------|-----------|\n"
markdown+="$(table_row "Java" "$(java --version | grep 'Server')")\n"
markdown+="$(table_row "Python" "$(python3 --version | awk '{print $2}')")\n"
markdown+="$(table_row "OS" "$(grep 'PRETTY_NAME' /etc/os-release | cut -d '"' -f 2)")\n"
markdown+="$(table_row "Kernel" "$(uname -r)")\n"
markdown+="$(table_row "CPU" "$(lscpu | grep "Model name" | sed 's/Model name:\s*//')")\n"
markdown+="$(table_row "CPU Cores" "$(grep -c ^processor /proc/cpuinfo)")\n"
markdown+="$(table_row "RAM" "$(free -h | awk '/^Mem:/ {print $2 " total, " $7 " available"}')")\n"
markdown+="$(table_row "Disk" "$(df -h --total | awk '/^total/ {print $2 " total, " $4 " available"}')")\n"

markdown+="\n## Scenarios\n\n"
markdown+="**Scenario file:** $scenarios_file\n\n"
markdown+="| Scenario | k6 Config | Server Profiles | Delay Call Depth | Delay (ms) | Connections | Requests per Second | Warmup Duration (s) | Test Duration (s) |\n"
markdown+="|----------|-----------|-----------------|------------------|------------|-------------|---------------------|---------------------|------------------|\n"
while IFS=',' read -r scenario k6Config serverProfiles delayCallDepth delayInMillis connections requestsPerSecond warmupDurationInSeconds testDurationInSeconds; do
    if [[ -z "$scenario" || $scenario == "#"* ]]; then
        continue
    fi
    markdown+="| [$scenario](#$scenario) | $k6Config | $serverProfiles | $delayCallDepth | $delayInMillis | $connections | $requestsPerSecond | $warmupDurationInSeconds | $testDurationInSeconds |\n"
done < <(tail -n +2 "$scenarios_file")

markdown+="\n## Result Overview\n"

markdown+="\n### Overall\n\n"
markdown+="![Overall Results](./results.png)"

markdown+="\n### Netty-based\n\n"
markdown+="![Netty Results](./results-netty.png)\n"

markdown+="\n## Result Details\n\n"
while IFS=',' read -r scenario k6Config serverProfiles delayCallDepth delayInMillis connections requestsPerSecond warmupDurationInSeconds testDurationInSeconds; do
    if [[ -z "$scenario" || $scenario == "#"* ]]; then
        continue
    fi
    markdown+="\n### $scenario\n\n"
    IFS=',' read -ra approach_array <<< "$approaches"
    for approach in "${approach_array[@]}"; do
        image_path="$results_dir/$scenario/$approach.png"
        if [[ -f "$image_path" ]]; then
            markdown+="#### $approach\n\n"
            markdown+="![$approach](./$scenario/$approach.png)\n\n"
        else
            markdown+="#### $approach (No image available)\n\n"
        fi
    done
done < <(tail -n +2 "$scenarios_file")

echo -e "$markdown" > "$results_md_file"
