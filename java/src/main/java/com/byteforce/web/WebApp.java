package com.byteforce.web;

import static spark.Spark.*;

import com.byteforce.core.ByteForceDB;
import com.byteforce.core.ExecutionResult;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import spark.ModelAndView;
import spark.template.handlebars.HandlebarsTemplateEngine;

public class WebApp {
  private static ByteForceDB db;

  public static void main(String[] args) {
    port(4567);

    db = new ByteForceDB("data");
    initDb();

    HandlebarsTemplateEngine engine = new HandlebarsTemplateEngine();

    get(
        "/",
        (req, res) -> {
          res.redirect("/tasks/active");
          return null;
        });

    get(
        "/tasks/:view",
        (req, res) -> {
          String view = req.params(":view");
          ExecutionResult result =
              db.execute("SELECT * FROM tasks LEFT JOIN subtasks ON id = parent_id");

          if (result.isError()) return "Error: " + result.getMessage();

          // Intermediate structure to group subtasks by task ID
          Map<Integer, TaskBuilder> builders = new LinkedHashMap<>();
          for (Map<String, Object> row : result.getRows()) {
            Integer id = (Integer) row.get("id");
            if (id == null) continue;

            builders.putIfAbsent(id, new TaskBuilder(row));
            TaskBuilder builder = builders.get(id);

            if (row.get("sid") != null) {
              builder.addSubTask(row);
            }
          }

          List<Task> finalTasks =
              builders.values().stream()
                  .map(TaskBuilder::build)
                  .filter(
                      t -> {
                        if ("trash".equals(view)) return t.isDeleted();
                        if ("drafts".equals(view)) return !t.isDeleted() && t.isDraft();
                        return !t.isDeleted() && !t.isDraft();
                      })
                  .sorted(
                      (t1, t2) -> {
                        if (t1.isCompleted() != t2.isCompleted()) return t1.isCompleted() ? 1 : -1;
                        return Integer.compare(t2.id(), t1.id());
                      })
                  .collect(Collectors.toList());

          Map<String, Object> model = new HashMap<>();
          model.put("tasks", finalTasks);
          model.put("view", view);
          model.put("isActive", "active".equals(view));
          model.put("isDrafts", "drafts".equals(view));
          model.put("isTrash", "trash".equals(view));

          return engine.render(new ModelAndView(model, "index.hbs"));
        });

    post(
        "/add",
        (req, res) -> {
          String title = req.queryParams("title");
          boolean isDraft = "on".equals(req.queryParams("is_draft"));
          if (title != null && !title.isEmpty()) {
            int newId = getNextId("tasks", "id");
            String createdAt =
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            db.execute(
                "INSERT INTO tasks VALUES (?, ?, 'pending', ?, ?, ?)",
                Arrays.asList(newId, title, createdAt, isDraft, false));
          }
          res.redirect("/tasks/" + (isDraft ? "drafts" : "active"));
          return null;
        });

    post(
        "/subtask/add",
        (req, res) -> {
          String parentIdStr = req.queryParams("parent_id");
          String subtitle = req.queryParams("subtitle");
          if (parentIdStr != null && subtitle != null) {
            int sid = getNextId("subtasks", "sid");
            db.execute(
                "INSERT INTO subtasks VALUES (?, ?, ?, 'pending')",
                Arrays.asList(sid, Integer.parseInt(parentIdStr), subtitle));
          }
          res.redirect("/tasks/active");
          return null;
        });

    get(
        "/subtask/toggle/:sid",
        (req, res) -> {
          int sid = Integer.parseInt(req.params(":sid"));
          ExecutionResult r =
              db.execute("SELECT * FROM subtasks WHERE sid = ?", Collections.singletonList(sid));
          if (!r.getRows().isEmpty()) {
            String next =
                "pending".equals(r.getRows().get(0).get("substatus")) ? "completed" : "pending";
            db.execute("UPDATE subtasks SET substatus = ? WHERE sid = ?", Arrays.asList(next, sid));
          }
          res.redirect("/tasks/active");
          return null;
        });

    get(
        "/action/:action/:id",
        (req, res) -> {
          String action = req.params(":action");
          int id = Integer.parseInt(req.params(":id"));
          ExecutionResult r =
              db.execute("SELECT * FROM tasks WHERE id = ?", Collections.singletonList(id));
          if (r.getRows().isEmpty()) return "Not Found";
          Map<String, Object> task = r.getRows().get(0);

          if ("toggle".equals(action)) {
            String status = "pending".equals(task.get("status")) ? "completed" : "pending";
            db.execute("UPDATE tasks SET status = ? WHERE id = ?", Arrays.asList(status, id));
          } else if ("delete".equals(action)) {
            db.execute("UPDATE tasks SET is_deleted = ? WHERE id = ?", Arrays.asList(true, id));
          } else if ("restore".equals(action)) {
            db.execute("UPDATE tasks SET is_deleted = ? WHERE id = ?", Arrays.asList(false, id));
          } else if ("publish".equals(action)) {
            db.execute("UPDATE tasks SET is_draft = ? WHERE id = ?", Arrays.asList(false, id));
          } else if ("destroy".equals(action)) {
            db.execute("DELETE FROM tasks WHERE id = ?", Collections.singletonList(id));
            db.execute("DELETE FROM subtasks WHERE parent_id = ?", Collections.singletonList(id));
          }

          String ref = req.headers("Referer");
          if (ref != null && ref.contains("trash")) res.redirect("/tasks/trash");
          else if (ref != null && ref.contains("drafts")) res.redirect("/tasks/drafts");
          else res.redirect("/tasks/active");
          return null;
        });

    System.out.println("Java Server running on http://localhost:4567");
  }

  private static void initDb() {
    List<String> tables = db.getStorage().listTables();
    if (!tables.contains("tasks")) {
      db.execute(
          "CREATE TABLE tasks (id INTEGER PRIMARY KEY, title TEXT, status TEXT, created_at TEXT, is_draft BOOLEAN, is_deleted BOOLEAN)");
    }
    if (!tables.contains("subtasks")) {
      db.execute(
          "CREATE TABLE subtasks (sid INTEGER PRIMARY KEY, parent_id INTEGER, subtitle TEXT, substatus TEXT)");
    }
  }

  private static int getNextId(String table, String idCol) {
    ExecutionResult r = db.execute("SELECT * FROM " + table);
    return r.getRows().stream().mapToInt(row -> (Integer) row.get(idCol)).max().orElse(0) + 1;
  }

  // Helper class to accumulate subtasks
  private static class TaskBuilder {
    private final int id;
    private final String title;
    private final String status;
    private final String createdAt;
    private final boolean isDraft;
    private final boolean isDeleted;
    private final List<SubTask> subtasks = new ArrayList<>();

    TaskBuilder(Map<String, Object> row) {
      this.id = (Integer) row.get("id");
      this.title = (String) row.get("title");
      this.status = (String) row.get("status");
      this.createdAt = (String) row.get("created_at");
      this.isDraft = (Boolean) row.get("is_draft");
      this.isDeleted = (Boolean) row.get("is_deleted");
    }

    void addSubTask(Map<String, Object> row) {
      subtasks.add(
          new SubTask(
              (Integer) row.get("sid"),
              (Integer) row.get("parent_id"),
              (String) row.get("subtitle"),
              (String) row.get("substatus"),
              "completed".equals(row.get("substatus"))));
    }

    Task build() {
      return new Task(
          id, title, status, "completed".equals(status), createdAt, isDraft, isDeleted, subtasks);
    }
  }
}
