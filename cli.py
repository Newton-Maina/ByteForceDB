import sys
import time
import csv
import random
import string
from rich.console import Console
from rich.table import Table as RichTable
from prompt_toolkit import PromptSession
from prompt_toolkit.history import FileHistory
from prompt_toolkit.lexers import PygmentsLexer
from prompt_toolkit.completion import WordCompleter
from prompt_toolkit.styles import Style
from pygments.lexers.sql import SqlLexer
from core.database import ByteForceDB
from core.models import DataType

console = Console()

# SQL Keywords for Auto-completion
sql_completer = WordCompleter([
    'SELECT', 'FROM', 'WHERE', 'INSERT', 'INTO', 'VALUES', 'CREATE', 'TABLE', 
    'INDEX', 'UPDATE', 'SET', 'DELETE', 'JOIN', 'ON', 'PRIMARY', 'KEY', 
    'UNIQUE', 'NOT', 'NULL', 'INTEGER', 'TEXT', 'FLOAT', 'BOOLEAN',
    '.exit', '.tables', '.schema', '.help', '.seed', '.export'
], ignore_case=True)

def main():
    db = ByteForceDB()
    
    # Setup history and session with "Pro" features
    session = PromptSession(
        history=FileHistory('.history'),
        lexer=PygmentsLexer(SqlLexer),
        completer=sql_completer,
        style=Style.from_dict({
            'completion-menu.completion': 'bg:#008888 #ffffff',
            'completion-menu.completion.current': 'bg:#00aaaa #000000',
            'scrollbar.background': 'bg:#88aaaa',
            'scrollbar.button': 'bg:#222222',
        })
    )

    console.print("="*50)
    console.print("[bold green]ByteForce RDBMS - Junior Developer Assessment[/bold green]")
    console.print("[dim]Candidate: Newton Maina[/dim]")
    console.print("="*50)
    console.print("Type [bold].help[/bold] for commands or [bold].exit[/bold] to quit.")
    console.print("")

    while True:
        try:
            # Add a bottom toolbar
            text = session.prompt(
                'ByteForce> ',
                bottom_toolbar=" [F4] Toggle History | [Tab] Autocomplete | Newton Maina @ Pesapal "
            )
            
            if not text.strip():
                continue
                
            if text.strip().lower() == '.exit':
                console.print("[yellow]Goodbye![/yellow]")
                break
                
            if text.strip().startswith('.'):
                handle_meta_command(text.strip(), db)
                continue

            # Time the execution
            start_time = time.perf_counter()
            result = db.execute(text)
            end_time = time.perf_counter()
            duration = end_time - start_time

            display_result(result)
            console.print(f"[dim]Query executed in {duration:.4f} seconds[/dim]")

        except KeyboardInterrupt:
            continue
        except EOFError:
            break
        except Exception as e:
            console.print(f"[red]Error:[/red] {str(e)}")

def handle_meta_command(command, db):
    parts = command.split()
    cmd = parts[0]

    if cmd == '.help':
        console.print("[bold blue]Available Meta Commands:[/bold blue]")
        console.print("  .tables              - List all tables")
        console.print("  .schema <table>      - Show schema for a table")
        console.print("  .seed <table> <num>  - Insert <num> random rows into <table>")
        console.print("  .export <table> <f>  - Export table to CSV file <f>")
        console.print("  .help                - Show this menu")
        console.print("  .exit                - Quit")
    
    elif cmd == '.tables':
        tables = db.storage.list_tables()
        console.print(f"Tables: {', '.join(tables) if tables else 'None'}")
    
    elif cmd == '.schema' and len(parts) > 1:
        table_name = parts[1]
        table = db.storage.get_table(table_name)
        if table:
            console.print(f"Schema for {table_name}:")
            for col_name, col in table.columns.items():
                extra = []
                if col.is_primary_key: extra.append("PK")
                if col.is_unique: extra.append("UNIQUE")
                if not col.is_nullable: extra.append("NOT NULL")
                console.print(f"  {col_name}: {col.data_type.name} {' '.join(extra)}")
        else:
            console.print(f"[red]Table {table_name} not found.[/red]")

    elif cmd == '.seed' and len(parts) > 2:
        generate_data(db, parts[1], int(parts[2]))

    elif cmd == '.export' and len(parts) > 2:
        export_data(db, parts[1], parts[2])

    else:
        console.print(f"Unknown command or wrong arguments: {command}")

def generate_data(db, table_name, count):
    table = db.storage.get_table(table_name)
    if not table:
        console.print(f"[red]Table {table_name} not found.[/red]")
        return

    console.print(f"Seeding {count} rows into {table_name}...")
    start = time.time()
    successful = 0
    
    for i in range(count):
        row = {}
        for col in table.columns.values():
            if col.is_primary_key and col.data_type == DataType.INTEGER:
                # Auto-increment logic mostly
                val = len(table.rows) + 1 + i 
            elif col.data_type == DataType.INTEGER:
                val = random.randint(1, 10000)
            elif col.data_type == DataType.FLOAT:
                val = round(random.uniform(10.0, 500.0), 2)
            elif col.data_type == DataType.BOOLEAN:
                val = random.choice([True, False])
            else: # TEXT
                val = ''.join(random.choices(string.ascii_letters, k=8))
            row[col.name] = val
        
        try:
            table.add_row(row)
            successful += 1
        except ValueError:
            continue # Skip constraint violations during random generation

    # Bulk save at the end
    db.storage.save_table(table_name)
    console.print(f"[green]Successfully seeded {successful} rows in {time.time() - start:.2f}s.[/green]")

def export_data(db, table_name, filename):
    table = db.storage.get_table(table_name)
    if not table:
        console.print(f"[red]Table {table_name} not found.[/red]")
        return
    
    if not table.rows:
        console.print("[yellow]Table is empty.[/yellow]")
        return

    try:
        with open(filename, 'w', newline='') as csvfile:
            writer = csv.DictWriter(csvfile, fieldnames=table.columns.keys())
            writer.writeheader()
            writer.writerows(table.rows)
        console.print(f"[green]Exported {len(table.rows)} rows to {filename}[/green]")
    except Exception as e:
        console.print(f"[red]Export failed: {e}[/red]")

def display_result(result):
    if isinstance(result, str):
        console.print(result)
    elif isinstance(result, list):
        if not result:
            console.print("Empty set.")
            return

        table = RichTable()
        # Use keys from first row for headers
        headers = result[0].keys()
        for header in headers:
            table.add_column(header)
        
        for row in result:
            table.add_row(*[str(row.get(h)) for h in headers])
        
        console.print(table)
        console.print(f"{len(result)} row(s) in set.")

if __name__ == "__main__":
    main()