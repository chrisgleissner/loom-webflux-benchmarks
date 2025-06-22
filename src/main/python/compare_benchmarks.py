#!/usr/bin/env python3
# Compares two results.csv files and creates a Markdown file documenting the RPS / latency change.

import argparse
import os
from datetime import datetime

import pandas as pd
import sys

THROUGHPUT_COLUMNS = [
    "requests_per_second_p50",
    "requests_per_second_p90",
    "requests_per_second_max",
]

LATENCY_COLUMNS = [
    "latency_millis_min",
    "latency_millis_p50",
    "latency_millis_p90",
    "latency_millis_p99",
    "latency_millis_max",
]


def load_csv(path: str) -> pd.DataFrame:
    if not os.path.isfile(path):
        print(f"Error: File not found: {path}", file=sys.stderr)
        sys.exit(1)
    try:
        return pd.read_csv(path)
    except Exception as e:
        print(f"Error reading '{path}': {e}", file=sys.stderr)
        sys.exit(1)


def summarize(df: pd.DataFrame, metrics: list[str]) -> pd.DataFrame:
    summaries = []

    for metric in metrics:
        if metric not in df.columns:
            continue
        agg = df.groupby(['scenario', 'approach'])[metric].agg(['mean']).reset_index()
        agg['metric'] = metric
        summaries.append(agg)

    if not summaries:
        raise ValueError("No valid metric columns found for summarization.")

    return pd.concat(summaries, ignore_index=True)


def compute_deltas(before: pd.DataFrame, after: pd.DataFrame) -> pd.DataFrame:
    merged = before.merge(after, on=['scenario', 'approach', 'metric'], suffixes=('_b', '_a'))

    def safe_pct_change(before_val, after_val):
        if before_val == 0:
            return float('nan')
        return ((after_val - before_val) / before_val) * 100

    merged['mean_change_pct'] = merged.apply(
        lambda row: safe_pct_change(row['mean_b'], row['mean_a']),
        axis=1
    )
    merged['mean_change_abs'] = merged['mean_a'] - merged['mean_b']

    return merged[['scenario', 'approach', 'metric', 'mean_change_pct', 'mean_change_abs']]


def classify_change(metric: str, delta_pct: float, threshold: float) -> str:
    if metric in LATENCY_COLUMNS:
        return "✅" if delta_pct < -threshold else "❌" if delta_pct > threshold else "➖"
    elif metric in THROUGHPUT_COLUMNS:
        return "✅" if delta_pct > threshold else "❌" if delta_pct < -threshold else "➖"
    else:
        return "➖"


def format_float(val: float | None, precision=2) -> str:
    if pd.isna(val):
        return "n/a"
    return f"{val:.{precision}f}"


def generate_file_info_table(before_path: str, after_path: str) -> str:
    cwd = os.getcwd()
    before_rel = os.path.relpath(before_path, cwd)
    after_rel = os.path.relpath(after_path, cwd)

    before_mtime = datetime.fromtimestamp(os.path.getmtime(before_path)).strftime("%Y-%m-%d %H:%M:%S")
    after_mtime = datetime.fromtimestamp(os.path.getmtime(after_path)).strftime("%Y-%m-%d %H:%M:%S")

    lines = [
        "| File Role | Relative Path | Last Modified |",
        "|-----------|---------------|---------------|",
        f"| Before    | {before_rel} | {before_mtime} |",
        f"| After     | {after_rel} | {after_mtime} |",
        ""
    ]
    return "\n".join(lines)


def generate_table_section(name: str, metric_list: list[str], delta_df: pd.DataFrame, threshold: float) -> str:
    filtered = delta_df[delta_df['metric'].isin(metric_list)]

    overall_mean = filtered['mean_change_pct'].mean()
    overall_std = filtered['mean_change_pct'].std()

    header = f"## {name} Metrics\n\n"
    summary = f"- Overall Change: **{format_float(overall_mean)}%**\n- Std Dev: {format_float(overall_std)}%\n\n"
    rows = ["| Impact | Scenario | Approach | Metric | Change (%) | Change (abs) |", "|--------|----------|----------|--------|------------|--------------|"]
    for _, row in filtered.iterrows():
        emoji = classify_change(row['metric'], row['mean_change_pct'], threshold)
        metric_short = row['metric'].replace("requests_per_second_", "rps_").replace("latency_millis_", "lat_")
        rows.append(
            f"| {emoji} | {row['scenario']} | {row['approach']} | {metric_short} | {format_float(row['mean_change_pct'])} | {format_float(row['mean_change_abs'])} |"
        )

    return header + summary + "\n".join(rows) + "\n"


def main():
    parser = argparse.ArgumentParser(description="Compare benchmark CSVs and output Markdown report.")
    parser.add_argument("-b", "--before", required=True, help="Path to baseline CSV file")
    parser.add_argument("-a", "--after", required=True, help="Path to comparison CSV file")
    parser.add_argument("-o", "--output", required=True, help="Path to Markdown report output")
    parser.add_argument("--threshold", type=float, default=2.0,
                        help="Percentage threshold for classifying a change as significant (default: 2.0)")
    args = parser.parse_args()

    before = load_csv(args.before)
    after = load_csv(args.after)

    markdown = ["# Benchmark Comparison Report\n", generate_file_info_table(args.before, args.after)]
    for name, columns in [("Throughput", THROUGHPUT_COLUMNS), ("Latency", LATENCY_COLUMNS)]:
        before_summary = summarize(before, columns)
        after_summary = summarize(after, columns)
        deltas = compute_deltas(before_summary, after_summary)
        section = generate_table_section(name, columns, deltas, args.threshold)
        markdown.append(section)

    with open(args.output, 'w') as f:
        f.write("\n".join(markdown))


if __name__ == "__main__":
    main()
