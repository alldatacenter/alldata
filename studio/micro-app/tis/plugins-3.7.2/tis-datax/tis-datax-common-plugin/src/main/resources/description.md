* 定义DataxProcessor实现录入DataX实例基本信息的功能

* 为其他DataX Reader、Writer插件提供了公共类

* 定义DataXGlobalConfig，提供DataX配置的基础配置参数，如：channel,errorLimitCount 等

* 提供了DataX 任务提交方式的实现
  
  1. `LocalDataXJobSubmit`本地任务提交
  2. `DistributedOverseerDataXJobSubmit`基于K8S的分布式任务提交方式（生产环境中建议使用该种提交方式）