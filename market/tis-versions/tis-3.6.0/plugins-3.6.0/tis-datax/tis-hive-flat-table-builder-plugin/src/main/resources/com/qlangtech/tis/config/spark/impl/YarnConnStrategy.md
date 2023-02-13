## yarnSite

```xml
<?xml version="1.0"?>
<configuration>
 <!-- Site specific YARN configuration properties -->
  <!--RM的主机名 -->
  <property>
    <name>yarn.resourcemanager.hostname</name>
    <value>192.168.28.200</value>
  </property>

  <!--RM对客户端暴露的地址,客户端通过该地址向RM提交应用程序、杀死应用程序等-->
  <property>
    <name>yarn.resourcemanager.address</name>
    <value>${yarn.resourcemanager.hostname}:8032</value>
  </property>

  <!--RM对AM暴露的访问地址,AM通过该地址向RM申请资源、释放资源等-->
  <property>
    <name>yarn.resourcemanager.scheduler.address</name>
    <value>${yarn.resourcemanager.hostname}:8030</value>
  </property>

  <!--RM对外暴露的web http地址,用户可通过该地址在浏览器中查看集群信息-->
  <property>
    <name>yarn.resourcemanager.webapp.address</name>
    <value>${yarn.resourcemanager.hostname}:8088</value>
  </property>

  <!--RM对NM暴露地址,NM通过该地址向RM汇报心跳、领取任务等-->
  <property>
    <name>yarn.resourcemanager.resource-tracker.address</name>
    <value>${yarn.resourcemanager.hostname}:8031</value>
  </property>

  <!--RM对管理员暴露的访问地址,管理员通过该地址向RM发送管理命令等-->
  <property>
    <name>yarn.resourcemanager.admin.address</name>
    <value>${yarn.resourcemanager.hostname}:8033</value>
  </property>
</configuration>
```
