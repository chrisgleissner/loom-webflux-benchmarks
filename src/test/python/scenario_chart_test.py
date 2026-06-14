import csv
import os
import pytest
import shutil
import sys
from datetime import datetime, timezone
from unittest.mock import patch

sys.path.append("src/main/python")
sys.path.append("../../main/python")

import scenario_chart

PROJECT_ROOT_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "../../.."))
TEST_RELATIVE_DIR = "scenario_chart_test_py"
RESOURCES_DIR = PROJECT_ROOT_DIR + "/src/test/resources/" + TEST_RELATIVE_DIR + "/"
TEST_OUTPUT_DIR = PROJECT_ROOT_DIR + "/build/test-output/" + TEST_RELATIVE_DIR + "/"


@pytest.fixture(autouse=True)
def clear_test_output_dir():
    shutil.rmtree(TEST_OUTPUT_DIR, ignore_errors=True)
    os.makedirs(TEST_OUTPUT_DIR, exist_ok=True)


parameterization = [
    ("loom-netty-latency.csv",
     "loom-netty-system.csv",
     "loom-netty-jvm.csv",
     "loom-netty.png",
     "results.csv",
     ),
    ("missing-latencies/webflux-netty-latency.csv",
     "missing-latencies/webflux-netty-system.csv",
     "missing-latencies/webflux-netty-jvm.csv",
     "missing-latencies/webflux-netty.png",
     "missing-latencies/results.csv",
     ),
]


@pytest.mark.parametrize("latency_csv_filename, system_csv_filename, jvm_csv_filename, png_filename, results_csv_filename", parameterization)
def test_main(latency_csv_filename, system_csv_filename, jvm_csv_filename, png_filename, results_csv_filename):
    expected_png_file = TEST_OUTPUT_DIR + png_filename

    results_csv_file = TEST_OUTPUT_DIR + results_csv_filename
    expected_results_csv_file = RESOURCES_DIR + results_csv_filename

    argv = [
        "scenario_chart.py",
        "smoketest",
        "loom-netty",
        RESOURCES_DIR + latency_csv_filename,
        RESOURCES_DIR + system_csv_filename,
        RESOURCES_DIR + jvm_csv_filename,
        expected_png_file,
        results_csv_file
    ]
    with patch('sys.argv', argv):
        scenario_chart.main()

        assert_file_exists(expected_results_csv_file)
        assert_files_match(results_csv_file, expected_results_csv_file)

        assert_file_exists(expected_png_file)
        assert os.path.getsize(expected_png_file) > 0, f"Output file '{expected_png_file}' is empty"


def assert_file_exists(file):
    assert os.path.exists(file), f"File '{file}' does not exist"


def normalize_newlines(content):
    return content.replace(b'\r\n', b'\n').replace(b'\r', b'\n')


def assert_files_match(actual_file, expected_file):
    assert os.path.exists(actual_file), f"File '{actual_file}' does not exist"
    assert os.path.exists(expected_file), f"File '{expected_file}' does not exist"
    with open(actual_file, 'rb') as f, open(expected_file, 'rb') as expected_f:
        actual_content = normalize_newlines(f.read())
        expected_content = normalize_newlines(expected_f.read())
        assert actual_content == expected_content, f"Contents of '{actual_file}' do not match '{expected_file}'"


def test_main_failed_result_mode():
    results_csv_file = TEST_OUTPUT_DIR + "failed/results.csv"
    argv = [
        "scenario_chart.py",
        "--failed",
        "high-load",
        "loom-tomcat",
        results_csv_file,
        "123"
    ]

    with patch('sys.argv', argv):
        scenario_chart.main()

    assert_file_exists(results_csv_file)
    with open(results_csv_file, 'r', encoding='utf-8') as f:
        rows = normalize_newlines(f.read().encode('utf-8')).decode('utf-8').splitlines()

    assert rows[0].startswith("scenario,approach,requests_ok,requests_error,requests_per_second_p50")
    assert rows[1] == (
        "high-load,loom-tomcat,0,123,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0"
    )


# --- P1: GC count/time are the window delta of cumulative counters, never the sum of samples ---

def test_cumulative_window_delta_is_last_minus_first_not_sum():
    # gcCount/gcTime are cumulative JVM counters. The number of events during the window is the
    # difference of the endpoints; summing the per-sample cumulative values is meaningless and scales
    # with the number of samples.
    cumulative = [432, 432, 433, 450, 468]  # 36 GC events occurred during the window
    assert scenario_chart.cumulative_window_delta(cumulative) == 36
    assert scenario_chart.cumulative_window_delta(cumulative) != sum(cumulative)


def test_cumulative_window_delta_zero_when_counter_flat():
    # No GC events during the window -> 0, even though many cumulative samples were recorded.
    assert scenario_chart.cumulative_window_delta([8, 8, 8, 8, 8, 8]) == 0


def test_cumulative_window_delta_handles_empty():
    assert scenario_chart.cumulative_window_delta([]) == 0


def test_gc_columns_in_results_match_jvm_csv_window_delta():
    # End-to-end guard tying results.csv to the JVM CSV endpoints for the missing-latencies fixture
    # (cumulative gcCount 432->468 = 36 events, gcTime 9657->10254 = 597 ms).
    jvm_csv = RESOURCES_DIR + "missing-latencies/webflux-netty-jvm.csv"
    jvm = scenario_chart.JvmMetrics(jvm_csv)
    assert scenario_chart.cumulative_window_delta(jvm.gc_counts) == jvm.gc_counts[-1] - jvm.gc_counts[0] == 36
    assert scenario_chart.cumulative_window_delta(jvm.gc_times) == jvm.gc_times[-1] - jvm.gc_times[0] == 597


# --- P2: GC% divides by the actual sample interval, not a hard-coded 1 second ---

def test_gc_time_percentages_uses_actual_interval():
    # gc_times cumulative; sample 1 added 100 ms of GC over a 2 s interval -> 5%, not 10%.
    seconds_elapsed = [0.0, 2.0]
    gc_times = [1000, 1100]
    assert scenario_chart.gc_time_percentages(seconds_elapsed, gc_times) == [0.0, pytest.approx(5.0)]


def test_gc_time_percentages_one_second_interval():
    seconds_elapsed = [0.0, 1.0, 2.0]
    gc_times = [0, 50, 50]  # 50 ms in a 1 s interval -> 5%, then 0 ms -> 0%
    assert scenario_chart.gc_time_percentages(seconds_elapsed, gc_times) == [0.0, pytest.approx(5.0), pytest.approx(0.0)]


def test_gc_time_percentages_zero_interval_is_safe():
    # Two samples with an identical timestamp must not divide by zero.
    assert scenario_chart.gc_time_percentages([0.0, 0.0], [0, 10]) == [0.0, 0.0]


# --- P3: network-per-request is the aggregate ratio over request-serving seconds ---

def test_per_request_stats_aggregate_ratio_ignores_zero_request_seconds():
    # rate per second = [10, 20, 30], requests = [0, 2, 4]. The first second served no requests and
    # must be excluded (no zero-drag). avg = (20+30)/(2+4) = 8.333..., max = max(20/2, 30/4) = 10.
    avg, maximum = scenario_chart.per_request_stats([10, 20, 30], [0, 2, 4])
    assert avg == pytest.approx(50 / 6)
    assert maximum == pytest.approx(10.0)


def test_per_request_stats_is_not_mean_of_ratios():
    # Mean-of-ratios would be mean(100/1, 100/100) = 50.5; the unbiased aggregate is 200/101.
    avg, _ = scenario_chart.per_request_stats([100, 100], [1, 100])
    assert avg == pytest.approx(200 / 101)
    assert avg != pytest.approx(50.5)


def test_per_request_stats_no_requests_returns_zero():
    assert scenario_chart.per_request_stats([5, 6, 7], [0, 0, 0]) == (0.0, 0.0)


# --- P4: RPS histogram is sized from the maximum timestamp, so no samples are dropped ---

def test_calculate_rps_does_not_drop_samples_for_unsorted_input():
    # Last element (2.0) is smaller than the max (5.5). Using the last element would size the histogram
    # too small and silently drop the 5.5 sample.
    rps = scenario_chart._calculate_rps([0.0, 5.5, 2.0])
    assert sum(rps) == 3, "every request must be counted regardless of row order"


def test_calculate_rps_empty():
    assert list(scenario_chart._calculate_rps([])) == []


# --- P8: latency buckets are emitted per elapsed second, including idle seconds (gap-aligned) ---

def test_latency_percentile_buckets_fill_idle_seconds():
    # Requests only in seconds 0 and 3; seconds 1 and 2 are idle. There must be one bucket PER second
    # (4 total) so the percentile-over-time x-axis stays aligned to wall-clock, with idle seconds = 0.
    seconds_elapsed = [0.0, 0.5, 3.2]
    latencies = [10.0, 20.0, 99.0]
    p50, p90, p99 = scenario_chart.latency_percentile_buckets(seconds_elapsed, latencies)
    assert len(p50) == len(p90) == len(p99) == 4
    assert p50[0] == 15.0          # median of [10, 20]
    assert p50[1] == 0 and p50[2] == 0   # idle seconds are not dropped
    assert p99[3] == 99.0


def test_latency_percentile_buckets_empty():
    assert scenario_chart.latency_percentile_buckets([], []) == ([], [], [])


# --- P7: CPU/RAM/socket aggregates are scoped to the load window, full series kept for plotting ---

def test_system_aggregates_scoped_to_load_window_full_series_retained():
    # The missing-latencies fixture's sar capture (secs ...626..925) is wider than its load window
    # (...628..800). The full series must be retained for plotting, while the aggregates are restricted
    # to the load window so idle pre/post-test seconds do not bias them.
    base = RESOURCES_DIR + "missing-latencies/"
    latency = scenario_chart.LatencyMetrics(base + "webflux-netty-latency.csv")
    system = scenario_chart.SystemMetrics(base + "webflux-netty-system.csv", latency)
    assert len(system.system_times) == 300        # full series retained (not clipped)
    assert len(system.total_cpu) == 300
    full_mean = sum(system.total_cpu) / len(system.total_cpu)
    assert system.cpu_use_avg == pytest.approx(63.33, abs=0.01)   # windowed
    assert system.cpu_use_avg != pytest.approx(full_mean, abs=0.01)  # differs from full-series mean


def test_system_aggregates_equal_full_mean_when_window_covers_all_samples():
    # The loom-netty fixture's sar samples are all inside the load window, so the windowed aggregate
    # equals the full-series mean.
    latency = scenario_chart.LatencyMetrics(RESOURCES_DIR + "loom-netty-latency.csv")
    system = scenario_chart.SystemMetrics(RESOURCES_DIR + "loom-netty-system.csv", latency)
    assert len(system.system_times) == 4
    assert system.cpu_use_avg == pytest.approx(sum(system.total_cpu) / len(system.total_cpu))


# --- Chart x-axis: every panel begins at 0 from its own first real sample (no invented points) ---

def _panel_x_values(ax):
    values = []
    for line in ax.get_lines():
        values.extend(line.get_xdata().tolist())
    for collection in ax.collections:
        offsets = collection.get_offsets()
        if len(offsets):
            values.extend(offsets[:, 0].tolist())
    return values


def test_normalized_to_load_window_drops_preload_and_starts_at_zero():
    # epoch seconds 100..106; load window [103, 106]. Pre-load samples (<103, e.g. JVM startup) are
    # dropped and the first kept sample becomes x=0.
    epoch = [100, 101, 102, 103, 104, 105, 106]
    seconds, (clipped,) = scenario_chart.normalized_to_load_window(epoch, [list(epoch)], 103, 106)
    assert seconds == [0, 1, 2, 3]
    assert clipped == [103, 104, 105, 106]


def test_normalized_to_load_window_drops_trailing_samples():
    # sar is captured a few seconds past the load; trailing samples (>103) must be dropped so the
    # system panels do not extend into post-load idle.
    epoch = [100, 101, 102, 103, 104, 105, 106]
    seconds, (clipped,) = scenario_chart.normalized_to_load_window(epoch, [list(epoch)], 100, 103)
    assert seconds == [0, 1, 2, 3]
    assert clipped == [100, 101, 102, 103]


def test_normalized_to_load_window_falls_back_when_no_overlap():
    seconds, (clipped,) = scenario_chart.normalized_to_load_window([100, 101, 102], [[7, 8, 9]], 500, 600)
    assert seconds == [0, 1, 2]
    assert clipped == [7, 8, 9]


def _write_csv(path, header, rows):
    with open(path, 'w', newline='') as f:
        if header:
            f.write(header + "\n")
        csv.writer(f, delimiter=';' if ';' in (header or '') else ',').writerows(rows)


def test_system_panel_clipped_to_load_window_with_trailing_sar():
    # sar now runs a few seconds past k6; the extra trailing samples must NOT extend the system panels
    # past the load window (they would otherwise trail into post-load idle).
    import matplotlib.pyplot as plt
    lat_csv = TEST_OUTPUT_DIR + "lat.csv"
    sys_csv = TEST_OUTPUT_DIR + "sys.csv"
    jvm_csv = TEST_OUTPUT_DIR + "jvm.csv"
    # Requests over absolute seconds 1000.1..1003.9 -> load window [1000, 1004].
    _write_csv(lat_csv, "", [[int((1000.1 + 3.8 * i / 39) * 1000), 5, 200, "", ""] for i in range(40)])
    # sar samples 1001..1010: 1005..1010 are trailing (past the load window).
    sys_header = "timestamp;%user;%system;%iowait;%memused;rxpck/s;txpck/s;rxkB/s;txkB/s;tcpsck;active/s;passive/s;iseg/s;oseg/s"
    _write_csv(sys_csv, sys_header, [[s, 40, 5, 0, 30, 100, 100, 10, 10, 50, 1, 1, 100, 100] for s in range(1001, 1011)])
    _write_csv(jvm_csv, "epochMillis,memUsed,memCommitted,memMax,gcCount,gcTime,platformThreadCount",
               [[s * 1000, 500000000, 2147483648, 2147483648, 10, 100, 25] for s in range(1000, 1006)])

    latency = scenario_chart.LatencyMetrics(lat_csv)
    system = scenario_chart.SystemMetrics(sys_csv, latency)
    jvm = scenario_chart.JvmMetrics(jvm_csv)
    out = TEST_OUTPUT_DIR + "clip.png"
    scenario_chart.create_plot_png_file("t", latency, system, jvm, out)
    fig = plt.gcf()
    try:
        socket_panel = fig.axes[4]  # latency, rps, cpu, ram, socket, throughput
        xs = _panel_x_values(socket_panel)
        assert min(xs) <= 0.5, "system panel does not start at ~0"
        # Load window spans ~4 s; without clipping the trailing sar samples it would reach ~9 s.
        assert max(xs) <= 4.5, f"system panel trails into post-load idle (max x = {max(xs):.2f})"
    finally:
        plt.close(fig)


def test_all_chart_panels_start_at_zero():
    # The user's requirement: every panel (latency, RPS, CPU, heap, sockets, throughput) begins at x=0
    # drawn from its own first REAL sample -- the sar panels start at 0 just like the JVM heap, with no
    # invented data points.
    import matplotlib.pyplot as plt
    latency = scenario_chart.LatencyMetrics(RESOURCES_DIR + "loom-netty-latency.csv")
    system = scenario_chart.SystemMetrics(RESOURCES_DIR + "loom-netty-system.csv", latency)
    jvm = scenario_chart.JvmMetrics(RESOURCES_DIR + "loom-netty-jvm.csv")
    out = TEST_OUTPUT_DIR + "alignment.png"
    scenario_chart.create_plot_png_file("t", latency, system, jvm, out)
    fig = plt.gcf()
    try:
        for ax in fig.axes:
            assert ax.get_xlim()[0] == 0, "x-axis is not pinned to 0"
            xs = _panel_x_values(ax)
            assert xs, "panel has no plotted data"
            assert min(xs) <= 0.5, f"panel starts at {min(xs):.2f}s rather than ~0"
    finally:
        plt.close(fig)
