# 自 [v0.0.6](https://github.com/Edgenesis/shifu/releases/tag/v0.0.6) 以来的变化日志

# ⚠️ 重大变化 ⚠️
* 将Shifu的项目重构为单一项目目录 by @BtXin in https://github.com/Edgenesis/shifu/pull/217

## 错误修正
* OPC UA例子的更新，由 @tomqin93 在 https://github.com/Edgenesis/shifu/pull/233
* 修复 README 中的演示链接，由 @Yang-Xijie 在 https://github.com/Edgenesis/shifu/pull/229
* 修复遥测YAML和设备样本YAML中的一些错误 by @tomqin93 在 https://github.com/Edgenesis/shifu/pull/235
* [issue#234] MQTT和其他telemetrySetting的修复 由 @tomqin93 在 https://github.com/Edgenesis/shifu/pull/236
* 修复当defaulttimeoutSeconds<0时http和socket协议的问题 @MrLeea-13155bc 在 https://github.com/Edgenesis/shifu/pull/237

## 功能和改进
* 创建 codecov.yml by @tomqin93 在 https://github.com/Edgenesis/shifu/pull/216
* 更新模拟设备的版本 by @tomqin93 在 https://github.com/Edgenesis/shifu/pull/222
* 在README中添加CodeCov和星图 by @tomqin93 在 https://github.com/Edgenesis/shifu/pull/226
* 将Shifu的项目重构为单一项目目录 by @BtXin in https://github.com/Edgenesis/shifu/pull/217
* 尝试修复CI，由 @tomqin93 在 https://github.com/Edgenesis/shifu/pull/228 发表
* 将k8s.io/client-go从0.24.4提升到0.25.0 由 @dependabot 在 https://github.com/Edgenesis/shifu/pull/220
* 将github.com/onsi/gomega从1.18.1升至1.20.1 由@dependabot在 https://github.com/Edgenesis/shifu/pull/227
* 将github.com/onsi/gomega从1.20.1升级到1.20.2 由@dependabot在 https://github.com/Edgenesis/shifu/pull/232
* 使用 gcr distroless 而不是 edgehub 的镜像，由 @BtXin 在 https://github.com/Edgenesis/shifu/pull/231 发表
* 将sigs.k8s.io/controller-runtime从0.12.3提升到0.13.0 由 @dependabot 在 https://github.com/Edgenesis/shifu/pull/238

## 文档
* 更新问题模板，由 @tomqin93 在 https://github.com/Edgenesis/shifu/pull/213 发表。
* 修复changelog v0.0.6的名称 by @BtXin 在 https://github.com/Edgenesis/shifu/pull/214

**Full Changelog**: https://github.com/Edgenesis/shifu/compare/v0.0.6...v0.1.0