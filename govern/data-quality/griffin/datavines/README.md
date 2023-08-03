<!--
  ~ Licensed to the Apache Software Foundation (ASF) under one or more
  ~ contributor license agreements.  See the NOTICE file distributed with
  ~ this work for additional information regarding copyright ownership.
  ~ The ASF licenses this file to You under the Apache License, Version 2.0
  ~ (the "License"); you may not use this file except in compliance with
  ~ the License.  You may obtain a copy of the License at
  ~
  ~    http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  ~
  -->

# Datavines
[![EN doc](https://img.shields.io/badge/document-English-blue.svg)](README.md)
[![CN doc](https://img.shields.io/badge/文档-中文版-blue.svg)](README.zh-CN.md)
---

Data quality is used to ensure the accuracy of data in the process of integration and processing. It is also the core component of DataOps. DataVines is an easy-to-use data quality service platform that supports multiple metric.

## Architecture Design
![DataVinesArchitecture](docs/img/architecture.jpg)

## Install

Need: Maven 3.6.1 and later
```sh
$ mvn clean package -Prelease -DskipTests
```
## Features

### Data Catalog

- Obtain **data source metadata** regularly to construct data directory 
- Regular monitoring of **metadata changes**
- **Tag management** with support for metadata

![Data Catalog](docs/img/data-catalog.jpg)

### Data Quality

- Built-in **27** data quality check rules
- Support **4** data quality check rule types
    - Single Table-Column Check
    - Single Table Custom `SQL` check 
    - Cross Table Accuracy Check
    - Two Table Value Comparison Check
- Support schedule tasks for check
- Support `SLA` for **check result alert**

![Data Quality](docs/img/data-quality.jpg)

### Data Profile

- Support timing execution of data detection, output **data profile report**
- Support **automatically identify** column types to automatically match appropriate data profile indicators
- Support **table row number trend** monitoring
- Support **data distribution** view

![数据目录](docs/img/data-profile.jpg)

### Plug-in Design

The platform is based on plug-in design, and the following modules support user-defined plug-ins to expand

- **Data Source**: `MySQL`, `Impala`, `Starocks`, `Doris`, `Presto`, `Trino`, `ClickHouse`, `PostgreSQL` are already supported
- **Check Rules**: 27 check rules such as built-in null value check, non-null check, enumeration check, etc.
- **Job Execution Engine**: Two execution engines `Spark` and `Local` have been supported. The `Spark` engine currently only supports the `Spark2.4` version, and the `Local` engine is a local execution engine developed based on `JDBC`, without relying on other execution engines.
- **Alert Channel**: Supported **Email**
- **Error Data Storage**: `MySQL` and **local files** are already supported (only `Local` execution engine is supported)
- **Registry**: Already supports `MySQL`, `PostgreSQL` and `ZooKeeper`

### Multiple Execute Modes

- Provide **Web page** to configure check jobs, run jobs, view job execution logs, view error data and check results
- Support **online generation** job running scripts, submit jobs through `datavines-submit.sh`, can be used in conjunction with the scheduling system

![作业脚本](docs/img/data-job-script.jpg)

### Easy Deployment & High Availability

- Less platform dependency, easy to deploy
- Minimal only rely on `MySQL` to start the project and complete the check of data quality operations
- Support horizontal expansion, automatic fault tolerance
- **Decentralized design**, `Server` node supports horizontal expansion to improve performance
- Job **Automatic Fault Tolerance**, to ensure that jobs are not lost or repeated

## Environmental Dependency

1. java runtime environment: jdk8
2. If the data volume is small, or the goal is merely for functional verification, you can use JDBC engine
3. If you want to run DataVines based on Spark, you need to ensure that your server has spark installed
## Quick Start
Click [Document](https://datavane.github.io/datavines-website/docs/user-guide/quick-start) for more information

## Development

Click [Document](https://datavane.github.io/datavines-website/docs/development/environment-preparation) for more information

## Contribution

[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](https://github.com/datavane/datavines/pulls)

You can submit any ideas as [pull requests](https://github.com/datavane/datavines/pulls) or as [GitHub issues](https://github.com/datavane/datavines/issues/new/choose).

> If you're new to posting issues, we ask that you read [*How To Ask Questions The Smart Way*](http://www.catb.org/~esr/faqs/smart-questions.html) (**This guide does not provide actual support services for this project!**), [How to Report Bugs Effectively](http://www.chiark.greenend.org.uk/~sgtatham/bugs.html) prior to posting. Well written bug reports help us help you!

Thank you to all the people who already contributed to Datavines!

[![contrib graph](https://contrib.rocks/image?repo=datavane/datavines)](https://github.com/datavane/datavines/graphs/contributors)

## License

Datavines is licensed under the [Apache License 2.0](LICENSE). Datavines relies on some third-party components, and their open source protocols are also Apache License 2.0 or compatible with Apache License 2.0. In addition, Datavines also directly references or modifies some codes in Apache DolphinScheduler, SeaTunnel and Dubbo, all of which are Apache License 2.0. Thanks for contributions to these projects.

## Social Media

- WeChat Official Account (in Chinese, scan the QR code to follow)

![wx-qrcode](docs/img/wechat-qrcode-en.jpg)

