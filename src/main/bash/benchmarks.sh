#!/bin/bash
# Wrapper over benchmark.sh

DEFAULT_SCENARIOS=("scenarios-deep-call-stack" "scenarios-postgres" "scenarios-sharp-spikes" "scenarios-soaktest")

show_help() {
    cat <<EOF
Usage: $(basename "$0") [OPTION]... [SCENARIO]...
Wrapper over benchmark.sh that supports multiple scenarios and suspends the system on completion.

SCENARIO:                Zero or more space-separated scenario names.
                         Default: ${DEFAULT_SCENARIOS[*]}

OPTION:
  -d, --dry-run          Print what would be done without actually performing it.
  -o, --options "<opts>" Pass additional options to the benchmark.sh script. Run "./benchmark.sh -h" for supported options.
  -h, --help             Show this help message and exit.
EOF
}

DRY_RUN=false
OPTIONS=""
SCENARIOS=()

while [[ $# -gt 0 ]]; do
    case $1 in
        -d|--dry-run)
            DRY_RUN=true
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
            SCENARIOS+=("$1")
            shift
            ;;
    esac
done

if [ ${#SCENARIOS[@]} -eq 0 ]; then
    SCENARIOS=("${DEFAULT_SCENARIOS[@]}")
fi

echo "Scenarios to be executed:"
for scenario in "${SCENARIOS[@]}"; do
    echo "  - $scenario"
done

echo "Options passed to benchmark.sh: $OPTIONS"
echo "Dry run mode: $DRY_RUN"

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
    local scenario=$1
    echo
    echo "Executing $scenario"

    perform_action "Removing 'build/results'"
    if ! $DRY_RUN; then rm -Rf build/results; fi

    perform_action "Executing 'benchmark.sh $OPTIONS $scenario.csv'"
    if ! $DRY_RUN; then ./src/main/bash/benchmark.sh $OPTIONS "$scenario.csv" 2>&1 | tee "results/benchmark-${scenario}.log"; fi

    perform_action "Clearing 'results/$scenario'"
    if ! $DRY_RUN; then rm -Rf "results/$scenario" && mkdir -p "results/$scenario"; fi

    perform_action "Copying 'build/results/*' to 'results/$scenario'"
    if ! $DRY_RUN; then cp -R build/results/* "results/$scenario"; fi

    perform_action "Killing all Java processes"
    if ! $DRY_RUN; then killall java; fi
}

for scenario in "${SCENARIOS[@]}"; do
    process_scenario "$scenario"
done

end_time=$(date +%s)
elapsed_time=$((end_time - start_time))
printf "\nTotal duration: %02dh %02dm %02ds\n" $((elapsed_time / 3600)) $(((elapsed_time % 3600) / 60)) $((elapsed_time % 60))

perform_action "Suspending the system"
if ! $DRY_RUN; then systemctl suspend; fi
