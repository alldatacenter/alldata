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

Completeness Measure - Configuration Guide
=====================================

### Introduction

Completeness refers to the degree to which values are present in a data collection. When data is incomplete due to
unavailability (missing records), this does not represent a lack of completeness. As far as an individual datum is
concerned, only two situations are possible - either a value is assigned to the attribute in question or not. The latter
case is usually represented by a `null` value.

The definition of Completeness and its scope itself may change from one user to another. Thus, Apache Griffin allows
users to define SQL-like expressions which describe their definition of completeness. For a tabular data set with
columns `name`, `email` and `age`, some examples of such completeness definitions are mentioned below,

- `name is NOT NULL`
- `name is NOT NULL and age is NOT NULL`
- `email RLIKE '^[a-zA-Z0-9+_.-]+@[a-zA-Z0-9.-]+$'`

### Configuration

The completeness measure can be configured as below,

```json
{
  ...

  "measures": [
    {
      "name": "completeness_measure",
      "type": "completeness",
      "data.source": "crime_report_source",
      "config": {
        "expr": "zipcode is not null and city is not null"
      },
      "out": [
        {
          "type": "metric",
          "name": "comp_metric",
          "flatten": "map"
        },
        {
          "type": "record",
          "name": "comp_records"
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

`config` object for completeness measure contains only one key `expr`. The value for `expr` is a SQL-like expression
string which definition this completeness. For more complex definitions, expressions can be clubbed with `AND` and `OR`.

_Note:_ This expression describes the good or complete records. This means that for `"expr": "zipcode is NOT NULL"` the
records which contain `null` in zipcode column are incomplete.

It can be defined as mentioned below,

```json
{
  ...

  "config": {
    "expr": "zipcode is NOT null AND city is NOT null"
  }

  ...
}
 ```

### Outputs

Completeness measure supports the following two outputs,

- Metrics
- Records

Users can choose to define any combination of these 2 outputs. For no outputs, skip this `out: [ ... ]` section from the
measure configuration.

#### Metrics Outputs

To write metrics for completeness measure, configure the measure with output section as below,

```json
{
  ...

  "out": [
    {
      "name": "comp_metric",
      "type": "metric"
    }
  ]

  ...
}
 ```

This will generate the metrics like below,

```json
{
  "applicationId": "local-1623452412322",
  "job_name": "Batch-All-Measures-Example",
  "measure_name": "completeness_measure",
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
  "measure_type": "Completeness",
  "data_source": "crime_report_source"
}
```

#### Record Outputs

To write records as output for completeness measure, configure the measure with output section as below,

```json
{
  ...

  "out": [
    {
      "type": "record",
      "name": "comp_records"
    }
  ]

  ...
}
 ```

The above configuration will generate the records output like below,

```
+-------------------+---------------------------------------------+---------------------------------------+-------------+-------+-------------+--------+
|date_time          |incident                                     |address                                |city         |zipcode|__tmst       |__status|
+-------------------+---------------------------------------------+---------------------------------------+-------------+-------+-------------+--------+
|2015-05-26 05:56:00|PENAL CODE/MISC (PENALMI)                    |3900 Block BLOCK EL CAMINO REAL        |PALO ALTO    |94306  |1619969055893|good    |
|2015-05-26 05:56:00|DRUNK IN PUBLIC ADULT/MISC (647FA)           |3900 Block BLOCK EL CAMINO REAL        |PALO ALTO    |94306  |1619969055893|good    |
|2015-05-26 05:56:00|PENAL CODE/MISC (PENALMI)                    |3900 Block BLOCK EL CAMINO REAL        |PALO ALTO    |94306  |1619969055893|good    |
|2015-05-26 22:55:00|DRIVER'S LICENSE SUSPENDED/ALC (14601.2(A)VC)|EL CAMINO REAL & N SAN ANTONIO RD      |""           |null   |1619969055893|bad     |
|2015-05-26 22:55:00|TRAFFIC/SUSPENDED LICENSE (14601)            |EL CAMINO REAL & N SAN ANTONIO RD      |""           |null   |1619969055893|bad     |
|2015-06-01 01:41:00|TRAFFIC/SUSPENDED LICENSE (14601)            |QUARRY RD & ARBORETUM RD               |PALO ALTO    |94304  |1619969055893|good    |
|2015-06-01 02:49:00|TRAFFIC/SUSPENDED LICENSE (14601)            |2000 Block BLOCK E BAYSHORE RD         |PALO ALTO    |94303  |1619969055893|good    |
|2015-06-01 03:13:00|DRIVING WITH A SUSPENDED OR RE (14601.1(A)VC)|100 Block SAN ANTONIO RD               |PALO ALTO    |null   |1619969055893|bad     |
|2015-06-01 03:13:00|TRAFFIC/SUSPENDED LICENSE (14601)            |100 Block SAN ANTONIO RD               |PALO ALTO    |null   |1619969055893|bad     |
|2015-06-01 03:13:00|WARRANT/PALO ALTO (PWARRANT)                 |100 Block SAN ANTONIO RD               |PALO ALTO    |null   |1619969055893|bad     |
|2015-06-01 16:20:00|BURGLARY ATTEMPT/AUTO (459AA)                |300 Block LOWELL AV                    |PALO ALTO    |94301  |1619969055893|good    |
|2015-06-01 16:30:00|BURGLARY/AUTO (459A)                         |800 Block EL CAMINO REAL               |PALO ALTO    |94301  |1619969055893|good    |
|2015-06-01 16:30:00|ASSAULT WITH DEADLY WEAPON (245)             |1100 Block N RENGSTORFF AV             |MOUNTAIN VIEW|null   |1619969055893|bad     |
|2015-06-01 16:30:00|BURGLARY/AUTO (459A)                         |2000 Block EL CAMINO REAL              |PALO ALTO    |94306  |1619969055893|good    |
|2015-06-01 16:30:00|TRAFFIC/SUSPENDED LICENSE (14601)            |1100 Block N RENGSTORFF AV             |MOUNTAIN VIEW|null   |1619969055893|bad     |
|2015-06-01 16:30:00|PENAL CODE/RESISTING ARREST (148RA)          |1100 Block N RENGSTORFF AV             |MOUNTAIN VIEW|null   |1619969055893|bad     |
|2015-06-01 16:30:00|BURGLARY/AUTO (459A)                         |1100 Block N RENGSTORFF AV             |MOUNTAIN VIEW|null   |1619969055893|bad     |
|2015-06-01 16:30:00|ASSAULT WITH DEADLY WEAPON (245)             |1100 Block N RENGSTORFF AV             |MOUNTAIN VIEW|null   |1619969055893|bad     |
|2015-06-01 17:30:00|IDENTITY THEFT/MISC. (530M)                  |300 Block CREEKSIDE DR                 |PALO ALTO    |94306  |1619969055893|good    |
|2015-06-01 17:30:00|PENAL CODE/TERRORIST THREATS (422)           |100 Block CALIFORNIA AV                |PALO ALTO    |94306  |1619969055893|good    |
+-------------------+---------------------------------------------+---------------------------------------+-------------+-------+-------------+--------+
only showing top 20 rows
 ```

A new column `__status` has been added to the original data set on which this measure was executed. The value of this
column can be either `bad` or `good` which can be used to calculate the metrics/ separate data based on quality etc.

_Note:_ These outputs are for `ConsoleSink`. 