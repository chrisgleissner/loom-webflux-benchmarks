#!/usr/bin/env python3
# Converts latency and system measurement CSV files to PNG

import sys
import csv
import matplotlib.pyplot as plt
import numpy as np
from datetime import datetime
from matplotlib.ticker import ScalarFormatter

def plot_chart(latency_csv_file, system_csv_file, output_png_file):
    fig, (latency, cpu, ram, socket, throughput) = plt.subplots(5, 1, figsize=(15, 15), sharex=True)
    fig.suptitle(title, fontsize=18)
    fig.subplots_adjust(top=0.93)
    plots = (latency, cpu, ram, socket, throughput)

    add_latency_plot(latency_csv_file, latency)
    add_system_plot(system_csv_file, cpu, ram, socket, throughput)

    for plot in plots:
        plot.xaxis.set_major_formatter(ScalarFormatter(useMathText=False))
        plot.xaxis.set_minor_formatter(ScalarFormatter(useMathText=False))
        plot.yaxis.set_major_formatter(ScalarFormatter(useMathText=False))
        plot.yaxis.set_minor_formatter(ScalarFormatter(useMathText=False))
        plot.ticklabel_format(style='plain')
        plot.grid(True, which='both', linestyle=':', linewidth=0.5)
        plot.legend(loc='upper left')

    plt.savefig(output_png_file, format='png', bbox_inches='tight')

def add_system_plot(system_csv_file, cpu, ram, socket, throughput):
    times = []
    user_cpu = []
    system_cpu = []
    iowait_cpu = []
    mem_used = []
    tcpsck = []
    active_s = []
    passive_s = []
    iseg_s = []
    oseg_s = []

    with open(system_csv_file, 'r') as file:
        csv_reader = csv.DictReader(file, delimiter=';')
        for row in csv_reader:
            # see system-measure.csv for available column names
            times.append(datetime.utcfromtimestamp(int(row['timestamp'])))
            user_cpu.append(float(row['%user']))
            system_cpu.append(float(row['%system']))
            iowait_cpu.append(float(row['%iowait']))
            mem_used.append(float(row['%memused']))
            tcpsck.append(float(row['tcpsck']))
            active_s.append(float(row['active/s']))
            passive_s.append(float(row['passive/s']))
            iseg_s.append(float(row['iseg/s']))
            oseg_s.append(float(row['oseg/s']))

    seconds_elapsed = [(timestamp - times[0]).total_seconds() for timestamp in times]

    cpu.plot(seconds_elapsed, user_cpu, label='User', linestyle='solid', linewidth=1)
    cpu.plot(seconds_elapsed, system_cpu, label='System', linestyle='solid', linewidth=1)
    cpu.plot(seconds_elapsed, iowait_cpu, label='IO Wait', linestyle='solid', linewidth=1)
    total_cpu = [user + system + iowait for user, system, iowait in zip(user_cpu, system_cpu, iowait_cpu)]
    cpu.plot(seconds_elapsed, total_cpu, label='Total', color='black', linestyle='solid', linewidth=2)
    cpu.set_ylabel('CPU %', color='black')
    cpu.set_title('CPU', weight='bold')

    ram.plot(seconds_elapsed, mem_used, label='Used', linestyle='solid', linewidth=2)
    ram.set_ylabel('RAM %', color='black')
    ram.set_title('RAM', weight='bold')

    socket.plot(seconds_elapsed, tcpsck, label='TCP', linestyle='solid', linewidth=2)
    socket.plot(seconds_elapsed, active_s, label='TCP Active Opens / s', linestyle='solid', linewidth=1)
    socket.plot(seconds_elapsed, passive_s, label='TCP Passive Opens / s', linestyle='solid', linewidth=1)
    socket.set_ylabel('Sockets', color='black')
    socket.set_title('Sockets', weight='bold')

    throughput.plot(seconds_elapsed, iseg_s, label='Received / s', color='tab:gray', linestyle='solid', linewidth=2)
    throughput.plot(seconds_elapsed, oseg_s, label='Sent / s', color='tab:olive', linestyle='solid', linewidth=2)
    throughput.set_ylabel('TCP Segments', color='black')
    throughput.set_title('Throughput', weight='bold')
    throughput.set_xlabel('Seconds')

def add_latency_plot(latency_csv_file, latency_plot):
    times = []
    status_codes = []
    latencies = []

    with open(latency_csv_file, 'r') as file:
        reader = csv.reader(file)
        next(reader)
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
        if (status_code != 200):
            error_seconds_elapsed.append(time)
            error_latencies.append(latency)
        if time < latency_bucket_end:
            bucket_latencies.append(latency)
        else:
            append_percentile_values(bucket_latencies, p50_values, p90_values, p99_values)
            latency_bucket_start = time
            latency_bucket_end = latency_bucket_start + 1
            bucket_latencies = [latency]

    append_percentile_values(bucket_latencies, p50_values, p90_values, p99_values)

    latency_plot.scatter(seconds_elapsed, latencies, label='Latency', c='grey', alpha=0.7, s=1)
    latency_plot.scatter(error_seconds_elapsed, error_latencies, label='Error', c='red')

    x_bucket = np.arange(len(p50_values))
    latency_plot.plot(x_bucket, p99_values, label='p99: ' + str(int(np.percentile(latencies, 99))) + "ms", color='blue', linewidth=1)
    latency_plot.plot(x_bucket, p90_values, label='p90: ' + str(int(np.percentile(latencies, 90))) + "ms", color='green', linewidth=1)
    latency_plot.plot(x_bucket, p50_values, label='p50: ' + str(int(np.percentile(latencies, 50))) + "ms", color='black', linewidth=2)

    latency_plot.set_ylabel('Latency (ms)', color='black')
    latency_plot.set_title('Latency', weight='bold')
    latency_plot.set_yscale('log')

if __name__ == "__main__":
    if len(sys.argv) != 5:
        print("Syntax: system-chart.py <title> <latencyCsvFile> <systemCsvFile> <outputPngFile>")
    else:
        title = sys.argv[1]
        latency_csv_file = sys.argv[2]
        system_csv_file = sys.argv[3]
        output_png_file = sys.argv[4]
        plot_chart(latency_csv_file, system_csv_file, output_png_file)
        print("Saved " + output_png_file)
