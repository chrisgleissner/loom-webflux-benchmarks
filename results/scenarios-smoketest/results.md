# scenarios-smoketest

## Test Time

| **Name**                | **Value** |
|-------------------------|-----------|
| **Start Time (UTC)** | 2024-07-26 16:29:36 |
| **End Time (UTC)**   | 2024-07-26 16:31:26 |
| **Duration (hh:mm:ss)** | 00:01:50 |

## System Specs

| **Name**                | **Value** |
|-------------------------|-----------|
| **Java Version**        | OpenJDK 64-Bit Server VM Corretto-21.0.4.7.1 (build 21.0.4+7-LTS, mixed mode, sharing) |
| **Python Version**      | 3.10.12 |
| **OS Version**          | Ubuntu 22.04.4 LTS |
| **Kernel Version**      | 6.5.0-45-generic |
| **CPU Model**           | Intel(R) Core(TM) i7-6700K CPU @ 4.00GHz |
| **CPU Cores**           | 8 |
| **RAM**                 | 31Gi total, 24Gi available |
| **Disk**                | 506G total, 288G available |

## Scenarios

**Scenario file:** src/main/resources/scenarios/scenarios-smoketest.csv

| Scenario | k6 Config | Server Profiles | Delay Call Depth | Delay (ms) | Connections | Requests per Second | Warmup Duration (s) | Test Duration (s) |
|----------|-----------|-----------------|------------------|------------|-------------|---------------------|---------------------|------------------|
| smoketest-get-time | get-time.js |  | 0 | 0 | 100 | 200 | 2 | 6 |
| smoketest-get-movies-h2 | get-movies.js |  | 1 | 100 | 10 | 10 | 0 | 6 |
| smoketest-get-movies-postgres | get-movies.js | postgres | 1 | 100 | 10 | 10 | 0 | 6 |
| smoketest-get-movies-postgres-no-cache | get-movies.js | postgres|no-cache | 1 | 100 | 10 | 10 | 0 | 6 |
