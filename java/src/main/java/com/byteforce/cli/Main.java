package com.byteforce.cli;

import com.byteforce.core.ByteForceDB;
import com.byteforce.core.models.Column;
import com.byteforce.core.models.DataType;
import com.byteforce.core.models.Table;
import com.github.freva.asciitable.AsciiTable;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public class Main {
  private static final String HISTORY_FILE =
      System.getProperty("user.home") + "/.byteforce_java_history";

  public static void main(String[] args) {
    ByteForceDB db = new ByteForceDB();

    try {
      Terminal terminal = TerminalBuilder.builder().system(true).jansi(true).build();

      String[] keywords = {
        "SELECT", "select", "FROM", "from", "WHERE", "where", "INSERT", "insert", "INTO", "into",
        "VALUES", "values", "CREATE", "create", "TABLE", "table", "INDEX", "index", "UPDATE",
        "update", "SET", "set", "DELETE", "delete", "JOIN", "join", "ON", "on", "PRIMARY",
        "primary", "KEY", "key", "UNIQUE", "unique", "NOT", "not", "NULL", "null", "INTEGER",
        "integer", "TEXT", "text", "FLOAT", "float", "BOOLEAN", "boolean", ".exit", ".tables",
        ".schema", ".help", ".seed", ".export"
      };

      StringsCompleter completer = new StringsCompleter(keywords);

      LineReader reader =
          LineReaderBuilder.builder()
              .terminal(terminal)
              .completer(completer)
              .parser(new DefaultParser())
              .variable(LineReader.HISTORY_FILE, HISTORY_FILE)
              .variable(LineReader.SECONDARY_PROMPT_PATTERN, "%M%P > ")
              .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
              .build();

      // Use Right Prompt for hints (More stable than Status bar on Windows)
      reader.setVariable("RPROMPT", "(TAB) Autocomplete | .help | .exit");

      System.out.println("=".repeat(50));
      System.out.println("ByteForce RDBMS (Java Edition)");
      System.out.println("Newton Maina");
      System.out.println("=".repeat(50));
      System.out.println("Type .help for commands or .exit to quit.\n");

      while (true) {
        String line;
        try {
          line = reader.readLine("ByteForce> ");
        } catch (org.jline.reader.UserInterruptException | org.jline.reader.EndOfFileException e) {
          break;
        }

        line = line.trim();
        if (line.isEmpty()) continue;

        if (line.equalsIgnoreCase(".exit")) {
          System.out.println("Goodbye!");
          break;
        }

        if (line.startsWith(".")) {
          handleMetaCommand(line, db);
          continue;
        }

        long startTime = System.nanoTime();
        com.byteforce.core.ExecutionResult result = db.execute(line);
        long endTime = System.nanoTime();
        double duration = (endTime - startTime) / 1_000_000_000.0;

        displayResult(result);
        System.out.printf("Query executed in %.4f seconds%n", duration);
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // ... (keeping handleMetaCommand and other helper methods as is) ...

  private static void handleMetaCommand(String command, ByteForceDB db) {
    // ... (rest of handleMetaCommand implementation, no changes needed here) ...
    String[] parts = command.split("\\s+");
    String cmd = parts[0];

    if (cmd.equals(".help")) {
      System.out.println("Available Meta Commands:");
      System.out.println("  .tables              - List all tables");
      System.out.println("  .schema <table>      - Show schema for a table");
      System.out.println("  .seed <table> <num>  - Insert <num> random rows into <table>");
      System.out.println("  .export <table> <f>  - Export table to CSV file <f>");
      System.out.println("  .help                - Show this menu");
      System.out.println("  .exit                - Quit");
    } else if (cmd.equals(".tables")) {
      List<String> tables = db.getStorage().listTables();
      System.out.println("Tables: " + (tables.isEmpty() ? "None" : String.join(", ", tables)));
    } else if (cmd.equals(".schema") && parts.length > 1) {
      String tableName = parts[1];
      Table table = db.getStorage().getTable(tableName);
      if (table != null) {
        System.out.println("Schema for " + tableName + ":");
        for (Map.Entry<String, Column> entry : table.getColumns().entrySet()) {
          Column col = entry.getValue();
          List<String> extra = new ArrayList<>();
          if (col.isPrimaryKey()) extra.add("PK");
          if (col.isUnique()) extra.add("UNIQUE");
          if (!col.isNullable()) extra.add("NOT NULL");
          System.out.println(
              "  " + entry.getKey() + ": " + col.getDataType() + " " + String.join(" ", extra));
        }
      } else {
        System.out.println("Table " + tableName + " not found.");
      }
    } else if (cmd.equals(".seed") && parts.length > 2) {
      generateData(db, parts[1], Integer.parseInt(parts[2]));
    } else if (cmd.equals(".export") && parts.length > 2) {
      exportData(db, parts[1], parts[2]);
    } else {
      System.out.println("Unknown command: " + command);
    }
  }

  // ... (generateData, generateRandomString, exportData remain the same) ...
  private static void generateData(ByteForceDB db, String tableName, int count) {
    // ... implementation ...
    Table table = db.getStorage().getTable(tableName);
    if (table == null) {
      System.out.println("Table " + tableName + " not found.");
      return;
    }

    System.out.println("Seeding " + count + " rows into " + tableName + "...");
    long start = System.currentTimeMillis();
    int successful = 0;
    Random random = new Random();

    for (int i = 0; i < count; i++) {
      Map<String, Object> row = new HashMap<>();
      for (Column col : table.getColumns().values()) {
        Object val;
        if (col.isPrimaryKey() && col.getDataType() == DataType.INTEGER) {
          val = table.getRows().size() + 1 + i;
        } else if (col.getDataType() == DataType.INTEGER) {
          val = random.nextInt(10000) + 1;
        } else if (col.getDataType() == DataType.FLOAT) {
          val = 10.0 + (500.0 - 10.0) * random.nextDouble();
        } else if (col.getDataType() == DataType.BOOLEAN) {
          val = random.nextBoolean();
        } else {
          val = generateRandomString(8);
        }
        row.put(col.getName(), val);
      }

      try {
        table.addRow(row);
        successful++;
      } catch (Exception ignored) {
        // Skip constraint violations
      }
    }

    try {
      db.getStorage().saveTable(tableName);
      double time = (System.currentTimeMillis() - start) / 1000.0;
      System.out.printf("Successfully seeded %d rows in %.2fs.%n", successful, time);
    } catch (IOException e) {
      System.out.println("Error saving table: " + e.getMessage());
    }
  }

  private static String generateRandomString(int length) {
    String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    StringBuilder sb = new StringBuilder();
    Random random = new Random();
    for (int i = 0; i < length; i++) {
      sb.append(chars.charAt(random.nextInt(chars.length())));
    }
    return sb.toString();
  }

  private static void exportData(ByteForceDB db, String tableName, String filename) {
    Table table = db.getStorage().getTable(tableName);
    if (table == null) {
      System.out.println("Table " + tableName + " not found.");
      return;
    }

    if (table.getRows().isEmpty()) {
      System.out.println("Table is empty.");
      return;
    }

    try (FileWriter writer = new FileWriter(filename)) {
      // Header
      List<String> headers = new ArrayList<>(table.getColumns().keySet());
      writer.write(String.join(",", headers));
      writer.write("\n");

      // Rows
      for (Map<String, Object> row : table.getRows()) {
        List<String> values = new ArrayList<>();
        for (String header : headers) {
          Object val = row.get(header);
          values.add(val != null ? val.toString() : "");
        }
        writer.write(String.join(",", values));
        writer.write("\n");
      }
      System.out.println("Exported " + table.getRows().size() + " rows to " + filename);
    } catch (IOException e) {
      System.out.println("Export failed: " + e.getMessage());
    }
  }

  private static void displayResult(com.byteforce.core.ExecutionResult result) {
    if (result.isError()) {
      System.err.println(result.getMessage());
    } else if (result.isQuery()) {
      List<Map<String, Object>> rows = result.getRows();
      if (rows.isEmpty()) {
        System.out.println("Empty set.");
        return;
      }

      String[] headers = rows.get(0).keySet().toArray(new String[0]);
      String[][] data = new String[rows.size()][headers.length];

      for (int i = 0; i < rows.size(); i++) {
        Map<String, Object> row = rows.get(i);
        for (int j = 0; j < headers.length; j++) {
          Object val = row.get(headers[j]);
          data[i][j] = val != null ? val.toString() : "NULL";
        }
      }

      System.out.println(AsciiTable.getTable(headers, data));
      System.out.println(rows.size() + " row(s) in set.");
    } else {
      System.out.println(result.getMessage());
    }
  }
}
