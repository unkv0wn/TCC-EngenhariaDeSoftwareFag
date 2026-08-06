package com.routewise.validation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Writes a generated Markdown report to disk, creating parent directories as needed.
 *
 * <p>Exists so the report scripts under {@code com.routewise.validation} share one
 * write-and-fail behaviour instead of each repeating the same try/catch.
 */
public final class MarkdownReportFile {

  private static final DateTimeFormatter TIMESTAMP =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private MarkdownReportFile() {}

  public static void write(Path path, String content) {
    try {
      Files.createDirectories(path.getParent());
      Files.writeString(path, content);
    } catch (IOException ex) {
      throw new UncheckedIOException("Failed to write report to " + path, ex);
    }
    System.out.println("Relatório: " + path.toAbsolutePath().normalize());
  }

  /** Timestamp line used at the top of every generated report. */
  public static String generatedAt() {
    return "Gerado em: " + LocalDateTime.now().format(TIMESTAMP) + "\n\n";
  }
}
