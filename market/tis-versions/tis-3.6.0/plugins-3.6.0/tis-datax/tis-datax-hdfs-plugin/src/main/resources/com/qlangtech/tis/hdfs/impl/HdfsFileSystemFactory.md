## hdfsSiteContent

配置实例,实现了HDFS HA高可用方案：

[hdfs-site.xml参数详解](https://hadoop.apache.org/docs/stable/hadoop-project-dist/hadoop-hdfs/hdfs-default.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
  <property>
    <name>dfs.nameservices</name>
    <value>daily-cdh</value>
  </property>
  <property>
    <name>dfs.client.failover.proxy.provider.daily-cdh</name>
  <value>org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider</value>
  </property>
  <property>
    <name>dfs.ha.automatic-failover.enabled.daily-cdh</name>
    <value>true</value>
  </property>
  <property>
    <name>dfs.ha.namenodes.daily-cdh</name>
    <value>namenode228,namenode295</value>
  </property>
  <property>
    <name>dfs.namenode.rpc-address.daily-cdh.namenode228</name>
    <value>192.168.28.200:9000</value>
  </property>
  <property>
    <name>dfs.namenode.servicerpc-address.daily-cdh.namenode228</name>
    <value>192.168.28.200:8022</value>
  </property>
  <property>
    <name>dfs.namenode.http-address.daily-cdh.namenode228</name>
    <value>192.168.28.200:50070</value>
  </property>
  <property>
    <name>dfs.namenode.https-address.daily-cdh.namenode228</name>
    <value>192.168.28.200:50470</value>
  </property>
  <property>
    <name>dfs.namenode.rpc-address.daily-cdh.namenode295</name>
    <value>192.168.28.200:9000</value>
  </property>
  <property>
    <name>dfs.namenode.servicerpc-address.daily-cdh.namenode295</name>
    <value>192.168.28.200:8022</value>
  </property>
  <property>
    <name>dfs.namenode.http-address.daily-cdh.namenode295</name>
    <value>192.168.28.200:50070</value>
  </property>
  <property>
    <name>dfs.namenode.https-address.daily-cdh.namenode295</name>
    <value>192.168.28.200:50470</value>
  </property>
  <property>
    <name>dfs.replication</name>
    <value>2</value>
  </property>
  <property>
    <name>dfs.blocksize</name>
    <value>134217728</value>
  </property>
  <property>
    <name>dfs.client.use.datanode.hostname</name>
    <value>false</value>
  </property>
  <property>
    <name>fs.permissions.umask-mode</name>
    <value>022</value>
  </property>
  <property>
    <name>dfs.namenode.acls.enabled</name>
    <value>false</value>
  </property>
  <property>
    <name>dfs.client.use.legacy.blockreader</name>
    <value>false</value>
  </property>
  <property>
    <name>dfs.client.read.shortcircuit</name>
    <value>false</value>
  </property>
  <property>
    <name>dfs.domain.socket.path</name>
    <value>/var/run/hdfs-sockets/dn</value>
  </property>
  <property>
    <name>dfs.client.read.shortcircuit.skip.checksum</name>
    <value>false</value>
  </property>
  <property>
    <name>dfs.client.domain.socket.data.traffic</name>
    <value>false</value>
  </property>
  <property>
    <name>dfs.datanode.hdfs-blocks-metadata.enabled</name>
    <value>true</value>
  </property>
</configuration>
```

## userHostname
当客户端导入数据到HDFS过程中，客户端会使用hostname（域名）而不是ip地址的方式去连接HDFS DataNode地址。当用户利用Docker Compose的方式启动hadoop环境(例如：[Hudi测试环境](https://hudi.apache.org/docs/next/docker_demo))
，客户端取得的DataNode地址一般会是Docker容器的内部Ip地址，从容器外部是访问不到的，此时将该选项设置为`是`，可以解决数据无法导入到HDFS的问题。

详细请查看[https://segmentfault.com/q/1010000008473574](https://segmentfault.com/q/1010000008473574)

当选择`是`，在创HDFS FileSystem实例时加上如下参数：
```java
  conf.set("dfs.client.use.datanode.hostname", "true");
```
