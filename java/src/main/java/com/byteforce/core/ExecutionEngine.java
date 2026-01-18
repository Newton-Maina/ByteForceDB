package com.byteforce.core;

import com.byteforce.core.models.Column;
import com.byteforce.core.models.Table;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ExecutionEngine {
  private final StorageEngine storage;
  private List<Object> currentParams;
  private int paramIndex;

  public ExecutionEngine(StorageEngine storage) {
    this.storage = storage;
  }

  public ExecutionResult execute(Map<String, Object> plan, List<Object> params) {
    this.currentParams = params != null ? params : new ArrayList<>();
    this.paramIndex = 0;

    String cmdType = (String) plan.get("type");
    try {
      switch (cmdType) {
        case "create_table":
          return executeCreateTable(plan);
        case "insert":
          return executeInsert(plan);
        case "select":
          return executeSelect(plan);
        case "create_index":
          return executeCreateIndex(plan);
        case "update":
          return executeUpdate(plan);
        case "delete":
          return executeDelete(plan);
        default:
          throw new IllegalArgumentException("Unknown command type: " + cmdType);
      }
    } catch (Exception e) {
      // Allow specific exceptions to bubble up or wrap them
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  private <T> T safeGet(Map<String, Object> plan, String key) {
    return (T) plan.get(key);
  }

  private Object resolveValue(Object valObj) {
    if (valObj instanceof Map) {
      Map<?, ?> map = (Map<?, ?>) valObj;
      if ("placeholder".equals(map.get("type"))) {
        if (paramIndex >= currentParams.size()) {
          throw new IllegalArgumentException(
              "Not enough parameters provided for query placeholders.");
        }
        return currentParams.get(paramIndex++);
      }
    }
    return valObj;
  }

  private ExecutionResult executeCreateIndex(Map<String, Object> plan) throws IOException {
    String tableName = (String) plan.get("table_name");
    Table table = storage.getTable(tableName);
    if (table == null) throw new IllegalArgumentException("Table '" + tableName + "' not found");

    String colName = (String) plan.get("column_name");
    table.createIndex(colName);
    storage.saveTable(tableName);
    return ExecutionResult.success(
        "Index '" + plan.get("index_name") + "' created on " + tableName + "(" + colName + ").");
  }

  private ExecutionResult executeCreateTable(Map<String, Object> plan) throws IOException {
    String tableName = (String) plan.get("table_name");
    List<Column> columnList = safeGet(plan, "columns");
    Map<String, Column> columns = new LinkedHashMap<>();
    for (Column col : columnList) {
      columns.put(col.getName(), col);
    }

    Table table = new Table(tableName, columns);
    storage.createTable(table);
    return ExecutionResult.success("Table '" + tableName + "' created.");
  }

  private ExecutionResult executeInsert(Map<String, Object> plan) throws IOException {
    String tableName = (String) plan.get("table_name");
    Table table = storage.getTable(tableName);
    if (table == null) throw new IllegalArgumentException("Table '" + tableName + "' not found");

    List<String> targetColumns = safeGet(plan, "columns");
    List<Object> rawValues = safeGet(plan, "values");
    List<Object> values = rawValues.stream().map(this::resolveValue).collect(Collectors.toList());

    Map<String, Object> rowData = new HashMap<>();
    if (targetColumns != null) {
      if (targetColumns.size() != values.size()) {
        throw new IllegalArgumentException("Column count doesn't match value count");
      }
      for (int i = 0; i < targetColumns.size(); i++) {
        rowData.put(targetColumns.get(i), values.get(i));
      }
    } else {
      // Implicit column order based on creation (LinkedHashMap)
      if (values.size() != table.getColumns().size()) {
        throw new IllegalArgumentException("Value count doesn't match table column count");
      }
      int i = 0;
      for (String colName : table.getColumns().keySet()) {
        rowData.put(colName, values.get(i++));
      }
    }

    table.addRow(rowData);
    storage.saveTable(tableName);
    return ExecutionResult.success("1 row inserted.");
  }

  private ExecutionResult executeSelect(Map<String, Object> plan) {
    String tableName = (String) plan.get("table_name");
    Table table = storage.getTable(tableName);
    if (table == null) throw new IllegalArgumentException("Table '" + tableName + "' not found");

    List<Map<String, Object>> results = new ArrayList<>(table.getRows());
    Map<String, Object> join = safeGet(plan, "join");
    Map<String, Object> where = safeGet(plan, "where");

    // Resolve WHERE
    Map<String, Object> resolvedWhere = null;
    if (where != null) {
      resolvedWhere = new HashMap<>(where);
      resolvedWhere.put("value", resolveValue(where.get("value")));
    }

    // Index Optimization
    if (resolvedWhere != null && join == null) {
      String col = (String) resolvedWhere.get("column");
      String op = (String) resolvedWhere.get("operator");
      Object valObj = resolvedWhere.get("value");

      if ("=".equals(op) && table.getIndices().containsKey(col)) {
        // Check if value is a column reference (cannot index scan that easily)
        boolean isColRef =
            valObj instanceof Map && "column".equals(((Map<?, ?>) valObj).get("type"));
        if (!isColRef) {
          List<Integer> rowIndices = table.getIndices().get(col).get(valObj);
          if (rowIndices != null) {
            results = new ArrayList<>();
            for (int idx : rowIndices) {
              results.add(table.getRows().get(idx));
            }
          } else {
            results = new ArrayList<>();
          }
        }
      }
    }

    // JOIN
    if (join != null) {
      String joinTableName = (String) join.get("join_table");
      boolean isLeft = (Boolean) join.getOrDefault("is_left", false);
      Table joinTable = storage.getTable(joinTableName);
      if (joinTable == null)
        throw new IllegalArgumentException("Join table '" + joinTableName + "' not found");

      Map<String, Object> condition = safeGet(join, "condition");
      List<Map<String, Object>> joinedResults = new ArrayList<>();

      for (Map<String, Object> leftRow : results) {
        boolean matched = false;
        for (Map<String, Object> rightRow : joinTable.getRows()) {
          if (evaluateJoinCondition(leftRow, rightRow, condition)) {
            Map<String, Object> merged = new HashMap<>(leftRow);
            merged.putAll(rightRow);
            joinedResults.add(merged);
            matched = true;
          }
        }
        if (!matched && isLeft) {
          Map<String, Object> merged = new HashMap<>(leftRow);
          // Fill right table columns with null
          for (String rightCol : joinTable.getColumns().keySet()) {
            if (!merged.containsKey(rightCol)) {
              merged.put(rightCol, null);
            }
          }
          joinedResults.add(merged);
        }
      }
      results = joinedResults;
    }

    // Filter
    if (resolvedWhere != null) {
      final Map<String, Object> w = resolvedWhere;
      results = results.stream().filter(row -> evaluateWhere(row, w)).collect(Collectors.toList());
    }

    // Projection
    Object selectedColsObj = plan.get("columns");
    if (!"*".equals(selectedColsObj)) {
      List<String> selectedCols = safeGet(plan, "columns");
      results =
          results.stream()
              .map(
                  row -> {
                    Map<String, Object> newRow = new LinkedHashMap<>(); // Keep order
                    for (String col : selectedCols) {
                      newRow.put(col, row.get(col));
                    }
                    return newRow;
                  })
              .collect(Collectors.toList());
    }

    // Limit
    if (plan.containsKey("limit")) {
      int limit = (int) plan.get("limit");
      if (limit < results.size()) {
        results = results.subList(0, limit);
      }
    }

    return ExecutionResult.queryResult(results);
  }

  private ExecutionResult executeUpdate(Map<String, Object> plan) throws IOException {
    String tableName = (String) plan.get("table_name");
    Table table = storage.getTable(tableName);
    if (table == null) throw new IllegalArgumentException("Table '" + tableName + "' not found");

    Map<String, Object> assignments = safeGet(plan, "assignments");
    Map<String, Object> where = safeGet(plan, "where");

    Map<String, Object> resolvedAssignments = new HashMap<>();
    for (Map.Entry<String, Object> entry : assignments.entrySet()) {
      resolvedAssignments.put(entry.getKey(), resolveValue(entry.getValue()));
    }

    Map<String, Object> resolvedWhere = null;
    if (where != null) {
      resolvedWhere = new HashMap<>(where);
      resolvedWhere.put("value", resolveValue(where.get("value")));
    }

    int count = 0;
    for (Map<String, Object> row : table.getRows()) {
      if (resolvedWhere == null || evaluateWhere(row, resolvedWhere)) {
        for (Map.Entry<String, Object> entry : resolvedAssignments.entrySet()) {
          String col = entry.getKey();
          Object val = entry.getValue();

          if (!table.getColumns().containsKey(col)) {
            throw new IllegalArgumentException("Column '" + col + "' not found");
          }
          if (!table.getColumns().get(col).validate(val)) {
            throw new IllegalArgumentException("Invalid value for column '" + col + "': " + val);
          }
          row.put(col, val);
        }
        count++;
      }
    }

    if (count > 0) {
      // Rebuild indices
      for (String colName : table.getIndices().keySet()) {
        table.createIndex(colName);
      }
      storage.saveTable(tableName);
    }

    return ExecutionResult.success(count + " row(s) updated.");
  }

  private ExecutionResult executeDelete(Map<String, Object> plan) throws IOException {
    String tableName = (String) plan.get("table_name");
    Table table = storage.getTable(tableName);
    if (table == null) throw new IllegalArgumentException("Table '" + tableName + "' not found");

    Map<String, Object> where = safeGet(plan, "where");
    Map<String, Object> resolvedWhere = null;
    if (where != null) {
      resolvedWhere = new HashMap<>(where);
      resolvedWhere.put("value", resolveValue(where.get("value")));
    }

    int initialCount = table.getRows().size();
    final Map<String, Object> w = resolvedWhere;

    List<Map<String, Object>> newRows =
        table.getRows().stream()
            .filter(row -> !(w == null || evaluateWhere(row, w)))
            .collect(Collectors.toList());

    table.setRows(newRows);
    int finalCount = table.getRows().size();
    int count = initialCount - finalCount;

    if (count > 0) {
      for (String colName : table.getIndices().keySet()) {
        table.createIndex(colName);
      }
      storage.saveTable(tableName);
    }

    return ExecutionResult.success(count + " row(s) deleted.");
  }

  private boolean evaluateJoinCondition(
      Map<String, Object> leftRow, Map<String, Object> rightRow, Map<String, Object> condition) {
    String col = (String) condition.get("column");
    String op = (String) condition.get("operator");
    Object valObj = condition.get("value");

    Object leftVal = leftRow.get(col);
    Object rightVal;

    if (valObj instanceof Map && "column".equals(((Map<?, ?>) valObj).get("type"))) {
      rightVal = rightRow.get(((Map<?, ?>) valObj).get("name"));
    } else {
      rightVal = valObj;
    }

    return compare(leftVal, op, rightVal);
  }

  private boolean evaluateWhere(Map<String, Object> row, Map<String, Object> where) {
    String col = (String) where.get("column");
    String op = (String) where.get("operator");
    Object valObj = where.get("value");
    Object rowVal = row.get(col);

    Object targetVal;
    if (valObj instanceof Map && "column".equals(((Map<?, ?>) valObj).get("type"))) {
      targetVal = row.get(((Map<?, ?>) valObj).get("name"));
    } else {
      targetVal = valObj;
    }

    return compare(rowVal, op, targetVal);
  }

  @SuppressWarnings("unchecked")
  private boolean compare(Object left, String op, Object right) {
    if (left == null || right == null) return left == right; // Simple null check

    // Convert to compatible types for comparison
    if (left instanceof Number && right instanceof Number) {
      double l = ((Number) left).doubleValue();
      double r = ((Number) right).doubleValue();
      switch (op) {
        case "=":
          return l == r;
        case ">":
          return l > r;
        case "<":
          return l < r;
        case ">=":
          return l >= r;
        case "<=":
          return l <= r;
        case "!=":
          return l != r;
      }
    } else if (left instanceof Comparable && left.getClass().isInstance(right)) {
      Comparable<Object> c1 = (Comparable<Object>) left;
      int cmp = c1.compareTo(right);
      switch (op) {
        case "=":
          return cmp == 0;
        case ">":
          return cmp > 0;
        case "<":
          return cmp < 0;
        case ">=":
          return cmp >= 0;
        case "<=":
          return cmp <= 0;
        case "!=":
          return cmp != 0;
      }
    } else {
      // Fallback for equality only
      switch (op) {
        case "=":
          return left.equals(right);
        case "!=":
          return !left.equals(right);
      }
    }
    return false;
  }
}
