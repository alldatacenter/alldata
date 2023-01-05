## flinkCluster 

对应Flink的执行任务集群，TIS组装好Flink Job之后，提交任务时会向 Flink Cluster中提交任务。

TIS平台中提交Flink任务之前，请先创建Flink Cluster，支持两种模式：

1. Native Kubernetes: [详细请查看](https://nightlies.apache.org/flink/flink-docs-release-1.14/docs/deployment/resource-providers/native_kubernetes/)
   [安装说明](http://tis.pub/docs/install/flink-cluster/)：
      - 在本地局域网中安装k8s环境
      - 在TIS中部署Flink-Cluster，[入口](/base/flink-cluster)

2. Standalone: [详细请查看](https://nightlies.apache.org/flink/flink-docs-release-1.14/docs/deployment/resource-providers/standalone/overview/)
   
   [安装说明](http://tis.pub/docs/install/flink-cluster/standalone/):
      - 下载、解压
        ```shell script
         wget http://tis-release.oss-cn-beijing.aliyuncs.com/${project.version}/tis/flink-tis-1.13.1-bin.tar.gz && rm -rf flink-tis-1.13.1 && mkdir flink-tis-1.13.1 && tar xvf flink-tis-1.13.1-bin.tar.gz -C ./flink-tis-1.13.1
        ```
      - 启动Flink-Cluster：
         ```shell script
         ./bin/start-cluster.sh
         ```
         

## parallelism

任务执行并行度

在 Flink 里面代表每个任务的并行度，适当的提高并行度可以大大提高 job 的执行效率，比如你的 job 消费 kafka 数据过慢，适当调大可能就消费正常了。

## restartStrategy

The cluster can be started with a default restart strategy which is always used when no job specific restart strategy has been defined. In case that the job is submitted with a restart strategy, this strategy overrides the cluster’s default setting.

Detailed description:[restart-strategies](https://nightlies.apache.org/flink/flink-docs-master/docs/ops/state/task_failure_recovery/#restart-strategies)

There are 4 types of restart-strategy:

1. `off`: No restart strategy.
2. `fixed-delay`: Fixed delay restart strategy. More details can be found [here](https://nightlies.apache.org/flink/flink-docs-master/docs/ops/state/task_failure_recovery/#fixed-delay-restart-strategy).
3. `failure-rate`: Failure rate restart strategy. More details can be found [here](https://nightlies.apache.org/flink/flink-docs-master/docs/ops/state/task_failure_recovery#failure-rate-restart-strategy).
4. `exponential-delay`: Exponential delay restart strategy. More details can be found [here](https://nightlies.apache.org/flink/flink-docs-master/docs/ops/state/task_failure_recovery#exponential-delay-restart-strategy).


## checkpoint

Checkpoints make state in Flink fault tolerant by allowing state and the corresponding stream positions to be recovered, thereby giving the application the same semantics as a failure-free execution.

Detailed description:
1. [https://nightlies.apache.org/flink/flink-docs-master/docs/ops/state/checkpoints/](https://nightlies.apache.org/flink/flink-docs-master/docs/ops/state/checkpoints/)
2. [https://nightlies.apache.org/flink/flink-docs-master/docs/dev/datastream/fault-tolerance/checkpointing/](https://nightlies.apache.org/flink/flink-docs-master/docs/dev/datastream/fault-tolerance/checkpointing/)

## stateBackend

Flink provides different state backends that specify how and where state is stored.

State can be located on Java’s heap or off-heap. Depending on your state backend, Flink can also manage the state for the application, meaning Flink deals with the memory management (possibly spilling to disk if necessary) to allow applications to hold very large state. By default, the configuration file flink-conf.yaml determines the state backend for all Flink jobs.

However, the default state backend can be overridden on a per-job basis, as shown below.

For more information about the available state backends, their advantages, limitations, and configuration parameters see the corresponding section in [Deployment & Operations](https://nightlies.apache.org/flink/flink-docs-master/docs/ops/state/state_backends/).





