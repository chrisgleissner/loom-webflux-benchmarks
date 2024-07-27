# scenarios-deep-call-stack

## Test Time

| **Name**                | **Value** |
|-------------------------|-----------|
| **Start (UTC)** | 2024-07-26 23:03:50 |
| **End (UTC)** | 2024-07-27 00:28:24 |
| **Duration (hh:mm:ss)** | 01:24:34 |

## System Specs

| **Name**                | **Value** |
|-------------------------|-----------|
| **Java** | OpenJDK 64-Bit Server VM Corretto-21.0.4.7.1 (build 21.0.4+7-LTS, mixed mode, sharing) |
| **Python** | 3.10.12 |
| **OS** | Ubuntu 22.04.4 LTS |
| **Kernel** | 6.5.0-45-generic |
| **CPU** | Intel(R) Core(TM) i7-6700K CPU @ 4.00GHz |
| **CPU Cores** | 8 |
| **RAM** | 31Gi total, 27Gi available |
| **Disk** | 506G total, 287G available |

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

#### loom-tomcat

![loom-tomcat](./5k-vus-and-1k-rps-get-movies-call-depth-0/loom-tomcat.png)

#### loom-netty

![loom-netty](./5k-vus-and-1k-rps-get-movies-call-depth-0/loom-netty.png)

#### webflux-netty

![webflux-netty](./5k-vus-and-1k-rps-get-movies-call-depth-0/webflux-netty.png)


### 5k-vus-and-1k-rps-get-movies-call-depth-1

#### loom-tomcat

![loom-tomcat](./5k-vus-and-1k-rps-get-movies-call-depth-1/loom-tomcat.png)

#### loom-netty

![loom-netty](./5k-vus-and-1k-rps-get-movies-call-depth-1/loom-netty.png)

#### webflux-netty

![webflux-netty](./5k-vus-and-1k-rps-get-movies-call-depth-1/webflux-netty.png)


### 5k-vus-and-1k-rps-get-movies-call-depth-2

#### loom-tomcat

![loom-tomcat](./5k-vus-and-1k-rps-get-movies-call-depth-2/loom-tomcat.png)

#### loom-netty

![loom-netty](./5k-vus-and-1k-rps-get-movies-call-depth-2/loom-netty.png)

#### webflux-netty

![webflux-netty](./5k-vus-and-1k-rps-get-movies-call-depth-2/webflux-netty.png)


### 5k-vus-and-1k-rps-get-movies-call-depth-5

#### loom-tomcat

![loom-tomcat](./5k-vus-and-1k-rps-get-movies-call-depth-5/loom-tomcat.png)

#### loom-netty

![loom-netty](./5k-vus-and-1k-rps-get-movies-call-depth-5/loom-netty.png)

#### webflux-netty

![webflux-netty](./5k-vus-and-1k-rps-get-movies-call-depth-5/webflux-netty.png)


### 5k-vus-smooth-spike-get-post-movies-call-depth-0

#### loom-tomcat

![loom-tomcat](./5k-vus-smooth-spike-get-post-movies-call-depth-0/loom-tomcat.png)

#### loom-netty

![loom-netty](./5k-vus-smooth-spike-get-post-movies-call-depth-0/loom-netty.png)

#### webflux-netty

![webflux-netty](./5k-vus-smooth-spike-get-post-movies-call-depth-0/webflux-netty.png)


### 5k-vus-smooth-spike-get-post-movies-call-depth-1

#### loom-tomcat

![loom-tomcat](./5k-vus-smooth-spike-get-post-movies-call-depth-1/loom-tomcat.png)

#### loom-netty

![loom-netty](./5k-vus-smooth-spike-get-post-movies-call-depth-1/loom-netty.png)

#### webflux-netty

![webflux-netty](./5k-vus-smooth-spike-get-post-movies-call-depth-1/webflux-netty.png)


### 5k-vus-smooth-spike-get-post-movies-call-depth-2

#### loom-tomcat

![loom-tomcat](./5k-vus-smooth-spike-get-post-movies-call-depth-2/loom-tomcat.png)

#### loom-netty

![loom-netty](./5k-vus-smooth-spike-get-post-movies-call-depth-2/loom-netty.png)

#### webflux-netty

![webflux-netty](./5k-vus-smooth-spike-get-post-movies-call-depth-2/webflux-netty.png)


### 5k-vus-smooth-spike-get-post-movies-call-depth-5

#### loom-tomcat

![loom-tomcat](./5k-vus-smooth-spike-get-post-movies-call-depth-5/loom-tomcat.png)

#### loom-netty

![loom-netty](./5k-vus-smooth-spike-get-post-movies-call-depth-5/loom-netty.png)

#### webflux-netty

![webflux-netty](./5k-vus-smooth-spike-get-post-movies-call-depth-5/webflux-netty.png)


