# Premium RDBMS Architecture Documentation

## Table of Contents
1. [System Architecture Overview](#system-architecture-overview)
2. [Core Components](#core-components)
3. [Design Patterns](#design-patterns)
4. [Technology Choices](#technology-choices)
5. [Key Algorithms](#key-algorithms)
6. [Implementation Phases](#implementation-phases)
7. [Testing Strategy](#testing-strategy)
8. [Advanced Features](#advanced-features)

---

## System Architecture Overview

### Layered Architecture Diagram

```
┌─────────────────────────────────────────┐
│        REPL / CLI Interface             │
│  - Interactive command processing       │
│  - Result formatting & display          │
├─────────────────────────────────────────┤
│         SQL Parser Layer                │
│  - Lexer (Tokenization)                 │
│  - Parser (AST Generation)              │
│  - Semantic Analyzer                    │
├─────────────────────────────────────────┤
│       Query Planner/Optimizer           │
│  - Logical Plan Generation              │
│  - Physical Plan Selection              │
│  - Cost-Based Optimization              │
├─────────────────────────────────────────┤
│       Execution Engine                  │
│  - Operator Pipeline                    │
│  - Scan, Join, Filter, Project          │
│  - Result Materialization               │
├─────────────────────────────────────────┤
│      Storage Engine Layer               │
│  - Page-based Storage                   │
│  - Buffer Pool Manager                  │
│  - Heap File Organization               │
├─────────────────────────────────────────┤
│    Index Manager (B+ Tree/Hash)         │
│  - Index Creation & Maintenance         │
│  - Fast Lookup Operations               │
│  - Range Query Support                  │
├─────────────────────────────────────────┤
│   Transaction Manager (ACID)            │
│  - Lock Manager                         │
│  - Write-Ahead Logging                  │
│  - Recovery System                      │
├─────────────────────────────────────────┤
│      Catalog/Schema Manager             │
│  - Metadata Storage                     │
│  - Statistics Collection                │
│  - Constraint Enforcement               │
└─────────────────────────────────────────┘
```

### Architecture Principles

- **Separation of Concerns**: Each layer has distinct responsibilities
- **Modularity**: Components can be developed and tested independently
- **Extensibility**: New operators, indexes, or storage formats can be added
- **Performance**: Multiple optimization points at different layers
- **Reliability**: ACID properties ensure data integrity

---

## Core Components

### 1. REPL / CLI Interface

**Purpose**: Provide interactive command-line interface for database operations

**Responsibilities**:
- Accept SQL commands from user input
- Parse and execute commands
- Display query results in formatted tables
- Handle multi-line SQL statements
- Provide command history and auto-completion
- Show execution statistics (time, rows affected)

**Key Features**:
```
.help              - Show available commands
.tables            - List all tables
.schema <table>    - Show table structure
.indexes <table>   - Show table indexes
.explain <query>   - Show query execution plan
.exit              - Exit REPL
```

**Implementation Details**:
- Use readline library for history and editing
- Implement command buffer for multi-line statements
- Format results using tabular display (padding, borders)
- Color-code output for better readability
- Handle Ctrl+C gracefully without exiting

---

### 2. SQL Parser Layer

**Purpose**: Transform SQL text into Abstract Syntax Tree (AST)

#### 2.1 Lexer/Tokenizer

**Responsibilities**:
- Break SQL string into meaningful tokens
- Identify keywords: SELECT, FROM, WHERE, JOIN, etc.
- Extract identifiers: table names, column names
- Parse literals: strings, numbers, booleans, NULL
- Recognize operators: =, <, >, <=, >=, !=, AND, OR, NOT
- Handle special characters and delimiters

**Token Types**:
```python
KEYWORD, IDENTIFIER, INTEGER, FLOAT, STRING, 
OPERATOR, COMMA, LPAREN, RPAREN, SEMICOLON, 
ASTERISK, DOT, EOF
```

**Example**:
```sql
SELECT name, age FROM users WHERE age > 25
```
Tokenizes to:
```
[KEYWORD:SELECT, IDENTIFIER:name, COMMA, IDENTIFIER:age,
 KEYWORD:FROM, IDENTIFIER:users, KEYWORD:WHERE,
 IDENTIFIER:age, OPERATOR:>, INTEGER:25]
```

#### 2.2 Parser

**Responsibilities**:
- Build Abstract Syntax Tree from tokens
- Enforce SQL grammar rules
- Handle operator precedence
- Support nested expressions
- Validate syntax correctness

**Grammar (Simplified BNF)**:
```
query          ::= select_stmt | insert_stmt | update_stmt | delete_stmt | create_stmt
select_stmt    ::= SELECT columns FROM table [WHERE condition] [JOIN ...]
insert_stmt    ::= INSERT INTO table [(columns)] VALUES (values)
update_stmt    ::= UPDATE table SET assignments WHERE condition
delete_stmt    ::= DELETE FROM table WHERE condition
create_stmt    ::= CREATE TABLE table (column_defs)
```

**AST Node Types**:
- SelectNode: columns, table, where_clause, joins
- InsertNode: table, columns, values
- UpdateNode: table, assignments, where_clause
- DeleteNode: table, where_clause
- CreateTableNode: table, column_definitions

**Implementation Approach**:
- Recursive Descent Parser (hand-written, simple)
- Parser Generator (ANTLR4, PLY for complex grammars)
- Operator Precedence Parsing for expressions

#### 2.3 Semantic Analyzer

**Responsibilities**:
- Validate table existence in catalog
- Verify column names are valid
- Check data type compatibility
- Resolve ambiguous column references
- Enforce constraint rules
- Type checking for operations

**Validation Checks**:
- Table exists in schema
- Columns exist in referenced tables
- No duplicate column names in result
- JOIN conditions use compatible types
- INSERT values match column count and types
- PRIMARY KEY and UNIQUE constraints respected

---

### 3. Query Planner/Optimizer

**Purpose**: Generate efficient execution plans for queries

#### 3.1 Logical Plan

**Responsibilities**:
- High-level representation of query operations
- Independent of physical implementation
- Apply logical transformations

**Logical Operators**:
- **Scan**: Read table rows
- **Filter**: Apply WHERE conditions
- **Project**: Select specific columns
- **Join**: Combine tables
- **Aggregate**: GROUP BY operations
- **Sort**: ORDER BY operations

**Example Logical Plan**:
```
SELECT u.name, o.total 
FROM users u 
JOIN orders o ON u.id = o.user_id 
WHERE u.age > 25

Logical Plan:
Project(u.name, o.total)
  └─ Join(u.id = o.user_id)
      ├─ Filter(age > 25)
      │   └─ Scan(users)
      └─ Scan(orders)
```

#### 3.2 Physical Plan

**Responsibilities**:
- Convert logical plan to executable operations
- Choose specific algorithms (hash join vs nested loop)
- Select indexes to use
- Determine join order
- Estimate costs

**Physical Operators**:
- **SeqScan**: Sequential table scan
- **IndexScan**: Index-based lookup
- **NestedLoopJoin**: O(n×m) join
- **HashJoin**: Hash-based join
- **SortMergeJoin**: Sort both sides, merge
- **Filter**: Predicate evaluation
- **Project**: Column projection

**Example Physical Plan**:
```
Project(u.name, o.total)
  └─ HashJoin(u.id = o.user_id)
      ├─ IndexScan(users, idx_age, age > 25)
      └─ SeqScan(orders)
```

#### 3.3 Query Optimization Techniques

**Rule-Based Optimization**:
- **Predicate Pushdown**: Move filters closer to data source
- **Projection Pushdown**: Eliminate unneeded columns early
- **Join Reordering**: Process smaller tables first
- **Constant Folding**: Evaluate constant expressions at compile time
- **Index Selection**: Use indexes for WHERE, JOIN conditions

**Cost-Based Optimization**:
- Estimate I/O cost: `pages_read × page_io_cost`
- Estimate CPU cost: `tuples_processed × cpu_cost`
- Calculate selectivity: `matching_rows / total_rows`
- Choose plan with minimum total cost

**Statistics Used**:
- Table cardinality (row count)
- Column cardinality (distinct values)
- Index depth and fanout
- Data distribution histograms

**Optimization Examples**:

```sql
-- Original: Scan all users, then filter
SELECT * FROM users WHERE id = 100

-- Optimized: Use index on id
IndexScan(users, pk_users, id = 100)
```

```sql
-- Original: Large join
FROM large_table JOIN small_table

-- Optimized: Switch join order
FROM small_table JOIN large_table
```

---

### 4. Execution Engine

**Purpose**: Execute physical query plans and return results

#### 4.1 Volcano/Iterator Model

**Concept**: Each operator implements iterator interface

**Interface**:
```python
class Operator:
    def open(self):
        """Initialize operator, allocate resources"""
        
    def next(self) -> Optional[Tuple]:
        """Return next tuple or None if exhausted"""
        
    def close(self):
        """Clean up resources"""
```

**Benefits**:
- Simple composability
- Pipeline execution (no materialization between operators)
- Memory efficient (streaming)
- Easy to add new operators

#### 4.2 Core Operators

**SeqScan (Sequential Scan)**:
```python
def next(self):
    if self.current_row < len(self.table.rows):
        row = self.table.rows[self.current_row]
        self.current_row += 1
        return row
    return None
```

**IndexScan**:
```python
def next(self):
    # Use index to get matching row IDs
    row_ids = self.index.lookup(self.search_key)
    for row_id in row_ids:
        return self.table.get_row(row_id)
    return None
```

**Filter**:
```python
def next(self):
    while True:
        row = self.child.next()
        if row is None:
            return None
        if self.predicate(row):
            return row
```

**Project**:
```python
def next(self):
    row = self.child.next()
    if row is None:
        return None
    return {col: row[col] for col in self.columns}
```

**NestedLoopJoin**:
```python
def next(self):
    while True:
        if self.right_row is None:
            self.left_row = self.left_child.next()
            if self.left_row is None:
                return None
            self.right_child.open()  # Restart right side
        
        self.right_row = self.right_child.next()
        if self.right_row is None:
            continue
            
        if self.join_condition(self.left_row, self.right_row):
            return {**self.left_row, **self.right_row}
```

**HashJoin**:
```python
def open(self):
    # Build phase: hash smaller table
    self.hash_table = defaultdict(list)
    while True:
        row = self.build_child.next()
        if row is None:
            break
        key = row[self.build_key]
        self.hash_table[key].append(row)
    
def next(self):
    # Probe phase
    while True:
        if not self.current_matches:
            probe_row = self.probe_child.next()
            if probe_row is None:
                return None
            key = probe_row[self.probe_key]
            self.current_matches = self.hash_table.get(key, [])
            self.current_probe = probe_row
        
        if self.current_matches:
            match = self.current_matches.pop()
            return {**self.current_probe, **match}
```

#### 4.3 Execution Flow

```
1. Parser generates AST
2. Planner creates physical plan (operator tree)
3. Execution engine calls root.open()
4. Loop: result = root.next()
5. Display each result tuple
6. When root.next() returns None, call root.close()
```

---

### 5. Storage Engine Layer

**Purpose**: Manage persistent data storage and memory buffers

#### 5.1 Page-Based Storage

**Concept**: Data organized into fixed-size pages (4KB, 8KB typical)

**Benefits**:
- Efficient I/O (read/write entire pages)
- Natural unit for buffer pool
- Aligns with OS page size
- Simplifies space management

**Page Structure**:
```
┌──────────────────────────────────────┐
│         Page Header                  │
│  - Page ID                           │
│  - Page Type (data/index)            │
│  - Free space pointer                │
│  - Slot count                        │
├──────────────────────────────────────┤
│         Slot Directory               │
│  - Offset to tuple 1                 │
│  - Offset to tuple 2                 │
│  - ...                               │
├──────────────────────────────────────┤
│         Free Space                   │
├──────────────────────────────────────┤
│         Tuple N                      │
│         Tuple N-1                    │
│         ...                          │
│         Tuple 1                      │
└──────────────────────────────────────┘
```

**Slotted Page Format**:
- Slots grow from top down
- Tuples grow from bottom up
- Allows variable-length records
- Efficient space utilization

#### 5.2 Buffer Pool Manager

**Purpose**: Cache frequently accessed pages in memory

**Responsibilities**:
- Load pages from disk to memory
- Evict pages when buffer is full
- Track dirty pages (modified)
- Write dirty pages back to disk

**Key Operations**:
```python
def fetch_page(page_id):
    if page_id in buffer_pool:
        update_lru(page_id)
        return buffer_pool[page_id]
    
    if buffer_pool.is_full():
        victim = lru_evict()
        if victim.is_dirty:
            write_to_disk(victim)
        remove_from_pool(victim)
    
    page = read_from_disk(page_id)
    buffer_pool[page_id] = page
    return page
```

**Replacement Policies**:
- **LRU (Least Recently Used)**: Evict oldest accessed page
- **Clock**: Circular list with reference bits
- **LRU-K**: Track K most recent accesses
- **2Q**: Separate queues for hot/cold pages

#### 5.3 Heap File Organization

**Concept**: Unordered collection of pages for table storage

**Structure**:
```
Table File
├── Page 0 (metadata)
├── Page 1 (data)
├── Page 2 (data)
├── ...
└── Page N (data)
```

**Operations**:
- **Insert**: Find page with free space, add tuple
- **Delete**: Mark tuple as deleted, reclaim space
- **Update**: Delete old, insert new (or in-place if fits)
- **Scan**: Read all pages sequentially

#### 5.4 Persistence

**Serialization Options**:

**Option 1: Pickle (Python)**
```python
with open('table.db', 'wb') as f:
    pickle.dump(table, f)
```
- Pros: Simple, handles Python objects
- Cons: Not portable, security issues

**Option 2: JSON**
```python
with open('table.json', 'w') as f:
    json.dump(table.to_dict(), f)
```
- Pros: Human-readable, portable
- Cons: Slower, larger file size

**Option 3: Binary Format**
```python
# Custom binary format
# [page_count][page_1][page_2]...[page_n]
```
- Pros: Fast, compact, professional
- Cons: More complex to implement

**Option 4: Memory-Mapped Files**
```python
import mmap
# Map file directly to memory
```
- Pros: OS-managed caching, very fast
- Cons: Platform-specific behavior

---

### 6. Index Manager

**Purpose**: Provide fast data access through indexes

#### 6.1 Index Types

**Hash Index**:
- **Structure**: Hash table mapping keys to row IDs
- **Lookup**: O(1) average case
- **Use Case**: Equality searches (WHERE id = 100)
- **Limitations**: No range queries, no sorting

**B+ Tree Index**:
- **Structure**: Balanced tree with data in leaves
- **Lookup**: O(log n)
- **Use Case**: Range queries, sorting, prefix matching
- **Benefits**: Keeps data sorted, supports scans

#### 6.2 B+ Tree Implementation

**Structure**:
```
               [50|100]           Internal Node
              /    |    \
    [10|20|30] [60|75] [110|150]  Leaf Nodes (with data pointers)
```

**Properties**:
- All data in leaf nodes
- Internal nodes for routing only
- Leaf nodes linked for range scans
- Balanced (all leaves at same depth)
- Order M: max M children per internal node

**Node Structure**:
```python
class BTreeNode:
    keys: List[Any]           # Search keys
    children: List[Node]      # Child pointers (internal)
    values: List[RowID]       # Data pointers (leaf)
    is_leaf: bool
    next_leaf: Node          # For range scans
```

**Operations**:

**Insert**:
1. Find correct leaf node
2. Insert key-value pair
3. If node overflows (> M keys):
   - Split node in half
   - Promote middle key to parent
   - Recursively split parent if needed

**Search**:
1. Start at root
2. Binary search for key in node
3. Follow appropriate child pointer
4. Repeat until leaf
5. Return associated value

**Range Scan**:
1. Search for start key
2. Traverse leaf nodes using next_leaf pointers
3. Collect all keys until end key

**Delete**:
1. Find leaf containing key
2. Remove key-value pair
3. If underflow (< M/2 keys):
   - Borrow from sibling, or
   - Merge with sibling
   - Update parent recursively

#### 6.3 Index Maintenance

**During INSERT**:
```python
def insert_row(table, row):
    row_id = table.add_row(row)
    for index in table.indexes:
        key = extract_key(row, index.columns)
        index.insert(key, row_id)
```

**During UPDATE**:
```python
def update_row(table, row_id, new_values):
    old_row = table.get_row(row_id)
    table.update_row(row_id, new_values)
    
    for index in table.indexes:
        old_key = extract_key(old_row, index.columns)
        new_key = extract_key(new_values, index.columns)
        
        if old_key != new_key:
            index.delete(old_key, row_id)
            index.insert(new_key, row_id)
```

**During DELETE**:
```python
def delete_row(table, row_id):
    row = table.get_row(row_id)
    table.remove_row(row_id)
    
    for index in table.indexes:
        key = extract_key(row, index.columns)
        index.delete(key, row_id)
```

---

### 7. Transaction Manager

**Purpose**: Ensure ACID properties (Atomicity, Consistency, Isolation, Durability)

#### 7.1 ACID Properties

**Atomicity**: All-or-nothing execution
- Transaction commits entirely or rolls back entirely
- Use Write-Ahead Logging (WAL)

**Consistency**: Database moves from valid state to valid state
- Enforce constraints (PRIMARY KEY, UNIQUE, FOREIGN KEY)
- Check constraint violations before commit

**Isolation**: Concurrent transactions don't interfere
- Use locking or MVCC (Multi-Version Concurrency Control)
- Isolation levels: Read Uncommitted, Read Committed, Repeatable Read, Serializable

**Durability**: Committed data survives crashes
- Write-Ahead Logging (WAL)
- Force log to disk before commit

#### 7.2 Lock Manager

**Purpose**: Prevent concurrent access conflicts

**Lock Types**:
- **Shared Lock (S)**: Allow concurrent reads
- **Exclusive Lock (X)**: Block all other access

**Lock Compatibility Matrix**:
```
       S    X
S    ✓    ✗
X    ✗    ✗
```

**Granularity**:
- **Row-level**: Lock individual rows (fine-grained, more concurrency)
- **Table-level**: Lock entire table (coarse-grained, simpler)
- **Page-level**: Lock pages (middle ground)

**Two-Phase Locking (2PL) Protocol**:
1. **Growing Phase**: Acquire locks, no releases
2. **Shrinking Phase**: Release locks, no acquisitions
3. Guarantees serializability

**Deadlock Handling**:
- **Detection**: Wait-for graph, detect cycles
- **Prevention**: Lock ordering, timeouts
- **Resolution**: Abort one transaction (victim selection)

#### 7.3 Write-Ahead Logging (WAL)

**Concept**: Log all changes before applying them

**Log Record Types**:
```
BEGIN <txn_id>
UPDATE <txn_id> <table> <row_id> <old_value> <new_value>
INSERT <txn_id> <table> <row_id> <new_value>
DELETE <txn_id> <table> <row_id> <old_value>
COMMIT <txn_id>
ABORT <txn_id>
```

**Protocol**:
1. Write log record to buffer
2. Force log to disk
3. Apply change to database
4. Transaction can commit only after log is on disk

**Recovery Process** (after crash):
1. **Redo Phase**: Replay committed transactions from log
2. **Undo Phase**: Rollback uncommitted transactions

**Example**:
```
Log:
BEGIN T1
UPDATE T1 users 5 {age:30} {age:31}
COMMIT T1
BEGIN T2
INSERT T2 users 6 {name:"Alice"}
<crash>

Recovery:
- T1 committed → REDO: Apply UPDATE
- T2 not committed → UNDO: Remove INSERT
```

#### 7.4 Transaction API

```python
def begin_transaction():
    txn_id = generate_txn_id()
    txn_table[txn_id] = Transaction(status='active')
    log.write(f"BEGIN {txn_id}")
    return txn_id

def commit_transaction(txn_id):
    # Write commit record to log
    log.write(f"COMMIT {txn_id}")
    log.flush()  # Force to disk
    
    # Release all locks
    lock_manager.release_all(txn_id)
    txn_table[txn_id].status = 'committed'

def rollback_transaction(txn_id):
    # Undo all changes
    for log_record in reversed(txn_table[txn_id].log_records):
        undo_operation(log_record)
    
    log.write(f"ABORT {txn_id}")
    lock_manager.release_all(txn_id)
    txn_table[txn_id].status = 'aborted'
```

---

### 8. Catalog/Schema Manager

**Purpose**: Store and manage database metadata

#### 8.1 System Catalog

**Stored Information**:
- Table definitions (name, columns, constraints)
- Column definitions (name, type, nullable, default)
- Index definitions (name, type, columns, unique)
- Statistics (row counts, cardinalities)
- User permissions (for access control)

**Implementation Approach**:

**Option 1: System Tables**
```sql
-- Store metadata in special tables
CREATE TABLE _tables (
    table_id INTEGER PRIMARY KEY,
    table_name VARCHAR(255),
    row_count INTEGER
)

CREATE TABLE _columns (
    column_id INTEGER PRIMARY KEY,
    table_id INTEGER,
    column_name VARCHAR(255),
    data_type VARCHAR(50),
    is_nullable BOOLEAN,
    is_primary_key BOOLEAN
)

CREATE TABLE _indexes (
    index_id INTEGER PRIMARY KEY,
    table_id INTEGER,
    index_name VARCHAR(255),
    index_type VARCHAR(50),
    is_unique BOOLEAN
)
```

**Option 2: In-Memory Dictionary**
```python
catalog = {
    'tables': {
        'users': {
            'columns': [...],
            'indexes': [...],
            'row_count': 1000
        }
    }
}
```

#### 8.2 Schema Validation

**CREATE TABLE Checks**:
- Table name doesn't already exist
- Column names are unique within table
- Data types are valid
- At most one PRIMARY KEY
- UNIQUE columns are indexed

**INSERT Checks**:
- Table exists
- Column count matches VALUES count
- Data types compatible
- PRIMARY KEY unique
- UNIQUE constraints satisfied
- NOT NULL constraints satisfied

**Query Checks**:
- Referenced tables exist
- Referenced columns exist
- Ambiguous column names resolved
- Type compatibility in joins/filters

#### 8.3 Statistics Collection

**Purpose**: Help query optimizer make decisions

**Statistics Tracked**:
- **Table Cardinality**: Number of rows
- **Column Cardinality**: Number of distinct values
- **Null Count**: Number of NULL values
- **Min/Max Values**: For range estimation
- **Histogram**: Distribution of values

**Example**:
```python
table_stats = {
    'users': {
        'row_count': 10000,
        'columns': {
            'age': {
                'distinct_values': 80,
                'null_count': 15,
                'min': 18,
                'max': 95,
                'histogram': [...]
            }
        }
    }
}
```

**Usage in Optimization**:
```python
# Estimate selectivity of: age > 50
distinct_ages = 80
qualifying_ages = 95 - 50  # max - threshold
selectivity = qualifying_ages / distinct_ages
estimated_rows = table_row_count * selectivity
```

---

## Design Patterns

### 1. Strategy Pattern (Execution Operators)

**Purpose**: Interchangeable algorithms for same task

```python
class JoinStrategy(ABC):
    @abstractmethod
    def execute(self, left, right, condition):
        pass

class NestedLoopJoin(JoinStrategy):
    def execute(self, left, right, condition):
        # O(n*m) implementation
        pass

class HashJoin(JoinStrategy):
    def execute(self, left, right, condition):
        # Hash-based implementation
        pass

# Usage
join_strategy = HashJoin() if use_hash else NestedLoopJoin()
result = join_strategy.execute(left_table, right_table, condition)
```

### 2. Iterator Pattern (Volcano Model)

**Purpose**: Uniform interface for accessing sequences

```python
class Operator(ABC):
    @abstractmethod
    def open(self): pass
    
    @abstractmethod
    def next(self) -> Optional[Tuple]: pass
    
    @abstractmethod
    def close(self): pass

# All operators implement same interface
scan = SeqScan(table)
filter_op = Filter(scan, predicate)
project = Project(filter_op, columns)

project.open()
while row := project.next():
    print(row)
project.close()
```

### 3. Builder Pattern (Query Plans)

**Purpose**: Construct complex objects step-by-step

```python
class QueryPlanBuilder:
    def __init__(self):
        self.plan = None
    
    def scan(self, table):
        self.plan = SeqScan(table)
        return self
    
    def filter(self, predicate):
        self.plan = Filter(self.plan, predicate)
        return self
    
    def join(self, other_table, condition):
        right = SeqScan(other_table)
        self.plan = HashJoin(self.plan, right, condition)
        return self
    
    def build(self):
        return self.plan

# Usage
plan = (QueryPlanBuilder()
    .scan('users')
    .filter(lambda r: r['age'] > 25)
    .join('orders', lambda l, r: l['id'] == r['user_id'])
    .build())
```

### 4. Visitor Pattern (AST Traversal)

**Purpose**: Separate algorithms from object structure

```python
class ASTVisitor(ABC):
    @abstractmethod
    def visit_select(self, node): pass
    
    @abstractmethod
    def visit_insert(self, node): pass
    
    @abstractmethod
    def visit_join(self, node): pass

class PlanGenerator(ASTVisitor):
    def visit_select(self, node):
        plan = self.visit(node.from_clause)
        if node.where_clause:
            plan = Filter(plan, node.where_clause)
        plan = Project(plan, node.select_list)
        return plan
    
    def visit_join(self, node):
        left = self.visit(node.left)
        right = self.visit(node.right)
        return HashJoin(left, right, node.condition)
```

### 5. Command Pattern (Transactions)

**Purpose**: Encapsulate operations for undo/redo

```python
class Command(ABC):
    @abstractmethod
    def execute(self): pass
    
    @abstractmethod
    def undo(self): pass

class InsertCommand(Command):
    def __init__(self, table, row):
        self.table = table
        self.row = row
        self.row_id = None
    
    def execute(self):
        self.row_id = self.table.insert(self.row)
    
    def undo(self):
        self.table.delete(self.row_id)

# Transaction as list of commands
transaction = [
    InsertCommand(users, {'name': 'Alice'}),
    UpdateCommand(users, 5, {'age': 30})
]

# Rollback
for cmd in reversed(transaction):
    cmd.undo()
```

### 6. Factory Pattern (Index Creation)

**Purpose**: Create objects without specifying exact class

```python
class IndexFactory:
    @staticmethod
    def create_index(index_type, columns):
        if index_type == 'HASH':
            return HashIndex(columns)
        elif index_type == 'BTREE':
            return BTreeIndex(columns)
        else:
            raise ValueError(f"Unknown index type: {index_type}")

# Usage
index = IndexFactory.create_index('BTREE', ['user_id'])
```

---

## Technology Choices

### For Production-Grade Implementation

**Parser Generation**:
- **ANTLR4**: Industry-standard, generates parsers from grammar files
- **PLY**: Python Lex-Yacc, easier learning curve
- **Lark**: Modern, fast, easy-to-use parsing library

**Storage**:
- **Memory-Mapped Files (mmap)**: OS-managed caching, very fast
- **Apache Arrow**: Columnar format, inter-process sharing
- **Apache Parquet**: Compressed columnar storage
- **RocksDB**: LSM-tree storage engine (write-optimized)

**Indexing**:
- **B+ Tree Libraries**: blist, BTrees (persistent)
- **Skip Lists**: Probabilistic balanced structure, simpler than B+ trees
- **LSM Trees**: LevelDB, RocksDB (write-heavy workloads)
- **Roaring Bitmaps**: Compressed bitmap indexes

**Serialization**:
- **Protocol Buffers**: Fast, compact, schema evolution
- **MessagePack**: Binary JSON, compact and fast
- **FlatBuffers**: Zero-copy deserialization
- **Cap'n Proto**: Similar to Protocol Buffers, zero-copy

**Concurrency**:
- **Threading**: Python threading (limited by GIL)
- **Multiprocessing**: True parallelism for query execution
- **asyncio**: Async I/O for network operations
- **uvloop**: Faster asyncio event loop

**Testing**:
- **pytest**: Modern Python testing framework
- **Hypothesis**: Property-based testing
- **SQLsmith**: Random SQL query generator for fuzzing
- **Locust**: Load testing tool

### For Learning/Simplicity

**Parser**:
- **pyparsing**: Pure Python, no grammar file needed
- **Hand-rolled Recursive Descent**: Full control, educational
- **Regular Expressions**: For very simple SQL subset

**Storage**:
- **Python dict + pickle**: Simplest persistence
- **SQLite**: Study existing implementation
- **JSON files**: Human-readable, easy debugging

**Indexing**:
- **Python dict**: Hash index (immediate)
- **Sorted list + bisect**: Simple B+ tree alternative
- **OrderedDict**: Maintains insertion order

**Data Structures**:
- **dataclasses**: Clean data modeling
- **namedtuple**: Lightweight, immutable records
- **TypedDict**: Type hints for dictionaries

---

## Key Algorithms

### 1. B+ Tree Operations

#### Insert Algorithm

```
INSERT(tree, key, value):
    leaf = find_leaf(tree.root, key)
    
    if leaf has space:
        insert (key, value) into leaf
        return
    
    # Leaf overflow - split
    new_leaf = create_leaf()
    middle_index = M / 2
    
    move keys[middle_index..end] to new_leaf
    new_leaf.next = leaf.next
    leaf.next = new_leaf
    
    promote_key = new_leaf.keys[0]
    insert_into_parent(leaf.parent, promote_key, new_leaf)

INSERT_INTO_PARENT(node, key, new_child):
    if node is None:
        # Create new root
        new_root = create_internal_node()
        new_root.keys = [key]
        new_root.children = [old_root, new_child]
        tree.root = new_root
        return
    
    if node has space:
        insert key and new_child into node
        return
    
    # Internal node overflow - split
    new_node = create_internal_node()
    middle_index = M / 2
    
    move keys[middle_index+1..end] to new_node
    move children[middle_index+1..end] to new_node
    
    promote_key = node.keys[middle_index]
    insert_into_parent(node.parent, promote_key, new_node)
```

#### Search Algorithm

```
SEARCH(node, key):
    if node is leaf:
        for i in range(len(node.keys)):
            if node.keys[i] == key:
                return node.values[i]
        return None
    
    # Internal node - find child to descend
    for i in range(len(node.keys)):
        if key < node.keys[i]:
            return SEARCH(node.children[i], key)
    
    # Key is >= all keys in node
    return SEARCH(node.children[len(node.keys)], key)
```

#### Range Scan Algorithm

```
RANGE_SCAN(tree, start_key, end_key):
    results = []
    
    # Find starting leaf
    leaf = find_leaf(tree.root, start_key)
    
    # Traverse leaf nodes using next pointers
    while leaf is not None:
        for i in range(len(leaf.keys)):
            if start_key <= leaf.keys[i] <= end_key:
                results.append((leaf.keys[i], leaf.values[i]))
            elif leaf.keys[i] > end_key:
                return results
        
        leaf = leaf.next
    
    return results
```

### 2. Join Algorithms

#### Nested Loop Join

```
NESTED_LOOP_JOIN(left_table, right_table, condition):
    results = []
    
    for left_row in left_table:
        for right_row in right_table:
            if condition(left_row, right_row):
                results.append(merge(left_row, right_row))
    
    return results

Time Complexity: O(n × m)
Space Complexity: O(1)
Best for: Small tables, no indexes
```

#### Hash Join

```
HASH_JOIN(left_table, right_table, left_key, right_key):
    # Build phase
    hash_table = {}
    for row in left_table:
        key = row[left_key]
        if key not in hash_table:
            hash_table[key] = []
        hash_table[key].append(row)
    
    # Probe phase
    results = []
    for row in right_table:
        key = row[right_key]
        if key in hash_table:
            for matching_row in hash_table[key]:
                results.append(merge(matching_row, row))
    
    return results

Time Complexity: O(n + m) average case
Space Complexity: O(min(n, m))
Best for: Equality joins, sufficient memory
```

#### Sort-Merge Join

```
SORT_MERGE_JOIN(left_table, right_table, left_key, right_key):
    # Sort both tables
    left_sorted = sort(left_table, key=left_key)
    right_sorted = sort(right_table, key=right_key)
    
    results = []
    i, j = 0, 0
    
    while i < len(left_sorted) and j < len(right_sorted):
        left_val = left_sorted[i][left_key]
        right_val = right_sorted[j][right_key]
        
        if left_val < right_val:
            i += 1
        elif left_val > right_val:
            j += 1
        else:
            # Found match - collect all matching pairs
            j_start = j
            while j < len(right_sorted) and right_sorted[j][right_key] == left_val:
                results.append(merge(left_sorted[i], right_sorted[j]))
                j += 1
            j = j_start
            i += 1
    
    return results

Time Complexity: O(n log n + m log m)
Space Complexity: O(1) if tables already sorted
Best for: Both tables already sorted, or merge join preferred
```

### 3. Query Optimization Algorithms

#### Cost Estimation

```
ESTIMATE_COST(operator):
    if operator is SeqScan:
        io_cost = table.page_count × PAGE_IO_COST
        cpu_cost = table.row_count × CPU_TUPLE_COST
        return io_cost + cpu_cost
    
    elif operator is IndexScan:
        # B+ tree depth + leaf page reads
        io_cost = (index.depth + selectivity × index.leaf_pages) × PAGE_IO_COST
        cpu_cost = estimated_rows × CPU_TUPLE_COST
        return io_cost + cpu_cost
    
    elif operator is Filter:
        child_cost = ESTIMATE_COST(operator.child)
        filter_cost = child_rows × EVAL_PREDICATE_COST
        return child_cost + filter_cost
    
    elif operator is HashJoin:
        left_cost = ESTIMATE_COST(operator.left)
        right_cost = ESTIMATE_COST(operator.right)
        
        # Build hash table on smaller side
        build_cost = min(left_rows, right_rows) × HASH_BUILD_COST
        probe_cost = max(left_rows, right_rows) × HASH_PROBE_COST
        
        return left_cost + right_cost + build_cost + probe_cost
```

#### Selectivity Estimation

```
ESTIMATE_SELECTIVITY(predicate, column_stats):
    if predicate is "column = value":
        # Assume uniform distribution
        return 1.0 / column_stats.distinct_values
    
    elif predicate is "column > value":
        if column_stats has histogram:
            return histogram_estimate(value)
        else:
            # Fallback: linear interpolation
            range = column_stats.max - column_stats.min
            return (column_stats.max - value) / range
    
    elif predicate is "column BETWEEN low AND high":
        range = column_stats.max - column_stats.min
        return (high - low) / range
    
    elif predicate is "column IN (values)":
        return len(values) / column_stats.distinct_values
    
    elif predicate is "column IS NULL":
        return column_stats.null_count / column_stats.total_rows
```

#### Join Order Optimization (Dynamic Programming)

```
FIND_BEST_JOIN_ORDER(tables):
    # dp[subset] = (cost, plan) for joining tables in subset
    dp = {}
    
    # Base case: single tables
    for table in tables:
        dp[{table}] = (cost_of_scan(table), SeqScan(table))
    
    # Build up subsets of increasing size
    for size in range(2, len(tables) + 1):
        for subset in combinations(tables, size):
            best_cost = infinity
            best_plan = None
            
            # Try all ways to split subset into two parts
            for left_subset in powerset(subset):
                if len(left_subset) == 0 or len(left_subset) == size:
                    continue
                
                right_subset = subset - left_subset
                
                left_cost, left_plan = dp[left_subset]
                right_cost, right_plan = dp[right_subset]
                
                # Try different join algorithms
                for join_type in [NestedLoopJoin, HashJoin]:
                    join_plan = join_type(left_plan, right_plan)
                    total_cost = left_cost + right_cost + cost_of_join(join_plan)
                    
                    if total_cost < best_cost:
                        best_cost = total_cost
                        best_plan = join_plan
            
            dp[subset] = (best_cost, best_plan)
    
    return dp[set(tables)][1]

Time Complexity: O(3^n) where n is number of tables
Note: Use heuristics (greedy) for n > 10 tables
```

### 4. LRU Cache (Buffer Pool)

```
class LRUCache:
    def __init__(self, capacity):
        self.capacity = capacity
        self.cache = {}  # key -> (value, timestamp)
        self.access_order = []  # Most recent at end
    
    def get(self, key):
        if key not in self.cache:
            return None
        
        # Update access order
        self.access_order.remove(key)
        self.access_order.append(key)
        
        return self.cache[key][0]
    
    def put(self, key, value):
        if key in self.cache:
            self.access_order.remove(key)
        elif len(self.cache) >= self.capacity:
            # Evict least recently used
            victim = self.access_order.pop(0)
            del self.cache[victim]
        
        self.cache[key] = (value, time.time())
        self.access_order.append(key)
    
    def evict(self):
        if not self.access_order:
            return None
        victim = self.access_order.pop(0)
        value = self.cache[victim][0]
        del self.cache[victim]
        return value
```

### 5. Deadlock Detection (Wait-For Graph)

```
class DeadlockDetector:
    def __init__(self):
        self.wait_for_graph = defaultdict(set)  # txn -> set of txns it waits for
    
    def add_wait(self, waiting_txn, holding_txn):
        self.wait_for_graph[waiting_txn].add(holding_txn)
        
        if self.has_cycle():
            return self.select_victim()
        return None
    
    def has_cycle(self):
        visited = set()
        rec_stack = set()
        
        def dfs(node):
            visited.add(node)
            rec_stack.add(node)
            
            for neighbor in self.wait_for_graph[node]:
                if neighbor not in visited:
                    if dfs(neighbor):
                        return True
                elif neighbor in rec_stack:
                    return True
            
            rec_stack.remove(node)
            return False
        
        for node in self.wait_for_graph:
            if node not in visited:
                if dfs(node):
                    return True
        
        return False
    
    def select_victim(self):
        # Simple heuristic: youngest transaction
        return max(self.wait_for_graph.keys(), key=lambda txn: txn.start_time)
```

---

## Implementation Phases

### Phase 1: Foundation (MVP)

**Goal**: Basic working database with core functionality

**Components**:
- In-memory table storage (list of dicts)
- Simple SQL parser for CREATE, INSERT, SELECT (no WHERE)
- REPL interface
- Primary key enforcement
- Sequential scan execution

**Deliverables**:
```sql
CREATE TABLE users (id INTEGER PRIMARY KEY, name VARCHAR(100))
INSERT INTO users VALUES (1, 'Alice')
SELECT * FROM users
```

**Success Criteria**:
- Can create tables
- Can insert and query data
- Data persists in memory during session
- Primary key violations caught

### Phase 2: Queries & Constraints

**Goal**: Add filtering and more constraints

**Components**:
- WHERE clause parser and executor
- Filter operator in execution engine
- UNIQUE constraints
- NOT NULL constraints
- Basic type checking

**Deliverables**:
```sql
CREATE TABLE users (
    id INTEGER PRIMARY KEY,
    email VARCHAR(255) UNIQUE,
    name VARCHAR(100) NOT NULL
)
SELECT * FROM users WHERE id > 5
UPDATE users SET name = 'Bob' WHERE id = 1
DELETE FROM users WHERE id = 2
```

**Success Criteria**:
- Can filter results
- Constraint violations prevented
- UPDATE and DELETE work correctly

### Phase 3: Indexing

**Goal**: Fast lookups via indexes

**Components**:
- Hash index implementation
- Index on PRIMARY KEY and UNIQUE columns
- Index maintenance during INSERT/UPDATE/DELETE
- IndexScan operator
- Query planner chooses IndexScan vs SeqScan

**Deliverables**:
```sql
CREATE INDEX idx_email ON users(email)
-- Automatically uses index for:
SELECT * FROM users WHERE email = 'alice@example.com'
```

**Success Criteria**:
- Indexes speed up equality lookups
- Indexes maintained correctly
- Query planner selects appropriate access method

### Phase 4: Joins

**Goal**: Combine data from multiple tables

**Components**:
- JOIN clause parser
- NestedLoopJoin operator
- HashJoin operator
- Multi-table query planner

**Deliverables**:
```sql
SELECT u.name, o.total
FROM users u
JOIN orders o ON u.id = o.user_id
WHERE u.age > 25
```

**Success Criteria**:
- Inner joins work correctly
- Multiple join algorithms available
- Planner chooses efficient join method

### Phase 5: Persistence

**Goal**: Data survives program restarts

**Components**:
- Serialize tables to disk (pickle or JSON)
- Load tables on startup
- COMMIT/ROLLBACK commands
- Write-ahead logging (basic)

**Deliverables**:
```sql
-- Data persists across sessions
.save database.db
.load database.db
```

**Success Criteria**:
- Tables persist to disk
- Data recovers after restart
- No data loss on normal shutdown

### Phase 6: Transactions

**Goal**: ACID compliance

**Components**:
- Transaction manager
- Lock manager (table-level locks)
- Two-phase locking protocol
- Rollback capability
- Write-ahead logging (full)

**Deliverables**:
```sql
BEGIN TRANSACTION
INSERT INTO users VALUES (10, 'Charlie')
UPDATE accounts SET balance = balance - 100 WHERE id = 1
COMMIT

BEGIN TRANSACTION
DELETE FROM users WHERE id = 5
ROLLBACK  -- Undo the delete
```

**Success Criteria**:
- Transactions are atomic
- Concurrent transactions don't conflict
- Rollback works correctly
- Data recovers after crash

### Phase 7: Optimization

**Goal**: Improve query performance

**Components**:
- B+ Tree index (replace hash for range queries)
- Cost-based query optimizer
- Statistics collection
- Query plan EXPLAIN
- Predicate pushdown
- Join reordering

**Deliverables**:
```sql
EXPLAIN SELECT * FROM users WHERE age BETWEEN 25 AND 35
-- Shows: IndexScan on idx_age (cost=10.5)

ANALYZE users  -- Collect statistics
```

**Success Criteria**:
- Range queries use B+ tree
- Optimizer chooses efficient plans
- EXPLAIN shows query execution plan
- Statistics improve optimizer decisions

### Phase 8: Advanced Features

**Goal**: Production-ready capabilities

**Components**:
- Aggregations (COUNT, SUM, AVG, GROUP BY)
- Sorting (ORDER BY)
- LIMIT/OFFSET
- Subqueries
- LEFT/RIGHT/OUTER joins
- Views
- Better error messages

**Deliverables**:
```sql
SELECT department, COUNT(*), AVG(salary)
FROM employees
GROUP BY department
HAVING AVG(salary) > 50000
ORDER BY COUNT(*) DESC
LIMIT 10

CREATE VIEW high_earners AS
SELECT * FROM employees WHERE salary > 100000
```

**Success Criteria**:
- Aggregations work correctly
- Sorting efficient (external sort if needed)
- Subqueries supported
- Views behave like tables

---

## Testing Strategy

### Unit Testing

**Component-Level Tests**:

```python
# Test Lexer
def test_lexer_tokenizes_select():
    lexer = Lexer("SELECT * FROM users")
    tokens = lexer.tokenize()
    assert tokens[0].type == TokenType.KEYWORD
    assert tokens[0].value == "SELECT"

# Test Parser
def test_parser_builds_ast():
    parser = Parser()
    ast = parser.parse("SELECT id FROM users WHERE age > 25")
    assert isinstance(ast, SelectNode)
    assert ast.table == "users"

# Test B+ Tree
def test_btree_insert_and_search():
    tree = BPlusTree(order=3)
    tree.insert(10, "value_10")
    tree.insert(20, "value_20")
    assert tree.search(10) == "value_10"
    assert tree.search(99) is None

# Test Hash Join
def test_hash_join():
    left = [{'id': 1, 'name': 'Alice'}, {'id': 2, 'name': 'Bob'}]
    right = [{'user_id': 1, 'order': 'A'}, {'user_id': 2, 'order': 'B'}]
    result = hash_join(left, right, 'id', 'user_id')
    assert len(result) == 2
```

### Integration Testing

**End-to-End Query Tests**:

```python
def test_query_with_filter():
    db = Database()
    db.execute("CREATE TABLE users (id INTEGER PRIMARY KEY, age INTEGER)")
    db.execute("INSERT INTO users VALUES (1, 25)")
    db.execute("INSERT INTO users VALUES (2, 30)")
    
    result = db.execute("SELECT * FROM users WHERE age > 26")
    assert len(result) == 1
    assert result[0]['id'] == 2

def test_join_query():
    db = Database()
    db.execute("CREATE TABLE users (id INTEGER PRIMARY KEY, name VARCHAR(50))")
    db.execute("CREATE TABLE orders (id INTEGER PRIMARY KEY, user_id INTEGER)")
    db.execute("INSERT INTO users VALUES (1, 'Alice')")
    db.execute("INSERT INTO orders VALUES (100, 1)")
    
    result = db.execute("""
        SELECT u.name, o.id 
        FROM users u 
        JOIN orders o ON u.id = o.user_id
    """)
    assert result[0]['name'] == 'Alice'
    assert result[0]['id'] == 100
```

### Performance Testing

**Benchmark Queries**:

```python
import time

def benchmark_sequential_scan():
    db = setup_large_database(rows=100000)
    
    start = time.time()
    result = db.execute("SELECT * FROM users WHERE id = 50000")
    end = time.time()
    
    print(f"SeqScan: {end - start:.4f}s")

def benchmark_index_scan():
    db = setup_large_database(rows=100000)
    db.execute("CREATE INDEX idx_id ON users(id)")
    
    start = time.time()
    result = db.execute("SELECT * FROM users WHERE id = 50000")
    end = time.time()
    
    print(f"IndexScan: {end - start:.4f}s")

def benchmark_join():
    db = setup_join_benchmark(left_rows=1000, right_rows=10000)
    
    # Nested Loop Join
    start = time.time()
    result = db.execute("SELECT /*+ NESTED_LOOP */ * FROM a JOIN b ON a.id = b.a_id")
    nl_time = time.time() - start
    
    # Hash Join
    start = time.time()
    result = db.execute("SELECT /*+ HASH_JOIN */ * FROM a JOIN b ON a.id = b.a_id")
    hash_time = time.time() - start
    
    print(f"NestedLoop: {nl_time:.4f}s, HashJoin: {hash_time:.4f}s")
```

### TPC-H Inspired Queries

**Standard Benchmark Queries**:

```sql
-- Q1: Pricing summary report
SELECT
    l_returnflag,
    l_linestatus,
    SUM(l_quantity) as sum_qty,
    SUM(l_extendedprice) as sum_base_price,
    AVG(l_quantity) as avg_qty
FROM lineitem
WHERE l_shipdate <= '1998-12-01'
GROUP BY l_returnflag, l_linestatus
ORDER BY l_returnflag, l_linestatus

-- Q3: Shipping priority query
SELECT
    l_orderkey,
    SUM(l_extendedprice * (1 - l_discount)) as revenue,
    o_orderdate,
    o_shippriority
FROM customer, orders, lineitem
WHERE c_mktsegment = 'BUILDING'
    AND c_custkey = o_custkey
    AND l_orderkey = o_orderkey
    AND o_orderdate < '1995-03-15'
    AND l_shipdate > '1995-03-15'
GROUP BY l_orderkey, o_orderdate, o_shippriority
ORDER BY revenue DESC, o_orderdate
LIMIT 10
```

### Edge Cases & Error Handling

```python
def test_empty_table():
    db = Database()
    db.execute("CREATE TABLE empty_table (id INTEGER)")
    result = db.execute("SELECT * FROM empty_table")
    assert result == []

def test_null_handling():
    db = Database()
    db.execute("CREATE TABLE test (id INTEGER, value INTEGER)")
    db.execute("INSERT INTO test VALUES (1, NULL)")
    result = db.execute("SELECT * FROM test WHERE value IS NULL")
    assert len(result) == 1

def test_constraint_violation():
    db = Database()
    db.execute("CREATE TABLE test (id INTEGER PRIMARY KEY)")
    db.execute("INSERT INTO test VALUES (1)")
    
    with pytest.raises(ConstraintViolation):
        db.execute("INSERT INTO test VALUES (1)")  # Duplicate PK

def test_syntax_error():
    db = Database()
    with pytest.raises(SyntaxError):
        db.execute("SELCT * FROM users")  # Typo

def test_table_not_found():
    db = Database()
    with pytest.raises(TableNotFound):
        db.execute("SELECT * FROM nonexistent")
```

### ACID Compliance Tests

```python
def test_atomicity():
    db = Database()
    db.execute("CREATE TABLE accounts (id INTEGER PRIMARY KEY, balance INTEGER)")
    db.execute("INSERT INTO accounts VALUES (1, 100)")
    
    db.execute("BEGIN TRANSACTION")
    db.execute("UPDATE accounts SET balance = balance - 50 WHERE id = 1")
    db.execute("INSERT INTO accounts VALUES (2, 50)")
    
    # Simulate crash before commit
    db.crash()
    
    # After recovery
    db.recover()
    result = db.execute("SELECT * FROM accounts WHERE id = 1")
    assert result[0]['balance'] == 100  # Transaction rolled back

def test_isolation():
    db = Database()
    db.execute("CREATE TABLE counter (value INTEGER)")
    db.execute("INSERT INTO counter VALUES (0)")
    
    # Two concurrent transactions
    txn1 = db.begin_transaction()
    txn2 = db.begin_transaction()
    
    # Both read initial value
    val1 = db.execute("SELECT value FROM counter", txn1)[0]['value']
    val2 = db.execute("SELECT value FROM counter", txn2)[0]['value']
    
    # Both increment
    db.execute("UPDATE counter SET value = value + 1", txn1)
    db.execute("UPDATE counter SET value = value + 1", txn2)
    
    db.commit(txn1)
    db.commit(txn2)
    
    # Final value should be 2 (both increments applied)
    result = db.execute("SELECT value FROM counter")
    assert result[0]['value'] == 2
```

---

## Advanced Features

### Aggregations

**GROUP BY Implementation**:

```python
class GroupByOperator(Operator):
    def __init__(self, child, group_columns, agg_functions):
        self.child = child
        self.group_columns = group_columns
        self.agg_functions = agg_functions  # {'sum_salary': ('SUM', 'salary')}
        self.groups = {}
        self.result_iter = None
    
    def open(self):
        self.child.open()
        
        # Build groups
        while True:
            row = self.child.next()
            if row is None:
                break
            
            # Extract group key
            key = tuple(row[col] for col in self.group_columns)
            
            if key not in self.groups:
                self.groups[key] = {
                    'count': 0,
                    'sums': {},
                    'rows': []
                }
            
            self.groups[key]['count'] += 1
            self.groups[key]['rows'].append(row)
            
            for agg_name, (func, col) in self.agg_functions.items():
                if func == 'SUM':
                    self.groups[key]['sums'][col] = \
                        self.groups[key]['sums'].get(col, 0) + row[col]
        
        # Generate results
        results = []
        for key, group_data in self.groups.items():
            result_row = dict(zip(self.group_columns, key))
            
            for agg_name, (func, col) in self.agg_functions.items():
                if func == 'COUNT':
                    result_row[agg_name] = group_data['count']
                elif func == 'SUM':
                    result_row[agg_name] = group_data['sums'][col]
                elif func == 'AVG':
                    result_row[agg_name] = group_data['sums'][col] / group_data['count']
                elif func == 'MIN':
                    result_row[agg_name] = min(r[col] for r in group_data['rows'])
                elif func == 'MAX':
                    result_row[agg_name] = max(r[col] for r in group_data['rows'])
            
            results.append(result_row)
        
        self.result_iter = iter(results)
    
    def next(self):
        try:
            return next(self.result_iter)
        except StopIteration:
            return None
```

### Views

**Virtual Table Implementation**:

```python
class View:
    def __init__(self, name, query_ast):
        self.name = name
        self.query_ast = query_ast
    
    def materialize(self, database):
        # Execute underlying query
        plan = database.planner.create_plan(self.query_ast)
        return database.executor.execute(plan)

# Usage
db.execute("CREATE VIEW high_earners AS SELECT * FROM employees WHERE salary > 100000")
db.execute("SELECT * FROM high_earners")  # Transparently executes underlying query
```

### Subqueries

**Correlated vs Non-Correlated**:

```sql
-- Non-correlated (can execute once)
SELECT * FROM employees
WHERE department_id IN (SELECT id FROM departments WHERE location = 'NY')

-- Correlated (must execute for each outer row)
SELECT e1.name, e1.salary
FROM employees e1
WHERE e1.salary > (SELECT AVG(e2.salary) FROM employees e2 WHERE e2.department = e1.department)
```

**Implementation Strategy**:
- Non-correlated: Execute subquery once, use results in outer query
- Correlated: Use semi-join or nested loop with caching

### Window Functions

**Example**:

```sql
SELECT
    name,
    salary,
    department,
    AVG(salary) OVER (PARTITION BY department) as dept_avg,
    RANK() OVER (ORDER BY salary DESC) as salary_rank
FROM employees
```

**Implementation**: Requires sorting/partitioning, then window frame evaluation

### Full-Text Search

**Inverted Index**:

```python
inverted_index = {
    'hello': [1, 5, 12],      # document IDs containing "hello"
    'world': [1, 3, 8, 12],
    'database': [5, 12, 15]
}

# Search for "hello world"
results = set(inverted_index['hello']) & set(inverted_index['world'])
# Returns: {1, 12}
```

### Columnar Storage

**Row vs Column Layout**:

```
Row-oriented:
[{id:1, name:'Alice', age:25}, {id:2, name:'Bob', age:30}]

Column-oriented:
{
    'id': [1, 2],
    'name': ['Alice', 'Bob'],
    'age': [25, 30]
}
```

**Benefits**:
- Better compression (similar values together)
- Faster aggregations (read only needed columns)
- SIMD vectorization

**Use Case**: Analytics queries (OLAP)

---

## Summary

This architecture provides a solid foundation for building a premium RDBMS with:

- **Clean separation of concerns** across layers
- **Extensible design** using proven patterns
- **Performance optimization** at multiple levels
- **ACID compliance** for data integrity
- **Standard SQL interface** for fam