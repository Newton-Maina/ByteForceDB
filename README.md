# ByteForceDB 💾

**My logic to creating this:** [How I Crafted](./How_I_Crafted.md)

**ByteForce** is a lightweight, Relational Database Management System (RDBMS) built from scratch. It is designed to demonstrate the core internals of a database engine—parsing, query planning, execution, indexing, and persistence—without the complexity of a production-grade system like PostgreSQL.

This project offers a unique **comparative study** by implementing the *exact same architecture* in two different languages: **Python** (Dynamic, Interpreted) and **Java** (Static, Compiled).

---

## 🌍 Two Worlds, One Logic

| Feature | [🐍 Python Edition](./python/) | [☕ Java Edition](./java/) |
| :--- | :--- | :--- |
| **Philosophy** | Rapid prototyping, readability, and flexibility. | Strict type safety, performance, and robustness. |
| **Parsing** | **Lark** (EBNF grammar). | **ANTLR4** (LL(*) parser generator). |
| **Type System** | Dynamic (Duck typing). | Static (Strongly typed models & Records). |
| **Execution** | Dictionary-based processing. | Strictly typed `ExecutionResult` and Maps. |
| **Build Tool** | `pip` & `venv`. | **Maven** (Dependency & Lifecycle management). |
| **Testing** | `pytest` (Concise assertions). | `JUnit 5` (Structured test lifecycle). |

---

## 🏗️ System Architecture

Both implementations follow a classic, layered database architecture:

```mermaid
graph TD
    User([User]) -->|SQL Command| REPL[Interactive CLI]
    REPL -->|Submit| Facade[ByteForceDB Facade]
    
    subgraph Core Engine
        Facade -->|1. Parse SQL| Parser[SQL Parser]
        Parser -->|2. AST / Plan| Facade
        Facade -->|3. Execute Plan| Executor[Execution Engine]
        
        Executor -->|Filter/Join| Optimizer[Query Optimizer]
        Optimizer -->|Read/Write| Storage[Storage Engine]
        
        subgraph In-Memory Models
            Table[Table]
            Index[Hash Index]
        end
        
        Storage -.->|Manage| Table
        Storage -.->|Manage| Index
    end
    
    Storage -->|Serialize to Disk| Disk[("File System")]
```

---

## 🚀 Key Features (Both Editions)

*   **SQL Subset**: Supports standard `CREATE TABLE`, `INSERT`, `SELECT`, `UPDATE`, `DELETE`.
*   **Joins**: Implements `INNER JOIN` logic to combine data across tables.
*   **Indexing**: Hash-based indexing for O(1) retrieval of records by key.
*   **Persistence**: Auto-saves data to disk (JSON/Pickle/Java Serialization) to persist state between runs.
*   **REPL**: A robust command-line interface with history, auto-completion, and ASCII table formatting.

---

## 🐍 Python Edition

*Best for understanding the logical flow and rapid experimentation.*

### Prerequisites
*   Python 3.9+

### Quick Start
```bash
cd python
./run.bat  # Windows (Auto-setup venv & run)
# or
python cli.py
```

### Visuals
**Logic & Architecture**
![Logic](python/assets/logic.png)

**Testing Suite (`pytest`)**
![Tests](python/assets/test.png)

**Linting (`black`)**
![Lint](python/assets/lint.png)

---

## ☕ Java Edition

*Best for seeing strict engineering practices, type safety, and build tooling.*

### Prerequisites
*   Java 21+
*   Maven 3.6+

### Quick Start
```bash
cd java
./run.bat  # Windows (Auto-compile & run)
# or
mvn clean install
java -jar target/byteforce-db-1.0-SNAPSHOT-jar-with-dependencies.jar
```

### Visuals
**Running the CLI**
![Run](java/src/assets/java-run.png)

**Build Process**
![Build](java/src/assets/java-build.png)

**Testing Suite (`JUnit 5`)**
![Tests](java/src/assets/java-tests.png)

**Linting (`Spotify FMT`)**
![Lint](java/src/assets/java-lint.png)

---

## 💡 Usage Example (Works in Both!)

Once the shell is running, you can execute standard SQL commands:

```sql
-- 1. Create a table
CREATE TABLE users (id INTEGER PRIMARY KEY, name TEXT)

-- 2. Insert data
INSERT INTO users VALUES (1, 'Alice')
INSERT INTO users VALUES (2, 'Bob')

-- 3. Create a secondary index for speed
CREATE INDEX idx_name ON users(name)

-- 4. Query with filtering (Uses Index!)
SELECT * FROM users WHERE name = 'Alice'

-- 5. Update data
UPDATE users SET name = 'Alicia' WHERE id = 1

-- 6. Complex Join
CREATE TABLE orders (oid INTEGER PRIMARY KEY, user_id INTEGER, amount FLOAT)
INSERT INTO orders VALUES (100, 1, 50.5)
SELECT name, amount FROM users JOIN orders ON id = user_id
```

---

## 📂 Project Structure

```text
ByteForceDB/
├── python/                 # Python Implementation
│   ├── cli.py              # Entry point
│   ├── core/               # Engine logic
│   └── tests/              # Pytest suite
│
├── java/                   # Java Implementation
│   ├── src/main/antlr4/    # SQL Grammar (Sql.g4)
│   ├── src/main/java/      # Core Engine & CLI
│   └── src/test/java/      # JUnit tests
│
└── How_I_Crafted.md        # Detailed engineering blog/logic
```

---
*Created by Newton Maina*
