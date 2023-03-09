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

Profiling Measure - Configuration Guide
=====================================

### Introduction

Data processing and its analysis can't truly be complete without data profiling - reviewing source data for content and
quality. Data profiling helps to find data quality rules and requirements that will support a more thorough data quality
assessment in a later step. Data profiling can help us to,

- **Discover Structure of data**

  Validating that data is consistent and formatted correctly, and performing mathematical checks on the data (e.g. sum,
  minimum or maximum). Structure discovery helps understand how well data is structured—for example, what percentage of
  phone numbers do not have the correct number of digits.

- **Discover Content of data**

  Looking into individual data records to discover errors. Content discovery identifies which specific rows in a table
  contain problems, and which systemic issues occur in the data (for example, phone numbers with no area code).

The process of Data profiling involves:

- Collecting descriptive statistics like min, max, count and sum
- Collecting data types, length and recurring patterns
- Discovering metadata and assessing its accuracy, etc.

A common problem in data management circles is the confusion around what is meant by Data profiling as opposed to Data
Quality Assessment due to the interchangeable use of these 2 terms.

Data Profiling helps us create a huge amount of insight into the quality levels of our data and helps to find data
quality rules and requirements that will support a more thorough data quality assessment in a later step. For example,
data profiling can help us to discover value frequencies, formats and patterns for each attribute in the data asset.
Using data profiling alone we can find some perceived defects and outliers in the data asset, and we end up with a whole
range of clues based on which correct Quality assessment measures can be defined like completeness/ distinctness etc.

### Configuration

The Profiling measure can be configured as below,

```json
{
  ...

  "measures": [
    {
      "name": "profiling_measure",
      "type": "profiling",
      "data.source": "crime_report_source",
      "config": {
        "expr": "city,zipcode",
        "approx.distinct.count": true,
        "round.scale": 2,
        "dataset.sample": 1.0
      },
      "out": [
        {
          "type": "metric",
          "name": "prof_metric",
          "flatten": "map"
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

`config` object for Profiling measure contains the following keys,

- `expr`: The value for `expr` is a comma separated string of columns in the data asset on which the profiling measure
  is to be executed. `expr` is an optional key for Profiling measure, i.e., if it is not defined, all columns in the
  data set will be profiled.

- `approx.distinct.count`: The value for this key is boolean. If this is `true`, the distinct counts will be
  approximated to allow up to 5% error. Approximate counts are usually faster but are less accurate. If this is set
  to `false`, then the counts will be 100% accurate.

- `round.scale`: Several resultant metrics of profiling measure are floating-point numbers. This key controls to extent
  to which these floating-point numbers are rounded. For example, if `round.scale = 2` then all floating-point metric
  values will be rounded to 2 decimal places.

- `dataset.sample`: The value of this key determines what percentage of data is to be profiled. The decimal value
  belongs to range [0.0, 1.0], where 0.0 means the whole dataset will be skipped, 1.0 means the whole dataset will be
  profiled. An intermediate value, say 0.5 will approximately take random 50% of the dataset rows (without replacement)
  and perform profiling on it. This option can be used when the dataset to be profiled is large, and an approximate
  profile is needed.

### Outputs

Unlike other measures, Profiling does not produce record outputs. Thus, only metric outputs must be configured.

#### Metrics Outputs

For each column in the data set, the profile contains the following,

- avg_col_len: Average length of the column value across all rows
- max_col_len: Maximum length of the column value across all rows
- min_col_len: Minimum length of the column value across all rows
- avg: Average column value across all rows
- max: Maximum column value across all rows
- min: Minimum column value across all rows
- null_count: Count of null values across all rows for this column
- approx_distinct_count **OR** distinct_count: Count of (approx) distinct values across all rows for this column
- variance: Variance measures variability from the average or mean.
- kurtosis: Kurtosis is a measure of whether the data are heavy-tailed or light-tailed relative to a normal
  distribution.
- std_dev: Standard deviation is a measure of the amount of variation or dispersion of a set of values.
- total: Total values across all rows. This is same for all columns.
- data_type: Data type of this column.

To write Profiling metrics, configure the measure with output section as below,

```json
{
  ...

  "out": [
    {
      "name": "prof_metric",
      "type": "metric"
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
  "tmst": 1623451547985,
  "measure_name": "profiling_measure",
  "metrics": [
    {
      "city": {
        "avg_col_len": null,
        "max_col_len": "25",
        "variance": null,
        "kurtosis": null,
        "avg": null,
        "min": null,
        "null_count": "0",
        "approx_distinct_count": "6",
        "total": "4617",
        "std_dev": null,
        "data_type": "string",
        "max": null,
        "min_col_len": "2"
      }
    },
    {
      "zipcode": {
        "avg_col_len": "5.0",
        "max_col_len": "5",
        "variance": "4.57",
        "kurtosis": "-1.57",
        "avg": "94303.11",
        "min": "94301",
        "null_count": "158",
        "approx_distinct_count": "4",
        "total": "4617",
        "std_dev": "2.14",
        "data_type": "int",
        "max": "94306",
        "min_col_len": "5"
      }
    }
  ],
  "measure_type": "Profiling",
  "data_source": "crime_report_source"
}
```

_Note:_ Some mathematical metrics are bound to the type of attribute under consideration, for example standard deviation
cannot be calculated for a column name of string type, thus, the value for these metrics are null for such columns.

_Note:_ This output is for `ConsoleSink`. 