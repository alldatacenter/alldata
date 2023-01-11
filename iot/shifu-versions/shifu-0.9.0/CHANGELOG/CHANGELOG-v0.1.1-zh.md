# 自[v0.1.0](https://github.com/Edgenesis/shifu/releases/tag/v0.1.0) 以来的变化日志

## 错误修正
* 修改默认 handler 从 deviceshifu-base 到每个协议的里面 由 @MrLeea-13155bc 在 https://github.com/Edgenesis/shifu/pull/248
* 移除OPC UA mockdevice 在linux/armv7 上的 Pipeline 构建 由 @tomqin93 在 https://github.com/Edgenesis/shifu/pull/250
* 修复 mockdeviceName 由 @MrLeea-13155bc 在 https://github.com/Edgenesis/shifu/pull/252
* 恢复 ClusterRoleBinding，并添加docs 由 @MrLeea-13155bc 在 https://github.com/Edgenesis/shifu/pull/253

## 功能和改进
* 集成测试，并将其添加到流水线中 由 @MrLeea-13155bc 在 https://github.com/Edgenesis/shifu/pull/244
* 添加 golangci-lint 和 github action 由 @MrLeea-13155bc 在 https://github.com/Edgenesis/shifu/pull/241
* 更新 golangci-lint.yml 由 @tomqin93 在https://github.com/Edgenesis/shifu/pull/246
* 更新 Pipeline, 分离 push 到多个 job 里 由 @tomqin93 在 https://github.com/Edgenesis/shifu/pull/247
* 添加 telemetry 由 @MrLeea-13155bc 在 https://github.com/Edgenesis/shifu/pull/249
* 将 log 替换为 klog 由 @MrLeea-13155bc 在 https://github.com/Edgenesis/shifu/pull/251
* 将 k8s.io/client-go 从 0.25.0 提升到 0.25.1 由 @dependabot 在 https://github.com/Edgenesis/shifu/pull/258
* 将 k8s.io/klog/v2 从 2.70.1 提升到 2.80.1，由 @dependabot 在 https://github.com/Edgenesis/shifu/pull/257

## 文档
* 修复 shifu 文档，由 @ BtXin 在 https://github.com/Edgenesis/shifu/pull/243
* 更新 readme 中的链接，因为shifu.run使用en作为默认语言 由 @Yang-Xijie 在 https://github.com/Edgenesis/shifu/pull/242
* docs: 为了好玩而增加徽章 由 @yufeiminds 在 https://github.com/Edgenesis/shifu/pull/245
* feat：修改错误 由 @geffzhang 在 https://github.com/Edgenesis/shifu/pull/259 中完成。

## 新的贡献者
* @yufeiminds 在 https://github.com/Edgenesis/shifu/pull/245 中做出了他们的第一个贡献。
* @geffzhang 在 https://github.com/Edgenesis/shifu/pull/259 做出了他们的第一个贡献。

**完整的更新日志**：https://github.com/Edgenesis/shifu/compare/v0.1.0...v0.1.1