#!/usr/bin/env python3
# Converts results.csv to a PNG file.

import argparse
import csv
import math
import matplotlib.pyplot as plt
import sys
from dataclasses import dataclass
from datetime import datetime
from typing import List, Dict, Tuple

TIME = 'time_epoch_millis'
SCENARIO = 'scenario'
APPROACH = 'approach'


@dataclass
class Color:
    name: str
    saturation: float


def log(msg):
    print(datetime.now().strftime("%H:%M:%S") + " " + msg)


def metric_prefix(s: str) -> str:
    return s.split('_', 1)[0].lower()


def calculate_winning_result_delta_perc(winning_result, runner_up_result):
    winning_result_delta = abs(winning_result - runner_up_result)
    if winning_result_delta == 0 and runner_up_result == 0:
        winning_result_delta_perc = 0
    else:
        if runner_up_result != 0:
            winning_result_delta_perc = winning_result_delta / runner_up_result
        else:
            winning_result_delta_perc = float('inf')
    return winning_result_delta_perc


class CSVRenderer:
    def __init__(self, csv_file: str, output_file: str):
        self.csv_file = csv_file
        self.output_file = output_file
        self.more_is_better_by_metric_name = {
            "cpu": False,
            "errors": False,
            "gc": False,
            "heap": False,
            "latency": False,
            "platform": False,
            "ram": False,
            "rps": True,
            "tcp": False
        }
        self.colors = ['g', 'b', 'c', 'r', 'm', 'y', 'orange', 'purple', 'brown', 'pink']

        csv_headers, csv_rows = self.read_csv()
        self.csv_rows = csv_rows
        self.approaches = sorted(list(set(row[APPROACH] for row in csv_rows)))
        self.scenarios = sorted(list(set(row[SCENARIO] for row in csv_rows)))
        self.metrics = [key for key in csv_rows[0].keys() if key not in [TIME, APPROACH, SCENARIO]]

        self.color_name_by_approach = {}
        for index, approach in enumerate(self.approaches):
            self.color_name_by_approach[approach] = self.colors[index % len(self.colors)]

    def read_csv(self) -> Tuple[List[str], List[Dict[str, str]]]:
        try:
            with open(self.csv_file, 'r') as file:
                reader = csv.DictReader(file)
                headers = reader.fieldnames
                rows = list(reader)
            if not rows:
                raise ValueError("CSV file is empty")
            return headers, rows
        except FileNotFoundError:
            print(f"Error: CSV file '{self.csv_file}' not found.")
            sys.exit(1)
        except ValueError as ve:
            print(f"Error: {ve}")
            sys.exit(1)

    def get_winning_approach(self, approaches: List[str], scenario: Dict[str, str]) -> str:
        best_approach = None
        best_grade = -math.inf if self.more_is_better_by_metric_name else math.inf
        for approach in approaches:
            grade = float(scenario[approach])
            if (self.more_is_better_by_metric_name.get(approach, True) and grade > best_grade) or \
                    (not self.more_is_better_by_metric_name.get(approach, True) and grade < best_grade):
                best_approach = approach
                best_grade = grade
        return best_approach

    def get_color_rows(self) -> List[List[Color]]:
        color_rows = []
        for metric in self.metrics:
            color_row = []
            for scenario in self.scenarios:
                result_by_approach = {row[APPROACH]: float(row[metric]) for row in self.csv_rows if row[SCENARIO] == scenario}
                more_is_better = self.more_is_better_by_metric_name.get(metric_prefix(metric), True)

                ranked_approaches = sorted(result_by_approach.keys(), key=lambda x: result_by_approach[x], reverse=more_is_better)
                winning_approach = ranked_approaches[0]
                runner_up_approach = ranked_approaches[1]
                winning_result_delta_perc = calculate_winning_result_delta_perc(result_by_approach[winning_approach], result_by_approach[runner_up_approach])
                saturation = max(0.0, min(1.0, winning_result_delta_perc))
                saturation = round(1 - math.exp(-7 * saturation), 2)  # Skew small differences to make colors easier to distinguish

                color_name = self.color_name_by_approach[winning_approach]
                color_row.append(Color(color_name, saturation))

            color_rows.append(color_row)

        return color_rows

    def render_png(self):
        color_rows = self.get_color_rows()
        num_rows = len(color_rows)
        num_cols = len(color_rows[0]) if color_rows else 0

        fig, ax = plt.subplots(figsize=(10, 10))
        plt.xlim([0, num_cols])
        plt.ylim([0, num_rows])
        plt.gca().invert_yaxis()

        for row, color_row in enumerate(color_rows):
            for col, color in enumerate(color_row):
                # log("Set color for row {row} and col {col} to {name} / {sat}".format(row=row, col=col, name=color.name, sat=color.saturation))
                ax.add_patch(plt.Rectangle((col, row), 1, 1, color=color.name, alpha=color.saturation))

        ax.set_xticks(range(num_cols))
        ax.set_xticklabels(self.scenarios, rotation=20, ha='right', rotation_mode='anchor')

        ax.set_yticks(range(num_rows))
        ax.set_yticklabels(self.metrics)

        legend_handles = []
        for approach in sorted(self.color_name_by_approach):
            legend_handles.append(plt.Rectangle((0, 0), 1, 1, color=(self.color_name_by_approach[approach]), label=approach))
        ax.legend(handles=legend_handles, loc='center left', bbox_to_anchor=(1.05, 0.5), fontsize='small')

        ax.set_title('Best Approaches by Metric and Scenario', weight='bold')

        plt.savefig(self.output_file, bbox_inches='tight')
        plt.close()
        log("Saved " + self.output_file)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Render PNG from CSV')
    parser.add_argument('-i', '--csvFile', type=str, help='Input CSV file', required=True)  # Mark as required
    parser.add_argument('-o', '--pngFile', type=str, help='Output PNG file', required=True)  # Mark as required
    args = parser.parse_args()

    if not (args.csvFile and args.pngFile):
        print("Error: Both input CSV file and output PNG file are required.")
        sys.exit(1)

    csv_parser = CSVRenderer(args.csvFile, args.pngFile)
    csv_parser.render_png()
