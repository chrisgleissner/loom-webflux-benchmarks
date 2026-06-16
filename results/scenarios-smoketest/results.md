# scenarios-smoketest

## Test Time

| **Name**                | **Value** |
|-------------------------|-----------|
| **Start (UTC)** | 2026-06-12 21:37:08 |
| **End (UTC)** | 2026-06-12 21:40:55 |
| **Duration (hh:mm:ss)** | 00:03:47 |

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
| **RAM** | 62Gi total, 59Gi available |
| **Disk** | 2.8T total, 1.5T available |

## Scenarios

**Scenario file:** src/main/resources/scenarios/scenarios-smoketest.csv

| Scenario | k6 Config | Server Profiles | Delay Call Depth | Delay (ms) | Connections | Requests per Second | Warmup Duration (s) | Test Duration (s) |
|----------|-----------|-----------------|------------------|------------|-------------|---------------------|---------------------|------------------|
| [smoketest-get-time](#smoketest-get-time) | get-time.js |  | 0 | 0 | 100 | 200 | 2 | 6 |
| [smoketest-get-movies-h2](#smoketest-get-movies-h2) | get-movies.js |  | 1 | 100 | 10 | 10 | 0 | 6 |
| [smoketest-get-movies-postgres](#smoketest-get-movies-postgres) | get-movies.js | postgres | 1 | 100 | 10 | 10 | 0 | 6 |
| [smoketest-get-movies-postgres-no-cache](#smoketest-get-movies-postgres-no-cache) | get-movies.js | postgres|no-cache | 1 | 100 | 10 | 10 | 0 | 6 |

## Result Overview

### Overall

![Overall Results](./results.png)
### Netty-based

![Netty Results](./results-netty.png)

## Result Details


### smoketest-get-time

#### loom-tomcat

![loom-tomcat](./smoketest-get-time/loom-tomcat.png)

#### loom-netty

![loom-netty](./smoketest-get-time/loom-netty.png)

#### webflux-netty

![webflux-netty](./smoketest-get-time/webflux-netty.png)


### smoketest-get-movies-h2

#### loom-tomcat

![loom-tomcat](./smoketest-get-movies-h2/loom-tomcat.png)

#### loom-netty

![loom-netty](./smoketest-get-movies-h2/loom-netty.png)

#### webflux-netty

![webflux-netty](./smoketest-get-movies-h2/webflux-netty.png)


### smoketest-get-movies-postgres

#### loom-tomcat

![loom-tomcat](./smoketest-get-movies-postgres/loom-tomcat.png)

#### loom-netty

![loom-netty](./smoketest-get-movies-postgres/loom-netty.png)

#### webflux-netty

![webflux-netty](./smoketest-get-movies-postgres/webflux-netty.png)


### smoketest-get-movies-postgres-no-cache

#### loom-tomcat

![loom-tomcat](./smoketest-get-movies-postgres-no-cache/loom-tomcat.png)

#### loom-netty

![loom-netty](./smoketest-get-movies-postgres-no-cache/loom-netty.png)

#### webflux-netty

![webflux-netty](./smoketest-get-movies-postgres-no-cache/webflux-netty.png)


