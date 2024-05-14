#!/bin/bash
# Generate charts based on previously captured CSV files.

resultsDir=../../../build/results
approaches=loom-tomcat,loom-netty,webflux-netty
resultsCsvFile="$resultsDir"/results.csv

echo "Removing PNG files and results.csv"
find "$resultsDir" -type f -name "*.png" -exec rm -f {} +
rm -f "$resultsDir"/results.*

echo "Generating PNG files and results.csv"
for dir in "$resultsDir"/*; do
  if [ -d "$dir" ]; then
    scenario=$(basename "$dir")
    IFS=',' read -ra approach_array <<< "$approaches"
    for approach in "${approach_array[@]}"; do
      latencyCsvFile="$dir/$approach-latency.csv"
      systemCsvFile="$dir/$approach-system.csv"
      jvmCsvFile="$dir/$approach-jvm.csv"
      outputPngFile="$dir/$approach.png"

      echo
      echo "Generating scenario PNG files: dir=$dir, scenario=$scenario, approach=$approach"
      echo "CLI args: $scenario $approach $latencyCsvFile $systemCsvFile $jvmCsvFile $outputPngFile $resultsCsvFile"
      ../python/scenario_chart.py "$scenario" "$approach" "$latencyCsvFile" "$systemCsvFile" "$jvmCsvFile" "$outputPngFile" "$resultsCsvFile"
    done
  fi
done

echo "Generating results PNG files"
../python/results_chart.py -i "$resultsCsvFile" -o "$resultsDir"/results.png
../python/results_chart.py -i "$resultsCsvFile" -o "$resultsDir"/results-netty.png -a loom-netty,webflux-netty
