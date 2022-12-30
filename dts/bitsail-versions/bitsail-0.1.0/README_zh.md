# BitSail

[English](README.md) | 简体中文

[![Build](https://github.com/bytedance/bitsail/actions/workflows/cicd.yml/badge.svg)](https://github.com/bytedance/bitsail/actions/workflows/cicd.yml)
[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![加入Slack](https://img.shields.io/badge/slack-%23BitSail-72eff8?logo=slack&color=5DADE2&label=加入%20Slack)](https://join.slack.com/t/slack-ted3816/shared_invite/zt-1inff2sip-u7Ej_o73sUgdpJAvqwlEwQ)

## 介绍
BitSail是字节跳动开源的基于分布式架构的高性能数据集成引擎, 支持多种异构数据源间的数据同步，并提供离线、实时、全量、增量场景下的全域数据集成解决方案，目前服务于字节内部几乎所有业务线，包括抖音、今日头条等，每天同步数百万亿数据

## 为什么我们要使用BitSail
BitSail目前已被广泛使用,并支持数百万亿的大流量场景。同时在火山引擎云原生环境、客户私有云环境等多种场景下得到验证。

我们积累了很多经验，并做了多项优化，以完善数据集成的功能

- 全域数据集成解决方案, 覆盖离线、实时、增量场景
- 分布式以及云原生架构, 支持水平扩展
- 在准确性、稳定性、性能上，成熟度更好
- 丰富的基础功能，例如类型转换、脏数据处理、流控、数据湖集成、自动并发度推断等
- 完善的任务运行状态监控，例如流量、QPS、脏数据、延迟等

## BitSail使用场景
- 异构数据源海量数据同步
- 流批一体数据处理能力
- 湖仓一体数据处理能力
- 高性能、高可靠的数据同步
- 分布式、云原生架构数据集成引擎

## BitSail主要特点
- 简单易用，灵活配置
- 流批一体、湖仓一体架构，一套框架覆盖几乎所有数据同步场景
- 高性能、海量数据处理能力
- DDL自动同步
- 类型系统，不同数据源类型之间的转换
- 独立于引擎的读写接口，开发成本低
- 任务进度实时展示，正在开发中
- 任务状态实时监控

## BitSail架构
![](docs/images/bitsail_arch.png)

 ```
 Source[Input Sources] -> Framework[Data Transmission] -> Sink[Output Sinks]
 ```
数据处理流程如下，首先通过 Input Sources 拉取源端数据，然后通过中间框架层处理，最后通过 Output Sinks 将数据写入目标端

在框架层，我们提供了丰富的基础功能，并对所有同步场景生效，比如脏数据收集、自动并发度计算、流控、任务监控等

在数据同步场景上，全面覆盖批式、流式、增量场景

在Runtime层，支持多种执行模式，比如yarn、local，k8s在开发中

## 支持连接器列表
<table>
  <tr>
    <th>DataSource</th>
    <th>Sub Modules</th>
    <th>Reader</th>
    <th>Writer</th>
  </tr>
  <tr>
    <td>Hive</td>
    <td>-</td>
    <td>✅</td>
    <td>✅</td>
  </tr>
  <tr>
    <td>Hadoop</td>
    <td>-</td>
    <td>✅</td>
    <td>✅</td>
  </tr>
  <tr>
    <td>Hbase</td>
    <td>-</td>
    <td>✅</td>
    <td>✅</td>
  </tr>
  <tr>
    <td>Hudi</td>
    <td>-</td>
    <td>✅</td>
    <td>✅</td>
  </tr>
  <tr>
    <td>Kafka</td>
    <td>-</td>
    <td>✅</td>
    <td>✅</td>
  </tr>
  <tr>
    <td>RocketMQ</td>
    <td>-</td>
    <td> </td>
    <td>✅</td>
  </tr>
  <tr>
    <td>Redis</td>
    <td>-</td>
    <td> </td>
    <td>✅</td>
  </tr>
  <tr>
    <td>Doris</td>
    <td>-</td>
    <td> </td>
    <td>✅</td>
  </tr>
  <tr>
    <td>MongoDB</td>
    <td>-</td>
    <td>✅</td>
    <td>✅</td>
  </tr>
  <tr>
    <td rowspan="4">JDBC</td>
    <td>MySQL</td>
    <td rowspan="4">✅</td>
    <td rowspan="4">✅</td>
  </tr>
  <tr>
    <td>Oracle</td>
  </tr>
  <tr>
    <td>PostgreSQL</td>
  </tr>
  <tr>
    <td>SqlServer</td>
  </tr>
    <tr>
    <td>FTP/SFTP</td>
    <td>-</td>
    <td>✅</td>
    <td> </td>
  </tr>
  <tr>
    <td>Fake</td>
    <td>-</td>
    <td>✅</td>
    <td> </td>
  </tr>
  <tr>
    <td>Print</td>
    <td>-</td>
    <td> </td>
    <td>✅</td>
  </tr>
</table>

## 社区支持
### Slack
通过此链接可以直接下载并加入BitSail的Slack频道 [link](https://join.slack.com/t/slack-ted3816/shared_invite/zt-1inff2sip-u7Ej_o73sUgdpJAvqwlEwQ)

### 邮件列表
当前，BitSail社区通过谷歌群组作为邮件列表的提供者，邮件列表可以在绝大部分地区正常收发邮件。
在订阅BitSail小组的邮件列表后可以通过发送邮件发言

订阅: 发送Email到此地址 `bitsail+subscribe@googlegroups.com`，
你会收到一封回信询问你是否希望加入BitSail群组，`Join This Group`按钮可能因网络原因无法使用，直接回复此封邮件便可确认加入。

开启一个话题: 发送Email到此地址 `bitsail@googlegroups.com`

取消订阅: 发送Email到此地址 `bitsail+unsubscribe@googlegroups.com`

### 微信群
欢迎加入BitSail微信群参与社区讨论与贡献

<img src="docs/images/wechat_QR.png" alt="qr" width="100"/>

## 环境配置
跳转[环境配置](docs/env_setup_zh.md).

## 如何部署
跳转[部署指南](docs/deployment_zh.md).

## BitSail参数指引
跳转[参数指引](docs/config_zh.md).

## 如何贡献
跳转[贡献者指引](docs/contributing_zh.md).

## 开源协议
[Apache 2.0 License](LICENSE).

