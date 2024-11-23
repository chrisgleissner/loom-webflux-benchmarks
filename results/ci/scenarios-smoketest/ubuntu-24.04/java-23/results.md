# scenarios-smoketest

## Test Time

| **Name**                | **Value** |
|-------------------------|-----------|
| **Start (UTC)** | 2024-11-22 16:47:15 |
| **End (UTC)** | 2024-11-22 16:52:46 |
| **Duration (hh:mm:ss)** | 00:05:31 |

## System Specs

| **Name**                | **Value** |
|-------------------------|-----------|
| **Java** | OpenJDK 64-Bit Server VM Corretto-23.0.1.8.1 (build 23.0.1+8-FR, mixed mode, sharing) |
| **Spring Boot** | 3.4.0 |
| **Python** | 3.12.3 |
| **OS** | Ubuntu 24.04.1 LTS |
| **Kernel** | 6.8.0-1017-azure |
| **CPU** | AMD EPYC 7763 64-Core Processor |
| **CPU Cores** | 4 |
| **RAM** | 15Gi total, 12Gi available |
| **Disk** | 159G total, 104G available |

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

#### platform-tomcat

![platform-tomcat](./smoketest-get-time/platform-tomcat.png)

#### loom-tomcat

![loom-tomcat](./smoketest-get-time/loom-tomcat.png)

#### loom-netty

![loom-netty](./smoketest-get-time/loom-netty.png)

#### webflux-netty

![webflux-netty](./smoketest-get-time/webflux-netty.png)


### smoketest-get-movies-h2

#### platform-tomcat

![platform-tomcat](./smoketest-get-movies-h2/platform-tomcat.png)

#### loom-tomcat

![loom-tomcat](./smoketest-get-movies-h2/loom-tomcat.png)

#### loom-netty

![loom-netty](./smoketest-get-movies-h2/loom-netty.png)

#### webflux-netty

![webflux-netty](./smoketest-get-movies-h2/webflux-netty.png)


### smoketest-get-movies-postgres

#### platform-tomcat

![platform-tomcat](./smoketest-get-movies-postgres/platform-tomcat.png)

#### loom-tomcat

![loom-tomcat](./smoketest-get-movies-postgres/loom-tomcat.png)

#### loom-netty

![loom-netty](./smoketest-get-movies-postgres/loom-netty.png)

#### webflux-netty

![webflux-netty](./smoketest-get-movies-postgres/webflux-netty.png)


### smoketest-get-movies-postgres-no-cache

#### platform-tomcat

![platform-tomcat](./smoketest-get-movies-postgres-no-cache/platform-tomcat.png)

#### loom-tomcat

![loom-tomcat](./smoketest-get-movies-postgres-no-cache/loom-tomcat.png)

#### loom-netty

![loom-netty](./smoketest-get-movies-postgres-no-cache/loom-netty.png)

#### webflux-netty

![webflux-netty](./smoketest-get-movies-postgres-no-cache/webflux-netty.png)


