# PLANS.md — High-load Tomcat result investigation

Authoritative execution plan. Status legend: `[ ]` todo, `[~]` in progress, `[x]` done (evidence in WORKLOG.md).

## Goal
Explain, with measured evidence, why `loom-tomcat` fails the 40k fixed-RPS high-load
scenarios (≈no successful requests) yet passes the 60k VU-paced scenarios, and produce a
targeted, defensible `README.md` update. Not to make Tomcat "pass".

## Phases

1. [x] Repository and benchmark orientation
   - Branch `fix/update-deps`, working tree state, README/scenario/script inventory.
2. [x] Existing-results inspection
   - `results/scenarios-high-load/results.csv` + the 3.1 GB run log.
3. [x] k6 workload-shape analysis
   - 40k rows = `constant-vus` + global `rps` cap, NO `sleep()` (closed-loop hammer).
   - 60k rows = `ramping-vus` + `sleep(1–3s)`, NO rps cap (organic, paced).
4. [x] Tomcat configuration diff analysis
   - Committed branch (tuned) vs uncommitted working-tree (restrictive) `application.yaml`.
5. [x] Short 15s reproduction runs (loom-tomcat, restrictive config, current host).
6. [x] Targeted load-variation matrix (connections, RPS, keep-alive, delay, endpoint).
7. [x] Hypothesis classification (must explain BOTH the failing 40k and passing 60k).
8. [x] Justified fix: CONCLUSION = no per-contender config change is justified/defensible (would
   breach mostly-vanilla/level-playing-field). Only shared OS tuning applies (unverifiable here,
   no sudo). application.yaml left as-is per user. Find a MINIMAL, production-realistic change to
   lets Tomcat compete at 40k (user directive: treat Tomcat fairly WITHOUT compromising the
   benchmark). Leading candidate = documented host tuning (somaxconn/tcp_max_syn_backlog=65535
   via tune-benchmark-host.sh) which applies to ALL servers equally. Keep application.yaml
   working-tree values AS-IS (user decision). Test on tuned host whether Tomcat passes 40k.
9. [x] Validation: ALL scenarios x 3 approaches at 30s each (current host; sudo unavailable so
   host could not be tuned). Reproduces published pattern exactly (build/results, published
   results/ untouched).
10. [x] README update (targeted, preserve style).
11. [x] Final verification and summary.

## Phase 12 (probe mode, 2026-06-13) — DONE
- Found exact get-movies breaking point for loom-tomcat (benchmark/restrictive config, tuned host):
  works at 13k, breaks at 14k (~90% connection refused). Sharp cliff.
- Added 2 documenting scenarios to scenarios-high-load.csv: 13k-vus-and-rps-get-movies (passes),
  14k-vus-and-rps-get-movies (breaks).
- README: added "Where Tomcat Breaks, and How to Push It Further" — keep-alive recommendation
  (verified: 14k 89.5%->0%, threshold 13k->~18-19k) + downsides (no 20k/40k fix; higher idle
  resource use; comparability). Experiment via ephemeral env overrides; application.yaml & JVM
  unchanged (nothing to revert; verified).

## User decisions (2026-06-13)
- Host tuning: user provides sudo; I test somaxconn=65535 effect on Tomcat 40k.
- Validation: all scenarios, 30s each (not full 3-min, for now).
- application.yaml: KEEP working-tree change (accept-count 2000, keep-alive-timeout 1s,
  max-keep-alive-requests 1) — agreed baseline for fair competition. Do not revert.
- Explore minimal, defensible, production-realistic config to let Tomcat pass fairly.

## Key early findings (see WORKLOG for evidence)
- 100% of the 14,153,590 `connection refused` errors are `loom-tomcat`; 0 from Netty/WebFlux.
- Dominant failure class = `connection refused` (TCP accept-queue / backlog), NOT
  server-side processing timeouts. Minor: `connection reset by peer` (110k), `EOF` (9k).
- `loom-tomcat` 40k+delay: requests_ok≈22k–40k, requests_error≈6.9–7.0M, sockets_max≈40010.
- `loom-tomcat` 60k organic: requests_ok≈2.6–3.0M, requests_error=0, sockets_max≈2.9k–10k.
- `loom-tomcat` 40k **no-delay**: requests_ok≈7.26M, requests_error=0, sockets_max≈298 (PASSES).
- Existing failing results were produced with the **uncommitted working-tree** Tomcat config
  (`max-keep-alive-requests: 1`, `keep-alive-timeout: 1s`, `accept-count: 2000`) and an
  **under-tuned host** (`net.core.somaxconn=4096`, intended 65535 via tune-benchmark-host.sh).

## Constraints
Do not redesign benchmark; keep changes targeted; prefer measured evidence; keep diagnostic
runs short (~15s) until cause understood; do not tune Tomcat merely to pass; do not hide errors.
