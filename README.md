# <samp>mch</samp>

[![test](https://github.com/mcenv/mch/actions/workflows/test.yml/badge.svg)](https://github.com/mcenv/mch/actions/workflows/test.yml)

<samp>mch</samp> is a highly-compatible lightweight benchmark harness for Minecraft: Java Edition[^1].

> **Warning**
> - Back up your world. It is recommended to create a world specifically for benchmarking.
> - Ensure that the benchmarked functions are [idempotent](https://en.wikipedia.org/wiki/Idempotence); otherwise, the benchmark results will be inaccurate.
> - Remember that the benchmark results may be affected by Minecraft/JVM/OS/HW.

## Usage

1. Download [`mch.jar`](https://github.com/mcenv/mch/releases/latest/download/mch.jar) and place it in the same directory as `server.jar`.
2. Create `mch.properties` file in the same directory and specify a comma-separated list of function names to be benchmarked in `execute-benchmarks` property. For example:
    ```properties
    execute-benchmarks=a,b
    ```
3. Run `java -jar mch.jar`.
4. The benchmark results will be dumped to `mch-results.json`.

## Requirements

- Java 17+

## Options

| Name                     | Description                                                       | Default |
|:-------------------------|:------------------------------------------------------------------|:--------|
| `warmup-iterations`      | Number of warmup iterations.                                      | `5`     |
| `measurement-iterations` | Number of measurement iterations.                                 | `5`     |
| `time`                   | Duration of iterations in seconds.                                | `10`    |
| `forks`                  | Number of forks.                                                  | `5`     |
| `time-unit`              | Output time unit. (`ns`, `us`, `ms`, `s`, `m`)                    | `s`     |
| `jvm-args`               | JVM arguments to use with forks.                                  | `,`     |
| `mc-args`                | Minecraft arguments to use with forks.                            | `nogui` |
| `parsing-benchmarks`     | Comma-separated list of function **paths** for parsing benchmark. | `,`     |
| `execute-benchmarks`     | Comma-separated list of function **names** for execute benchmark. | `,`     |

## Fixtures

| Function tag              | Description                                              |
|:--------------------------|:---------------------------------------------------------|
| `#mch:setup`              | To be run once before the benchmarks                     |
| `#mch:setup.trial`        | To be run before each run of the execute benchmark       |
| `#mch:setup.iteration`    | To be run before each iteration of the execute benchmark |
| `#mch:teardown.trial`     | To be run after each run of the execute benchmark        |
| `#mch:teardown.iteration` | To be run after each iteration of the execute benchmark  |

[^1]: NOT OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG.
