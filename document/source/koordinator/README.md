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

English | [简体中文](./README-zh_CN.md)
## Introduction

Koordinator is a QoS based scheduling system for hybrid orchestration workloads on Kubernetes. It aims to improve the
runtime efficiency and reliability of both latency sensitive workloads and batch jobs, simplify the complexity of
resource-related configuration tuning, and increase pod deployment density to improve resource utilization.

Koordinator enhances the kubernetes user experiences in the workload management by providing the following:

- Well-designed priority and QoS mechanism to co-locate different types of workloads in a cluster, a node.
- Allowing for resource overcommitments to achieve high resource utilization but still satisfying the QoS guarantees by
  leveraging an application profiling mechanism.
- Fine-grained resource orchestration and isolation mechanism to improve the efficiency of latency-sensitive workloads
  and batch jobs.
- Flexible job scheduling mechanism to support workloads in specific areas, e.g., big data, AI, audio and video.
- A set of tools for monitoring, troubleshooting and operations.

## Quick Start

You can view the full documentation from the [Koordinator website](https://koordinator.sh/docs).

- Install or upgrade Koordinator with [the latest version](https://koordinator.sh/docs/installation).
- Referring to [best practices](https://koordinator.sh/docs/best-practices/colocation-of-spark-jobs), there will be
  examples on running co-located workloads.

## Code of conduct

The Koordinator community is guided by our [Code of Conduct](CODE_OF_CONDUCT.md), which we encourage everybody to read
before participating.

In the interest of fostering an open and welcoming environment, we as contributors and maintainers pledge to making
participation in our project and our community a harassment-free experience for everyone, regardless of age, body size,
disability, ethnicity, level of experience, education, socio-economic status,
nationality, personal appearance, race, religion, or sexual identity and orientation.

## Contributing

You are warmly welcome to hack on Koordinator. We have prepared a detailed guide [CONTRIBUTING.md](CONTRIBUTING.md).

## Community

The [koordinator-sh/community repository](https://github.com/koordinator-sh/community) hosts all information about
the community, membership and how to become them, developing inspection, who to contact about what, etc.

We encourage all contributors to become members. We aim to grow an active, healthy community of contributors, reviewers,
and code owners. Learn more about requirements and responsibilities of membership in
the [community membership](https://github.com/koordinator-sh/community/blob/main/community-membership.md) page.

Active communication channels:

- Bi-weekly Community Meeting (APAC, *Chinese*):
  - Tuesday 19:30 GMT+8 (Asia/Shanghai)
  - [Meeting Link(DingTalk)](https://meeting.dingtalk.com/j/cgTTojEI8Zy)
  - [Notes and agenda](https://shimo.im/docs/m4kMLdgO1LIma9qD)
- Slack(English): [koordinator channel](https://kubernetes.slack.com/channels/koordinator) in Kubernetes workspace
- DingTalk(Chinese): Search Group ID `33383887` or scan the following QR Code

<div>
  <img src="https://github.com/koordinator-sh/koordinator/raw/main/docs/images/dingtalk.png" width="300" alt="Dingtalk QRCode">
</div>

## License

Koordinator is licensed under the Apache License, Version 2.0. See [LICENSE](./LICENSE) for the full license text.

## 官方项目地址
https://github.com/koordinator-sh/koordinator