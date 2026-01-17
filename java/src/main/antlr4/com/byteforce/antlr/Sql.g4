grammar Sql;

parse
    : statement EOF
    ;

statement
    : createTableStmt
    | createIndexStmt
    | insertStmt
    | updateStmt
    | deleteStmt
    | selectStmt
    ;

// --- DDL ---

createTableStmt
    : K_CREATE K_TABLE table_name=identifier '(' colDef (',' colDef)* ')'
    ;

createIndexStmt
    : K_CREATE K_INDEX index_name=identifier K_ON table_name=identifier '(' column_name=identifier ')'
    ;

colDef
    : col_name=identifier typeName constraint*
    ;

typeName
    : K_INTEGER
    | K_TEXT
    | K_BOOLEAN
    | K_FLOAT
    ;

constraint
    : K_PRIMARY K_KEY
    | K_UNIQUE
    | K_NOT K_NULL
    ;

// --- DML ---

insertStmt
    : K_INSERT K_INTO table_name=identifier ('(' columnList ')')? K_VALUES '(' valueList ')'
    ;

valueList
    : value (',' value)*
    ;

updateStmt
    : K_UPDATE table_name=identifier K_SET assignment (',' assignment)* whereClause?
    ;

assignment
    : column_name=identifier '=' value
    ;

deleteStmt
    : K_DELETE K_FROM table_name=identifier whereClause?
    ;

// --- DQL ---

selectStmt
    : K_SELECT (K_ASTERISK | columnList) K_FROM table_name=identifier joinClause? whereClause? limitClause?
    ;

columnList
    : identifier (',' identifier)*
    ;

joinClause
    : K_JOIN join_table=identifier K_ON condition
    ;

whereClause
    : K_WHERE condition
    ;

limitClause
    : K_LIMIT limit_val=INTEGER_LITERAL
    ;

condition
    : left=identifier operator right=condValue
    ;

condValue
    : value
    | identifier
    ;

operator
    : EQ | GT | LT | GTE | LTE | NEQ
    ;

value
    : INTEGER_LITERAL
    | FLOAT_LITERAL
    | STRING_LITERAL
    | K_TRUE
    | K_FALSE
    | K_NULL
    | PLACEHOLDER
    ;

identifier
    : ID
    ;

// --- Lexer Rules ---

K_CREATE:   'CREATE';
K_TABLE:    'TABLE';
K_INDEX:    'INDEX';
K_ON:       'ON';
K_INTEGER:  'INTEGER';
K_TEXT:     'TEXT';
K_BOOLEAN:  'BOOLEAN';
K_FLOAT:    'FLOAT';
K_PRIMARY:  'PRIMARY';
K_KEY:      'KEY';
K_UNIQUE:   'UNIQUE';
K_NOT:      'NOT';
K_NULL:     'NULL';
K_INSERT:   'INSERT';
K_INTO:     'INTO';
K_VALUES:   'VALUES';
K_UPDATE:   'UPDATE';
K_SET:      'SET';
K_DELETE:   'DELETE';
K_FROM:     'FROM';
K_SELECT:   'SELECT';
K_JOIN:     'JOIN';
K_WHERE:    'WHERE';
K_LIMIT:    'LIMIT';
K_TRUE:     'TRUE';
K_FALSE:    'FALSE';
K_ASTERISK: '*';

EQ:   '=';
GT:   '>';
LT:   '<';
GTE:  '>=';
LTE:  '<=';
NEQ:  '!=';
PLACEHOLDER: '?';

ID: [a-zA-Z_] [a-zA-Z0-9_]*;
INTEGER_LITERAL: [0-9]+;
FLOAT_LITERAL: [0-9]+ '.' [0-9]* | '.' [0-9]+;
STRING_LITERAL: '\'' ~[']* '\'';

WS: [ \t\r\n]+ -> skip;
