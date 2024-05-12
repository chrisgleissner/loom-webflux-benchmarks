#!/usr/bin/env python3
# Converts latency, JVM metric, and system metric CSV files to a PNG file.

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


class LatencyMetrics:

    def __init__(self, filename):
        self.times = []
        self.seconds_elapsed = []
        self.latencies = []
        self.status_codes = []
        self.error_seconds_elapsed = []
        self.error_latencies = []
        self.rps = []
        self.rps_error = []
        self.rps_bin_count = 0
        self.percentile_time_buckets = []
        self.p50_values = []
        self.p90_values = []
        self.p99_values = []

        self._parse_csv(filename)

        min_time = np.array(self.times).min()
        self.seconds_elapsed = [t - min_time for t in self.times]
        self._calculate_latency()
        self._calculate_rps()
        self.percentile_time_buckets = np.arange(len(self.p50_values))

    def _parse_csv(self, filename):
        with open(filename, 'r') as file:
            reader = csv.reader(file)
            for row in reader:
                self.times.append(int(row[0]) / 1000)
                self.latencies.append(float(row[1]))
                self.status_codes.append(int(row[2]))

    def _calculate_latency(self):
        latency_bucket_start = self.seconds_elapsed[0]
        latency_bucket_end = latency_bucket_start + 1
        bucket_latencies = []

        def _append_percentile_values():
            self.p50_values.append(np.percentile(bucket_latencies, 50))
            self.p90_values.append(np.percentile(bucket_latencies, 90))
            self.p99_values.append(np.percentile(bucket_latencies, 99))

        for seconds_elapsed, status_code, latency in zip(self.seconds_elapsed, self.status_codes, self.latencies):
            status_ok = int(status_code / 100) == 2
            if not status_ok:
                self.error_seconds_elapsed.append(seconds_elapsed)
                self.error_latencies.append(latency + 0.1)  # Ensure latency 0 is plotted
            if seconds_elapsed < latency_bucket_end:
                if status_ok:
                    bucket_latencies.append(latency)
            else:
                _append_percentile_values()
                latency_bucket_start = seconds_elapsed
                latency_bucket_end = latency_bucket_start + 1
                bucket_latencies = [latency]

        _append_percentile_values()

    def _calculate_rps(self):
        self.rps_bin_count = int(np.ceil(self.seconds_elapsed[-1])) + 1
        self.rps, _ = np.histogram(self.seconds_elapsed, bins=self.rps_bin_count, range=(0, self.rps_bin_count))
        if len(self.error_latencies) > 0:
            self.rps_error, _ = np.histogram(self.error_seconds_elapsed, bins=self.rps_bin_count, range=(0, self.rps_bin_count))


class SystemMetrics:

    def __init__(self, filename, latency_metrics):
        self.system_times = []
        self.user_cpu = []
        self.system_cpu = []
        self.iowait_cpu = []
        self.mem_used = []
        self.tcpsck = []
        self.active_s = []
        self.passive_s = []
        self.iseg_s = []
        self.oseg_s = []

        self._parse_csv(filename)

        self.total_seg_s = [iseg_s + oseg_s for iseg_s, oseg_s in zip(self.iseg_s, self.oseg_s)]
        self.total_cpu = [user + system + iowait for user, system, iowait in zip(self.user_cpu, self.system_cpu, self.iowait_cpu)]
        self.seconds_elapsed = [(timestamp - self.system_times[0]).total_seconds() for timestamp in self.system_times]

        tcp_segments_len = min(len(self.total_seg_s), len(latency_metrics.rps))
        self.tcp_segments_per_request_total = np.divide(self.total_seg_s[:tcp_segments_len], latency_metrics.rps[:tcp_segments_len])
        self.tcp_segments_per_request_rcv = np.divide(self.iseg_s[:tcp_segments_len], latency_metrics.rps[:tcp_segments_len])
        self.tcp_segments_per_request_snt = np.divide(self.oseg_s[:tcp_segments_len], latency_metrics.rps[:tcp_segments_len])

    def _parse_csv(self, filename):
        with open(filename, 'r') as file:
            csv_reader = csv.DictReader(file, delimiter=';')
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


def plot(title, latency_metrics, system_metrics, jvm_metrics, output_png_file):
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
                         label=legend_label('Latency', latency_metrics.latencies, 'ms'), color='silver', alpha=0.7, s=1)
    latency_plot.scatter(latency_metrics.error_seconds_elapsed, latency_metrics.error_latencies,
                         label='Errors: ' + str(len(latency_metrics.error_latencies)), color='red')

    latency_plot.plot(latency_metrics.percentile_time_buckets, latency_metrics.p99_values,
                      label='p99: {:,.0f}ms'.format(np.percentile(latency_metrics.latencies, 99)), color='blue', linewidth=1)
    latency_plot.plot(latency_metrics.percentile_time_buckets, latency_metrics.p90_values,
                      label='p90: {:,.0f}ms'.format(np.percentile(latency_metrics.latencies, 90)), color='green', linewidth=1)
    latency_plot.plot(latency_metrics.percentile_time_buckets, latency_metrics.p50_values,
                      label='p50: {:,.0f}ms'.format(np.percentile(latency_metrics.latencies, 50)), color='black', linewidth=2)

    latency_plot.set_ylabel('Latency (ms)', color='black')
    latency_plot.set_title(title_label('Latency', latency_metrics.latencies, 'ms'), weight='bold')
    latency_plot.set_yscale('log')
    latency_plot.yaxis.set_minor_locator(LogLocator(subs=(1.0, 3.0)))

    if len(latency_metrics.error_latencies) > 0:
        rps_plot.plot(list(range(latency_metrics.rps_bin_count)), latency_metrics.rps_error,
                      label=legend_label('RPS (Failed)', latency_metrics.rps_error), color='orange', linewidth=2)

    rps_plot.plot(list(range(latency_metrics.rps_bin_count)), latency_metrics.rps, label=legend_label('RPS', latency_metrics.rps), color='black', linewidth=2)
    rps_plot.set_ylabel('Requests per second', color='black')
    title_postfix = '. Total requests: {:,.0f} ok, {:,.0f} failed.'.format(len(latency_metrics.latencies) - len(latency_metrics.error_latencies),
                                                                           len(latency_metrics.error_latencies))
    rps_plot.set_title(title_label(prefix='RPS', measurements=latency_metrics.rps, unit='', postfix=title_postfix), weight='bold')


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
                label=legend_label('TCP Active Opens / s', system_metrics.active_s), linestyle='solid', linewidth=1)
    socket.plot(system_metrics.seconds_elapsed, system_metrics.passive_s,
                label=legend_label('TCP Passive Opens / s', system_metrics.passive_s), linestyle='solid', linewidth=1)
    socket.set_ylabel('Sockets', color='black')
    socket.set_title(title_label('Sockets', system_metrics.tcpsck, ''), weight='bold')

    throughput.plot(system_metrics.seconds_elapsed, system_metrics.total_seg_s,
                    label=legend_label('Total', system_metrics.total_seg_s, ''), color='black', linestyle='solid', linewidth=2)
    throughput.plot(system_metrics.seconds_elapsed, system_metrics.iseg_s,
                    label=legend_label('Received / s', system_metrics.iseg_s), color='tab:gray', linestyle='solid', linewidth=1)
    throughput.plot(system_metrics.seconds_elapsed, system_metrics.oseg_s,
                    label=legend_label('Sent / s', system_metrics.oseg_s), color='tab:green', linestyle='solid', linewidth=1)
    throughput.set_ylabel('TCP Segments', color='black')
    throughput.set_title(title_label('Throughput', system_metrics.total_seg_s, ''), weight='bold')
    throughput.set_xlabel('Seconds')


def title_label(prefix, measurements, unit=None, postfix=''):
    if unit is None:
        unit = {}
    return '{} (Min / Avg / Max: {:,.0f} / {:,.0f} / {:,.0f}{}{})'.format(prefix, np.min(measurements), np.average(measurements), np.max(measurements), unit, postfix)


def legend_label(name, measurements, unit=''):
    return '{}: {:,.0f} / {:,.0f} / {:,.0f}{}'.format(name, np.min(measurements), np.average(measurements), np.max(measurements), unit)


def format_float(value):
    return f"{value:.0f}" if value % 1 == 0 or value > 100 else (f"{value:.1f}" if value > 10 else f"{value:.2f}")


def append_results(scenario, approach, latency_metrics, system_metrics, jvm_metrics, results_csv_file):
    values_by_name = {
        'scenario': scenario,
        'approach': approach,
        'requests_ok': sum(latency_metrics.rps),
        'requests_error': len(latency_metrics.error_latencies),
        'rps_avg': sum(latency_metrics.rps) / len(latency_metrics.rps),
        'rps_max': max(latency_metrics.rps),
        'latency_min': min(latency_metrics.latencies),
        'latency_avg': sum(latency_metrics.latencies) / len(latency_metrics.latencies),
        'latency_p50': np.percentile(latency_metrics.latencies, 50),
        'latency_p90': np.percentile(latency_metrics.latencies, 90),
        'latency_p99': np.percentile(latency_metrics.latencies, 99),
        'latency_max': max(latency_metrics.latencies),
        'cpu_total_use_percent_avg': sum(system_metrics.total_cpu) / len(system_metrics.total_cpu),
        'cpu_total_use_percent_max': max(system_metrics.total_cpu),
        'cpu_system_use_percent_avg': sum(system_metrics.system_cpu) / len(system_metrics.system_cpu),
        'cpu_system_use_percent_max': max(system_metrics.system_cpu),
        'cpu_iowait_use_percent_avg': sum(system_metrics.iowait_cpu) / len(system_metrics.iowait_cpu),
        'cpu_iowait_use_percent_max': max(system_metrics.iowait_cpu),
        'ram_use_percent_avg': sum(system_metrics.mem_used) / len(system_metrics.mem_used),
        'ram_use_percent_max': max(system_metrics.mem_used),
        'heap_use_percent_avg': sum(jvm_metrics.heap_used) / len(jvm_metrics.heap_used),
        'heap_use_percent_max': max(jvm_metrics.heap_used),
        'gc_count': sum(jvm_metrics.gc_counts),
        'gc_time_millis': sum(jvm_metrics.gc_times),
        'platform_threads_avg': int(np.average(jvm_metrics.platform_thread_count)),
        'platform_threads_max': int(np.max(jvm_metrics.platform_thread_count)),
        'sockets_avg': int(sum(system_metrics.tcpsck) / len(system_metrics.tcpsck)),
        'sockets_max': int(max(system_metrics.tcpsck)),
        'tcp_segments_per_req_min': int(min(system_metrics.tcp_segments_per_request_total)),
        'tcp_segments_per_req_avg': int(np.average(system_metrics.tcp_segments_per_request_total)),
        'tcp_segments_per_req_max': int(max(system_metrics.tcp_segments_per_request_total))
    }
    file_exists = os.path.isfile(results_csv_file)
    with open(results_csv_file, 'a', newline='') as file:
        writer = csv.DictWriter(file, fieldnames=values_by_name.keys())
        if not file_exists:
            writer.writeheader()
        writer.writerow({key: format_float(value) if isinstance(value, (int, float)) else value for key, value in values_by_name.items()})


def main():
    if len(sys.argv) != 8:
        print("Syntax: scenario_chart.py <scenario> <approach> <latencyCsvFile> <systemCsvFile> <jvmCsvFile> <outputPngFile> <scenarioStatsFile>")
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

        plot(approach + ": " + scenario, latency_metrics, system_metrics, jvm_metrics, output_png_file)
        log("Saved " + output_png_file + " in " + str(int((time.time() - start_time) * 1000)) + "ms")

        append_results(scenario, approach, latency_metrics, system_metrics, jvm_metrics, results_csv_file)
        log("Updated " + results_csv_file)


if __name__ == "__main__":
    main()
