# mch

[![test](https://github.com/mcenv/mch/actions/workflows/test.yml/badge.svg)](https://github.com/mcenv/mch/actions/workflows/test.yml)

<samp>mch</samp> is a highly-compatible lightweight benchmark harness for Minecraft: Java Edition.

> **Warning**
> - Do not forget to back up your world. It is recommended to create a world for benchmarking.
> - Make sure that the benchmarked functions are idempotent, i.e. do not have any side effects, otherwise the benchmark results will be inaccurate.
> - Remember that the benchmark results may be affected by Minecraft/JVM/OS/HW.

## Usage

1. Download [`mch.jar`](https://github.com/mcenv/mch/releases/latest/download/mch.jar) and put it under the directory containing `server.jar`.
2. Create `mch.properties` file under the same directory and specify the comma-separated list of function names to be benchmarked in `benchmarks` property. For example:
    ```properties
    benchmarks=a,b
    ```
3. Run `java -jar mch.jar`.
4. The benchmark results are dumped to `mch-results.json`.

## Requirements

- Java 17+

## Options

| Name                     | Description                                               | Default |
|:-------------------------|:----------------------------------------------------------|:--------|
| `warmup-iterations`      | Number of warmup iterations.                              | `5`     |
| `measurement-iterations` | Number of measurement iterations.                         | `5`     |
| `time`                   | Duration of iterations in seconds.                        | `10`    |
| `forks`                  | Number of forks.                                          | `5`     |
| `benchmarks`             | Comma-separated list of function names to be benchmarked. | `,`     |

## Fixtures

| Function tag              | Description                                      |
|:--------------------------|:-------------------------------------------------|
| `#mch:setup.trial`        | To be run before each run of the benchmark       |
| `#mch:setup.iteration`    | To be run before each iteration of the benchmark |
| `#mch:teardown.trial`     | To be run after each run of the benchmark        |
| `#mch:teardown.iteration` | To be run after each iteration of the benchmark  |
