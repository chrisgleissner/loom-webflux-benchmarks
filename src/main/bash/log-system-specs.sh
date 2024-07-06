#!/bin/bash
# Logs system specs.

printf "Java:\t"
java --version | grep "Server"

printf "Python:\t"
python3 --version | awk '{print $2}'

printf "OS:\t"
grep "PRETTY_NAME" /etc/os-release | cut -d '"' -f 2

printf "Kernel:\t"
uname -r

printf "CPU:\t"
lscpu | grep "Model name" | sed 's/Model name:\s*//'

printf "Cores:\t"
grep -c ^processor /proc/cpuinfo

printf "RAM:\t"
free -h | awk '/^Mem:/ {print $2 " total, " $7 " available"}'

printf "Disk:\t"
df -h --total | awk '/^total/ {print $2 " total, " $4 " available"}'
