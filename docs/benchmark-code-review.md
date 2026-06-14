# Adversarial Benchmark Code Review

**Scope:** Full code base (Python result processing, Bash orchestration/measurement, Java/Kotlin
workload, k6 load scripts).
**Lens:** Scientific validity of the published Loom-vs-WebFlux benchmark numbers — bugs, edge
conditions, and incorrect statistical calculations that would not withstand peer review.
**Date:** 2026-06-14

Findings are ordered by descending priority. Each lists the location, why it is wrong, the
observable impact, and the fix. **All P1–P10 are now fixed except P6, which is an intentional,
defensible design choice that is documented and clarified in code.**

Verification: `pytest` (31 tests, was 13) and the Gradle/Kotlin suite (153 tests, 0 failures) both
green. Golden `results.csv` fixtures were regenerated and **hand-verified from first principles**
(not circularly), and the realistic `loom-netty` fixture is unchanged by P4/P7/P8/P10.

---

## P1 — Critical: GC count and GC time are sums of *cumulative* counters

**Location:** `src/main/python/scenario_chart.py`, `append_results()`

```python
'garbage_collection_count':        sum(jvm_metrics.gc_counts),
'garbage_collection_time_millis':  sum(jvm_metrics.gc_times),
```

`gcCount` and `gcTime` (`MetricCsvLogger.java`) come from
`GarbageCollectorMXBean.getCollectionCount()` / `getCollectionTime()`, which are **cumulative
counters since JVM start**. They increase monotonically and are sampled ~once per second. Summing
the per-sample cumulative values produces a number with no physical meaning that scales with the
**number of samples** — i.e. with test duration — not with the GC activity during the test.

**Evidence (the test fixtures themselves enshrined the bug):**

| Fixture | Samples | Counter range during window | Reported `gc_count` | Reported `gc_time_millis` |
|---|---|---|---|---|
| `loom-netty` | 6 | 8 → 8 (0 GCs, 0 ms) | **48** (= 8×6) | **102** (= 17×6) |
| `missing-latencies` | 183 | 432 → 468 (36 GCs), 9657 → 10254 ms (597 ms) | **82076** | **1822234** |

The large case is wrong by **~2000×** (count) and **~3000×** (time).

**Impact:** Both columns are in `results.csv`, which `results_chart.py` uses to decide the *winning
approach* per metric (`garbage` is "lower is better"). Every GC comparison in the published results
was based on a meaningless quantity, contaminating the win/loss tallies.

**Fix:** Report the change across the measured window, `gc_counts[-1] - gc_counts[0]` (helper
`cumulative_window_delta`). The JVM metrics CSV is written fresh for the test phase, so the window
delta is the GC activity during the test (excluding startup/warmup). The chart title now uses the
same delta so the PNG and CSV agree (they previously disagreed: the title used the raw cumulative
`last`, the CSV used the `sum`).

---

## P2 — High: per-second GC% assumes a fixed 1000 ms sampling interval

**Location:** `src/main/python/scenario_chart.py`, `JvmMetrics`

```python
gc_time_diff = [self.gc_times[i] - self.gc_times[i - 1] for i in range(1, len(self.gc_times))]
self.gc_times_percentage = [(time_diff / 1000) * 100 for time_diff in gc_time_diff]
```

The "% of wall-clock spent in GC" divided the GC-time delta by a hard-coded `1000` ms. The sampler
is `@Scheduled(fixedRate = 1000)` on a single-threaded scheduler, so **under load the real interval
drifts above 1 s**. When the interval is 2 s, the code still divided by 1 s and reported **double**
the true GC% — and the error is correlated with the high-load condition under study.

**Impact:** Overstated GC% spikes on the CPU chart precisely when the system is most stressed
(chart-only; does not feed `results.csv`).

**Fix:** `gc_time_percentages()` divides by the *actual* elapsed time between consecutive JVM samples.

---

## P3 — High: network-per-request is a biased mean-of-ratios and is time-misaligned

**Location:** `src/main/python/scenario_chart.py`, `SystemMetrics` + `append_results()`

Two independent statistical defects in the original `mean(kb_s[i] / rps[i])`:

1. **Biased aggregation.** The unbiased estimator of bytes-per-request is `Σ kb / Σ requests`. A
   mean-of-ratios over-weights low-traffic seconds, and `divide0` mapped every `rps == 0` second
   (ramp/idle) to `0`, which was then averaged in, dragging the mean **down**; the `max` was
   symmetrically inflated by a single low-request second.
2. **Time misalignment.** `kb_s[i]` (`sar`) and `rps[i]` (k6) were paired by **list index** despite
   different time origins (`sar` started 2 s after k6), giving a ~2 s offset.

**Impact:** `network_kib_per_req_*` and `network_packets_per_req_*` in `results.csv` were
systematically distorted, and they feed the win/loss matrix.

**Fix:** `per_request_stats()` aligns requests to absolute wall-clock seconds (via
`LatencyMetrics.requests_by_second`) and reports the aggregate ratio over seconds that actually
served requests. Hand-verified: loom-netty `30.24 KiB / 38 req = 0.80`, matching the code.

---

## P4 — Medium: RPS histogram range uses the last row instead of the maximum timestamp

**Location:** `src/main/python/scenario_chart.py`, `_calculate_rps()`

k6's CSV is written in completion order with concurrent VUs, so it is only approximately time-sorted.
`np.histogram` silently discards samples outside `range`, and the code sized the range from
`seconds_elapsed[-1]`, so any request whose timestamp exceeded the final row's was dropped from RPS.

**Fix:** size the histogram from `max(seconds_elapsed)`.

---

## P5 — Medium: `results_chart.py` crashes on an empty numeric cell

**Location:** `src/main/python/results_chart.py`, `sort_approaches()` / `get_color_rows()`

`get_color_rows()` parsed `int(row.get("requests_error", 0))` without the `or 0` guard that
`sort_approaches()` had, so an empty `requests_error` crashed `int("")`. Writing the regression test
also surfaced that the unguarded `float(row[metric])` in **both** methods crashes on any empty metric
cell (e.g. `requests_error` itself, which is a metric column).

**Fix:** centralised tolerant parsing in `metric_value()` (empty/missing → `0.0`) and `has_errors()`
(empty/missing → no error), used in both code paths.

---

## P6 — Low (intentional, documented): error count vs. "submitted to server" filter

**Location:** `src/main/python/scenario_chart.py`, `_parse_csv()`

The latency/throughput percentiles exclude failed requests with latency `< 10 ms` ("most likely
never submitted to server"), but the error count/scatter include all non-2xx responses, so
`requests_ok + requests_error ≠ requests_requested`.

This is **two intentional, defensible choices**, not a bug — kept, with a clarifying code comment:
excluding ~0 ms client failures avoids artificially lowering the latency distribution, while counting
*all* non-2xx as errors avoids hiding a server that has fallen over and is refusing connections
(status 0, ~0 ms). Silently dropping those would be the more dangerous error.

---

## P7 — Medium: `sar` window does not match the k6 window

**Location:** `src/main/bash/benchmark-scenario.sh` (`load_and_measure_system`) + `scenario_chart.py`

`sar` was started `sleep 2` after k6 and ran for `durationInSeconds` samples, covering
`[t0+2s, t0+2s+duration]`: it missed the first ~2 s of load and included ~2 s of post-test idle, so
CPU/RAM/socket averages were taken over a window shifted from the load window.

**Fix (two parts):**
1. **Bash:** start `sar` together with k6 (removed the 2 s lead) so the start of load is captured.
2. **Python:** `SystemMetrics._clip_to_load_window()` clips all system series to the actual load
   window `[first request second, last request second]` from the k6 latency timestamps before
   aggregating, trimming pre-load and post-test idle. Falls back to the full series if the window is
   unknown or non-overlapping. Hand-verified on `missing-latencies`: 300 `sar` rows → 173 in-window,
   `cpu_use_percent_avg` `60.3 → 63.3`. The realistic `loom-netty` fixture is unchanged (its `sar`
   window already sat inside the load window).

---

## P8 — Low: per-second latency buckets misalign when there are idle seconds

**Location:** `src/main/python/scenario_chart.py`, `_calculate_latency_1s_buckets_and_errors()`

The bucket boundary advanced by one second per *flush*, not per *elapsed second*, so an idle second
(low RPS or a stall) compressed/shifted the percentile-over-time lines on the x-axis (chart-only).

**Fix:** `latency_percentile_buckets()` emits one bucket per elapsed second from `0..floor(max)`,
including idle seconds (value 0), keeping the x-axis aligned to wall-clock time. Does not affect
`results.csv` (latency percentiles there are computed over the full sample, not the buckets).

---

## P9 — Low: reactive recursion added a scheduler hop with no non-reactive equivalent

**Location:** `src/main/java/uk/gleissner/loomwebflux/common/AbstractService.java`

For `delayCallDepth > 0` the reactive path performed a `Mono.delay(0)` (a `parallel`-scheduler hop)
at each level, while the blocking path recursed directly with no equivalent — a structural asymmetry
in the deep-call-stack comparison.

**Fix:** mirror the blocking structure — only the leaf (depth 0) introduces the delay; intermediate
levels recurse straight into the next call. Each model still uses its idiomatic wait primitive at the
leaf (`Mono.delay` vs `Thread.sleep`), which is the intended comparison. Covered by the existing 153
integration/unit tests (all green).

---

## P10 — Low: cosmetic / latent edge cases

- `is_ok(...)` redundant boolean term simplified to `is_ok(...) or latency >= 10` (`scenario_chart.py`).
- `platform_threads_avg` / `sockets_avg` now `int(round(...))` instead of truncating (`int(...)`).
- k6 `get-post-movies-*.js` / `get-movies-stepped-vus-spike.js`: fractional VU stage targets wrapped
  in `Math.floor(...)` so a non-multiple-of-N `VUS` cannot yield a fractional `target`.
- k6 `get-time.js` / `get-movies.js`: `rps` is now only set when a positive RPS is configured
  (`...(rps ? {rps} : {})`), so an empty `RPS` means "no cap" instead of passing `rps: ""`.

---

## P11 — Medium: chart panels started at inconsistent x-positions

**Location:** `src/main/python/scenario_chart.py`, `create_plot_png_file` / `_add_request_plots` /
`_add_system_plots`

On a short run the panels did not line up: latency showed ~5 s, RPS ~6 s, and
CPU/heap/sockets/throughput ~4 s, with the `sar`-based panels starting seconds into the chart. Two
causes: (1) P7's earlier clipping shortened the system series; (2) the panels did not share a
consistent left edge. An intermediate attempt to plot everything against a single absolute origin
made it worse — the JVM metric logger runs from application startup, so on runs without a warmup its
CSV begins seconds before the first request, which pushed the shared origin earlier than the load and
left the `sar` panels starting late.

**Requirement (from the user):** every panel should begin at `x = 0`, drawn from its own first *real*
sample — the `sar` curves starting at 0 exactly like the JVM heap — with no invented data points.

**Fix:** each series is restricted to the load window and normalised to **its own first in-window
sample**, so every panel starts at 0 (`plot.set_xlim(left=0)` pins the axis) while dropping the JVM's
pre-load startup samples and any trailing samples. `results.csv` is byte-identical (the windowed
aggregates are unchanged; this is a plotting change). Verified on a synthetic run where the JVM logs
3 s of startup before load: all six panels start at `0.00 s` and the JVM heap no longer shows the
pre-load ramp.

**Extended `sar` (follow-up):** `sar` timestamps each sample at the END of its 1 s interval, so an
N-second capture only yields samples up to elapsed second N-1, and it stopped at the nominal test
duration while requests kept completing through k6's graceful stop — leaving the CPU/RAM/socket/
throughput panels ~1-2 s short of the latency/RPS panels. `benchmark-scenario.sh` now runs `sar` for
`testDurationInSeconds + systemMeasureTrailingSeconds` (3 s) so it has samples covering the whole load
window including the wind-down; the analysis clips those trailing samples back to the load window so
they never reach the chart or the aggregates. (`load_and_measure_system` also removes any stale system
CSV before each phase and waits for the longer capture to finish; the obsolete kill-on-overrun was
removed.) Verified on a synthetic 5 s test with stragglers: the system panels now reach the load
window instead of stopping ~2 s early, still starting at 0. A residual sub-second difference at the
right edge remains and is inherent — `sar`'s first sample (shown at x=0) actually covers the first
second, which shifts it ~1 s left of the JVM/latency series; closing it fully would require either an
invented sample or starting the sar curve at x=1 (conflicting with "start at 0").

---

## Summary

| ID | Severity | Area | Feeds `results.csv` / win-loss? | Status |
|----|----------|------|--------------------------------|--------|
| P1 | Critical | GC count/time = sum of cumulative | Yes | Fixed + tests |
| P2 | High | GC% fixed-interval assumption | No (chart) | Fixed + tests |
| P3 | High | Network-per-request bias + misalignment | Yes | Fixed + tests |
| P4 | Medium | RPS histogram range | Indirectly | Fixed + tests |
| P5 | Medium | Empty numeric cell crash | Chart gen | Fixed + tests |
| P7 | Medium | `sar` window offset | CPU/RAM/socket aggregates | Fixed + tests |
| P11 | Medium | Chart panels not on a common time axis | No (chart) | Fixed + tests |
| P6 | Low | Error vs. latency filter | n/a | Intentional — documented |
| P8 | Low | Latency bucket gaps | No (chart) | Fixed + tests |
| P9 | Low | Reactive scheduler hop asymmetry | Workload | Fixed (153 ITs green) |
| P10 | Low | Cosmetic / latent edge cases | No | Fixed + verified |
