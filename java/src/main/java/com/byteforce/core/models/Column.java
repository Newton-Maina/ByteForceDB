package com.byteforce.core.models;

import java.io.Serializable;

public class Column implements Serializable {
  private static final long serialVersionUID = 1L;

  private String name;
  private DataType dataType;
  private boolean isPrimaryKey;
  private boolean isUnique;
  private boolean isNullable;

  public Column(
      String name, DataType dataType, boolean isPrimaryKey, boolean isUnique, boolean isNullable) {
    this.name = name;
    this.dataType = dataType;
    this.isPrimaryKey = isPrimaryKey;
    this.isUnique = isUnique;
    this.isNullable = isNullable;
  }

  public String getName() {
    return name;
  }

  public DataType getDataType() {
    return dataType;
  }

  public boolean isPrimaryKey() {
    return isPrimaryKey;
  }

  public boolean isUnique() {
    return isUnique;
  }

  public boolean isNullable() {
    return isNullable;
  }

  public boolean validate(Object value) {
    if (value == null) {
      return isNullable;
    }

    switch (dataType) {
      case INTEGER:
        return value instanceof Integer;
      case TEXT:
        return value instanceof String;
      case BOOLEAN:
        return value instanceof Boolean;
      case FLOAT:
        // Allow Integer to pass as Float for convenience, or strictly Float/Double
        return value instanceof Float || value instanceof Double || value instanceof Integer;
      default:
        return false;
    }
  }
}
