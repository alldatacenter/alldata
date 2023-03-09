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

Duplication Measure - Configuration Guide
=====================================

### Introduction

Asserting the measure of duplication of the entities within a data set implies that no entity exists more than once
within the data set and that there is a key that can be used to uniquely access each entity. For example, in a master
product table, each product must appear once and be assigned a unique identifier that represents that product within a
system or across multiple applications/ systems.

Redundancies in a dataset can be measured in terms of the following metrics,

- **Duplicate:** the number of values that are the same as other values in the list
- **Distinct:** the number of non-null values that are different from each other (Non-unique + Unique)
- **Non-Unique:** the number of values that have at least one duplicate in the list
- **Unique:** the number of values that have no duplicates

Duplication measure in Apache Griffin computes all of these metrics for a user-defined data asset.

As an example, consider the data set below,

```
+---+-------------+------+
|id |name         |gender|
+---+-------------+------+
|1  |John Smith   |Male  |
|2  |John Smith   |Male  |
|3  |Rebecca Davis|Female|
|4  |Paul Adams   |Male  |
|5  |null         |null  |
+---+-------------+------+
```

From the above definitions of redundancy measurement following results are generated (considering only `name` column),

| Metric    | Count | Records         | Explanation                                                |
|:----------|:-----:|:----------------|:-----------------------------------------------------------|
|Duplicate  | 1     | Record 2        | There is one duplicate of the John Smith record (Record 1) |
|Distinct   | 3     | Records 1, 3, 4 | These records contain distinct values                      |
|Non-unique | 1     | Record 1        | John Smith has a duplicate record - it isn't unique        |
|Unique     | 2     | Records 3 and 4 | Rebecca Davis and Paul Adams appear only once in the list, they have no duplicates |

_Note:_ The above results are when only `name` column is measured by duplication measure. Results for whole row would be
different.

### Configuration

The duplication measure can be configured as below,

```json
{
  ...

  "measures": [
    {
      "name": "duplication_measure",
      "type": "duplication",
      "data.source": "crime_report_source",
      "config": {
        "expr": "incident",
        "bad.record.definition": "duplicate"
      },
      "out": [
        {
          "type": "metric",
          "name": "duplication_metric",
          "flatten": "map"
        },
        {
          "type": "record",
          "name": "duplication_records"
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

`config` object for duplication measure contains the following keys,

- `expr`: The value for `expr` is a comma separated string of columns in the data asset on which the duplication measure
  is to be executed. `expr` is an optional key for Duplication measure, i.e., if it is not defined, the entire row will
  be checked by duplication measure.

- `bad.record.definition`: As the key suggests, its value defines what exactly would be considered as a bad record after
  this measure computes redundancies on the data asset. Since the redundancies are calculated as `duplicate`, `unique`
  , `non_unique`, and  `distinct`, the value of this key must also be one of these values. This key is mandatory and
  must be defined with appropriate value.

  _Note:_ This expression describes the bad records. This means that for the config,
  ```json
  {
    ...
    
    "config": {
      "expr": "incident",
      "bad.record.definition": "duplicate"
    }

    ...
  }
  ```
  the records with duplicate value in `incident` column will be considered bad.

  If `expr` was not defined then the duplicate records themselves will be considered bad.

### Outputs

Duplication measure supports the following two outputs,

- Metrics
- Records

Users can choose to define any combination of these 2 outputs. For no outputs, skip this `out: [ ... ]` section from the
measure configuration.

#### Metrics Outputs

To write metrics for duplication measure, configure the measure with output section as below,

```json
{
  ...

  "out": [
    {
      "name": "duplication_metric",
      "type": "metric"
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
  "tmst": 1623452154930,
  "measure_name": "duplication_measure",
  "metrics": [
    {
      "metric_name": "total",
      "metric_value": "4617"
    },
    {
      "metric_name": "duplicate",
      "metric_value": "4363"
    },
    {
      "metric_name": "unique",
      "metric_value": "58"
    },
    {
      "metric_name": "non_unique",
      "metric_value": "196"
    },
    {
      "metric_name": "distinct",
      "metric_value": "254"
    }
  ],
  "measure_type": "Duplication",
  "data_source": "crime_report_source"
}
```

#### Record Outputs

To write records as output for duplication measure, configure the measure with output section as below,

```json
{
  ...

  "out": [
    {
      "type": "record",
      "name": "duplication_records"
    }
  ]

  ...
}
 ```

The above configuration will generate the records output like below,

```
+-------------------+-----------------------------------------+----------------------------------+-------------+-------+-------------+--------+
|date_time          |incident                                 |address                           |city         |zipcode|__tmst       |__status|
+-------------------+-----------------------------------------+----------------------------------+-------------+-------+-------------+--------+
|2015-10-30 03:32:00|MUNI CODE/MISC                           |400 Block LYTTON AV               |PALO ALTO    |94301  |1619970144048|good    |
|2015-10-31 23:00:00|MUNI CODE/MISC                           |1 Block ENCINA AV                 |PALO ALTO    |94301  |1619970144048|bad     |
|2015-10-31 23:10:00|MUNI CODE/MISC                           |LYTTON AV & BRYANT ST             |PALO ALTO    |94301  |1619970144048|bad     |
|2016-02-07 07:38:00|MUNI CODE/MISC                           |200 Block UNIVERSITY AV           |PALO ALTO    |94301  |1619970144048|bad     |
|2015-05-31 07:27:00|THEFT PETTY ATT/FROM AUTO (488FAA)       |2000 Block EMERSON ST             |PALO ALTO    |94301  |1619970144048|good    |
|2015-06-02 16:00:00|OUTSIDE INVESTIGATION/MISC (OUTSIDE)     |700 Block CEREZA DR               |PALO ALTO    |94306  |1619970144048|good    |
|2015-10-31 07:21:00|VEHICLE/IMPOUNDED                        |400 Block BRYANT ST               |PALO ALTO    |94301  |1619970144048|good    |
|2016-02-02 16:50:00|VEHICLE/IMPOUNDED                        |1400 Block BLOCK EL CAMINO REAL   |PALO ALTO    |null   |1619970144048|bad     |
|2016-02-04 07:14:00|VEHICLE/IMPOUNDED                        |EMBARCADERO RD & GREER RD         |PALO ALTO    |94303  |1619970144048|bad     |
|2015-05-27 05:00:00|FOUND PROPERTY/MISC (FOUND)              |HAMILTON AV & WAVERLEY ST         |PALO ALTO    |94301  |1619970144048|good    |
|2015-05-29 20:30:00|FOUND PROPERTY/MISC (FOUND)              |700 Block GREER RD                |PALO ALTO    |94303  |1619970144048|bad     |
|2015-05-30 22:50:00|FOUND PROPERTY/MISC (FOUND)              |600 Block GUINDA ST               |PALO ALTO    |94301  |1619970144048|bad     |
|2015-06-01 18:30:00|FOUND PROPERTY/MISC (FOUND)              |200 Block UNIVERSITY AV           |PALO ALTO    |94301  |1619970144048|bad     |
|2015-06-01 22:30:00|FOUND PROPERTY/MISC (FOUND)              |300 Block HOMER AV                |PALO ALTO    |94301  |1619970144048|bad     |
|2015-10-20 13:30:00|FOUND PROPERTY/MISC (FOUND)              |2600 Block MIDDLEFIELD RD         |PALO ALTO    |94306  |1619970144048|bad     |
|2015-10-21 23:30:00|FOUND PROPERTY/MISC (FOUND)              |3200 Block BLOCK E BAYSHORE RD    |PALO ALTO    |94303  |1619970144048|bad     |
|2015-09-28 06:35:00|STALKING/MISC (6469)                     |700 Block DE SOTO DR              |PALO ALTO    |94303  |1619970144048|good    |
|2015-07-02 20:23:00|DRUNK IN PUBLIC (647(F)PC)               |800 Block EL CAMINO REAL          |PALO ALTO    |94301  |1619970144048|good    |
|2015-08-01 01:30:00|DRUNK IN PUBLIC (647(F)PC)               |500 Block HIGH ST                 |PALO ALTO    |94301  |1619970144048|bad     |
|2015-07-11 21:56:00|OUTSIDE WARRANT (O/W-MISD)               |3500 Block BLOCK EL CAMINO REAL   |PALO ALTO    |94306  |1619970144048|good    |
+-------------------+-----------------------------------------+----------------------------------+-------------+-------+-------------+--------+
only showing top 20 rows
 ```

A new column `__status` has been added to the original data set on which this measure was executed. The value of this
column can be either `bad` or `good` which can be used to calculate the metrics/ separate data based on quality etc.
These values for `__status` column are based on the value of user-defined key `bad.record.definition`.

_Note:_ These outputs are for `ConsoleSink`. 