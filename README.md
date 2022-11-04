# mch

[![test](https://github.com/mcenv/mch/actions/workflows/test.yml/badge.svg)](https://github.com/mcenv/mch/actions/workflows/test.yml)

<samp>mch</samp> is a highly-compatible lightweight benchmark harness for Minecraft: Java Edition.

> **Warning**
> - Make sure that the benchmarked functions are idempotent, i.e. do not have any side effects, otherwise the benchmark results will be incorrect.
> - Remember that the benchmark results may be affected by Minecraft/JVM/OS/HW.

## Usage

1. Download [`mch.jar`](https://github.com/mcenv/mch/releases/latest/download/mch.jar) and put it under the directory containing `server.jar`.
2. Create `benchmarks` file under the same directory. `benchmarks` file contains line-separated function names to be benchmarked.
3. Run `java -jar mch.jar`.
4. The benchmark results are dumped as `results.json`.
