package dev.mcenv.mch;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.commonmark.ext.gfm.tables.*;
import org.commonmark.node.Code;
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
      try (final var out = new BufferedOutputStream(Files.newOutputStream(Paths.get(mchConfig.output() + ".json")))) {
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
      try (final var out = new OutputStreamWriter(new BufferedOutputStream(Files.newOutputStream(Paths.get(mchConfig.output() + ".md"))))) {
        final var document = new Document();
        {
          {
            final var heading = new Heading();
            document.appendChild(heading);
            heading.setLevel(3);
            heading.appendChild(new Text("Results"));
          }
          {
            final var table = new TableBlock();
            document.appendChild(table);
            {
              final var thead = new TableHead();
              table.appendChild(thead);
              {
                final var tr = new TableRow();
                thead.appendChild(tr);
                {
                  final var group = new TableCell();
                  tr.appendChild(group);
                  group.setHeader(true);
                  group.setAlignment(TableCell.Alignment.LEFT);
                  group.appendChild(new Text("Group"));
                }
                {
                  final var benchmark = new TableCell();
                  tr.appendChild(benchmark);
                  benchmark.setHeader(true);
                  benchmark.setAlignment(TableCell.Alignment.LEFT);
                  benchmark.appendChild(new Text("Benchmark"));
                }
                {
                  final var mode = new TableCell();
                  tr.appendChild(mode);
                  mode.setHeader(true);
                  mode.setAlignment(TableCell.Alignment.CENTER);
                  mode.appendChild(new Text("Mode"));
                }
                {
                  final var count = new TableCell();
                  tr.appendChild(count);
                  count.setHeader(true);
                  count.setAlignment(TableCell.Alignment.RIGHT);
                  count.appendChild(new Text("Count"));
                }
                {
                  final var score = new TableCell();
                  tr.appendChild(score);
                  score.setHeader(true);
                  score.setAlignment(TableCell.Alignment.RIGHT);
                  score.appendChild(new Text("Score"));
                }
                {
                  final var error = new TableCell();
                  tr.appendChild(error);
                  error.setHeader(true);
                  error.setAlignment(TableCell.Alignment.RIGHT);
                  error.appendChild(new Text("Error"));
                }
                {
                  final var unit = new TableCell();
                  tr.appendChild(unit);
                  unit.setHeader(true);
                  unit.setAlignment(TableCell.Alignment.LEFT);
                  unit.appendChild(new Text("Unit"));
                }
              }
            }
            final var unitString = String.format("%s/op", abbreviate(mchConfig.timeUnit()));
            for (final var runResult : runResults) {
              final var tbody = new TableBody();
              table.appendChild(tbody);
              {
                final var tr = new TableRow();
                tbody.appendChild(tr);
                {
                  final var group = new TableCell();
                  tr.appendChild(group);
                  group.setAlignment(TableCell.Alignment.LEFT);
                  group.appendChild(new Text(runResult.group()));
                }
                {
                  final var benchmark = new TableCell();
                  tr.appendChild(benchmark);
                  benchmark.setAlignment(TableCell.Alignment.LEFT);
                  benchmark.appendChild(new Code(runResult.benchmark()));
                }
                {
                  final var mode = new TableCell();
                  tr.appendChild(mode);
                  mode.setAlignment(TableCell.Alignment.CENTER);
                  mode.appendChild(new Text(runResult.mode().toString()));
                }
                {
                  final var count = new TableCell();
                  tr.appendChild(count);
                  count.setAlignment(TableCell.Alignment.RIGHT);
                  count.appendChild(new Text(String.valueOf(mchConfig.measurementIterations() * mchConfig.forks())));
                }
                {
                  final var score = new TableCell();
                  tr.appendChild(score);
                  score.setAlignment(TableCell.Alignment.RIGHT);
                  score.appendChild(new Text(String.format("%f", convert(Statistics.mean(runResult.scores()), TimeUnit.NANOSECONDS, mchConfig.timeUnit()))));
                }
                {
                  final var error = new TableCell();
                  tr.appendChild(error);
                  error.setAlignment(TableCell.Alignment.RIGHT);
                  error.appendChild(new Text(String.format("± %f", convert(Statistics.error(runResult.scores()), TimeUnit.NANOSECONDS, mchConfig.timeUnit()))));
                }
                {
                  final var unit = new TableCell();
                  tr.appendChild(unit);
                  unit.setAlignment(TableCell.Alignment.LEFT);
                  unit.appendChild(new Text(unitString));
                }
              }
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
