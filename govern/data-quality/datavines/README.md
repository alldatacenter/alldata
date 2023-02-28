# DataVines

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
## Features of DataVines

* Easy to use
* Built in multiple Metric、ExpectedType、ResultFormula
  * [Metric Plugins](docs/plugin/en/metric/index.md)
  * [ExpectedType Plugins](docs/plugin/en/expected-value/index.md)
  * [ResultFormula Plugins](docs/plugin/en/result-formula/index.md)

* Modular and plug-in mechanism, easy to extend
  * [Engine Plugins](docs/plugin/en/engine/index.md)
  * [Connector Plugins](docs/plugin/en/connector/index.md)
  * [Register Plugins](docs/plugin/en/register/index.md)
  * [Notification Plugins](docs/plugin/en/notification/index.md)
* Support Spark 2.x、JDBC Engine

## Environmental dependency

1. java runtime environment: jdk8
2. If the data volume is small, or the goal is merely for functional verification, you can use JDBC engine
3. If you want to run DataVines based on Spark, you need to ensure that your server has spark installed
## Quick start
[QuickStart](docs/document/en/quick-start.md)

## Development
[Developer Guide](docs/development/en/index.md)

## Contribution

For guides on how to contribute, visit: [Contribution Guidelines](docs/community/en/index.md)

## RoadMap
[V1.0.0 RoadMap](docs/roadmap/en/roadmap-1.0.0.md)

## Contact Us
datavines@gmail.com

## License

DataVines is licensed under the [Apache License 2.0](LICENSE). DataVines relies on some third-party components, and their open source protocols are also Apache License 2.0 or compatible with Apache License 2.0. In addition, DataVines also directly references or modifies some codes in Apache DolphinScheduler, SeaTunnel and Dubbo, all of which are Apache License 2.0. Thanks for contributions to these projects.

## 官方项目地址
https://github.com/datavines-ops/datavines