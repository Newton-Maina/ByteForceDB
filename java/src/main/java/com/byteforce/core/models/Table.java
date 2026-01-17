package com.byteforce.core.models;

import java.io.Serializable;
import java.util.*;

public class Table implements Serializable {
  private static final long serialVersionUID = 1L;

  private String name;
  private Map<String, Column> columns;
  private List<Map<String, Object>> rows;
  private Map<String, Map<Object, List<Integer>>> indices;

  public Table(String name, Map<String, Column> columns) {
    this.name = name;
    this.columns = columns;
    this.rows = new ArrayList<>();
    this.indices = new HashMap<>();
  }

  public String getName() {
    return name;
  }

  public Map<String, Column> getColumns() {
    return columns;
  }

  public List<Map<String, Object>> getRows() {
    return rows;
  }

  public Map<String, Map<Object, List<Integer>>> getIndices() {
    return indices;
  }

  public void addRow(Map<String, Object> rowData) {
    // Validate columns and check types
    for (Map.Entry<String, Column> entry : columns.entrySet()) {
      String colName = entry.getKey();
      Column column = entry.getValue();
      Object val = rowData.get(colName);

      if (!column.validate(val)) {
        throw new IllegalArgumentException("Invalid value for column '" + colName + "': " + val);
      }

      // Check constraints (Primary Key and Unique)
      if ((column.isPrimaryKey() || column.isUnique()) && val != null) {
        // Naive linear scan for uniqueness check (O(N))
        // Note: In a production DB, this would use the index if available.
        for (Map<String, Object> existingRow : rows) {
          if (Objects.equals(existingRow.get(colName), val)) {
            throw new IllegalArgumentException(
                "Constraint violation: duplicate value '"
                    + val
                    + "' for unique/PK column '"
                    + colName
                    + "'");
          }
        }
      }
    }

    // Track the index of the new row for indexing
    int rowIndex = rows.size();
    rows.add(rowData);

    // Update existing indices with the new row
    for (Map.Entry<String, Map<Object, List<Integer>>> entry : indices.entrySet()) {
      String colName = entry.getKey();
      Map<Object, List<Integer>> index = entry.getValue();

      Object val = rowData.get(colName);
      index.computeIfAbsent(val, k -> new ArrayList<>()).add(rowIndex);
    }
  }

  public void createIndex(String colName) {
    if (!columns.containsKey(colName)) {
      throw new IllegalArgumentException(
          "Column '" + colName + "' not found in table '" + name + "'");
    }

    Map<Object, List<Integer>> index = new HashMap<>();
    // Populate index with existing data
    for (int i = 0; i < rows.size(); i++) {
      Object val = rows.get(i).get(colName);
      index.computeIfAbsent(val, k -> new ArrayList<>()).add(i);
    }

    indices.put(colName, index);
  }

  public void setRows(List<Map<String, Object>> rows) {
    this.rows = rows;
  }
}
