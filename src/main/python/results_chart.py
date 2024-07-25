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
    approach: str
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
            "sockets": True
        }
        self.colors = ['goldenrod', 'maroon', 'black']

        csv_headers, csv_rows = self.read_csv()
        self.csv_rows = csv_rows
        self.approaches = approaches if approaches else sorted(list(set(row[APPROACH] for row in csv_rows)))
        self.scenarios = list(OrderedDict.fromkeys(row[SCENARIO] for row in csv_rows))
        self.metrics = [key for key in csv_rows[0].keys() if key not in [APPROACH, SCENARIO]]

        self.color_name_by_approach = {}
        predefined_colors = {'loom-netty': 'forestgreen', 'webflux-netty': 'royalblue'}
        other_approaches = [a for a in self.approaches if a not in predefined_colors]
        self.color_name_by_approach.update(predefined_colors)
        self.color_name_by_approach.update({approach: self.colors[i % len(self.colors)] for i, approach in enumerate(other_approaches)})

        self.approach_wins = {approach: 0 for approach in self.approaches}
        self.draw_count = 0
        self.contest_count = 0

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

    def sort_approaches(self, metric: str, scenario: str) -> Tuple[List[str], Dict[str, float], Dict[str, bool]]:
        result_by_approach = {row[APPROACH]: float(row[metric]) for row in self.csv_rows if row[SCENARIO] == scenario}
        errors_by_approach = {row[APPROACH]: int(row.get("requests_error", 0) or 0) > 0 for row in self.csv_rows if row[SCENARIO] == scenario}
        more_is_better = self.more_is_better_by_metric_name.get(metric_prefix(metric), False) if "error" not in metric else False

        # Sort primarily by whether there are errors (False < True), and secondarily by the metric value (reversed if more_is_better)
        ranked_approaches = sorted(
            [approach for approach in result_by_approach.keys() if approach in self.approaches],
            key=lambda x: (errors_by_approach[x], -result_by_approach[x] if more_is_better else result_by_approach[x])
        )
        return ranked_approaches, result_by_approach, errors_by_approach

    def calculate_wins(self):
        for metric in self.metrics:
            for scenario in self.scenarios:
                ranked_approaches, result_by_approach, errors_by_approach = self.sort_approaches(metric, scenario)
                if ranked_approaches:
                    self.contest_count += 1
                    winning_approach = ranked_approaches[0]
                    has_runner_up = len(ranked_approaches) > 1

                    if has_runner_up:
                        runner_up_approach = ranked_approaches[1]
                        top_two_share_result = result_by_approach[winning_approach] == result_by_approach[runner_up_approach]
                        top_two_have_errors = errors_by_approach[winning_approach] and errors_by_approach[runner_up_approach]
                        if top_two_share_result or top_two_have_errors:
                            self.draw_count += 1
                        else:
                            self.approach_wins[winning_approach] += 1
                    else:
                        if errors_by_approach[winning_approach]:
                            self.draw_count += 1
                        else:
                            self.approach_wins[winning_approach] += 1

    def get_color_rows(self) -> List[List[Color]]:
        color_rows = []
        for metric in self.metrics:
            color_row = []
            for scenario in self.scenarios:
                result_by_approach = {row[APPROACH]: float(row[metric]) for row in self.csv_rows if row[SCENARIO] == scenario}
                errors_by_approach = {row[APPROACH]: int(row.get("requests_error", 0)) > 0 for row in self.csv_rows if row[SCENARIO] == scenario}

                ranked_approaches = self.sort_approaches(metric, scenario)[0]  # only need the ranked approaches here
                ranked_results = [Result(approach, result_by_approach[approach], errors_by_approach[approach]) for approach in ranked_approaches]

                winning_approach = ranked_approaches[0]
                runner_up_approach = ranked_approaches[min(len(ranked_approaches) - 1, 1)]
                winning_result_delta_perc = calculate_winning_result_delta_perc(result_by_approach[winning_approach], result_by_approach[runner_up_approach])
                saturation = max(0.0, min(1.0, winning_result_delta_perc))
                saturation = round(1 - math.exp(-7 * saturation), 2)  # Skew small differences to make colors easier to distinguish

                if errors_by_approach[winning_approach] and errors_by_approach[runner_up_approach]:
                    color_name = 'white'
                else:
                    color_name = self.color_name_by_approach[winning_approach]

                color_row.append(Color(color_name, saturation, ranked_results))
            color_rows.append(color_row)
        return color_rows

    def _win_percentages(self, ranked_approaches):
        percentages = {approach: (self.approach_wins[approach] / self.contest_count) * 100 if self.contest_count != 0 else 0 for approach in ranked_approaches}
        rounded_percentages = {approach: round(percentage) for approach, percentage in percentages.items()}

        draw_percentage = (self.draw_count / self.contest_count) * 100 if self.contest_count != 0 else float(0)
        if draw_percentage > 0:
            rounded_percentages["draw"] = round(draw_percentage)

        return rounded_percentages

    def render_png(self):
        self.calculate_wins()
        ranked_approaches = sorted(self.approach_wins.keys(), key=lambda x: -self.approach_wins[x])
        approach_ranks = {approach: rank + 1 for rank, approach in enumerate(ranked_approaches)}

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
                    formatted_result = format_float(result.value)
                    approach = result.approach
                    rank = approach_ranks[approach]
                    formatted_result += f" ({rank})"
                    font_col = 'black'
                    weight = 'bold'
                    text_col = col + 0.5
                    text_row = row + 0.35
                    if result.errors > 0:
                        font_col = 'red'
                        formatted_result += 'E'
                    if idx > 0:
                        weight = 'normal'
                        text_row += 0.4 * idx
                    ax.text(text_col, text_row, formatted_result, ha='center', va='center', fontsize='x-small', weight=weight, color=font_col)

        ax.set_xticks([tick + 0.5 for tick in range(num_cols)])
        ax.set_xticklabels(self.scenarios, rotation=20, ha='right', rotation_mode='anchor')
        ax.set_yticks([tick + 0.5 for tick in range(num_rows)])
        ax.set_yticklabels(self.metrics)

        legend_handles = []
        win_percentages = self._win_percentages(ranked_approaches)

        for index, (approach, rounded_percentage) in enumerate(win_percentages.items()):
            if approach == "draw":
                legend_text = f"{rounded_percentage:.0f}% no winner"
                legend_handles.append(plt.Rectangle((0, 0), 1, 2, facecolor='white', edgecolor='black', linewidth=1, label=legend_text))
            else:
                legend_text = f"({approach_ranks[approach]}) {approach}\n{rounded_percentage:.0f}% wins"
                legend_handles.append(plt.Rectangle((0, 0), 1, 2, color=self.color_name_by_approach[approach], label=legend_text))

            # Add blank legend entry between consecutive entries
            if index < len(win_percentages) - 1:
                legend_handles.append(plt.Rectangle((0, 0), 1, 0.5, color='white', label=''))

        ax.legend(handles=legend_handles, loc='center left', bbox_to_anchor=(1.03, 0.5), fontsize='small')

        plt.suptitle('Best Approaches by Metric and Scenario', weight='bold', y=0.94, fontsize='x-large')
        plt.title(
            'Each cell shows metric value for best approach above runner-up. Color saturation is based on win margin.\n'
            'Approach ranking based on wins is shown in legend and cells. Red values indicate request errors.',
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
