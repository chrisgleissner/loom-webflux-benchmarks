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
from datetime import datetime
from matplotlib.ticker import LogLocator
from matplotlib.ticker import ScalarFormatter


def log(msg):
    print(datetime.now().strftime("%H:%M:%S") + " " + msg)


def divide0(a, b):
    return np.divide(a, b, out=np.zeros_like(a), where=b != 0)


def is_ok(status_code):
    return int(status_code / 100) == 2


def _seconds_elapsed(times):
    min_time = np.array(times).min()
    return [t - min_time for t in times]


def _calculate_rps(seconds_elapsed):
    seconds_bin = int(np.ceil(seconds_elapsed[-1])) + 1 if seconds_elapsed else 0
    rps = np.histogram(seconds_elapsed, bins=seconds_bin, range=(0, seconds_bin))[0] if seconds_elapsed else []
    return rps


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

                if latency > 0:
                    self.times_requested.append(time)
                    self.latencies_requested.append(latency)

                if is_ok(status_code):
                    self.times_ok.append(time)
                    self.latencies_ok.append(latency)

    def _calculate_latency_1s_buckets_and_errors(self):
        bucket_latencies = []
        bucket_latencies_seconds_elapsed_threshold = self.seconds_elapsed[0] + 1

        def _append_percentile_values_from_bucket_latencies():
            if len(bucket_latencies) > 0:
                self.latency_1s_buckets_p50.append(np.percentile(bucket_latencies, 50))
                self.latency_1s_buckets_p90.append(np.percentile(bucket_latencies, 90))
                self.latency_1s_buckets_p99.append(np.percentile(bucket_latencies, 99))
            else:
                self.latency_1s_buckets_p50.append(0)
                self.latency_1s_buckets_p90.append(0)
                self.latency_1s_buckets_p99.append(0)

        for seconds_elapsed, status_code, latency in zip(self.seconds_elapsed, self.status_codes, self.latencies):
            bucket_latencies.append(latency)
            if not is_ok(status_code):
                self.seconds_elapsed_error.append(seconds_elapsed)
                self.latencies_error.append(latency + 0.1)  # Ensure latency 0 is plotted
            if seconds_elapsed >= bucket_latencies_seconds_elapsed_threshold:
                _append_percentile_values_from_bucket_latencies()
                bucket_latencies_seconds_elapsed_threshold = seconds_elapsed + 1
                bucket_latencies = []

        _append_percentile_values_from_bucket_latencies()

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

        self.total_cpu = [user + system + iowait for user, system, iowait in zip(self.user_cpu, self.system_cpu, self.iowait_cpu)]
        self.seconds_elapsed = [(timestamp - self.system_times[0]).total_seconds() for timestamp in self.system_times]

        self.total_kb_s = [rxkb_s + txkb_s for rxkb_s, txkb_s in zip(self.rxkb_s, self.txkb_s)]
        kb_len = min(len(self.total_kb_s), len(latency_metrics.rps))
        self.kb_per_request_total = divide0(self.total_kb_s[:kb_len], latency_metrics.rps[:kb_len])

        self.total_pck_s = [rxpck_s + txpck_s for rxpck_s, txpck_s in zip(self.rxpck_s, self.txpck_s)]
        pck_len = min(len(self.total_pck_s), len(latency_metrics.rps))
        self.pck_per_request_total = divide0(self.total_pck_s[:pck_len], latency_metrics.rps[:pck_len])

    def _parse_csv(self, filename):
        with open(filename, 'r') as file:
            csv_reader = csv.DictReader(file, delimiter=';')
            # CSV cols: timestamp;%user;%system;%iowait;%memused;rxpck/s;txpck/s;rxkB/s;txkB/s;tcpsck;active/s;passive/s;iseg/s;oseg/s
            for row in csv_reader:
                self.system_times.append(datetime.utcfromtimestamp(int(row['timestamp'])))
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


class JvmMetrics:

    def __init__(self, filename):
        self.jvm_times = []
        self.heap_used = []
        self.gc_counts = []
        self.gc_times = []
        self.platform_thread_count = []

        self._parse_csv(filename)

        self.seconds_elapsed = [(timestamp - self.jvm_times[0]).total_seconds() for timestamp in self.jvm_times]
        gc_time_diff = [self.gc_times[i] - self.gc_times[i - 1] for i in range(1, len(self.gc_times))]
        self.gc_times_percentage = [(time_diff / 1000) * 100 for time_diff in gc_time_diff]
        self.gc_times_percentage.insert(0, 0)

    def _parse_csv(self, filename):
        with open(filename, 'r') as file:
            csv_reader = csv.DictReader(file, delimiter=',')
            for row in csv_reader:
                self.jvm_times.append(datetime.utcfromtimestamp(int(row['epochMillis']) / 1000))
                self.heap_used.append(100.0 * float(row['memUsed']) / float(row['memMax']))
                self.gc_counts.append(int(row['gcCount']))
                self.gc_times.append(int(row['gcTime']))
                self.platform_thread_count.append(int(row['platformThreadCount']))


def create_plot_png_file(title, latency_metrics, system_metrics, jvm_metrics, output_png_file):
    fig, (latency, rps, cpu, ram, socket, throughput) = plt.subplots(6, 1, figsize=(25, 20), sharex=True)
    fig.suptitle(title, fontsize=18)
    fig.subplots_adjust(top=0.93)
    plots = (latency, rps, cpu, ram, socket, throughput)

    _add_request_plots(latency_metrics, latency, rps)
    _add_system_plots(system_metrics, jvm_metrics, cpu, ram, socket, throughput)

    for plot in plots:
        plot.xaxis.set_major_formatter(ScalarFormatter(useMathText=False))
        plot.grid(True, which='both', linestyle=':', linewidth=0.5)
        plot.legend(loc='upper left')

    for plot in (latency, rps, socket, throughput):
        plot.yaxis.set_major_formatter(ScalarFormatter(useMathText=False))
        plot.yaxis.set_minor_formatter(ScalarFormatter(useMathText=False))
        plot.ticklabel_format(style='plain')

    for plot in (cpu, ram):
        plot.yaxis.set_major_formatter(mtick.PercentFormatter(100.0))

    plt.savefig(output_png_file, format='png', bbox_inches='tight')


def _add_request_plots(latency_metrics, latency_plot, rps_plot):
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


def _add_system_plots(system_metrics, jvm_metrics, cpu, ram, socket, throughput):
    cpu.plot(system_metrics.seconds_elapsed, system_metrics.total_cpu,
             label=legend_label('Total', system_metrics.total_cpu, '%'), color='black', linestyle='solid', linewidth=2)
    cpu.plot(system_metrics.seconds_elapsed, system_metrics.user_cpu,
             label=legend_label('User', system_metrics.user_cpu, '%'), color='green', linestyle='solid', linewidth=1)
    cpu.plot(system_metrics.seconds_elapsed, system_metrics.system_cpu,
             label=legend_label('System', system_metrics.system_cpu, '%'), color='blue', linestyle='solid', linewidth=1)
    cpu.plot(system_metrics.seconds_elapsed, system_metrics.iowait_cpu,
             label=legend_label('IO Wait', system_metrics.iowait_cpu, '%'), color='red', linestyle='solid', linewidth=1)
    cpu.plot(jvm_metrics.seconds_elapsed, jvm_metrics.gc_times_percentage,
             label=legend_label('JVM GC Time', jvm_metrics.gc_times_percentage, '%'), color='orange', linestyle='solid', linewidth=1)

    cpu.set_ylabel('CPU', color='black')
    cpu.set_title(title_label('CPU', system_metrics.total_cpu, '%'), weight='bold')
    title_postfix = ('. JVM platform threads: {:,.0f} avg, {:,.0f} max. {:,.0f} GCs totalling {:,.1f}s'
                     .format(np.average(jvm_metrics.platform_thread_count), np.max(jvm_metrics.platform_thread_count),
                             jvm_metrics.gc_counts[-1], jvm_metrics.gc_times[-1] / 1000))
    cpu.set_title(title_label(prefix='CPU', measurements=system_metrics.total_cpu, unit='%', postfix=title_postfix), weight='bold')

    ram.plot(jvm_metrics.seconds_elapsed, jvm_metrics.heap_used,
             label=legend_label('JVM Heap Used', jvm_metrics.heap_used, '%'), color='black', linestyle='solid', linewidth=1)
    ram.plot(system_metrics.seconds_elapsed, system_metrics.mem_used,
             label=legend_label('RAM Used', system_metrics.mem_used, '%'), color='green', linestyle='solid', linewidth=2)

    ram.set_ylabel('Memory', color='black')
    ram.yaxis.set_major_formatter(mtick.PercentFormatter())
    ram.set_title(title_label('JVM Heap', jvm_metrics.heap_used, '%'), weight='bold')

    socket.plot(system_metrics.seconds_elapsed, system_metrics.tcpsck,
                label=legend_label('TCP', system_metrics.tcpsck), linestyle='solid', linewidth=2)
    socket.plot(system_metrics.seconds_elapsed, system_metrics.active_s,
                label=legend_label('TCP Active Opens', system_metrics.active_s), linestyle='solid', linewidth=1)
    socket.plot(system_metrics.seconds_elapsed, system_metrics.passive_s,
                label=legend_label('TCP Passive Opens', system_metrics.passive_s), linestyle='solid', linewidth=1)
    socket.set_ylabel('Sockets', color='black')
    socket.set_title(title_label('Sockets', system_metrics.tcpsck, ''), weight='bold')

    def _mbps(kib_per_sec_array):
        return [value / 1024 * 8 for value in kib_per_sec_array]

    total_mbps = _mbps(system_metrics.total_kb_s)
    rx_mbps = _mbps(system_metrics.rxkb_s)
    tx_mbps = _mbps(system_metrics.txkb_s)

    throughput.plot(system_metrics.seconds_elapsed, total_mbps, label=legend_label('Total', total_mbps, 'Mbps'), color='black', linestyle='solid', linewidth=2)
    throughput.plot(system_metrics.seconds_elapsed, rx_mbps, label=legend_label('Received', rx_mbps, 'Mbps'), color='tab:gray', linestyle='solid', linewidth=1)
    throughput.plot(system_metrics.seconds_elapsed, tx_mbps, label=legend_label('Sent', tx_mbps, 'Mbps'), color='tab:green', linestyle='solid', linewidth=1)
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

        'cpu_use_percent_avg': sum(system_metrics.total_cpu) / len(system_metrics.total_cpu),
        'cpu_use_percent_max': max(system_metrics.total_cpu),

        'ram_use_percent_avg': sum(system_metrics.mem_used) / len(system_metrics.mem_used),
        'ram_use_percent_max': max(system_metrics.mem_used),

        'heap_use_percent_avg': sum(jvm_metrics.heap_used) / len(jvm_metrics.heap_used),
        'heap_use_percent_max': max(jvm_metrics.heap_used),

        'garbage_collection_count': sum(jvm_metrics.gc_counts),
        'garbage_collection_time_millis': sum(jvm_metrics.gc_times),

        'platform_threads_avg': int(np.average(jvm_metrics.platform_thread_count)),
        'platform_threads_max': int(np.max(jvm_metrics.platform_thread_count)),

        'sockets_avg': int(sum(system_metrics.tcpsck) / len(system_metrics.tcpsck)),
        'sockets_max': int(max(system_metrics.tcpsck)),

        'network_kib_per_req_avg': np.average(system_metrics.kb_per_request_total),
        'network_kib_per_req_max': max(system_metrics.kb_per_request_total),
        'network_packets_per_req_avg': np.average(system_metrics.pck_per_request_total),
        'network_packets_per_req_max': max(system_metrics.pck_per_request_total)
    }
    file_exists = os.path.isfile(results_csv_file)
    with open(results_csv_file, 'a', newline='') as file:
        writer = csv.DictWriter(file, fieldnames=values_by_name.keys())
        if not file_exists:
            writer.writeheader()
        writer.writerow({key: format_float(value) if isinstance(value, (int, float)) else value for key, value in values_by_name.items()})


def main():
    if len(sys.argv) != 8:
        print("Syntax: scenario_chart.py <scenario> <approach> <latencyCsvFile> <systemCsvFile> <jvmCsvFile> <outputPngFile> <resultsCsvFile>")
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
