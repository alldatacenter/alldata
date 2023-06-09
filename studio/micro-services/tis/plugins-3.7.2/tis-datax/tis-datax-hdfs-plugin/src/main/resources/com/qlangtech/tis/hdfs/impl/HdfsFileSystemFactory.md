## hdfsSiteContent

配置实例,实现了HDFS HA高可用方案：

[hdfs-site.xml参数详解](https://hadoop.apache.org/docs/stable/hadoop-project-dist/hadoop-hdfs/hdfs-default.xml)

```xml
<configuration>
    <property>
        <name>fs.defaultFS</name>
        <value>hdfs://192.168.28.200</value>
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
