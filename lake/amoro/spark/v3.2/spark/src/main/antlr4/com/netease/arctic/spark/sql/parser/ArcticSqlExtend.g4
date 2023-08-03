/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This file is an adaptation of Presto's presto-parser/src/main/antlr4/com/facebook/presto/sql/parser/SqlBase.g4 grammar.
 */

grammar ArcticSqlExtend;

@parser::members {
  /**
   * When false, INTERSECT is given the greater precedence over the other set
   * operations (UNION, EXCEPT and MINUS) as per the SQL standard.
   */
  public boolean legacy_setops_precedence_enabled = false;

  /**
   * When false, a literal with an exponent would be converted into
   * double type rather than decimal type.
   */
  public boolean legacy_exponent_literal_as_decimal_enabled = false;

  /**
   * When true, the behavior of keywords follows ANSI SQL standard.
   */
  public boolean SQL_standard_keyword_behavior = false;
}

@lexer::members {
  /**
   * When true, parser should throw ParseExcetion for unclosed bracketed comment.
   */
  public boolean has_unclosed_bracketed_comment = false;

  /**
   * Verify whether current token is a valid decimal token (which contains dot).
   * Returns true if the character that follows the token is not a digit or letter or underscore.
   *
   * For example:
   * For char stream "2.3", "2." is not a valid decimal token, because it is followed by digit '3'.
   * For char stream "2.3_", "2.3" is not a valid decimal token, because it is followed by '_'.
   * For char stream "2.3W", "2.3" is not a valid decimal token, because it is followed by 'W'.
   * For char stream "12.0D 34.E2+0.12 "  12.0D is a valid decimal token because it is followed
   * by a space. 34.E2 is a valid decimal token because it is followed by symbol '+'
   * which is not a digit or letter or underscore.
   */
  public boolean isValidDecimal() {
    int nextChar = _input.LA(1);
    if (nextChar >= 'A' && nextChar <= 'Z' || nextChar >= '0' && nextChar <= '9' ||
      nextChar == '_') {
      return false;
    } else {
      return true;
    }
  }

  /**
   * This method will be called when we see '/*' and try to match it as a bracketed comment.
   * If the next character is '+', it should be parsed as hint later, and we cannot match
   * it as a bracketed comment.
   *
   * Returns true if the next character is '+'.
   */
  public boolean isHint() {
    int nextChar = _input.LA(1);
    if (nextChar == '+') {
      return true;
    } else {
      return false;
    }
  }

  /**
   * This method will be called when the character stream ends and try to find out the
   * unclosed bracketed comment.
   * If the method be called, it means the end of the entire character stream match,
   * and we set the flag and fail later.
   */
  public void markUnclosedComment() {
    has_unclosed_bracketed_comment = true;
  }
}


extendStatement
    : statement ';'* EOF
    ;

statement
    : createTableHeader colListAndPk tableProvider?
        createTableClauses
        (AS? query)?                                                   #createTableWithPk
    | EXPLAIN (LOGICAL | FORMATTED | EXTENDED | CODEGEN | COST)?
        statement                                                      #explain
    ;

createTableHeader
    : CREATE TEMPORARY? EXTERNAL? TABLE (IF NOT EXISTS)? multipartIdentifier
    ;

colListAndPk
    :'(' colTypeList (',' primarySpec )? ')'                            #colListWithPk
    | primarySpec                                                       #colListOnlyPk
    ;

primarySpec
    : PRIMARY KEY identifierList
    ;

bucketSpec
    : CLUSTERED BY identifierList
      (SORTED BY orderedIdentifierList)?
      INTO INTEGER_VALUE BUCKETS
    ;

skewSpec
    : SKEWED BY identifierList
      ON (constantList | nestedConstantList)
      (STORED AS DIRECTORIES)?
    ;

locationSpec
    : LOCATION STRING
    ;

commentSpec
    : COMMENT STRING
    ;

query
    : ctes? queryTerm queryOrganization
    ;


ctes
    : WITH namedQuery (',' namedQuery)*
    ;

namedQuery
    : name=errorCapturingIdentifier (columnAliases=identifierList)? AS? '(' query ')'
    ;

tableProvider
    : USING multipartIdentifier
    ;

createTableClauses
    :((OPTIONS options=tablePropertyList) |
     (PARTITIONED BY partitioning=partitionFieldList) |
     skewSpec |
     bucketSpec |
     rowFormat |
     createFileFormat |
     locationSpec |
     commentSpec |
     (TBLPROPERTIES tableProps=tablePropertyList))*
    ;

tablePropertyList
    : '(' tableProperty (',' tableProperty)* ')'
    ;

tableProperty
    : key=tablePropertyKey (EQ? value=tablePropertyValue)?
    ;

tablePropertyKey
    : identifier ('.' identifier)*
    | STRING
    ;

tablePropertyValue
    : INTEGER_VALUE
    | DECIMAL_VALUE
    | booleanValue
    | STRING
    ;

constantList
    : '(' constant (',' constant)* ')'
    ;

nestedConstantList
    : '(' constantList (',' constantList)* ')'
    ;

createFileFormat
    : STORED AS fileFormat
    | STORED BY storageHandler
    ;

fileFormat
    : INPUTFORMAT inFmt=STRING OUTPUTFORMAT outFmt=STRING    #tableFileFormat
    | identifier                                             #genericFileFormat
    ;

storageHandler
    : STRING (WITH SERDEPROPERTIES tablePropertyList)?
    ;



queryOrganization
    : (ORDER BY order+=sortItem (',' order+=sortItem)*)?
      (CLUSTER BY clusterBy+=expression (',' clusterBy+=expression)*)?
      (DISTRIBUTE BY distributeBy+=expression (',' distributeBy+=expression)*)?
      (SORT BY sort+=sortItem (',' sort+=sortItem)*)?
      windowClause?
      (LIMIT (ALL | limit=expression))?
    ;



queryTerm
    : queryPrimary                                                                       #queryTermDefault
    | left=queryTerm {legacy_setops_precedence_enabled}?
        operator=(INTERSECT | UNION | EXCEPT | SETMINUS) setQuantifier? right=queryTerm  #setOperation
    | left=queryTerm {!legacy_setops_precedence_enabled}?
        operator=INTERSECT setQuantifier? right=queryTerm                                #setOperation
    | left=queryTerm {!legacy_setops_precedence_enabled}?
        operator=(UNION | EXCEPT | SETMINUS) setQuantifier? right=queryTerm              #setOperation
    ;

queryPrimary
    : querySpecification                                                    #queryPrimaryDefault
    | fromStatement                                                         #fromStmt
    | TABLE multipartIdentifier                                             #table
    | inlineTable                                                           #inlineTableDefault1
    | '(' query ')'                                                         #subquery
    ;

sortItem
    : expression ordering=(ASC | DESC)? (NULLS nullOrder=(LAST | FIRST))?
    ;

fromStatement
    : fromClause fromStatementBody+
    ;

fromStatementBody
    : transformClause
      whereClause?
      queryOrganization
    | selectClause
      lateralView*
      whereClause?
      aggregationClause?
      havingClause?
      windowClause?
      queryOrganization
    ;

querySpecification
    : transformClause
      fromClause?
      lateralView*
      whereClause?
      aggregationClause?
      havingClause?
      windowClause?                                                         #transformQuerySpecification
    | selectClause
      fromClause?
      lateralView*
      whereClause?
      aggregationClause?
      havingClause?
      windowClause?                                                         #regularQuerySpecification
    ;

transformClause
    : (SELECT kind=TRANSFORM '(' setQuantifier? expressionSeq ')'
            | kind=MAP setQuantifier? expressionSeq
            | kind=REDUCE setQuantifier? expressionSeq)
      inRowFormat=rowFormat?
      (RECORDWRITER recordWriter=STRING)?
      USING script=STRING
      (AS (identifierSeq | colTypeList | ('(' (identifierSeq | colTypeList) ')')))?
      outRowFormat=rowFormat?
      (RECORDREADER recordReader=STRING)?
    ;

selectClause
    : SELECT (hints+=hint)* setQuantifier? namedExpressionSeq
    ;



whereClause
    : WHERE booleanExpression
    ;

havingClause
    : HAVING booleanExpression
    ;

hint
    : '/*+' hintStatements+=hintStatement (','? hintStatements+=hintStatement)* '*/'
    ;

hintStatement
    : hintName=identifier
    | hintName=identifier '(' parameters+=primaryExpression (',' parameters+=primaryExpression)* ')'
    ;

fromClause
    : FROM relation (',' relation)* lateralView* pivotClause?
    ;

aggregationClause
    : GROUP BY groupingExpressionsWithGroupingAnalytics+=groupByClause
        (',' groupingExpressionsWithGroupingAnalytics+=groupByClause)*
    | GROUP BY groupingExpressions+=expression (',' groupingExpressions+=expression)* (
      WITH kind=ROLLUP
    | WITH kind=CUBE
    | kind=GROUPING SETS '(' groupingSet (',' groupingSet)* ')')?
    ;

groupByClause
    : groupingAnalytics
    | expression
    ;

groupingAnalytics
    : (ROLLUP | CUBE) '(' groupingSet (',' groupingSet)* ')'
    | GROUPING SETS '(' groupingElement (',' groupingElement)* ')'
    ;

groupingElement
    : groupingAnalytics
    | groupingSet
    ;

groupingSet
    : '(' (expression (',' expression)*)? ')'
    | expression
    ;

pivotClause
    : PIVOT '(' aggregates=namedExpressionSeq FOR pivotColumn IN '(' pivotValues+=pivotValue (',' pivotValues+=pivotValue)* ')' ')'
    ;

pivotColumn
    : identifiers+=identifier
    | '(' identifiers+=identifier (',' identifiers+=identifier)* ')'
    ;

pivotValue
    : expression (AS? identifier)?
    ;

lateralView
    : LATERAL VIEW (OUTER)? qualifiedName '(' (expression (',' expression)*)? ')' tblName=identifier (AS? colName+=identifier (',' colName+=identifier)*)?
    ;

setQuantifier
    : DISTINCT
    | ALL
    ;

relation
    : LATERAL? relationPrimary joinRelation*
    ;

joinRelation
    : (joinType) JOIN LATERAL? right=relationPrimary joinCriteria?
    | NATURAL joinType JOIN LATERAL? right=relationPrimary
    ;

joinType
    : INNER?
    | CROSS
    | LEFT OUTER?
    | LEFT? SEMI
    | RIGHT OUTER?
    | FULL OUTER?
    | LEFT? ANTI
    ;

joinCriteria
    : ON booleanExpression
    | USING identifierList
    ;

sample
    : TABLESAMPLE '(' sampleMethod? ')'
    ;

sampleMethod
    : negativeSign=MINUS? percentage=(INTEGER_VALUE | DECIMAL_VALUE) PERCENTLIT   #sampleByPercentile
    | expression ROWS                                                             #sampleByRows
    | sampleType=BUCKET numerator=INTEGER_VALUE OUT OF denominator=INTEGER_VALUE
        (ON (identifier | qualifiedName '(' ')'))?                                #sampleByBucket
    | bytes=expression                                                            #sampleByBytes
    ;

identifierList
    : '(' identifierSeq ')'
    ;

identifierSeq
    : ident+=errorCapturingIdentifier (',' ident+=errorCapturingIdentifier)*
    ;

orderedIdentifierList
    : '(' orderedIdentifier (',' orderedIdentifier)* ')'
    ;

orderedIdentifier
    : ident=errorCapturingIdentifier ordering=(ASC | DESC)?
    ;




relationPrimary
    : multipartIdentifier sample? tableAlias  #tableName
    | '(' query ')' sample? tableAlias        #aliasedQuery
    | '(' relation ')' sample? tableAlias     #aliasedRelation
    | inlineTable                             #inlineTableDefault2
    | functionTable                           #tableValuedFunction
    ;

inlineTable
    : VALUES expression (',' expression)* tableAlias
    ;

functionTable
    : funcName=functionName '(' (expression (',' expression)*)? ')' tableAlias
    ;

tableAlias
    : (AS? strictIdentifier identifierList?)?
    ;

rowFormat
    : ROW FORMAT SERDE name=STRING (WITH SERDEPROPERTIES props=tablePropertyList)?  #rowFormatSerde
    | ROW FORMAT DELIMITED
      (FIELDS TERMINATED BY fieldsTerminatedBy=STRING (ESCAPED BY escapedBy=STRING)?)?
      (COLLECTION ITEMS TERMINATED BY collectionItemsTerminatedBy=STRING)?
      (MAP KEYS TERMINATED BY keysTerminatedBy=STRING)?
      (LINES TERMINATED BY linesSeparatedBy=STRING)?
      (NULL DEFINED AS nullDefinedAs=STRING)?                                       #rowFormatDelimited
    ;

multipartIdentifier
    : parts+=errorCapturingIdentifier ('.' parts+=errorCapturingIdentifier)*
    ;

namedExpression
    : expression (AS? (name=errorCapturingIdentifier | identifierList))?
    ;

namedExpressionSeq
    : namedExpression (',' namedExpression)*
    ;

partitionFieldList
    : '(' fields+=partitionField (',' fields+=partitionField)* ')'
    ;

partitionField
    : transform  #partitionTransform
    | colType    #partitionColumn
    ;

transform
    : qualifiedName                                                           #identityTransform
    | transformName=identifier
      '(' argument+=transformArgument (',' argument+=transformArgument)* ')'  #applyTransform
    ;

transformArgument
    : qualifiedName
    | constant
    ;

expression
    : booleanExpression
    ;

expressionSeq
    : expression (',' expression)*
    ;

booleanExpression
    : NOT booleanExpression                                        #logicalNot
    | EXISTS '(' query ')'                                         #exists
    | valueExpression predicate?                                   #predicated
    | left=booleanExpression operator=AND right=booleanExpression  #logicalBinary
    | left=booleanExpression operator=OR right=booleanExpression   #logicalBinary
    ;

predicate
    : NOT? kind=BETWEEN lower=valueExpression AND upper=valueExpression
    | NOT? kind=IN '(' expression (',' expression)* ')'
    | NOT? kind=IN '(' query ')'
    | NOT? kind=RLIKE pattern=valueExpression
    | NOT? kind=LIKE quantifier=(ANY | SOME | ALL) ('('')' | '(' expression (',' expression)* ')')
    | NOT? kind=LIKE pattern=valueExpression (ESCAPE escapeChar=STRING)?
    | IS NOT? kind=NULL
    | IS NOT? kind=(TRUE | FALSE | UNKNOWN)
    | IS NOT? kind=DISTINCT FROM right=valueExpression
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
    : name=(CURRENT_DATE | CURRENT_TIMESTAMP | CURRENT_USER)                                   #currentLike
    | CASE whenClause+ (ELSE elseExpression=expression)? END                                   #searchedCase
    | CASE value=expression whenClause+ (ELSE elseExpression=expression)? END                  #simpleCase
    | name=(CAST | TRY_CAST) '(' expression AS dataType ')'                                    #cast
    | STRUCT '(' (argument+=namedExpression (',' argument+=namedExpression)*)? ')'             #struct
    | FIRST '(' expression (IGNORE NULLS)? ')'                                                 #first
    | LAST '(' expression (IGNORE NULLS)? ')'                                                  #last
    | POSITION '(' substr=valueExpression IN str=valueExpression ')'                           #position
    | constant                                                                                 #constantDefault
    | ASTERISK                                                                                 #star
    | qualifiedName '.' ASTERISK                                                               #star
    | '(' namedExpression (',' namedExpression)+ ')'                                           #rowConstructor
    | '(' query ')'                                                                            #subqueryExpression
    | functionName '(' (setQuantifier? argument+=expression (',' argument+=expression)*)? ')'
       (FILTER '(' WHERE where=booleanExpression ')')?
       (nullsOption=(IGNORE | RESPECT) NULLS)? ( OVER windowSpec)?                             #functionCall
    | identifier '->' expression                                                               #lambda
    | '(' identifier (',' identifier)+ ')' '->' expression                                     #lambda
    | value=primaryExpression '[' index=valueExpression ']'                                    #subscript
    | identifier                                                                               #columnReference
    | base=primaryExpression '.' fieldName=identifier                                          #dereference
    | '(' expression ')'                                                                       #parenthesizedExpression
    | EXTRACT '(' field=identifier FROM source=valueExpression ')'                             #extract
    | (SUBSTR | SUBSTRING) '(' str=valueExpression (FROM | ',') pos=valueExpression
      ((FOR | ',') len=valueExpression)? ')'                                                   #substring
    | TRIM '(' trimOption=(BOTH | LEADING | TRAILING)? (trimStr=valueExpression)?
       FROM srcStr=valueExpression ')'                                                         #trim
    | OVERLAY '(' input=valueExpression PLACING replace=valueExpression
      FROM position=valueExpression (FOR length=valueExpression)? ')'                          #overlay
    ;

constant
    : NULL                                                                                     #nullLiteral
    | interval                                                                                 #intervalLiteral
    | identifier STRING                                                                        #typeConstructor
    | number                                                                                   #numericLiteral
    | booleanValue                                                                             #booleanLiteral
    | STRING+                                                                                  #stringLiteral
    ;

comparisonOperator
    : EQ | NEQ | NEQJ | LT | LTE | GT | GTE | NSEQ
    ;

booleanValue
    : TRUE | FALSE
    ;

interval
    : INTERVAL (errorCapturingMultiUnitsInterval | errorCapturingUnitToUnitInterval)?
    ;

errorCapturingMultiUnitsInterval
    : body=multiUnitsInterval unitToUnitInterval?
    ;

multiUnitsInterval
    : (intervalValue unit+=identifier)+
    ;

errorCapturingUnitToUnitInterval
    : body=unitToUnitInterval (error1=multiUnitsInterval | error2=unitToUnitInterval)?
    ;

unitToUnitInterval
    : value=intervalValue from=identifier TO to=identifier
    ;

intervalValue
    : (PLUS | MINUS)? (INTEGER_VALUE | DECIMAL_VALUE | STRING)
    ;

colPosition
    : position=FIRST | position=AFTER afterCol=errorCapturingIdentifier
    ;

dataType
    : complex=ARRAY '<' dataType '>'                            #complexDataType
    | complex=MAP '<' dataType ',' dataType '>'                 #complexDataType
    | complex=STRUCT ('<' complexColTypeList? '>' | NEQ)        #complexDataType
    | INTERVAL from=(YEAR | MONTH) (TO to=MONTH)?               #yearMonthIntervalDataType
    | INTERVAL from=(DAY | HOUR | MINUTE | SECOND)
      (TO to=(HOUR | MINUTE | SECOND))?                         #dayTimeIntervalDataType
    | identifier ('(' INTEGER_VALUE (',' INTEGER_VALUE)* ')')?  #primitiveDataType
    ;


colTypeList
    : colType (',' colType)*
    ;

colType
    : colName=errorCapturingIdentifier dataType (NOT NULL)? commentSpec?
    ;

complexColTypeList
    : complexColType (',' complexColType)*
    ;

complexColType
    : identifier ':'? dataType (NOT NULL)? commentSpec?
    ;

whenClause
    : WHEN condition=expression THEN result=expression
    ;

windowClause
    : WINDOW namedWindow (',' namedWindow)*
    ;

namedWindow
    : name=errorCapturingIdentifier AS windowSpec
    ;

windowSpec
    : name=errorCapturingIdentifier         #windowRef
    | '('name=errorCapturingIdentifier')'   #windowRef
    | '('
      ( CLUSTER BY partition+=expression (',' partition+=expression)*
      | ((PARTITION | DISTRIBUTE) BY partition+=expression (',' partition+=expression)*)?
        ((ORDER | SORT) BY sortItem (',' sortItem)*)?)
      windowFrame?
      ')'                                   #windowDef
    ;

windowFrame
    : frameType=RANGE start=frameBound
    | frameType=ROWS start=frameBound
    | frameType=RANGE BETWEEN start=frameBound AND end=frameBound
    | frameType=ROWS BETWEEN start=frameBound AND end=frameBound
    ;

frameBound
    : UNBOUNDED boundType=(PRECEDING | FOLLOWING)
    | boundType=CURRENT ROW
    | expression boundType=(PRECEDING | FOLLOWING)
    ;


functionName
    : qualifiedName
    | FILTER
    | LEFT
    | RIGHT
    ;

qualifiedName
    : identifier ('.' identifier)*
    ;

// this rule is used for explicitly capturing wrong identifiers such as test-table, which should actually be `test-table`
// replace identifier with errorCapturingIdentifier where the immediate follow symbol is not an expression, otherwise
// valid expressions such as "a-b" can be recognized as an identifier
errorCapturingIdentifier
    : identifier errorCapturingIdentifierExtra
    ;

// extra left-factoring grammar
errorCapturingIdentifierExtra
    : (MINUS identifier)+    #errorIdent
    |                        #realIdent
    ;

identifier
    : strictIdentifier
    | {!SQL_standard_keyword_behavior}? strictNonReserved
    ;

strictIdentifier
    : IDENTIFIER              #unquotedIdentifier
    | quotedIdentifier        #quotedIdentifierAlternative
    | {SQL_standard_keyword_behavior}? ansiNonReserved #unquotedIdentifier
    | {!SQL_standard_keyword_behavior}? nonReserved    #unquotedIdentifier
    ;

quotedIdentifier
    : BACKQUOTED_IDENTIFIER
    ;

number
    : {!legacy_exponent_literal_as_decimal_enabled}? MINUS? EXPONENT_VALUE #exponentLiteral
    | {!legacy_exponent_literal_as_decimal_enabled}? MINUS? DECIMAL_VALUE  #decimalLiteral
    | {legacy_exponent_literal_as_decimal_enabled}? MINUS? (EXPONENT_VALUE | DECIMAL_VALUE) #legacyDecimalLiteral
    | MINUS? INTEGER_VALUE            #integerLiteral
    | MINUS? BIGINT_LITERAL           #bigIntLiteral
    | MINUS? SMALLINT_LITERAL         #smallIntLiteral
    | MINUS? TINYINT_LITERAL          #tinyIntLiteral
    | MINUS? DOUBLE_LITERAL           #doubleLiteral
    | MINUS? FLOAT_LITERAL            #floatLiteral
    | MINUS? BIGDECIMAL_LITERAL       #bigDecimalLiteral
    ;


// When `SQL_standard_keyword_behavior=true`, there are 2 kinds of keywords in Spark SQL.
// - Reserved keywords:
//     Keywords that are reserved and can't be used as identifiers for table, view, column,
//     function, alias, etc.
// - Non-reserved keywords:
//     Keywords that have a special meaning only in particular contexts and can be used as
//     identifiers in other contexts. For example, `EXPLAIN SELECT ...` is a command, but EXPLAIN
//     can be used as identifiers in other places.
// You can find the full keywords list by searching "Start of the keywords list" in this file.
// The non-reserved keywords are listed below. Keywords not in this list are reserved keywords.
ansiNonReserved
//--ANSI-NON-RESERVED-START
    : ADD
    | AFTER
    | ALTER
    | ANALYZE
    | ANTI
    | ARCHIVE
    | ARRAY
    | ASC
    | AT
    | BETWEEN
    | BUCKET
    | BUCKETS
    | BY
    | CACHE
    | CASCADE
    | CHANGE
    | CLEAR
    | CLUSTER
    | CLUSTERED
    | CODEGEN
    | COLLECTION
    | COLUMNS
    | COMMENT
    | COMMIT
    | COMPACT
    | COMPACTIONS
    | COMPUTE
    | CONCATENATE
    | COST
    | CUBE
    | CURRENT
    | DATA
    | DATABASE
    | DATABASES
    | DAY
    | DBPROPERTIES
    | DEFINED
    | DELETE
    | DELIMITED
    | DESC
    | DESCRIBE
    | DFS
    | DIRECTORIES
    | DIRECTORY
    | DISTRIBUTE
    | DIV
    | DROP
    | ESCAPED
    | EXCHANGE
    | EXISTS
    | EXPLAIN
    | EXPORT
    | EXTENDED
    | EXTERNAL
    | EXTRACT
    | FIELDS
    | FILEFORMAT
    | FIRST
    | FOLLOWING
    | FORMAT
    | FORMATTED
    | FUNCTION
    | FUNCTIONS
    | GLOBAL
    | GROUPING
    | HOUR
    | IF
    | IGNORE
    | IMPORT
    | INDEX
    | INDEXES
    | INPATH
    | INPUTFORMAT
    | INSERT
    | INTERVAL
    | ITEMS
    | KEYS
    | LAST
    | LAZY
    | LIKE
    | LIMIT
    | LINES
    | LIST
    | LOAD
    | LOCAL
    | LOCATION
    | LOCK
    | LOCKS
    | LOGICAL
    | MACRO
    | MAP
    | MATCHED
    | MERGE
    | MINUTE
    | MONTH
    | MSCK
    | NAMESPACE
    | NAMESPACES
    | NO
    | NULLS
    | OF
    | OPTION
    | OPTIONS
    | OUT
    | OUTPUTFORMAT
    | OVER
    | OVERLAY
    | OVERWRITE
    | PARTITION
    | PARTITIONED
    | PARTITIONS
    | PERCENTLIT
    | PIVOT
    | PLACING
    | POSITION
    | PRECEDING
    | PRINCIPALS
    | PROPERTIES
    | PURGE
    | QUERY
    | RANGE
    | RECORDREADER
    | RECORDWRITER
    | RECOVER
    | REDUCE
    | REFRESH
    | RENAME
    | REPAIR
    | REPLACE
    | RESET
    | RESPECT
    | RESTRICT
    | REVOKE
    | RLIKE
    | ROLE
    | ROLES
    | ROLLBACK
    | ROLLUP
    | ROW
    | ROWS
    | SCHEMA
    | SECOND
    | SEMI
    | SEPARATED
    | SERDE
    | SERDEPROPERTIES
    | SET
    | SETMINUS
    | SETS
    | SHOW
    | SKEWED
    | SORT
    | SORTED
    | START
    | STATISTICS
    | STORED
    | STRATIFY
    | STRUCT
    | SUBSTR
    | SUBSTRING
    | SYNC
    | TABLES
    | TABLESAMPLE
    | TBLPROPERTIES
    | TEMPORARY
    | TERMINATED
    | TOUCH
    | TRANSACTION
    | TRANSACTIONS
    | TRANSFORM
    | TRIM
    | TRUE
    | TRUNCATE
    | TRY_CAST
    | TYPE
    | UNARCHIVE
    | UNBOUNDED
    | UNCACHE
    | UNLOCK
    | UNSET
    | UPDATE
    | USE
    | VALUES
    | VIEW
    | VIEWS
    | WINDOW
    | YEAR
    | ZONE
//--ANSI-NON-RESERVED-END
    ;

// When `SQL_standard_keyword_behavior=false`, there are 2 kinds of keywords in Spark SQL.
// - Non-reserved keywords:
//     Same definition as the one when `SQL_standard_keyword_behavior=true`.
// - Strict-non-reserved keywords:
//     A strict version of non-reserved keywords, which can not be used as table alias.
// You can find the full keywords list by searching "Start of the keywords list" in this file.
// The strict-non-reserved keywords are listed in `strictNonReserved`.
// The non-reserved keywords are listed in `nonReserved`.
// These 2 together contain all the keywords.
strictNonReserved
    : ANTI
    | CROSS
    | EXCEPT
    | FULL
    | INNER
    | INTERSECT
    | JOIN
    | LATERAL
    | LEFT
    | NATURAL
    | ON
    | RIGHT
    | SEMI
    | SETMINUS
    | UNION
    | USING
    ;

nonReserved
//--DEFAULT-NON-RESERVED-START
    : ADD
    | AFTER
    | ALL
    | ALTER
    | ANALYZE
    | AND
    | ANY
    | ARCHIVE
    | ARRAY
    | AS
    | ASC
    | AT
    | AUTHORIZATION
    | BETWEEN
    | BOTH
    | BUCKET
    | BUCKETS
    | BY
    | CACHE
    | CASCADE
    | CASE
    | CAST
    | CHANGE
    | CHECK
    | CLEAR
    | CLUSTER
    | CLUSTERED
    | CODEGEN
    | COLLATE
    | COLLECTION
    | COLUMN
    | COLUMNS
    | COMMENT
    | COMMIT
    | COMPACT
    | COMPACTIONS
    | COMPUTE
    | CONCATENATE
    | CONSTRAINT
    | COST
    | CREATE
    | CUBE
    | CURRENT
    | CURRENT_DATE
    | CURRENT_TIME
    | CURRENT_TIMESTAMP
    | CURRENT_USER
    | DATA
    | DATABASE
    | DATABASES
    | DAY
    | DBPROPERTIES
    | DEFINED
    | DELETE
    | DELIMITED
    | DESC
    | DESCRIBE
    | DFS
    | DIRECTORIES
    | DIRECTORY
    | DISTINCT
    | DISTRIBUTE
    | DIV
    | DROP
    | ELSE
    | END
    | ESCAPE
    | ESCAPED
    | EXCHANGE
    | EXISTS
    | EXPLAIN
    | EXPORT
    | EXTENDED
    | EXTERNAL
    | EXTRACT
    | FALSE
    | FETCH
    | FILTER
    | FIELDS
    | FILEFORMAT
    | FIRST
    | FOLLOWING
    | FOR
    | FOREIGN
    | FORMAT
    | FORMATTED
    | FROM
    | FUNCTION
    | FUNCTIONS
    | GLOBAL
    | GRANT
    | GROUP
    | GROUPING
    | HAVING
    | HOUR
    | IF
    | IGNORE
    | IMPORT
    | IN
    | INDEX
    | INDEXES
    | INPATH
    | INPUTFORMAT
    | INSERT
    | INTERVAL
    | INTO
    | IS
    | ITEMS
    | KEYS
    | LAST
    | LAZY
    | LEADING
    | LIKE
    | LIMIT
    | LINES
    | LIST
    | LOAD
    | LOCAL
    | LOCATION
    | LOCK
    | LOCKS
    | LOGICAL
    | MACRO
    | MAP
    | MATCHED
    | MERGE
    | MINUTE
    | MONTH
    | MSCK
    | NAMESPACE
    | NAMESPACES
    | NO
    | NOT
    | NULL
    | NULLS
    | OF
    | ONLY
    | OPTION
    | OPTIONS
    | OR
    | ORDER
    | OUT
    | OUTER
    | OUTPUTFORMAT
    | OVER
    | OVERLAPS
    | OVERLAY
    | OVERWRITE
    | PARTITION
    | PARTITIONED
    | PARTITIONS
    | PERCENTLIT
    | PIVOT
    | PLACING
    | POSITION
    | PRECEDING
    | PRIMARY
    | PRINCIPALS
    | PROPERTIES
    | PURGE
    | QUERY
    | RANGE
    | RECORDREADER
    | RECORDWRITER
    | RECOVER
    | REDUCE
    | REFERENCES
    | REFRESH
    | RENAME
    | REPAIR
    | REPLACE
    | RESET
    | RESPECT
    | RESTRICT
    | REVOKE
    | RLIKE
    | ROLE
    | ROLES
    | ROLLBACK
    | ROLLUP
    | ROW
    | ROWS
    | SCHEMA
    | SECOND
    | SELECT
    | SEPARATED
    | SERDE
    | SERDEPROPERTIES
    | SESSION_USER
    | SET
    | SETS
    | SHOW
    | SKEWED
    | SOME
    | SORT
    | SORTED
    | START
    | STATISTICS
    | STORED
    | STRATIFY
    | STRUCT
    | SUBSTR
    | SUBSTRING
    | SYNC
    | TABLE
    | TABLES
    | TABLESAMPLE
    | TBLPROPERTIES
    | TEMPORARY
    | TERMINATED
    | THEN
    | TIME
    | TO
    | TOUCH
    | TRAILING
    | TRANSACTION
    | TRANSACTIONS
    | TRANSFORM
    | TRIM
    | TRUE
    | TRUNCATE
    | TRY_CAST
    | TYPE
    | UNARCHIVE
    | UNBOUNDED
    | UNCACHE
    | UNIQUE
    | UNKNOWN
    | UNLOCK
    | UNSET
    | UPDATE
    | USE
    | USER
    | VALUES
    | VIEW
    | VIEWS
    | WHEN
    | WHERE
    | WINDOW
    | WITH
    | YEAR
    | ZONE
//--DEFAULT-NON-RESERVED-END
    ;

// NOTE: If you add a new token in the list below, you should update the list of keywords
// and reserved tag in `docs/sql-ref-ansi-compliance.md#sql-keywords`.

//============================
// Start of the keywords list
//============================
//--SPARK-KEYWORD-LIST-START
ADD: 'ADD';
AFTER: 'AFTER';
ALL: 'ALL';
ALTER: 'ALTER';
ANALYZE: 'ANALYZE';
AND: 'AND';
ANTI: 'ANTI';
ANY: 'ANY';
ARCHIVE: 'ARCHIVE';
ARRAY: 'ARRAY';
AS: 'AS';
ASC: 'ASC';
AT: 'AT';
AUTHORIZATION: 'AUTHORIZATION';
BETWEEN: 'BETWEEN';
BOTH: 'BOTH';
BUCKET: 'BUCKET';
BUCKETS: 'BUCKETS';
BY: 'BY';
CACHE: 'CACHE';
CASCADE: 'CASCADE';
CASE: 'CASE';
CAST: 'CAST';
CHANGE: 'CHANGE';
CHECK: 'CHECK';
CLEAR: 'CLEAR';
CLUSTER: 'CLUSTER';
CLUSTERED: 'CLUSTERED';
CODEGEN: 'CODEGEN';
COLLATE: 'COLLATE';
COLLECTION: 'COLLECTION';
COLUMN: 'COLUMN';
COLUMNS: 'COLUMNS';
COMMENT: 'COMMENT';
COMMIT: 'COMMIT';
COMPACT: 'COMPACT';
COMPACTIONS: 'COMPACTIONS';
COMPUTE: 'COMPUTE';
CONCATENATE: 'CONCATENATE';
CONSTRAINT: 'CONSTRAINT';
COST: 'COST';
CREATE: 'CREATE';
CROSS: 'CROSS';
CUBE: 'CUBE';
CURRENT: 'CURRENT';
CURRENT_DATE: 'CURRENT_DATE';
CURRENT_TIME: 'CURRENT_TIME';
CURRENT_TIMESTAMP: 'CURRENT_TIMESTAMP';
CURRENT_USER: 'CURRENT_USER';
DAY: 'DAY';
DATA: 'DATA';
DATABASE: 'DATABASE';
DATABASES: 'DATABASES' | 'SCHEMAS';
DBPROPERTIES: 'DBPROPERTIES';
DEFINED: 'DEFINED';
DELETE: 'DELETE';
DELIMITED: 'DELIMITED';
DESC: 'DESC';
DESCRIBE: 'DESCRIBE';
DFS: 'DFS';
DIRECTORIES: 'DIRECTORIES';
DIRECTORY: 'DIRECTORY';
DISTINCT: 'DISTINCT';
DISTRIBUTE: 'DISTRIBUTE';
DIV: 'DIV';
DROP: 'DROP';
ELSE: 'ELSE';
END: 'END';
ESCAPE: 'ESCAPE';
ESCAPED: 'ESCAPED';
EXCEPT: 'EXCEPT';
EXCHANGE: 'EXCHANGE';
EXISTS: 'EXISTS';
EXPLAIN: 'EXPLAIN';
EXPORT: 'EXPORT';
EXTENDED: 'EXTENDED';
EXTERNAL: 'EXTERNAL';
EXTRACT: 'EXTRACT';
FALSE: 'FALSE';
FETCH: 'FETCH';
FIELDS: 'FIELDS';
FILTER: 'FILTER';
FILEFORMAT: 'FILEFORMAT';
FIRST: 'FIRST';
FOLLOWING: 'FOLLOWING';
FOR: 'FOR';
FOREIGN: 'FOREIGN';
FORMAT: 'FORMAT';
FORMATTED: 'FORMATTED';
FROM: 'FROM';
FULL: 'FULL';
FUNCTION: 'FUNCTION';
FUNCTIONS: 'FUNCTIONS';
GLOBAL: 'GLOBAL';
GRANT: 'GRANT';
GROUP: 'GROUP';
GROUPING: 'GROUPING';
HAVING: 'HAVING';
HOUR: 'HOUR';
IF: 'IF';
IGNORE: 'IGNORE';
IMPORT: 'IMPORT';
IN: 'IN';
INDEX: 'INDEX';
INDEXES: 'INDEXES';
INNER: 'INNER';
INPATH: 'INPATH';
INPUTFORMAT: 'INPUTFORMAT';
INSERT: 'INSERT';
INTERSECT: 'INTERSECT';
INTERVAL: 'INTERVAL';
INTO: 'INTO';
IS: 'IS';
ITEMS: 'ITEMS';
JOIN: 'JOIN';
KEYS: 'KEYS';
LAST: 'LAST';
LATERAL: 'LATERAL';
LAZY: 'LAZY';
LEADING: 'LEADING';
LEFT: 'LEFT';
LIKE: 'LIKE';
LIMIT: 'LIMIT';
LINES: 'LINES';
LIST: 'LIST';
LOAD: 'LOAD';
LOCAL: 'LOCAL';
LOCATION: 'LOCATION';
LOCK: 'LOCK';
LOCKS: 'LOCKS';
LOGICAL: 'LOGICAL';
MACRO: 'MACRO';
MAP: 'MAP';
MATCHED: 'MATCHED';
MERGE: 'MERGE';
MINUTE: 'MINUTE';
MONTH: 'MONTH';
MSCK: 'MSCK';
NAMESPACE: 'NAMESPACE';
NAMESPACES: 'NAMESPACES';
NATURAL: 'NATURAL';
NO: 'NO';
NOT: 'NOT' | '!';
NULL: 'NULL';
NULLS: 'NULLS';
OF: 'OF';
ON: 'ON';
ONLY: 'ONLY';
OPTION: 'OPTION';
OPTIONS: 'OPTIONS';
OR: 'OR';
ORDER: 'ORDER';
OUT: 'OUT';
OUTER: 'OUTER';
OUTPUTFORMAT: 'OUTPUTFORMAT';
OVER: 'OVER';
OVERLAPS: 'OVERLAPS';
OVERLAY: 'OVERLAY';
OVERWRITE: 'OVERWRITE';
PARTITION: 'PARTITION';
PARTITIONED: 'PARTITIONED';
PARTITIONS: 'PARTITIONS';
PERCENTLIT: 'PERCENT';
PIVOT: 'PIVOT';
PLACING: 'PLACING';
POSITION: 'POSITION';
PRECEDING: 'PRECEDING';
PRIMARY: 'PRIMARY';
PRINCIPALS: 'PRINCIPALS';
PROPERTIES: 'PROPERTIES';
PURGE: 'PURGE';
QUERY: 'QUERY';
RANGE: 'RANGE';
RECORDREADER: 'RECORDREADER';
RECORDWRITER: 'RECORDWRITER';
RECOVER: 'RECOVER';
REDUCE: 'REDUCE';
REFERENCES: 'REFERENCES';
REFRESH: 'REFRESH';
RENAME: 'RENAME';
REPAIR: 'REPAIR';
REPLACE: 'REPLACE';
RESET: 'RESET';
RESPECT: 'RESPECT';
RESTRICT: 'RESTRICT';
REVOKE: 'REVOKE';
RIGHT: 'RIGHT';
RLIKE: 'RLIKE' | 'REGEXP';
ROLE: 'ROLE';
ROLES: 'ROLES';
ROLLBACK: 'ROLLBACK';
ROLLUP: 'ROLLUP';
ROW: 'ROW';
ROWS: 'ROWS';
SECOND: 'SECOND';
SCHEMA: 'SCHEMA';
SELECT: 'SELECT';
SEMI: 'SEMI';
SEPARATED: 'SEPARATED';
SERDE: 'SERDE';
SERDEPROPERTIES: 'SERDEPROPERTIES';
SESSION_USER: 'SESSION_USER';
SET: 'SET';
SETMINUS: 'MINUS';
SETS: 'SETS';
SHOW: 'SHOW';
SKEWED: 'SKEWED';
SOME: 'SOME';
SORT: 'SORT';
SORTED: 'SORTED';
START: 'START';
STATISTICS: 'STATISTICS';
STORED: 'STORED';
STRATIFY: 'STRATIFY';
STRUCT: 'STRUCT';
SUBSTR: 'SUBSTR';
SUBSTRING: 'SUBSTRING';
SYNC: 'SYNC';
TABLE: 'TABLE';
TABLES: 'TABLES';
TABLESAMPLE: 'TABLESAMPLE';
TBLPROPERTIES: 'TBLPROPERTIES';
TEMPORARY: 'TEMPORARY' | 'TEMP';
TERMINATED: 'TERMINATED';
THEN: 'THEN';
TIME: 'TIME';
TO: 'TO';
TOUCH: 'TOUCH';
TRAILING: 'TRAILING';
TRANSACTION: 'TRANSACTION';
TRANSACTIONS: 'TRANSACTIONS';
TRANSFORM: 'TRANSFORM';
TRIM: 'TRIM';
TRUE: 'TRUE';
TRUNCATE: 'TRUNCATE';
TRY_CAST: 'TRY_CAST';
TYPE: 'TYPE';
UNARCHIVE: 'UNARCHIVE';
UNBOUNDED: 'UNBOUNDED';
UNCACHE: 'UNCACHE';
UNION: 'UNION';
UNIQUE: 'UNIQUE';
UNKNOWN: 'UNKNOWN';
UNLOCK: 'UNLOCK';
UNSET: 'UNSET';
UPDATE: 'UPDATE';
USE: 'USE';
USER: 'USER';
USING: 'USING';
VALUES: 'VALUES';
VIEW: 'VIEW';
VIEWS: 'VIEWS';
WHEN: 'WHEN';
WHERE: 'WHERE';
WINDOW: 'WINDOW';
WITH: 'WITH';
YEAR: 'YEAR';
ZONE: 'ZONE';
KEY: 'KEY';
//--SPARK-KEYWORD-LIST-END
//============================
// End of the keywords list
//============================

EQ  : '=' | '==';
NSEQ: '<=>';
NEQ : '<>';
NEQJ: '!=';
LT  : '<';
LTE : '<=' | '!>';
GT  : '>';
GTE : '>=' | '!<';

PLUS: '+';
MINUS: '-';
ASTERISK: '*';
SLASH: '/';
PERCENT: '%';
TILDE: '~';
AMPERSAND: '&';
PIPE: '|';
CONCAT_PIPE: '||';
HAT: '^';

STRING
    : '\'' ( ~('\''|'\\') | ('\\' .) )* '\''
    | '"' ( ~('"'|'\\') | ('\\' .) )* '"'
    ;

BIGINT_LITERAL
    : DIGIT+ 'L'
    ;

SMALLINT_LITERAL
    : DIGIT+ 'S'
    ;

TINYINT_LITERAL
    : DIGIT+ 'Y'
    ;

INTEGER_VALUE
    : DIGIT+
    ;

EXPONENT_VALUE
    : DIGIT+ EXPONENT
    | DECIMAL_DIGITS EXPONENT {isValidDecimal()}?
    ;

DECIMAL_VALUE
    : DECIMAL_DIGITS {isValidDecimal()}?
    ;

FLOAT_LITERAL
    : DIGIT+ EXPONENT? 'F'
    | DECIMAL_DIGITS EXPONENT? 'F' {isValidDecimal()}?
    ;

DOUBLE_LITERAL
    : DIGIT+ EXPONENT? 'D'
    | DECIMAL_DIGITS EXPONENT? 'D' {isValidDecimal()}?
    ;

BIGDECIMAL_LITERAL
    : DIGIT+ EXPONENT? 'BD'
    | DECIMAL_DIGITS EXPONENT? 'BD' {isValidDecimal()}?
    ;

IDENTIFIER
    : (LETTER | DIGIT | '_')+
    ;

BACKQUOTED_IDENTIFIER
    : '`' ( ~'`' | '``' )* '`'
    ;

fragment DECIMAL_DIGITS
    : DIGIT+ '.' DIGIT*
    | '.' DIGIT+
    ;

fragment EXPONENT
    : 'E' [+-]? DIGIT+
    ;

fragment DIGIT
    : [0-9]
    ;

fragment LETTER
    : [A-Z]
    ;

SIMPLE_COMMENT
    : '--' ('\\\n' | ~[\r\n])* '\r'? '\n'? -> channel(HIDDEN)
    ;

BRACKETED_COMMENT
    : '/*' {!isHint()}? ( BRACKETED_COMMENT | . )*? ('*/' | {markUnclosedComment();} EOF) -> channel(HIDDEN)
    ;

WS
    : [ \r\n\t]+ -> channel(HIDDEN)
    ;

// Catch-all for anything we can't recognize.
// We use this to be able to ignore and recover all the text
// when splitting statements with DelimiterLexer
UNRECOGNIZED
    : .
    ;
