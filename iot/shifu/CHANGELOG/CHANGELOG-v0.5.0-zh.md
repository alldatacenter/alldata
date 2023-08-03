# 自 [v0.4.0](https://github.com/Edgenesis/shifu/releases/tag/v0.4.0) 以来的变更

## 新功能 🎉

* 实现 TDengine 的 TelemetryService by @MrLeea-13155bc in https://github.com/Edgenesis/shifu/pull/350

## Bug 修复

* <BugFix> 添加通道来避免测试 MQTT 服务器在test完成之前关闭 by @MrLeea-13155bc in https://github.com/Edgenesis/shifu/pull/359
* <enhance> 将 v0.4.0 中的修复 rebase 回主分支 by @tomqin93 in https://github.com/Edgenesis/shifu/pull/360
* <BugFix> 将 v0.4.1 中的修复 rebase 回主分支 by @tomqin93 in https://github.com/Edgenesis/shifu/pull/368
* <BugFix> 修复一些单元测试的问题 by @MrLeea-13155bc in https://github.com/Edgenesis/shifu/pull/373
* <bugFix> 修复 TelemetryService 的路径 by @MrLeea-13155bc in https://github.com/Edgenesis/shifu/pull/364
* 修复 demo 安装脚本 by @MrLeea-13155bc in https://github.com/Edgenesis/shifu/pull/376

## 改进

* <Enhance> 使用多段 build 来构建 socket 测试设备 by @tomqin93 in https://github.com/Edgenesis/shifu/pull/370
* 将所有 int 类型更新为 int64 by @MrLeea-13155bc in https://github.com/Edgenesis/shifu/pull/366
* 使用 `T.Setenv` 来设定测试中的环境变量 by @Juneezee in https://github.com/Edgenesis/shifu/pull/375
* 更新 gitignore by @180909 in https://github.com/Edgenesis/shifu/pull/357
* 添加 Shifu 的初始化配置 by @MrLeea-13155bc in https://github.com/Edgenesis/shifu/pull/367

## 文档

* 更新 README by @rachelzhang0922 in https://github.com/Edgenesis/shifu/pull/352
* 添加贡献文档以及添加 shifuctl by @saiyan86 in https://github.com/Edgenesis/shifu/pull/288
* 更新贡献文档 by @saiyan86 in https://github.com/Edgenesis/shifu/pull/377

## 新的贡献者 🌟

* @Juneezee 在  https://github.com/Edgenesis/shifu/pull/375 中提交了第一个PR
* @180909 在 https://github.com/Edgenesis/shifu/pull/357 中提交了第一个PR

## Dependabot 自动更新 🤖

* Bump github.com/eclipse/paho.mqtt.golang from 1.4.1 to 1.4.2 by @dependabot in https://github.com/Edgenesis/shifu/pull/355
* Bump github.com/taosdata/driver-go/v3 from 3.0.2 to 3.0.3 by @dependabot in https://github.com/Edgenesis/shifu/pull/363
* Bump github.com/spf13/cobra from 1.5.0 to 1.6.1 by @dependabot in https://github.com/Edgenesis/shifu/pull/371
* Bump github.com/onsi/gomega from 1.22.1 to 1.24.0 by @dependabot in https://github.com/Edgenesis/shifu/pull/372

**完整变更日志**: https://github.com/Edgenesis/shifu/compare/v0.4.0...v0.5.0
