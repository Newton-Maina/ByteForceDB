package com.byteforce.core;

import java.util.List;
import java.util.Map;

public class ByteForceDB {
  private final StorageEngine storage;
  private final ExecutionEngine executor;
  private final SQLParser parser;

  public ByteForceDB() {
    this("data");
  }

  public ByteForceDB(String dataDir) {
    this.storage = new StorageEngine(dataDir);
    this.storage.loadAllTables();
    this.parser = new SQLParser();
    this.executor = new ExecutionEngine(this.storage);
  }

  public ExecutionResult execute(String sql) {
    return execute(sql, null);
  }

  public ExecutionResult execute(String sql, List<Object> params) {
    try {
      Map<String, Object> plan = parser.parse(sql);
      return executor.execute(plan, params);
    } catch (Exception e) {
      return ExecutionResult.error("Error: " + e.getMessage());
    }
  }

  public StorageEngine getStorage() {
    return storage;
  }
}
