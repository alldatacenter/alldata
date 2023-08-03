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


# [A one-stop, full-scenario integration framework for massive data](https://inlong.apache.org/)
[![GitHub Actions](https://github.com/apache/inlong/actions/workflows/ci_build.yml/badge.svg)](https://github.com/apache/inlong/actions)
[![CodeCov](https://codecov.io/gh/apache/inlong/branch/master/graph/badge.svg)](https://codecov.io/gh/apache/inlong)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.inlong/inlong/badge.svg)](http://search.maven.org/#search%7Cga%7C1%7Corg.apache.inlong)
[![GitHub release](https://img.shields.io/badge/release-download-orange.svg)](https://inlong.apache.org/download/)
[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![Twitter](https://img.shields.io/twitter/url?label=Follow%20ApacheInlong&style=social&url=https%3A%2F%2Ftwitter.com%2FApacheInlong)](https://twitter.com/ApacheInlong)
[![Slack](https://img.shields.io/badge/%20Slack-InLong%20Channel-blueviolet)](https://the-asf.slack.com/archives/C01QAG6U00L)

- [What is Apache InLong?](#what-is-apache-inlong)
- [Features](#features)
- [When should I use InLong?](#when-should-i-use-inlong)
- [Build InLong](#build-inlong)
- [Deploy InLong](#deploy-inlong)
- [Contribute to InLong](#contribute-to-inlong)
- [Contact Us](#contact-us)
- [Documentation](#documentation)
- [License](#license)

# What is Apache InLong?
|                                       **Stargazers Over Time**                                        |                                                                                                **Contributors Over Time**                                                                                                |
|:-----------------------------------------------------------------------------------------------------:|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|
| [![Stargazers over time](https://starchart.cc/apache/inlong.svg)](https://starchart.cc/apache/inlong) | [![Contributor Over Time](https://contributor-overtime-api.git-contributor.com/contributors-svg?chart=contributorOverTime&repo=apache/inlong)](https://git-contributor.com?chart=contributorOverTime&repo=apache/inlong) |

[Apache InLong](https://inlong.apache.org) is a one-stop, full-scenario integration framework for massive data that supports `Data Ingestion`, `Data Synchronization` and `Data Subscription`, and it provides automatic, secure and reliable data transmission capabilities. InLong also supports both batch and stream data processing at the same time, which offers great power to build data analysis, modeling and other real-time  applications based on streaming data.

InLong (应龙) is a divine beast in Chinese mythology who guides the river into the sea, and it is regarded as a metaphor of the InLong system for reporting data streams.

InLong was originally built at Tencent, which has served online businesses for more than 8 years, to support massive data (data scale of more than 80 trillion pieces of data per day) reporting services in big data scenarios. The entire platform has integrated 5 modules:  Ingestion, Convergence, Caching, Sorting, and Management, so that the business only needs to provide data sources, data service quality, data landing clusters and data landing formats, that is, the data can be continuously pushed from the source to the target cluster, which greatly meets the data reporting service requirements in the business big data scenario.

For getting more information, please visit our project documentation at https://inlong.apache.org/.
![inlong-structure-en.png](https://github.com/apache/inlong-website/blob/master/static/img/inlong-structure-en.png)


## Features
Apache InLong offers a variety of features:
* **Ease of Use**: a SaaS-based service platform. Users can easily and quickly report, transfer, and distribute data by publishing and subscribing to data based on topics.
* **Stability & Reliability**: derived from the actual online production environment. It delivers high-performance processing capabilities for 10 trillion-level data streams and highly reliable services for 100 billion-level data streams.
* **Comprehensive Features**: supports various types of data access methods and can be integrated with different types of Message Queue (MQ). It also provides real-time data extract, transform, and load (ETL) and sorting capabilities based on rules. InLong also allows users to plug features to extend system capabilities.
* **Service Integration**: provides unified system monitoring and alert services. It provides fine-grained metrics to facilitate data visualization. Users can view the running status of queues and topic-based data statistics in a unified data metric platform. Users can also configure the alert service based on their business requirements so that users can be alerted when errors occur.
* **Scalability**: adopts a pluggable architecture that allows you to plug modules into the system based on specific protocols. Users can replace components and add features based on their business requirements.


## When should I use InLong?
InLong aims to provide a one-stop, full-scenario integration framework for massive data, users can easily build stream-based data applications. It supports `Data Ingestion`, `Data Synchronization` and `Data Subscription` at the same time, and is suitable for environments that need to quickly build a data reporting platform, as well as an ultra-large-scale data reporting environment that InLong is very suitable for, and an environment that needs to automatically sort and land the reported data.

You can use InLong in the following ways：
- Integrate InLong, manage data streams through [SDK](https://inlong.apache.org/docs/next/sdk/manager-sdk/example).
- Use [the InLong command-line tool](https://inlong.apache.org/docs/next/user_guide/command_line_tools) to view and create data streams.
- Visualize your operations on [InLong dashboard](https://inlong.apache.org/docs/next/user_guide/dashboard_usage).

## Supported Data Nodes (Updating)
| Type         | Name              | Version                      |
|--------------|-------------------|------------------------------|
| Extract Node | Auto Push         | None                         |
|              | File              | None                         |
|              | Kafka             | 2.x                          |
|              | MongoDB           | >= 3.6                       |
|              | MQTT              | >= 3.1                       |
|              | MySQL             | 5.6, 5.7, 8.0.x              |
|              | Oracle            | 11,12,19                     |
|              | PostgreSQL        | 9.6, 10, 11, 12              |
|              | Pulsar            | 2.8.x                        |
|              | Redis             | 2.6.x                        |
|              | SQLServer         | 2012, 2014, 2016, 2017, 2019 |
| Load Node    | Auto Consumption  | None                         |
|              | ClickHouse        | 20.7+                        |
|              | Elasticsearch     | 6.x, 7.x                     |
|              | Greenplum         | 4.x, 5.x, 6.x                |
|              | HBase             | 2.2.x                        |
|              | HDFS              | 2.x, 3.x                     |
|              | Hive              | 1.x, 2.x, 3.x                |
|              | Iceberg           | 0.12.x                       |
|              | Hudi              | 0.12.x                       |
|              | Kafka             | 2.x                          |
|              | MySQL             | 5.6, 5.7, 8.0.x              |
|              | Oracle            | 11, 12, 19                   |
|              | PostgreSQL        | 9.6, 10, 11, 12              |
|              | SQLServer         | 2012, 2014, 2016, 2017, 2019 |
|              | TDSQL-PostgreSQL  | 10.17                        |
|              | Doris             | >= 0.13                      |
|              | StarRocks         | >= 2.0                       |
|              | Kudu              | >= 1.12.0                    |
|              | Redis             | >= 3.0                       |

## Build InLong
More detailed instructions can be found at [Quick Start](https://inlong.apache.org/docs/next/quick_start/how_to_build) section in the documentation.

Requirements:
- Java [JDK 8](https://adoptopenjdk.net/?variant=openjdk8)
- Maven 3.6.1+
- [Docker](https://docs.docker.com/engine/install/) 19.03.1+

CodeStyle:

```
mvn spotless:apply
```

Compile and install:
```
mvn clean install -DskipTests
```
(Optional) Compile using docker image:
```
docker pull maven:3.6-openjdk-8
docker run -v `pwd`:/inlong  -w /inlong maven:3.6-openjdk-8 mvn clean install -DskipTests
```
after compile successfully, you could find distribution file at `inlong-distribution/target`.

## Deploy InLong
- [Standalone for InLong](https://inlong.apache.org/docs/next/deployment/standalone)
- [Docker Compose](https://inlong.apache.org/docs/next/deployment/docker)
- [InLong on Kubernetes](https://inlong.apache.org/docs/next/deployment/k8s)
- [Bare Metal](https://inlong.apache.org/docs/next/deployment/bare_metal)

## Develop InLong
- [Agent Plugin extends a Extract Data Node](https://inlong.apache.org/docs/next/design_and_concept/how_to_write_plugin_agent)
- [Sort Plugin extends a Data Node](https://inlong.apache.org/docs/next/design_and_concept/how_to_extend_data_node_for_sort)
- [Manager Plugin extends a Data Node](https://inlong.apache.org/docs/next/design_and_concept/how_to_extend_data_node_for_manager)
- [Dashboard Plugin extends a Data Node page](https://inlong.apache.org/docs/next/design_and_concept/how_to_write_plugin_dashboard)

## Contribute to InLong
- Report any issue on [GitHub Issue](https://github.com/apache/inlong/issues)
- Code pull request according to [How to contribute](https://inlong.apache.org/community/how-to-contribute).

## Contact Us
- Join Apache InLong mailing lists:
    | Name                                                                          | Scope                           |                                                                 |                                                                     |                                                                              |
    |:------------------------------------------------------------------------------|:--------------------------------|:----------------------------------------------------------------|:--------------------------------------------------------------------|:-----------------------------------------------------------------------------|
    | [dev@inlong.apache.org](mailto:dev@inlong.apache.org)     | Development-related discussions | [Subscribe](mailto:dev-subscribe@inlong.apache.org)   | [Unsubscribe](mailto:dev-unsubscribe@inlong.apache.org)   | [Archives](http://mail-archives.apache.org/mod_mbox/inlong-dev/)   |
- Ask questions on [Apache InLong Slack](https://the-asf.slack.com/archives/C01QAG6U00L)

## Documentation
- Home page: https://inlong.apache.org/
- Issues: https://github.com/apache/inlong/issues

## License
© Contributors Licensed under an [Apache-2.0](LICENSE) license.


