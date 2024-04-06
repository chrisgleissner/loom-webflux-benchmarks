#!/usr/bin/env python3

import sys
import csv
import matplotlib.pyplot as plt
from datetime import datetime
from matplotlib.ticker import ScalarFormatter

def plot_chart(input_file, output_file):
    timestamps = []
    user_cpu = []
    system_cpu = []
    iowait_cpu = []
    mem_used = []
    tcpsck = []
    active_s = []
    passive_s = []
    iseg_s = []
    oseg_s = []

    with open(input_file, 'r') as file:
        csv_reader = csv.DictReader(file, delimiter=';')
        for row in csv_reader:
            timestamps.append(datetime.utcfromtimestamp(int(row['timestamp'])))
            user_cpu.append(float(row['%user']))
            system_cpu.append(float(row['%system']))
            iowait_cpu.append(float(row['%iowait']))
            mem_used.append(int(round(float(row['kbmemused']) / 1024, 0)))
            tcpsck.append(float(row['tcpsck']))
            active_s.append(float(row['active/s']))
            passive_s.append(float(row['passive/s']))
            iseg_s.append(float(row['iseg/s']))
            oseg_s.append(float(row['oseg/s']))

    seconds_elapsed = [(timestamp - timestamps[0]).total_seconds() for timestamp in timestamps]

    # plt.style.use('seaborn-v0_8-darkgrid')
    fig, (cpu, ram, socket, throughput) = plt.subplots(4, 1, figsize=(20, 15), sharex=True)
    plots = (cpu, ram, socket, throughput)

    cpu.plot(seconds_elapsed, user_cpu, label='User', linestyle='solid', linewidth=2)
    cpu.plot(seconds_elapsed, system_cpu, label='System', linestyle='dashed', linewidth=2)
    cpu.plot(seconds_elapsed, iowait_cpu, label='IO Wait', linestyle='dotted', linewidth=2)
    cpu.ticklabel_format(style='plain')
    cpu.set_ylabel('CPU %', color='black')
    cpu.legend(loc='upper left')
    cpu.set_title('CPU', weight='bold')

    ram.plot(seconds_elapsed, mem_used, label='Used', linestyle='solid', linewidth=2)
    ram.set_ylabel('RAM (MiB)', color='black')
    ram.legend(loc='upper left')
    ram.set_title('RAM', weight='bold')

    socket.plot(seconds_elapsed, tcpsck, label='TCP', linestyle='solid', linewidth=2)
    socket.plot(seconds_elapsed, active_s, label='TCP Active Opens / s', linestyle='dotted', linewidth=2)
    socket.plot(seconds_elapsed, passive_s, label='TCP Passive Opens / s', linestyle='dashed', linewidth=2)
    socket.set_ylabel('Sockets', color='black')
    socket.legend(loc='upper left')
    socket.set_title('Sockets', weight='bold')

    throughput.plot(seconds_elapsed, iseg_s, label='Received / s', color='tab:gray', linestyle='solid', linewidth=2)
    throughput.plot(seconds_elapsed, oseg_s, label='Sent / s', color='tab:olive', linestyle='dotted', linewidth=2)
    throughput.set_ylabel('Segment Count', color='black')
    throughput.legend(loc='upper left')
    throughput.set_title('Throughput', weight='bold')
    throughput.set_xlabel('Seconds')

    for plot in plots:
        plot.yaxis.set_major_formatter(ScalarFormatter(useMathText=False))
        plot.xaxis.set_major_formatter(ScalarFormatter(useMathText=False))
        plot.ticklabel_format(style='plain')
        plot.grid(True, linestyle=':', linewidth=0.5)

    plt.savefig(output_file, format='png', bbox_inches='tight')

if __name__ == "__main__":
    if len(sys.argv) != 4:
        print("Syntax: system-chart.py <title> <input csv filename> <output png filename>")
    else:
        title = sys.argv[1]
        input_file = sys.argv[2]
        output_file = sys.argv[3]
        plot_chart(input_file, output_file)
        print("Updated " + output_file)
