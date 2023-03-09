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
# Apache Griffin Roadmap

## Current feature list
In the current release, we've implemented main DQ features below

- **Data Asset Detection**
  Enabling configuration in service module, Apache Griffin can detect the Hive tables metadata through Hive metastore service.

- **Measure Management**
  Performing operations on UI, user can create, delete and update three types of measures, including: accuracy, profiling and publish metrics.
  However, calling service APIs, user can create, delete and update six types of measures, including: accuracy, profiling, timeliness, uniqueness, completeness and publish metrics.

- **Job Management**
  User can create, delete jobs to schedule a batch job for calculative measures, data range of each calculation, and the extra trigger condition like "done file" on hdfs.

- **Measure Calculation on Spark**
  Service module will trigger and submit calculation jobs to Spark cluster through livy, the measure module calculates and persists the metric values to elasticsearch by default.

- **Metrics Visualization**
  Through service APIs, user can get metric values of each job from elasticsearch.
  Accuracy metrics will be rendered as a chart, profiling metrics will be showed as a table on UI.


## Short-term Roadmap

- **Support more data source types**
  Currently, Apache Griffin only supports Hive table, avro files on hdfs as data source in batch mode, Kafka as data source in streaming mode.
  We plan to support more data source types, like RDBM, elasticsearch.

- **Support more data quality dimensions**
  Apache Griffin needs to support more data quality dimensions, like consistency and validity.

- **Anomaly Detection**
  Apache Griffin plans to support anomaly detection, by analyzing metrics calculated from elasticsearch.
