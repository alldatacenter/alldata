lexer grammar SchemaLexer;

@header {
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
}

// data types
// https://drill.apache.org/docs/supported-data-types/
INT: 'INT';
INTEGER: 'INTEGER';
BIGINT: 'BIGINT';

FLOAT: 'FLOAT';
DOUBLE: 'DOUBLE';

DEC: 'DEC';
DECIMAL: 'DECIMAL';
NUMERIC: 'NUMERIC';

BOOLEAN: 'BOOLEAN';

CHAR: 'CHAR';
CHARACTER: 'CHARACTER';
VARYING: 'VARYING';
VARCHAR: 'VARCHAR';
BINARY: 'BINARY';
VARBINARY: 'VARBINARY';

TIME: 'TIME';
DATE: 'DATE';
TIMESTAMP: 'TIMESTAMP';
INTERVAL: 'INTERVAL';

YEAR: 'YEAR';
MONTH: 'MONTH';
DAY: 'DAY';
HOUR: 'HOUR';
MINUTE: 'MINUTE';
SECOND: 'SECOND';

MAP: 'MAP';
STRUCT: 'STRUCT';
ARRAY: 'ARRAY';
UNION: 'UNION';

// dynamic columns
LATE: 'LATE';
DYNAMIC: 'DYNAMIC';

// additional data types, primary used for Parquet
UINT1: 'UINT1';
UINT2: 'UINT2';
UINT4: 'UINT4';
UINT8: 'UINT8';
TINYINT: 'TINYINT';
SMALLINT: 'SMALLINT';

// symbols
COMMA: ',';
REVERSE_QUOTE: '`';
LEFT_PAREN: '(';
RIGHT_PAREN: ')';
LEFT_ANGLE_BRACKET: '<';
RIGHT_ANGLE_BRACKET: '>';
SINGLE_QUOTE: '\'';
DOUBLE_QUOTE: '"';
LEFT_BRACE: '{';
RIGHT_BRACE: '}';
EQUALS_SIGN: '=';

NOT: 'NOT';
NULL: 'NULL';
AS: 'AS';
FORMAT: 'FORMAT';
DEFAULT: 'DEFAULT';
PROPERTIES: 'PROPERTIES';

NUMBER: [1-9] DIGIT* | '0';
fragment DIGIT: [0-9];

// identifiers

// column name can start with any letter, dollar sign ($) or underscore (_),
// consequently can contain any letter, dollar sign ($), underscore (_) or digit
// if any other symbols are present, use QUOTED_ID
ID: ([A-Z$_]) ([A-Z$_] | DIGIT)*;

// column name should be enclosed into backticks, can contain any symbols including space
// if contains backtick, it should be escaped with backslash (`a\\`b` -> a`b)
// if contains backslash, it should be escaped as well (`a\\\\b` -> a\b)
QUOTED_ID: REVERSE_QUOTE (~[`\\] | '\\' [`\\])* REVERSE_QUOTE;
SINGLE_QUOTED_STRING: SINGLE_QUOTE (~['\\] | '\\' ['\\])* SINGLE_QUOTE;
DOUBLE_QUOTED_STRING: DOUBLE_QUOTE (~["\\] | '\\' ["\\])* DOUBLE_QUOTE;

// skip
LINE_COMMENT:  '//' ~[\r\n]* -> skip;
BLOCK_COMMENT: '/*' .*? '*/' -> skip;
SPACE: [ \n\t\r\u000C]+ -> skip;
