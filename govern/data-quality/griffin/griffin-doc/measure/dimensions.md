<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements. See the NOTICE file
distributed with this work for additional information
regarding copyright ownership. The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License. You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied. See the License for the
specific language governing permissions and limitations
under the License.
-->


Dimensions of Data Quality
==========================

In light of the management axiom “what gets measured gets managed” (Willcocks and Lester, 1996), the dimensions of Data
Quality signifies a crucial management element in the domain of data quality. Through time, researchers and
practitioners have suggested several views of data quality measures/ dimensions many of which have overlapping, and
sometimes conflicting interpretations.

While it is important to embrace the diversity of views of data quality measures/ dimensions, it is equally important
for the data quality research and practitioner community to be united in the consistent interpretation of this
foundational concept.

Apache Griffin, takes a step towards this consistent interpretation through a systematic review of research and
practitioner literature and measures/ assesses the quality of user-defined data assets in terms of the following
dimension/ measures,

- [Accuracy](#accuracy)
- [Completeness](#completeness)
- [Duplication](#duplication)
- [Custom User-defined (SparkSQL based)](#sparksql)
- [Profiling](#profiling)

## Accuracy

Data accuracy refers to the degree to which the values of a said attribute agrees with an identified reference truth
data (source of correct information). In-accurate data may come from different sources like,

- Dynamically computed values,
- the result of a manual workflow,
- irate customers, etc.

Accuracy measure quantifies the extent to which data sets contains are correct, reliable and certified values that are
free of error. Higher accuracy values signify that the said data set represents the “real-life” values/ objects that it
intends to model.

A detailed measure configuration guide is available [here](measure-configuration-guide/accuracy.md).

## Completeness

Completeness refers to the degree to which values are present in a data collection. When data is incomplete due to
unavailability (missing records), this does not represent a lack of completeness. As far as an individual datum is
concerned, only two situations are possible - either a value is assigned to the attribute in question or not. The latter
case is usually represented by a `null` value.

The definition of Completeness and its scope itself may change from one user to another. Thus, Apache Griffin allows
users to define SQL-like expressions which describe their definition of completeness. For a tabular data set with
columns `name`, `email` and `age`, some examples of such completeness definitions are mentioned below,

- `name is NULL`
- `name is NULL and age is NULL`
- `email NOT RLIKE '^[a-zA-Z0-9+_.-]+@[a-zA-Z0-9.-]+$'`

A detailed measure configuration guide is available [here](measure-configuration-guide/completeness.md).

## Duplication

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

A detailed measure configuration guide is available [here](measure-configuration-guide/duplication.md).

## SparkSQL

In some cases, the above-mentioned dimensions/ measures may not enough to model a complete data quality definition. For
such cases, Apache Griffin allows the definition of complex custom user-defined checks as SparkSQL queries.

SparkSQL measure is like a pro mode that allows advanced users to configure complex custom checks that are not covered
by other measures. These SparkSQL queries may contain clauses like `select`, `from`, `where`, `group-by`, `order-by`
, `limit`, etc.

A detailed measure configuration guide is available [here](measure-configuration-guide/sparksql.md).

## Schema Conformance

Schema Conformance ensures that the attributes of a given dataset follow a set of standard definitions in terms of data
type. Most binary file formats (orc, avro, etc.) and tabular sources (Hive, RDBMS, etc.) already impose type constraints
on the data they represent. However, text based formats like csv, json, xml, etc. do not retain schema information. Such
formats must be explicitly validated for attribute type conformance.

For example, date of birth of customer should be a date, age should be an integer.

A detailed measure configuration guide is available [here](measure-configuration-guide/schema_conformance.md).

## Profiling

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

Data Profiling helps us create a huge amount of insight into the quality levels of our data and helps to find data
quality rules and requirements that will support a more thorough data quality assessment in a later step.

A detailed measure configuration guide is available [here](measure-configuration-guide/profiling.md).
