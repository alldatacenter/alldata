# 流程设计
## 任务执行流程
- 创建任务
- 生成一条Command放到Command表中
- TaskScheduler周期性从Command表中获取一条Command
- 生成TaskRequest交给TaskExecuteManager去执行
- TaskExecuteManager将TaskRequest放入队列中
- TaskSender线程会不停地从队列中取出Task，然后启动TaskRunner线程去执行
- TaskRunner执行完后将TaskAckResponseCommand放到TaskResponse中
- TaskResponseExecutor会不停地拿出Response来处理，更新Task地状态或者进行Task重试

## 容错设计
- Server宕机的容错
  - 当有Server宕机的时候，其他server监听到有其他server下线，则会开始走容错流程
  - 通过竞争分布式锁拿到容错的资格，接着获取该server上仍然在运行的task
    - 首先判读该task有没有appId,有的话则放到轮询线程，去查询Yarn任务的状态，更新到任务
    - 如果该task没有appId,那么通过applicationTag去查询appId，如果还查不到那么证明该任务并不是运行在YARN上
    - 执行任务重跑流程
  - Server启动以后，首先会去获取当前active server list
    - 所有的容错处理都会先竞争分布式锁
    - 第一，根据当前server的host:port去库里面查询有没有当前server running状态的任务，有的话，先进行容错
    - 第二，查询task中host不存在activeServerList的任务进行容错
- Command获取以后，开始执行，如果Server挂掉了，如何去恢复任务