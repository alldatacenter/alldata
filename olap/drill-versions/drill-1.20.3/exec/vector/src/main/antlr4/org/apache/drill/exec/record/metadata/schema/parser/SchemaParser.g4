parser grammar SchemaParser;

options {
  language=Java;
  tokenVocab=SchemaLexer;
}

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

schema: (columns | LEFT_PAREN columns? RIGHT_PAREN | /* empty input */) property_values? EOF;

columns: column_def (COMMA column_def)*;

column_def: column property_values?;

column: (primitive_column | struct_column | map_column | simple_array_column | complex_array_column | union_column);

primitive_column: column_id simple_type nullability? format_value? default_value?;

simple_array_column: column_id simple_array_type nullability?;

struct_column: column_id struct_type nullability?;

map_column: column_id map_type nullability?;

complex_array_column: column_id complex_array_type nullability?;

union_column: column_id union_type nullability?;

column_id
: ID # id
| QUOTED_ID # quoted_id
;

simple_type
: (INT | INTEGER) # int
| BIGINT # bigint
| FLOAT # float
| DOUBLE # double
| (DEC | DECIMAL | NUMERIC) (LEFT_PAREN NUMBER (COMMA NUMBER)? RIGHT_PAREN)? # decimal
| BOOLEAN # boolean
| (CHAR | VARCHAR | CHARACTER VARYING?) (LEFT_PAREN NUMBER RIGHT_PAREN)? # varchar
| (BINARY | VARBINARY) (LEFT_PAREN NUMBER RIGHT_PAREN)? # binary
| TIME (LEFT_PAREN NUMBER RIGHT_PAREN)? # time
| DATE # date
| TIMESTAMP (LEFT_PAREN NUMBER RIGHT_PAREN)? # timestamp
| INTERVAL (YEAR | MONTH) # interval_year
| INTERVAL (DAY | HOUR | MINUTE | SECOND)  # interval_day
| INTERVAL # interval
| UINT1 # unit1
| UINT2 # unit2
| UINT4 # unit4
| UINT8 # unit8
| TINYINT # tinyint
| SMALLINT # smallint
| (LATE | DYNAMIC) # dynamic
;

union_type: UNION;

array_type: (simple_array_type | complex_array_type);

simple_array_type: ARRAY LEFT_ANGLE_BRACKET simple_array_value_type RIGHT_ANGLE_BRACKET;

simple_array_value_type
: simple_type # array_simple_type_def
| struct_type # array_struct_type_def
| map_type # array_map_type_def
| union_type # array_union_type_def
;

complex_array_type: ARRAY LEFT_ANGLE_BRACKET array_type RIGHT_ANGLE_BRACKET;

struct_type: STRUCT LEFT_ANGLE_BRACKET columns RIGHT_ANGLE_BRACKET;

map_type: MAP LEFT_ANGLE_BRACKET map_key_type_def COMMA map_value_type_def RIGHT_ANGLE_BRACKET;

map_key_type_def: map_key_type nullability?;

map_key_type
: simple_type # map_key_simple_type_def
;

map_value_type_def: map_value_type nullability?;

map_value_type
: simple_type # map_value_simple_type_def
| struct_type # map_value_struct_type_def
| map_type # map_value_map_type_def
| array_type # map_value_array_type_def
| union_type # map_value_union_type_def
;

nullability: NOT NULL;

format_value: FORMAT string_value;

default_value: DEFAULT string_value;

property_values: PROPERTIES LEFT_BRACE property_pair (COMMA property_pair)* RIGHT_BRACE;

property_pair: string_value EQUALS_SIGN string_value;

string_value: (QUOTED_ID | SINGLE_QUOTED_STRING | DOUBLE_QUOTED_STRING);
