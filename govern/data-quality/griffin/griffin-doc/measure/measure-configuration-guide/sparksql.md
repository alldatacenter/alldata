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

SparkSQL Measure - Configuration Guide
=====================================

### Introduction

In some cases, the pre-defined dimensions/ measures may not enough to model a complete data quality definition. For such
cases, Apache Griffin allows the definition of complex custom user-defined checks as SparkSQL queries.

SparkSQL measure is like a pro mode that allows advanced users to configure complex custom checks that are not covered
by other measures. These SparkSQL queries may contain clauses like `select`, `from`, `where`, `group-by`, `order-by`
, `limit`, `join`, etc.

### Configuration

The SparkSQL measure can be configured as below,

```json
{
  ...

  "measures": [
    {
      "name": "spark_sql_measure",
      "type": "sparkSQL",
      "data.source": "crime_report_source",
      "config": {
        "expr": "SELECT t.*, sq.zip IS NULL AS __measure_spark_sql_measure FROM crime_report_source AS t LEFT OUTER JOIN (SELECT zipcode as zip, COUNT(DISTINCT city) AS city_count FROM crime_report_source GROUP BY zipcode having city_count = 1) as sq ON sq.zip=t.zipcode",
        "bad.record.definition": "__measure_spark_sql_measure"
      },
      "out": [
        {
          "type": "metric",
          "name": "spark_sql_metric",
          "flatten": "map"
        },
        {
          "type": "record",
          "name": "spark_sql_records"
        }
      ]
    }
  ]

  ...
}
 ```

##### Key Parameters:

| Name    | Type     | Description                            | Supported Values                                          |
|:--------|:---------|:---------------------------------------|:----------------------------------------------------------|
| name    | `String` | User-defined name of this measure      | -                                                         |
| type    | `String` | Type of Measure                        | completeness, duplication, profiling, accuracy, sparksql, schemaConformance  |
| data.source | `String` | Name of data source on which this measure is applied  | -                                      |
| config  | `Object` | Configuration params of the measure    | Depends on measure type ([see below](#example-config-object))                       |
| out     | `List  ` | Define output(s) of measure execution  | [See below](#outputs)                                               |

##### Example `config` Object:

`config` object for SparkSQL measure contains the following keys,

- `expr`: The value for `expr` is a valid SparkSQL query string. This is a mandatory parameter

- `bad.record.definition`: As the key suggests, its value defines what exactly would be considered as a bad record after
  this query executes. For example, the output of the configured query above has the following schema,
  ```
    root
      |-- date_time: timestamp (nullable = true)
      |-- incident: string (nullable = true)
      |-- address: string (nullable = true)
      |-- city: string (nullable = true)
      |-- zipcode: integer (nullable = true)
      |-- __measure_spark_sql_measure: integer (nullable = false)
  ```

  In order to separate the good data from bad data, a `bad.record.definition` expression must be set. This expression
  can be a SparkSQL like expression and must yield a column with boolean data type.

  _Note:_ This expression describes the bad records, i.e. if `bad.record.definition` = `true` for a record, it is marked
  as bad/ incomplete record.

### Outputs

SparkSQL measure supports the following two outputs,

- Metrics
- Records

Users can choose to define any combination of these 2 outputs. For no outputs, skip this `out: [ ... ]` section from the
measure configuration.

#### Metrics Outputs

To write metrics for SparkSQL measure, configure the measure with output section as below,

```json
{
  ...

  "out": [
    {
      "name": "spark_sql_metric",
      "type": "metric"
    }
  ]

  ...
}
 ```

This will generate the metrics like below,

```json
{
  "applicationId": "local-1623450429783",
  "job_name": "Batch-All-Measures-Example",
  "tmst": 1623450438754,
  "measure_name": "spark_sql_measure",
  "metrics": [
    {
      "metric_name": "total",
      "metric_value": "4617"
    },
    {
      "metric_name": "complete",
      "metric_value": "1983"
    },
    {
      "metric_name": "incomplete",
      "metric_value": "2634"
    }
  ],
  "measure_type": "SparkSQL",
  "data_source": "crime_report_source"
}
```

_Note:_ This output is similar to that of completeness measure as it produces the `total`, `complete` and `incomplete`
metrics. For each record of the query resultant data set the `bad.record.definition` expression is evaluated, and the
bad records are marked as incomplete records (`bad.record.definition` = `true`).

#### Record Outputs

To write records as output for SparkSQL measure, configure the measure with output section as below,

```json
{
  ...

  "out": [
    {
      "type": "record",
      "name": "spark_sql_records"
    }
  ]

  ...
}
 ```

The above configuration will generate the records output like below,

```
+-------------------+----------------------------------+-------------------------------+---------+-------+-------------+--------+
|date_time          |incident                          |address                        |city     |zipcode|__tmst       |__status|
+-------------------+----------------------------------+-------------------------------+---------+-------+-------------+--------+
|2015-05-26 05:56:00|PENAL CODE/MISC (PENALMI)         |3900 Block BLOCK EL CAMINO REAL|PALO ALTO|94306  |1619986534335|bad     |
|2015-05-26 05:56:00|DRUNK IN PUBLIC ADULT/MISC (647FA)|3900 Block BLOCK EL CAMINO REAL|PALO ALTO|94306  |1619986534335|bad     |
|2015-05-26 05:56:00|PENAL CODE/MISC (PENALMI)         |3900 Block BLOCK EL CAMINO REAL|PALO ALTO|94306  |1619986534335|bad     |
|2015-05-26 05:56:00|PENAL CODE/MISC (PENALMI)         |3900 Block BLOCK EL CAMINO REAL|PALO ALTO|94306  |1619986534335|bad     |
|2015-05-26 08:00:00|N&D/POSSESSION (11350)            |WILKIE WAY & JAMES RD          |PALO ALTO|94306  |1619986534335|bad     |
|2015-05-26 08:00:00|N&D/PARAPHERNALIA (11364)         |WILKIE WAY & JAMES RD          |PALO ALTO|94306  |1619986534335|bad     |
|2015-05-26 08:00:00|TRAFFIC/SUSPENDED LICENSE (14601) |WILKIE WAY & JAMES RD          |PALO ALTO|94306  |1619986534335|bad     |
|2015-05-26 10:30:00|TRAFFIC/MISC (TRAFMISC)           |EL CAMINO REAL & UNIVERSITY AV |PALO ALTO|94301  |1619986534335|good    |
|2015-05-26 11:31:00|PENAL CODE/MISC (PENALMI)         |400 Block ALMA ST              |PALO ALTO|94301  |1619986534335|good    |
|2015-05-26 12:04:00|B&P/MISC (B&PMISC)                |700 Block URBAN LN             |PALO ALTO|94301  |1619986534335|good    |
|2015-05-26 12:25:00|PENAL CODE/MISC (PENALMI)         |500 Block HIGH ST              |PALO ALTO|94301  |1619986534335|good    |
|2015-05-26 13:06:00|WARRANT/OTHER AGENCY (OWARRANT)   |800 Block BRYANT ST            |PALO ALTO|94301  |1619986534335|good    |
|2015-05-26 13:30:00|THEFT GRAND/BIKE/BIKE PARTS (487B)|1 Block UNIVERSITY AV          |PALO ALTO|94301  |1619986534335|good    |
|2015-05-26 13:35:00|PENAL CODE/MISC (PENALMI)         |800 Block BRYANT ST            |PALO ALTO|94301  |1619986534335|good    |
|2015-05-26 14:30:00|F&W/BRANDISHING (417)             |2200 Block EL CAMINO REAL      |PALO ALTO|94306  |1619986534335|bad     |
|2015-05-26 14:43:00|ACCIDENT MINOR INJURY (1181)      |3300 Block BLOCK EL CAMINO REAL|PALO ALTO|94306  |1619986534335|bad     |
|2015-05-26 15:22:00|THEFT PETTY/MISC (488M)           |300 Block POE ST               |PALO ALTO|94301  |1619986534335|good    |
|2015-05-26 16:31:00|B&P/MISC (B&PMISC)                |500 Block WAVERLEY ST          |PALO ALTO|94301  |1619986534335|good    |
|2015-05-26 16:48:00|MUNI CODE/MISC (1090MISC)         |200 Block UNIVERSITY AV        |PALO ALTO|94301  |1619986534335|good    |
|2015-05-26 16:48:00|MUNI CODE/MISC (1090MISC)         |200 Block UNIVERSITY AV        |PALO ALTO|94301  |1619986534335|good    |
+-------------------+----------------------------------+-------------------------------+---------+-------+-------------+--------+
only showing top 20 rows
 ```

A new column `__status` has been added to the original data set on which this measure was executed. The value of this
column can be either `bad` or `good` which can be used to calculate the metrics/ separate data based on quality etc.
These values for `__status` column are based on the value of user-defined key `bad.record.definition`.

_Note:_ These outputs are for `ConsoleSink`.

**Further Reading**

Unlike the other measures, the record output of SparkSQL measure can be different from the source data set (in terms of
schema and/ or content). This is because the output is dependent on a user-defined query. In the example described
above, the query has been explicitly written in a way that the output and input data sets have the same schema.

Users can do the same by explicitly defining a boolean column (like `bad.record.definition`) with
alias `__measure_{measure_name}` directly in the SQL query and defining
`bad.record.definition: "__measure_{measure_name}"` in the measure config.

For example if the user-defined name of the measure is `spark_sql_measure`, the alias column name
becomes `__measure_spark_sql_measure`.
