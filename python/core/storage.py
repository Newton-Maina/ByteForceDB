import pickle
import os
from typing import Dict, Optional, List
from .models import Table


class StorageEngine:
    """
    Manages the persistence of database tables to disk.

    Each table is serialized as a separate .db file using Python's pickle module.
    """

    def __init__(self, data_dir: str = "data"):
        """
        Initialize the storage engine.

        Args:
            data_dir: The directory where table files will be stored.
        """
        self.data_dir = data_dir
        self.tables: Dict[str, Table] = {}
        if not os.path.exists(self.data_dir):
            os.makedirs(self.data_dir)

    def create_table(self, table: Table):
        """
        Registers a new table in the system and persists it.

        Args:
            table: The Table object to create.

        Raises:
            ValueError: If a table with the same name already exists.
        """
        if table.name in self.tables:
            raise ValueError(f"Table '{table.name}' already exists")
        self.tables[table.name] = table
        self.save_table(table.name)

    def get_table(self, name: str) -> Optional[Table]:
        """
        Retrieves a table by name.

        Args:
            name: The name of the table.

        Returns:
            Table: The table object, or None if not found.
        """
        return self.tables.get(name)

    def save_table(self, name: str):
        """
        Serializes and saves a specific table to disk.

        Args:
            name: The name of the table to save.
        """
        if name not in self.tables:
            return
        file_path = os.path.join(self.data_dir, f"{name}.db")
        with open(file_path, "wb") as f:
            pickle.dump(self.tables[name], f)

    def load_all_tables(self):
        """
        Loads all existing .db files from the data directory into memory.
        """
        for filename in os.listdir(self.data_dir):
            if filename.endswith(".db"):
                table_name = filename[:-3]
                file_path = os.path.join(self.data_dir, filename)
                with open(file_path, "rb") as f:
                    self.tables[table_name] = pickle.load(f)

    def list_tables(self) -> List[str]:
        """
        Returns a list of all table names in the database.
        """
        return list(self.tables.keys())
