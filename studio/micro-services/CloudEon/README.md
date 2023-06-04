<div align="center">
<h1>CloudEon云原生大数据平台</h1>

[![GitHub Pull Requests](https://img.shields.io/github/stars/dromara/CloudEon)](https://github.com/dromara/CloudEon/stargazers)
[![HitCount](https://views.whatilearened.today/views/github/dromara/CloudEon.svg)](https://github.com/dromara/CloudEon)
[![Commits](https://img.shields.io/github/commit-activity/m/dromara/CloudEon?color=ffff00)](https://github.com/dromara/CloudEon/commits/main)
[![pre-commit](https://img.shields.io/badge/pre--commit-enabled-brightgreen?logo=pre-commit)](https://github.com/pre-commit/pre-commit)
[![All Contributors](https://img.shields.io/badge/all_contributors-3-orange.svg?style=flat-square)](#contributors-)
[![GitHub license](https://img.shields.io/github/license/dromara/CloudEon)](https://github.com/dromara/CloudEon/LICENSE)

<p> 🌉 构建于kubernetes集群之上的大数据集群管理平台 🌉</p>

<img src="https://camo.githubusercontent.com/82291b0fe831bfc6781e07fc5090cbd0a8b912bb8b8d4fec0696c881834f81ac/68747470733a2f2f70726f626f742e6d656469612f394575424971676170492e676966" width="800"  height="3">
</div><br>



## ℹ️ 项目简介

`CloudEon`是一款基于`kubernetes`的开源大数据平台，旨在为用户提供一种简单、高效、可扩展的大数据解决方案。该平台支持多种大数据服务的部署和管理，如hadoop、doris、Spark、Flink、Hive等，能够满足不同规模和业务需求下的大数据处理和分析需求。

## 🔗 文档快链

项目相关介绍，使用，最佳实践等相关内容，都会在官方文档呈现，如有疑问，请先阅读官方文档，以下列举以下常用快链。

- [官网地址](https://cloudeon.top//)
- [项目介绍](https://docs.cloudeon.top/en/latest/)
- [安装部署](https://docs.cloudeon.top/en/latest/%E5%AE%89%E8%A3%85%E9%83%A8%E7%BD%B2/docker)
- [支持组件](https://docs.cloudeon.top/en/latest/%E6%94%AF%E6%8C%81%E7%BB%84%E4%BB%B6/supportservice/)
- [Roadmap](https://docs.cloudeon.top/en/latest/Roadmap/)


## 🔍 功能特点

- 🚀 **快速搭建大数据集群**：通过`CloudEon`，用户可以在kubernetes上快速搭建部署hadoop集群、doris集群等大数据集群，省去了手动安装和配置的繁琐过程。

- 🐳 **容器化运行所有大数据服务**：`CloudEon`将所有大数据服务都以容器方式运行，使得这些服务的部署和管理更加灵活和便捷，同时也能更好地利用kubernetes的资源调度和管理能力。

- 📈 **支持监控告警等功能**：`CloudEon`提供了监控告警等功能，帮助用户实时监控集群运行状态，及时发现和解决问题。

- 🔧 **支持配置修改等功能**：`CloudEon`还提供了配置修改等功能，使得用户能够更加灵活地管理和配置自己的大数据集群。

- 🤖 **自动化运维**：`CloudEon`通过自动化运维，能够降低集群管理的难度和人力成本，同时也能提高集群的可用性和稳定性。

- 👀 **可视化管理界面**：`CloudEon`提供了可视化的管理界面，使得用户能够更加直观地管理和监控自己的大数据集群

- 🔌 **灵活的扩展性**：提供了插件机制，让用户可以自定义拓展和安装更多的大数据服务。这个插件机制是基于开放API和标准化接口实现的，可以支持用户快速开发和集成新的服务。

- 📊 **多种大数据服务支持**：除了hadoop和doris，`CloudEon`还支持其他多种大数据服务的部署和管理，如Spark、Flink、Hive、Kyuubi等。

**页面功能概览：**

|           ![集群](https://user-images.githubusercontent.com/123344357/230782193-d2830fa7-92c8-4efc-a44e-df0e8742b012.png)           | ![告警](https://user-images.githubusercontent.com/123344357/230778648-653dc9a7-f78e-4f1d-9aaa-7689ad257f10.png)  |
|:---------------------------------------------------------------------------------------:|---------------------------------------------------------------------|
|  ![服务列表](https://user-images.githubusercontent.com/123344357/230782573-aca46f56-46d6-4eb1-b0e4-4dbaf78a9695.png)  |  ![新增服务](https://user-images.githubusercontent.com/123344357/230782108-b9e322b3-c1de-4ad1-a34d-d89ab0319252.png)  |
|           ![配置](https://user-images.githubusercontent.com/123344357/230782069-93574212-8628-4af5-934a-b09ea0c073e5.png)           | ![角色实例](https://user-images.githubusercontent.com/123344357/230778761-0accabf4-209e-4666-8b7d-0fe8dcd52056.png)  |
|           ![指令日志](https://user-images.githubusercontent.com/123344357/230778679-6520b845-e354-4a73-a661-fb5b7596f217.png)       | ![指令详情](https://user-images.githubusercontent.com/123344357/230778699-a152755b-8c66-40a8-8fdc-27aa1f8e239c.png) |

## 官方项目地址
https://github.com/dromara/CloudEon
