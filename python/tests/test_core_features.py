import pytest
import os
import shutil
from core.database import ByteForceDB

@pytest.fixture
def db():
    """
    Pytest fixture that creates a fresh database instance in a temporary
    directory for each test function, ensuring test isolation.
    """
    test_dir = "test_data"
    if os.path.exists(test_dir):
        shutil.rmtree(test_dir)
    db = ByteForceDB(data_dir=test_dir)
    yield db
    if os.path.exists(test_dir):
        shutil.rmtree(test_dir)

def test_create_table(db):
    """Verifies that a table can be created and appears in the table list."""
    res = db.execute("CREATE TABLE users (id INTEGER PRIMARY KEY, name TEXT)")
    assert "created" in res
    assert "users" in db.storage.list_tables()

def test_insert_and_select(db):
    """Verifies basic data insertion and retrieval."""
    db.execute("CREATE TABLE users (id INTEGER PRIMARY KEY, name TEXT)")
    db.execute("INSERT INTO users VALUES (1, 'Alice')")
    db.execute("INSERT INTO users VALUES (2, 'Bob')")
    
    res = db.execute("SELECT * FROM users")
    assert len(res) == 2
    assert res[0]['name'] == 'Alice'
    assert res[1]['name'] == 'Bob'

def test_select_where(db):
    """Verifies filtering logic with WHERE clause."""
    db.execute("CREATE TABLE users (id INTEGER PRIMARY KEY, name TEXT)")
    db.execute("INSERT INTO users VALUES (1, 'Alice')")
    db.execute("INSERT INTO users VALUES (2, 'Bob')")
    
    res = db.execute("SELECT name FROM users WHERE id = 1")
    assert len(res) == 1
    assert res[0]['name'] == 'Alice'
    assert 'id' not in res[0] # Verify projection

def test_pk_constraint(db):
    """Verifies that duplicate Primary Keys raise an error."""
    db.execute("CREATE TABLE users (id INTEGER PRIMARY KEY, name TEXT)")
    db.execute("INSERT INTO users VALUES (1, 'Alice')")
    res = db.execute("INSERT INTO users VALUES (1, 'Bob')")
    assert "Error" in res
    assert "duplicate" in res.lower()

def test_join(db):
    """Verifies INNER JOIN logic between two tables."""
    db.execute("CREATE TABLE users (id INTEGER PRIMARY KEY, name TEXT)")
    db.execute("CREATE TABLE orders (oid INTEGER PRIMARY KEY, user_id INTEGER, amount FLOAT)")
    
    db.execute("INSERT INTO users VALUES (1, 'Alice')")
    db.execute("INSERT INTO users VALUES (2, 'Bob')")
    
    db.execute("INSERT INTO orders VALUES (101, 1, 50.5)")
    db.execute("INSERT INTO orders VALUES (102, 1, 20.0)")
    db.execute("INSERT INTO orders VALUES (103, 2, 100.0)")
    
    # JOIN orders ON user_id = id
    res = db.execute("SELECT name, amount FROM users JOIN orders ON id = user_id")
    assert len(res) == 3
    assert any(r['name'] == 'Alice' and r['amount'] == 50.5 for r in res)
    assert any(r['name'] == 'Bob' and r['amount'] == 100.0 for r in res)

def test_index(db):
    """
    Verifies that the Hash Index correctly retrieves data.
    Note: While this tests correctness, the performance benefit is verified manually via seeding.
    """
    db.execute("CREATE TABLE users (id INTEGER PRIMARY KEY, name TEXT)")
    for i in range(100):
        db.execute(f"INSERT INTO users VALUES ({i}, 'User {i}')")
    
    db.execute("CREATE INDEX idx_name ON users(name)")
    
    # This should use the index internally
    res = db.execute("SELECT * FROM users WHERE name = 'User 50'")
    assert len(res) == 1
    assert res[0]['id'] == 50

def test_update(db):
    """Verifies UPDATE functionality with a WHERE clause."""
    db.execute("CREATE TABLE users (id INTEGER PRIMARY KEY, name TEXT)")
    db.execute("INSERT INTO users VALUES (1, 'Alice')")
    db.execute("UPDATE users SET name = 'Alicia' WHERE id = 1")
    res = db.execute("SELECT name FROM users WHERE id = 1")
    assert res[0]['name'] == 'Alicia'

def test_delete(db):
    """Verifies DELETE functionality."""
    db.execute("CREATE TABLE users (id INTEGER PRIMARY KEY, name TEXT)")
    db.execute("INSERT INTO users VALUES (1, 'Alice')")
    db.execute("DELETE FROM users WHERE id = 1")
    res = db.execute("SELECT * FROM users")
    assert len(res) == 0

def test_limit(db):
    """Verifies the LIMIT clause restricts the number of returned rows."""
    db.execute("CREATE TABLE users (id INTEGER PRIMARY KEY, name TEXT)")
    for i in range(10):
        db.execute(f"INSERT INTO users VALUES ({i}, 'User {i}')")
    
    res = db.execute("SELECT * FROM users LIMIT 5")
    assert len(res) == 5
    assert res[0]['id'] == 0
    assert res[4]['id'] == 4