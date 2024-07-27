# scenarios-smoketest

## Test Time

| **Name**                | **Value** |
|-------------------------|-----------|
| **Start Time (UTC)** | 2024-07-26 16:37:08 |
| **End Time (UTC)**   | 2024-07-26 16:42:39 |
| **Duration (hh:mm:ss)** | 00:05:31 |

## System Specs

| **Name**                | **Value** |
|-------------------------|-----------|
| **Java Version**        | OpenJDK 64-Bit Server VM Corretto-21.0.4.7.1 (build 21.0.4+7-LTS, mixed mode, sharing) |
| **Python Version**      | 3.12.3 |
| **OS Version**          | Ubuntu 24.04 LTS |
| **Kernel Version**      | 6.8.0-1010-azure |
| **CPU Model**           | AMD EPYC 7763 64-Core Processor |
| **CPU Cores**           | 4 |
| **RAM**                 | 15Gi total, 12Gi available |
| **Disk**                | 159G total, 108G available |

## Scenarios

**Scenario file:** src/main/resources/scenarios/scenarios-smoketest.csv

| Scenario | k6 Config | Server Profiles | Delay Call Depth | Delay (ms) | Connections | Requests per Second | Warmup Duration (s) | Test Duration (s) |
|----------|-----------|-----------------|------------------|------------|-------------|---------------------|---------------------|------------------|
| smoketest-get-time | get-time.js |  | 0 | 0 | 100 | 200 | 2 | 6 |
| smoketest-get-movies-h2 | get-movies.js |  | 1 | 100 | 10 | 10 | 0 | 6 |
| smoketest-get-movies-postgres | get-movies.js | postgres | 1 | 100 | 10 | 10 | 0 | 6 |
| smoketest-get-movies-postgres-no-cache | get-movies.js | postgres|no-cache | 1 | 100 | 10 | 10 | 0 | 6 |
