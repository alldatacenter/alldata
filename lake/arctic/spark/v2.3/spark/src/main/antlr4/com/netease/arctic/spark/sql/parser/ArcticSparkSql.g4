/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * The parser file is forked from spark 3.2.0's SqlBase.g4
 */

grammar ArcticSparkSql;

import SqlBase;


singleStatement
    : statement EOF
    ;

statement
  : createTableHeader colListAndPk? tableProvider
       ((OPTIONS options=tablePropertyList) |
       (PARTITIONED BY partitionColumnNames=partitionFieldList) |
       bucketSpec |
       locationSpec |
       (COMMENT comment=STRING) |
       (TBLPROPERTIES tableProps=tablePropertyList))*
       (AS? query)?                                                     #createArcticTable
  | .*?                                                                 #passThrough
  ;



colListAndPk
  :'(' colTypeList (',' primaryKey )? ')'                            #colListWithPk
  | primaryKey                                                        #colListOnlyPk
  ;


primaryKey
  : PRIMARY KEY identifierList
  ;


partitionFieldList
    : '(' fields+=partitionField (',' fields+=partitionField)* ')'
    ;

partitionField
    : identifier                                                     #partitionColumnRef
    | colType                                                       #partitionColumnDefine
    ;


PRIMARY: 'PRIMARY';
KEY: 'KEY';
