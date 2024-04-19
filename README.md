# Benchmark of Java Virtual Threads vs WebFlux

This Java 21 project benchmarks a simple [Spring Boot 3.2.5](https://spring.io/projects/spring-boot) HTTP endpoint using configurable scenarios, comparing Java Virtual Threads (introduced by [Project Loom, JEP 444](https://openjdk.org/jeps/444)) using Tomcat and Netty with [Spring WebFlux](https://docs.spring.io/spring-framework/reference/web/webflux.html) (relying on [Project Reactor](https://projectreactor.io/)) using Netty.

### Background

Both Spring WebFlux and Virtual Threads are alternative technologies to create Java microservices that support a high number of concurrent users, mapping all incoming requests to very few shared operating system threads. This reduces the resource overhead incurred by dedicating a single operating system thread to each user.

Spring WebFlux was first introduced in September 2017. Virtual Threads were first introduced as preview feature with Java 19 and were fully rolled out with Java 21 in September 2023.

### Features

* Fully automated and CLI-driven via `benchmark-all.sh`. 
* Test scenario support, see `config/scenario.csv`.
* Produces single PNG plot using [Matplotlib](https://matplotlib.org/) for each scenario and approach (Loom or WebFlux), containing:
  * Raw latencies and P50/90/99 percentiles, as well as any errors
  * System metrics for CPU, RAM, sockets, and network throughput

### Design
* The benchmark is driven by [k6](https://k6.io/docs/) which repeatedly issues HTTP GET requests to a service listening at http://localhost:8080/epoch-millis/$approach?delayInMillis=$delayInMillis
* The service implementation consists of two steps:
  1. It waits `$delayInMillis` (default: `100`) to mimic a network call, filesystem wait, or similar. Whilst the request waits, its operating system thread can be reused by another request. Both Loom and WebFlux use their respective idiomatic ways to wait. 
  2. It then returns the milliseconds since the epoch.

## Requirements

### Software
* Unix-based OS; tested with Ubuntu 22.04
* Java 21 or above
* [k6](https://k6.io/docs/) and Python 3 with [Matplotlib](https://matplotlib.org/) to drive load and measure latency
* [sar/sadf](https://linux.die.net/man/1/sar) to measure system resource use
* Python 3 and [Matplotlib](https://matplotlib.org/) to convert latency and system CSV measurements into a PNG image

### Hardware

The hardware requirements depend purely on the scenarios configured in `config/scenarios.csv`. The following is recommended to run the default scenarios committed to this repo:
* CPU comparable to Intel 6700K or above
* 16 GiB RAM

## Setup 

The following instructions assume you are using a Debian-based Linux such as Ubuntu 22.04. 

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

Python 3 and `matplotlib` are used to convert the CSV output of `k6` and `sar`/`sadf` to a single PNG chart. The `sar` and `sadf` tools come as part of `sysstat` and are used to measure resource use. To install them run:

```shell
sudo apt update && sudo apt install -y python3 python3-matplotlib sysstat
```

### Linux Optimizations

The following adjustments optimize Linux for HTTP load tests. 

#### Increase Open File Limit

Ensure your system can handle a large number of concurrent connections:

```shell
printf '* soft nofile 1048576\n* hard nofile 1048576\n' | sudo tee -a /etc/security/limits.conf 
```

#### Allow Fast Connection Reuse

Ensure the TCP connections created by a test scenario can be quickly reused by subsequent scenarios:

```shell
printf 'net.ipv4.tcp_tw_reuse = 1\nnet.ipv4.tcp_tw_recycle = 1\n' | sudo tee -a /etc/sysctl.conf
```

Please note that the latter adjustment (`net.ipv4.tcp_tw_recycle = 1`) is [known](https://www.speedguide.net/articles/linux-tweaking-121) to cause problems if your Linux machine
hosts an externally visible website and uses a load balancer. In that case, revert it after you are done with load tests.

#### Activate Changes

Log out and back in.


## Benchmark 

The following command runs the benchmark for each combination of approaches and scenarios in [config/scenarios.csv](config/scenarios.csv). Results are recorded in the `results` folder:

```shell
./benchmark-all.sh 
```

### Approaches

- **loom-tomcat**: Virtual Threads using Tomcat
- **loom-netty**: Virtual Threads on Netty
- **webflux-netty**: WebFlux on Netty

All approaches use the same Spring Boot 3.2.x version.

### Scenarios

Scenarios configured in [config/scenarios.csv](config/scenarios.csv):

| Scenario                            | Description                                       | Virtual Users (VU) | Requests per Second (RPS)   | Client delay (ms)    | Server delay (ms) |
|-------------------------------------|---------------------------------------------------|--------------------|-----------------------------|----------------------|-------------------|
| smoketest                           | Smoke test of test infrastructure                 | 500                | 500                         | 0                    | 100               |
| [5k-vus-and-rps](#5k-vus-and-rps)   | Constant users, constant request rate             | 5,000              | 5,000                       | 0                    | 100               |
| [10k-vus-and-rps](#10k-vus-and-rps) | Constant users, constant request rate             | 10,000             | 10,000                      | 0                    | 100               |
| [5k-vus-and-rps](#5k-vus-and-rps)   | Constant users                                    | 5,000              | Depends on users and delays | 1000 - 3000 (random) | 100               |
| [10k-vus-and-rps](#10k-vus-and-rps) | Constant users                                    | 10,000             | Depends on users and delays | 1000 - 3000 (random) | 100               |
| [ramp-vus-steps](#ramp-vus-steps)   | User spike: Ramp-up in 5k steps, linear ramp-down | 0 - 25,000         | Depends on users and delays | 1000 - 3000 (random) | 100               |
| [ramp-vus-linear](#ramp-vus-linear) | User spike: Linear ramp-up and ramp-down          | 0 - 25,000         | Depends on users and delays | 1000 - 3000 (random) | 100               |

### Steps

The benchmark run for each scenario consists of the following steps:
* Build and start Spring Boot service with chosen approach (Loom or WebFlux).
* Run two iterations of the benchmark. The result of each iteration is copied to the `results` folder, where each new iteration overwrites the previous one.
* Stop the service.

## Config

### Common

- The `build-$approach.gradle` file configures the heap space to 2 GiB. The value of `$approach` is replaced with either `loom` or `webflux`, depending on the approach under test.
- The `src/main/resources/application.yaml` file enables HTTP/2.
- Time-out is 60s for both client and server.

### Scenario-specific

Each line in [config/scenarios.csv](config/scenarios.csv) configures a test scenario which is performed first for Java Virtual Threads, then for WebFlux.

#### Example

|scenario                 |k6Config                    |delayInMillis|connections|requestsPerSecond|warmupDurationInSeconds|testDurationInSeconds|
|-------------------------|----------------------------|-------------|-----------|-----------------|-----------------------|---------------------|
|5k_users                 |k6.js                       |100          |5000       |5000             |10                     |360                  |
|ramp-vus-steps           |k6-ramp-vus-to-25k-steps.js |100          |           |                 |0                      |360                  |

#### Columns 

1. `scenario`: Name of scenario. Is printed on top of each diagram.
2. `k6Config`: Name of the [K6 Config File](https://k6.io/docs/using-k6/http-requests/) which is assumed to be in the `config` folder. If specified and different from `k6.js`, the value of the `connections`, `requestsPerSecond`, and `warmUpDurationInSeconds` columns is ignored.
3. `delayInMillis`: Server-side delay of each request, in milliseconds.
4. `connections`: Number of TCP connections, i.e. virtual users. Ignored if the `k6Config` column contains `k6.js`.
5. `requestsPerSecond`: Number of requests per second across all connections. Ignored if the `k6Config` column contains `k6.js`.
6. `warmUpDurationInSeconds`: Duration of a warm-up iteration before the actual test. Warm-up is skipped if `0`. Ignored if the `k6Config` column contains `k6.js`. 
7. `testDurationInSeconds`: Duration of the test iteration. If the `k6Config` column has a value other than `k6.js`, the test duration is instead controlled by the K6 config and the value of this cell instead purely controls the duration of the system monitoring. In this case, it needs to match the test duration configured in the K6 config file.

## Results 

## Test Environment

### Hardware
- CPU: [Intel Core i7-6700K](https://www.intel.com/content/www/us/en/products/sku/88195/intel-core-i76700k-processor-8m-cache-up-to-4-20-ghz/specifications.html) @ 4.00GHz with 4 cores (8 threads)
- Virtualization: None; bare metal desktop
- RAM: 32 GiB DDR4 (2 x Corsair 16 GiB, 2133 MT/s)
- Network: Loopback interface

### Software
- OS: Ubuntu 22.04.4 LTS
- Kernel: 5.15.86-051586-generic
- Java: Amazon Corretto JDK 21.0.3.9.1
- Spring Boot 3.2.5

### Other Notes

* **Preparation**: The system was rebooted before each test and quieted down as much as possible. The baseline total CPU use before test start was 0.3%.
* **Co-location**: Test driver (k6) and server under test (Spring Boot microservice) were co-located on the same physical machine. The aim of this benchmark is not to achieve maximum absolute performance, but rather to compare different server-side approaches with each other. Considering that the test driver and the load it produced was identical for the combination of server-side approach and scenario, this co-location should not affect the validity of the test results.
* **Specs**: Hardware specs are included at the top of the [full logs](results/benchmark.log) and were obtained via
```shell
log-system-specs.sh
```

## Charts

### 5k-vus-and-rps

This scenario aims to maintain a steady number of 5k virtual users (VUs, i.e. TCP connections) as well as 5k requests per second (RPS) across all users for 5 minutes:
- Each user issues a request and then waits. This wait between consecutive requests is controlled by k6 in order to achieve the desired number of RPS.
- The server-side delay is 100ms.

#### Virtual Threads (Tomcat)

![Loom](results/5k-vus-and-rps/loom-tomcat.png)

#### Virtual Threads (Netty)

![WebFlux](results/5k-vus-and-rps/loom-netty.png)

#### WebFlux (Netty)

![WebFlux](results/5k-vus-and-rps/webflux-netty.png)

### 10k-vus-and-rps

Like the earlier scenario, but it aims to maintain 10k users and RPS.

#### Virtual Threads (Tomcat)

![Loom](results/10k-vus-and-rps/loom-tomcat.png)

#### Virtual Threads (Netty)

![Loom](results/10k-vus-and-rps/loom-netty.png)

#### WebFlux (Netty)

![WebFlux](results/10k-vus-and-rps/webflux-netty.png)

### 10k-vus

Like the earlier scenario, but each user waits a random time of between 1s and 3s between consecutive requests. 

This reduces the load and better mimics real user interactions with a service, assuming
the service calls are driven by user interactions with a website that relies on the service under test.

#### Virtual Threads (Tomcat)

![Loom](results/10k-vus/loom-tomcat.png)

#### Virtual Threads (WebFlux)

![Loom](results/10k-vus/loom-netty.png)

#### WebFlux (Netty)

![WebFlux](results/10k-vus/webflux-netty.png)

### ramp-vus-steps

This scenario ramps up virtual users (and thus TCP connections) from 0 to 25k in multiple steps, then back down:
- Each step has a 20s ramp-time followed by a 40s steady time.
- Each user issues a request, waits for the response, and then waits for a random delay between 1s and 3s.
- The server-side delay before returning a response is 100ms.

#### Virtual Threads (Tomcat)

![Loom](results/ramp-vus-steps/loom-tomcat.png)

#### Virtual Threads (Netty)

![Loom](results/ramp-vus-steps/loom-netty.png)

#### WebFlux (Netty)

![WebFlux](results/ramp-vus-steps/webflux-netty.png)

### ramp-vus-linear

Like the earlier scenario, but linear ramp-up and down.

#### Virtual Threads (Tomcat)

![Loom](results/ramp-vus-linear/loom-tomcat.png)

#### Virtual Threads (Netty)

![Loom](results/ramp-vus-linear/loom-netty.png)

#### WebFlux (Netty)

![WebFlux](results/ramp-vus-linear/webflux-netty.png)
