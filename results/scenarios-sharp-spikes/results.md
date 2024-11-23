# scenarios-sharp-spikes

## Test Time

| **Name**                | **Value** |
|-------------------------|-----------|
| **Start (UTC)** | 2024-11-22 21:25:34 |
| **End (UTC)** | 2024-11-22 23:02:26 |
| **Duration (hh:mm:ss)** | 01:36:52 |

## System Specs

| **Name**                | **Value** |
|-------------------------|-----------|
| **Java** | OpenJDK 64-Bit Server VM Corretto-21.0.5.11.1 (build 21.0.5+11-LTS, mixed mode, sharing) |
| **Spring Boot** | 3.4.0 |
| **Python** | 3.12.3 |
| **OS** | Ubuntu 24.04.1 LTS |
| **Kernel** | 6.8.0-49-generic |
| **CPU** | Intel(R) Core(TM) i7-6700K CPU @ 4.00GHz |
| **CPU Cores** | 8 |
| **RAM** | 31Gi total, 23Gi available |
| **Disk** | 506G total, 274G available |

## Scenarios

**Scenario file:** src/main/resources/scenarios/scenarios-sharp-spikes.csv

| Scenario | k6 Config | Server Profiles | Delay Call Depth | Delay (ms) | Connections | Requests per Second | Warmup Duration (s) | Test Duration (s) |
|----------|-----------|-----------------|------------------|------------|-------------|---------------------|---------------------|------------------|
| [10k-vus-sharp-spikes-get-post-movies-call-depth-0](#10k-vus-sharp-spikes-get-post-movies-call-depth-0) | get-post-movies-sharp-vus-spikes.js |  | 0 | 100 | 10000 |  | 0 | 300 |
| [10k-vus-sharp-spikes-get-post-movies-call-depth-1](#10k-vus-sharp-spikes-get-post-movies-call-depth-1) | get-post-movies-sharp-vus-spikes.js |  | 1 | 100 | 10000 |  | 0 | 300 |
| [20k-vus-sharp-spikes-get-post-movies-call-depth-0](#20k-vus-sharp-spikes-get-post-movies-call-depth-0) | get-post-movies-sharp-vus-spikes.js |  | 0 | 100 | 20000 |  | 0 | 300 |
| [20k-vus-sharp-spikes-get-post-movies-call-depth-1](#20k-vus-sharp-spikes-get-post-movies-call-depth-1) | get-post-movies-sharp-vus-spikes.js |  | 1 | 100 | 20000 |  | 0 | 300 |
| [30k-vus-sharp-spikes-get-post-movies-call-depth-0](#30k-vus-sharp-spikes-get-post-movies-call-depth-0) | get-post-movies-sharp-vus-spikes.js |  | 0 | 100 | 30000 |  | 0 | 300 |
| [30k-vus-sharp-spikes-get-post-movies-call-depth-1](#30k-vus-sharp-spikes-get-post-movies-call-depth-1) | get-post-movies-sharp-vus-spikes.js |  | 1 | 100 | 30000 |  | 0 | 300 |

## Result Overview

### Overall

![Overall Results](./results.png)
### Netty-based

![Netty Results](./results-netty.png)

## Result Details


### 10k-vus-sharp-spikes-get-post-movies-call-depth-0

#### loom-tomcat

![loom-tomcat](./10k-vus-sharp-spikes-get-post-movies-call-depth-0/loom-tomcat.png)

#### loom-netty

![loom-netty](./10k-vus-sharp-spikes-get-post-movies-call-depth-0/loom-netty.png)

#### webflux-netty

![webflux-netty](./10k-vus-sharp-spikes-get-post-movies-call-depth-0/webflux-netty.png)


### 10k-vus-sharp-spikes-get-post-movies-call-depth-1

#### loom-tomcat

![loom-tomcat](./10k-vus-sharp-spikes-get-post-movies-call-depth-1/loom-tomcat.png)

#### loom-netty

![loom-netty](./10k-vus-sharp-spikes-get-post-movies-call-depth-1/loom-netty.png)

#### webflux-netty

![webflux-netty](./10k-vus-sharp-spikes-get-post-movies-call-depth-1/webflux-netty.png)


### 20k-vus-sharp-spikes-get-post-movies-call-depth-0

#### loom-tomcat

![loom-tomcat](./20k-vus-sharp-spikes-get-post-movies-call-depth-0/loom-tomcat.png)

#### loom-netty

![loom-netty](./20k-vus-sharp-spikes-get-post-movies-call-depth-0/loom-netty.png)

#### webflux-netty

![webflux-netty](./20k-vus-sharp-spikes-get-post-movies-call-depth-0/webflux-netty.png)


### 20k-vus-sharp-spikes-get-post-movies-call-depth-1

#### loom-tomcat

![loom-tomcat](./20k-vus-sharp-spikes-get-post-movies-call-depth-1/loom-tomcat.png)

#### loom-netty

![loom-netty](./20k-vus-sharp-spikes-get-post-movies-call-depth-1/loom-netty.png)

#### webflux-netty

![webflux-netty](./20k-vus-sharp-spikes-get-post-movies-call-depth-1/webflux-netty.png)


### 30k-vus-sharp-spikes-get-post-movies-call-depth-0

#### loom-tomcat

![loom-tomcat](./30k-vus-sharp-spikes-get-post-movies-call-depth-0/loom-tomcat.png)

#### loom-netty

![loom-netty](./30k-vus-sharp-spikes-get-post-movies-call-depth-0/loom-netty.png)

#### webflux-netty

![webflux-netty](./30k-vus-sharp-spikes-get-post-movies-call-depth-0/webflux-netty.png)


### 30k-vus-sharp-spikes-get-post-movies-call-depth-1

#### loom-tomcat

![loom-tomcat](./30k-vus-sharp-spikes-get-post-movies-call-depth-1/loom-tomcat.png)

#### loom-netty

![loom-netty](./30k-vus-sharp-spikes-get-post-movies-call-depth-1/loom-netty.png)

#### webflux-netty

![webflux-netty](./30k-vus-sharp-spikes-get-post-movies-call-depth-1/webflux-netty.png)


