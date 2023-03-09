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

Accuracy Measure - Configuration Guide
=====================================

### Introduction

Data accuracy refers to the degree to which the values of a said attribute in a data source agree with an identified
reference truth data (source of correct information). In-accurate data may come from different sources like,

- Dynamically computed values,
- the result of a manual workflow,
- irate customers, etc.

Accuracy measure quantifies the extent to which data sets contains are correct, reliable and certified values that are
free of error. Higher accuracy values signify that the said data set represents the “real-life” values/ objects that it
intends to model.

Accuracy measure is comparative in nature - attributes of data source to be checked are compared with attributes of
another reference source. Thus, unlike other measures/ dimensions, Accuracy relies on definition of 2 sources,

- the reference (truth) source which contains the good/ correct/ accurate values.
- the actual data source to be assessed and measured for data accuracy.

### Configuration

The accuracy measure can be configured as below,

```json
{
  ...

  "measures": [
    {
      "name": "accuracy_measure",
      "type": "accuracy",
      "data.source": "crime_report_source",
      "config": {
        "ref.source": "crime_report_truth",
        "expr": [
          {
            "source.col": "city",
            "ref.col": "city_name"
          }
        ]
      },
      "out": [
        {
          "type": "metric",
          "name": "accuracy_metric",
          "flatten": "map"
        },
        {
          "type": "record",
          "name": "accuracy_records"
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

`config` object for Accuracy measure contains the following keys,

- `expr`: The value for `expr` is a json array of comparison objects where each object has 2 fields `source.col`
  and `ref.col` which must be actual columns in the source and reference data sets respectively. This key is mandatory
  and `expr` array must not be empty i.e. at least one comparison must be defined.

- `ref.source`: This is a mandatory parameter which selects the data source which will be used as reference. This is a
  mandatory parameter and this data source must be defined in the sources section of the application configuration.

_Note:_ This expression describes the accurate records. This means that for all records where `expr` is true, the row is
marked as accurate, and rest of the records are in-accurate.

The conditions of attribute comparison are defined by users, and it looks something like,

```json
{
  ...

  "config": {
    "ref.source": "crime_report_truth",
    "expr": [
      {
        "source.col": "city",
        "ref.col": "city_name"
      }
    ]
  }

  ...
}
```

This expression means that the users is trying to access the accuracy of `city` column in the data source under
assessment against the accurate values in `city_name` column of the reference data source. For understand, in SQL this
will boil down to `city == city_name`.

Since `expr` is an array, users can define as many comparisons as they require. In case of multiple conditions, the
overall rule representation in terms of SQL becomes something like,
`s1 == r1 AND s2 == r2 AND sn == rn`, where `sn` and `rn` are all actual columns.

Note: The values of `source.col` and `ref.col` must be actual column names in the source and reference data sets
respectively.

### Outputs

Accuracy measure supports the following two outputs,

- Metrics
- Records

Users can choose to define any combination of these 2 outputs. For no outputs, skip this `out: [ ... ]` section from the
measure configuration.

#### Metrics Outputs

To write metrics for accuracy measure, configure the measure with output section as below,

```json
{
  ...

  "out": [
    {
      "type": "metric",
      "name": "accuracy_metric"
    }
  ]

  ...
}
 ```

This will generate the metrics like below,

```json
{
  "applicationId": "local-1623451540444",
  "job_name": "Batch-All-Measures-Example",
  "tmst": 1623451547986,
  "measure_name": "accuracy_measure",
  "metrics": [
    {
      "metric_name": "total",
      "metric_value": "4617"
    },
    {
      "metric_name": "accurate",
      "metric_value": "4511"
    },
    {
      "metric_name": "inaccurate",
      "metric_value": "106"
    }
  ],
  "measure_type": "Accuracy",
  "data_source": "crime_report_source"
}
```

#### Record Outputs

To write records as output for accuracy measure, configure the measure with output section as below,

```json
{
  ...

  "out": [
    {
      "type": "record",
      "name": "accuracy_records"
    }
  ]

  ...
}
 ```

The above configuration will generate the records output like below,

```
+-------------------+---------------------------------------------+---------------------------------+-------------+-------+-------------+--------+
|date_time          |incident                                     |address                          |city         |zipcode|__tmst       |__status|
+-------------------+---------------------------------------------+---------------------------------+-------------+-------+-------------+--------+
|2015-05-26 20:15:00|F&W/DISPOSAL REQUEST (F&WDISPS)              |200 Block FOREST AV              |PALO ALTO    |94301  |1622231569966|good    |
|2015-05-26 20:45:00|ACCIDENT PROPERTY DAMAGE (1182)              |MIDDLEFIELD RD & SAN ANTONIO RD  |PALO ALTO    |94303  |1622231569966|good    |
|2015-05-26 22:55:00|DRIVER'S LICENSE SUSPENDED/ALC (14601.2(A)VC)|EL CAMINO REAL & N SAN ANTONIO RD|""           |null   |1622231569966|bad     |
|2015-05-26 22:55:00|TRAFFIC/SUSPENDED LICENSE (14601)            |EL CAMINO REAL & N SAN ANTONIO RD|""           |null   |1622231569966|bad     |
|2015-05-26 23:40:00|BURGLARY/AUTO (459A)                         |300 Block EMERSON ST             |PALO ALTO    |94301  |1622231569966|good    |
|2015-05-27 00:30:00|BURGLARY/AUTO (459A)                         |1900 Block EL CAMINO REAL        |PALO ALTO    |94306  |1622231569966|good    |
|2015-05-30 19:28:00|SICK & CARED FOR/MISC (1053M)                |100 Block LOIS LN                |PALO ALTO    |94303  |1622231569966|good    |
|2015-05-30 19:30:00|BURGLARY/AUTO (459A)                         |100 Block EL CAMINO REAL         |PALO ALTO    |94304  |1622231569966|good    |
|2015-05-30 20:50:00|BURGLARY/AUTO (459A)                         |100 Block EL CAMINO REAL         |PALO ALTO    |94304  |1622231569966|good    |
|2015-05-30 20:54:00|BURGLARY/AUTO (459A)                         |400 Block E OKEEFE ST            |""           |94303  |1622231569966|bad     |
|2015-05-30 20:54:00|BURGLARY/AUTO (459A)                         |400 Block E OKEEFE ST            |""           |94303  |1622231569966|bad     |
|2015-05-30 21:17:00|BURGLARY/AUTO (459A)                         |2000 Block EL CAMINO REAL        |PALO ALTO    |94306  |1622231569966|good    |
|2015-05-30 21:17:00|CREDIT CARDS/FRAUDULENT USE (484G)           |2000 Block EL CAMINO REAL        |PALO ALTO    |94306  |1622231569966|good    |
|2015-06-01 16:00:00|ACCIDENT PROPERTY DAMAGE (1182)              |3100 Block BLOCK MIDDLEFIELD RD  |PALO ALTO    |94306  |1622231569966|good    |
|2015-06-01 16:00:00|BURGLARY/AUTO (459A)                         |4200 Block EL CAMINO REAL        |PALO ALTO    |94306  |1622231569966|good    |
|2015-06-01 16:20:00|BURGLARY ATTEMPT/AUTO (459AA)                |300 Block LOWELL AV              |PALO ALTO    |94301  |1622231569966|good    |
|2015-06-01 16:30:00|BURGLARY/AUTO (459A)                         |800 Block EL CAMINO REAL         |PALO ALTO    |94301  |1622231569966|good    |
|2015-06-01 16:30:00|ASSAULT WITH DEADLY WEAPON (245)             |1100 Block N RENGSTORFF AV       |MOUNTAIN VIEW|null   |1622231569966|bad     |
|2015-06-01 16:30:00|BURGLARY/AUTO (459A)                         |2000 Block EL CAMINO REAL        |PALO ALTO    |94306  |1622231569966|good    |
|2015-06-01 16:30:00|TRAFFIC/SUSPENDED LICENSE (14601)            |1100 Block N RENGSTORFF AV       |MOUNTAIN VIEW|null   |1622231569966|bad     |
+-------------------+---------------------------------------------+---------------------------------+-------------+-------+-------------+--------+
only showing top 20 rows
 ```

A new column `__status` has been added to the original data set on which this measure was executed. The value of this
column can be either `bad` or `good` which can be used to calculate the metrics/ separate data based on quality etc.

_Note:_ These outputs are for `ConsoleSink`. 