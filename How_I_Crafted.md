# How I Crafted ByteForce: A Developer's Journal

This document outlines my thought process, and the decisions I made while building **ByteForce** for the Pesapal Technical Assessment.

## 1. The "Why" and The Tools
Mai motive was that I wanted to simulate a real-world database development experience.

*   **DBeaver & Real-World Context**: I regularly use tools like DBeaver to interact with Postgres and MySQL. I noticed how tedious it can be to manually `INSERT` rows one by one for testing. This observation directly inspired the `.seed` command (see Section 3).
*   **Python Stack**: I chose Python 3.9+ because its standard library is rich, and libraries like `lark` (for parsing) and `rich` (for UI) allow for rapid development of complex systems without reinventing the wheel. Apparently Rust is or was the top recommendation to working on this but i had to work with logic "engraved in my fingertips hehe".

## 2. The Architecture (The Four Layers)
Before writing a single line of code, I conceptualized the system as four distinct layers. This separation of concerns was crucial for keeping the codebase maintainable.

1.  **Presentation Layer (CLI)**: Handles user input, history, and formatting. It knows *nothing* about how data is stored.
2.  **Facade Layer (ByteForceDB)**: The API surface. It initializes the subsystems and orchestrates the flow. This is the **Facade Pattern**.
3.  **Logic Layer (Executor)**: The brain. It interprets the AST (Abstract Syntax Tree) and decides *how* to retrieve data (e.g., "Do I scan the whole table or use the index?").
4.  **Persistence Layer (Storage)**: The disk manager. It handles serialization (pickling) and file I/O, abstracting these details away from the rest of the system.

## 3. Iterative Development (TDD Approach)
I followed a Test-Driven Development (TDD) cycle to ensure stability at every layer.

1.  **Phase 1: The Foundation (Basic CRUD)**
    *   *Goal*: Can I store data?
    *   *Action*: I started by defining the `Table` and `Column` models (`core/models.py`) and a simple in-memory storage engine.
    *   *Test*: Wrote `test_create_table` and `test_insert_and_select` to verify basic persistence.

2.  **Phase 2: The Grammar (Parsing)**
    *   *Challenge*: Parsing SQL is hard. Regex isn't enough.
    *   *Solution*: I implemented a grammar using **Lark**. It allowed me to define rules like `select_stmt` and `where_clause` cleanly.
    *   *Refinement*: I initially struggled with parsing strings vs. identifiers but refined the grammar to handle `CNAME` (identifiers) and `STRING` (literals) correctly.

3.  **Phase 3: Relational Logic (JOINs)**
    *   *Goal*: Connect two tables.
    *   *Action*: Implemented a Nested Loop Join algorithm.
    *   *Test*: Created `test_join` where I joined `users` and `orders`. Seeing the data merge correctly was a major milestone.

4.  **Phase 4: Optimization (Indexing)**
    *   *Goal*: Make it fast.
    *   *Action*: Implemented a Hash Index.
    *   *Verification*: Updated the execution engine to check if a `WHERE` clause references an indexed column. If so, it skips the full table scan.

## 3. Solving the "Tedious Data" Problem
As mentioned earlier, manually typing `INSERT INTO users...` 50 times to test performance is impractical.

*   **The `.seed` Command**: I researched how to generate realistic dummy data.
    *   *Logic*: I built a generator that inspects the column type. If it's `INTEGER`, pick a random number. If it's `TEXT`, generate a random string.
    *   *Result*: Now, `ByteForce> .seed users 1000` populates the database instantly, allowing me to actually see the difference my Hash Index makes (0.0001s vs 0.05s).

## 4. Code Quality & Professionalism
Writing code that works is only half the battle; writing code that *others can read* is the other half.

*   **Linting (`lint.bat`)**: I integrated `Black`, an uncompromising code formatter. This ensures that no matter how fast I code, the final output always adheres to PEP 8 standards.
*   **Documentation**: I added Mermaid diagrams and Docstrings because I believe a project isn't finished until it's documented.

## 5. Limitations & Self-Reflection (The "1TB" Problem)
This just popped in my mind: While testing, I realized a significant edge case I didn't solve for in this prototype: **Memory Management**.

Currently, the `StorageEngine` uses Python's `pickle` module to serialize the entire `Table` object. This means if the database grew to **1TB**, "what are the chances but who knows - my dbs only grow to megabytes hehe" it would attempt to load all 1TB into my 12GB of RAM, immediately crashing the system (Out of Memory).

**How I would fix this in V2:**
Instead of loading the whole file, I would implement a **Page-Based Storage Engine**:
1.  Split the data file into fixed-size chunks (e.g., **4KB Pages**).
2.  Implement a **Buffer Pool Manager** to cache only the most frequently used pages in RAM.
3.  Read/Write only specific pages from disk as needed, allowing the DB to handle datasets much larger than available RAM.

## 6. Conclusion
Building ByteForce was an exercise in understanding the layers of abstraction that make modern software possible. From parsing text to managing disk I/O, every component had to be carefully crafted and tested.
