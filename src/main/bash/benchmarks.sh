#!/bin/bash
# Wrapper over benchmark.sh

DEFAULT_SCENARIOS=("scenarios-default.csv" "scenarios-deep-call-stack.csv" "scenarios-postgres.csv" "scenarios-sharp-spikes.csv" "scenarios-soaktest.csv")

show_help() {
    cat <<EOF
Usage: $(basename "$0") [OPTION]... [SCENARIO_FILE]...
Wrapper over benchmark.sh that supports multiple scenario files and optionally suspends the system on completion.

SCENARIO_FILE:           Zero or more space-separated scenario configuration CSV files in src/main/resources/scenarios/.
                         Default: ${DEFAULT_SCENARIOS[*]}

OPTION:
  -d, --dry-run          Print what would be done without actually performing it.
  -k, --kill-java        Kill all Java processes after each benchmark. Default: false
  -o, --options "<opts>" Pass additional options to the benchmark.sh script. Run "./benchmark.sh -h" for supported options.
  -s, --suspend          Suspend the system upon completion of the script. Default: false
  -h, --help             Show this help message and exit.
EOF
}

DRY_RUN=false
SUSPEND=false
KILL_JAVA=false
OPTIONS=""
SCENARIO_FILES=()

while [[ $# -gt 0 ]]; do
    case $1 in
        -d|--dry-run)
            DRY_RUN=true
            shift
            ;;
        -s|--suspend)
            SUSPEND=true
            shift
            ;;
        -k|--kill-java)
            KILL_JAVA=true
            shift
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        -o|--options)
            OPTIONS="$2"
            shift 2
            ;;
        *)
            SCENARIO_FILES+=("$1")
            shift
            ;;
    esac
done

if [ ${#SCENARIO_FILES[@]} -eq 0 ]; then
    SCENARIO_FILES=("${DEFAULT_SCENARIOS[@]}")
fi

echo "Scenario files:"
for scenario_file in "${SCENARIO_FILES[@]}"; do
    echo "  - $scenario_file"
done

echo "Options passed to benchmark.sh: $OPTIONS"
echo "Dry run mode: $DRY_RUN"
echo "Suspend system after completion: $SUSPEND"
echo "Kill Java processes after benchmarks: $KILL_JAVA"

start_time=$(date +%s)

perform_action() {
    local message=$1
    if $DRY_RUN; then
        echo "Dry run mode enabled: $message"
    else
        echo "$message"
    fi
}

process_scenario() {
    local scenario_file="$1"
    local scenario
    scenario=$(basename "$scenario_file" .csv)

    echo
    echo "Executing benchmark using $scenario_file"

    perform_action "Removing 'build/results'"
    if ! $DRY_RUN; then rm -Rf build/results; fi

    perform_action "Executing 'benchmark.sh $OPTIONS $scenario_file'"
    if ! $DRY_RUN; then ./src/main/bash/benchmark.sh $OPTIONS "$scenario_file" 2>&1 | tee "results/benchmark-${scenario}.log"; fi

    perform_action "Clearing 'results/$scenario'"
    if ! $DRY_RUN; then rm -Rf "results/$scenario" && mkdir -p "results/$scenario"; fi

    perform_action "Copying 'build/results/*' to 'results/$scenario'"
    if ! $DRY_RUN; then cp -R build/results/* "results/$scenario"; fi

    if $KILL_JAVA; then
        perform_action "Killing all Java processes"
        if ! $DRY_RUN; then killall java; fi
    fi
}

for scenario_file in "${SCENARIO_FILES[@]}"; do
    process_scenario "$scenario_file"
done

end_time=$(date +%s)
elapsed_time=$((end_time - start_time))
printf "\nTotal duration: %02dh %02dm %02ds\n" $((elapsed_time / 3600)) $(((elapsed_time % 3600) / 60)) $((elapsed_time % 60))

if $SUSPEND; then
    perform_action "Suspending the system"
    if ! $DRY_RUN; then systemctl suspend; fi
fi
