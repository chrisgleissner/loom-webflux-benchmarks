# scenarios-clients

## Test Time

| **Name**                | **Value** |
|-------------------------|-----------|
| **Start (UTC)** | 2025-03-05 11:27:55 |
| **End (UTC)** | 2025-03-05 13:21:10 |
| **Duration (hh:mm:ss)** | 01:53:15 |

## System Specs

| **Name**                | **Value** |
|-------------------------|-----------|
| **Java** | OpenJDK 64-Bit Server VM Corretto-21.0.6.7.1 (build 21.0.6+7-LTS, mixed mode, sharing) |
| **Spring Boot** | 3.4.3 |
| **Python** | 3.12.3 |
| **OS** | Ubuntu 24.04.2 LTS |
| **Kernel** | 6.11.0-17-generic |
| **CPU** | Intel(R) Core(TM) i5-14600K |
| **CPU Cores** | 20 |
| **RAM** | 62Gi total, 55Gi available |
| **Disk** | 1023G total, 640G available |

## Scenarios

**Scenario file:** src/main/resources/scenarios/scenarios-clients.csv

| Scenario | k6 Config | Server Profiles | Delay Call Depth | Delay (ms) | Connections | Requests per Second | Warmup Duration (s) | Test Duration (s) |
|----------|-----------|-----------------|------------------|------------|-------------|---------------------|---------------------|------------------|
| [rest-client-jdk-5k-vus-smooth-spike-get-post-movies-call-depth-1](#rest-client-jdk-5k-vus-smooth-spike-get-post-movies-call-depth-1) | get-post-movies-smooth-vus-spike.js | rest-client-jdk | 1 | 100 | 5000 |  | 0 | 180 |
| [rest-client-apache5-5k-vus-smooth-spike-get-post-movies-call-depth-1](#rest-client-apache5-5k-vus-smooth-spike-get-post-movies-call-depth-1) | get-post-movies-smooth-vus-spike.js | rest-client-apache5 | 1 | 100 | 5000 |  | 0 | 180 |
| [rest-client-reactor-netty-5k-vus-smooth-spike-get-post-movies-call-depth-1](#rest-client-reactor-netty-5k-vus-smooth-spike-get-post-movies-call-depth-1) | get-post-movies-smooth-vus-spike.js | rest-client-reactor-netty | 1 | 100 | 5000 |  | 0 | 180 |
| [web-client-reactor-netty-5k-vus-smooth-spike-get-post-movies-call-depth-1](#web-client-reactor-netty-5k-vus-smooth-spike-get-post-movies-call-depth-1) | get-post-movies-smooth-vus-spike.js |  | 1 | 100 | 5000 |  | 0 | 180 |
| [rest-client-jdk-5k-vus-smooth-spike-get-post-movies-call-depth-2](#rest-client-jdk-5k-vus-smooth-spike-get-post-movies-call-depth-2) | get-post-movies-smooth-vus-spike.js | rest-client-jdk | 2 | 100 | 5000 |  | 0 | 180 |
| [rest-client-apache5-5k-vus-smooth-spike-get-post-movies-call-depth-2](#rest-client-apache5-5k-vus-smooth-spike-get-post-movies-call-depth-2) | get-post-movies-smooth-vus-spike.js | rest-client-apache5 | 2 | 100 | 5000 |  | 0 | 180 |
| [rest-client-reactor-netty-5k-vus-smooth-spike-get-post-movies-call-depth-2](#rest-client-reactor-netty-5k-vus-smooth-spike-get-post-movies-call-depth-2) | get-post-movies-smooth-vus-spike.js | rest-client-reactor-netty | 2 | 100 | 5000 |  | 0 | 180 |
| [web-client-reactor-netty-5k-vus-smooth-spike-get-post-movies-call-depth-2](#web-client-reactor-netty-5k-vus-smooth-spike-get-post-movies-call-depth-2) | get-post-movies-smooth-vus-spike.js |  | 2 | 100 | 5000 |  | 0 | 180 |
| [rest-client-jdk-5k-vus-smooth-spike-get-post-movies-call-depth-5](#rest-client-jdk-5k-vus-smooth-spike-get-post-movies-call-depth-5) | get-post-movies-smooth-vus-spike.js | rest-client-jdk | 5 | 100 | 5000 |  | 0 | 180 |
| [rest-client-apache5-5k-vus-smooth-spike-get-post-movies-call-depth-5](#rest-client-apache5-5k-vus-smooth-spike-get-post-movies-call-depth-5) | get-post-movies-smooth-vus-spike.js | rest-client-apache5 | 5 | 100 | 5000 |  | 0 | 180 |
| [rest-client-reactor-netty-5k-vus-smooth-spike-get-post-movies-call-depth-5](#rest-client-reactor-netty-5k-vus-smooth-spike-get-post-movies-call-depth-5) | get-post-movies-smooth-vus-spike.js | rest-client-reactor-netty | 5 | 100 | 5000 |  | 0 | 180 |
| [web-client-reactor-netty-5k-vus-smooth-spike-get-post-movies-call-depth-5](#web-client-reactor-netty-5k-vus-smooth-spike-get-post-movies-call-depth-5) | get-post-movies-smooth-vus-spike.js |  | 5 | 100 | 5000 |  | 0 | 180 |

## Result Overview

### Overall

![Overall Results](./results.png)
### Netty-based

![Netty Results](./results-netty.png)

## Result Details


### rest-client-jdk-5k-vus-smooth-spike-get-post-movies-call-depth-1

#### loom-tomcat

![loom-tomcat](./rest-client-jdk-5k-vus-smooth-spike-get-post-movies-call-depth-1/loom-tomcat.png)

#### loom-netty

![loom-netty](./rest-client-jdk-5k-vus-smooth-spike-get-post-movies-call-depth-1/loom-netty.png)

#### webflux-netty

![webflux-netty](./rest-client-jdk-5k-vus-smooth-spike-get-post-movies-call-depth-1/webflux-netty.png)


### rest-client-apache5-5k-vus-smooth-spike-get-post-movies-call-depth-1

#### loom-tomcat

![loom-tomcat](./rest-client-apache5-5k-vus-smooth-spike-get-post-movies-call-depth-1/loom-tomcat.png)

#### loom-netty

![loom-netty](./rest-client-apache5-5k-vus-smooth-spike-get-post-movies-call-depth-1/loom-netty.png)

#### webflux-netty

![webflux-netty](./rest-client-apache5-5k-vus-smooth-spike-get-post-movies-call-depth-1/webflux-netty.png)


### rest-client-reactor-netty-5k-vus-smooth-spike-get-post-movies-call-depth-1

#### loom-tomcat

![loom-tomcat](./rest-client-reactor-netty-5k-vus-smooth-spike-get-post-movies-call-depth-1/loom-tomcat.png)

#### loom-netty

![loom-netty](./rest-client-reactor-netty-5k-vus-smooth-spike-get-post-movies-call-depth-1/loom-netty.png)

#### webflux-netty

![webflux-netty](./rest-client-reactor-netty-5k-vus-smooth-spike-get-post-movies-call-depth-1/webflux-netty.png)


### web-client-reactor-netty-5k-vus-smooth-spike-get-post-movies-call-depth-1

#### loom-tomcat

![loom-tomcat](./web-client-reactor-netty-5k-vus-smooth-spike-get-post-movies-call-depth-1/loom-tomcat.png)

#### loom-netty

![loom-netty](./web-client-reactor-netty-5k-vus-smooth-spike-get-post-movies-call-depth-1/loom-netty.png)

#### webflux-netty

![webflux-netty](./web-client-reactor-netty-5k-vus-smooth-spike-get-post-movies-call-depth-1/webflux-netty.png)


### rest-client-jdk-5k-vus-smooth-spike-get-post-movies-call-depth-2

#### loom-tomcat

![loom-tomcat](./rest-client-jdk-5k-vus-smooth-spike-get-post-movies-call-depth-2/loom-tomcat.png)

#### loom-netty

![loom-netty](./rest-client-jdk-5k-vus-smooth-spike-get-post-movies-call-depth-2/loom-netty.png)

#### webflux-netty

![webflux-netty](./rest-client-jdk-5k-vus-smooth-spike-get-post-movies-call-depth-2/webflux-netty.png)


### rest-client-apache5-5k-vus-smooth-spike-get-post-movies-call-depth-2

#### loom-tomcat

![loom-tomcat](./rest-client-apache5-5k-vus-smooth-spike-get-post-movies-call-depth-2/loom-tomcat.png)

#### loom-netty

![loom-netty](./rest-client-apache5-5k-vus-smooth-spike-get-post-movies-call-depth-2/loom-netty.png)

#### webflux-netty

![webflux-netty](./rest-client-apache5-5k-vus-smooth-spike-get-post-movies-call-depth-2/webflux-netty.png)


### rest-client-reactor-netty-5k-vus-smooth-spike-get-post-movies-call-depth-2

#### loom-tomcat

![loom-tomcat](./rest-client-reactor-netty-5k-vus-smooth-spike-get-post-movies-call-depth-2/loom-tomcat.png)

#### loom-netty

![loom-netty](./rest-client-reactor-netty-5k-vus-smooth-spike-get-post-movies-call-depth-2/loom-netty.png)

#### webflux-netty

![webflux-netty](./rest-client-reactor-netty-5k-vus-smooth-spike-get-post-movies-call-depth-2/webflux-netty.png)


### web-client-reactor-netty-5k-vus-smooth-spike-get-post-movies-call-depth-2

#### loom-tomcat

![loom-tomcat](./web-client-reactor-netty-5k-vus-smooth-spike-get-post-movies-call-depth-2/loom-tomcat.png)

#### loom-netty

![loom-netty](./web-client-reactor-netty-5k-vus-smooth-spike-get-post-movies-call-depth-2/loom-netty.png)

#### webflux-netty

![webflux-netty](./web-client-reactor-netty-5k-vus-smooth-spike-get-post-movies-call-depth-2/webflux-netty.png)


### rest-client-jdk-5k-vus-smooth-spike-get-post-movies-call-depth-5

#### loom-tomcat

![loom-tomcat](./rest-client-jdk-5k-vus-smooth-spike-get-post-movies-call-depth-5/loom-tomcat.png)

#### loom-netty

![loom-netty](./rest-client-jdk-5k-vus-smooth-spike-get-post-movies-call-depth-5/loom-netty.png)

#### webflux-netty

![webflux-netty](./rest-client-jdk-5k-vus-smooth-spike-get-post-movies-call-depth-5/webflux-netty.png)


### rest-client-apache5-5k-vus-smooth-spike-get-post-movies-call-depth-5

#### loom-tomcat

![loom-tomcat](./rest-client-apache5-5k-vus-smooth-spike-get-post-movies-call-depth-5/loom-tomcat.png)

#### loom-netty

![loom-netty](./rest-client-apache5-5k-vus-smooth-spike-get-post-movies-call-depth-5/loom-netty.png)

#### webflux-netty

![webflux-netty](./rest-client-apache5-5k-vus-smooth-spike-get-post-movies-call-depth-5/webflux-netty.png)


### rest-client-reactor-netty-5k-vus-smooth-spike-get-post-movies-call-depth-5

#### loom-tomcat

![loom-tomcat](./rest-client-reactor-netty-5k-vus-smooth-spike-get-post-movies-call-depth-5/loom-tomcat.png)

#### loom-netty

![loom-netty](./rest-client-reactor-netty-5k-vus-smooth-spike-get-post-movies-call-depth-5/loom-netty.png)

#### webflux-netty

![webflux-netty](./rest-client-reactor-netty-5k-vus-smooth-spike-get-post-movies-call-depth-5/webflux-netty.png)


### web-client-reactor-netty-5k-vus-smooth-spike-get-post-movies-call-depth-5

#### loom-tomcat

![loom-tomcat](./web-client-reactor-netty-5k-vus-smooth-spike-get-post-movies-call-depth-5/loom-tomcat.png)

#### loom-netty

![loom-netty](./web-client-reactor-netty-5k-vus-smooth-spike-get-post-movies-call-depth-5/loom-netty.png)

#### webflux-netty

![webflux-netty](./web-client-reactor-netty-5k-vus-smooth-spike-get-post-movies-call-depth-5/webflux-netty.png)


