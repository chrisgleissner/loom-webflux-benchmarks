# scenarios-postgres

## Test Time

| **Name**                | **Value** |
|-------------------------|-----------|
| **Start (UTC)** | 2024-11-22 20:15:56 |
| **End (UTC)** | 2024-11-22 21:25:33 |
| **Duration (hh:mm:ss)** | 01:09:37 |

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
| **RAM** | 31Gi total, 27Gi available |
| **Disk** | 506G total, 275G available |

## Scenarios

**Scenario file:** src/main/resources/scenarios/scenarios-postgres.csv

| Scenario | k6 Config | Server Profiles | Delay Call Depth | Delay (ms) | Connections | Requests per Second | Warmup Duration (s) | Test Duration (s) |
|----------|-----------|-----------------|------------------|------------|-------------|---------------------|---------------------|------------------|
| [10-vus-and-rps-get-movies-postgres-no-cache](#10-vus-and-rps-get-movies-postgres-no-cache) | get-movies.js | postgres|no-cache | 0 | 100 | 10 | 10 | 10 | 300 |
| [20-vus-and-rps-get-movies-postgres-no-cache](#20-vus-and-rps-get-movies-postgres-no-cache) | get-movies.js | postgres|no-cache | 0 | 100 | 20 | 20 | 10 | 300 |
| [40-vus-and-rps-get-movies-postgres-no-cache](#40-vus-and-rps-get-movies-postgres-no-cache) | get-movies.js | postgres|no-cache | 0 | 100 | 40 | 40 | 10 | 300 |
| [80-vus-and-rps-get-movies-postgres-no-cache](#80-vus-and-rps-get-movies-postgres-no-cache) | get-movies.js | postgres|no-cache | 0 | 100 | 80 | 80 | 10 | 300 |

## Result Overview

### Overall

![Overall Results](./results.png)
### Netty-based

![Netty Results](./results-netty.png)

## Result Details


### 10-vus-and-rps-get-movies-postgres-no-cache

#### loom-tomcat

![loom-tomcat](./10-vus-and-rps-get-movies-postgres-no-cache/loom-tomcat.png)

#### loom-netty

![loom-netty](./10-vus-and-rps-get-movies-postgres-no-cache/loom-netty.png)

#### webflux-netty

![webflux-netty](./10-vus-and-rps-get-movies-postgres-no-cache/webflux-netty.png)


### 20-vus-and-rps-get-movies-postgres-no-cache

#### loom-tomcat

![loom-tomcat](./20-vus-and-rps-get-movies-postgres-no-cache/loom-tomcat.png)

#### loom-netty

![loom-netty](./20-vus-and-rps-get-movies-postgres-no-cache/loom-netty.png)

#### webflux-netty

![webflux-netty](./20-vus-and-rps-get-movies-postgres-no-cache/webflux-netty.png)


### 40-vus-and-rps-get-movies-postgres-no-cache

#### loom-tomcat

![loom-tomcat](./40-vus-and-rps-get-movies-postgres-no-cache/loom-tomcat.png)

#### loom-netty

![loom-netty](./40-vus-and-rps-get-movies-postgres-no-cache/loom-netty.png)

#### webflux-netty

![webflux-netty](./40-vus-and-rps-get-movies-postgres-no-cache/webflux-netty.png)


### 80-vus-and-rps-get-movies-postgres-no-cache

#### loom-tomcat

![loom-tomcat](./80-vus-and-rps-get-movies-postgres-no-cache/loom-tomcat.png)

#### loom-netty

![loom-netty](./80-vus-and-rps-get-movies-postgres-no-cache/loom-netty.png)

#### webflux-netty

![webflux-netty](./80-vus-and-rps-get-movies-postgres-no-cache/webflux-netty.png)


