import pytest
from core.database import ByteForceDB

@pytest.fixture
def db():
    # Use a temporary directory for test data
    import shutil
    import os
    test_dir = "test_data_joins"
    if os.path.exists(test_dir):
        shutil.rmtree(test_dir)
    
    db_instance = ByteForceDB(data_dir=test_dir)
    yield db_instance
    
    if os.path.exists(test_dir):
        shutil.rmtree(test_dir)

def test_left_join_functionality(db):
    # 1. Setup tables
    db.execute("CREATE TABLE authors (id INTEGER PRIMARY KEY, name TEXT)")
    db.execute("CREATE TABLE books (bid INTEGER PRIMARY KEY, author_id INTEGER, title TEXT)")
    
    # 2. Insert data
    db.execute("INSERT INTO authors VALUES (1, 'Alice')")
    db.execute("INSERT INTO authors VALUES (2, 'Bob')") # Bob has no books
    
    db.execute("INSERT INTO books VALUES (101, 1, 'Alice Adventures')")
    
    # 3. Test INNER JOIN (Standard) - Should only return Alice
    inner_results = db.execute("SELECT name, title FROM authors JOIN books ON id = author_id")
    assert len(inner_results) == 1
    assert inner_results[0]['name'] == 'Alice'
    
    # 4. Test LEFT JOIN - Should return Alice (with book) AND Bob (with None book)
    left_results = db.execute("SELECT name, title FROM authors LEFT JOIN books ON id = author_id")
    
    assert len(left_results) == 2
    
    # Find Alice and Bob in results
    alice_row = next(r for r in left_results if r['name'] == 'Alice')
    bob_row = next(r for r in left_results if r['name'] == 'Bob')
    
    assert alice_row['title'] == 'Alice Adventures'
    assert bob_row['title'] is None  # This is the core of LEFT JOIN success
