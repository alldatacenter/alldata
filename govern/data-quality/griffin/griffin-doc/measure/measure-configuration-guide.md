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

# Apache Griffin Measure Configuration Guide
Apache Griffin measure module needs two configuration files to define the parameters of execution, one is for environment, the other is for dq job.

## Environment Parameters
```
{
  "spark": {
    "log.level": "WARN",
    "checkpoint.dir": "hdfs:///griffin/streaming/cp",
    "batch.interval": "1m",
    "process.interval": "5m",
    "config": {
      "spark.default.parallelism": 5,
      "spark.task.maxFailures": 5,
      "spark.streaming.kafkaMaxRatePerPartition": 1000,
      "spark.streaming.concurrentJobs": 4,
      "spark.yarn.maxAppAttempts": 5,
      "spark.yarn.am.attemptFailuresValidityInterval": "1h",
      "spark.yarn.max.executor.failures": 120,
      "spark.yarn.executor.failuresValidityInterval": "1h",
      "spark.hadoop.fs.hdfs.impl.disable.cache": true
    }
  },

  "sinks": [
    {
      "name": "ConsoleSink",
      "type": "console",
      "config": {
        "max.log.lines": 100
      }
    }, {
      "name": "HdfsSink",
      "type": "hdfs",
      "config": {
        "path": "hdfs:///griffin/streaming/persist",
        "max.lines.per.file": 10000
      }
    }
  ],

  "griffin.checkpoint": [
    {
      "type": "zk",
      "config": {
        "hosts": "<zookeeper host ip>:2181",
        "namespace": "griffin/infocache",
        "lock.path": "lock",
        "mode": "persist",
        "init.clear": true,
        "close.clear": false
      }
    }
  ]
}
```
Above lists environment parameters.  

- **spark**: This field configures spark and spark streaming parameters.  
	+ log.level: Level of spark log.
	+ checkpoint.dir: Check point directory of spark streaming, for streaming mode.
	+ batch.interval: Interval of dumping streaming data, for streaming mode.
	+ process.interval: Interval of processing dumped streaming data, for streaming mode.
	+ config: Configuration of spark parameters.
- **sinks**: This field configures list of sink definitions to persist both records and metrics. Details of sink configuration are available [here](#sinks).
- **griffin.checkpoint**: This field configures list of griffin checkpoint parameters, multiple cache ways are supported. It is only for streaming dq case. Details of info cache configuration [here](#griffin-checkpoint).

### Sinks
Sinks allow persistence of job metrics and bad data (source records that violated the defined rules) to external 
storage systems. 
Sinks have to be defined in the Env Config, and their `name` are mentioned in the Job Config. 

List of supported sinks:
 - Console
 - HDFS
 - MongoDB
 - ElasticSearch 
 - Custom Implementations
 
 #### Configuration
  A sample sink configuration is as following,
  
  ```
...

 "sinks": [
     {
       "name": "ConsoleSink",
       "type": "CONSOLE",
       "config": {
         "numRows": 10,
         "truncate": false
       }
     }
   ]

...
  ```
 
  ##### Key Parameters:
  | Name    | Type     | Description                             | Supported Values                                 |
  |:--------|:---------|:----------------------------------------|:-------------------------------------------------|
  | name    | `String` | User defined unique name for Sink       |                                                  |
  | type    | `String` | Type of Sink (Value is case insensitive)| console, hdfs, elasticsearch, mongodb, custom    |
  | config  | `Object` | Configuration params of the sink        | Depends on sink type (see below)                 |
 
  ##### For Custom Sinks:
  - **config** object must contain the key **class** whose value specifies class name for user-provided sink 
  implementation. This class should implement  `org.apache.griffin.measure.sink.Sink` trait
  - Example:
       ```
    ...
    
     "sinks": [
         {
           "name": "MyCustomSink",
           "type": "CUSTOM",
           "config": {
             "class": "my.package.sink.MyCustomSinkImpl",
             ...
           }
         }
       ]
    
    ...
       ```
  
  **Note:** User-provided sink should be present in Spark job's class path, by either providing custom jar with 
 `--jars` parameter to spark-submit or by adding setting `spark.jars` in `spark -> config` section of environment config.  

##### For Console Sink:
  - Console Sink, supports the following configurations. Other alias' like 'Log' as value for `type`.
  
     | Name           | Type     | Description                            | Default Values |
     |:---------------|:---------|:---------------------------------------|:-------------- |
     | numRows        | `Integer`| Number of records to log               | 20             |
     | truncate       | `Boolean`| If true, strings more than 20 characters will be truncated and all cells will be aligned right| `true` |
     
 - Example:
      ```
     ...
     
      "sinks": [
          {
            "name": "ConsoleSink",
            "type": "CONSOLE",
            "config": {
              "numRows": 10,
              "truncate": false
            }
          }
        ]
     
     ...
      ```

 ##### For HDFS Sink:
   - HDFS Sink, supports the following configurations
   
      | Name               | Type     | Description                            | Default Values |
      |:-------------------|:---------|:---------------------------------------|:-------------- |
      | path               | `String` | HDFS base path to sink metrics         |                |
      | max.persist.lines  | `Integer`| the max lines of total sink data       | -1             |
      | max.lines.per.file | `Integer`| the max lines of each sink file        | 1000000        |
      
  - Example:
       ```
      ...
      
       "sinks": [
           {
             "name": "hdfsSink",
             "type": "HDFS",
             "config": {
               "path": "hdfs://localhost/griffin/batch/persist",
               "max.persist.lines": 10000,
               "max.lines.per.file": 10000
             }
           }
         ]
      
      ...
       ```
 
  ##### For MongoDB Sink:
  - MongoDB Sink, supports the following configurations. Other alias' like 'Mongo' as value for `type`.
  
     | Name       | Type     | Description       | Default Values |
     |:-----------|:---------|:------------------|:-------------- |
     | url        | `String` | URL of MongoDB    |                |
     | database   | `String` | Database name     |                |
     | collection | `String` | Collection name   |                |
     | over.time  | `Long`   | Wait Duration     | -1             |
     | retry      | `Int`    | Number of retries | 10             |
     
 - Example:
      ```
     ...
     
      "sinks": [
          {
            "name": "MongoDBSink",
            "type": "MongoDB",
            "config": {
              ...
            }
          }
        ]
     
     ...
      ```

 ##### For Elasticsearch Sink:
 - Elasticsearch Sink, supports the following configurations. Other alias' like 'ES' and 'HTTP' as value for `type`.
   
      | Name               | Type     | Description                   | Default Values |
      |:-------------------|:---------|:------------------------------|:-------------- |
      | api                | `String` | api to submit sink metrics    |                |
      | method             | `String` | http method, "post" default   |                |
      | connection.timeout | `Long`   | Wait Duration                 | -1             |
      | retry              | `Integer`| Number of retries             | 10             |
      
  - Example:
       ```
      ...
      
       "sinks": [
           {
             "name": "ElasticsearchSink",
             "type": "Elasticsearch",
             "config": {
               ...
             }
           }
         ]
      
      ...
       ```

### Griffin Checkpoint
- **type**: Griffin checkpoint type, "zk" for zookeeper checkpoint.
- **config**: Configure parameters of griffin checkpoint type.
	+ zookeeper checkpoint
		* hosts: zookeeper hosts list as a string, separated by comma.
		* namespace: namespace of cache info, "" as default.
		* lock.path: path of lock info, "lock" as default.
		* mode: create mode of zookeeper node, "persist" as default.
		* init.clear: clear cache info when initialize, true default.
		* close.clear: clear cache info when close connection, false default.

## DQ Job Parameters
```
{
  "name": "accu_batch",
  "process.type": "BATCH",
  "data.sources": [
    {
      "name": "src",
      "connector": {
        "type": "AVRO",
        "config": {
          "file.path": "<path>/<to>",
          "file.name": "<source-file>.avro"
        }
      }
    },
    {
      "name": "tgt",
      "connector": {
        "type": "AVRO",
        "config": {
          "file.path": "<path>/<to>",
          "file.name": "<target-file>.avro"
        }
      }
    }
  ],
  "evaluate.rule": {
    "rules": [
      {
        "dsl.type": "griffin-dsl",
        "dq.type": "ACCURACY",
        "out.dataframe.name": "accu",
        "rule": "source.user_id = target.user_id AND upper(source.first_name) = upper(target.first_name) AND source.last_name = target.last_name AND source.address = target.address AND source.email = target.email AND source.phone = target.phone AND source.post_code = target.post_code",
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
            "type": "record"
          }
        ]
      }
    ]
  },
  "sinks": [
    "CONSOLESink",
    "HTTPSink",
    "HDFSSink"
  ]
}
```
Above lists DQ job configure parameters.  

- **name**: Name of DQ job.
- **process.type**: Process type of DQ job, "BATCH" or "STREAMING".
- **data.sources**: List of data sources in this DQ job.
	+ name: Name of this data source, it should be different from other data sources.
	+ connector: Data connector for this data source. Details of data connector configuration [here](#data-connector).
- **evaluate.rule**: Evaluate rule parameters of this DQ job.
	+ dsl.type: Default dsl type of all the rules.
	+ rules: List of rules, to define every rule step. Details of rule configuration [here](#rule).
- **sinks**: Whitelisted sink types for this job. Note: no sinks will be used, if empty or omitted. 

### Data Connector

Data Connector help connect to external sources on which DQ checks can be applied.

List of supported data connectors:
 - Hive
 - Kafka (Steaming only)
 - ElasticSearch (Batch only) 
 - **File based:** Parquet, Avro, ORC, CSV, TSV, Text.
 - **JDBC based:** MySQL, PostgreSQL etc.
 - **Custom:** Cassandra
 
 #### Configuration
 A sample data connector configuration is as following,
 
 ```
...

"connector": {
    "type": "file",
    "config": {
      "key1": "value1",
      "key2": "value2"
    }
  }

...
 ```

 ##### Key Parameters:
 | Name    | Type     | Description                            | Supported Values                                 |
 |:--------|:---------|:---------------------------------------|:-------------------------------------------------|
 | type    | `String` | Type of the Connector                  | file, hive, kafka (streaming only), jdbc, custom |
 | config  | `Object` | Configuration params of the connector  | Depends on connector type (see below)            |

 ##### For Custom Data Connectors:
 - **config** object must contain the key **class** whose value specifies class name for user-provided data connector 
 implementation. 
    + For **Batch** it should implement BatchDataConnector trait.
    + For **Streaming** it should implement StreamingDataConnector trait.
 - Example:
      ```
     "connector": {
         "type": "custom",
         "config": {
           "class": "org.apache.griffin.measure.datasource.connector.batch.CassandraDataConnector",
           ...
         }
       }
      ```
 
 **Note:** User-provided data connector should be present in Spark job's class path, by either providing custom jar with 
`--jars` parameter to spark-submit or by adding setting `spark.jars` in `spark -> config` section of environment config.  

 ##### [Deprecated] ~~For ElasticSearch Custom Data Connectors:~~ 
  - Currently supported SQL mode (for ElasticSearch with sql plugin) and NORMAL mode.
  - For NORMAL mode, config object supports the following keys,
  
 | Name       | Type     | Description                            | Default Values |
 |:-----------|:---------|:---------------------------------------|:-------------- |
 | index      | `String` | ElasticSearch index name| default |
 | type       | `String` | ElasticSearch data type | accuracy |
 | host       | `String` | ElasticSearch url host | `Empty` |
 | port       | `String` | ElasticSearch url port | `Empty` |
 | fields     | `List`   | list of columns | `Empty` |
 | size       | `Integer`| data size (lines) to load | 100 |
 | metric.name| `String` | metric name to load | * |

 - Example:
      ```
     "connectors": [
       { 
         "type": "custom",
         "config": {
           "class": "org.apache.griffin.measure.datasource.connector.batch.ElasticSearchGriffinDataConnector",
           "index": "test-index-v1",
           "type": "metric",
           "host": "test.es-xxx.org",
           "port": "80",
           "fields": ["col_a", "col_b", "col_c"],
           "size": 20
         }
       }
     ]
      ```
  - For SQL mode, config object supports the following keys,
  
 | Name       | Type     | Description                            | Default Values |
 |:-----------|:---------|:---------------------------------------|:-------------- |
 | host       | `String` | ElasticSearch url host | `Empty` |
 | port       | `String` | ElasticSearch url port | `Empty` |
 | sql.mode   | `Boolean`| use sql mode | false |
 | sql        | `String` | ElasticSearch SQL | `Empty` |

 - Example:
      ```
     "connectors": [
       { 
         "type": "custom",
         "config": {
           "class": "org.apache.griffin.measure.datasource.connector.batch.ElasticSearchGriffinDataConnector",
           "host": "test.es-xxx.org",
           "port": "80",
           "sql.mode": true,
           "sql": "select col_a, col_b, col_c from test-index-v1 limit 20"
         }
       }
     ]
      ```
 
 ##### For File based Data Connectors:

 - Currently supports formats like Parquet, ORC, AVRO, Text and Delimited types like CSV, TSV etc.
 - Local files can also be read by prepending `file://` namespace.
 - **config** object supports the following keys,
        
 | Name       | Type     | Description                            | Supported Values | Default Values |
 |:-----------|:---------|:---------------------------------------|:-----------------|:-------------- |
 | format     | `String` | type of file source| parquet, avro, orc, csv, tsv, text | parquet |
 | paths      | `List`   | path(s) to be read | | `Empty` |
 | options    | `Object` | format specific options | | `Empty` |
 | skipOnError| `Boolean`| whether to continue execution if one or more paths are invalid | true, false | false |
 | schema     | `List`   | given as list of key value pairs | See example below | `null` |

 - Example:
      ```
     "connector": {
         "type": "file",
         "config": {
           "format": "csv",
           "paths": [
             "/path/to/csv/dir/*",
             "/path/to/dir/test.csv"
           ],
           "options": {
             "header": "true"
           },
           "skipOnError": "false",
           "schema": [
             {
               "name": "user_id",
               "type": "string",
               "nullable": "true"
             },
             {
               "name": "age",
               "type": "int",
               "nullable": "false"
             }
           ]
         }
       }
   
 **Note:** Additional examples of schema:
- "schema":[{"name":"user_id","type":"string","nullable":"true"},{"name":"age","type":"int","nullable":"false"}]
- "schema":[{"name":"user_id","type":"decimal(5,2)","nullable":"true"}]
- "schema":[{"name":"my_struct","type":"struct<f1:int,f2:string>","nullable":"true"}]


##### For ElasticSearch Data Connectors:
  - Elasticsearch Data Connector, supports the following configurations
  
 | Name           | Type     | Description                            | Default Values |
 |:---------------|:---------|:---------------------------------------|:-------------- |
 | paths          | `List`   | Elasticsearch indices (Required)                  |  |
 | filterExprs    | `List`   | List of string expressions that act as where conditions (row filters)| `Empty` |
 | selectionExprs | `List`   | List of string expressions that act as selection conditions (column filters) | `Empty` |
 | options        | `Object` | Additional elasticsearch options. Refer to [ConfigurationOptions](https://github.com/elastic/elasticsearch-hadoop/blob/v7.6.1/mr/src/main/java/org/elasticsearch/hadoop/cfg/ConfigurationOptions.java) for options | `Empty` |
 
 - Example:
      ```
     "connector": {
             "type": "elasticsearch",
             "config": {
               "selectionExprs": [
                 "account_number",
                 "city",
                 "gender",
                 "age > 18"
               ],
               "filterExprs": [
                 "account_number < 10"
               ],
               "paths": [
                 "bank",
                 "customer"
               ],
               "options": {
                 "es.nodes": "localhost",
                 "es.port": 9200
               }
             }
           }
      ```

 ##### For Hive Data Connectors:
 - **config** object supports the following keys,
    * database: data base name, optional, "default" as default.
    * table.name: table name.
    * where: where conditions string, split by ",", optional.
        e.g. `dt=20170410 AND hour=15, dt=20170411 AND hour=15, dt=20170412 AND hour=15`
        
 ##### For JDBC based Data Connectors:
- **config** object supports the following keys,

| Name       | Type     | Description                            | Default Values |
|:-----------|:---------|:---------------------------------------|:-------------- |
| database   | `String` | database name                          | default |
| tablename  | `String` | table name to be read                  | `Empty` |
| url        | `String` | the connection string URL to database  | `Empty` |
| user       | `String` | user for connection to database        | `Empty` |
| password   | `String` | password for connection to database    | `null`  |
| driver     | `String` | driver class for JDBC connection to database | com.mysql.jdbc.Driver |
| where      | `String` | condition for reading data from table  | `Empty` |

- Example:
   ```
  "connector": {
      "type": "jdbc",
      "config": {
        "database": "default",
        "tablename": "test",
        "url": "jdbc:mysql://localhost:3306/default",
        "user": "test_u",
        "password": "test_p",
        "driver": "com.mysql.jdbc.Driver",
        "where": ""
      }
    } 
  
**Note:** Jar containing driver class should be present in Spark job's class path, by either providing custom jar with 
`--jars` parameter to spark-submit or by adding setting `spark.jars` in `spark -> config` section of environment config.  

### Rule
- **dsl.type**: Rule dsl type, "spark-sql", "df-ops" and "griffin-dsl".
- **dq.type**: DQ type of this rule, only for "griffin-dsl" type. Supported types: "ACCURACY", "PROFILING", "TIMELINESS", "UNIQUENESS", "COMPLETENESS".
- **out.dataframe.name** (step information): Output table name of this rule, could be used in the following rules.
- **in.dataframe.name** (step information): Input table name of this rule, only used for "df-ops" type.
- **rule**: The rule string.
- **details**: Details of this rule, optional.
  + accuracy dq type detail configuration
    * source: the data source name which as source in accuracy, default is the name of first data source in "data.sources" if not configured.
    * target: the data source name which as target in accuracy, default is the name of second data source in "data.sources" if not configured.
    * miss: the miss count name in metric, optional.
    * total: the total count name in metric, optional.
    * matched: the matched count name in metric, optional.
  + profiling dq type detail configuration
    * source: the data source name which as source in profiling, default is the name of first data source in "data.sources" if not configured. If the griffin-dsl rule contains from clause, this parameter is ignored.
  + distinctness dq type detail configuration
    * source: name of data source to measure uniqueness.
    * target: name of data source to compare with. It is always the same as source, or more than source.
    * distinct: the unique count name in metric, optional.
    * total: the total count name in metric, optional.
    * dup: the duplicate count name in metric, optional.
    * accu_dup: the accumulate duplicate count name in metric, optional, only in streaming mode and "with.accumulate" enabled.
    * num: the duplicate number name in metric, optional.
    * duplication.array: optional, if set as a non-empty string, the duplication metric will be computed, and the group metric name is this string.
    * with.accumulate: optional, default is true, if set as false, in streaming mode, the data set will not compare with old data to check distinctness.
  + uniqueness dq type detail configuration
    * source: name of data source to measure uniqueness.
    * target: name of data source to compare with. It is always the same as source, or more than source.
    * unique: the unique count name in metric, optional.
    * total: the total count name in metric, optional.
    * dup: the duplicate count name in metric, optional.
    * num: the duplicate number name in metric, optional.
    * duplication.array: optional, if set as a non-empty string, the duplication metric will be computed, and the group metric name is this string.
  + completeness dq type detail configuration
    * source: name of data source to measure completeness.
    * total: name of data source to compare with. It is always the same as source, or more than source.
    * complete: the column name in metric, optional. The number of not null values.
    * incomplete: the column name in metric, optional. The number of null values.
  + timeliness dq type detail configuration
    * source: name of data source to measure timeliness.
    * latency: the latency column name in metric, optional.
    * total: column name, optional.
    * avg: column name, optional. The average latency.
    * step: column nmae, optional. The histogram where "bin" is step=floor(latency/step.size).
    * count: column name, optional. The number of the same latencies in the concrete step.
    * percentile: column name, optional.
    * threshold: optional, if set as a time string like "1h", the items with latency more than 1 hour will be record.
    * step.size: optional, used to build the histogram of latencies, in milliseconds (ex. "100").
    * percentile.values: optional, used to compute the percentile metrics, values between 0 and 1. For instance, We can see fastest and slowest latencies if set [0.1, 0.9].
- **cache**: Cache output dataframe. Optional, valid only for "spark-sql" and "df-ops" mode. Defaults to `false` if not specified.
- **out**: List of output sinks for the job.
  + Metric output.
    * type: "metric"
    * name: Metric name, semantics depends on "flatten" field value.   
    * flatten: Aggregation method used before sending data frame result into the sink:  
      - default: use "array" if data frame returned multiple records, otherwise use "entries" 
      - entries: sends first row of data frame as metric results, like like `{"agg_col": "value"}`
      - array: wraps all metrics into a map, like `{"my_out_name": [{"agg_col": "value"}]}`
      - map: wraps first row of data frame into a map, like `{"my_out_name": {"agg_col": "value"}}`
  + Record output. Currently handled only by HDFS sink.
    * type: "record"
    * name: File name within sink output folder to dump files to.   
  + Data source cache update for streaming jobs.
    * type: "dsc-update"
    * name: Data source name to update cache.   
