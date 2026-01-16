import sys
from rich.console import Console
from rich.table import Table as RichTable
from prompt_toolkit import PromptSession
from prompt_toolkit.history import FileHistory
from core.database import ByteForceDB

console = Console()

def main():
    db = ByteForceDB()
    session = PromptSession(history=FileHistory('.history'))

    console.print("[bold green]ByteForce RDBMS - Technical Assessment[/bold green]")
    console.print("[dim]Newton Maina[/dim]")
    console.print("="*50)
    console.print("Type [bold].help[/bold] for commands or [bold].exit[/bold] to quit.")
    console.print("")

    while True:
        try:
            text = session.prompt('ByteForce> ')
            if not text.strip():
                continue
            if text.strip().lower() == '.exit':
                break
            if text.strip().startswith('.'):
                handle_meta_command(text.strip(), db)
                continue

            result = db.execute(text)
            display_result(result)

        except KeyboardInterrupt:
            continue
        except EOFError:
            break
        except Exception as e:
            console.print(f"[red]Error:[/red] {str(e)}")

def handle_meta_command(command, db):
    if command == '.help':
        console.print("[bold blue]Available Meta Commands:[/bold blue]")
        console.print("  .tables          - List all tables in the database")
        console.print("  .schema <table>  - Show the schema for a specific table")
        console.print("  .help            - Show this help menu")
        console.print("  .exit            - Exit the application")
        console.print("")
        console.print("[bold blue]SQL Support:[/bold blue]")
        console.print("  CREATE TABLE, INSERT, SELECT, UPDATE, DELETE, CREATE INDEX")
    elif command == '.tables':
        tables = db.storage.list_tables()
        console.print(f"Tables: {', '.join(tables) if tables else 'None'}")
    elif command.startswith('.schema'):
        parts = command.split()
        if len(parts) > 1:
            table_name = parts[1]
            table = db.storage.get_table(table_name)
            if table:
                console.print(f"Schema for {table_name}:")
                for col_name, col in table.columns.items():
                    console.print(f"  {col_name}: {col.data_type.name} {'PK' if col.is_primary_key else ''}")
            else:
                console.print(f"[red]Table {table_name} not found.[/red]")
    else:
        console.print(f"Unknown meta command: {command}")

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