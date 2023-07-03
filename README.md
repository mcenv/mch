# <samp>mch</samp>

[![test](https://github.com/mcenv/mch/actions/workflows/test.yml/badge.svg)](https://github.com/mcenv/mch/actions/workflows/test.yml)

<samp>mch</samp> is a highly-compatible lightweight benchmark harness for Minecraft: Java Edition[^1].

> **Warning**
> - Back up your world. It is recommended to create a world specifically for benchmarking.
> - Ensure that the benchmarked functions are [idempotent](https://en.wikipedia.org/wiki/Idempotence); otherwise, the benchmark results will be inaccurate.
> - Remember that the benchmark results may be affected by Minecraft/JVM/OS/HW.

## Supported versions

You can check the supported versions [here](https://github.com/mcenv/mch/blob/versions/versions.csv).
(Automatically updated every day)

## Usages

### Basic usage

1. Download [`mch.jar`](https://github.com/mcenv/mch/releases/latest/download/mch.jar) and place it in the same directory as `server.jar`.
2. Run `java -jar mch.jar --function_benchmarks=a,b`. This will start the benchmarks for functions `a` and `b`.
3. The benchmark results will be dumped to `mch-results.json`.

### Advanced usage

1. Download [`mch.jar`](https://github.com/mcenv/mch/releases/latest/download/mch.jar).
2. Create `mch-config.json` with the following content in the same directory as `mch.jar` and add options to it.
   ```json
   {
     "$schema": "https://raw.githubusercontent.com/mcenv/mch/main/mch-config-schema.json"
   }
   ```
3. Run `java -jar mch.jar`. This will start the benchmarks with the options in `mch-config.json`.
4. The benchmark results will be dumped to `mch-results.json`.

## Requirements

- Java 17+

## Options

| Name                     | Description                                                       | Default       |
|:-------------------------|:------------------------------------------------------------------|:--------------|
| `warmup_iterations`      | Number of warmup iterations                                       | `5`           |
| `measurement_iterations` | Number of measurement iterations                                  | `5`           |
| `time`                   | Duration of iterations in seconds                                 | `10`          |
| `forks`                  | Number of forks                                                   | `5`           |
| `time_unit`              | Output time unit (`ns`, `us`, `ms`, `s`, `m`)                     | `s`           |
| `mc`                     | Path to Minecraft server                                          | `server.jar`  |
| `jvm_args`               | JVM arguments to use with forks                                   | <code></code> |
| `mc_args`                | Minecraft arguments to use with forks                             | `nogui`       |
| `parsing_benchmarks`     | Comma-separated list of commands for parsing benchmark            | <code></code> |
| `function_benchmarks`    | Comma-separated list of function **names** for function benchmark | <code></code> |

## Fixtures

| Function tag              | Description                                               |
|:--------------------------|:----------------------------------------------------------|
| `#mch:setup`              | To be run once before the function benchmarks             |
| `#mch:setup.trial`        | To be run before each run of the function benchmark       |
| `#mch:setup.iteration`    | To be run before each iteration of the function benchmark |
| `#mch:teardown.trial`     | To be run after each run of the function benchmark        |
| `#mch:teardown.iteration` | To be run after each iteration of the function benchmark  |
| `#mch:teardown`           | To be run once after the function benchmarks              |

## Commands

| Command    | Description                          |
|:-----------|:-------------------------------------|
| `mch:noop` | Do nothing except for returning `0`. |

[^1]: NOT OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG.
