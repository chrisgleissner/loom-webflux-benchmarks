#!/usr/bin/env python3
# Converts latency and system measurement CSV files to PNG

import csv
import matplotlib.pyplot as plt
import matplotlib.ticker as mtick
import numpy as np
import sys
from datetime import datetime
from matplotlib.ticker import LogLocator
from matplotlib.ticker import ScalarFormatter


def plot_chart(latency_csv_file, system_csv_file, jvm_csv_file, output_png_file):
    fig, (latency, rps, cpu, ram, socket, throughput) = plt.subplots(6, 1, figsize=(25, 20), sharex=True)
    fig.suptitle(title, fontsize=18)
    fig.subplots_adjust(top=0.93)
    plots = (latency, rps, cpu, ram, socket, throughput)

    add_request_plots(latency_csv_file, latency, rps)
    add_system_plots(system_csv_file, jvm_csv_file, cpu, ram, socket, throughput)

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


def add_system_plots(system_csv_file, jvm_csv_file, cpu, ram, socket, throughput):
    system_times = []
    user_cpu = []
    system_cpu = []
    iowait_cpu = []
    mem_used = []
    tcpsck = []
    active_s = []
    passive_s = []
    iseg_s = []
    oseg_s = []

    jvm_times = []
    heap_used = []
    gc_counts = []
    gc_times = []
    platform_thread_count = []

    with open(system_csv_file, 'r') as file:
        csv_reader = csv.DictReader(file, delimiter=';')
        for row in csv_reader:
            system_times.append(datetime.utcfromtimestamp(int(row['timestamp'])))
            user_cpu.append(float(row['%user']))
            system_cpu.append(float(row['%system']))
            iowait_cpu.append(float(row['%iowait']))
            mem_used.append(float(row['%memused']))
            tcpsck.append(float(row['tcpsck']))
            active_s.append(float(row['active/s']))
            passive_s.append(float(row['passive/s']))
            iseg_s.append(float(row['iseg/s']))
            oseg_s.append(float(row['oseg/s']))

    with open(jvm_csv_file, 'r') as file:
        csv_reader = csv.DictReader(file, delimiter=',')
        for row in csv_reader:
            jvm_times.append(datetime.utcfromtimestamp(int(row['epochMillis']) / 1000))
            heap_used.append(100.0 * float(row['memUsed']) / float(row['memMax']))
            gc_counts.append(int(row['gcCount']))
            gc_times.append(int(row['gcTime']))
            platform_thread_count.append(int(row['platformThreadCount']))

    system_seconds_elapsed = [(timestamp - system_times[0]).total_seconds() for timestamp in system_times]
    jvm_seconds_elapsed = [(timestamp - jvm_times[0]).total_seconds() for timestamp in jvm_times]

    total_cpu = [user + system + iowait for user, system, iowait in zip(user_cpu, system_cpu, iowait_cpu)]
    cpu.plot(system_seconds_elapsed, total_cpu, label=legend_label('Total', total_cpu, '%'), color='black',
             linestyle='solid', linewidth=2)
    cpu.plot(system_seconds_elapsed, user_cpu, label=legend_label('User', user_cpu, '%'), color='green',
             linestyle='solid', linewidth=1)
    cpu.plot(system_seconds_elapsed, system_cpu, label=legend_label('System', system_cpu, '%'), color='blue',
             linestyle='solid', linewidth=1)
    cpu.plot(system_seconds_elapsed, iowait_cpu, label=legend_label('IO Wait', iowait_cpu, '%'), color='red',
             linestyle='solid', linewidth=1)
    gc_time_diff = [gc_times[i] - gc_times[i - 1] for i in range(1, len(gc_times))]
    gc_times_percentage = [(time_diff / 1000) * 100 for time_diff in gc_time_diff]
    gc_times_percentage.insert(0, 0)
    cpu.plot(jvm_seconds_elapsed, gc_times_percentage, label=legend_label('JVM GC Time', gc_times_percentage, '%'),
             color='orange', linestyle='solid', linewidth=1)
    cpu.set_ylabel('CPU', color='black')
    cpu.set_title(title_label('CPU', total_cpu, '%'), weight='bold')
    title_postfix = ('. JVM platform threads: {:,.0f} avg, {:,.0f} max. {:,.0f} GCs totalling {:,.1f}s'
                     .format(np.average(platform_thread_count), np.max(platform_thread_count), gc_counts[-1],
                             gc_times[-1] / 1000))
    cpu.set_title(title_label(prefix='CPU', measurements=total_cpu, unit='%', postfix=title_postfix), weight='bold')

    ram.plot(jvm_seconds_elapsed, heap_used, label=legend_label('JVM Heap Used', heap_used, '%'), color='black',
             linestyle='solid', linewidth=1)
    ram.plot(system_seconds_elapsed, mem_used, label=legend_label('RAM Used', mem_used, '%'), color='green',
             linestyle='solid', linewidth=2)

    ram.set_ylabel('Memory', color='black')
    ram.yaxis.set_major_formatter(mtick.PercentFormatter())
    ram.set_title(title_label('JVM Heap', heap_used, '%'), weight='bold')

    socket.plot(system_seconds_elapsed, tcpsck, label=legend_label('TCP', tcpsck), linestyle='solid', linewidth=2)
    socket.plot(system_seconds_elapsed, active_s, label=legend_label('TCP Active Opens / s', active_s),
                linestyle='solid', linewidth=1)
    socket.plot(system_seconds_elapsed, passive_s, label=legend_label('TCP Passive Opens / s', passive_s),
                linestyle='solid', linewidth=1)
    socket.set_ylabel('Sockets', color='black')
    socket.set_title(title_label('Sockets', tcpsck, ''), weight='bold')

    total_seg_s = [iseg_s + oseg_s for iseg_s, oseg_s in zip(iseg_s, oseg_s)]
    throughput.plot(system_seconds_elapsed, total_seg_s, label=legend_label('Total', total_seg_s, ''), color='black',
                    linestyle='solid', linewidth=2)
    throughput.plot(system_seconds_elapsed, iseg_s, label=legend_label('Received / s', iseg_s), color='tab:gray',
                    linestyle='solid', linewidth=1)
    throughput.plot(system_seconds_elapsed, oseg_s, label=legend_label('Sent / s', oseg_s), color='tab:green',
                    linestyle='solid', linewidth=1)
    throughput.set_ylabel('TCP Segments', color='black')
    throughput.set_title(title_label('Throughput', total_seg_s, ''), weight='bold')
    throughput.set_xlabel('Seconds')


def add_request_plots(latency_csv_file, latency_plot, rps_plot):
    times = []
    status_codes = []
    latencies = []

    with open(latency_csv_file, 'r') as file:
        reader = csv.reader(file)
        for row in reader:
            times.append(int(row[0]) / 1000)
            latencies.append(float(row[1]))
            status_codes.append(int(row[2]))

    min_time = np.array(times).min()
    seconds_elapsed = [t - min_time for t in times]
    latency_bucket_start = seconds_elapsed[0]
    latency_bucket_end = latency_bucket_start + 1
    bucket_latencies = []
    error_seconds_elapsed = []
    error_latencies = []
    p50_values = []
    p90_values = []
    p99_values = []

    def append_percentile_values(bucket_latencies, p50_values, p90_values, p99_values):
        p50_values.append(np.percentile(bucket_latencies, 50))
        p90_values.append(np.percentile(bucket_latencies, 90))
        p99_values.append(np.percentile(bucket_latencies, 99))

    for time, status_code, latency in zip(seconds_elapsed, status_codes, latencies):
        if int(status_code / 100) != 2:
            error_seconds_elapsed.append(time)
            error_latencies.append(latency + 0.1)  # Ensure latency 0 is plotted
        if time < latency_bucket_end:
            bucket_latencies.append(latency)
        else:
            append_percentile_values(bucket_latencies, p50_values, p90_values, p99_values)
            latency_bucket_start = time
            latency_bucket_end = latency_bucket_start + 1
            bucket_latencies = [latency]

    append_percentile_values(bucket_latencies, p50_values, p90_values, p99_values)
    latency_plot.scatter(seconds_elapsed, latencies, label=legend_label('Latency', latencies, 'ms'), color='silver',
                         alpha=0.7, s=1)
    latency_plot.scatter(error_seconds_elapsed, error_latencies, label='Errors: ' + str(len(error_latencies)),
                         color='red')
    x_bucket = np.arange(len(p50_values))
    latency_plot.plot(x_bucket, p99_values, label='p99: {:,.0f}ms'.format(np.percentile(latencies, 99)), color='blue',
                      linewidth=1)
    latency_plot.plot(x_bucket, p90_values, label='p90: {:,.0f}ms'.format(np.percentile(latencies, 90)), color='green',
                      linewidth=1)
    latency_plot.plot(x_bucket, p50_values, label='p50: {:,.0f}ms'.format(np.percentile(latencies, 50)), color='black',
                      linewidth=2)
    latency_plot.set_ylabel('Latency (ms)', color='black')
    latency_plot.set_title(title_label('Latency', latencies, 'ms'), weight='bold')
    latency_plot.set_yscale('log')
    latency_plot.yaxis.set_minor_locator(LogLocator(subs=(1.0, 3.0)))

    rps_bin_count = int(np.ceil(seconds_elapsed[-1])) + 1
    rps, _ = np.histogram(seconds_elapsed, bins=rps_bin_count, range=(0, rps_bin_count))
    rps_plot.plot(list(range(rps_bin_count)), rps, label=legend_label('RPS', rps), color='black', linewidth=2)
    if len(error_latencies) > 0:
        rps_error, _ = np.histogram(error_seconds_elapsed, bins=rps_bin_count, range=(0, rps_bin_count))
        rps_plot.plot(list(range(rps_bin_count)), rps_error, label=legend_label('RPS (Failed)', rps_error),
                      color='orange', linewidth=2)
    rps_plot.set_ylabel('Requests per second', color='black')
    title_postfix = '. Total requests: {:,.0f} ok, {:,.0f} failed.'.format(len(latencies) - len(error_latencies),
                                                                           len(error_latencies))
    rps_plot.set_title(title_label(prefix='RPS', measurements=rps, unit='', postfix=title_postfix), weight='bold')


def title_label(prefix, measurements, unit={}, postfix=''):
    return '{} (Min / Avg / Max: {:,.0f} / {:,.0f} / {:,.0f}{}{})'.format(prefix, np.min(measurements),
                                                                          np.average(measurements),
                                                                          np.max(measurements), unit, postfix)


def legend_label(name, measurements, unit=''):
    return '{}: {:,.0f} / {:,.0f} / {:,.0f}{}'.format(name, np.min(measurements), np.average(measurements),
                                                      np.max(measurements), unit)


if __name__ == "__main__":
    if len(sys.argv) != 6:
        print("Syntax: system-chart.py <title> <latencyCsvFile> <systemCsvFile> <jvmCsvFile> <outputPngFile>")
    else:
        title = sys.argv[1]
        latency_csv_file = sys.argv[2]
        system_csv_file = sys.argv[3]
        jvm_csv_file = sys.argv[4]
        output_png_file = sys.argv[5]
        plot_chart(latency_csv_file, system_csv_file, jvm_csv_file, output_png_file)
