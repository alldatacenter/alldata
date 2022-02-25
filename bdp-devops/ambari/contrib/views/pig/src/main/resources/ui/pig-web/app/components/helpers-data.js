/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

module.exports = [
  {
    'title':'Eval Functions',
    'helpers':[
      'AVG(%VAR%)',
      'CONCAT(%VAR1%, %VAR2%)',
      'COUNT(%VAR%)',
      'COUNT_START(%VAR%)',
      'IsEmpty(%VAR%)',
      'DIFF(%VAR1%, %VAR2%)',
      'MAX(%VAR%)',
      'MIN(%VAR%)',
      'SIZE(%VAR%)',
      'SUM(%VAR%)',
      'TOKENIZE(%VAR%, %DELIM%)'
    ]
  },
  {
    'title':'Relational Operators',
    'helpers':[
      'COGROUP %VAR1% BY %VAR2%',
      'CROSS %VAR1%, %VAR2%;',
      'DISTINCT %VAR%;',
      'FILTER %VAR% BY %COND%',
      'FLATTEN(%VAR%)',
      'FOREACH %DATA% GENERATE %NEW_DATA%',
      'FOREACH %DATA% {%NESTED_BLOCK%}',
      'GROUP %VAR1% BY %VAR2%',
      'GROUP %VAR% ALL',
      'JOIN %VAR% BY ',
      'LIMIT %VAR% %N%',
      'ORDER %VAR% BY %FIELD%',
      'SAMPLE %VAR% %SIZE%',
      'SPLIT %VAR1% INTO %VAR2% IF %EXPRESSIONS%',
      'UNION %VAR1%, %VAR2%'
    ]
  },
  {
    'title':'I/0',
    'helpers':[
      "LOAD '%FILE%';",
      'DUMP %VAR%;',
      'STORE %VAR% INTO %PATH%;'
    ]
  },
  {
    'title':'Debug',
    'helpers':[
      'EXPLAIN %VAR%;',
      'ILLUSTRATE %VAR%;',
      'DESCRIBE %VAR%;'
    ]
  },
  {
    'title':'HCatalog',
    'helpers':[
      "LOAD '%TABLE%' USING org.apache.hive.hcatalog.pig.HCatLoader();"
    ]
  },
  {
    'title':'Math',
    'helpers':[
      'ABS(%VAR%)',
      'ACOS(%VAR%)',
      'ASIN(%VAR%)',
      'ATAN(%VAR%)',
      'CBRT(%VAR%)',
      'CEIL(%VAR%)',
      'COS(%VAR%)',
      'COSH(%VAR%)',
      'EXP(%VAR%)',
      'FLOOR(%VAR%)',
      'LOG(%VAR%)',
      'LOG10(%VAR%)',
      'RANDOM(%VAR%)',
      'ROUND(%VAR%)',
      'SIN(%VAR%)',
      'SINH(%VAR%)',
      'SQRT(%VAR%)',
      'TAN(%VAR%)',
      'TANH(%VAR%)'
    ]
  },
  {
    'title':'Tuple, Bag, Map Functions',
    'helpers':[
      'TOTUPLE(%VAR%)',
      'TOBAG(%VAR%)',
      'TOMAP(%KEY%, %VALUE%)',
      'TOP(%topN%, %COLUMN%, %RELATION%)'
    ]
  },
  {
    'title':'String Functions',
    'helpers':[
      "INDEXOF(%STRING%, '%CHARACTER%', %STARTINDEX%)",
      "LAST_INDEX_OF(%STRING%, '%CHARACTER%', %STARTINDEX%)",
      "LOWER(%STRING%)",
      "REGEX_EXTRACT(%STRING%, %REGEX%, %INDEX%)",
      "REGEX_EXTRACT_ALL(%STRING%, %REGEX%)",
      "REPLACE(%STRING%, '%oldChar%', '%newChar%')",
      "STRSPLIT(%STRING%, %REGEX%, %LIMIT%)",
      "SUBSTRING(%STRING%, %STARTINDEX%, %STOPINDEX%)",
      "TRIM(%STRING%)",
      "UCFIRST(%STRING%)",
      "UPPER(%STRING%)"
    ]
  },
  {
    'title':'Macros',
    'helpers':[
      "IMPORT '%PATH_TO_MACRO%';"
    ]
  },
  {
    'title':'HBase',
    'helpers':[
      "LOAD 'hbase://%TABLE%' USING org.apache.pig.backend.hadoop.hbase.HBaseStorage('%columnList%')",
      "STORE %VAR% INTO 'hbase://%TABLE%' USING org.apache.pig.backend.hadoop.hbase.HBaseStorage('%columnList%')"
    ]
  },
  {
    'title':'Python UDF',
    'helpers':[
      "REGISTER 'python_udf.py' USING jython AS myfuncs;"
    ]
  }
]
