# scenarios-soaktest

## Test Time

| **Name**                | **Value** |
|-------------------------|-----------|
| **Start (UTC)** | 2025-10-02 05:25:00 |
| **End (UTC)** | 2025-10-02 06:06:43 |
| **Duration (hh:mm:ss)** | 00:41:43 |

## System Specs

| **Name**                | **Value** |
|-------------------------|-----------|
| **Java** | OpenJDK 64-Bit Server VM Corretto-25.0.0.36.2 (build 25+36-LTS, mixed mode, sharing) |
| **Spring Boot** | 3.5.6 |
| **Python** | 3.12.3 |
| **OS** | Ubuntu 24.04.3 LTS |
| **Kernel** | 6.11.0-1018-azure |
| **CPU** | AMD EPYC 7763 64-Core Processor |
| **CPU Cores** | 4 |
| **RAM** | 15Gi total, 12Gi available |
| **Disk** | 159G total, 99G available |

## Scenarios

**Scenario file:** src/main/resources/scenarios/scenarios-soaktest.csv

| Scenario | k6 Config | Server Profiles | Delay Call Depth | Delay (ms) | Connections | Requests per Second | Warmup Duration (s) | Test Duration (s) |
|----------|-----------|-----------------|------------------|------------|-------------|---------------------|---------------------|------------------|
| [10k-vus-smooth-spike-get-post-movies](#10k-vus-smooth-spike-get-post-movies) | get-post-movies-smooth-vus-spike.js |  | 0 | 100 | 10000 |  | 0 | 1200 |

## Result Overview

### Overall

![Overall Results](./results.png)
### Netty-based

![Netty Results](./results-netty.png)

## Result Details


### 10k-vus-smooth-spike-get-post-movies

#### loom-netty

![loom-netty](./10k-vus-smooth-spike-get-post-movies/loom-netty.png)

#### webflux-netty

![webflux-netty](./10k-vus-smooth-spike-get-post-movies/webflux-netty.png)


