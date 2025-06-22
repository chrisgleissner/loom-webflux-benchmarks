# scenarios-default

## Test Time

| **Name**                | **Value** |
|-------------------------|-----------|
| **Start (UTC)** | 2025-06-21 10:09:41 |
| **End (UTC)** | 2025-06-21 11:51:07 |
| **Duration (hh:mm:ss)** | 01:41:26 |

## System Specs

| **Name**                | **Value** |
|-------------------------|-----------|
| **Java** | OpenJDK 64-Bit Server VM Corretto-21.0.7.6.1 (build 21.0.7+6-LTS, mixed mode, sharing) |
| **Spring Boot** | 3.5.3 |
| **Python** | 3.12.3 |
| **OS** | Ubuntu 24.04.2 LTS |
| **Kernel** | 6.11.0-26-generic |
| **CPU** | Intel(R) Core(TM) i5-14600K |
| **CPU Cores** | 20 |
| **RAM** | 62Gi total, 53Gi available |
| **Disk** | 1023G total, 610G available |

## Scenarios

**Scenario file:** src/main/resources/scenarios/scenarios-default.csv

| Scenario | k6 Config | Server Profiles | Delay Call Depth | Delay (ms) | Connections | Requests per Second | Warmup Duration (s) | Test Duration (s) |
|----------|-----------|-----------------|------------------|------------|-------------|---------------------|---------------------|------------------|
| [smoketest](#smoketest) | get-time.js |  | 1 | 100 | 5 | 5 | 0 | 5 |
| [1k-vus-and-rps-get-time-no-delay](#1k-vus-and-rps-get-time-no-delay) | get-time.js |  | 0 | 0 | 1000 | 1000 | 10 | 180 |
| [5k-vus-and-rps-get-time](#5k-vus-and-rps-get-time) | get-time.js |  | 0 | 100 | 5000 | 5000 | 10 | 180 |
| [5k-vus-and-rps-get-movies](#5k-vus-and-rps-get-movies) | get-movies.js |  | 0 | 100 | 5000 | 5000 | 10 | 180 |
| [10k-vus-and-rps-get-movies](#10k-vus-and-rps-get-movies) | get-movies.js |  | 0 | 100 | 10000 | 10000 | 10 | 180 |
| [10k-vus-and-rps-get-movies-call-depth-1](#10k-vus-and-rps-get-movies-call-depth-1) | get-movies.js |  | 1 | 100 | 10000 | 10000 | 10 | 180 |
| [20k-vus-stepped-spike-get-movies](#20k-vus-stepped-spike-get-movies) | get-movies-stepped-vus-spike.js |  | 0 | 100 | 20000 |  | 0 | 180 |
| [20k-vus-smooth-spike-get-movies](#20k-vus-smooth-spike-get-movies) | get-movies-smooth-vus-spike.js |  | 0 | 100 | 20000 |  | 0 | 180 |
| [20k-vus-smooth-spike-get-post-movies](#20k-vus-smooth-spike-get-post-movies) | get-post-movies-smooth-vus-spike.js |  | 0 | 100 | 20000 |  | 0 | 180 |
| [20k-vus-smooth-spike-get-post-movies-call-depth-1](#20k-vus-smooth-spike-get-post-movies-call-depth-1) | get-post-movies-smooth-vus-spike.js |  | 1 | 100 | 20000 |  | 0 | 180 |
| [20k-vus-smooth-spike-get-post-movies-call-depth-2](#20k-vus-smooth-spike-get-post-movies-call-depth-2) | get-post-movies-smooth-vus-spike.js |  | 2 | 100 | 20000 |  | 0 | 180 |

## Result Overview

### Overall

![Overall Results](./results.png)
### Netty-based

![Netty Results](./results-netty.png)

## Result Details


### smoketest

#### loom-tomcat

![loom-tomcat](./smoketest/loom-tomcat.png)

#### loom-netty

![loom-netty](./smoketest/loom-netty.png)

#### webflux-netty

![webflux-netty](./smoketest/webflux-netty.png)


### 1k-vus-and-rps-get-time-no-delay

#### loom-tomcat

![loom-tomcat](./1k-vus-and-rps-get-time-no-delay/loom-tomcat.png)

#### loom-netty

![loom-netty](./1k-vus-and-rps-get-time-no-delay/loom-netty.png)

#### webflux-netty

![webflux-netty](./1k-vus-and-rps-get-time-no-delay/webflux-netty.png)


### 5k-vus-and-rps-get-time

#### loom-tomcat

![loom-tomcat](./5k-vus-and-rps-get-time/loom-tomcat.png)

#### loom-netty

![loom-netty](./5k-vus-and-rps-get-time/loom-netty.png)

#### webflux-netty

![webflux-netty](./5k-vus-and-rps-get-time/webflux-netty.png)


### 5k-vus-and-rps-get-movies

#### loom-tomcat

![loom-tomcat](./5k-vus-and-rps-get-movies/loom-tomcat.png)

#### loom-netty

![loom-netty](./5k-vus-and-rps-get-movies/loom-netty.png)

#### webflux-netty

![webflux-netty](./5k-vus-and-rps-get-movies/webflux-netty.png)


### 10k-vus-and-rps-get-movies

#### loom-tomcat

![loom-tomcat](./10k-vus-and-rps-get-movies/loom-tomcat.png)

#### loom-netty

![loom-netty](./10k-vus-and-rps-get-movies/loom-netty.png)

#### webflux-netty

![webflux-netty](./10k-vus-and-rps-get-movies/webflux-netty.png)


### 10k-vus-and-rps-get-movies-call-depth-1

#### loom-tomcat

![loom-tomcat](./10k-vus-and-rps-get-movies-call-depth-1/loom-tomcat.png)

#### loom-netty

![loom-netty](./10k-vus-and-rps-get-movies-call-depth-1/loom-netty.png)

#### webflux-netty

![webflux-netty](./10k-vus-and-rps-get-movies-call-depth-1/webflux-netty.png)


### 20k-vus-stepped-spike-get-movies

#### loom-tomcat

![loom-tomcat](./20k-vus-stepped-spike-get-movies/loom-tomcat.png)

#### loom-netty

![loom-netty](./20k-vus-stepped-spike-get-movies/loom-netty.png)

#### webflux-netty

![webflux-netty](./20k-vus-stepped-spike-get-movies/webflux-netty.png)


### 20k-vus-smooth-spike-get-movies

#### loom-tomcat

![loom-tomcat](./20k-vus-smooth-spike-get-movies/loom-tomcat.png)

#### loom-netty

![loom-netty](./20k-vus-smooth-spike-get-movies/loom-netty.png)

#### webflux-netty

![webflux-netty](./20k-vus-smooth-spike-get-movies/webflux-netty.png)


### 20k-vus-smooth-spike-get-post-movies

#### loom-tomcat

![loom-tomcat](./20k-vus-smooth-spike-get-post-movies/loom-tomcat.png)

#### loom-netty

![loom-netty](./20k-vus-smooth-spike-get-post-movies/loom-netty.png)

#### webflux-netty

![webflux-netty](./20k-vus-smooth-spike-get-post-movies/webflux-netty.png)


### 20k-vus-smooth-spike-get-post-movies-call-depth-1

#### loom-tomcat

![loom-tomcat](./20k-vus-smooth-spike-get-post-movies-call-depth-1/loom-tomcat.png)

#### loom-netty

![loom-netty](./20k-vus-smooth-spike-get-post-movies-call-depth-1/loom-netty.png)

#### webflux-netty

![webflux-netty](./20k-vus-smooth-spike-get-post-movies-call-depth-1/webflux-netty.png)


### 20k-vus-smooth-spike-get-post-movies-call-depth-2

#### loom-tomcat

![loom-tomcat](./20k-vus-smooth-spike-get-post-movies-call-depth-2/loom-tomcat.png)

#### loom-netty

![loom-netty](./20k-vus-smooth-spike-get-post-movies-call-depth-2/loom-netty.png)

#### webflux-netty

![webflux-netty](./20k-vus-smooth-spike-get-post-movies-call-depth-2/webflux-netty.png)


