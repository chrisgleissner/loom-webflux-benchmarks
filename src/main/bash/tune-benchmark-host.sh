#!/bin/bash
# Applies Linux host tuning required for high-connection benchmark runs.

set -euo pipefail

SYSCTL_SETTINGS=(
  "net.ipv4.ip_local_port_range=1024 65535"
  "net.ipv4.tcp_timestamps=1"
  "net.ipv4.tcp_tw_reuse=2"
  "net.ipv4.tcp_max_syn_backlog=65535"
  "net.core.somaxconn=65535"
  "fs.file-max=1048576"
)

mode=ensure

usage() {
  cat <<EOF
Usage: $(basename "$0") [--check|--apply]
Checks and applies Linux host tuning required for high-connection benchmark runs.

OPTION:
  --check  Print current values and fail if any benchmark setting differs.
  --apply  Apply benchmark settings and verify them.
EOF
}

normalize_value() {
  awk '{$1=$1; print}' <<< "$1"
}

current_sysctl_value() {
  local key=$1
  sysctl -n "$key" 2>/dev/null | awk '{$1=$1; print}'
}

print_current_values() {
  local setting key current

  echo "Current benchmark host sysctl values:"
  for setting in "${SYSCTL_SETTINGS[@]}"; do
    key=${setting%%=*}
    if current=$(current_sysctl_value "$key"); then
      printf '  %s = %s\n' "$key" "$current"
    else
      printf '  %s = unavailable\n' "$key"
    fi
  done
}

find_mismatches() {
  local setting key expected current
  MISMATCHES=()

  for setting in "${SYSCTL_SETTINGS[@]}"; do
    key=${setting%%=*}
    expected=$(normalize_value "${setting#*=}")
    if ! current=$(current_sysctl_value "$key"); then
      MISMATCHES+=("$key is unavailable; expected $expected")
    elif [[ "$current" != "$expected" ]]; then
      MISMATCHES+=("$key is $current; expected $expected")
    fi
  done
}

print_mismatches() {
  local mismatch

  echo "Benchmark host sysctl settings differ:"
  for mismatch in "${MISMATCHES[@]}"; do
    echo "  - $mismatch"
  done
}

apply_settings() {
  local setting key value

  echo "Applying Linux host tuning for benchmark"
  for setting in "${SYSCTL_SETTINGS[@]}"; do
    key=${setting%%=*}
    value=${setting#*=}
    if ! sysctl -w "$key=$value"; then
      echo "Failed to set $key. Check kernel support and sysctl permissions."
      exit 1
    fi
  done
}

apply_and_verify() {
  apply_settings
  find_mismatches
  print_current_values

  if (( ${#MISMATCHES[@]} > 0 )); then
    print_mismatches
    echo "Failed to apply benchmark host tuning. Check kernel support and sysctl permissions."
    exit 1
  fi
}

run_with_sudo() {
  local -a sudo_cmd

  if ! command -v sudo >/dev/null 2>&1; then
    echo "Benchmark host tuning requires sudo, but sudo is not available."
    exit 1
  fi

  if [[ "${CI:-}" == "true" ]]; then
    sudo_cmd=(sudo -n)
    echo "Benchmark host tuning requires sudo; using non-interactive sudo because CI=true."
  else
    sudo_cmd=(sudo)
    echo "Benchmark host tuning requires sudo. You may be prompted for your sudo password."
  fi

  if ! "${sudo_cmd[@]}" "$0" --apply; then
    echo "Failed to apply benchmark host tuning with sudo. In CI, preconfigure sysctl values or provide non-interactive sudo."
    exit 1
  fi
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --check)
      mode=check
      shift
      ;;
    --apply)
      mode=apply
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Invalid option: $1"
      usage
      exit 1
      ;;
  esac
done

case "$mode" in
  check)
    find_mismatches
    print_current_values
    if (( ${#MISMATCHES[@]} > 0 )); then
      print_mismatches
      exit 1
    fi
    ;;
  apply)
    if (( EUID != 0 )); then
      run_with_sudo
    else
      apply_and_verify
    fi
    ;;
  ensure)
    find_mismatches
    if (( ${#MISMATCHES[@]} == 0 )); then
      echo "Benchmark host sysctl settings are already applied."
      print_current_values
    elif (( EUID == 0 )); then
      print_mismatches
      apply_and_verify
    else
      print_mismatches
      run_with_sudo
    fi
    ;;
esac
