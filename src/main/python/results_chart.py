#!/usr/bin/env python3
# Converts results CSV file to a PNG file.

import argparse
import csv
import math
import matplotlib.pyplot as plt
import sys
from collections import OrderedDict
from dataclasses import dataclass, field
from datetime import datetime
from typing import List, Dict, Tuple

SCENARIO = 'scenario'
APPROACH = 'approach'


@dataclass
class Result:
    value: float
    errors: bool


@dataclass
class Color:
    name: str
    saturation: float
    results: List[Result] = field(default_factory=list)


def format_float(value):
    return f"{value:.0f}" if value % 1 == 0 or value > 100 else (f"{value:.1f}" if value > 10 else f"{value:.2f}")


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
    def __init__(self, csv_file: str, output_file: str, approaches: List[str] = []):
        self.csv_file = csv_file
        self.output_file = output_file
        self.more_is_better_by_metric_name = {
            "cpu": False,
            "garbage": False,
            "heap": False,
            "latency": False,
            "network": False,
            "platform": False,
            "ram": False,
            "requests": True,
        }
        self.colors = ['forestgreen', 'royalblue', 'goldenrod', 'maroon', 'black']

        csv_headers, csv_rows = self.read_csv()
        self.csv_rows = csv_rows
        self.approaches = approaches if approaches else sorted(list(set(row[APPROACH] for row in csv_rows)))
        self.scenarios = list(OrderedDict.fromkeys(row[SCENARIO] for row in csv_rows))
        self.metrics = [key for key in csv_rows[0].keys() if key not in [APPROACH, SCENARIO]]

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

    def get_color_rows(self) -> List[List[Color]]:
        color_rows = []
        for metric in self.metrics:
            color_row = []
            for scenario in self.scenarios:
                result_by_approach = {row[APPROACH]: float(row[metric]) for row in self.csv_rows if row[SCENARIO] == scenario}
                errors_by_approach = {row[APPROACH]: int(row["requests_error"]) > 0 for row in self.csv_rows if row[SCENARIO] == scenario}
                more_is_better = self.more_is_better_by_metric_name.get(metric_prefix(metric), False) if "error" not in metric else False

                # Sort primarily by whether there are errors (False < True), and secondarily by the metric value (reversed if more_is_better)
                ranked_approaches = sorted(
                    [approach for approach in result_by_approach.keys() if approach in self.approaches],
                    key=lambda x: (errors_by_approach[x], -result_by_approach[x] if more_is_better else result_by_approach[x])
                )

                ranked_results = [Result(result_by_approach[approach], errors_by_approach[approach]) for approach in ranked_approaches]

                winning_approach = ranked_approaches[0]
                runner_up_approach = ranked_approaches[min(len(ranked_approaches) - 1, 1)]
                winning_result_delta_perc = calculate_winning_result_delta_perc(result_by_approach[winning_approach], result_by_approach[runner_up_approach])
                saturation = max(0.0, min(1.0, winning_result_delta_perc))
                saturation = round(1 - math.exp(-7 * saturation), 2)  # Skew small differences to make colors easier to distinguish
                color_name = self.color_name_by_approach[winning_approach]
                color_row.append(Color(color_name, saturation, ranked_results))
            color_rows.append(color_row)
        return color_rows

    def render_png(self):
        color_rows = self.get_color_rows()
        num_rows = len(color_rows)
        num_cols = len(color_rows[0]) if color_rows else 0
        fig, ax = plt.subplots(figsize=(12, 14))
        plt.xlim([0, num_cols])
        plt.ylim([0, num_rows])
        plt.gca().invert_yaxis()

        for row, color_row in enumerate(color_rows):
            for col, color in enumerate(color_row):
                ax.add_patch(plt.Rectangle((col, row), 1, 1, color=color.name, alpha=color.saturation))
                for idx, result in enumerate(color.results[:2] if len(color.results) >= 2 else color.results):
                    text_col = col + 0.5
                    text_row = row + 0.35
                    formatted_result = format_float(result.value)
                    font_col = 'black'
                    cell_text = f"{formatted_result}"
                    if result.errors > 0:
                        font_col = 'red'
                    if idx == 0:
                        ax.text(text_col, text_row, cell_text, ha='center', va='center', fontsize='small', weight='bold', color=font_col)
                    else:
                        ax.text(text_col, text_row + 0.40 * idx, cell_text, ha='center', va='center', fontsize='x-small', color=font_col)

        ax.set_xticks([tick + 0.5 for tick in range(num_cols)])
        ax.set_xticklabels(self.scenarios, rotation=20, ha='right', rotation_mode='anchor')
        ax.set_yticks([tick + 0.5 for tick in range(num_rows)])
        ax.set_yticklabels(self.metrics)

        legend_handles = []
        for approach in sorted(self.color_name_by_approach):
            legend_handles.append(plt.Rectangle((0, 0), 1, 1, color=(self.color_name_by_approach[approach]), label=approach))
        ax.legend(handles=legend_handles, loc='center left', bbox_to_anchor=(1.03, 0.5), fontsize='small')

        plt.suptitle('Best Approaches by Metric and Scenario', weight='bold', y=0.94, fontsize='x-large')
        plt.title(
            'Cells show metric value for best approach above runner-up. Color saturation is based on win margin.\n'
            'Values for approaches with request errors are shown in red.',
            y=1.02, size='small')
        plt.savefig(self.output_file, bbox_inches='tight')
        plt.close()
        log("Saved " + self.output_file)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Render PNG from CSV')
    parser.add_argument('-i', '--csvFile', type=str, help='Input CSV file', required=True)
    parser.add_argument('-o', '--pngFile', type=str, help='Output PNG file', required=True)
    parser.add_argument('-a', '--approaches', type=str, help='Comma-separated list of approaches (optional)')
    args = parser.parse_args()

    if not (args.csvFile and args.pngFile):
        print("Error: Both input CSV file and output PNG file are required.")
        sys.exit(1)

    approaches = [] if args.approaches is None else args.approaches.split(',')
    csv_parser = CSVRenderer(args.csvFile, args.pngFile, approaches)
    csv_parser.render_png()
