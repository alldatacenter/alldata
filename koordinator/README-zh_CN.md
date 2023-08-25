<h1 align="center">
  <p align="center">Koordinator</p>
  <a href="https://koordinator.sh"><img src="https://github.com/koordinator-sh/koordinator/raw/main/docs/images/koordinator-logo.jpeg" alt="Koordinator"></a>
</h1>

[![License](https://img.shields.io/github/license/koordinator-sh/koordinator.svg?color=4EB1BA&style=flat-square)](https://opensource.org/licenses/Apache-2.0)
[![GitHub release](https://img.shields.io/github/v/release/koordinator-sh/koordinator.svg?style=flat-square)](https://github.com/koordinator-sh/koordinator/releases/latest)
[![CI](https://img.shields.io/github/actions/workflow/status/koordinator-sh/koordinator/ci.yaml?label=CI&logo=github&style=flat-square&branch=main)](https://github.com/koordinator-sh/koordinator/actions/workflows/ci.yaml)
[![Go Report Card](https://goreportcard.com/badge/github.com/koordinator-sh/koordinator?style=flat-square)](https://goreportcard.com/report/github.com/koordinator-sh/koordinator)
[![codecov](https://img.shields.io/codecov/c/github/koordinator-sh/koordinator?logo=codecov&style=flat-square)](https://codecov.io/github/koordinator-sh/koordinator)
[![PRs Welcome](https://badgen.net/badge/PRs/welcome/green?icon=https://api.iconify.design/octicon:git-pull-request.svg?color=white&style=flat-square)](CONTRIBUTING.md)
[![Slack](https://badgen.net/badge/slack/join/4A154B?icon=slack&style=flat-square)](https://join.slack.com/t/koordinator-sh/shared_invite/zt-1756qoub4-Cn4~esfdlfAPsD7cwO2NzA)


[English](./README.md) | 简体中文



## 介绍

Koordinator 基于 QoS 调度系统，支持 Kubernetes 上多种工作负载的混部调度。它的目标是提高工作负载的运行时效率和可靠性（包括延迟敏感型负载和批处理任务），简化资源相关配置调优的复杂性，并增加 Pod 部署密度以提高资源利用率。

Koordinator 通过提供如下功能来增强用户在 Kubernetes 上管理工作负载的体验：

- 提高资源利用率：Koordinator 旨在优化集群资源的利用率，确保所有节点都被有效和高效地使用。
- 提高性能：通过使用先进的算法和技术，Koordinator 帮助用户提高 Kubernetes 集群的性能，减少容器间的干扰，提高整体系统的速度。
- 灵活的调度策略：Koordinator 提供了多种自定义调度策略的选项，允许管理员微调系统的行为以适应其特定需求。
- 简易集成：Koordinator 被设计为易于集成到现有的 Kubernetes 集群中，允许用户快速且无麻烦地开始使用它。


## 快速开始

你可以在 [Koordinator website](https://koordinator.sh/docs) 查看到完整的文档集。

- 安装/升级 Koordinator [最新版本](https://koordinator.sh/docs/installation)
- 参考[最佳实践](https://koordinator.sh/docs/best-practices/colocation-of-spark-jobs)，里面有一些关于运行混部工作负载的示例。

## 行为守则

Koordinator 社区遵照[行为守则](CODE_OF_CONDUCT.md)。我们鼓励每个人在参与之前先读一下它。

为了营造一个开放和热情的环境，我们作为贡献者和维护者承诺：无论年龄、体型、残疾、种族、经验水平、教育程度、社会经济地位、国籍、个人外貌、种族、宗教或性认同和性取向如何，参与我们的项目和社区的每个人都不会受到骚扰。

## 贡献

我们非常欢迎每一位社区同学共同参与 Koordinator 的建设，你可以从 [CONTRIBUTING.md](CONTRIBUTING.md) 手册开始。

## 成员

我们鼓励所有贡献者成为成员。我们的目标是发展一个由贡献者、审阅者和代码所有者组成的活跃、健康的社区。在我们的[社区成员](https://github.com/koordinator-sh/community/blob/main/community-membership.md)页面，详细了解我们的成员要求和责任。

## 社区

在 [koordinator-sh/community 仓库](https://github.com/koordinator-sh/community) 中托管了所有社区信息， 例如成员制度、代码规范等。

我们鼓励所有贡献者成为成员。我们的目标是发展一个由贡献者、审阅者和代码所有者组成的活跃、健康的社区。
请在[社区成员制度](https://github.com/koordinator-sh/community/blob/main/community-membership.md)页面，详细了解我们的成员要求和责任。

活跃的社区途径：

- 社区双周会（中文）：
  - 周二 19:30 GMT+8 (北京时间)
  - [钉钉会议链接](https://meeting.dingtalk.com/j/cgTTojEI8Zy)
  - [议题&记录文档](https://shimo.im/docs/m4kMLdgO1LIma9qD)
- Slack( English ): [koordinator channel](https://kubernetes.slack.com/channels/koordinator) in Kubernetes workspace
- 钉钉( Chinese ): 搜索群ID `33383887`或者扫描二维码加入

<div>
  <img src="https://github.com/koordinator-sh/koordinator/raw/main/docs/images/dingtalk.png" width="300" alt="Dingtalk QRCode">
</div>

## License

Koordinator is licensed under the Apache License, Version 2.0. See [LICENSE](./LICENSE) for the full license text.
<!--

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=koordinator-sh/koordinator&type=Date)](https://star-history.com/#koordinator-sh/koordinator&Date)
-->
