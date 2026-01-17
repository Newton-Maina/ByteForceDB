# ByteForceDB - Java Edition ☕

**My logic to creating this:** [How I Crafted](../How_I_Crafted.md)

Welcome to the **Java Edition** of ByteForce. While the Python version focuses on being dynamic and readable, this implementation is all about **strict engineering rigor**. I built this port to demonstrate how type safety, compilation, and robust tooling can catch errors *before* the code even runs.

If you want to see how a database engine handles strict schemas and structured execution plans, you are in the right place.

---

## 🏗️ System Architecture

The Java implementation adheres to the same logical architecture as the Python version but leverages Java's type system and ecosystem for robustness.

```mermaid
graph TD
    User([User]) -->|SQL Command| REPL[JLine CLI]
    REPL -->|Submit| Facade[ByteForceDB Facade]
    
    subgraph Core Engine
        Facade -->|1. Parse SQL| Parser[ANTLR4 Parser]
        Parser -->|2. AST / Plan| Facade
        Facade -->|3. Execute Plan| Executor[Execution Engine]
        
        Executor -->|Filter/Join| Optimizer[Query Optimizer]
        Optimizer -->|Read/Write| Storage[Storage Engine]
        
        subgraph In-Memory Models
            Table[Table Class]
            Index[HashMap Index]
        end
        
        Storage -.->|Manage| Table
        Storage -.->|Manage| Index
    end
    
    Storage -->|Serialize (ObjectOutputStream)| Disk[("File System")]
```

---

## ✨ Key Features

-   **SQL Interface**: Support for standard DDL and DML operations (`CREATE`, `INSERT`, `SELECT`, `UPDATE`, `DELETE`).
-   **Strict Parsing**: Robust SQL parsing using **ANTLR4** (LL(*) parser generator), preventing ambiguity.
-   **Performance**:
    -   **Hash Indexing**: O(1) lookups for equality searches.
    -   **Query Optimization**: Automatically utilizes indices for `WHERE` clauses.
-   **Relational Algebra**: Supports `INNER JOIN` operations to combine data across tables.
-   **Type Safety**:
    -   Uses **Java Records** and **Enums** (`DataType`) to enforce schema validity.
    -   Custom `ExecutionResult` types to prevent runtime casting errors.
-   **Persistence**: Automatic serialization to disk via `ObjectOutputStream`, ensuring data survives restarts.
-   **Rich REPL (Interactive Shell)**:
    -   **Auto-complete**: Context-aware suggestions for SQL keywords (`SELECT`, `FROM`, ...) and meta-commands.
    -   **History**: Persists command history to `~/.byteforce_java_history`.
    -   **Formatting**: Beautiful ASCII tables for query results.

---

## 🚀 Getting Started

### Prerequisites
*   **Java**: JDK 21+
*   **Maven**: 3.6+

### Option A: Automated Setup (Windows) ⚡
I've included batch scripts to make your life easier. Just click and go!

*   **Run**: Double-click `run.bat` (Compiles and starts the interactive CLI).
*   **Test**: Double-click `test.bat` (Runs the JUnit test suite).

### Option B: Manual Workflow 🛠️

#### 1. Build the Project
This step generates the ANTLR sources, compiles the code, and packages the JAR.
```bash
mvn clean install
```

#### 2. Run the Database
Launch the interactive shell using the shaded JAR (Fat JAR):
```bash
java -jar target/byteforce-db-1.0-SNAPSHOT-jar-with-dependencies.jar
```

---

## 💡 Usage Guide

### Meta Commands
ByteForce CLI supports special commands for managing the database environment:

-   `.tables`: List all tables.
-   `.schema <table>`: Show the structure of a table (columns, types, constraints).
-   `.seed <table> <num>`: Automatically insert `<count>` random rows for performance testing.
-   `.export <table> <f>`: Export a table's data to a CSV file.
-   `.help`: Show available commands.
-   `.exit`: Quit the application.

### SQL Examples
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

## 🛠️ Development Workflow

### Code Quality (Linting)
We enforce code style (Google Java Format) automatically using the Spotless/FMT plugin.
```bash
mvn com.spotify.fmt:fmt-maven-plugin:format
```

### Running Tests
We use **JUnit 5** for unit testing core database features.
```bash
mvn test
```

---

## 🆚 Comparison: Java vs. Python Implementation

| Feature | Java Edition | Python Edition |
| :--- | :--- | :--- |
| **Parsing** | **ANTLR4** (Generated code, strict grammar) | **Lark** (Dynamic EBNF parsing) |
| **Typing** | **Static** (Compile-time checks) | **Dynamic** (Runtime duck typing) |
| **Data Models** | Classes (`Table`, `Column`, `DataType` Enum) | Dictionaries & Dataclasses |
| **Safety** | High (Compiler catches type errors) | Moderate (Relies on extensive testing) |
| **Boilerplate** | High (Getters/Setters, Visitors, Factories) | Low (Concise logic) |

## 📂 Project Structure

```text
java/
├── src/main/antlr4/       # SQL Grammar Definition (Sql.g4)
├── src/main/java/
│   └── com/byteforce/
│       ├── cli/           # Main entry point & JLine REPL logic
│       ├── core/          # Engine Core
│       │   ├── ByteForceDB.java      # Main Facade
│       │   ├── ExecutionEngine.java  # Logic for SELECT, INSERT, etc.
│       │   ├── StorageEngine.java    # Disk persistence
│       │   └── SQLParser.java        # ANTLR Visitor implementation
│       └── core/models/   # Data Structures
│           ├── Table.java            # Table schema & data
│           ├── Column.java           # Column metadata
│           └── DataType.java         # Supported types (INT, TEXT, etc.)
└── src/test/java/         # JUnit Tests
```