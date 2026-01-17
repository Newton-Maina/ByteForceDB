package com.byteforce;

import static org.junit.jupiter.api.Assertions.*;

import com.byteforce.core.ByteForceDB;
import com.byteforce.core.models.Table;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CoreFeaturesTest {

  private ByteForceDB db;
  private static final String TEST_DIR = "test_data";

  @BeforeEach
  void setUp() {
    // Clean up test dir
    deleteDirectory(new File(TEST_DIR));
    db = new ByteForceDB(TEST_DIR);
  }

  @AfterEach
  void tearDown() {
    deleteDirectory(new File(TEST_DIR));
  }

  private void deleteDirectory(File dir) {
    if (dir.exists()) {
      try {
        Files.walk(dir.toPath())
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  @Test
  void testCreateTable() {
    String sql = "CREATE TABLE users (id INTEGER PRIMARY KEY, name TEXT)";
    com.byteforce.core.ExecutionResult result = db.execute(sql);
    assertEquals("Table 'users' created.", result.getMessage());

    Table table = db.getStorage().getTable("users");
    assertNotNull(table);
    assertTrue(table.getColumns().containsKey("id"));
    assertTrue(table.getColumns().containsKey("name"));
  }

  @Test
  void testInsertAndSelect() {
    db.execute("CREATE TABLE users (id INTEGER, name TEXT)");
    db.execute("INSERT INTO users VALUES (1, 'Alice')");
    db.execute("INSERT INTO users VALUES (2, 'Bob')");

    com.byteforce.core.ExecutionResult result = db.execute("SELECT * FROM users");
    List<Map<String, Object>> rows = result.getRows();
    assertEquals(2, rows.size());
    assertEquals(1, rows.get(0).get("id"));
    assertEquals("Alice", rows.get(0).get("name"));
  }

  @Test
  void testFilter() {
    db.execute("CREATE TABLE users (id INTEGER, name TEXT)");
    db.execute("INSERT INTO users VALUES (1, 'Alice')");
    db.execute("INSERT INTO users VALUES (2, 'Bob')");

    com.byteforce.core.ExecutionResult result =
        db.execute("SELECT * FROM users WHERE name = 'Alice'");
    List<Map<String, Object>> rows = result.getRows();
    assertEquals(1, rows.size());
    assertEquals("Alice", rows.get(0).get("name"));
  }

  @Test
  void testUpdate() {
    db.execute("CREATE TABLE users (id INTEGER, name TEXT)");
    db.execute("INSERT INTO users VALUES (1, 'Alice')");

    db.execute("UPDATE users SET name = 'Alicia' WHERE id = 1");

    com.byteforce.core.ExecutionResult result = db.execute("SELECT * FROM users");
    List<Map<String, Object>> rows = result.getRows();
    assertEquals("Alicia", rows.get(0).get("name"));
  }

  @Test
  void testDelete() {
    db.execute("CREATE TABLE users (id INTEGER, name TEXT)");
    db.execute("INSERT INTO users VALUES (1, 'Alice')");
    db.execute("INSERT INTO users VALUES (2, 'Bob')");

    db.execute("DELETE FROM users WHERE id = 1");

    com.byteforce.core.ExecutionResult result = db.execute("SELECT * FROM users");
    List<Map<String, Object>> rows = result.getRows();
    assertEquals(1, rows.size());
    assertEquals("Bob", rows.get(0).get("name"));
  }
}
