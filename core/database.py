from typing import Any, List
from .parser import SQLParser
from .storage import StorageEngine
from .executor import ExecutionEngine

class ByteForceDB:
    """
    The main facade for the ByteForce Database.
    
    It orchestrates the flow of data between the Parser, Storage Engine, and Execution Engine.
    """
    def __init__(self, data_dir: str = "data"):
        """
        Initialize the database system.
        
        Args:
            data_dir: The directory to store database files.
        """
        self.storage = StorageEngine(data_dir)
        self.storage.load_all_tables()
        self.parser = SQLParser()
        self.executor = ExecutionEngine(self.storage)

    def execute(self, sql: str, params: List[Any] = None):
        """
        Executes a SQL query.
        
        Args:
            sql: The SQL command string.
            params: Optional list of parameters for parameterized queries (prevent SQL injection).
            
        Returns:
            The result of the execution (e.g., query results or status message).
        """
        try:
            plan = self.parser.parse(sql)
            return self.executor.execute(plan, params)
        except Exception as e:
            return f"Error: {str(e)}"
    
    def execute_safe(self, sql: str, params: List[Any]):
        """
        Explicitly executes a parameterized query.
        """
        return self.execute(sql, params)
