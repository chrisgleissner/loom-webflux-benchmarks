# AGENTS.md

Guidance for AI coding agents working in this repository.

## Project Overview

This is a Java/Spring Boot benchmarking project comparing Java virtual-thread approaches with Spring WebFlux/Reactor:

- `platform-tomcat`: traditional platform threads on Tomcat
- `loom-tomcat`: virtual threads on Tomcat
- `loom-netty`: virtual-thread style application code on Netty
- `webflux-netty`: reactive WebFlux application code on Netty

The repository includes:

- Java production code in `src/main/java/uk/gleissner/loomwebflux/`
- Kotlin and Java tests in `src/test/java/`
- Python chart/report tooling in `src/main/python/` with tests in `src/test/python/`
- k6 scenario definitions in `src/main/resources/scenarios/`
- benchmark orchestration scripts in `src/main/bash/`
- checked-in benchmark reports and CI result snapshots under `results/`

Prefer small, behavior-preserving changes. This repository is primarily a benchmark, so seemingly harmless changes to client configuration,
profiles, timing, CSV parsing, metrics, or chart generation can invalidate comparisons.

## Required Tooling

- Java 21 or newer is required. The Gradle bytecode target defaults to 21 via `gradle.properties`.
- Use the checked-in Gradle wrapper: `./gradlew`.
- Python tests and chart tooling require Python 3 plus `matplotlib`, `pandas`, and `numpy`.
- Benchmarks require `k6` and `sysstat` tools such as `sar`/`sadf`.
- PostgreSQL scenarios require Docker Compose via `src/main/docker/docker-compose-postgres.yaml`.

Before diagnosing build failures, check `java -version`. Java 17 and older are not supported.

## Validation Commands

Use the narrowest validation that covers your change, then broaden when touching shared behavior.

```bash
# Java compile, tests, and packaging
./gradlew build

# Java tests only
./gradlew test

# Coverage XML used by CI
./gradlew jacocoTestReport

# Python chart/report tests
python3 -m pytest

# CI-like Java dependency/build/coverage path
./gradlew dependencies build jacocoTestReport
```

For benchmark-related changes, run a smoke benchmark when the local machine has `k6`, Python chart dependencies, and `sysstat` installed:

```bash
./benchmark.sh scenarios-smoketest.csv
```

To limit benchmark approaches during development:

```bash
./benchmark.sh -a loom-netty,webflux-netty scenarios-smoketest.csv
```

Do not run high-load, soak, default, or monthly CI benchmark scenarios unless the user explicitly asks or the task requires it. They are
expensive and sensitive to host tuning.

## Running the App

Run one approach at a time with Spring profiles:

```bash
SPRING_PROFILES_ACTIVE=loom-netty ./gradlew bootRun
SPRING_PROFILES_ACTIVE=webflux-netty ./gradlew bootRun
SPRING_PROFILES_ACTIVE=loom-tomcat ./gradlew bootRun
SPRING_PROFILES_ACTIVE=platform-tomcat ./gradlew bootRun
```

`build.gradle.kts` conditionally adds `spring-boot-starter-web` for Tomcat profiles. If profile-sensitive dependency behavior looks wrong,
check `SPRING_PROFILES_ACTIVE` before editing dependencies.

Health is exposed at:

```text
http://localhost:8080/actuator/health
```

## Code Map

- `time/`: minimal epoch-millis endpoint used for low-overhead scenarios.
- `movie/`: movie REST API used for more realistic JSON/JPA/cache scenarios.
- `movie/repo/`: JPA repository, cached repository wrapper, and data initialization.
- `common/proxy/nonreactive/`: blocking service proxy path for imperative approaches.
- `common/proxy/reactive/`: reactive proxy path for WebFlux.
- `config/`: application properties, Spring profiles, metrics CSV logging, and HTTP client configuration.
- `src/main/resources/application.yaml`: shared application configuration.
- `src/main/resources/application-*.yaml`: profile-specific overrides.
- `src/main/resources/scenarios/*.csv`: benchmark scenario inputs.
- `src/main/resources/scenarios/*.js`: k6 scripts referenced by scenario CSV files.
- `src/main/python/results_chart.py`: aggregate benchmark chart generation.
- `src/main/python/scenario_chart.py`: per-scenario chart generation.

## Change Guidelines

- Preserve parity between approaches. When adding endpoint behavior or benchmarked work, update both the non-reactive and reactive paths
  unless the task explicitly targets only one approach.
- Keep scenario CSV schema compatibility. The benchmark scripts read positional CSV columns.
- Be cautious with timeouts, connection pools, cache behavior, JVM args, and HTTP client defaults. These are benchmark inputs, not incidental configuration.
- Do not commit files under `build/`, `.gradle/`, virtualenvs, caches, or generated local benchmark output.
- Treat checked-in files under `results/` as published benchmark artifacts. Update them only when the task is specifically about benchmark
  results or documentation that references them.
- Keep shell scripts POSIX/Bash-friendly and preserve executable entry points at the repository root (`benchmark.sh`, `benchmarks.sh`, `compare-benchmarks.sh`).
- Prefer structured parsing for Python chart/report changes. Avoid ad hoc string parsing of CSV or Markdown when `csv`, `pandas`, or existing
  helpers are available.

## Style

- Follow `.editorconfig`: UTF-8, LF line endings, 4-space indentation, and 160-character maximum line length.
- Production code is Java. Tests are mostly Kotlin. Match the surrounding language and style instead of introducing new frameworks.
- Lombok is used in Java code; do not remove it opportunistically.
- Keep comments sparse and focused on benchmark-specific reasoning or non-obvious tradeoffs.

## Tests

- Java tests use JUnit 5 and Spring Boot test support. Integration tests often exercise multiple approaches through profiles and random ports.
- Python tests live under `src/test/python/` and are run from the repository root with `python3 -m pytest`.
- When changing chart generation, run the Python tests and inspect whether generated chart paths or CSV expectations changed.
- When changing controllers, services, repositories, profiles, or client configuration, run `./gradlew test` at minimum.
- When changing build logic or dependencies, run `./gradlew dependencies build`.

## Benchmark Workflow Notes

`./benchmark.sh` is a thin wrapper around `src/main/bash/benchmark.sh`.

The benchmark script:

1. Builds profile-specific boot jars.
2. Starts Docker Compose services for scenarios whose `serverProfiles` require them.
3. Runs each scenario/approach combination through k6.
4. Captures JVM/system metrics.
5. Generates per-scenario PNGs, aggregate PNGs, CSV results, and Markdown summaries under `build/results/`.

Scenario CSV files are in `src/main/resources/scenarios/`. The supported approaches are:

```text
platform-tomcat, loom-tomcat, loom-netty, webflux-netty
```

## CI

GitHub Actions build on pushes via `.github/workflows/build.yaml`, which calls `.github/workflows/reusable-build.yaml`.

The reusable build matrix runs on Ubuntu 22.04 and 24.04 with Java 21 and 25. It installs k6 and Python dependencies, runs:

```bash
python3 -m pytest
./gradlew dependencies build jacocoTestReport
./benchmark.sh -a <approaches> <scenario-file>
```

Then it verifies generated PNGs and `build/results/results.csv`. Monthly benchmark workflows run heavier scenarios.
