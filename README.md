# Benchmark of Java Virtual Threads vs Webflux

This project benchmarks a simple [Spring Boot 3.2.4](https://spring.io/projects/spring-boot) HTTP endpoint, comparing Java Virtual Threads (introduced by [Project Loom, JEP 425](https://openjdk.org/jeps/425)) with [Spring Webflux](https://docs.spring.io/spring-framework/reference/web/webflux.html) (relying on [Project Reactor](https://projectreactor.io/)):
* The benchmark is driven by [vegeta](https://github.com/tsenart/vegeta) which repeatedly issues HTTP GET requests to a service listening at http://localhost:8080/epoch-millis/$approach?delayMillis=$delayMillis
* The value of `$approach` in the URL is either `loom` or `webflux`.
* The service implementation consists of two steps:
  1. It waits `$delayMillis` (default: `100`) to mimic a network call, filesystem wait, or similar. Whilst the request waits, its operating system thread can be reused by another request. Both Loom and Webflux use their respective idiomatic ways to wait. 
  2. It then returns the milliseconds since the epoch.

## Setup 

The following instructions assume you are using a Debian-based Linux such as Ubuntu 22.04. 

### Increase Open File Limit

```shell
printf '* soft nofile 1048576\n* hard nofile 1048576\n' | sudo tee -a /etc/security/limits.conf 
```
Then log out and back in.


### Build Load Tester

We are using [vegeta](https://github.com/tsenart/vegeta) for driving the load test. Here's how to build it from source:

```shell
git clone https://github.com/tsenart/vegeta
cd vegeta
make vegeta
mv vegeta ~/bin
```

Make sure that the `vegeta` executable is in your `$PATH`.

## Benchmark 

The following command runs the benchmark first for Project Loom, then for Webflux:

```shell
./benchmark-all.sh 
```

Each benchmark run consists of the following steps:
* Build and start Spring Boot service with chosen approach (Loom or Webflux).
* Run two iterations of the benchmark. The result of each iteration is copied to the `results` folder, where each new iteration overwrites the previous one.
* Stop the service.

### Config

Configuration of the benchmark:
* Client: `benchmark.sh` configures `totalRate`, `connections`, `workers`, `delayMillis` and `testIterationDuration`. Their values are logged during the benchmark.
* Service: `build-*.gradle` configures the heap space to 1 GiB.

### Results

The following is the output of the `./benchmark-all.sh` command when executed on the test environment described further below.

```
Starting service with loom approach
Service URL: http://localhost:8080/epoch-millis/loom?delayMillis=100
....

Running benchmark: totalRate=5000/s, connections=5000, workers=5000, delayMillis=100, testIterationDuration=60s
Test iteration #1...
Requests      [total, rate, throughput]         300000, 5003.58, 4995.15
Duration      [total, attack, wait]             1m0s, 59.957s, 101.267ms
Latencies     [min, mean, 50, 90, 95, 99, max]  100.358ms, 115.793ms, 101.189ms, 111.903ms, 127.196ms, 644.686ms, 1.252s
Bytes In      [total, mean]                     3900000, 13.00
Bytes Out     [total, mean]                     0, 0.00
Success       [ratio]                           100.00%
Status Codes  [code:count]                      200:300000  
Error Set:
Test iteration #2...
Requests      [total, rate, throughput]         300000, 5003.60, 4994.87
Duration      [total, attack, wait]             1m0s, 59.957s, 104.873ms
Latencies     [min, mean, 50, 90, 95, 99, max]  100.316ms, 103.503ms, 101.071ms, 105.309ms, 113.344ms, 153.922ms, 419.383ms
Bytes In      [total, mean]                     3900000, 13.00
Bytes Out     [total, mean]                     0, 0.00
Success       [ratio]                           100.00%
Status Codes  [code:count]                      200:300000  
Error Set:


Stopping service
{"message":"Shutting down, bye..."}


Starting service with webflux approach
Service URL: http://localhost:8080/epoch-millis/webflux?delayMillis=100
....INFO: Created TensorFlow Lite XNNPACK delegate for CPU.


Running benchmark: totalRate=5000/s, connections=5000, workers=5000, delayMillis=100, testIterationDuration=60s
Test iteration #1...
[5999:5999:0323/085007.269979:ERROR:atom_cache.cc(229)] Add WM_CHANGE_STATE to kAtomsToCache
Requests      [total, rate, throughput]         299999, 5000.85, 4992.26
Duration      [total, attack, wait]             1m0s, 59.99s, 103.161ms
Latencies     [min, mean, 50, 90, 95, 99, max]  100.393ms, 102.39ms, 101.009ms, 102.29ms, 105.835ms, 119.972ms, 418.903ms
Bytes In      [total, mean]                     3899987, 13.00
Bytes Out     [total, mean]                     0, 0.00
Success       [ratio]                           100.00%
Status Codes  [code:count]                      200:299999  
Error Set:
Test iteration #2...
Requests      [total, rate, throughput]         299998, 5003.56, 4995.14
Duration      [total, attack, wait]             1m0s, 59.957s, 101.035ms
Latencies     [min, mean, 50, 90, 95, 99, max]  100.295ms, 101.917ms, 100.9ms, 101.674ms, 103.305ms, 118.605ms, 412.653ms
Bytes In      [total, mean]                     3899974, 13.00
Bytes Out     [total, mean]                     0, 0.00
Success       [ratio]                           100.00%
Status Codes  [code:count]                      200:299998  
Error Set:


Stopping service
{"message":"Shutting down, bye..."}
```

### Latencies over Time 

The following diagrams show the client-side end-to-end request latencies (Y axis, in ms) over elapsed benchmark time (X axis, in seconds). 

They were exported from the HTML results in the `results` folder which were produced by the benchmark run above.

#### Loom

![Loom](results/loom.png "Loom")

#### Webflux

![Webflux](results/webflux.png "Webflux")

### Test Environment

The benchmark was performed on the following environment:

```
Java:   OpenJDK 64-Bit Server VM Corretto-21.0.2.13.1 (build 21.0.2+13-LTS, mixed mode, sharing)
OS:     PRETTY_NAME="Ubuntu 22.04.4 LTS"
Kernel: 5.15.86-051586-generic
CPU:    Model name:                      Intel(R) Core(TM) i7-6700K CPU @ 4.00GHz
Cores:  8
```

This output was obtained via:

```shell
printf "Java:\t" && java --version | grep "Server" && printf "OS:\t" && cat /etc/os-release | grep "PRETTY" && printf "Kernel:\t" && uname -r && printf "CPU:\t" && lscpu | grep "Model name" && printf "Cores:\t" && cat /proc/cpuinfo | awk '/^processor/{print $3}' | wc -l
```
