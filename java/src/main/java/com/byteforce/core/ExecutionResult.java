package com.byteforce.core;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents the result of a database execution. This class ensures type safety by avoiding the use
 * of raw 'Object' return types.
 */
public class ExecutionResult {
  private final boolean isQuery;
  private final String message;
  private final List<Map<String, Object>> rows;
  private final boolean isError;

  private ExecutionResult(
      boolean isQuery, String message, List<Map<String, Object>> rows, boolean isError) {
    this.isQuery = isQuery;
    this.message = message;
    this.rows = rows;
    this.isError = isError;
  }

  public static ExecutionResult success(String message) {
    return new ExecutionResult(false, message, Collections.emptyList(), false);
  }

  public static ExecutionResult queryResult(List<Map<String, Object>> rows) {
    return new ExecutionResult(true, null, rows, false);
  }

  public static ExecutionResult error(String errorMessage) {
    return new ExecutionResult(false, errorMessage, Collections.emptyList(), true);
  }

  public boolean isQuery() {
    return isQuery;
  }

  public String getMessage() {
    return message;
  }

  public List<Map<String, Object>> getRows() {
    return rows;
  }

  public boolean isError() {
    return isError;
  }
}
