parser grammar FlinkCdcSqlParser;

options { tokenVocab = FlinkCdcSqlLexer; }

singleStatement
    : statement SEMICOLON* EOF
    ;

statement
    : BEGIN STATEMENT SET                                            #beginStatement
    | END                                                            #endStatement
    | CREATE TABLE IF NOT EXISTS sink=multipartIdentifier
        (PARTITIONED BY identifierList)?
        commentSpec?
        (WITH sinkOptions=propertyList)?
        AS TABLE source=multipartIdentifier
        (OPTIONS sourceOptions=propertyList)?
        (ADD COLUMN LEFT_PAREN computeColList RIGHT_PAREN)?           #createTable
    | CREATE DATABASE IF NOT EXISTS sink=multipartIdentifier
        (PARTITIONED BY identifierList)?
        commentSpec?
        (WITH sinkOptions=propertyList)?
        AS (DATABASE | KAFKA) source=multipartIdentifier
        (INCLUDING (ALL TABLES | TABLE includeTable=STRING))?
        (EXCLUDING TABLE excludeTable=STRING)?
        (OPTIONS sourceOptions=propertyList)?                         #createDatabase
    ;

multipartIdentifier
    : parts+=identifier (DOT parts+=identifier)*
    ;

identifierList
    : LEFT_PAREN identifierSeq RIGHT_PAREN
    ;

identifierSeq
    : ident+=identifier (COMMA ident+=identifier)*
    ;

stringLit
    : STRING
    ;

commentSpec
    : COMMENT STRING
    ;

propertyList
    : LEFT_PAREN property (COMMA property)* RIGHT_PAREN
    ;

property
    : key=propertyKey (EQ? value=propertyValue)?
    ;

propertyKey
    : identifier (DOT identifier)*
    | STRING
    ;

propertyValue
    : INTEGER_VALUE
    | DECIMAL_VALUE
    | booleanValue
    | STRING
    ;

computeColList
    : computeColDef (COMMA computeColDef)*
    ;

computeColDef
    : colName=identifier AS expression (FIRST | AFTER name=identifier)?
    ;

expression
    : valueExpression
    ;

valueExpression
    : primaryExpression                                                                      #valueExpressionDefault
    | operator=(MINUS | PLUS | TILDE) valueExpression                                        #arithmeticUnary
    | left=valueExpression operator=(ASTERISK | SLASH | PERCENT | DIV) right=valueExpression #arithmeticBinary
    | left=valueExpression operator=(PLUS | MINUS | CONCAT_PIPE) right=valueExpression       #arithmeticBinary
    | left=valueExpression operator=AMPERSAND right=valueExpression                          #arithmeticBinary
    | left=valueExpression operator=HAT right=valueExpression                                #arithmeticBinary
    | left=valueExpression operator=PIPE right=valueExpression                               #arithmeticBinary
    | left=valueExpression comparisonOperator right=valueExpression                          #comparison
    ;

primaryExpression
    : identifier                                                                             #columnReference
    | constant                                                                               #constantDefault
    | name=CAST LEFT_PAREN expression AS identifier RIGHT_PAREN                              #cast
    | functionName LEFT_PAREN (setQuantifier? argument+=expression
        (COMMA argument+=expression)*)? RIGHT_PAREN                                          #functionCall
    ;

functionName
    : identifier (DOT identifier)*
    ;

setQuantifier
    : DISTINCT
    | ALL
    ;

identifier
    : IDENTIFIER              #unquotedIdentifier
    | quotedIdentifier        #quotedIdentifierAlternative
    ;

quotedIdentifier
    : BACKQUOTED_IDENTIFIER
    ;

constant
    : NULL                                                                                     #nullLiteral
    | COLON identifier                                                                         #parameterLiteral
    //| interval                                                                                 #intervalLiteral
    | identifier stringLit                                                                     #typeConstructor
    | number                                                                                   #numericLiteral
    | booleanValue                                                                             #booleanLiteral
    | stringLit+                                                                               #stringLiteral
    ;

number
    : MINUS? (EXPONENT_VALUE | DECIMAL_VALUE) #legacyDecimalLiteral
    | MINUS? INTEGER_VALUE            #integerLiteral
    | MINUS? BIGINT_LITERAL           #bigIntLiteral
    | MINUS? SMALLINT_LITERAL         #smallIntLiteral
    | MINUS? TINYINT_LITERAL          #tinyIntLiteral
    | MINUS? DOUBLE_LITERAL           #doubleLiteral
    | MINUS? FLOAT_LITERAL            #floatLiteral
    | MINUS? BIGDECIMAL_LITERAL       #bigDecimalLiteral
    ;

comparisonOperator
    : EQ | NEQ | NEQJ | LT | LTE | GT | GTE | NSEQ
    ;

arithmeticOperator
    : PLUS | MINUS | ASTERISK | SLASH | PERCENT | DIV | TILDE | AMPERSAND | PIPE | CONCAT_PIPE | HAT
    ;

booleanValue
    : TRUE | FALSE
    ;
