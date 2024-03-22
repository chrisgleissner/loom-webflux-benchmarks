# Loom vs Webflux

This project benchmarks a simple Spring Boot 3.2.* REST endpoint, comparing Java 21 Virtual Threads (Project Loom) with Webflux:
* The benchmark repeatedly issues HTTP GET requests to http://localhost:8080/epoch-millis/$approach?delayMillis=100 via the `vegeta` load tester, using a configurable number of connections (default: 10,000) as well as a configurable request rate (default: 10,000) across all connections. These settings can be changed in `./benchmark.sh`. 
* The implementation of this endpoint is the same for both approaches: it first waits $delayMillis (default if not specified: `50`), using the wait approach which is idiomatic for the respective framework. It then returns the millis since epoch.
* The value of `$approach` in the URL is either `loom` or `webflux`.

## Setup 

### Increase Open File Limit

```shell
printf '* soft nofile 1048576\n* hard nofile 1048576\n' | sudo tee -a /etc/security/limits.conf 
```
Then log out and back in.


### Build Load Tester

```shell
git clone https://github.com/tsenart/vegeta
cd vegeta
make vegeta
mv vegeta ~/bin
```

Make sure that the `vegeta` executable is in your `$PATH`.

## Benchmark 

The following command runs the benchmark for Loom as well as Webflux. Each benchmark consists of the following steps:
* Build and start Spring Boot web server with chosen approach (Loom or Webflux).
* Run two iterations of the benchmark. The result of each iteration is copied to the `results` folder, where each new iteration overwrites the previous one.
* Stop the server.

```shell
./benchmark-all.sh 
```

### Results

The following is the output of the `./benchmark-all.sh` command when executed on the test environment described further below.

```
Starting server with loom approach


Running benchmark: rate=10000, max-workers=10000, max-connections=10000, duration=60s
Test iteration #1...
Requests      [total, rate, throughput]         600000, 10000.03, 9980.98
Duration      [total, attack, wait]             1m0s, 1m0s, 114.508ms
Latencies     [min, mean, 50, 90, 95, 99, max]  100.264ms, 103.625ms, 100.802ms, 106.702ms, 117.045ms, 163.565ms, 213.101ms
Bytes In      [total, mean]                     7800000, 13.00
Bytes Out     [total, mean]                     0, 0.00
Success       [ratio]                           100.00%
Status Codes  [code:count]                      200:600000  
Error Set:
Test iteration #2...
Requests      [total, rate, throughput]         600000, 9999.97, 9983.16
Duration      [total, attack, wait]             1m0s, 1m0s, 101.01ms
Latencies     [min, mean, 50, 90, 95, 99, max]  100.287ms, 103.694ms, 100.745ms, 106.884ms, 117.273ms, 161.225ms, 305.039ms
Bytes In      [total, mean]                     7800000, 13.00
Bytes Out     [total, mean]                     0, 0.00
Success       [ratio]                           100.00%
Status Codes  [code:count]                      200:600000  
Error Set:


Stopping server


Starting server with webflux approach
....INFO: Created TensorFlow Lite XNNPACK delegate for CPU.
.

Running benchmark: rate=10000, max-workers=10000, max-connections=10000, duration=60s
Test iteration #1...
Requests      [total, rate, throughput]         600000, 10000.03, 9983.20
Duration      [total, attack, wait]             1m0s, 1m0s, 101.111ms
Latencies     [min, mean, 50, 90, 95, 99, max]  100.297ms, 106.242ms, 100.445ms, 100.905ms, 102.745ms, 332.303ms, 741.836ms
Bytes In      [total, mean]                     7800000, 13.00
Bytes Out     [total, mean]                     0, 0.00
Success       [ratio]                           100.00%
Status Codes  [code:count]                      200:600000  
Error Set:
Test iteration #2...
Requests      [total, rate, throughput]         600000, 9999.99, 9983.22
Duration      [total, attack, wait]             1m0s, 1m0s, 100.824ms
Latencies     [min, mean, 50, 90, 95, 99, max]  100.297ms, 101.11ms, 100.436ms, 100.784ms, 101.563ms, 115.352ms, 256.291ms
Bytes In      [total, mean]                     7800000, 13.00
Bytes Out     [total, mean]                     0, 0.00
Success       [ratio]                           100.00%
Status Codes  [code:count]                      200:600000  
Error Set:


Stopping server
```

### Latencies over Time 

The following diagrams show the latencies over time. They were exported from the HTML results in the `results` folder which were produced by the benchmark run above. 

#### Loom

![Loom](results/loom.png "Loom")

#### Webflux

![Webflux](results/webflux.png "Webflux")

### Test Environment

The benchmark was performed on the following environment:

```shell
printf "Java:\t" && java --version | grep "Server" && printf "OS:\t" && cat /etc/os-release | grep "PRETTY" && printf "Kernel:\t" && uname -r && printf "CPU:\t" && lscpu | grep "Model name" && printf "Cores:\t" && cat /proc/cpuinfo | awk '/^processor/{print $3}' | wc -l
```

Result:
```
Java:   OpenJDK 64-Bit Server VM Corretto-21.0.2.13.1 (build 21.0.2+13-LTS, mixed mode, sharing)
OS:     PRETTY_NAME="Ubuntu 22.04.4 LTS"
Kernel: 5.15.86-051586-generic
CPU:    Model name:                      Intel(R) Core(TM) i7-6700K CPU @ 4.00GHz
Cores:  8
```

