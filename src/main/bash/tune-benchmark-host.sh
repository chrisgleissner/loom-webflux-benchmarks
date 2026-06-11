#!/bin/bash
# Applies Linux host tuning required for high-connection benchmark runs.

set -euo pipefail

run_privileged() {
  if (( EUID == 0 )); then
    "$@"
  else
    sudo "$@"
  fi
}

apply_sysctl() {
  local key=$1
  local value=$2

  run_privileged sysctl -w "$key=$value"
}

echo "Optimizing Linux host for benchmark"
apply_sysctl net.ipv4.ip_local_port_range "1024 65535"
apply_sysctl net.ipv4.tcp_tw_reuse 1
apply_sysctl net.ipv4.tcp_max_syn_backlog 65535
apply_sysctl net.core.somaxconn 65535
apply_sysctl fs.file-max 1048576

sysctl net.ipv4.ip_local_port_range \
  net.ipv4.tcp_tw_reuse \
  net.ipv4.tcp_max_syn_backlog \
  net.core.somaxconn \
  fs.file-max
