# 自 [v0.3.0](https://github.com/Edgenesis/shifu/releases/tag/v0.3.0) 以来的变更

## 新功能 🎉

* <enhance> 实现 MQTT Telemetry Service by @MrLeea-13155bc in https://github.com/Edgenesis/shifu/pull/327

## Bug 修复
* 无


## 改进
* [Issue #321] 在所有的镜像中使用 gcr.io/distroless by @tomqin93 in https://github.com/Edgenesis/shifu/pull/322
* <enhance> 加入 deviceshifu-socket 的单元测试 by @MrLeea-13155bc in https://github.com/Edgenesis/shifu/pull/311
* <feature> 从 v0.3.0 分支的 rebase by @tomqin93 in https://github.com/Edgenesis/shifu/pull/329
* 将单元测试的 `strPointer .. ` 更改为 Paradigm 类型 by @MrLeea-13155bc in https://github.com/Edgenesis/shifu/pull/332
* TestCollectMQTTTelemetry 单元测试 by @Twpeak in https://github.com/Edgenesis/shifu/pull/334
* TestCommandHandleMQTTFunc 单元测试以及测试客户端和服务器 by @Twpeak in https://github.com/Edgenesis/shifu/pull/335
* TestNew 单元测试 by @Twpeak in https://github.com/Edgenesis/shifu/pull/336
* Mockdevice plc 单元测试 by @Twpeak in https://github.com/Edgenesis/shifu/pull/343
* Mockdevice thermometer 单元测试 by @Twpeak in https://github.com/Edgenesis/shifu/pull/341
* Mockdevice agv 单元测试 by @Twpeak in https://github.com/Edgenesis/shifu/pull/345
* Mockdevice plate reader 单元测试 by @Twpeak in https://github.com/Edgenesis/shifu/pull/344
* 跳过 mock device 单元测试 by @kris21he in https://github.com/Edgenesis/shifu/pull/351
* 更新Go版本从 go 1.18/1.19 => 1.19.2 by @MrLeea-13155bc in https://github.com/Edgenesis/shifu/pull/349

## 文档

* 英文更新 by @rachelzhang0922 in https://github.com/Edgenesis/shifu/pull/304
* 加入 telemetry_service 到 MQTT broker 的设计 by @BtXin in https://github.com/Edgenesis/shifu/pull/310
* <docs> 修复 README 中的错误 by @MrLeea-13155bc in https://github.com/Edgenesis/shifu/pull/337
* 更好的文档结构 by @Yang-Xijie in https://github.com/Edgenesis/shifu/pull/326
* 加入整合 TDengine 的设计 by @BtXin in https://github.com/Edgenesis/shifu/pull/339
* [Issue #319] 添加 Linux 开发指南 by @tomqin93 in https://github.com/Edgenesis/shifu/pull/320

## Dependabot 自动更新 🤖

* Bump github.com/onsi/gomega from 1.22.0 to 1.22.1 by @dependabot in https://github.com/Edgenesis/shifu/pull/315
* Bump k8s.io/client-go from 0.25.2 to 0.25.3 by @dependabot in https://github.com/Edgenesis/shifu/pull/323
* Bump github.com/stretchr/testify from 1.8.0 to 1.8.1 by @dependabot in https://github.com/Edgenesis/shifu/pull/348

## 新的贡献者

* @rachelzhang0922  https://github.com/Edgenesis/shifu/pull/304 中做出第一次贡献
* @Twpeak在 https://github.com/Edgenesis/shifu/pull/334 中做出第一次贡献

**完整变更日志**: https://github.com/Edgenesis/shifu/compare/v0.3.0...v0.4.0