# Benchmark of Java Virtual Threads vs WebFlux

[![build](https://github.com/chrisgleissner/loom-webflux-benchmarks/actions/workflows/build.yaml/badge.svg)](https://github.com/chrisgleissner/loom-webflux-benchmarks/actions)
[![benchmark](https://github.com/chrisgleissner/loom-webflux-benchmarks/actions/workflows/benchmark.yaml/badge.svg)](https://github.com/chrisgleissner/loom-webflux-benchmarks/actions/workflows/benchmark.yaml)
[![soaktest](https://github.com/chrisgleissner/loom-webflux-benchmarks/actions/workflows/soaktest.yaml/badge.svg)](https://github.com/chrisgleissner/loom-webflux-benchmarks/actions/workflows/soaktest.yaml)
[![Coverage Status](https://coveralls.io/repos/github/chrisgleissner/loom-webflux-benchmarks/badge.svg)](https://coveralls.io/github/chrisgleissner/loom-webflux-benchmarks)

This Java project benchmarks a simple [Spring Boot 4.1](https://spring.io/projects/spring-boot) microservice using
configurable scenarios, comparing Java Virtual Threads (introduced by [Project Loom, JEP 444](https://openjdk.org/jeps/444)) using Tomcat and Netty
with [Spring WebFlux](https://docs.spring.io/spring-framework/reference/web/webflux.html) (relying on [Project Reactor](https://projectreactor.io/)) using Netty.

All benchmark results below come from a dedicated bare metal Ubuntu 24.04 test environment using Java 25 unless specified otherwise. The benchmark also runs monthly on GitHub-hosted runners, using [all combinations](./results/ci/ci.md) of (Ubuntu 22.04, Ubuntu 24.04) and (Java 21, Java 25).

## Background

Both Spring WebFlux and Virtual Threads are alternative technologies to create Java microservices that support a high
number of concurrent users, mapping all incoming requests to very few shared operating system threads. This reduces the
resource overhead incurred by dedicating a single operating system thread to each user.

Spring WebFlux was first introduced in September 2017. Virtual Threads were first introduced as preview feature with
Java 19 and were fully rolled out with Java 21 in September 2023.

## TL;DR

> [!NOTE]
> Headline findings from the latest run (Amazon Corretto 25.0.3, bare metal, June 2026; raw data in [results/](results/)):
>
> **Virtual Threads on Netty** (using [blocking code](https://docs.oracle.com/javase/8/docs/api/java/lang/Thread.html#sleep-long-)) matched or beat **WebFlux on Netty** (using non-blocking code relying on [Mono](https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Mono.html)
> and [Flux](https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Flux.html) from Project Reactor) across most - but not all - scenarios and metrics:
> - Counting every per-metric, per-scenario contest between the two Netty approaches on the [default scenarios](#netty-based-approaches), Virtual Threads on Netty won **~50%**, WebFlux on Netty won **~26%**, and **~25%** had no clear winner.
> - Adding Virtual Threads on Tomcat back into the comparison, the [overall win-count ranking](#all-approaches) on the default scenarios is Virtual Threads on Netty (**38%** of contests), Virtual Threads on Tomcat (**22%**), WebFlux on Netty (**17%**), with 22% undecided.
> - In the heaviest [fixed-rate `get-movies` scenarios](#high-load-results) (40k and 60k simultaneous connections), Virtual Threads on Netty held a flat ~100&nbsp;ms median latency, while WebFlux on Netty degraded sharply at 60k (median ~1.2&nbsp;s, P99 ~3.3&nbsp;s, ~99% heap, ~108&nbsp;s of GC time) and served ~31% fewer requests.
> - The advantage is **not** universal. In the lighter fixed-rate `get-time` scenarios the two Netty approaches track each other closely, and at [60k `get-time`](#high-load-results) WebFlux on Netty actually delivered slightly *lower* tail latency (P90/P99 222/613&nbsp;ms vs 288/734&nbsp;ms) and marginally *more* requests.
> - Both Netty approaches accepted and served every fixed-rate connection burst up to 60k connections with **zero errors**.
>
> **Virtual Threads on Tomcat** are not recommended for bursty, high-connection workloads:
> - Higher steady-state resource use than either Netty approach where all three pass - e.g. at 10k fixed-rate `get-movies`, ~42% average CPU and ~53% average heap versus ~32–34% CPU and ~37–38% heap for the Netty approaches.
> - It fails the [fixed-rate scenarios](#where-tomcat-breaks-and-how-to-push-it-further) once the simultaneous-connection burst exceeds its acceptor's capacity - from 20k connections upward for `get-movies` - with **>99% `connection refused`** errors, even though average CPU during those runs stays well below saturation. The bottleneck is TCP connection *acceptance*, not request processing. Both Netty approaches accept the identical bursts with zero errors. This even happens in one [default scenario](#10k-vus-and-rps-get-movies-call-depth-1).
> - Tomcat does serve high *user* counts when the load is paced: it passes every [paced 60k spike scenario](#reading-the-40k-and-60k-results) cleanly, because client think-time keeps only a few thousand connections open at once. See [High Load Results](#high-load-results).

## Benchmark Winners

Below are top-performing approaches across all scenarios and metrics, visualizing the contents of [results/scenarios-default/results.csv](results/scenarios-default/results.csv):

- Each cell shows the metric values of best approach (on top) and runner-up.
    - What "best" is depends on the metric: A lower value is better for all metrics except for metrics starting with `requests_ok`, `requests_per_second`, or `sockets`.
    - Approaches which encountered request errors are ranked below those approaches with only successful requests. Such "failed" approaches have all their metric values printed in red and suffixed with `E`.
    - An overall ranking based on the win count of each approach is shown in the legend: `(1)` indicates the overall best approach, `(2)` the runner-up, and so on. This overall ranking is also shown next to each metric value.
- Cells are colored based on the winning approach. The darker the color, the bigger the lead of the winning approach over the runner-up. If cells are white or faded, there's no clear winner as the top two approaches performed similarly.
- For [detailed charts](#results) on each approach and test scenario combination, have a look at the second half of this document.
- All measurements below were performed on the dedicated, non-virtualized test environment described under [Results](#results). Additional [monthly measurements](./results/ci/ci.md) are performed on virtualized GitHub-hosted Runners.

### All Approaches

This chart compares Project Loom (on both Tomcat and Netty) with Project Reactor (on Netty). Across the 26 metrics × 11
default scenarios (286 per-cell contests), the win-count ranking is **Virtual Threads on Netty (38%)**, **Virtual Threads
on Tomcat (22%)**, **WebFlux on Netty (17%)**, with the remaining 22% undecided. Virtual Threads on Tomcat edges out
WebFlux on Netty here on win count despite its catastrophic failure in
[10k-vus-and-rps-get-movies-call-depth-1](#10k-vus-and-rps-get-movies-call-depth-1) (where the error-derank rule costs it
every metric), largely because it records the lowest GC counts of the three - winning both GC columns (count and time)
in 8 of the 11 scenarios - and the lowest P99 latency in all five 20k spike scenarios. Note that this win-count ranking weights all 26 metrics equally; on the
latency/throughput metrics that matter most for a service, the two Netty approaches lead.

![All Results](results/scenarios-default/results.png)

### Netty-based Approaches

This chart is based on the same benchmark as before, but only considers the two Netty-based approaches. Head-to-head,
**Virtual Threads on Netty wins ~50%** of the per-cell contests, **WebFlux on Netty ~26%**, and ~25% show no clear winner.

![Netty Results](results/scenarios-default/results-netty.png)

## Benchmark Features

* Fully automated and CLI-driven by running a single command: `benchmark.sh`.
* Different test scenario files, each containing one or more scenarios. Example: `src/main/resources/scenarios/scenarios-default.csv`.
* Operating system thread re-use by waiting and by performing transitive HTTP calls of configurable call depth.
* Interacts with realistic JSON APIs.
* Creates single PNG image via [Matplotlib](https://matplotlib.org/) for each combination of scenario and approach which contains:
    * Raw latencies and P50/90/99 percentiles, as well as any errors.
    * System metrics for CPU, RAM, sockets, and network throughput.
    * JVM metrics such as heap usage, garbage collections (GCs), and platform thread count.
* Creates summary PNG image of all scenarios which shows best approaches.

## Benchmark Design

The benchmark is driven by [k6](https://k6.io/docs/) which repeatedly issues HTTP requests to a service listening at http://localhost:8080/

The service exposes multiple REST endpoints. The implementation of each has the same 3 stages:

1. **HTTP Call**: If `$delayCallDepth > 0`, call `GET /$approach/epoch-millis` recursively `$delayCallDepth` times to mimic calls to upstream service(s).
    - By default, all approaches use `Spring Boot`'s [WebFlux WebClient](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-webclient)
      based on Netty.
    - The scenarios in [scenarios-clients.csv](src/main/resources/scenarios/scenarios-clients.csv) compare the `WebClient` with Spring Boot's
      [RestClient](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-restclient) using various client implementations. For details see the [Multi-Client Scenarios](#multi-client-scenarios) chapter.
2. **Wait**: If `$delayCallDepth = 0`, wait `$delayInMillis` (default: `100`) to mimic the delay incurred by a network call, filesystem access, or similar.
    - Whilst the request waits, its operating system thread can be reused by another request.
    - The imperative approaches (`platform-tomcat`, `loom-tomcat`, and `loom-netty`) use blocking wait whilst the reactive approach (`webflux-netty`) uses non-blocking wait.
3. **Calculate and Return Response** specific to REST endpoint.

### Sample Flow

Get all [movies](#movies) using `loom-netty` approach, an HTTP call depth of `1` and a delay of `100` milliseconds:

```mermaid
sequenceDiagram
    participant k6s
    participant service
    k6s->>+service: GET /loom-netty/movies?delayCallDepth=1&delayInMillis=100
    service->>+service: GET /loom-netty/epoch-millis?delayCallDepth=0&delayInMillis=100
    service->>service: Wait 100 milliseconds
    service-->>-service: Return current epoch millis
    service->>service: Find movies
    service-->>-k6s: Return movies
```

### REST APIs

The microservice under test exposes several RESTful APIs. In the following descriptions, `$approach` is the approach
under test and can be one of `loom-tomcat`, `loom-netty`, and `webflux-netty`.

All REST APIs support the following query parameters:

- `delayCallDepth`: Depth of recursive HTTP call stack to `$approach/epoch-millis` endpoint prior to server-side delay; see [Scenario Columns](#columns) for more details.
- `delayInMillis`: Server-side delay in milliseconds; see [Scenario Columns](#columns) for more details.

#### epoch-millis

The [TimeController](src/main/java/uk/gleissner/loomwebflux/time/TimeController.java) returns the milliseconds since the
epoch, i.e. 1 Jan 1970:

- This is one of the simplest possible APIs to provide a best-case performance scenario.
- Supported requests:
    - `GET /$approach/epoch-millis`

#### movies

The [MovieController](src/main/java/uk/gleissner/loomwebflux/movie/MovieController.java) gets and saves movies which are
stored in an [H2](https://h2database.com/) in-memory DB via [Spring Data JPA](https://spring.io/projects/spring-data-jpa), fronted by a [Caffeine](https://github.com/ben-manes/caffeine)-backed [Spring Boot cache](https://docs.spring.io/spring-boot/reference/io/caching.html):

- This is a realistic JSON API as exposed by a typical microservice.
- Several hard-coded movies by three directors are provided.

DB Considerations:

- By default, writes are not saved since the code under test is identical for all approaches and would thus only
  contribute to CPU use. However, this can be controlled with the Spring Boot property `loom-webflux.repo-read-only`
  in `src/main/resources/application.yaml`.
- The H2 DB was chosen for the same reason. To swap it for PostgreSQL, specify `postgres` in the `serverProfiles` column of the scenario CSV file. See [scenarios-postgres.csv](./src/main/resources/scenarios/scenarios-postgres.csv)
  and [PostgreSQL results](results/scenarios-postgres/results-netty.png).

Supported requests:

- `GET /$approach/movies?directorLastName={director}`:
    - Returns movies by the specified director.
    - Supported `{director}` values and their respective response body size in bytes, based on the default movies:
        - `Allen`: 1664 bytes (unindented)
        - `Hitchcock`: 1622 bytes (unindented)
        - `Kubrick`: 1235 bytes (unindented)
- `POST /$approach/movies`:
    - Saves one or more movies.
    - The request body is the [sample movies](src/main/resources/scenarios/movies.json) file (7,010 bytes on disk). Before POSTing, k6 re-serialises it to compact JSON via `JSON.stringify(JSON.parse(...))`, so the body actually sent over the wire is ~4.1 KB.

## Requirements

### Software

* Unix-based OS; tested with Ubuntu 22.04 and 24.04
* Java 21 or above
* [k6](https://k6.io/docs/) to drive load and measure latency as well as throughput.
* Python 3 with [Matplotlib](https://matplotlib.org/), [pandas](https://pandas.pydata.org/), and [NumPy](https://numpy.org/) to drive load and generate benchmark comparison reports.
* [sar/sadf](https://linux.die.net/man/1/sar) to measure system resource use
* Python 3 and [Matplotlib](https://matplotlib.org/) to convert latency and system CSV measurements into a PNG image

### Hardware

The hardware requirements depend on the scenarios you intend to run. If you run all scenarios (e.g. via `./benchmarks.sh`), then the following is recommended:

* CPU: Intel i5 12600K or similar
* RAM: 32 GiB

If you only run the default scenarios configured in `src/main/resources/scenarios/scenarios-default.csv`, the following is sufficient:

* CPU: Intel 6700K or similar
* RAM: 16 GiB

## Setup

The following instructions assume you are using a Debian-based Linux such as Ubuntu 22.04 or 24.04.

### Java 21

You'll need Java 21 or above:

```shell
sudo apt install openjdk-21-jdk
```

### k6

[k6](https://k6.io/docs/) is used to load the service:

```shell
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6
```

### Python 3, matplotlib, sar and sadf

Python 3 and `matplotlib` are used to convert the CSV output of `k6` and `sar`/`sadf` to a single PNG chart. The `sar`
and `sadf` tools come as part of `sysstat` and are used to measure resource use. To install them run:

```shell
sudo apt update && sudo apt install -y python3 python3-matplotlib python3-pandas python3-numpy sysstat

```

### Linux Host Tuning

The benchmark uses high numbers of direct HTTP client connections, so the host must be tuned consistently with the
server connection limits used by the application. These settings are intended for benchmark hosts, not as general
production-server guidance.

The executable source of truth is `src/main/bash/tune-benchmark-host.sh`. Local `./benchmark.sh` executions check these
values before building and request `sudo` only when a setting needs to be changed:

```shell
./src/main/bash/tune-benchmark-host.sh
```

Use `--check` to verify the current runtime values without applying changes:

```shell
./src/main/bash/tune-benchmark-host.sh --check
```

The benchmark sysctl baseline is:

```shell
sudo tee -a /etc/sysctl.conf <<'EOF'
net.ipv4.ip_local_port_range = 1024 65535
net.ipv4.tcp_timestamps = 1
net.ipv4.tcp_tw_reuse = 2
net.ipv4.tcp_max_syn_backlog = 65535
net.core.somaxconn = 65535
fs.file-max = 1048576
EOF
sudo sysctl -p
```

`ip_local_port_range` supports high local k6 concurrency, `tcp_timestamps` is required for TCP TIME-WAIT reuse semantics,
and `tcp_tw_reuse = 2` limits reuse to loopback-focused benchmark traffic on modern Linux. `tcp_max_syn_backlog` and
`somaxconn` avoid measuring connection queue limits during ramp-up. The effective server accept backlog is still the
lower of the application's listen backlog and `net.core.somaxconn`.

CI benchmark runs use non-interactive sudo:

```shell
sudo -n ./src/main/bash/tune-benchmark-host.sh
```

CI hosts must either provide non-interactive sudo for these sysctl writes or preconfigure the same runtime values before
running the benchmark. CI must not wait for a sudo password prompt.

### File Descriptor Limits

`fs.file-max` is a system-wide ceiling. It does not replace per-process open-file limits, so both `k6` and the JVM
process need a sufficient `nofile` limit:

```shell
printf '* soft nofile 1048576\n* hard nofile 1048576\n' | sudo tee -a /etc/security/limits.conf
```

If the benchmark service is launched through systemd, configure an equivalent `LimitNOFILE`. To verify active limits:

```shell
ulimit -n
cat /proc/$(pgrep -n k6)/limits | grep "Max open files"
cat /proc/$(pgrep -n java)/limits | grep "Max open files"
```

Log out and back in after changing persistent login limits.

## Execution

### benchmark.sh

Run a benchmark for each combination of approaches and scenarios defined in a scenario CSV file. Results are stored in
`build/results/`. The script checks benchmark host tuning before the run and may request `sudo` locally if runtime sysctl
values need to be applied:

```shell
./benchmark.sh 
```

Usage as per `benchmark.sh -h`:

```
Usage: benchmark.sh [OPTION]... [SCENARIO_FILE]
Runs benchmarks configured by a scenario file.

SCENARIO_FILE:     Scenario configuration CSV file in src/main/resources/scenarios/. Default: scenarios-default.csv

OPTION:
  -a <approaches>  Comma-separated list of approaches to test. Default: loom-tomcat, loom-netty, webflux-netty
                   Supported approaches: platform-tomcat, loom-tomcat, loom-netty, webflux-netty
  -C               Keep CSV files used to create chart. Default: false
  -w <seconds>     Override warmupDurationInSeconds from the scenario CSV for all scenarios. Default: value from CSV
  -t <seconds>     Override testDurationInSeconds from the scenario CSV for all scenarios. Default: value from CSV
  -h               Print this help
```

### benchmarks.sh

This is a wrapper over `benchmark.sh` and supports multiple scenario files:

```shell
./benchmarks.sh 
```

Usage as per `benchmarks.sh -h`:

```
Usage: benchmarks.sh [OPTION]... [SCENARIO_FILE]...
Wrapper over benchmark.sh that supports multiple scenario files and optionally suspends the system on completion.

SCENARIO_FILE:           Zero or more space-separated scenario configuration CSV files in src/main/resources/scenarios/.
                         Default: scenarios-default.csv scenarios-clients.csv scenarios-deep-call-stack.csv scenarios-postgres.csv scenarios-sharp-spikes.csv scenarios-soaktest.csv scenarios-high-load.csv

OPTION:
  -d, --dry-run          Print what would be done without actually performing it.
  -k, --kill-java        Kill all Java processes after each benchmark. Default: false
  -o, --options "<opts>" Pass additional options to the benchmark.sh script. Run "./benchmark.sh -h" for supported options.
  -s, --suspend          Suspend the system upon completion of the script. Default: false
  -h, --help             Show this help message and exit.
```

Please note that the default configured scenarios may take several hours to complete.

### Approaches

- **platform-tomcat**: Platform threads using [Tomcat](https://tomcat.apache.org/) server
- **loom-tomcat**: Virtual Threads using [Tomcat](https://tomcat.apache.org/) server
- **loom-netty**: Virtual Threads on [Netty](https://netty.io/) server
- **webflux-netty**: WebFlux on Netty server

All approaches use the same Spring Boot 4.1.0 version.

### Scenarios

#### Default Scenarios

These 11 scenarios cover a mixture of load patterns, from a 5-user smoke test and a 1k no-delay baseline up to 20k users,
mixing fixed-rate (`*-vus-and-rps-*`) and paced (`*-spike-*`) shapes, plain `get`/`get-post` workloads, and recursive
call depths of 0–2.

- Config: [./src/main/resources/scenarios/scenarios-default.csv](./src/main/resources/scenarios/scenarios-default.csv)
- Results: [./results/scenarios-default/results.md](./results/scenarios-default/results.md)

#### High-Load Scenarios

These use two deliberately different load shapes - do not read them as one continuous user-count scale:

- **Fixed-rate** scenarios (`*-vus-and-rps-*`) set `connections = requestsPerSecond` with **no client think-time**, so all *N* virtual users open and continuously drive their connections at once (~*N* simultaneous connections from the first second). They are swept across **10k → 20k → 40k → 60k** users to locate each endpoint's connection-acceptance threshold.
- **Paced** scenarios (`*-spike-*`) ramp the virtual-user count up and down with **1–3s of think-time** between requests and no fixed request rate, so connections are established gradually and, because each user spends most of its time sleeping, only a few thousand are ever in flight at once. They run at **60k** users.

As a result, a paced 60k scenario imposes far less instantaneous connection-establishment pressure than a fixed-rate scenario at the same - or even a much lower - user count. See [High Load Results](#high-load-results) for why this matters.

- Config: [./src/main/resources/scenarios/scenarios-high-load.csv](./src/main/resources/scenarios/scenarios-high-load.csv)
- Results: [./results/scenarios-high-load/results.md](./results/scenarios-high-load/results.md)

#### Multi-Client Scenarios

These scenarios compare both Spring Boot [RestClient](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-restclient) and
[WebClient](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-webclient) implementations with each other.

- Config: [./src/main/resources/scenarios/scenarios-clients.csv](./src/main/resources/scenarios/scenarios-clients.csv)
- Results: [./results/scenarios-clients/results.md](./results/scenarios-clients/results.md)

All scenarios except for those tested with a `webflux-netty` approach use the `WebClient` or `RestClient` implementation specified in the scenario name. However,
the `webflux-netty` approach always uses a fully reactive approach and therefore always uses the non-blocking `WebClient`.

The following clients are compared:

- Spring Boot `RestClient` based on:
    - [JDK HttpClient](https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpClient.html)
    - [Apache Commons HttpClient 5](https://hc.apache.org/httpcomponents-client-5.6.x/current/httpclient5/apidocs/)
    - [Netty](https://projectreactor.io/docs/netty/1.1.21/api/reactor/netty/http/client/HttpClient.html)
- Spring Boot `WebClient` based on:
    - [Netty](https://projectreactor.io/docs/netty/1.1.21/api/reactor/netty/http/client/HttpClient.html)

#### Other Scenarios

- [deep-call-stack](./results/scenarios-deep-call-stack/results.md): High delay call depths
- [postgres](./results/scenarios-postgres/results.md): Use PostgreSQL (started via Docker) instead of H2
- [sharp-spikes](./results/scenarios-sharp-spikes/results.md): Intermittent sharp load spikes from 0 to 10/20/30k users
- [soaktest](./results/scenarios-soaktest/results.md): Slow ramp-up to 10k users over ~16 minutes, followed by a ~4-minute ramp-down (20-minute test)

### Steps

The benchmark run for each `$scenario` consists of the following phases and steps:

#### Before Benchmark

* Build and start the Spring Boot service with a specific `$approach` as Spring Boot profile, using the config in `src/main/resources/application.yaml` and overridden by `src/main/resources/application-$approach.yaml` if defined.

#### Benchmark

* Run the benchmark as configured by the `$scenario`.
* For each `$resultType` (i.e. `latency`, `system`, or `jvm`), create a CSV file at `build/results/$scenario/$approach-$resultType.csv`.

#### After Benchmark

* Convert CSV files into `build/results/$scenario/$approach.png`
* Delete the CSV files unless the `-C` CLI option was specified.
* Stop the service.

## Config

### Common

- The `build.gradle.kts` file fixes the heap to 2 GiB (`-Xms2g -Xmx2g`) for the benchmarked service.
- The `src/main/resources/application.yaml` file enables HTTP/2.
- Time-outs are 60s: the WebClient `response-timeout` (used for the service's upstream calls) and the Hikari DB `connectionTimeout`.

### Scenario-specific

Each line in [src/main/resources/scenarios/scenarios-default.csv](src/main/resources/scenarios/scenarios-default.csv) configures a test scenario which is performed first for Java
Virtual Threads, then for WebFlux.

#### Example

| scenario                        | k6Config                        | serverProfiles | delayCallDepth | delayInMillis | connections | requestsPerSecond | warmupDurationInSeconds | testDurationInSeconds |
|---------------------------------|---------------------------------|----------------|----------------|---------------|-------------|-------------------|-------------------------|-----------------------|
| 5k-vus-and-rps-get-time         | get-time.js                     |                | 0              | 100           | 5000        | 5000              | 10                      | 180                   |
| 20k-vus-smooth-spike-get-movies | get-movies-smooth-vus-spike.js  |                | 0              | 100           | 20000       |                   | 0                       | 180                   |

#### Columns

1. `scenario`: Name of scenario. Is printed on top of each diagram.
2. `k6Config`: Name of the [K6 Config File](https://k6.io/docs/using-k6/http-requests/) which is assumed to be in
   the `config` folder
3. `serverProfiles`: Pipe-delimited Spring profiles which are also used to start and stop Docker containers. For example, specifying the value `postgres|no-cache` has these effects:
    - The Spring Boot profiles `postgres,no-cache` are added to the default Spring Boot profile of `$approach`.
    - The files `src/main/docker/docker-compose-postgres.yaml` and `src/main/docker/docker-compose-no-cache.yaml` (if existent)
      are used to start/stop Docker containers before/after each scenario run.
4. `delayCallDepth`: Depth of recursive HTTP call stack to `$approach/epoch-millis` endpoint prior to server-side delay.
    - Mimics calls to upstream services which allow for reuse of the current platform thread.
    - For example, a value of `0` means that the service waits for `$delayInMillis` milliseconds immediately upon receiving a request.
    - Otherwise, it calls the `$approach/epoch-millis` with `${delayCallDepth - 1}`.
    - This results in a recursive HTTP-request-based descent into the service, creating a call stack of depth `$delayCallDepth`.
5. `delayInMillis`: Server-side delay of each request, in milliseconds. Mimics a delay such as invoking a DB which allow for reuse of the current platform thread.
6. `connections`: Number of TCP connections, i.e. virtual users.
7. `requestsPerSecond`: Number of requests per second across all connections. Left empty for scenarios where the number
   of requests per second is organically derived based on the number of connections, the request latency, and any
   explicit client-side delays.
8. `warmupDurationInSeconds`: Duration of the warm-up iteration before the actual test. Warm-up is skipped if `0`.
9. `testDurationInSeconds`: Duration of the test iteration.

## Test Environment

- Unless noted otherwise, all tests were conducted on this test environment.
- **Preparation**: The system was rebooted before each test and quieted down as much as possible. The baseline total CPU
  use before test start was reduced to a negligible level.
- **Co-location**: Test driver (k6) and server under test (Spring Boot microservice) were co-located on the same
  physical machine. The aim of this benchmark is not to achieve maximum absolute performance, but rather to compare
  different server-side approaches with each other. Considering that the test driver and the load it produced was
  identical for the combination of server-side approach and scenario, this co-location should not affect the validity of
  the test results.

### Hardware

- CPU: [Intel Core i5-14600K](https://www.intel.com/content/www/us/en/products/sku/236799/intel-core-i5-processor-14600k-24m-cache-up-to-5-30-ghz/specifications.html) with 14 cores (6 P + 8 E), 20 threads, up to 5.3 GHz, and 125 W base / 181 W turbo power
- Motherboard: [Asus ProArt Z690-Creator WIFI](https://www.asus.com/uk/motherboards-components/motherboards/proart/proart-z690-creator-wifi/)
- RAM: 64GiB DDR5 (2 x [Corsair Vengeance 32 GiB 5600 MT/s CL40](https://www.corsair.com/uk/en/p/memory/cmk64gx5m2b5600c40/vengeancea-64gb-2x32gb-ddr5-dram-5600mhz-c40-memory-kit-a-black-cmk64gx5m2b5600c40))
- Disk: local NVMe SSD (not on the benchmark hot path: networking is loopback and the database is in-memory H2)
- Network: Loopback interface
- Virtualization: None; bare metal desktop

### Software

- OS: Ubuntu 24.04.4 LTS
- Kernel: 6.17.0-35-generic
- Java: Amazon Corretto 25.0.3.9.1 (build 25.0.3+9-LTS)
- Spring Boot 4.1.0

> [!NOTE]
> The actual software versions used by a benchmark are automatically determined and shown at the beginning of each `results.md` file. If there are differences to the above, then the values in `results.md` are correct.

## Results

This chapter shows the results of each default scenario, sorted by ascending scenario load. The results below can also be found in [./results/scenarios-default/results.md](./results/scenarios-default/results.md).

### Errors

During a run, any lines in the client-side or server-side log files which contain the term `error` (case-insensitive)
are preserved in error log files written to the results folder alongside the generated PNG files. (This committed
repository keeps only the PNG charts and the aggregated `results.csv`/`results.md`; the verbose per-scenario error logs
are not committed.) In the latest run, every error originated from `loom-tomcat`: the committed high-load `results.csv`
records ~37.3&nbsp;million failed `loom-tomcat` requests (concentrated in the fixed-rate `get-time`/`get-movies` bursts at
20k–60k), almost all of them `connection refused` as recorded in the verbose, non-committed error logs. The two Netty
approaches produced zero errors on this environment.

Any failed requests appear both in the latency chart as red dots, as well as in the RPS chart as part of a continuous orange
line. Additionally, they leave a trace in the `$approach-latency.csv` file, if preserved by running the benchmark with the `-C` option:

- A very small latency below 3ms indicates that the client failed to establish a TCP connection. Example from the latency CSV file: `1715728866471,0.000000,0,dial: i/o timeout,1211`. Such requests are not considered when reporting minimum latency since this could obscure the minimum latency of
  successful requests.
- A very large latency at ~60s reflects a request time-out (the server did not respond within the 60s time-out). Example from the latency CSV file: `1715728861008,60001.327066,0,request timeout,1050`.

### 1k-vus-and-rps-get-time-no-delay

A low-load baseline with no server-side delay: 1k virtual users issue 1k requests per second to the `epoch-millis`
endpoint, which returns immediately. With no artificial wait, each request completes in well under a millisecond
(median 0.26–0.54&nbsp;ms across approaches), so this scenario isolates raw request-handling overhead. All three
approaches handle it without errors at single-digit average CPU.

#### Virtual Threads (Tomcat)

![Loom](results/scenarios-default/1k-vus-and-rps-get-time-no-delay/loom-tomcat.png)

#### Virtual Threads (Netty)

![Loom](results/scenarios-default/1k-vus-and-rps-get-time-no-delay/loom-netty.png)

#### WebFlux (Netty)

![WebFlux](results/scenarios-default/1k-vus-and-rps-get-time-no-delay/webflux-netty.png)

### 5k-vus-and-rps-get-time

This scenario aims to maintain a steady number of 5k virtual users (VUs, i.e. TCP connections) as well as 5k requests
per second (RPS) across all users for 3 minutes:

- Each user issues a request and then waits. This wait between consecutive requests is controlled by k6 in order to
  achieve the desired number of RPS.
- The server-side delay is 100ms.
- The server returns the current millis since the epoch.

#### Virtual Threads (Tomcat)

![Loom](results/scenarios-default/5k-vus-and-rps-get-time/loom-tomcat.png)

#### Virtual Threads (Netty)

![WebFlux](results/scenarios-default/5k-vus-and-rps-get-time/loom-netty.png)

#### WebFlux (Netty)

![WebFlux](results/scenarios-default/5k-vus-and-rps-get-time/webflux-netty.png)

### 5k-vus-and-rps-get-movies

Like the previous scenario, but the response body contains a JSON of movies.

For further details, please see the [movies](#movies) section.

#### Virtual Threads (Tomcat)

![Loom](results/scenarios-default/5k-vus-and-rps-get-movies/loom-tomcat.png)

#### Virtual Threads (Netty)

![Loom](results/scenarios-default/5k-vus-and-rps-get-movies/loom-netty.png)

#### WebFlux (Netty)

![WebFlux](results/scenarios-default/5k-vus-and-rps-get-movies/webflux-netty.png)

### 10k-vus-and-rps-get-movies

Like the previous scenario, but 10k virtual users and requests per second.

#### Virtual Threads (Tomcat)

![Loom](results/scenarios-default/10k-vus-and-rps-get-movies/loom-tomcat.png)

#### Virtual Threads (Netty)

![Loom](results/scenarios-default/10k-vus-and-rps-get-movies/loom-netty.png)

#### WebFlux (Netty)

![WebFlux](results/scenarios-default/10k-vus-and-rps-get-movies/webflux-netty.png)

### 10k-vus-and-rps-get-movies-call-depth-1

Like the previous scenario, but mimics a request to an upstream service.

- On receiving an incoming HTTP request, the service calls itself via HTTP.
- This secondary request then waits 100 milliseconds.

> [!WARNING]
> This is the one **default** scenario in which `loom-tomcat` fails: it completed only 16,042 of ~1.74 million requests
> (a **99.1% error rate**, almost entirely `connection refused`), even though its average CPU stayed at ~20%. The nested
> self-call doubles the connection demand on Tomcat's connection-per-request acceptor (HTTP keep-alive is deliberately
> disabled - see [Where Tomcat Breaks](#where-tomcat-breaks-and-how-to-push-it-further)), and the acceptor cannot keep up.
> Both Netty approaches serve the full load with **zero errors** and ~100&nbsp;ms median latency. This is the same
> connection-acceptance limit explored in detail under [High Load Results](#high-load-results).

#### Virtual Threads (Tomcat)

![Loom](results/scenarios-default/10k-vus-and-rps-get-movies-call-depth-1/loom-tomcat.png)

#### Virtual Threads (Netty)

![Loom](results/scenarios-default/10k-vus-and-rps-get-movies-call-depth-1/loom-netty.png)

#### WebFlux (Netty)

![WebFlux](results/scenarios-default/10k-vus-and-rps-get-movies-call-depth-1/webflux-netty.png)

### 20k-vus-stepped-spike-get-movies

This scenario ramps up virtual users (and thus TCP connections) from 0 to 20k in multiple steps, then back down:

- Each step has a short [riser](https://en.wikipedia.org/wiki/Stair_riser) time when users are increased,
  followed by a longer [tread](https://en.wikipedia.org/wiki/Stair_tread) time when users are held
  constant.
- Each user issues a request, waits for the response, and then waits for a random delay between 1s and 3s. This reduces
  the load and better mimics real user interactions with a service, assuming the service calls are driven by user
  interactions with a website that relies on the service under test.
- The server-side delay before returning a response is 100ms.

#### Virtual Threads (Tomcat)

![Loom](results/scenarios-default/20k-vus-stepped-spike-get-movies/loom-tomcat.png)

#### Virtual Threads (Netty)

![Loom](results/scenarios-default/20k-vus-stepped-spike-get-movies/loom-netty.png)

#### WebFlux (Netty)

![WebFlux](results/scenarios-default/20k-vus-stepped-spike-get-movies/webflux-netty.png)

### 20k-vus-smooth-spike-get-movies

Like the previous scenario, but linear ramp-up and down.

#### Virtual Threads (Tomcat)

![Loom](results/scenarios-default/20k-vus-smooth-spike-get-movies/loom-tomcat.png)

#### Virtual Threads (Netty)

![Loom](results/scenarios-default/20k-vus-smooth-spike-get-movies/loom-netty.png)

#### WebFlux (Netty)

![WebFlux](results/scenarios-default/20k-vus-smooth-spike-get-movies/webflux-netty.png)

### 20k-vus-smooth-spike-get-post-movies

Like the previous scenario, but instead of just getting movies, we are now additionally saving them:

- 75% of requests are GET requests which are split into three groups, each requesting movies by a different director.
- 25% of requests are POST requests.

For further details, please see the [movies](#movies) section.

#### Virtual Threads (Tomcat)

![Loom](results/scenarios-default/20k-vus-smooth-spike-get-post-movies/loom-tomcat.png)

#### Virtual Threads (Netty)

![Loom](results/scenarios-default/20k-vus-smooth-spike-get-post-movies/loom-netty.png)

#### WebFlux (Netty)

![WebFlux](results/scenarios-default/20k-vus-smooth-spike-get-post-movies/webflux-netty.png)

### 20k-vus-smooth-spike-get-post-movies-call-depth-1

Like the previous scenario, but mimics call to upstream service as explained in [10k-vus-and-rps-get-movies-call-depth-1](#10k-vus-and-rps-get-movies-call-depth-1).

> [!NOTE]
> For `loom-netty` and `webflux-netty`, this scenario became CPU-contended at peak load (CPU max ~90% for both approaches).
> This raised tail latencies (P99 ~118-119&nbsp;ms, max ~371-398&nbsp;ms) but caused no errors.

#### Virtual Threads (Tomcat)

![Loom](results/scenarios-default/20k-vus-smooth-spike-get-post-movies-call-depth-1/loom-tomcat.png)

#### Virtual Threads (Netty)

![Loom](results/scenarios-default/20k-vus-smooth-spike-get-post-movies-call-depth-1/loom-netty.png)

#### WebFlux (Netty)

![WebFlux](results/scenarios-default/20k-vus-smooth-spike-get-post-movies-call-depth-1/webflux-netty.png)

### 20k-vus-smooth-spike-get-post-movies-call-depth-2

Like the previous scenario, but with a recursive call depth of 2 (each request triggers two nested `epoch-millis`
self-calls before the delay). The deeper call stack increases per-request work: versus call depth 1 it raises GC counts by
~1.5× on the Netty approaches and ~2.3× on Tomcat, and pushes average CPU to ~40–48% across approaches. Because this is a
*paced* spike (1–3s client think-time), all three approaches still complete it without errors; the two Netty approaches
keep P99 latency at ~119–122&nbsp;ms.

#### Virtual Threads (Tomcat)

![Loom](results/scenarios-default/20k-vus-smooth-spike-get-post-movies-call-depth-2/loom-tomcat.png)

#### Virtual Threads (Netty)

![Loom](results/scenarios-default/20k-vus-smooth-spike-get-post-movies-call-depth-2/loom-netty.png)

#### WebFlux (Netty)

![WebFlux](results/scenarios-default/20k-vus-smooth-spike-get-post-movies-call-depth-2/webflux-netty.png)

## High Load Results

This chapter highlights interesting results based on `scenarios-high-load.csv` which scales up to 60k users. For full
results, see [results/scenarios-high-load/results.md](results/scenarios-high-load/results.md).

### Summary

![Summary](results/scenarios-high-load/results.png)

### Netty Head-to-Head at High Load

Both Netty approaches accept and serve **every** fixed-rate burst up to 60k connections with zero errors, so the
interesting question at high load is *which one degrades more gracefully*. The answer depends on the workload:

- **`get-movies` (a ~1.6&nbsp;KB JSON response - Allen's movies): Virtual Threads on Netty win clearly at the top end.** At 60k fixed-rate
  `get-movies`, `loom-netty` holds a 100&nbsp;ms median and serves ~11.0M requests, while `webflux-netty` degrades
  sharply - its median climbs to ~1.2&nbsp;s, its P99 to ~3.3&nbsp;s, heap peaks at ~99%, GC time reaches ~108&nbsp;s, and
  it serves ~7.6M requests (~31% fewer). At 40k the two are closer, but `loom-netty` still has the lower tail latency.
- **`get-time` (a tiny response): the two are close, and WebFlux edges ahead at 60k.** At 60k fixed-rate `get-time`,
  `webflux-netty` serves marginally *more* requests (11.07M vs 10.99M) at *lower* tail latency (P90/P99 222/613&nbsp;ms vs
  288/734&nbsp;ms) and lower average CPU (61.6% vs 69.3%).

| 60k fixed-rate scenario | Approach | Requests (ok) | Latency P50/P90/P99/max (ms) | CPU avg % | GC time (ms) |
|-------------------------|----------|--------------:|------------------------------|----------:|-------------:|
| `get-time`   | `loom-netty`    | 10,989,762 | 100 / 288 / 734 / 3019   | 69.3 | 9,266 |
| `get-time`   | `webflux-netty` | 11,069,032 | 100 / 222 / 613 / 1954   | 61.6 | 8,140 |
| `get-movies` | `loom-netty`    | 10,971,441 | 100 / 379 / 757 / 2899   | 75.5 | 12,086 |
| `get-movies` | `webflux-netty` |  7,572,752 | 1174 / 2091 / 3319 / 16936 | 88.4 | 108,507 |

In the **paced** 60k spike scenarios (stepped, smooth, and smooth get-post), the two Netty approaches finish within a
fraction of a percent of each other on request count, and their P99 latencies are near-identical on the get-only spikes
(within ~1–2&nbsp;ms). On the mixed get-post workload `loom-netty` keeps a tighter tail (P99 117&nbsp;ms vs 137&nbsp;ms,
max 511&nbsp;ms vs 1107&nbsp;ms). The takeaway: Virtual Threads on Netty's advantage is real but concentrated in the
heaviest fixed-rate response-bound workloads, not a blanket win across every high-load scenario.

### Reading the 40k and 60k Results

A result that surprises many readers is that `loom-tomcat` records almost no successful requests for the higher **fixed-rate** scenarios with a server-side delay (e.g. 40k), yet passes the **paced** 60k scenarios cleanly - even though those use *more* users. This is a consequence of the [two load shapes](#high-load-scenarios), not of "60k being easier than 40k":

- A fixed-rate scenario presents its full user count as ~simultaneous connections (closed-loop, no think-time). The paced scenarios ramp up gradually and keep only a few thousand connections in flight at any instant, because each virtual user sleeps 1–3s between requests. The `sockets` rows in [results.csv](results/scenarios-high-load/results.csv) confirm this: in the paced 60k scenarios Tomcat holds only ~1.5k sockets on average (peak ~3k), whereas the failing fixed-rate runs only ever momentarily reach the full burst before the acceptor rejects most of it.
- `loom-tomcat` also passes the **no-delay** fixed-rate scenario at 60k users, with **zero errors** - and it does so while holding *tens of thousands* of concurrent sockets (avg ~40k, peak ~60k). So the distinguishing factor is **not** how many connections stay open. With no server-side delay, requests complete and connections recycle fast enough that Tomcat's acceptor drains the burst and admits the whole 60k - the run still saturates CPU (~94% average) and median latency climbs to ~372&nbsp;ms under the load, but nothing is refused. Add a 100ms delay and each accepted connection is held for at least that long; the connection-completion rate collapses, the acceptor falls behind the incoming burst, and ~99.7% of it is refused - which is *why* the failing fixed-rate runs show **low** average socket counts (most connections never get established).
- The fixed-rate failures are **TCP connection-acceptance** failures (`connection refused` dominates the error logs), not request-processing failures. The server does not return error responses; its connection acceptor simply cannot drain the accept queue as fast as the connection burst fills it. Latency and CPU during these runs reflect the few requests that did get accepted, not a saturated server.
- The two Netty-based approaches accept the *identical* fixed-rate bursts (including at 40k and 60k) with **zero errors**. The difference therefore lies in how each server's connection acceptor copes with a large simultaneous-connection burst (Tomcat's thread-based acceptor versus Netty's event-loop acceptor), not in the host or the load generator.

> [!NOTE]
> In short, the fixed-rate `loom-tomcat` failures are a connection-burst acceptance limit specific to the no-think-time shape - **not** a general inability to serve high user counts (it serves the paced 60k scenarios without errors). The fixed-rate rows are best read as a connection-acceptance stress test; the paced rows are closer to a typical production traffic shape.

All three approaches run near-vanilla server configuration and share the *same* OS-level tuning ([Linux Host Tuning](#linux-host-tuning) via `tune-benchmark-host.sh`), which raises connection limits for every contender equally. This result is not an artefact of under-tuning: in separate experiments (runtime overrides, not part of the committed dataset) the 40k `loom-tomcat` failure persisted - the large majority of connections still refused - even with the host fully tuned (`net.core.somaxconn` and `net.ipv4.tcp_max_syn_backlog` at 65535), Tomcat's `accept-count` raised to 65,000, and HTTP keep-alive enabled, while Netty served the same burst without errors on the same host. The limiting factor is the rate at which Tomcat's thread-based acceptor admits new connections, not the OS accept-queue depth or any single Tomcat setting.

### Where Tomcat Breaks, and How to Push It Further

To make the breaking point explicit and reproducible, `scenarios-high-load.csv` includes a round-number `get-movies` ladder of fixed-rate scenarios (each with `connections = requestsPerSecond`, no client think-time) that brackets it for `loom-tomcat` with this project's configuration:

| Scenario | Simultaneous users (= requests/s) | `loom-tomcat` result (latest run) |
|----------|-----------------------------------|-----------------------------------|
| [`10k-vus-and-rps-get-movies`](results/scenarios-high-load/results.md#10k-vus-and-rps-get-movies) | 10,000 | passes (0 errors) |
| [`20k-vus-and-rps-get-movies`](results/scenarios-high-load/results.md#20k-vus-and-rps-get-movies) | 20,000 | fails (>99.9% `connection refused`; 1,357 of ~3.57M ok) |
| [`40k-vus-and-rps-get-movies`](results/scenarios-high-load/results.md#40k-vus-and-rps-get-movies) | 40,000 | fails (~99.7% `connection refused`) |
| [`60k-vus-and-rps-get-movies`](results/scenarios-high-load/results.md#60k-vus-and-rps-get-movies) | 60,000 | fails (~99.8% `connection refused`) |

For `get-movies`, the breaking point sits between 10k and 20k simultaneous users, and the cliff is sharp: 10k is clean
while 20k is already an almost-total failure (>99.9% refused). The lighter `get-time` endpoint breaks *higher* - in this
run it passes both 10k **and** 20k with zero errors and only fails from 40k (~99.3% refused). Both endpoints impose the
same 100&nbsp;ms server-side delay, so the difference comes from per-request cost: `get-time` returns a few bytes with
negligible work, whereas the ~1.6&nbsp;KB `get-movies` response (Allen's movies) adds JSON serialisation and I/O per
request, leaving the acceptor less headroom and lowering the connection threshold. Both Netty-based approaches pass every
one of these fixed-rate points (`get-time` and `get-movies`, up to 60k) without errors.

The separate [`60k-vus-*-spike`](results/scenarios-high-load/results.md#60k-vus-smooth-spike-get-movies) scenarios use the **same 60,000 users but ramped with 1–3s of think-time**, so they are a different workload type - not the next rung of the ladder above - and Tomcat **passes** them, because pacing keeps only a few thousand connections concurrent (see [Reading the 40k and 60k Results](#reading-the-40k-and-60k-results)).

**How to push the threshold higher.** This project intentionally runs Tomcat with HTTP keep-alive effectively disabled (`server.tomcat.max-keep-alive-requests: 1`, `keep-alive-timeout: 1s`), so every request opens a fresh TCP connection. Re-enabling keep-alive lets clients reuse connections instead of reconnecting on every request, which relieves the acceptor:

```yaml
server:
  tomcat:
    max-keep-alive-requests: -1   # unlimited (Tomcat's own default is 100); current project value is 1
    keep-alive-timeout: 60s       # keep idle connections open for reuse; current project value is 1s
    accept-count: 65000           # deeper listen backlog to buffer connection bursts
```

We verified this experimentally with finer-grained probes (runtime overrides, reverted afterwards - the committed configuration is unchanged, and these probe points are not part of the committed dataset): enabling keep-alive raised the failure threshold by a few thousand simultaneous users - a real but limited gain, still short of the committed 20k scenario, which continues to fail under both settings.

**The downsides, and why this is not the default.**

- **It does not remove the ceiling.** Even with keep-alive enabled, the committed 20k and 40k scenarios still fail. The bottleneck is the *rate at which Tomcat's acceptor admits new connections*, not connection reuse, so keep-alive only shifts the threshold - it does not fix the burst case. A deeper `accept-count` / host `somaxconn` alone made no measurable difference in our tests.
- **Higher steady-state resource use.** Keep-alive pins one open connection (and, under Virtual Threads, one carrier-bound socket and file descriptor) per idle client for the duration of `keep-alive-timeout`. With many slow or idle clients and a high `max-connections`, Tomcat holds far more concurrent connections, threads, and memory than the connection-per-request model - the opposite of the lean profile this benchmark otherwise measures.
- **Comparability.** Changing the connection model alters Tomcat's connection-churn characteristics, breaking direct comparison with the historical results in this repository.

For these reasons the benchmark keeps the current, deliberately lean Tomcat setting (a level playing field with near-vanilla per-contender configuration), documents the breaking point above, and leaves the keep-alive trade-off as an informed choice for a developer tuning a real service.

### 60k-vus-smooth-spike-get-post-movies

Like [20k-vus-smooth-spike-get-post-movies](#20k-vus-smooth-spike-get-post-movies), but scaling up to 60k users.

#### Virtual Threads (Netty)

![Loom](results/scenarios-high-load/60k-vus-smooth-spike-get-post-movies/loom-netty.png)

#### WebFlux (Netty)

![WebFlux](results/scenarios-high-load/60k-vus-smooth-spike-get-post-movies/webflux-netty.png)
