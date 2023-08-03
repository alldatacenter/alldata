parser grammar AppJarParser;

options { tokenVocab=AppJarLexer; }

rootx
    : jobTasks? EOF
    ;

jobTasks
    : (jobTask SEMI | emptyStatement)*
    (jobTask (SEMI)? | emptyStatement)
    ;

jobTask
    :  setStatement | unsetStatement | jobStatement
    ;

jobStatement
    : resourceNameExpr classNameExpr paramsExpr?
    ;

resourceNameExpr
    : ID (DOT_ID)*
    | fileDir
    ;

classNameExpr
    : ID (DOT_ID)*
    ;

paramsExpr
    : paramExpr (paramExpr)*
    ;

paramExpr
    : ID (DOT_ID)*
    | fileDir
    | ID DOT_ID* ('/' '/'? ID DOT_ID*)*
    | STRING_LITERAL
    ;

fileDir
    : '/' ID DOT_ID* ('/' (ID DOT_ID* | '*'))* '/'?
    ;

setStatement
    : SET keyExpr EQUAL_SYMBOL value=valueExpr
    ;

unsetStatement
    : UNSET keyExpr
    ;

keyExpr
    : ID (DOT_ID)*
    ;

valueExpr
    : word (word)*
    ;

word
    : ID
    | DOT_ID | SET | UNSET
    | STAR | DIVIDE | MODULE | PLUS | MINUS
    | EQUAL_SYMBOL | GREATER_SYMBOL | LESS_SYMBOL | EXCLAMATION_SYMBOL
    | BIT_NOT_OP | BIT_OR_OP | BIT_AND_OP | BIT_XOR_OP
    | LR_BRACKET | RR_BRACKET | COMMA
    | STRING_LITERAL
    ;

emptyStatement
    : SEMI
    ;
