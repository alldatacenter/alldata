# 云原生大数据集群
> 
> 1、配置/etc/hosts
> 
> 10.0.12.4 namenode
> 10.0.12.4 resourcemanager
> 10.0.12.4 elasticsearch
> 10.0.12.4 kibana
> 10.0.12.4 prestodb
> 10.0.12.4 hbase-master
> 10.0.12.4 jobmanager
> 10.0.12.4 datanode
> 10.0.12.4 nodemanager
> 10.0.12.4 historyserver
> 10.0.12.4 hive-metastore
> 10.0.12.4 hive-metastore-pg
> 10.0.12.4 hive-server
> 10.0.12.4 zookeeper
> 10.0.12.4 kafka
> 10.0.12.4 elasticsearch
> 10.0.12.4 jobmanager
> 10.0.12.4 taskmanger
> 10.0.12.4 hbase-master
> 10.0.12.4 hbase-regionserver
> 10.0.12.4 hbase-thrift
> 10.0.12.4 hbase-stargate
> 10.0.12.4 alluxio-master
> 10.0.12.4 alluxio-worker
> 10.0.12.4 alluxio-proxy
> 10.0.12.4 filebeat
> 
> 2、docker-compose up -d
> 
> 3、访问hive
> 
> 3.1 进入hive-metastore 9083
> docker exec -it hive-metastore /bin/bash
> 3.2 进行hive客户端
> hive --hiveconf hive.root.logger=INFO,console
> 4、页面访问