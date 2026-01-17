package com.byteforce.core;

import com.byteforce.antlr.SqlBaseVisitor;
import com.byteforce.antlr.SqlLexer;
import com.byteforce.antlr.SqlParser;
import com.byteforce.core.models.Column;
import com.byteforce.core.models.DataType;
import java.util.*;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

public class SQLParser {

  public Map<String, Object> parse(String sql) {
    SqlLexer lexer = new SqlLexer(CharStreams.fromString(sql));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    SqlParser parser = new SqlParser(tokens);

    ParseTree tree = parser.parse();
    PlanBuilder visitor = new PlanBuilder();
    return asMap(visitor.visit(tree));
  }

  private static Map<String, Object> asMap(Object obj) {
    Map<String, Object> map = new HashMap<>();
    if (obj instanceof Map) {
      for (Map.Entry<?, ?> entry : ((Map<?, ?>) obj).entrySet()) {
        if (entry.getKey() instanceof String) {
          map.put((String) entry.getKey(), entry.getValue());
        }
      }
    }
    return map;
  }

  private static List<String> asStringList(Object obj) {
    List<String> list = new ArrayList<>();
    if (obj instanceof List) {
      for (Object item : (List<?>) obj) {
        if (item instanceof String) {
          list.add((String) item);
        }
      }
    }
    return list;
  }

  private static List<Object> asList(Object obj) {
    List<Object> list = new ArrayList<>();
    if (obj instanceof List) {
      list.addAll((List<?>) obj);
    }
    return list;
  }

  private static class PlanBuilder extends SqlBaseVisitor<Object> {

    @Override
    public Object visitParse(SqlParser.ParseContext ctx) {
      return visit(ctx.statement());
    }

    @Override
    public Object visitStatement(SqlParser.StatementContext ctx) {
      return visit(ctx.getChild(0));
    }

    @Override
    public Object visitCreateTableStmt(SqlParser.CreateTableStmtContext ctx) {
      Map<String, Object> plan = new HashMap<>();
      plan.put("type", "create_table");
      plan.put("table_name", ctx.table_name.getText());

      List<Column> columns = new ArrayList<>();
      for (SqlParser.ColDefContext colCtx : ctx.colDef()) {
        Object res = visit(colCtx);
        if (res instanceof Column) {
          columns.add((Column) res);
        }
      }
      plan.put("columns", columns);
      return plan;
    }

    @Override
    public Object visitColDef(SqlParser.ColDefContext ctx) {
      String name = ctx.col_name.getText();
      Object typeObj = visit(ctx.typeName());
      DataType type = typeObj instanceof DataType ? (DataType) typeObj : DataType.TEXT;

      boolean isPk = false;
      boolean isUnique = false;
      boolean isNullable = true;

      for (SqlParser.ConstraintContext c : ctx.constraint()) {
        Object cObj = visit(c);
        if (cObj instanceof String) {
          String cStr = (String) cObj;
          if ("pk".equals(cStr)) isPk = true;
          if ("unique".equals(cStr)) isUnique = true;
          if ("not_null".equals(cStr)) isNullable = false;
        }
      }

      return new Column(name, type, isPk, isUnique, isNullable);
    }

    @Override
    public Object visitTypeName(SqlParser.TypeNameContext ctx) {
      if (ctx.K_INTEGER() != null) return DataType.INTEGER;
      if (ctx.K_TEXT() != null) return DataType.TEXT;
      if (ctx.K_BOOLEAN() != null) return DataType.BOOLEAN;
      if (ctx.K_FLOAT() != null) return DataType.FLOAT;
      return DataType.TEXT;
    }

    @Override
    public Object visitConstraint(SqlParser.ConstraintContext ctx) {
      if (ctx.K_PRIMARY() != null) return "pk";
      if (ctx.K_UNIQUE() != null) return "unique";
      if (ctx.K_NOT() != null) return "not_null";
      return "";
    }

    @Override
    public Object visitCreateIndexStmt(SqlParser.CreateIndexStmtContext ctx) {
      Map<String, Object> plan = new HashMap<>();
      plan.put("type", "create_index");
      plan.put("index_name", ctx.index_name.getText());
      plan.put("table_name", ctx.table_name.getText());
      plan.put("column_name", ctx.column_name.getText());
      return plan;
    }

    @Override
    public Object visitInsertStmt(SqlParser.InsertStmtContext ctx) {
      Map<String, Object> plan = new HashMap<>();
      plan.put("type", "insert");
      plan.put("table_name", ctx.table_name.getText());

      if (ctx.columnList() != null) {
        plan.put("columns", asStringList(visit(ctx.columnList())));
      } else {
        plan.put("columns", null);
      }

      plan.put("values", asList(visit(ctx.valueList())));
      return plan;
    }

    @Override
    public Object visitValueList(SqlParser.ValueListContext ctx) {
      List<Object> values = new ArrayList<>();
      for (SqlParser.ValueContext v : ctx.value()) {
        values.add(visit(v));
      }
      return values;
    }

    @Override
    public Object visitSelectStmt(SqlParser.SelectStmtContext ctx) {
      Map<String, Object> plan = new HashMap<>();
      plan.put("type", "select");
      plan.put("table_name", ctx.table_name.getText());

      if (ctx.K_ASTERISK() != null) {
        plan.put("columns", "*");
      } else {
        plan.put("columns", asStringList(visit(ctx.columnList())));
      }

      if (ctx.joinClause() != null) {
        plan.put("join", asMap(visit(ctx.joinClause())));
      }

      if (ctx.whereClause() != null) {
        plan.put("where", asMap(visit(ctx.whereClause())));
      }

      if (ctx.limitClause() != null) {
        Object limitRes = visit(ctx.limitClause());
        if (limitRes instanceof Integer) {
          plan.put("limit", limitRes);
        }
      }

      return plan;
    }

    @Override
    public Object visitColumnList(SqlParser.ColumnListContext ctx) {
      List<String> cols = new ArrayList<>();
      for (SqlParser.IdentifierContext id : ctx.identifier()) {
        cols.add(id.getText());
      }
      return cols;
    }

    @Override
    public Object visitJoinClause(SqlParser.JoinClauseContext ctx) {
      Map<String, Object> join = new HashMap<>();
      join.put("join_table", ctx.join_table.getText());
      join.put("condition", asMap(visit(ctx.condition())));
      return join;
    }

    @Override
    public Object visitWhereClause(SqlParser.WhereClauseContext ctx) {
      return visit(ctx.condition());
    }

    @Override
    public Object visitCondition(SqlParser.ConditionContext ctx) {
      Map<String, Object> cond = new HashMap<>();
      cond.put("column", ctx.left.getText());
      cond.put("operator", ctx.operator().getText());
      cond.put("value", visit(ctx.right));
      return cond;
    }

    @Override
    public Object visitCondValue(SqlParser.CondValueContext ctx) {
      if (ctx.identifier() != null) {
        Map<String, Object> colRef = new HashMap<>();
        colRef.put("type", "column");
        colRef.put("name", ctx.identifier().getText());
        return colRef;
      }
      return visit(ctx.value());
    }

    @Override
    public Object visitLimitClause(SqlParser.LimitClauseContext ctx) {
      return Integer.parseInt(ctx.limit_val.getText());
    }

    @Override
    public Object visitUpdateStmt(SqlParser.UpdateStmtContext ctx) {
      Map<String, Object> plan = new HashMap<>();
      plan.put("type", "update");
      plan.put("table_name", ctx.table_name.getText());

      Map<String, Object> assignments = new HashMap<>();
      for (SqlParser.AssignmentContext a : ctx.assignment()) {
        Object res = visit(a);
        if (res instanceof Map.Entry) {
          Map.Entry<?, ?> entry = (Map.Entry<?, ?>) res;
          if (entry.getKey() instanceof String) {
            assignments.put((String) entry.getKey(), entry.getValue());
          }
        }
      }
      plan.put("assignments", assignments);

      if (ctx.whereClause() != null) {
        plan.put("where", asMap(visit(ctx.whereClause())));
      }
      return plan;
    }

    @Override
    public Object visitAssignment(SqlParser.AssignmentContext ctx) {
      return new AbstractMap.SimpleEntry<>(ctx.column_name.getText(), visit(ctx.value()));
    }

    @Override
    public Object visitDeleteStmt(SqlParser.DeleteStmtContext ctx) {
      Map<String, Object> plan = new HashMap<>();
      plan.put("type", "delete");
      plan.put("table_name", ctx.table_name.getText());

      if (ctx.whereClause() != null) {
        plan.put("where", asMap(visit(ctx.whereClause())));
      }
      return plan;
    }

    @Override
    public Object visitValue(SqlParser.ValueContext ctx) {
      if (ctx.INTEGER_LITERAL() != null) return Integer.parseInt(ctx.INTEGER_LITERAL().getText());
      if (ctx.FLOAT_LITERAL() != null) return Double.parseDouble(ctx.FLOAT_LITERAL().getText());
      if (ctx.STRING_LITERAL() != null) {
        String s = ctx.STRING_LITERAL().getText();
        return s.substring(1, s.length() - 1); // remove quotes
      }
      if (ctx.K_TRUE() != null) return true;
      if (ctx.K_FALSE() != null) return false;
      if (ctx.K_NULL() != null) return null;
      if (ctx.PLACEHOLDER() != null) {
        Map<String, String> p = new HashMap<>();
        p.put("type", "placeholder");
        return p;
      }
      return null;
    }
  }
}
