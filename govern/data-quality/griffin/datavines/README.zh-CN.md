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

Datavines 是一站式开源数据可观测性平台，提供元数据管理、数据概览报告、数据质量管理，数据分布查询、数据趋势洞察等核心能力，致力于帮助用户全面地了解和掌管数据，让您做到心中有数。

## 架构设计
![DataVinesArchitecture](docs/img/architecture.jpg)
## 安装

使用`Maven3.6.1`以及以上版本
```sh
$ mvn clean package -Prelease -DskipTests
```

## 特性

### 数据目录

*   定时获取**数据源元数据**，构造数据目录
*   定时监听**元数据变更**情况
*   支持元数据的**标签管理**

![数据目录](docs/img/data-catalog.jpg)

### 数据质量监控

- 内置 **27** 个数据质量检查规则，开箱即用
- 支持 **4** 种数据质量检查规则类型
  - 单表单列检查类型
  - 单表自定义` SQL `检查类型
  - 跨表准确性检查类型
  - 两表值比对检查类型
- 支持配置定时任务进行**定时检查**
- 支持配置 `SLA `用于**检查结果告警**

![数据质量检查](docs/img/data-quality.jpg)

### 数据概览

- 支持定时执行数据探测，输出**数据概览报告**
- 支持**自动识别**列的类型自动匹配合适的数据概况指标 
- 支持**表行数趋势**监控 
- 支持列的**数据分布**情况查看

![数据目录](docs/img/data-profile.jpg)

### 插件化设计

平台以插件化设计为核心，以下模块都支持用户`自定义插件`进行扩展

- **数据源**：已支持 `MySQL`、`Impala`、`Starocks`、`Doris`、`Presto`、`Trino`、`ClickHouse`、`PostgreSQL`
- **检查规则**：内置空值检查、非空检查、枚举检查等27个检查规则
- **作业执行引擎**：已支持`Spark`和`Local`两种执行引擎。`Spark `引擎目前仅支持`Spark2.4`版本，`Local` 引擎则是基于`JDBC`开发的本地执行引擎，无需依赖其他执行引擎。
- **告警通道**：已支持**邮件**
- **错误数据存储**：已支持 `MySQL` 和 **本地文件**（仅支持`Local`执行引擎）
- **注册中心**：已支持 `MySQL`、`PostgreSQL` 和 `ZooKeeper`

### 多种运行模式

- 提供**Web页面**配置检查作业、运行作业、查看作业执行日志、查看错误数据和检查结果
- 支持**在线生成**作业运行脚本，通过 `datavines-submit.sh` 来提交作业，可与调度系统配合使用

![作业脚本](docs/img/data-job-script.jpg)

### 容易部署&高可用

- 平台依赖少，容易部署
- 最小仅依赖 `MySQL` 既可启动项目，完成数据质量作业的检查
- 支持水平扩容，自动容错
- **无中心化设计**，`Server` 节点支持水平扩展提高性能
- 作业**自动容错**，保证作业不丢失和不重复执行

## 环境依赖

1. `Java` 运行环境:`Jdk8`
2. `Datavines` 支持 `JDBC` 引擎，如果你的数据量较小或者只是想做功能验证，可以使用 `JDBC` 引擎
3. 如果您要想要基于 `Spark` 来运行 `Datavines` ，那么需要保证你的服务器具有运行 `Spark` 应用程序的条件

## 快速入门

请参考官方文档：[快速入门指南](https://datavane.github.io/datavines-website/zh-CN/docs/user-guide/quick-start/)

## 开发指南
请参考官方文档：[开发指南](https://datavane.github.io/datavines-website/zh-CN/docs/development/environment-preparation/)

## 贡献指南

[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](https://github.com/datavane/datavines/pulls)

你可以提交 [pull requests](https://github.com/datavane/datavines/pulls) 或者 [GitHub issues](https://github.com/datavane/datavines/issues/new/choose).

> 如果您是发布问题的新手，我们要求您阅读 [*How To Ask Questions The Smart Way*](http://www.catb.org/~esr/faqs/smart-questions.html) (**本指南不提供此项目的实际支持服务！**)和[How to Report Bugs Effectively](http://www.chiark.greenend.org.uk/~sgtatham/bugs.html) 。好的错误报告可以让我们更好地帮助您！

感谢所有已经为 Datavines 做出贡献的人！

[![contrib graph](https://contrib.rocks/image?repo=datavane/datavines)](https://github.com/datavane/datavines/graphs/contributors)


## License
`Datavines` 基于 [Apache License 2.0](LICENSE) 协议。`Datavines` 依赖了一些第三方组件，它们的开源协议也为 `Apache License 2.0` 或者兼容 `Apache License 2.0`， 此外 `Datavines` 也直接引用或者修改了 `Apache DolphinScheduler`、`SeaTunnel` 以及 `Dubbo` 中的一些代码，均为 `Apache License 2.0` 协议的，感谢这些项目的贡献。

## 社交媒体

- 微信公众号（中文，扫描二维码关注）

![微信二维码](docs/img/wechat-qrcode.jpg)