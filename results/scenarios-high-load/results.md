# scenarios-high-load

## Test Time

| **Name**                | **Value** |
|-------------------------|-----------|
| **Start (UTC)** | 2025-03-06 11:32:01 |
| **End (UTC)** | 2025-03-06 12:39:33 |
| **Duration (hh:mm:ss)** | 01:07:32 |

## System Specs

| **Name**                | **Value** |
|-------------------------|-----------|
| **Java** | OpenJDK 64-Bit Server VM Corretto-21.0.6.7.1 (build 21.0.6+7-LTS, mixed mode, sharing) |
| **Spring Boot** | 3.4.3 |
| **Python** | 3.12.3 |
| **OS** | Ubuntu 24.04.2 LTS |
| **Kernel** | 6.11.0-19-generic |
| **CPU** | Intel(R) Core(TM) i5-14600K |
| **CPU Cores** | 20 |
| **RAM** | 62Gi total, 55Gi available |
| **Disk** | 1023G total, 640G available |

## Scenarios

**Scenario file:** src/main/resources/scenarios/scenarios-high-load.csv

| Scenario | k6 Config | Server Profiles | Delay Call Depth | Delay (ms) | Connections | Requests per Second | Warmup Duration (s) | Test Duration (s) |
|----------|-----------|-----------------|------------------|------------|-------------|---------------------|---------------------|------------------|
| [40k-vus-and-rps-get-time-no-delay](#40k-vus-and-rps-get-time-no-delay) | get-time.js |  | 0 | 0 | 40000 | 40000 | 10 | 180 |
| [40k-vus-and-rps-get-time](#40k-vus-and-rps-get-time) | get-time.js |  | 0 | 100 | 40000 | 40000 | 10 | 180 |
| [40k-vus-and-rps-get-movies](#40k-vus-and-rps-get-movies) | get-movies.js |  | 0 | 100 | 40000 | 40000 | 10 | 180 |
| [60k-vus-stepped-spike-get-movies](#60k-vus-stepped-spike-get-movies) | get-movies-stepped-vus-spike.js |  | 0 | 100 | 60000 |  | 0 | 180 |
| [60k-vus-smooth-spike-get-movies](#60k-vus-smooth-spike-get-movies) | get-movies-smooth-vus-spike.js |  | 0 | 100 | 60000 |  | 0 | 180 |
| [60k-vus-smooth-spike-get-post-movies](#60k-vus-smooth-spike-get-post-movies) | get-post-movies-smooth-vus-spike.js |  | 0 | 100 | 60000 |  | 0 | 180 |

## Result Overview

### Overall

![Overall Results](./results.png)
### Netty-based

![Netty Results](./results-netty.png)

## Result Details


### 40k-vus-and-rps-get-time-no-delay

#### loom-tomcat

![loom-tomcat](./40k-vus-and-rps-get-time-no-delay/loom-tomcat.png)

#### loom-netty

![loom-netty](./40k-vus-and-rps-get-time-no-delay/loom-netty.png)

#### webflux-netty

![webflux-netty](./40k-vus-and-rps-get-time-no-delay/webflux-netty.png)


### 40k-vus-and-rps-get-time

#### loom-tomcat

![loom-tomcat](./40k-vus-and-rps-get-time/loom-tomcat.png)

#### loom-netty

![loom-netty](./40k-vus-and-rps-get-time/loom-netty.png)

#### webflux-netty

![webflux-netty](./40k-vus-and-rps-get-time/webflux-netty.png)


### 40k-vus-and-rps-get-movies

#### loom-tomcat

![loom-tomcat](./40k-vus-and-rps-get-movies/loom-tomcat.png)

#### loom-netty

![loom-netty](./40k-vus-and-rps-get-movies/loom-netty.png)

#### webflux-netty

![webflux-netty](./40k-vus-and-rps-get-movies/webflux-netty.png)


### 60k-vus-stepped-spike-get-movies

#### loom-tomcat

![loom-tomcat](./60k-vus-stepped-spike-get-movies/loom-tomcat.png)

#### loom-netty

![loom-netty](./60k-vus-stepped-spike-get-movies/loom-netty.png)

#### webflux-netty

![webflux-netty](./60k-vus-stepped-spike-get-movies/webflux-netty.png)


### 60k-vus-smooth-spike-get-movies

#### loom-tomcat

![loom-tomcat](./60k-vus-smooth-spike-get-movies/loom-tomcat.png)

#### loom-netty

![loom-netty](./60k-vus-smooth-spike-get-movies/loom-netty.png)

#### webflux-netty

![webflux-netty](./60k-vus-smooth-spike-get-movies/webflux-netty.png)


### 60k-vus-smooth-spike-get-post-movies

#### loom-tomcat

![loom-tomcat](./60k-vus-smooth-spike-get-post-movies/loom-tomcat.png)

#### loom-netty

![loom-netty](./60k-vus-smooth-spike-get-post-movies/loom-netty.png)

#### webflux-netty

![webflux-netty](./60k-vus-smooth-spike-get-post-movies/webflux-netty.png)


