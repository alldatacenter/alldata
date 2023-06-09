parser grammar SparkStreamSqlParser;

options { tokenVocab=SparkStreamSqlLexer; }

root
    : sqlStatements? MINUSMINUS? EOF
    ;

sqlStatements
    : (sqlStatement MINUSMINUS? SEMI | emptyStatement)*
    (sqlStatement (MINUSMINUS? SEMI)? | emptyStatement)
    ;

sqlStatement
    : createStreamTable | insertStatement
    | setStatement
    ;

createStreamTable
    : CREATE STREAM TABLE tableName=tableIdentifier
      ('(' columns=colTypeList ')')?
      WITH tableProps=tablePropertyList
    ;

insertStatement
    : INSERT INTO tableName=tableIdentifier
      select = selectExpr
    ;

setStatement
    : SET setKeyExpr EQUAL_SYMBOL valueKeyExpr
    ;

emptyStatement
    : SEMI
    ;

setKeyExpr
    : ID ('.' ID)*
    ;

valueKeyExpr
    : word (word)*
    ;

selectExpr
    : word (word)*
    ;

word
    : ID
    | ('.' ID)
    | STRING_LITERAL | DECIMAL_LITERAL | REAL_LITERAL | booleanValue
    | AS | BY | SET
    | STAR | DIVIDE | MODULE | PLUS | MINUS
    | EQUAL_SYMBOL | GREATER_SYMBOL | LESS_SYMBOL | EXCLAMATION_SYMBOL
    | BIT_NOT_OP | BIT_OR_OP | BIT_AND_OP | BIT_XOR_OP
    | LR_BRACKET | RR_BRACKET | COMMA | TABLE
    ;

colTypeList
    : colType (',' colType)*
    ;

colType
    : ID (jsonPath=STRING_LITERAL)? dataType (PATTERN pattern=STRING_LITERAL)? (COMMENT comment=STRING_LITERAL)?
    ;

dataType
    : STRING | BOOLEAN | INT | BIGINT
    | FLOAT | DOUBLE
    | DATE  | TIMESTAMP
    ;

tablePropertyList
    : '(' tableProperty (',' tableProperty)* ')'
    ;

tableProperty
    : key=tablePropertyKey EQUAL_SYMBOL value=tablePropertyValue
    ;

tablePropertyKey
    : ID ('.' ID)*
    | STRING_LITERAL
    ;

tablePropertyValue
    : DECIMAL_LITERAL
    | booleanValue
    | STRING_LITERAL
    ;

booleanValue
    : TRUE | FALSE
    ;

tableIdentifier
    : (db=identifier '.')? table=identifier
    ;

identifier
    : ID
    ;
