## zkAddress

执行器利用基于Zookeeper的分布式队列来向K8S集群中分发DataX任务。

如本地环境中还没有部署Zookeeper，TIS Uber安装包中提供了Zookeeper服务端安装包，启动方法：

```shell script
cd ./tis-uber
sh ./bin/zookeeper start
```

