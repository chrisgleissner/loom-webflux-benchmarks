# Copilot Agent Instructions for loom-webflux-benchmarks

## Repository Overview

**Java Spring Boot 3.5 benchmarking project** comparing Java Virtual Threads (Loom) vs Spring WebFlux (Reactor) for microservices. ~310MB repo (mostly results), ~1MB source (29 Java, 15 Kotlin test files).

**Stack**: Java 21+, Gradle 9.1, Spring Boot 3.5.7, WebFlux, Netty, Tomcat, H2/PostgreSQL, k6, Python 3 (matplotlib, pandas, numpy), sysstat

**Approaches tested**: platform-tomcat, loom-tomcat, loom-netty, webflux-netty

## Build & Test - CRITICAL

**Java 21+ REQUIRED** - Fails with Java 17: "invalid source release: 21" (`gradle.properties`: `java.bytecode.version=21`)

```bash
# Set Java 21
export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
java -version  # Verify 21+

# Build & test (~2-3 min, 153 tests in 90s)
./gradlew clean build

# Fast build (no tests)
./gradlew assemble

# Tests only
./gradlew test

# Coverage report
./gradlew jacocoTestReport  # → build/reports/jacoco/test/html/
```

**Common issues**:
- Wrong Java: Always check `java -version` first
- OOM: Increase `org.gradle.jvmargs` in `gradle.properties` (default 512MB)
- Gradle stuck: `./gradlew --stop` then retry
- Port 8080 busy: Tests use random ports, bootRun uses 8080

## Running Application

```bash
SPRING_PROFILES_ACTIVE=loom-netty ./gradlew bootRun  # 2GB heap
```
Valid profiles: platform-tomcat, loom-tomcat, loom-netty, webflux-netty  
Health: `http://localhost:8080/actuator/health`  
**Note**: Tomcat profiles auto-add `spring-boot-starter-web` dependency at build time (`build.gradle.kts` L74-79)

## Project Structure

**Core Java**: `src/main/java/uk/gleissner/loomwebflux/`
- `time/TimeController.java` - Minimal endpoint (epoch millis)
- `movie/MovieController.java` - CRUD API (H2/Postgres, JPA, Caffeine cache)
- `movie/repo/` - CachedMovieRepo wraps JPA
- `config/` - Spring config, RestClient/WebClient beans
- `common/proxy/nonreactive/` - Blocking for Virtual Threads
- `common/proxy/reactive/` - Non-blocking for WebFlux

**Config**: `build.gradle.kts`, `gradle.properties`, `lombok.config`, `.editorconfig` (4 spaces, 160 chars, LF)
- `src/main/resources/application.yaml` - Base config (HTTP/2, virtual threads, pool sizes)
- `src/main/resources/application-{approach}.yaml` - Overrides per approach
- `src/main/resources/application-postgres.yaml` - PostgreSQL datasource
- `src/main/resources/scenarios/*.csv` - Benchmark scenarios (smoketest, default, high-load, clients, postgres, deep-call-stack, sharp-spikes, soaktest)

**Scripts**:
- `src/main/bash/benchmark.sh` - Main entry (runs scenarios CSV)
- `src/main/bash/benchmark-scenario.sh` - Single scenario runner
- `src/main/bash/system-measure.sh` - sar/sadf monitoring
- `src/main/python/scenario_chart.py` - PNG chart from CSVs
- `src/main/python/results_chart.py` - Summary chart
- `src/test/python/*_test.py` - Python tests (pytest)

**Docker**: `src/main/docker/docker-compose-postgres.yaml`

## CI/CD Workflows (.github/workflows/)

**build.yaml** (every push) - Uses `scenarios-smoketest.csv`, matrix: Java 21/25 × Ubuntu 22.04/24.04
- Installs: k6, python3-matplotlib/pandas/pytest, sysstat, inxi
- System tuning: file limits, port range, TCP reuse
- Steps: build → test Python → build Java → benchmark → verify PNGs → commit results to `results/ci/`

**reusable-build.yaml** - Base workflow with configurable scenarios/approaches

**benchmark.yaml** (monthly, 2nd @ 2am) - Uses `scenarios-ci.csv` (6h limit)

**Verification**: All PNG charts exist + `build/results/results.csv` is valid

## Benchmarking Locally

**Prerequisites**: Java 21+, k6, Python 3 (matplotlib/pandas/numpy), sysstat, optional Docker

**System optimization**:
```bash
printf '* soft nofile 1048576\n* hard nofile 1048576\n' | sudo tee -a /etc/security/limits.conf
printf 'net.ipv4.ip_local_port_range=1024 65535\nnet.ipv4.tcp_tw_reuse = 1\n' | sudo tee -a /etc/sysctl.conf && sudo sysctl -p
```

**Run**:
```bash
./benchmark.sh [SCENARIO_FILE]  # Default: scenarios-default.csv
./benchmark.sh -a "loom-netty,webflux-netty" scenarios-smoketest.csv  # Specific approaches
./benchmark.sh -C scenarios-smoketest.csv  # Keep CSVs
```
Results: `build/results/[scenario]/[approach].png`

**Flow**: Start Docker (if serverProfiles) → per approach: start service, warmup, test, chart, stop → stop Docker → summary charts

## Testing Details

**Java**: 153 tests (~90s), `@SpringBootTest` with random ports, parameterized for all 4 approaches  
**Known issues**: 
- 1 disabled test: `MovieControllerPostgresNoCacheIT` (SQL assertions)
- Platform thread tests use virtual threads in test mode (TODO in `LogCaptorFixture.kt`)
- Requires JVM args: `-XX:+EnableDynamicAgentLoading -Xshare:off`

**Python**: `python3 -m pytest` (requires matplotlib, pandas, numpy)

## Key Concepts

**Approaches**:
- `platform-tomcat`: Traditional threads on Tomcat
- `loom-tomcat`: Virtual threads on Tomcat (`spring.threads.virtual.enabled=true`)
- `loom-netty`: Virtual threads on Netty (WebFlux server, no reactive types)
- `webflux-netty`: Fully reactive (Mono/Flux) on Netty

**Delay simulation**:
- `delayCallDepth`: Recursive HTTP calls (simulates upstream services)
- `delayInMillis`: Final delay (simulates I/O)
- Enables thread reuse during waits

**Clients**: Non-WebFlux can use WebClient (Netty), RestClient (JDK/Apache/Netty). WebFlux always uses WebClient.

## Pre-Commit Validation

```bash
export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
./gradlew clean build  # Must pass with "SUCCESS: Executed 153 tests"
./benchmark.sh scenarios-smoketest.csv  # If k6/python/sysstat available
# Verify: PNG files in build/results/, no uncommitted build artifacts
```

**gitignore**: Excludes `build/`, `bin/`, `.gradle`, `__pycache__`

## Common Issues & Fixes

| Issue | Cause | Fix |
|-------|-------|-----|
| Build fails "invalid source release: 21" | Wrong Java | `java -version` must show 21+ |
| "k6: command not found" | k6 not installed | See README or skip benchmarks |
| Port 8080 busy | Another service | Kill process or skip bootRun |
| Gradle hangs | Daemon issue | `./gradlew --stop` then retry |
| Test timeouts | Slow machine | Expected for integration tests |
| Python module errors | Missing deps | `sudo apt install python3-matplotlib python3-pandas python3-numpy` |
| Lombok IDE errors | Plugin missing | Install Lombok plugin (build still works) |

## Making Changes

**Dependencies**: Edit `build.gradle.kts` → `./gradlew dependencies` → `./gradlew clean build`

**New endpoints**: 
- Create in `src/main/java/uk/gleissner/loomwebflux/[feature]/`
- Pattern: `@RequestMapping("/{approach}/[endpoint]")`
- Implement both blocking (Loom) and reactive (WebFlux) versions
- Add integration tests

**Scenarios**: Edit `src/main/resources/scenarios/*.csv` with columns: scenario, k6Config, serverProfiles, delayCallDepth, delayInMillis, connections, requestsPerSecond, warmupDurationInSeconds, testDurationInSeconds

**Code style**: Follow `.editorconfig`: 4 spaces, 160 chars, LF. Use Lombok. No explicit linters.

**Trust these instructions** - only search if encountering uncovered errors or modifying Python charts. All commands verified against fresh build.
