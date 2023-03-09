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

SchemaConformance Measure - Configuration Guide
=====================================

### Introduction

Schema Conformance ensures that the attributes of a given dataset follow a set of standard definitions in terms of data
type. Most binary file formats (orc, avro, etc.) and tabular sources (Hive, RDBMS, etc.) already impose type constraints
on the data they represent. However, text based formats like csv, json, xml, etc. do not retain schema information. Such
formats must be explicitly validated for attribute type conformance.

For example, date of birth of customer should be a date, age should be an integer.

### Configuration

The schema conformance measure can be configured as below,

```json
{
  ...

  "measures": [
    {
      "name": "schema_conformance_measure",
      "type": "schemaConformance",
      "data.source": "crime_report_source",
      "config": {
        "expr": [
          {
            "source.col": "zipcode_str",
            "type": "int"
          }
        ]
      },
      "out": [
        {
          "type": "metric",
          "name": "schema_conformance_metric"
        },
        {
          "type": "record",
          "name": "schema_conformance_records"
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

`config` object for schema conformance measure contains only one key `expr`. The value for `expr` is a json array of
mapping objects where each object has 2 fields - `source.col` and `type`. Each `source.col` must exist in the source
data set and will be checked for type. This key is mandatory and `expr` array must not be empty i.e., at least one
mapping must be defined.

It can be defined as mentioned below,

```json
{
  ...

  "config": {
    "expr": [
      ...

      {
        "source.col": "zipcode_str",
        "type": "int"
      }

      ...
    ]
  }

  ...
}
 ```

### Outputs

Schema Conformance measure supports the following two outputs,

- Metrics
- Records

Users can choose to define any combination of these 2 outputs. For no outputs, skip this `out: [ ... ]` section from the
measure configuration.

#### Metrics Outputs

To write metrics for Schema Conformance measure, configure the measure with output section as below,

```json
{
  ...

  "out": [
    {
      "type": "metric",
      "name": "schema_conformance_metric"
    }
  ]

  ...
}
 ```

This will generate the metrics like below,

```json
{
  "applicationId": "local-1623452147576",
  "job_name": "Batch-All-Measures-Example",
  "tmst": 1623452154931,
  "measure_name": "schema_conformance_measure",
  "metrics": [
    {
      "metric_name": "total",
      "metric_value": "4617"
    },
    {
      "metric_name": "complete",
      "metric_value": "4459"
    },
    {
      "metric_name": "incomplete",
      "metric_value": "158"
    }
  ],
  "measure_type": "SchemaConformance",
  "data_source": "crime_report_source"
}
```

#### Record Outputs

To write records as output for Schema Conformance measure, configure the measure with output section as below,

```json
{
  ...

  "out": [
    {
      "type": "record",
      "name": "schema_conformance_records"
    }
  ]

  ...
}
 ```

The above configuration will generate the records output like below,

```
+-------------------+--------------------+--------------------+---------+-----------+-------+-------------+--------+
|          date_time|            incident|             address|     city|zipcode_str|zipcode|       __tmst|__status|
+-------------------+--------------------+--------------------+---------+-----------+-------+-------------+--------+
|2015-05-26 05:56:00|PENAL CODE/MISC (...|3900 Block BLOCK ...|PALO ALTO|      94306|  94306|1623452414131|    good|
|2015-05-26 05:56:00|DRUNK IN PUBLIC A...|3900 Block BLOCK ...|PALO ALTO|      94306|  94306|1623452414131|    good|
|2015-05-26 05:56:00|PENAL CODE/MISC (...|3900 Block BLOCK ...|PALO ALTO|      ""   |  null |1623452414131|    bad |
|2015-05-26 05:56:00|PENAL CODE/MISC (...|3900 Block BLOCK ...|PALO ALTO|      94306|  94306|1623452414131|    good|
|2015-05-26 08:00:00|N&D/POSSESSION (1...|WILKIE WAY & JAME...|PALO ALTO|      94306|  94306|1623452414131|    good|
|2015-05-26 08:00:00|N&D/PARAPHERNALIA...|WILKIE WAY & JAME...|PALO ALTO|      94306|  94306|1623452414131|    good|
|2015-05-26 08:00:00|TRAFFIC/SUSPENDED...|WILKIE WAY & JAME...|PALO ALTO|      94306|  94306|1623452414131|    good|
|2015-05-26 10:30:00|TRAFFIC/MISC (TRA...|EL CAMINO REAL & ...|PALO ALTO|      94301|  94301|1623452414131|    good|
|2015-05-26 11:31:00|PENAL CODE/MISC (...|   400 Block ALMA ST|PALO ALTO|      94301|  94301|1623452414131|    good|
|2015-05-26 12:04:00|  B&P/MISC (B&PMISC)|  700 Block URBAN LN|PALO ALTO|      94301|  94301|1623452414131|    good|
|2015-05-26 12:25:00|PENAL CODE/MISC (...|   500 Block HIGH ST|PALO ALTO|      94301|  94301|1623452414131|    good|
|2015-05-26 13:06:00|WARRANT/OTHER AGE...| 800 Block BRYANT ST|PALO ALTO|      94301|  94301|1623452414131|    good|
|2015-05-26 13:30:00|THEFT GRAND/BIKE/...|1 Block UNIVERSIT...|PALO ALTO|      94301|  94301|1623452414131|    good|
|2015-05-26 13:35:00|PENAL CODE/MISC (...| 800 Block BRYANT ST|PALO ALTO|      94301|  94301|1623452414131|    good|
|2015-05-26 14:30:00|F&W/BRANDISHING (...|2200 Block EL CAM...|PALO ALTO|      ""   |  null |1623452414131|    bad |
|2015-05-26 14:43:00|ACCIDENT MINOR IN...|3300 Block BLOCK ...|PALO ALTO|      94306|  94306|1623452414131|    good|
|2015-05-26 15:22:00|THEFT PETTY/MISC ...|    300 Block POE ST|PALO ALTO|      94301|  94301|1623452414131|    good|
|2015-05-26 16:31:00|  B&P/MISC (B&PMISC)|500 Block WAVERLE...|PALO ALTO|      94301|  94301|1623452414131|    good|
|2015-05-26 16:48:00|MUNI CODE/MISC (1...|200 Block UNIVERS...|PALO ALTO|      94301|  94301|1623452414131|    good|
|2015-05-26 16:48:00|MUNI CODE/MISC (1...|200 Block UNIVERS...|PALO ALTO|      94301|  94301|1623452414131|    good|
+-------------------+--------------------+--------------------+---------+-----------+-------+-------------+--------+
only showing top 20 rows
 ```

A new column `__status` has been added to the source data set that acted as input to this measure. The value of this
column can be either `bad` or `good` which can be used to calculate the metrics/ separate data based on quality etc.

_Note:_ These outputs are for `ConsoleSink`. 
