#!/usr/bin/env python3
# Converts latency, JVM metric, and system metric CSV files to a PNG file.
# Appends high-level results to results.csv for later conversion to a PNG file by results_chart.py.

import csv
import matplotlib.pyplot as plt
import matplotlib.ticker as mtick
import numpy as np
import os
import sys
import time
from collections import Counter
from datetime import datetime, timezone
from matplotlib.ticker import LogLocator
from matplotlib.ticker import ScalarFormatter


def log(msg):
    print(datetime.now().strftime("%H:%M:%S") + " " + msg)


def is_ok(status_code):
    return int(status_code / 100) == 2


def _seconds_elapsed(times):
    min_time = np.array(times).min()
    return [t - min_time for t in times]


def _calculate_rps(seconds_elapsed):
    # Size the histogram from the maximum elapsed time, not the last list element: k6 writes its CSV
    # in request-completion order with concurrent VUs, so the rows are only approximately time-sorted
    # and the final row is not guaranteed to hold the latest timestamp. np.histogram silently drops
    # samples outside `range`, so using seconds_elapsed[-1] would discard any request whose timestamp
    # exceeds the last row's, under-counting RPS near the tail.
    if not seconds_elapsed:
        return []
    seconds_bin = int(np.ceil(max(seconds_elapsed))) + 1
    return np.histogram(seconds_elapsed, bins=seconds_bin, range=(0, seconds_bin))[0]


def cumulative_window_delta(samples):
    # gcCount/gcTime are cumulative JVM counters (monotonically non-decreasing since JVM start), so the
    # amount that accrued during the measured window is the difference between the last and first
    # samples -- NOT the sum of the per-sample cumulative values. The test-phase JVM CSV is written
    # fresh (the warmup file is moved away first), so this delta excludes startup/warmup activity.
    return samples[-1] - samples[0] if samples else 0


def gc_time_percentages(seconds_elapsed, gc_times):
    # Fraction of wall-clock spent in GC per sample, as a percentage. Divides the cumulative GC-time
    # delta by the ACTUAL elapsed time between consecutive samples rather than a hard-coded 1 s: the
    # sampler runs at a nominal 1 s fixedRate but drifts above 1 s under load (the very regime under
    # study), which would otherwise overstate GC%.
    percentages = [0.0]
    for i in range(1, len(gc_times)):
        interval_seconds = seconds_elapsed[i] - seconds_elapsed[i - 1]
        gc_millis = gc_times[i] - gc_times[i - 1]
        percentages.append((gc_millis / (interval_seconds * 1000)) * 100 if interval_seconds > 0 else 0.0)
    return percentages


def per_request_stats(rate_per_second, requests_per_second):
    # Aggregate "<resource> per request" over a window as the unbiased ratio of totals
    # (sum(rate) / sum(requests)), restricted to seconds that actually served requests. This avoids
    # two defects of averaging per-second ratios: (1) mean-of-ratios over-weights low-traffic seconds,
    # and (2) seconds with zero requests would otherwise contribute a 0 that drags the mean down. The
    # max is the busiest per-request second among seconds with requests.
    pairs = [(rate, requests) for rate, requests in zip(rate_per_second, requests_per_second) if requests > 0]
    if not pairs:
        return 0.0, 0.0
    total_rate = sum(rate for rate, _ in pairs)
    total_requests = sum(requests for _, requests in pairs)
    avg = total_rate / total_requests
    maximum = max(rate / requests for rate, requests in pairs)
    return avg, maximum


def _avg_max(values):
    if not values:
        return 0.0, 0.0
    return sum(values) / len(values), max(values)


def latency_percentile_buckets(seconds_elapsed, latencies):
    # One latency bucket per ELAPSED SECOND from 0..floor(max), including seconds that served no
    # requests. Bucketing by floor(elapsed second) keeps the percentile-over-time series aligned to
    # wall-clock time even when load is sparse or stalls -- the previous flush-on-threshold logic
    # advanced the x-axis by one per non-empty second only, compressing the curve across idle gaps.
    # Returns (p50, p90, p99) lists, one value per second.
    if not seconds_elapsed:
        return [], [], []
    bucket_count = int(np.floor(max(seconds_elapsed))) + 1
    latencies_by_second = [[] for _ in range(bucket_count)]
    for elapsed, latency in zip(seconds_elapsed, latencies):
        latencies_by_second[int(elapsed)].append(latency)

    def percentile_series(percentile):
        return [np.percentile(bucket, percentile) if bucket else 0 for bucket in latencies_by_second]

    return percentile_series(50), percentile_series(90), percentile_series(99)


class LatencyMetrics:

    def __init__(self, filename):
        start_time = time.time()
        self.times = []
        self.times_requested = []
        self.times_ok = []

        self.latencies = []
        self.latencies_requested = []
        self.latencies_ok = []
        self.latencies_error = []

        self.status_codes = []

        self.rps = []
        self.rps_requested = []
        self.rps_error = []

        self.latency_1s_buckets_p50 = []
        self.latency_1s_buckets_p90 = []
        self.latency_1s_buckets_p99 = []

        self._parse_csv(filename)

        self.seconds_elapsed = _seconds_elapsed(self.times)
        self.seconds_elapsed_requested = _seconds_elapsed(self.times_requested)
        self.seconds_elapsed_ok = _seconds_elapsed(self.times_ok)
        self.seconds_elapsed_error = []

        self._calculate_latency_1s_buckets_and_errors()
        self._calculate_all_rps()

        # Requests keyed by absolute wall-clock second, so system (sar) metrics can be aligned to the
        # request rate of the SAME second by timestamp instead of by list index (see SystemMetrics).
        self.requests_by_second = Counter(int(t) for t in self.times)

        self.percentile_time_buckets = np.arange(len(self.latency_1s_buckets_p50))
        log("Read latency metrics in " + str(int((time.time() - start_time) * 1000)) + "ms")

    def _parse_csv(self, filename):
        with open(filename, 'r') as file:
            reader = csv.reader(file)

            for row in reader:
                time = int(row[0]) / 1000
                self.times.append(time)

                latency = float(row[1])
                self.latencies.append(latency)

                status_code = int(row[2])
                self.status_codes.append(status_code)

                # The latency/throughput distribution deliberately excludes failed requests that
                # returned in under 10 ms, as these were most likely never submitted to the server
                # (e.g. a client-side connection refusal) and would otherwise pull the percentiles
                # down. NOTE: this filter is intentionally NOT applied to the error count/scatter
                # below, which counts ALL non-2xx responses so a server that has fallen over and is
                # refusing connections (status 0, ~0 ms) is surfaced rather than hidden. As a result
                # requests_ok + requests_error need not equal the number of "requested" samples.
                if is_ok(status_code) or latency >= 10:
                    self.times_requested.append(time)
                    self.latencies_requested.append(latency)

                if is_ok(status_code):
                    self.times_ok.append(time)
                    self.latencies_ok.append(latency)

    def _calculate_latency_1s_buckets_and_errors(self):
        for seconds_elapsed, status_code, latency in zip(self.seconds_elapsed, self.status_codes, self.latencies):
            if not is_ok(status_code):
                self.seconds_elapsed_error.append(seconds_elapsed)
                self.latencies_error.append(latency + 0.1)  # Ensure latency 0 is plotted

        self.latency_1s_buckets_p50, self.latency_1s_buckets_p90, self.latency_1s_buckets_p99 = \
            latency_percentile_buckets(self.seconds_elapsed, self.latencies)

    def _calculate_all_rps(self):
        self.rps = _calculate_rps(self.seconds_elapsed)
        self.rps_requested = _calculate_rps(self.seconds_elapsed_requested)
        self.rps_error = _calculate_rps(self.seconds_elapsed_error)


class SystemMetrics:

    def __init__(self, filename, latency_metrics):
        self.system_times = []

        # CPU
        self.user_cpu = []
        self.system_cpu = []
        self.iowait_cpu = []

        # Mem
        self.mem_used = []

        # Sockets
        self.tcpsck = []
        self.active_s = []
        self.passive_s = []

        # Network Transfer
        self.iseg_s = []
        self.oseg_s = []
        self.rxpck_s = []
        self.txpck_s = []
        self.rxkb_s = []
        self.txkb_s = []

        self._parse_csv(filename)

        # Absolute epoch seconds per sample; charts plot every series against a shared origin (see
        # create_plot_png_file) so all panels share one wall-clock x-axis. The full series is always
        # retained for plotting; only the scalar aggregates below are restricted to the load window.
        self.epoch_seconds = [timestamp.timestamp() for timestamp in self.system_times]
        self.total_cpu = [user + system + iowait for user, system, iowait in zip(self.user_cpu, self.system_cpu, self.iowait_cpu)]
        self.seconds_elapsed = [t - self.epoch_seconds[0] for t in self.epoch_seconds]

        # Align each sar sample to the request count of the SAME absolute wall-clock second. sar and the
        # k6 latencies have different time origins, so pairing them by list index introduced a
        # systematic offset; aligning by timestamp removes it. per_request_stats only counts seconds
        # that served requests, so these network ratios are inherently restricted to the load window.
        requests_per_second = [latency_metrics.requests_by_second.get(int(t), 0) for t in self.epoch_seconds]

        self.total_kb_s = [rxkb_s + txkb_s for rxkb_s, txkb_s in zip(self.rxkb_s, self.txkb_s)]
        self.kb_per_request_avg, self.kb_per_request_max = per_request_stats(self.total_kb_s, requests_per_second)

        self.total_pck_s = [rxpck_s + txpck_s for rxpck_s, txpck_s in zip(self.rxpck_s, self.txpck_s)]
        self.pck_per_request_avg, self.pck_per_request_max = per_request_stats(self.total_pck_s, requests_per_second)

        # CPU/RAM/socket aggregates are taken over the load window only (sar may sample a little before
        # the first and after the last request); see P7 in docs/benchmark-code-review.md.
        window = self._load_window_indices(latency_metrics)
        self.cpu_use_avg, self.cpu_use_max = _avg_max([self.total_cpu[i] for i in window])
        self.ram_use_avg, self.ram_use_max = _avg_max([self.mem_used[i] for i in window])
        self.sockets_avg, self.sockets_max = _avg_max([self.tcpsck[i] for i in window])

    def _parse_csv(self, filename):
        with open(filename, 'r') as file:
            csv_reader = csv.DictReader(file, delimiter=';')
            # CSV cols: timestamp;%user;%system;%iowait;%memused;rxpck/s;txpck/s;rxkB/s;txkB/s;tcpsck;active/s;passive/s;iseg/s;oseg/s
            for row in csv_reader:
                self.system_times.append(datetime.fromtimestamp(int(row['timestamp']), tz=timezone.utc))
                self.user_cpu.append(float(row['%user']))
                self.system_cpu.append(float(row['%system']))
                self.iowait_cpu.append(float(row['%iowait']))
                self.mem_used.append(float(row['%memused']))
                self.tcpsck.append(float(row['tcpsck']))
                self.active_s.append(float(row['active/s']))
                self.passive_s.append(float(row['passive/s']))
                self.iseg_s.append(float(row['iseg/s']))
                self.oseg_s.append(float(row['oseg/s']))
                self.rxpck_s.append(float(row['rxpck/s']))
                self.txpck_s.append(float(row['txpck/s']))
                self.rxkb_s.append(float(row['rxkB/s']))
                self.txkb_s.append(float(row['txkB/s']))

    def _load_window_indices(self, latency_metrics):
        # Indices of the sar samples that fall within the wall-clock window during which load was
        # actually applied, i.e. [first request second, last request second] from the k6 latencies.
        # Used to scope the CPU/RAM/socket aggregates so sar samples taken just before the first or
        # after the last request do not bias them. Falls back to all samples if the window is unknown
        # or non-overlapping (a degenerate run). The full series is left intact for plotting.
        all_indices = list(range(len(self.system_times)))
        if not latency_metrics.requests_by_second:
            return all_indices
        load_start = min(latency_metrics.requests_by_second)
        load_end = max(latency_metrics.requests_by_second)
        window = [i for i in all_indices if load_start <= int(self.epoch_seconds[i]) <= load_end]
        return window if window else all_indices


class JvmMetrics:

    def __init__(self, filename):
        self.jvm_times = []
        self.heap_used = []
        self.gc_counts = []
        self.gc_times = []
        self.platform_thread_count = []

        self._parse_csv(filename)

        self.epoch_seconds = [timestamp.timestamp() for timestamp in self.jvm_times]
        self.seconds_elapsed = [t - self.epoch_seconds[0] for t in self.epoch_seconds]
        self.gc_times_percentage = gc_time_percentages(self.seconds_elapsed, self.gc_times)

    def _parse_csv(self, filename):
        with open(filename, 'r') as file:
            csv_reader = csv.DictReader(file, delimiter=',')
            for row in csv_reader:
                self.jvm_times.append(datetime.fromtimestamp(int(row['epochMillis']) / 1000, tz=timezone.utc))
                self.heap_used.append(100.0 * float(row['memUsed']) / float(row['memMax']))
                self.gc_counts.append(int(row['gcCount']))
                self.gc_times.append(int(row['gcTime']))
                self.platform_thread_count.append(int(row['platformThreadCount']))


def _load_window(latency_metrics):
    # The wall-clock window (epoch seconds) over which k6 actually issued requests, widened to whole
    # seconds so a JVM/sar sample taken in the same second as the first/last request is included.
    if not latency_metrics.times:
        return float('-inf'), float('inf')
    return int(min(latency_metrics.times)), int(max(latency_metrics.times)) + 1


def normalized_to_load_window(epoch_seconds, series_list, load_start, load_end):
    # Keep only samples within the load window and express their times relative to the FIRST kept
    # sample, so the returned series begins at x=0. This lets each panel be drawn from its own first
    # real sample (no invented data points) -- the JVM logger, sar and k6 each start at a slightly
    # different wall-clock instant, but every panel begins at 0 -- while dropping JVM samples logged
    # before load (application startup on runs without a warmup), which would otherwise stretch the JVM
    # panels past the rest. Falls back to all samples if none fall in the window.
    kept = [i for i, second in enumerate(epoch_seconds) if load_start <= second <= load_end]
    if not kept:
        kept = list(range(len(epoch_seconds)))
    if not kept:
        return [], [[] for _ in series_list]
    base = epoch_seconds[kept[0]]
    seconds = [epoch_seconds[i] - base for i in kept]
    return seconds, [[series[i] for i in kept] for series in series_list]


def create_plot_png_file(title, latency_metrics, system_metrics, jvm_metrics, output_png_file):
    fig, (latency, rps, cpu, ram, socket, throughput) = plt.subplots(6, 1, figsize=(25, 20), sharex=True)
    fig.suptitle(title, fontsize=18)
    fig.subplots_adjust(top=0.93)
    plots = (latency, rps, cpu, ram, socket, throughput)

    _add_request_plots(latency_metrics, latency, rps)
    _add_system_plots(system_metrics, jvm_metrics, cpu, ram, socket, throughput, latency_metrics)

    for plot in plots:
        plot.xaxis.set_major_formatter(ScalarFormatter(useMathText=False))
        plot.grid(True, which='both', linestyle=':', linewidth=0.5)
        plot.legend(loc='upper left')
        plot.set_xlim(left=0)  # every panel begins at 0 (shared x-axis via sharex)

    for plot in (latency, rps, socket, throughput):
        plot.yaxis.set_major_formatter(ScalarFormatter(useMathText=False))
        plot.yaxis.set_minor_formatter(ScalarFormatter(useMathText=False))
        plot.ticklabel_format(style='plain')

    for plot in (cpu, ram):
        plot.yaxis.set_major_formatter(mtick.PercentFormatter(100.0))

    plt.savefig(output_png_file, format='png', bbox_inches='tight')


def _add_request_plots(latency_metrics, latency_plot, rps_plot):
    # Every series is measured from the first request (latency_metrics.seconds_elapsed starts at 0), so
    # the request panels begin at x=0; see create_plot_png_file for why all panels are normalised to
    # their own first sample rather than a shared absolute origin.
    latency_plot.scatter(latency_metrics.seconds_elapsed, latency_metrics.latencies,
                         label=legend_label('Latency', latency_metrics.latencies_requested, 'ms'), color='silver', alpha=0.7, s=1)

    if len(latency_metrics.latencies_error) > 0:
        latency_plot.scatter(latency_metrics.seconds_elapsed_error, latency_metrics.latencies_error,
                             label='Errors: ' + str(len(latency_metrics.latencies_error)), color='red')

    latency_plot.plot(latency_metrics.percentile_time_buckets, latency_metrics.latency_1s_buckets_p99,
                      label='p99: {:,.0f}ms'.format(np.percentile(latency_metrics.latencies, 99)), color='blue', linewidth=1)
    latency_plot.plot(latency_metrics.percentile_time_buckets, latency_metrics.latency_1s_buckets_p90,
                      label='p90: {:,.0f}ms'.format(np.percentile(latency_metrics.latencies, 90)), color='green', linewidth=1)
    latency_plot.plot(latency_metrics.percentile_time_buckets, latency_metrics.latency_1s_buckets_p50,
                      label='p50: {:,.0f}ms'.format(np.percentile(latency_metrics.latencies, 50)), color='black', linewidth=2)

    latency_plot.set_ylabel('Latency (ms)', color='black')
    latency_plot.set_title(title_label('Latency', latency_metrics.latencies_requested, 'ms'), weight='bold')
    latency_plot.set_yscale('log')
    latency_plot.yaxis.set_minor_locator(LogLocator(subs=(1.0, 3.0)))

    rps_plot.plot(list(range(len(latency_metrics.rps))), latency_metrics.rps, label=legend_label('RPS', latency_metrics.rps), color='black', linewidth=2)
    rps_plot.set_ylabel('Requests per second', color='black')
    title_postfix = '. Total requests: {:,.0f} ok, {:,.0f} failed.'.format(len(latency_metrics.latencies_ok), len(latency_metrics.latencies_error))
    rps_plot.set_title(title_label(prefix='RPS', measurements=latency_metrics.rps_requested, unit='', postfix=title_postfix), weight='bold')

    if len(latency_metrics.latencies_error) > 0:
        rps_plot.plot(list(range(len(latency_metrics.rps_error))), latency_metrics.rps_error,
                      label=legend_label('RPS (Failed)', latency_metrics.rps_error), color='orange', linewidth=2)


def _add_system_plots(system_metrics, jvm_metrics, cpu, ram, socket, throughput, latency_metrics):
    # Each source is restricted to the load window and normalised to ITS OWN first in-window sample, so
    # every panel begins at x=0 (the user wants the sar curves to start at 0 like the JVM heap, not
    # shifted to a shared absolute origin). sar is captured a few seconds past k6
    # (systemMeasureTrailingSeconds) so it has samples through the end of the load window; clipping here
    # drops those trailing post-load samples and the JVM's pre-load startup samples, so all panels span
    # the same load window without leaking idle data into the chart.
    load_start, load_end = _load_window(latency_metrics)
    system_seconds, (total_cpu, user_cpu, system_cpu, iowait_cpu, mem_used, tcpsck, active_s, passive_s,
                     total_kb_s, rxkb_s, txkb_s) = normalized_to_load_window(
        system_metrics.epoch_seconds,
        [system_metrics.total_cpu, system_metrics.user_cpu, system_metrics.system_cpu, system_metrics.iowait_cpu,
         system_metrics.mem_used, system_metrics.tcpsck, system_metrics.active_s, system_metrics.passive_s,
         system_metrics.total_kb_s, system_metrics.rxkb_s, system_metrics.txkb_s], load_start, load_end)
    jvm_seconds, (jvm_heap_used, jvm_gc_pct) = normalized_to_load_window(
        jvm_metrics.epoch_seconds, [jvm_metrics.heap_used, jvm_metrics.gc_times_percentage], load_start, load_end)

    cpu.plot(system_seconds, total_cpu,
             label=legend_label('Total', total_cpu, '%'), color='black', linestyle='solid', linewidth=2)
    cpu.plot(system_seconds, user_cpu,
             label=legend_label('User', user_cpu, '%'), color='green', linestyle='solid', linewidth=1)
    cpu.plot(system_seconds, system_cpu,
             label=legend_label('System', system_cpu, '%'), color='blue', linestyle='solid', linewidth=1)
    cpu.plot(system_seconds, iowait_cpu,
             label=legend_label('IO Wait', iowait_cpu, '%'), color='red', linestyle='solid', linewidth=1)
    cpu.plot(jvm_seconds, jvm_gc_pct,
             label=legend_label('JVM GC Time', jvm_gc_pct, '%'), color='orange', linestyle='solid', linewidth=1)

    cpu.set_ylabel('CPU', color='black')
    cpu.set_title(title_label('CPU', total_cpu, '%'), weight='bold')
    title_postfix = ('. JVM platform threads: {:,.0f} avg, {:,.0f} max. {:,.0f} GCs totalling {:,.1f}s'
                     .format(np.average(jvm_metrics.platform_thread_count), np.max(jvm_metrics.platform_thread_count),
                             cumulative_window_delta(jvm_metrics.gc_counts), cumulative_window_delta(jvm_metrics.gc_times) / 1000))
    cpu.set_title(title_label(prefix='CPU', measurements=total_cpu, unit='%', postfix=title_postfix), weight='bold')

    ram.plot(jvm_seconds, jvm_heap_used,
             label=legend_label('JVM Heap Used', jvm_heap_used, '%'), color='black', linestyle='solid', linewidth=1)
    ram.plot(system_seconds, mem_used,
             label=legend_label('RAM Used', mem_used, '%'), color='green', linestyle='solid', linewidth=2)

    ram.set_ylabel('Memory', color='black')
    ram.yaxis.set_major_formatter(mtick.PercentFormatter())
    ram.set_title(title_label('JVM Heap', jvm_heap_used, '%'), weight='bold')

    socket.plot(system_seconds, tcpsck,
                label=legend_label('TCP', tcpsck), linestyle='solid', linewidth=2)
    socket.plot(system_seconds, active_s,
                label=legend_label('TCP Active Opens', active_s), linestyle='solid', linewidth=1)
    socket.plot(system_seconds, passive_s,
                label=legend_label('TCP Passive Opens', passive_s), linestyle='solid', linewidth=1)
    socket.set_ylabel('Sockets', color='black')
    socket.set_title(title_label('Sockets', tcpsck, ''), weight='bold')

    def _mbps(kib_per_sec_array):
        return [value / 1024 * 8 for value in kib_per_sec_array]

    total_mbps = _mbps(total_kb_s)
    rx_mbps = _mbps(rxkb_s)
    tx_mbps = _mbps(txkb_s)

    throughput.plot(system_seconds, total_mbps, label=legend_label('Total', total_mbps, 'Mbps'), color='black', linestyle='solid', linewidth=2)
    throughput.plot(system_seconds, rx_mbps, label=legend_label('Received', rx_mbps, 'Mbps'), color='tab:gray', linestyle='solid', linewidth=1)
    throughput.plot(system_seconds, tx_mbps, label=legend_label('Sent', tx_mbps, 'Mbps'), color='tab:green', linestyle='solid', linewidth=1)
    throughput.set_ylabel('Mbps', color='black')
    throughput.set_title(title_label('Throughput', total_mbps, 'Mbps'), weight='bold')
    throughput.set_xlabel('Seconds')


def title_label(prefix, measurements, unit=None, postfix=''):
    if unit is None:
        unit = {}
    return '{} (Min / Median / Max: {:,.0f} / {:,.0f} / {:,.0f}{}{})'.format(prefix, np.min(measurements), np.percentile(measurements, 50),
                                                                             np.max(measurements), unit, postfix)


def legend_label(name, measurements, unit=''):
    return '{}: {:,.0f} / {:,.0f} / {:,.0f}{}'.format(name, np.min(measurements), np.percentile(measurements, 50), np.max(measurements), unit)


def format_float(value):
    return f"{value:.0f}" if value % 1 == 0 or value > 100 else (f"{value:.1f}" if value > 10 else f"{value:.2f}")


def append_results(scenario, approach, latency_metrics, system_metrics, jvm_metrics, results_csv_file):
    values_by_name = {
        'scenario': scenario,
        'approach': approach,

        'requests_ok': len(latency_metrics.latencies_ok),
        'requests_error': len(latency_metrics.latencies_error),

        'requests_per_second_p50': np.percentile(latency_metrics.rps_requested, 50),
        'requests_per_second_p90': np.percentile(latency_metrics.rps_requested, 90),
        'requests_per_second_max': max(latency_metrics.rps_requested),

        'latency_millis_min': min(latency_metrics.latencies_requested),
        'latency_millis_p50': np.percentile(latency_metrics.latencies_requested, 50),
        'latency_millis_p90': np.percentile(latency_metrics.latencies_requested, 90),
        'latency_millis_p99': np.percentile(latency_metrics.latencies_requested, 99),
        'latency_millis_max': max(latency_metrics.latencies_requested),

        # CPU/RAM/socket aggregates are scoped to the load window (computed in SystemMetrics); the
        # full series is still plotted so the chart spans the whole run.
        'cpu_use_percent_avg': system_metrics.cpu_use_avg,
        'cpu_use_percent_max': system_metrics.cpu_use_max,

        'ram_use_percent_avg': system_metrics.ram_use_avg,
        'ram_use_percent_max': system_metrics.ram_use_max,

        'heap_use_percent_avg': sum(jvm_metrics.heap_used) / len(jvm_metrics.heap_used),
        'heap_use_percent_max': max(jvm_metrics.heap_used),

        'garbage_collection_count': cumulative_window_delta(jvm_metrics.gc_counts),
        'garbage_collection_time_millis': cumulative_window_delta(jvm_metrics.gc_times),

        'platform_threads_avg': int(round(np.average(jvm_metrics.platform_thread_count))),
        'platform_threads_max': int(np.max(jvm_metrics.platform_thread_count)),

        'sockets_avg': int(round(system_metrics.sockets_avg)),
        'sockets_max': int(system_metrics.sockets_max),

        'network_kib_per_req_avg': system_metrics.kb_per_request_avg,
        'network_kib_per_req_max': system_metrics.kb_per_request_max,
        'network_packets_per_req_avg': system_metrics.pck_per_request_avg,
        'network_packets_per_req_max': system_metrics.pck_per_request_max
    }
    file_exists = os.path.isfile(results_csv_file)
    with open(results_csv_file, 'a', newline='') as file:
        writer = csv.DictWriter(file, fieldnames=values_by_name.keys())
        if not file_exists:
            writer.writeheader()
        writer.writerow({key: format_float(value) if isinstance(value, (int, float)) else value for key, value in values_by_name.items()})


def append_failed_results(scenario, approach, results_csv_file, requests_error=1):
    values_by_name = {
        'scenario': scenario,
        'approach': approach,

        'requests_ok': 0,
        'requests_error': requests_error,

        'requests_per_second_p50': 0,
        'requests_per_second_p90': 0,
        'requests_per_second_max': 0,

        'latency_millis_min': 0,
        'latency_millis_p50': 0,
        'latency_millis_p90': 0,
        'latency_millis_p99': 0,
        'latency_millis_max': 0,

        'cpu_use_percent_avg': 0,
        'cpu_use_percent_max': 0,

        'ram_use_percent_avg': 0,
        'ram_use_percent_max': 0,

        'heap_use_percent_avg': 0,
        'heap_use_percent_max': 0,

        'garbage_collection_count': 0,
        'garbage_collection_time_millis': 0,

        'platform_threads_avg': 0,
        'platform_threads_max': 0,

        'sockets_avg': 0,
        'sockets_max': 0,

        'network_kib_per_req_avg': 0,
        'network_kib_per_req_max': 0,
        'network_packets_per_req_avg': 0,
        'network_packets_per_req_max': 0
    }
    file_exists = os.path.isfile(results_csv_file)
    os.makedirs(os.path.dirname(results_csv_file), exist_ok=True)
    with open(results_csv_file, 'a', newline='') as file:
        writer = csv.DictWriter(file, fieldnames=values_by_name.keys())
        if not file_exists:
            writer.writeheader()
        writer.writerow(values_by_name)


def main():
    if len(sys.argv) == 6 and sys.argv[1] == "--failed":
        _, _, scenario, approach, results_csv_file, requests_error = sys.argv
        append_failed_results(scenario, approach, results_csv_file, int(requests_error))
        log("Updated " + results_csv_file)
    elif len(sys.argv) != 8:
        print("Syntax: scenario_chart.py <scenario> <approach> <latencyCsvFile> <systemCsvFile> <jvmCsvFile> <outputPngFile> <resultsCsvFile>")
        print("   or: scenario_chart.py --failed <scenario> <approach> <resultsCsvFile> <requestsError>")
        sys.exit(1)
    else:
        start_time = time.time()

        scenario = sys.argv[1]
        approach = sys.argv[2]
        latency_csv_file = sys.argv[3]
        system_csv_file = sys.argv[4]
        jvm_csv_file = sys.argv[5]
        output_png_file = sys.argv[6]
        results_csv_file = sys.argv[7]

        latency_metrics = LatencyMetrics(latency_csv_file)
        system_metrics = SystemMetrics(system_csv_file, latency_metrics)
        jvm_metrics = JvmMetrics(jvm_csv_file)

        os.makedirs(os.path.dirname(output_png_file), exist_ok=True)
        create_plot_png_file(approach + ": " + scenario, latency_metrics, system_metrics, jvm_metrics, output_png_file)
        log("Saved " + output_png_file + " in " + str(int((time.time() - start_time) * 1000)) + "ms")

        append_results(scenario, approach, latency_metrics, system_metrics, jvm_metrics, results_csv_file)
        log("Updated " + results_csv_file)


if __name__ == "__main__":
    main()
