# scenarios-high-load

## Test Time

| **Name**                | **Value** |
|-------------------------|-----------|
| **Start (UTC)** | 2026-06-16 04:28:27 |
| **End (UTC)** | 2026-06-16 07:12:31 |
| **Duration (hh:mm:ss)** | 02:44:04 |

## System Specs

| **Name**                | **Value** |
|-------------------------|-----------|
| **Java** | OpenJDK 64-Bit Server VM Corretto-25.0.3.9.1 (build 25.0.3+9-LTS, mixed mode, sharing) |
| **Spring Boot** | 4.1.0 |
| **Python** | 3.12.3 |
| **OS** | Ubuntu 24.04.4 LTS |
| **Kernel** | 6.17.0-35-generic |
| **CPU** | Intel(R) Core(TM) i5-14600K |
| **CPU Cores** | 20 |
| **RAM** | 62Gi total, 57Gi available |
| **Disk** | 2.8T total, 1.5T available |

## Scenarios

**Scenario file:** src/main/resources/scenarios/scenarios-high-load.csv

| Scenario | k6 Config | Server Profiles | Delay Call Depth | Delay (ms) | Connections | Requests per Second | Warmup Duration (s) | Test Duration (s) |
|----------|-----------|-----------------|------------------|------------|-------------|---------------------|---------------------|------------------|
| [10k-vus-and-rps-get-time](#10k-vus-and-rps-get-time) | get-time.js |  | 0 | 100 | 10000 | 10000 | 10 | 180 |
| [20k-vus-and-rps-get-time](#20k-vus-and-rps-get-time) | get-time.js |  | 0 | 100 | 20000 | 20000 | 10 | 180 |
| [40k-vus-and-rps-get-time](#40k-vus-and-rps-get-time) | get-time.js |  | 0 | 100 | 40000 | 40000 | 10 | 180 |
| [60k-vus-and-rps-get-time](#60k-vus-and-rps-get-time) | get-time.js |  | 0 | 100 | 60000 | 60000 | 10 | 180 |
| [60k-vus-and-rps-get-time-no-delay](#60k-vus-and-rps-get-time-no-delay) | get-time.js |  | 0 | 0 | 60000 | 60000 | 10 | 180 |
| [10k-vus-and-rps-get-movies](#10k-vus-and-rps-get-movies) | get-movies.js |  | 0 | 100 | 10000 | 10000 | 10 | 180 |
| [20k-vus-and-rps-get-movies](#20k-vus-and-rps-get-movies) | get-movies.js |  | 0 | 100 | 20000 | 20000 | 10 | 180 |
| [40k-vus-and-rps-get-movies](#40k-vus-and-rps-get-movies) | get-movies.js |  | 0 | 100 | 40000 | 40000 | 10 | 180 |
| [60k-vus-and-rps-get-movies](#60k-vus-and-rps-get-movies) | get-movies.js |  | 0 | 100 | 60000 | 60000 | 10 | 180 |
| [60k-vus-stepped-spike-get-movies](#60k-vus-stepped-spike-get-movies) | get-movies-stepped-vus-spike.js |  | 0 | 100 | 60000 |  | 0 | 180 |
| [60k-vus-smooth-spike-get-movies](#60k-vus-smooth-spike-get-movies) | get-movies-smooth-vus-spike.js |  | 0 | 100 | 60000 |  | 0 | 180 |
| [60k-vus-smooth-spike-get-post-movies](#60k-vus-smooth-spike-get-post-movies) | get-post-movies-smooth-vus-spike.js |  | 0 | 100 | 60000 |  | 0 | 180 |

## Result Overview

### Overall

![Overall Results](./results.png)
### Netty-based

![Netty Results](./results-netty.png)

## Result Details


### 10k-vus-and-rps-get-time

#### loom-tomcat

![loom-tomcat](./10k-vus-and-rps-get-time/loom-tomcat.png)

#### loom-netty

![loom-netty](./10k-vus-and-rps-get-time/loom-netty.png)

#### webflux-netty

![webflux-netty](./10k-vus-and-rps-get-time/webflux-netty.png)


### 20k-vus-and-rps-get-time

#### loom-tomcat

![loom-tomcat](./20k-vus-and-rps-get-time/loom-tomcat.png)

#### loom-netty

![loom-netty](./20k-vus-and-rps-get-time/loom-netty.png)

#### webflux-netty

![webflux-netty](./20k-vus-and-rps-get-time/webflux-netty.png)


### 40k-vus-and-rps-get-time

#### loom-tomcat

![loom-tomcat](./40k-vus-and-rps-get-time/loom-tomcat.png)

#### loom-netty

![loom-netty](./40k-vus-and-rps-get-time/loom-netty.png)

#### webflux-netty

![webflux-netty](./40k-vus-and-rps-get-time/webflux-netty.png)


### 60k-vus-and-rps-get-time

#### loom-tomcat

![loom-tomcat](./60k-vus-and-rps-get-time/loom-tomcat.png)

#### loom-netty

![loom-netty](./60k-vus-and-rps-get-time/loom-netty.png)

#### webflux-netty

![webflux-netty](./60k-vus-and-rps-get-time/webflux-netty.png)


### 60k-vus-and-rps-get-time-no-delay

#### loom-tomcat

![loom-tomcat](./60k-vus-and-rps-get-time-no-delay/loom-tomcat.png)

#### loom-netty

![loom-netty](./60k-vus-and-rps-get-time-no-delay/loom-netty.png)

#### webflux-netty

![webflux-netty](./60k-vus-and-rps-get-time-no-delay/webflux-netty.png)


### 10k-vus-and-rps-get-movies

#### loom-tomcat

![loom-tomcat](./10k-vus-and-rps-get-movies/loom-tomcat.png)

#### loom-netty

![loom-netty](./10k-vus-and-rps-get-movies/loom-netty.png)

#### webflux-netty

![webflux-netty](./10k-vus-and-rps-get-movies/webflux-netty.png)


### 20k-vus-and-rps-get-movies

#### loom-tomcat

![loom-tomcat](./20k-vus-and-rps-get-movies/loom-tomcat.png)

#### loom-netty

![loom-netty](./20k-vus-and-rps-get-movies/loom-netty.png)

#### webflux-netty

![webflux-netty](./20k-vus-and-rps-get-movies/webflux-netty.png)


### 40k-vus-and-rps-get-movies

#### loom-tomcat

![loom-tomcat](./40k-vus-and-rps-get-movies/loom-tomcat.png)

#### loom-netty

![loom-netty](./40k-vus-and-rps-get-movies/loom-netty.png)

#### webflux-netty

![webflux-netty](./40k-vus-and-rps-get-movies/webflux-netty.png)


### 60k-vus-and-rps-get-movies

#### loom-tomcat

![loom-tomcat](./60k-vus-and-rps-get-movies/loom-tomcat.png)

#### loom-netty

![loom-netty](./60k-vus-and-rps-get-movies/loom-netty.png)

#### webflux-netty

![webflux-netty](./60k-vus-and-rps-get-movies/webflux-netty.png)


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


