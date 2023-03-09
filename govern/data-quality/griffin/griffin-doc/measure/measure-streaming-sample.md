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

# Measure Streaming Sample
Apache Griffin measures consist of batch measure and streaming measure, this document merely gives the streaming measure sample.

## Streaming Accuracy Sample
```
{
  "name": "accu_streaming",
  "process.type": "STREAMING",
  "data.sources": [
    {
      "name": "source",
      "baseline": true,
      "connector": {
        "type": "KAFKA",
        "version": "0.8",
        "config": {
          "kafka.config": {
            "bootstrap.servers": "10.149.247.156:9092",
            "group.id": "src_group",
            "auto.offset.reset": "largest",
            "auto.commit.enable": "false"
          },
          "topics": "sss",
          "key.type": "java.lang.String",
          "value.type": "java.lang.String"
        },
        "pre.proc": [
          {
            "dsl.type": "df-ops",
            "in.dataframe.name": "this",
            "out.dataframe.name": "s1",
            "rule": "from_json"
          },
          {
            "dsl.type": "spark-sql",
            "out.dataframe.name": "this",
            "rule": "select name, age from s1"
          }
        ]
      },
      "cache": {
        "file.path": "hdfs://localhost/griffin/streaming/dump/source",
        "info.path": "source",
        "ready.time.interval": "10s",
        "ready.time.delay": "0",
        "time.range": [
          "-2m",
          "0"
        ],
        "updatable": true
      }
    },
    {
      "name": "target",
      "connector": {
        "type": "KAFKA",
        "version": "0.8",
        "config": {
          "kafka.config": {
            "bootstrap.servers": "10.149.247.156:9092",
            "group.id": "tgt_group",
            "auto.offset.reset": "largest",
            "auto.commit.enable": "false"
          },
          "topics": "ttt",
          "key.type": "java.lang.String",
          "value.type": "java.lang.String"
        },
        "pre.proc": [
          {
            "dsl.type": "df-ops",
            "in.dataframe.name": "this",
            "out.dataframe.name": "t1",
            "rule": "from_json"
          },
          {
            "dsl.type": "spark-sql",
            "out.dataframe.name": "this",
            "rule": "select name, age from t1"
          }
        ]
      },
      "cache": {
        "file.path": "hdfs://localhost/griffin/streaming/dump/target",
        "info.path": "target",
        "ready.time.interval": "10s",
        "ready.time.delay": "0",
        "time.range": [
          "-2m",
          "0"
        ]
      }
    }
  ],
  "evaluate.rule": {
    "rules": [
      {
        "dsl.type": "griffin-dsl",
        "dq.type": "ACCURACY",
        "out.dataframe.name": "accu",
        "rule": "source.name = target.name and source.age = target.age",
        "details": {
          "source": "source",
          "target": "target",
          "miss": "miss_count",
          "total": "total_count",
          "matched": "matched_count"
        },
        "out": [
          {
            "type": "metric",
            "name": "accu"
          },
          {
            "type": "record",
            "name": "missRecords"
          }
        ]
      }
    ]
  },
  "sinks": [
    "CONSOLESink",
    "ELASTICSEARCHSink"
  ]
}
```
Above is the configure file of streaming accuracy job.  

### Data source
In this sample, we use kafka topics as source and target.  
At current, Apache Griffin supports kafka 0.8, for 1.0 or later version is during implementation.  
In Apache Griffin implementation, we can only support json string as kafka data, which could describe itself in data. In some other solution, there might be a schema proxy for kafka binary data, you can implement such data source connector if you need, it's also during implementation by us.
In streaming cases, the data from topics always needs some pre-process first, which is configured in `pre.proc`, just like the `rules`, Apache Griffin will not parse sql content, so we use some pattern to mark your temporory tables. `${this}` means the origin data set, and the output table name should also be `${this}`.

For example, you can create two topics in kafka, for source and target data, the format could be json string.
Source data could be:
```
{"name": "kevin", "age": 24}
{"name": "jason", "age": 25}
{"name": "jhon", "age": 28}
{"name": "steve", "age": 31}
```
Target data could be:
```
{"name": "kevin", "age": 24}
{"name": "jason", "age": 25}
{"name": "steve", "age": 20}
```
You need to input the source data and target data into these two topics, through console producer might be a good choice for experimental purpose.

### Evaluate rule
In this accuracy sample, the rule describes the match condition: `source.name = target.name and source.age = target.age`.  
The accuracy metrics will be persisted as metric, with miss column named "miss_count", total column named "total_count", matched column named "matched_count".  
The miss records of source will be persisted as record.  

## Streaming Profiling Sample
```
{
  "name": "prof_streaming",
  "process.type": "STREAMING",
  "data.sources": [
    {
      "name": "source",
      "connector": {
        "type": "KAFKA",
        "version": "0.8",
        "config": {
          "kafka.config": {
            "bootstrap.servers": "10.149.247.156:9092",
            "group.id": "group1",
            "auto.offset.reset": "smallest",
            "auto.commit.enable": "false"
          },
          "topics": "sss",
          "key.type": "java.lang.String",
          "value.type": "java.lang.String"
        },
        "pre.proc": [
          {
            "dsl.type": "df-ops",
            "in.dataframe.name": "this",
            "out.dataframe.name": "s1",
            "rule": "from_json"
          },
          {
            "dsl.type": "spark-sql",
            "out.dataframe.name": "this",
            "rule": "select name, age from s1"
          }
        ]
      },
      "cache": {
        "file.path": "hdfs://localhost/griffin/streaming/dump/source",
        "info.path": "source",
        "ready.time.interval": "10s",
        "ready.time.delay": "0",
        "time.range": [
          "0",
          "0"
        ]
      }
    }
  ],
  "evaluate.rule": {
    "rules": [
      {
        "dsl.type": "griffin-dsl",
        "dq.type": "PROFILING",
        "out.dataframe.name": "prof",
        "rule": "select count(name) as `cnt`, max(age) as `max`, min(age) as `min` from source",
        "out": [
          {
            "type": "metric",
            "name": "prof"
          }
        ]
      },
      {
        "dsl.type": "griffin-dsl",
        "dq.type": "PROFILING",
        "out.dataframe.name": "grp",
        "rule": "select name, count(*) as `cnt` from source group by name",
        "out": [
          {
            "type": "metric",
            "name": "name_group",
            "flatten": "array"
          }
        ]
      }
    ]
  },
  "sinks": [
    "CONSOLESink",
    "ELASTICSEARCHSink"
  ]
}
```
Above is the configure file of streaming profiling job.  

### Data source
In this sample, we use kafka topics as source.  

### Evaluate rule
In this profiling sample, the rule describes the profiling request: `select count(name) as `cnt`, max(age) as `max`, min(age) as `min` from source` and `select name, count(*) as `cnt` from source group by name`.  
The profiling metrics will be persisted as metric, with these two results in one json.
