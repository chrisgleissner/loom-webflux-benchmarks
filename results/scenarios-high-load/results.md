# scenarios-high-load

## Test Time

| **Name**                | **Value** |
|-------------------------|-----------|
| **Start (UTC)** | 2025-02-10 02:18:30 |
| **End (UTC)** | 2025-02-10 02:52:49 |
| **Duration (hh:mm:ss)** | 00:34:19 |

## System Specs

| **Name**                | **Value** |
|-------------------------|-----------|
| **Java** | OpenJDK 64-Bit Server VM Corretto-21.0.6.7.1 (build 21.0.6+7-LTS, mixed mode, sharing) |
| **Spring Boot** | 3.4.2 |
| **Python** | 3.12.3 |
| **OS** | Ubuntu 24.04.1 LTS |
| **Kernel** | 6.8.0-52-generic |
| **CPU** | Intel(R) Core(TM) i5-14600K |
| **CPU Cores** | 20 |
| **RAM** | 62Gi total, 53Gi available |
| **Disk** | 544G total, 309G available |

## Scenarios

**Scenario file:** src/main/resources/scenarios/scenarios-high-load.csv

| Scenario | k6 Config | Server Profiles | Delay Call Depth | Delay (ms) | Connections | Requests per Second | Warmup Duration (s) | Test Duration (s) |
|----------|-----------|-----------------|------------------|------------|-------------|---------------------|---------------------|------------------|
| [60k-vus-smooth-spike-get-post-movies](#60k-vus-smooth-spike-get-post-movies) | get-post-movies-smooth-vus-spike.js |  | 0 | 100 | 60000 |  | 0 | 300 |
| [60k-vus-and-rps-get-movies](#60k-vus-and-rps-get-movies) | get-movies.js |  | 0 | 100 | 60000 | 60000 | 0 | 300 |

## Result Overview

### Overall

![Overall Results](./results.png)
### Netty-based

![Netty Results](./results-netty.png)

## Result Details


### 60k-vus-smooth-spike-get-post-movies

#### loom-tomcat

![loom-tomcat](./60k-vus-smooth-spike-get-post-movies/loom-tomcat.png)

#### loom-netty

![loom-netty](./60k-vus-smooth-spike-get-post-movies/loom-netty.png)

#### webflux-netty

![webflux-netty](./60k-vus-smooth-spike-get-post-movies/webflux-netty.png)


### 60k-vus-and-rps-get-movies

#### loom-tomcat

![loom-tomcat](./60k-vus-and-rps-get-movies/loom-tomcat.png)

#### loom-netty

![loom-netty](./60k-vus-and-rps-get-movies/loom-netty.png)

#### webflux-netty

![webflux-netty](./60k-vus-and-rps-get-movies/webflux-netty.png)


