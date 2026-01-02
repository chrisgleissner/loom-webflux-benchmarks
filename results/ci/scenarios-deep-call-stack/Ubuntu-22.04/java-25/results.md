# scenarios-deep-call-stack

## Test Time

| **Name**                | **Value** |
|-------------------------|-----------|
| **Start (UTC)** | 2026-01-02 17:11:08 |
| **End (UTC)** | 2026-01-02 18:05:28 |
| **Duration (hh:mm:ss)** | 00:54:20 |

## System Specs

| **Name**                | **Value** |
|-------------------------|-----------|
| **Java** | OpenJDK 64-Bit Server VM Corretto-25.0.1.9.1 (build 25.0.1+9-LTS, mixed mode, sharing) |
| **Spring Boot** | 3.5.9 |
| **Python** | 3.10.12 |
| **OS** | Ubuntu 22.04.5 LTS |
| **Kernel** | 6.8.0-1044-azure |
| **CPU** | AMD EPYC 9V74 80-Core Processor |
| **CPU Cores** | 4 |
| **RAM** | 15Gi total, 12Gi available |
| **Disk** | 159G total, 91G available |

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


