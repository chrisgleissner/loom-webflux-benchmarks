# WORKLOG.md — High-load Tomcat investigation

All times BST (host) unless marked UTC. Commands, observations, hypotheses, conclusions.

---

## Phase 1–4: Orientation, existing results, workload shape, config diff (read-only)

### Repo state
- Branch: `fix/update-deps`. Committed tip `8685d28`.
- Uncommitted working-tree changes (besides `results/*.png`):
  `application.yaml`, `benchmark-scenario.sh`, `benchmark.sh`, `benchmarks.sh`,
  `generate-results-markdown.sh`, `scenario_chart.py`, `scenario_chart_test.py`.

### scenarios-high-load.csv
| scenario | k6 | delayMs | connections | rps | warmup | test |
|---|---|---|---|---|---|---|
| 40k-vus-and-rps-get-time-no-delay | get-time.js | 0 | 40000 | 40000 | 10 | 180 |
| 40k-vus-and-rps-get-time | get-time.js | 100 | 40000 | 40000 | 10 | 180 |
| 40k-vus-and-rps-get-movies | get-movies.js | 100 | 40000 | 40000 | 10 | 180 |
| 60k-vus-stepped-spike-get-movies | get-movies-stepped-vus-spike.js | 100 | 60000 | (empty) | 0 | 180 |
| 60k-vus-smooth-spike-get-movies | get-movies-smooth-vus-spike.js | 100 | 60000 | (empty) | 0 | 180 |
| 60k-vus-smooth-spike-get-post-movies | get-post-movies-smooth-vus-spike.js | 100 | 60000 | (empty) | 0 | 180 |

### k6 workload shape (CRITICAL — the two groups are NOT the same load shape)
- `get-time.js`/`get-movies.js` (the 40k rows): default executor = `constant-vus`
  (`vus`+`duration`), global `rps` cap, and the default function has **no `sleep()`**.
  → 40000 VUs hammer in a closed loop, capped at 40000 rps. With a 100 ms server delay each
  in-flight request holds its connection ~100 ms.
- `*-vus-spike.js` (the 60k rows): `ramping-vus` executor, **`sleep(1–3s)` between requests**,
  **no rps cap**. → throughput is organic; at any instant only a small fraction of the 60000
  VUs are in-flight (≈ request_time / (request_time+sleep) ≈ 100ms/2.1s ≈ 5% → ~3000 conns).

### Existing results (results/scenarios-high-load/results.csv), loom-tomcat rows
| scenario | requests_ok | requests_error | sockets_max | latency p50/p99 (ms) |
|---|---|---|---|---|
| 40k get-time-no-delay | 7,256,026 | 0 | 298 | 0.10 / 15.9 |
| 40k get-time (100ms) | 39,613 | 6,913,296 | 40,010 | 1702 / 7578 |
| 40k get-movies (100ms) | 22,728 | 7,011,007 | 40,009 | 3224 / 6779 |
| 60k stepped get-movies | 2,961,914 | 0 | 3,062 | 100 / 115 |
| 60k smooth get-movies | 2,601,573 | 0 | 2,934 | 100 / 108 |
| 60k smooth get-post-movies | 2,589,503 | 0 | 10,424 | 100 / 226 |
- loom-netty & webflux-netty: requests_error=0 on ALL six scenarios; in 60k rows they hold
  sockets_max≈60,009 (one persistent conn per VU) vs Tomcat's ≈3,000.

### Failure classification from the 3.1 GB run log
Command:
```
grep -aoE 'msg="Request Failed" error=.*$' results/benchmark-scenarios-high-load.log \
 | grep -oE '(connection reset by peer|connection refused|i/o timeout|EOF|...)' | sort | uniq -c
```
Result:
- `connection refused`        14,153,590  (98.2%)
- `read: connection reset by peer` 110,206 (0.76%)
- `EOF`                             9,065  (0.06%)
- All 14,153,590 `connection refused` lines target `localhost:8080/loom-tomcat/` (0 for netty).
Sample line: `... read tcp 127.0.0.1:17780->127.0.0.1:8080: read: connection reset by peer`
and `... dial tcp 127.0.0.1:8080: connect: connection refused` (accept-queue overflow).

### Tomcat config — committed branch vs uncommitted working tree
`server.tomcat` (working tree, USED for existing failing results — jar built 22:40 BST from
application.yaml edited 08:48 BST):
- accept-count: 2000
- keep-alive-timeout: 1s
- max-keep-alive-requests: 1   ← keep-alive effectively DISABLED (new TCP conn per request)
- max-connections / threads.max: 65000

`server.tomcat` (committed branch HEAD — the "Tune Tomcat connection handling" commit a79211b):
- accept-count: ${max-connections} = 65000
- connection-timeout: 20s
- keep-alive-timeout: ${response-timeout} = 60s
- max-keep-alive-requests: -1  ← keep-alive UNLIMITED (connections reused)
- max-connections / threads.max: 65000

### Host (OS) limits at investigation time
- ulimit -n = 1048576 (fine)
- net.core.somaxconn = 4096           (tune-benchmark-host.sh intends 65535 — host UNDER-tuned)
- net.ipv4.tcp_max_syn_backlog = 4096 (intended 65535)
- net.ipv4.tcp_abort_on_overflow = 0
- ip_local_port_range = 1024 65535 ; tcp_tw_reuse = 1
NOTE: effective Tomcat accept backlog = min(accept-count, somaxconn).
For the restrictive run that was min(2000, 4096) = 2000.

### Uncommitted harness/reporting changes (prior session) — relevant to interpretation
`benchmark-scenario.sh`: previously `exit 1` on ANY `http_req_failed` or k6 non-zero exit
(whole run aborted). Now it CONTINUES and records error-marked rows; adds `record_failed_result`
→ `scenario_chart.py --failed`. The failing Tomcat rows have REAL latency data + real PNGs, i.e.
they went through the normal chart path (not the zero-filled --failed path) — the errors are
genuine k6 samples, not a reporting artifact.

---

## Hypotheses

1. 40k rows are more demanding because they impose a fixed 40k-rps arrival with no client
   pacing, while 60k rows are organically paced (sleep 1–3s) so instantaneous load is far lower.
   STATUS: supported (results.csv sockets_max 40010 vs ~3000; 60k achieves only ~14k rps p50).
2. 60k "passes" because instantaneous in-flight connection count is ~3000, not 60000.
   STATUS: supported (Tomcat sockets_max≈3000 in 60k; Netty holds 60009 idle keep-alive socks).
3. Tomcat fails the fixed-arrival workload at the TCP accept layer (backlog/keep-alive), not in
   request processing. STATUS: strongly supported (98% `connection refused`; no-delay 40k passes).
4. Result is a reporting/interpretation bug. STATUS: weakened — errors are real k6 samples;
   harness now records them faithfully.
5. Current-branch Tomcat config change explains the difference. STATUS: open — the COMMITTED
   tuned config was NOT used for the existing results; a working-tree REVERT to a restrictive
   config (keep-alive off, accept-count 2000) was. Needs a controlled A/B (Phase 5–6).
6. Load generator / OS limits contribute. STATUS: open — somaxconn=4096 caps backlog; keep-alive
   off forces 40k new conns/s. Needs measurement.

---

## Phase 5–6: Short diagnostic runs + load-variation matrix

Harness: `build/diag/run-diag.sh LABEL CONFIG VUS RPS DELAY_MS K6SCRIPT DURATION [APPROACH]`.
Launches the existing tomcat/webflux jar with Tomcat settings injected via Spring
relaxed-binding env vars (no rebuild) and runs a short k6 test. Host UNCHANGED:
`net.core.somaxconn=4096`, `tcp_max_syn_backlog=4096` throughout (no sudo available).
CONFIG: `restrictive` = accept-count 2000 / keep-alive-timeout 1s / max-keep-alive-requests 1;
`tuned` = accept-count 65000 / keep-alive-timeout 60s / max-keep-alive-requests -1 / conn-timeout 20s.
All runs: get-time.js, delayInMillis=100, 15s, loom-tomcat unless noted.

| run | config | VUs | rps | http_req_failed | dominant error |
|---|---|---|---|---|---|
| r40k | restrictive | 40000 | 40000 | **88.96%** (383986/431632) | connection refused (357590) |
| t40k | tuned | 40000 | 40000 | **97.05%** (549687/566347) | connection refused (521769) |
| netty40k | webflux-netty (jar default) | 40000 | 40000 | **0.00%** (0/589599) | — (none) |
| s5000_r | restrictive | 5000 | 5000 | 0.00% (0/79529) | — |
| s5000_t | tuned | 5000 | 5000 | 0.00% (0/79505) | — |
| s10000_r | restrictive | 10000 | 10000 | 0.00% (0/159130) | — |
| s10000_t | tuned | 10000 | 10000 | 0.00% (0/159873) | — |
| s20000_r | restrictive | 20000 | 20000 | 0.00% (0/321431) | — |
| s20000_t | tuned | 20000 | 20000 | 0.00% (0/323562) | — |

Findings:
- Reproduced the catastrophic failure at 40k VUs (closed-loop, no-sleep, 100ms delay): mostly
  `connection refused` — identical class to the published run.
- **Tuned config did NOT rescue 40k** (97% failed). On this host `accept-count=65000` is capped
  by `somaxconn=4096`, and keep-alive=-1 didn't prevent the connection-establishment storm.
- **Keep-alive / accept-count config makes NO difference at 5k/10k/20k (all 0%) and does not
  change the 40k outcome.** => Hypothesis 5 (config change is the lever) DISPROVED on this host.
- **Sharp threshold between 20k and 40k simultaneously-establishing connections.** Below it
  Tomcat is clean; above it the acceptor cannot keep the (somaxconn-capped) accept queue drained.
- **CONTROL: webflux-netty handles the identical 40k burst on the identical host with 0 errors.**
  => The OS limit is NOT the primary cause (Netty hit the same OS and passed). The differentiator
  is Tomcat's connection-acceptor behaviour under a large simultaneous-connection burst vs
  Netty's event-loop acceptor. Hypothesis 6 (OS limit primary) DISPROVED; Hypothesis 3 confirmed.
- `log-system-specs.sh` records NO sysctl values, so the published run's somaxconn is unknown
  (tune-benchmark-host.sh sets 65535 at runtime only; not persisted across reboot).

## Phase 7: Hypothesis classification (final)
- H1 (workload shape: 40k fixed-rps no-sleep vs 60k paced): **ACCEPTED**. 40k = constant-vus,
  no sleep, all VUs hammer at once; 60k = ramping-vus + sleep(1-3s) => ~3000 concurrent.
- H2 (60k passes due to ~3000 instantaneous conns, not 60000): **ACCEPTED** (sockets_max≈3000).
- H3 (failure is at TCP accept layer, not request processing): **ACCEPTED** (98% connection
  refused; 40k no-delay & ≤20k & 60k all pass; Netty passes identical 40k burst).
- H4 (reporting bug): **DISPROVED** as a cause of false errors; errors are real k6 samples. The
  uncommitted harness change only stops the run from aborting and records the real failures.
- H5 (current-branch Tomcat config change is the lever): **DISPROVED** for the 40k outcome —
  neither restrictive nor tuned config changes pass/fail at 5k/10k/20k/40k on this host.
- H6 (OS limit / load generator primary): **DISPROVED** as primary — Netty passes the same 40k
  burst on the same host. somaxconn modulates Tomcat's threshold but is not the root cause.

ROOT CAUSE (best supported): The 40k scenarios are fixed-RPS, no-pacing closed-loop workloads
that make all ~40,000 k6 VUs open and drive connections simultaneously and continuously. Tomcat's
connection acceptor cannot absorb that simultaneous-connection burst (accept-queue overflow →
`connection refused`), so almost all requests fail. The 60k scenarios are VU-paced ramps with
`sleep(1-3s)`, so only ~3,000 connections are ever concurrent — far below the burst that breaks
Tomcat — which is why "more users" (60k) succeeds where "fewer users at fixed RPS" (40k) fails.
Netty's event-loop acceptor absorbs the same 40k burst with zero errors on the same host.

OPEN: whether raising the host accept queue (`somaxconn`/`tcp_max_syn_backlog` to 65535 via
tune-benchmark-host.sh, the documented setup) lifts Tomcat's threshold above 40k. Requires sudo.

---

## Phase 8: Threshold + mechanism (no sudo) and fairness analysis

### Mechanism (why ECONNREFUSED, given syncookies=1, abort_on_overflow=0)
On **loopback** a full listen backlog (= min(accept-count, somaxconn)) makes connect() return
ECONNREFUSED immediately instead of SYN retransmit. So the `connection refused` storm =
listen-backlog overflow because Tomcat's acceptor cannot drain the backlog as fast as the 40k
burst fills it. KEY: my `t40k` run already had effective backlog = min(65000, 4096) = 4096 AND
keep-alive on, yet failed 97%; Netty at the same effective backlog (≤4096) passed 0%. => the
differentiator is **acceptor drain rate**, not backlog depth or keep-alive.

### Tomcat simultaneous-connection ceiling (get-time, 100ms, 15s, somaxconn=4096)
| VUs | loom-tomcat http_req_failed |
|----:|---|
| 5,000 | 0% |
| 10,000 | 0% |
| 20,000 | 0% |
| 25,000 | **90.8%** (connection refused) |
| 40,000 | 89–97% (connection refused) |
| 40,000 (webflux-netty) | **0%** |
=> Tomcat's acceptor saturates between 20k and 25k simultaneous connections on this host; Netty
has no such ceiling at 40k. (30k/35k probes aborted on back-to-back port reuse — not needed.)

### Sudo unavailable
`sudo -n` denied for sysctl; cannot raise somaxconn. Host-tuning effect on Tomcat's ceiling
remains MEASURED-OPEN. Netty passing at somaxconn=4096 shows the OS is not the binding
constraint for a fast acceptor; a deeper queue (65535) would help Tomcat only insofar as it can
buffer the burst faster than the acceptor overflows — plausible but unverified here.

### Fairness / senior-engineer position
- Shared OS tuning (somaxconn/tcp_max_syn_backlog=65535 via tune-benchmark-host.sh) is fair: it
  applies to ALL contenders and reflects real high-connection production hosts. The published
  run used an UNDER-tuned host (somaxconn=4096), which the README should call out.
- Per-contender config stays near-vanilla (user's "mostly vanilla" principle). A huge
  Tomcat-only accept-count just to pass would be a non-vanilla favour and is NOT recommended.
- Therefore the defensible conclusion: Tomcat's 40k failure is a real, fairly-measured
  connection-burst acceptance limit of its threaded acceptor vs Netty's event loop — interpreted
  against workload shape, not hidden or engineered away.

## Phase 9: 30s all-scenario validation (in progress)
Temp file `src/main/resources/scenarios/scenarios-high-load-30s.csv` (warmup 0, test 30, all 6
scenarios). Cmd: `./src/main/bash/benchmark.sh -a loom-tomcat,loom-netty,webflux-netty -C
scenarios-high-load-30s.csv` -> build/results (NOT overwriting published results/). Log:
build/diag/validation-30s.log. Config = working-tree (restrictive) per user instruction.

### 30s validation RESULTS (build/results/results.csv; published results/ untouched). 882s.
| scenario | loom-tomcat | loom-netty | webflux-netty |
|---|---|---|---|
| 40k get-time no-delay | ok 1,194,809 / err 0 | err 0 | err 0 |
| 40k get-time (100ms) | ok 28,860 / **err 1,069,435** | err 0 | err 0 |
| 40k get-movies (100ms) | ok 14,809 / **err 1,047,212** | err 0 | err 0 |
| 60k stepped get-movies | ok 517,532 / err 0 | err 0 | err 0 |
| 60k smooth get-movies | ok 460,435 / err 0 | err 0 | err 0 |
| 60k smooth get-post | ok 460,426 / err 0 | err 0 | err 0 |
sockets_max: failing 40k-tomcat ≈5.5k; **passing 60k-tomcat ≈1.5-1.8k** (paced); netty 34-42k.
Error histogram (validation log): connection refused 2,048,059 (ALL loom-tomcat),
connection reset by peer 57,219, EOF 11,369. => Reproduces the published pattern EXACTLY through
the real harness at 30s. CONCLUSION VALIDATED.

## Phase 10: README update — DONE
- `#### High-Load Scenarios`: clarified the two load shapes (40k fixed-rate closed-loop vs 60k
  paced ramp).
- `## High Load Results` > new `### Reading the 40k and 60k Results`: workload-shape explanation,
  connection-acceptance (not processing) failure, Netty control, fairness/OS-tuning note.
- TL;DR Tomcat bullet: added one precise qualifier (passes paced 60k; fails 40k connection burst
  with `connection refused`). No other claims altered; historical comparisons preserved.

## Phase 11: Verification & cleanup — DONE
- Temp file `scenarios-high-load-30s.csv` removed. Port free, no stray java.
- application.yaml LEFT AS-IS (working-tree values) per user instruction — no code/config change.
- Only tracked source change = README.md. build/diag (harness, logs) is gitignored.

## Phase 8b: OPEN LEVER CLOSED — tuned host A/B/C (sudo applied by user 2026-06-13)
Host now: net.core.somaxconn=65535, net.ipv4.tcp_max_syn_backlog=65535 (verified via sysctl).
40k VUs, get-time.js, 100ms, 15s:
| run | config | http_req_failed | dominant error |
|---|---|---|---|
| A tuned_A_restrictive | accept-count 2000, keep-alive off | **93.35%** (459371/492065) | connection refused 422830 |
| B tuned_B_tuned | accept-count 65000, keep-alive -1, 65k threads | **97.16%** (494232/508651) | connection refused 445973 |
| C tuned_C_netty | webflux-netty (jar defaults) | **0.00%** (0/584924) | none |

CONCLUSION: Raising the OS accept queue to 65535 AND Tomcat accept-count to 65000 AND enabling
unlimited keep-alive does NOT make Tomcat pass the 40k connection burst (still 97% refused).
=> The 40k failure is NOT an artifact of an under-tuned host or of accept-queue depth; it is a
genuine limit of Tomcat's connection-acceptor THROUGHPUT under a ~40,000 simultaneous-connection
burst, vs Netty's event-loop acceptor (0% on the same host). Host tuning remains the correct
shared baseline (benefits all contenders) but does not change this outcome. The measured-open
item is now CLOSED. (Note: catastrophically-failing 40k runs are slow to release port 8080 on
shutdown; back-to-back diagnostic runs need spacing — observed during A/B/C.)

## Phase 12: Probe mode — exact get-movies breaking point (tuned host, somaxconn=65535)
get-movies.js, delayInMillis=100, 20s, VUs=RPS, loom-tomcat. (Bimodal: ~0% or ~90%+; sharp cliff.)

RESTRICTIVE config (= benchmark's working-tree application.yaml: accept-count 2000,
keep-alive-timeout 1s, max-keep-alive-requests 1):
| VUs | http_req_failed |
|----:|---|
| 5,000 | 0% |
| 10,000 | 0% |
| 13,000 | 0% (272,354 reqs, 0 err) — confirmed twice |
| **14,000** | **89.54%** (251,322/280,654; connection refused) |
| 16,000 | 92.3% |
| 20,000 | 96.1% |
=> BREAKING POINT for get-movies = between 13k (works) and 14k (breaks).

TUNED config (keep-alive on: max-keep-alive-requests -1, keep-alive-timeout 60s, accept-count 65000):
| VUs | http_req_failed |
|----:|---|
| 14,000 | **0%** (293,897 reqs) — rescued vs restrictive's 89.5% |
| 18,000 | 0% (366,275 reqs) |
| 20,000 | 99.71% (connection refused) |
=> keep-alive raises the threshold ~13k -> ~18-19k AND rescues the 14k breaking scenario, but
does NOT pass 20k+; 40k still fails 97% (Phase 8b). Confirms acceptor-throughput ceiling.

RECOMMENDATION VERIFIED EXPERIMENTALLY via ephemeral Spring env-var overrides in build/diag
(no application.yaml / JVM-flag edits in the repo) -> nothing to revert; application.yaml stays
as the user's working-tree baseline.

### Documenting scenarios (amended per user 2026-06-13: round numbers, easier/defensible)
Replaced the precise 13k/14k pair with a round-number get-movies fixed-rate ladder in
scenarios-high-load.csv: 10k (passes, 0%), 20k (breaks, ~96%), 40k (breaks, ~97%), plus the
existing 60k paced scenarios (pass). Data backing: 10k=0%, 20k=96.09% (restrictive). Breaking
point bracketed between 10k and 20k (sharp cliff near ~13k). README "Where Tomcat Breaks"
section updated to this ladder; keep-alive claim kept honest (raises threshold ~13k->~18-19k,
still below 20k, so 20k/40k still fail).

## Phase 13: Config reconciliation (local vs README vs CI) + scenario polish (2026-06-13)

### Local host config vs README/CI
- README "Linux Optimizations" + CI (reusable-build.yaml runs `sudo tune-benchmark-host.sh`) +
  tune-benchmark-host.sh all specify the SAME 5 sysctls: ip_local_port_range "1024 65535",
  tcp_tw_reuse 1, tcp_max_syn_backlog 65535, somaxconn 65535, fs.file-max 1048576 (+ nofile
  1048576 for local persistent setup; CI relies on runner default + script).
- LOCAL runtime now: all 5 == documented values, ulimit -n 1048576. MATCHES exactly.
- LOCAL persisted /etc/sysctl.conf: has port_range + tw_reuse (correct) but MISSING somaxconn,
  syn_backlog, file-max; EXTRA: vm.swappiness=10, fs.inotify.max_user_watches=524288.
  -> swappiness/inotify are the user's general OS settings, unrelated to TCP accept / Tomcat /
     the 10k result; not benchmark config; left untouched (removal needs sudo + risks IDE/system).
  -> after REBOOT, runtime somaxconn/syn_backlog revert to defaults (4096) unless the user re-runs
     tune-benchmark-host.sh. benchmark.sh does NOT auto-run it (only CI does). ACTION FOR USER:
     run `sudo ./src/main/bash/tune-benchmark-host.sh` after restart, before the benchmark.
- Is host tuning NEEDED for 10k to pass? NO. accept-count=2000 caps the listen backlog at
  min(2000, somaxconn)=2000 whether somaxconn is 4096 or 65535, so 10k is identical either way
  (and 10k passes). Host tuning is the documented shared baseline (matches CI) and matters for
  consistency/defensibility, not for the 10k outcome.
- application.yaml: working-tree (restrictive: accept-count 2000, keep-alive 1/1s) == README prose,
  but DIFFERS from committed HEAD (tuned: 65000/-1/60s) which CI checks out. This is the only
  benchmark-relevant local-vs-CI divergence. It does NOT change the ladder pass/break pattern
  (10k pass, 20k/40k/60k break under BOTH configs; verified). To make local==CI==README, COMMIT
  the working-tree application.yaml (user's git decision; not reverted since user wants it kept).
- Nothing I introduced on the host persists (tuning was runtime via the documented script);
  nothing to revert.

### 60k fixed-rate variant + ascending progression
- Measured 60k-vus-and-rps-get-movies (restrictive): 95.69% failed (815,279/851,950 conn refused).
- Added 60k-vus-and-rps-get-movies to complete the fixed-rate ladder (10k/20k/40k/60k), making the
  paced 60k-vus-*-spike scenarios clearly a SEPARATE type, not a continuation.
- Reordered scenarios-high-load.csv to a monotonic ascending-load progression
  (connections: 10k,20k,40k,40k,40k,60k,60k,60k,60k).
- README "Where Tomcat Breaks" table updated: fixed-rate ladder 10k(pass)/20k(~96%)/40k(~97%)/
  60k(~96%), with the paced 60k-*-spike scenarios called out separately as passing. Table links
  point to results.md#<scenario> anchors (populated by the next full run).

## Phase 14: Final high-load scenario design — 12 scenarios (user-approved 2026-06-13)
Full 24-scenario grid (every type x 10/20/40/60k) = ~4.8h => runtime explosion. Chosen
"happy medium" (~2.4h) on the principle "sweep the full ladder only where a threshold exists;
anchor where it doesn't":
- get-time (delay 100) fixed-rate ladder: 10/20/40/60k  (light endpoint, threshold ~20-25k)
- get-movies (delay 100) fixed-rate ladder: 10/20/40/60k (heavy endpoint, threshold ~13k)
- get-time-no-delay fixed-rate: 60k only (control: no-delay passes even at 60k)
- paced spikes at 60k: stepped-get-movies, smooth-get-movies, smooth-get-post-movies
Order: get-time ladder -> no-delay control -> get-movies ladder -> paced trio (logical story).
README reframed from "40k vs 60k rows" to "fixed-rate vs paced shape" (the old dichotomy broke
once fixed-rate spanned 10-60k and the no-delay control moved to 60k); fixed "all three 60k
scenarios pass" -> only the paced 60k + no-delay pass (60k fixed-rate get-time/get-movies FAIL).
Test duration kept at 180s for comparability with historical runs.

### Final answer to "can a defensible change make Tomcat pass 40k?"
On this host, within mostly-vanilla per-contender config + somaxconn=4096, NO config (keep-alive,
accept-count up to the somaxconn cap) makes Tomcat pass 40k — the limit is acceptor drain rate,
proven by Netty passing the identical burst at the same effective backlog. The only remaining
lever is the shared OS tuning (somaxconn/tcp_max_syn_backlog=65535, benefits all contenders),
whose effect on Tomcat's 40k ceiling is MEASURED-OPEN here (no sudo). Recommendation: keep the
benchmark as-is and interpret the 40k Tomcat result as a fairly-measured connection-burst
acceptance limit; ensure high-connection runs apply the documented host tuning. A Tomcat-only
accept-count blow-up purely to pass would breach the "mostly vanilla / level playing field"
principle and is not recommended.

---
