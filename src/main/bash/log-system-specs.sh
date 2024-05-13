#!/bin/bash
# Logs system specs.

printf "Java:\t"
java --version | grep "Server"

printf "Python:\t"
python --version

printf "OS:\t"
cat /etc/os-release | grep "PRETTY"

printf "Kernel:\t"
uname -r

printf "CPU:\t"
lscpu | grep "Model name"

printf "Cores:\t"
cat /proc/cpuinfo | awk '/^processor/{print $3}' | wc -l
