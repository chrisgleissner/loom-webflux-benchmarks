# scenarios-postgres

## Test Time

| **Name**                | **Value** |
|-------------------------|-----------|
| **Start (UTC)** | 2025-09-18 16:27:38 |
| **End (UTC)** | 2025-09-18 17:31:59 |
| **Duration (hh:mm:ss)** | 01:04:21 |

## System Specs

| **Name**                | **Value** |
|-------------------------|-----------|
| **Java** | OpenJDK 64-Bit Server VM Corretto-25.0.0.36.2 (build 25+36-LTS, mixed mode, sharing) |
| **Spring Boot** | 3.5.5 |
| **Python** | 3.12.3 |
| **OS** | Ubuntu 24.04.3 LTS |
| **Kernel** | 6.14.0-29-generic |
| **CPU** | Intel(R) Core(TM) i5-14600K |
| **CPU Cores** | 20 |
| **RAM** | 62Gi total, 57Gi available |
| **Disk** | 1023G total, 614G available |

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


