from dataclasses import dataclass, field
from enum import Enum, auto
from typing import Any, Dict, List, Optional, Union

class DataType(Enum):
    """
    Enumeration of supported data types in ByteForce.
    """
    INTEGER = auto()
    TEXT = auto()
    BOOLEAN = auto()
    FLOAT = auto()

@dataclass
class Column:
    """
    Represents a column definition in a table.
    
    Attributes:
        name: The name of the column.
        data_type: The data type allowed in this column.
        is_primary_key: True if this column is the Primary Key.
        is_unique: True if values in this column must be unique.
        is_nullable: True if this column accepts NULL (None) values.
    """
    name: str
    data_type: DataType
    is_primary_key: bool = False
    is_unique: bool = False
    is_nullable: bool = True

    def validate(self, value: Any) -> bool:
        """
        Validates if a value matches the column's data type.
        
        Args:
            value: The value to validate.
            
        Returns:
            bool: True if valid, False otherwise.
        """
        if value is None:
            return self.is_nullable
        
        if self.data_type == DataType.INTEGER:
            return isinstance(value, int)
        elif self.data_type == DataType.TEXT:
            return isinstance(value, str)
        elif self.data_type == DataType.BOOLEAN:
            return isinstance(value, bool)
        elif self.data_type == DataType.FLOAT:
            return isinstance(value, (int, float))
        return False

@dataclass
class Table:
    """
    Represents a database table containing columns, rows, and indices.
    
    Attributes:
        name: Name of the table.
        columns: Dictionary mapping column names to Column definitions.
        rows: List of dictionaries, where each dictionary represents a row.
        indices: Dictionary for storing index data (column_name -> {value -> [row_indices]}).
    """
    name: str
    columns: Dict[str, Column]
    rows: List[Dict[str, Any]] = field(default_factory=list)
    indices: Dict[str, Dict[Any, List[int]]] = field(default_factory=dict)

    def add_row(self, row_data: Dict[str, Any]):
        """
        Inserts a new row into the table, enforcing type checks and constraints.
        
        Args:
            row_data: A dictionary representing the new row {col_name: value}.
            
        Raises:
            ValueError: If a value has the wrong type or violates a constraint (PK/Unique).
        """
        # Validate columns and check types
        for col_name, column in self.columns.items():
            val = row_data.get(col_name)
            if not column.validate(val):
                raise ValueError(f"Invalid value for column '{col_name}': {val}")
            
            # Check constraints (Primary Key and Unique)
            if (column.is_primary_key or column.is_unique) and val is not None:
                # Naive linear scan for uniqueness check (O(N))
                # Note: In a production DB, this would use the index if available.
                if any(r.get(col_name) == val for r in self.rows):
                    raise ValueError(f"Constraint violation: duplicate value '{val}' for unique/PK column '{col_name}'")

        # Track the index of the new row for indexing
        row_index = len(self.rows)
        self.rows.append(row_data)
        
        # Update existing indices with the new row
        for col_name, index in self.indices.items():
            val = row_data.get(col_name)
            if val not in index:
                index[val] = []
            index[val].append(row_index)

    def create_index(self, col_name: str):
        """
        Creates a Hash Index on the specified column for O(1) equality lookups.
        
        Args:
            col_name: The name of the column to index.
            
        Raises:
            ValueError: If the column does not exist.
        """
        if col_name not in self.columns:
            raise ValueError(f"Column '{col_name}' not found in table '{self.name}'")
        
        index = {}
        # Populate index with existing data
        for i, row in enumerate(self.rows):
            val = row.get(col_name)
            if val not in index:
                index[val] = []
            index[val].append(i)
        
        self.indices[col_name] = index