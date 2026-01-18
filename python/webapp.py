import os
import datetime
from flask import Flask, render_template, request, redirect, url_for, flash
from core.database import ByteForceDB

app = Flask(__name__)
app.secret_key = 'byteforce-4fefde4566-tech-assssment-54ftsdrtse456-secret-key'

# Initialize Database
db = ByteForceDB(data_dir="data")

def init_db():
    tables = db.storage.list_tables()
    
    if 'tasks' not in tables:
        db.execute("""
            CREATE TABLE tasks (
                id INTEGER PRIMARY KEY, 
                title TEXT, 
                status TEXT, 
                created_at TEXT, 
                is_draft BOOLEAN,
                is_deleted BOOLEAN
            )
        """)
    
    if 'subtasks' not in tables:
        # distinct column names to avoid collision in simple dict-based join
        db.execute("""
            CREATE TABLE subtasks (
                sid INTEGER PRIMARY KEY,
                parent_id INTEGER,
                subtitle TEXT,
                substatus TEXT
            )
        """)
        print("Database initialized with tasks and subtasks.")

init_db()

def get_next_id(table_name, id_col):
    rows = db.execute(f"SELECT * FROM {table_name}")
    if isinstance(rows, list) and rows:
        return max([r.get(id_col, 0) for r in rows]) + 1
    return 1

@app.route('/')
def index():
    return redirect(url_for('list_tasks', view='active'))

@app.route('/tasks/<view>')
def list_tasks(view):
    # USE THE NEW LEFT JOIN!
    # Fetch tasks and their subtasks in one go
    raw_data = db.execute("SELECT * FROM tasks LEFT JOIN subtasks ON id = parent_id")
    
    if isinstance(raw_data, str) and raw_data.startswith("Error"):
        return raw_data, 500
    
    # Process flat join results into hierarchical objects
    tasks_map = {}
    
    for row in raw_data:
        t_id = row.get('id')
        if not t_id: continue # Should not happen unless row is empty
        
        if t_id not in tasks_map:
            # New task encountered
            tasks_map[t_id] = {
                'id': row['id'],
                'title': row['title'],
                'status': row['status'],
                'created_at': row['created_at'],
                'is_draft': row.get('is_draft', False),
                'is_deleted': row.get('is_deleted', False),
                'subtasks': []
            }
        
        # Check if this row has a subtask (sid will be present and not None)
        if row.get('sid') is not None:
            subtask = {
                'sid': row['sid'],
                'parent_id': row['parent_id'],
                'subtitle': row['subtitle'],
                'substatus': row['substatus']
            }
            tasks_map[t_id]['subtasks'].append(subtask)

    # Filter based on view
    final_tasks = []
    for task in tasks_map.values():
        is_deleted = task['is_deleted']
        is_draft = task['is_draft']
        
        if view == 'trash':
            if is_deleted: final_tasks.append(task)
        elif view == 'drafts':
            if not is_deleted and is_draft: final_tasks.append(task)
        else: # active
            if not is_deleted and not is_draft: final_tasks.append(task)

    # Sort
    final_tasks.sort(key=lambda x: (x.get('status') == 'completed', -x.get('id')))
    
    return render_template('index.html', tasks=final_tasks, view=view)

@app.route('/add', methods=['POST'])
def add():
    title = request.form.get('title')
    is_draft = request.form.get('is_draft') == 'on'
    
    if title:
        new_id = get_next_id('tasks', 'id')
        created_at = datetime.datetime.now().strftime("%Y-%m-%d %H:%M")
        db.execute(
            "INSERT INTO tasks VALUES (?, ?, 'pending', ?, ?, ?)", 
            [new_id, title, created_at, is_draft, False]
        )
        flash("Draft saved." if is_draft else "Task added.", 'success')
        
    return redirect(url_for('list_tasks', view='drafts' if is_draft else 'active'))

@app.route('/subtask/add', methods=['POST'])
def add_subtask():
    parent_id = request.form.get('parent_id')
    subtitle = request.form.get('subtitle')
    
    if parent_id and subtitle:
        sid = get_next_id('subtasks', 'sid')
        db.execute(
            "INSERT INTO subtasks VALUES (?, ?, ?, 'pending')",
            [sid, int(parent_id), subtitle]
        )
        flash("Subtask added.", "success")
        
    return redirect(url_for('list_tasks', view='active'))

@app.route('/subtask/toggle/<int:sid>')
def toggle_subtask(sid):
    # We need to find the subtask first to check status
    # Simple query (no join needed for simple update)
    rows = db.execute("SELECT * FROM subtasks WHERE sid = ?", [sid])
    if rows and isinstance(rows, list):
        st = rows[0]
        new_status = 'completed' if st['substatus'] == 'pending' else 'pending'
        db.execute("UPDATE subtasks SET substatus = ? WHERE sid = ?", [new_status, sid])
    
    return redirect(url_for('list_tasks', view='active'))

@app.route('/action/<action>/<int:task_id>')
def task_action(action, task_id):
    rows = db.execute("SELECT * FROM tasks WHERE id = ?", [task_id])
    if not rows or not isinstance(rows, list): return "Task not found", 404
    task = rows[0]
    
    if action == 'toggle':
        new_status = 'completed' if task['status'] == 'pending' else 'pending'
        db.execute("UPDATE tasks SET status = ? WHERE id = ?", [new_status, task_id])
    elif action == 'delete':
        db.execute("UPDATE tasks SET is_deleted = ? WHERE id = ?", [True, task_id])
        flash('Moved to Trash.', 'warning')
    elif action == 'restore':
        db.execute("UPDATE tasks SET is_deleted = ? WHERE id = ?", [False, task_id])
        flash('Task restored.', 'success')
    elif action == 'publish':
        db.execute("UPDATE tasks SET is_draft = ? WHERE id = ?", [False, task_id])
        flash('Task published!', 'success')
    elif action == 'destroy':
        db.execute("DELETE FROM tasks WHERE id = ?", [task_id])
        # Also clean up subtasks? ideally yes, but keeping it simple
        db.execute("DELETE FROM subtasks WHERE parent_id = ?", [task_id])
        flash('Permanently deleted.', 'danger')

    referer = request.headers.get("Referer", "")
    if 'trash' in referer: return redirect(url_for('list_tasks', view='trash'))
    if 'drafts' in referer: return redirect(url_for('list_tasks', view='drafts'))
    return redirect(url_for('list_tasks', view='active'))

if __name__ == '__main__':
    app.run(debug=True, port=5000)
