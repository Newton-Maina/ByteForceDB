import pytest
import os
import shutil
from core.database import ByteForceDB

@pytest.fixture
def db():
    """
    Creates an isolated database instance for security testing.
    Uses 'test_data_sec' directory to avoid conflict with core tests.
    """
    test_dir = "test_data_sec"
    if os.path.exists(test_dir):
        shutil.rmtree(test_dir)
    db = ByteForceDB(data_dir=test_dir)
    yield db
    if os.path.exists(test_dir):
        shutil.rmtree(test_dir)

def test_parameterized_insert(db):
    """Verifies that INSERT works correctly with parameterized inputs."""
    db.execute("CREATE TABLE users (id INTEGER PRIMARY KEY, name TEXT)")
    
    # Safe insert with params
    db.execute("INSERT INTO users VALUES (?, ?)", [1, "Alice"])
    db.execute("INSERT INTO users VALUES (?, ?)", [2, "Bob"])
    
    res = db.execute("SELECT * FROM users")
    assert len(res) == 2
    assert res[0]['name'] == 'Alice'

def test_parameterized_select(db):
    """Verifies that SELECT works with parameterized WHERE clauses."""
    db.execute("CREATE TABLE users (id INTEGER PRIMARY KEY, name TEXT)")
    db.execute("INSERT INTO users VALUES (1, 'Alice')")
    db.execute("INSERT INTO users VALUES (2, 'Bob')")
    
    # Safe select
    res = db.execute("SELECT * FROM users WHERE name = ?", ["Alice"])
    assert len(res) == 1
    assert res[0]['id'] == 1
    
    # Verify non-match
    res = db.execute("SELECT * FROM users WHERE name = ?", ["Charlie"])
    assert len(res) == 0

def test_injection_attempt_handled_safely(db):
    """
    Demonstrates that a classic SQL injection string is treated as a literal value.
    The engine should look for a user literally named "' OR '1'='1" rather than
    interpreting the OR clause.
    """
    db.execute("CREATE TABLE users (id INTEGER PRIMARY KEY, name TEXT)")
    db.execute("INSERT INTO users VALUES (1, 'Admin')")
    
    malicious_input = "' OR '1'='1"
    
    # Parameterized query
    res = db.execute("SELECT * FROM users WHERE name = ?", [malicious_input])
    
    # Should return 0 results, proving the injection failed to retrieve all rows
    assert len(res) == 0

def test_parameter_mismatch_error(db):
    """Verifies that passing fewer parameters than placeholders raises an error."""
    db.execute("CREATE TABLE users (id INTEGER PRIMARY KEY)")
    
    res = db.execute("INSERT INTO users VALUES (?)", []) # Missing param
    assert "Error" in res