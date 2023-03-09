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

# Apache Griffin Build Guide - Measure Module

Like other modules of Apache Griffin, `measure` module is also built using Maven build tool. Building `measure` module
requires Maven version 3.5+ and Java 8.

## Version Compatibility

Starting from Apache Griffin 0.7, the `measure` module will be (scala-spark) cross version compatible. Since both Scala
and Spark are dependencies for Apache Griffin, details of Spark-Scala cross version compatibility is mentioned below,

|           | Spark 2.3.x | Spark 2.4.x | Spark 3.0.x |
| --------- |:-----------:|:-----------:|:-----------:|
| Scala 2.11| ✓           | ✓           | x           |
| Scala 2.12| x           | ✓           | ✓           |

## Building a Distribution

Execute the below commands to build `measure` with desired version of Spark and Scala,

By default, the build is compiled with Scala 2.11 and Apache Spark 2.4.x.

```
# For measure module with Scala 2.11 and Spark 2.4.x
mvn clean package
```

To change Scala or Spark version you can use the commands below,

```
# For measure module with Scala 2.12 and Spark 2.4.x
mvn clean package -Dscala-2.12
```

```
# For measure module with Scala 2.11 and Spark 2.3.x
mvn clean package -Dspark-2.3
```

```
# For measure module with Scala 2.12 and Spark 3.0.x
mvn clean package -Dscala-2.12 -Dspark-3.0 
```

Note: 
 - Using `-Dscala-2.12` and `-Dspark-2.3` option together will cause build failure due to missing dependencies as it
is not cross compiled, see details [here](#version-compatibility)
 - Using `-Dspark-3.0` option without `-Dscala-2.12` option will cause build failure due to missing dependencies as it
   is not cross compiled, see details [here](#version-compatibility)

### AVRO Source Support

Starting Spark 2.4.x, AVRO source (`spark-avro` package) was migrated from `com.databricks` group to `org.apache.spark`.
Additionally, the older dependency does not support scala 2.12.

All builds of `measure` module will contain AVRO source support by default.
