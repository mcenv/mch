# <samp>mch</samp>

[![test](https://github.com/mcenv/mch/actions/workflows/test.yml/badge.svg)](https://github.com/mcenv/mch/actions/workflows/test.yml)

<samp>mch</samp> is a highly-compatible lightweight benchmark harness for Minecraft: Java Edition[^1].

> **Warning**
> - Use a world dedicated for benchmarking.
> - Ensure that the benchmark target is [idempotent](https://en.wikipedia.org/wiki/Idempotence) for accurate results.
> - Remember that the benchmark results may be affected by Minecraft/JVM/OS/HW.
> - Do not replace functions in `mch` namespace.

## [Supported versions](https://github.com/mcenv/mch/blob/versions/versions.csv)

Automatically updated every day.

## Usage

1. Download [<samp>mch.jar</samp>](https://github.com/mcenv/mch/releases/latest/download/mch.jar) into the same directory as <samp>server.jar</samp>.
2. Create <samp>mch-config.json</samp> with the following content in the directory and add options to it.
   ```json
   {
     "$schema": "https://raw.githubusercontent.com/mcenv/mch/main/mch-config-schema.json"
   }
   ```
3. Run the following command to start the benchmarks.
   ```shell
   java -jar mch.jar
   ```
4. The benchmark results will be dumped to <samp>mch-results.*format*</samp>.

See [samples](https://github.com/mcenv/mch/tree/main/samples) for more details.

## Requirements

- Java 17+

## Options

| Name                     | Description                                   | Default       |
|:-------------------------|:----------------------------------------------|:--------------|
| `warmup_iterations`      | Number of warmup iterations                   | `5`           |
| `measurement_iterations` | Number of measurement iterations              | `5`           |
| `time`                   | Duration of iterations in seconds             | `10`          |
| `forks`                  | Number of forks                               | `5`           |
| `time_unit`              | Output time unit (`ns`, `us`, `ms`, `s`, `m`) | `s`           |
| `mc`                     | Path to Minecraft server                      | `server.jar`  |
| `output`                 | Output file name without extension            | `mch-results` |
| `formats`                | Output formats (`json`, `md`)                 | <code></code> |
| `jvm_args`               | JVM arguments to use with forks               | <code></code> |
| `mc_args`                | Minecraft arguments to use with forks         | `nogui`       |
| `parsing_benchmarks`     | Commands for parsing benchmark                | <code></code> |
| `execute_benchmarks`     | Commands for execute benchmark                | <code></code> |
| `function_benchmarks`    | Data pack names for function benchmark        | <code></code> |

## Fixtures

| Function tag              | Description                                                 |
|:--------------------------|:------------------------------------------------------------|
| `#mch:setup`              | To be run once before each group of the function benchmarks |
| `#mch:setup.trial`        | To be run before each run of the group                      |
| `#mch:setup.iteration`    | To be run before each iteration of the run                  |
| `#mch:teardown.iteration` | To be run after each iteration of the run                   |
| `#mch:teardown.trial`     | To be run after each run of the group                       |
| `#mch:teardown`           | To be run once after each group of the function benchmarks  |

## Commands

| Command    | Description                          |
|:-----------|:-------------------------------------|
| `mch:noop` | Do nothing except for returning `0`. |

[^1]: NOT OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG.
