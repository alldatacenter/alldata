# 自 [v0.8.0](https://github.com/Edgenesis/shifu/releases/tag/v0.8.0) 以来的变更

## 新功能 🎉

* Deviceshifu可以从secret中加载遥测密码（如果存在）by @FFFFFaraway in https://github.com/Edgenesis/shifu/pull/421

## Bug 修复

* <BugFix>修复不能正常使用自定义deviceshifu的问题 by @Twpeak in https://github.com/Edgenesis/shifu/pull/369
* <BugFix>修复Plc4x超时问题 by @leepala in https://github.com/Edgenesis/shifu/pull/391
* <BugFix>修复rtspDeviceShifu重新启动问题和线程未关闭的问题 by @cbgz121 in https://github.com/Edgenesis/shifu/pull/433

## 改进

* 更新devcontainer配置以解决预处理问题，更新插件 by @tomqin93 in https://github.com/Edgenesis/shifu/pull/427
* 为华为云自动推送图像添加管道配置 by @Vacant2333 in https://github.com/Edgenesis/shifu/pull/430
* 添加logger以替换klog by @jyyds in https://github.com/Edgenesis/shifu/pull/437 and https://github.com/Edgenesis/shifu/pull/440

## 文档

* telemetry加载secret的设计 by @FFFFFaraway in https://github.com/Edgenesis/shifu/pull/429
* deviceshifu-rtsp的设计 by @leepala in https://github.com/Edgenesis/shifu/pull/428

## 新的贡献者 🌟

* @Vacant2333 在 https://github.com/Edgenesis/shifu/pull/430 提交了第一个贡献，合并进主干！
* @jyyds 在 https://github.com/Edgenesis/shifu/pull/437 提交了第一个贡献，合并进主干！

## Dependabot 自动更新 🤖

* Bump sigs.k8s.io/controller-runtime from 0.13.1 to 0.14.1 by @dependabot in https://github.com/Edgenesis/shifu/pull/424
* Bump github.com/taosdata/driver-go/v3 from 3.0.3 to 3.0.4 by @dependabot in https://github.com/Edgenesis/shifu/pull/432
* Bump golang.org/x/crypto from 0.1.0 to 0.4.0 by @dependabot in https://github.com/Edgenesis/shifu/pull/435

**完整变更日志**: https://github.com/Edgenesis/shifu/compare/v0.8.0...v0.9.0