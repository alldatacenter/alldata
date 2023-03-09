<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# Apache Griffin Shell Guide

With Griffin shell, user can run dq jobs in command line. 
This is helpful for user to debug and run user dq jobs.

# Install

* Compile Griffin using maven.
* Decompress Griffin tool file `measure-x.x.x-package.tar.gz` in the target directory of measure module.
* Install and configure spark.
* Run Griffin tool with user defined env file and dq file. eg: `measure-x.x.x/bin/griffin-tool.sh ENV_FILE DQ_FILE`

# ENV_FILE demo

```json
{
  "spark": {
    "log.level": "WARN",
    "config": {
      "spark.master": "local[*]"
    }
  },

  "sinks": [
    {
      "name": "MyConsoleSink",
      "type": "CONSOLE",
      "config": {
        "max.log.lines": 10
      }
    },
    {
      "name": "MyHDFSSink",
      "type": "HDFS",
      "config": {
        "path": "hdfs://localhost/griffin/batch/persist",
        "max.persist.lines": 10000,
        "max.lines.per.file": 10000
      }
    },
    {
      "name": "MyElasticSearchSink",
      "type": "ELASTICSEARCH",
      "config": {
        "method": "post",
        "api": "http://localhost:9200/griffin/accuracy",
        "connection.timeout": "1m",
        "retry": 10
      }
    }
  ],

  "griffin.checkpoint": []
}

```

# DQ file Demo

```json
{
  "name": "accu_batch",
  "process.type": "batch",
  "data.sources": [
    {
      "name": "source",
      "baseline": true,
      "connector": {
        "type": "jdbc",
        "config": {
          "user": "xxx",
          "password": "xxx",
          "tablename":  "stu",
          "where": "id < 3",
          "url":"jdbc:mysql://localhost:3306/test",
          "database": "test",
          "driver": "com.mysql.jdbc.Driver"
        }
      }
    },
    {
      "name": "target",
      "connector": {
        "type": "jdbc",
        "config": {
          "user": "xxx",
          "password": "xxx",
          "tablename":  "stu2",
          "where": "id < 3",
          "url":"jdbc:mysql://localhost:3306/test",
          "database": "test",
          "driver": "com.mysql.jdbc.Driver"
        }
      }
    }
  ],
  "evaluate.rule": {
    "rules": [
      {
        "dsl.type": "griffin-dsl",
        "dq.type": "accuracy",
        "out.dataframe.name": "accu",
        "rule": "source.id = target.id AND upper(source.name) = upper(target.name) ",
        "details": {
          "source": "source",
          "target": "target",
          "miss": "miss_count",
          "total": "total_count",
          "matched": "matched_count"
        },
        "out": [
          {
            "type": "record",
            "name": "missRecords"
          }
        ]
      }
    ]
  },
  "sinks": [
    "consoleSink"
  ]
}

```