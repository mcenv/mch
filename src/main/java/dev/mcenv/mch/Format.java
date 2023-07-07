package dev.mcenv.mch;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.commonmark.ext.gfm.tables.*;
import org.commonmark.node.Document;
import org.commonmark.node.Heading;
import org.commonmark.node.Text;
import org.commonmark.renderer.html.HtmlRenderer;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static dev.mcenv.mch.Util.*;

sealed interface Format permits Format.Json, Format.Md {
  static Format parse(final String format) {
    return switch (format) {
      case "json" -> Json.INSTANCE;
      case "md" -> Md.INSTANCE;
      default -> throw new IllegalStateException("Unexpected format: " + format);
    };
  }

  void write(
    final MchConfig mchConfig,
    final String mcVersion,
    final Collection<RunResult> runResults
  ) throws IOException;

  final class Json implements Format {
    public static final Format INSTANCE = new Json();

    private Json() {
    }

    @Override
    public void write(
      final MchConfig mchConfig,
      final String mcVersion,
      final Collection<RunResult> runResults
    ) throws IOException {
      final var unit = String.format("%s/op", abbreviate(mchConfig.timeUnit()));
      try (final var out = new BufferedOutputStream(Files.newOutputStream(Paths.get("mch-results.json")))) {
        final String mchVersion;
        try (final var version = Main.class.getClassLoader().getResourceAsStream("version")) {
          mchVersion = new String(Objects.requireNonNull(version).readAllBytes(), StandardCharsets.UTF_8).trim();
        }
        final var forks = mchConfig.forks();
        final var jvm = getCurrentJvm();
        final var jvmArgs = mchConfig.jvmArgs();
        final var jdkVersion = System.getProperty("java.version");
        final var vmName = System.getProperty("java.vm.name");
        final var vmVersion = System.getProperty("java.vm.version");
        final var mc = mchConfig.mc();
        final var mcArgs = mchConfig.mcArgs();
        final var warmupIterations = mchConfig.warmupIterations();
        final var warmupTime = String.format("%d %s", mchConfig.time(), abbreviate(TimeUnit.SECONDS));
        final var measurementIterations = mchConfig.measurementIterations();
        final var measurementTime = String.format("%d %s", mchConfig.time(), abbreviate(TimeUnit.SECONDS));
        final var entries = runResults
          .stream()
          .map(runResult -> {
            try {
              return new Results.Result(
                runResult.group(),
                runResult.benchmark(),
                runResult.mode().toString(),
                mchConfig.measurementIterations() * mchConfig.forks(),
                convert(Statistics.mean(runResult.scores()), TimeUnit.NANOSECONDS, mchConfig.timeUnit()),
                convert(Statistics.error(runResult.scores()), TimeUnit.NANOSECONDS, mchConfig.timeUnit()),
                unit
              );
            } catch (NotStrictlyPositiveException e) {
              throw new RuntimeException(e);
            }
          })
          .toList();

        final var gson = new GsonBuilder()
          .setPrettyPrinting()
          .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
          .create();
        out.write(gson
          .toJson(new Results(
            mchVersion,
            forks,
            jvm,
            jvmArgs,
            jdkVersion,
            vmName,
            vmVersion,
            mc,
            mcArgs,
            mcVersion,
            warmupIterations,
            warmupTime,
            measurementIterations,
            measurementTime,
            entries
          ))
          .getBytes(StandardCharsets.UTF_8)
        );
      }
    }
  }

  final class Md implements Format {
    public static final Format INSTANCE = new Md();

    private Md() {
    }

    @Override
    public void write(
      final MchConfig mchConfig,
      final String mcVersion,
      final Collection<RunResult> runResults
    ) throws IOException {
      try (final var out = new OutputStreamWriter(new BufferedOutputStream(Files.newOutputStream(Paths.get("mch-results.md"))))) {
        final var document = new Document();
        {
          {
            final var heading = new Heading();
            heading.setLevel(3);
            heading.appendChild(new Text("Results"));
            document.appendChild(heading);
          }
          {
            final var table = new TableBlock();
            {
              final var thead = new TableHead();
              {
                final var tr = new TableRow();
                {
                  final var group = new TableCell();
                  group.setHeader(true);
                  group.setAlignment(TableCell.Alignment.LEFT);
                  tr.appendChild(group);
                }
                {
                  final var benchmark = new TableCell();
                  benchmark.setHeader(true);
                  benchmark.setAlignment(TableCell.Alignment.LEFT);
                  tr.appendChild(benchmark);
                }
                {
                  final var mode = new TableCell();
                  mode.setHeader(true);
                  mode.setAlignment(TableCell.Alignment.CENTER);
                  tr.appendChild(mode);
                }
                {
                  final var count = new TableCell();
                  count.setHeader(true);
                  count.setAlignment(TableCell.Alignment.RIGHT);
                  tr.appendChild(count);
                }
                {
                  final var score = new TableCell();
                  score.setHeader(true);
                  score.setAlignment(TableCell.Alignment.RIGHT);
                  tr.appendChild(score);
                }
                {
                  final var error = new TableCell();
                  error.setHeader(true);
                  error.setAlignment(TableCell.Alignment.RIGHT);
                  tr.appendChild(error);
                }
                {
                  final var unit = new TableCell();
                  unit.setHeader(true);
                  unit.setAlignment(TableCell.Alignment.LEFT);
                  tr.appendChild(unit);
                }
                thead.appendChild(tr);
              }
              table.appendChild(thead);
            }
            final var unitText = new Text(String.format("%s/op", abbreviate(mchConfig.timeUnit())));
            for (final var runResult : runResults) {
              final var tbody = new TableBody();
              {
                final var tr = new TableRow();
                {
                  final var group = new TableCell();
                  group.setAlignment(TableCell.Alignment.LEFT);
                  group.appendChild(new Text(runResult.group()));
                  tr.appendChild(group);
                }
                {
                  final var benchmark = new TableCell();
                  benchmark.setAlignment(TableCell.Alignment.LEFT);
                  benchmark.appendChild(new Text(runResult.benchmark()));
                  tr.appendChild(benchmark);
                }
                {
                  final var mode = new TableCell();
                  mode.setAlignment(TableCell.Alignment.CENTER);
                  mode.appendChild(new Text(runResult.mode().toString()));
                  tr.appendChild(mode);
                }
                {
                  final var count = new TableCell();
                  count.setAlignment(TableCell.Alignment.RIGHT);
                  count.appendChild(new Text(String.valueOf(mchConfig.measurementIterations() * mchConfig.forks())));
                  tr.appendChild(count);
                }
                {
                  final var score = new TableCell();
                  score.setAlignment(TableCell.Alignment.RIGHT);
                  score.appendChild(new Text(String.format("%f", convert(Statistics.mean(runResult.scores()), TimeUnit.NANOSECONDS, mchConfig.timeUnit()))));
                  tr.appendChild(score);
                }
                {
                  final var error = new TableCell();
                  error.setAlignment(TableCell.Alignment.RIGHT);
                  error.appendChild(new Text(String.format("± %f", convert(Statistics.error(runResult.scores()), TimeUnit.NANOSECONDS, mchConfig.timeUnit()))));
                  tr.appendChild(error);
                }
                {
                  final var unit = new TableCell();
                  unit.setAlignment(TableCell.Alignment.LEFT);
                  unit.appendChild(unitText);
                  tr.appendChild(unit);
                }
                tbody.appendChild(tr);
              }
              table.appendChild(tbody);
            }
          }
        }

        HtmlRenderer
          .builder()
          .extensions(List.of(TablesExtension.create()))
          .build()
          .render(document, out);
      }
    }
  }
}
