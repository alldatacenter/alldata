# 自 [v0.0.3](https://github.com/Edgenesis/shifu/releases/tag/v0.0.3) 以来的变更

## Bug 修复
* 修复Azure Pipeline中Docker Buildx的问题 by @tomqin93 in https://github.com/Edgenesis/shifu/pull/167
* 修复Azure Pipeline中拼写问题 by @tomqin93 in https://github.com/Edgenesis/shifu/pull/168

## 功能 & 增强
* 更新 azure-pipelines by @Sodawyx in https://github.com/Edgenesis/shifu/pull/160
* 更新 k8s.io/client-go from 0.24.2 to 0.24.3 by @dependabot in https://github.com/Edgenesis/shifu/pull/174
* 更新 k8s.io/apimachinery from 0.24.2 to 0.24.3 in /deviceshifu/pkg/mockdevice/mockdevice by @dependabot in https://github.com/Edgenesis/shifu/pull/175
* 删除HTTP以外的代码 by @13316272689 in https://github.com/Edgenesis/shifu/pull/171
* 删除deviceshifuSocket中不必要的代码 by @13316272689 in https://github.com/Edgenesis/shifu/pull/172
* 删除deviceshifuMQTT中不必要的代码 by @13316272689 in https://github.com/Edgenesis/shifu/pull/177
* 更新 go1.18 by @MrLeea-13155bc in https://github.com/Edgenesis/shifu/pull/176
* 在HTTP deviceShifu中加入超时 by @MrLeea-13155bc in https://github.com/Edgenesis/shifu/pull/170
* 让makefile使用go install @tomqin93 in https://github.com/Edgenesis/shifu/pull/183
* 在deviceShifu HTTP HTTP中加入自定义遥测 by @MrLeea-13155bc in https://github.com/Edgenesis/shifu/pull/179
* <ADD> 加入MQTT的遥测 by @Sodawyx in https://github.com/Edgenesis/shifu/pull/180
* 更新Socket协议的遥测 by @13316272689 in https://github.com/Edgenesis/shifu/pull/184
* 更新 github.com/onsi/gomega from 1.19.0 to 1.20.0 in /k8s/crd by @dependabot in https://github.com/Edgenesis/shifu/pull/185
* 更新makefile中所有的Tag by @MrLeea-13155bc in https://github.com/Edgenesis/shifu/pull/186

## 文档
* 无

**全部 Changelog**: https://github.com/Edgenesis/shifu/compare/v0.0.3...v0.0.4
