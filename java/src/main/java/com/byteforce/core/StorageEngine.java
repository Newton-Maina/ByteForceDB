package com.byteforce.core;

import com.byteforce.core.models.Table;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StorageEngine {
  private final String dataDir;
  private final Map<String, Table> tables;

  public StorageEngine(String dataDir) {
    this.dataDir = dataDir;
    this.tables = new HashMap<>();
    File dir = new File(dataDir);
    if (!dir.exists()) {
      dir.mkdirs();
    }
  }

  public void createTable(Table table) throws IOException {
    if (tables.containsKey(table.getName())) {
      throw new IllegalArgumentException("Table '" + table.getName() + "' already exists");
    }
    tables.put(table.getName(), table);
    saveTable(table.getName());
  }

  public Table getTable(String name) {
    return tables.get(name);
  }

  public void saveTable(String name) throws IOException {
    Table table = tables.get(name);
    if (table == null) return;

    File file = new File(dataDir, name + ".db");
    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
      oos.writeObject(table);
    }
  }

  public void loadAllTables() {
    File dir = new File(dataDir);
    File[] files = dir.listFiles((d, name) -> name.endsWith(".db"));

    if (files != null) {
      for (File file : files) {
        String tableName = file.getName().substring(0, file.getName().length() - 3);
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
          Table table = (Table) ois.readObject();
          tables.put(tableName, table);
        } catch (IOException | ClassNotFoundException e) {
          System.err.println("Failed to load table " + tableName + ": " + e.getMessage());
        }
      }
    }
  }

  public List<String> listTables() {
    return new ArrayList<>(tables.keySet());
  }
}
