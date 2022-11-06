# mch

[![test](https://github.com/mcenv/mch/actions/workflows/test.yml/badge.svg)](https://github.com/mcenv/mch/actions/workflows/test.yml)

<samp>mch</samp> is a highly-compatible lightweight benchmark harness for Minecraft: Java Edition.

> **Warning**
> - Make sure that the benchmarked functions are idempotent, i.e. do not have any side effects, otherwise the benchmark results will be inaccurate.
> - Remember that the benchmark results may be affected by Minecraft/JVM/OS/HW.

## Usage

1. Download [`mch.jar`](https://github.com/mcenv/mch/releases/latest/download/mch.jar) and put it under the directory containing `server.jar`.
2. Create `mch.properties` file under the same directory.
3. Specify the comma-separated list of function names to be benchmarked in `benchmarks` property. For example:
    ```properties
    benchmarks=a,b
    ```
4. Run `java -jar mch.jar`.
5. The benchmark results are dumped as `mch-results.json`.

## Options

| Name                     | Description                                               | Default |
|:-------------------------|:----------------------------------------------------------|:--------|
| `warmup-iterations`      | Number of warmup iterations.                              | `5`     |
| `measurement-iterations` | Number of measurement iterations.                         | `5`     |
| `time`                   | Duration of iterations in seconds.                        | `10`    |
| `forks`                  | Number of forks.                                          | `5`     |
| `benchmarks`             | Comma-separated list of function names to be benchmarked. | `,`     |
