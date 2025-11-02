# scenarios-deep-call-stack

## Test Time

| **Name**                | **Value** |
|-------------------------|-----------|
| **Start (UTC)** | 2025-11-02 17:23:44 |
| **End (UTC)** | 2025-11-02 18:17:49 |
| **Duration (hh:mm:ss)** | 00:54:05 |

## System Specs

| **Name**                | **Value** |
|-------------------------|-----------|
| **Java** | OpenJDK 64-Bit Server VM Corretto-21.0.9.10.1 (build 21.0.9+10-LTS, mixed mode, sharing) |
| **Spring Boot** | 3.5.7 |
| **Python** | 3.12.3 |
| **OS** | Ubuntu 24.04.3 LTS |
| **Kernel** | 6.11.0-1018-azure |
| **CPU** | Intel(R) Xeon(R) Platinum 8370C CPU @ 2.80GHz |
| **CPU Cores** | 4 |
| **RAM** | 15Gi total, 13Gi available |
| **Disk** | 159G total, 98G available |

## Scenarios

**Scenario file:** src/main/resources/scenarios/scenarios-deep-call-stack.csv

| Scenario | k6 Config | Server Profiles | Delay Call Depth | Delay (ms) | Connections | Requests per Second | Warmup Duration (s) | Test Duration (s) |
|----------|-----------|-----------------|------------------|------------|-------------|---------------------|---------------------|------------------|
| [5k-vus-and-1k-rps-get-movies-call-depth-0](#5k-vus-and-1k-rps-get-movies-call-depth-0) | get-movies.js |  | 0 | 100 | 5000 | 1000 | 10 | 180 |
| [5k-vus-and-1k-rps-get-movies-call-depth-1](#5k-vus-and-1k-rps-get-movies-call-depth-1) | get-movies.js |  | 1 | 100 | 5000 | 1000 | 10 | 180 |
| [5k-vus-and-1k-rps-get-movies-call-depth-2](#5k-vus-and-1k-rps-get-movies-call-depth-2) | get-movies.js |  | 2 | 100 | 5000 | 1000 | 10 | 180 |
| [5k-vus-and-1k-rps-get-movies-call-depth-5](#5k-vus-and-1k-rps-get-movies-call-depth-5) | get-movies.js |  | 5 | 100 | 5000 | 1000 | 10 | 180 |
| [5k-vus-smooth-spike-get-post-movies-call-depth-0](#5k-vus-smooth-spike-get-post-movies-call-depth-0) | get-post-movies-smooth-vus-spike.js |  | 0 | 100 | 5000 |  | 0 | 180 |
| [5k-vus-smooth-spike-get-post-movies-call-depth-1](#5k-vus-smooth-spike-get-post-movies-call-depth-1) | get-post-movies-smooth-vus-spike.js |  | 1 | 100 | 5000 |  | 0 | 180 |
| [5k-vus-smooth-spike-get-post-movies-call-depth-2](#5k-vus-smooth-spike-get-post-movies-call-depth-2) | get-post-movies-smooth-vus-spike.js |  | 2 | 100 | 5000 |  | 0 | 180 |
| [5k-vus-smooth-spike-get-post-movies-call-depth-5](#5k-vus-smooth-spike-get-post-movies-call-depth-5) | get-post-movies-smooth-vus-spike.js |  | 5 | 100 | 5000 |  | 0 | 180 |

## Result Overview

### Overall

![Overall Results](./results.png)
### Netty-based

![Netty Results](./results-netty.png)

## Result Details


### 5k-vus-and-1k-rps-get-movies-call-depth-0

#### loom-netty

![loom-netty](./5k-vus-and-1k-rps-get-movies-call-depth-0/loom-netty.png)

#### webflux-netty

![webflux-netty](./5k-vus-and-1k-rps-get-movies-call-depth-0/webflux-netty.png)


### 5k-vus-and-1k-rps-get-movies-call-depth-1

#### loom-netty

![loom-netty](./5k-vus-and-1k-rps-get-movies-call-depth-1/loom-netty.png)

#### webflux-netty

![webflux-netty](./5k-vus-and-1k-rps-get-movies-call-depth-1/webflux-netty.png)


### 5k-vus-and-1k-rps-get-movies-call-depth-2

#### loom-netty

![loom-netty](./5k-vus-and-1k-rps-get-movies-call-depth-2/loom-netty.png)

#### webflux-netty

![webflux-netty](./5k-vus-and-1k-rps-get-movies-call-depth-2/webflux-netty.png)


### 5k-vus-and-1k-rps-get-movies-call-depth-5

#### loom-netty

![loom-netty](./5k-vus-and-1k-rps-get-movies-call-depth-5/loom-netty.png)

#### webflux-netty

![webflux-netty](./5k-vus-and-1k-rps-get-movies-call-depth-5/webflux-netty.png)


### 5k-vus-smooth-spike-get-post-movies-call-depth-0

#### loom-netty

![loom-netty](./5k-vus-smooth-spike-get-post-movies-call-depth-0/loom-netty.png)

#### webflux-netty

![webflux-netty](./5k-vus-smooth-spike-get-post-movies-call-depth-0/webflux-netty.png)


### 5k-vus-smooth-spike-get-post-movies-call-depth-1

#### loom-netty

![loom-netty](./5k-vus-smooth-spike-get-post-movies-call-depth-1/loom-netty.png)

#### webflux-netty

![webflux-netty](./5k-vus-smooth-spike-get-post-movies-call-depth-1/webflux-netty.png)


### 5k-vus-smooth-spike-get-post-movies-call-depth-2

#### loom-netty

![loom-netty](./5k-vus-smooth-spike-get-post-movies-call-depth-2/loom-netty.png)

#### webflux-netty

![webflux-netty](./5k-vus-smooth-spike-get-post-movies-call-depth-2/webflux-netty.png)


### 5k-vus-smooth-spike-get-post-movies-call-depth-5

#### loom-netty

![loom-netty](./5k-vus-smooth-spike-get-post-movies-call-depth-5/loom-netty.png)

#### webflux-netty

![webflux-netty](./5k-vus-smooth-spike-get-post-movies-call-depth-5/webflux-netty.png)


