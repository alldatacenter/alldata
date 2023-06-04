

# Database related configuration, set database type, username and password
export DATABASE=mysql
export SPRING_PROFILES_ACTIVE=$DATABASE

# DolphinScheduler server related configuration
export SPRING_CACHE_TYPE=none
export SPRING_JACKSON_TIME_ZONE=UTC
<#--handle dependent.zookeeper-->
<#if dependencies.ZOOKEEPER??>
    <#assign zookeeper=dependencies.ZOOKEEPER quorum=[]>
    <#list zookeeper.serviceRoles['ZOOKEEPER_SERVER'] as role>
        <#assign quorum += [role.hostname + ":" + zookeeper.conf["zookeeper.client.port"]]>
    </#list>
</#if>
# Registry center configuration, determines the type and link of the registry center
export REGISTRY_TYPE=zookeeper
export REGISTRY_ZOOKEEPER_CONNECT_STRING=${quorum?join(",")}

# Tasks related configurations, need to change the configuration if you use the related tasks.
export HADOOP_HOME=/opt/soft/hadoop
export HADOOP_CONF_DIR=/opt/soft/hadoop/etc/hadoop
export SPARK_HOME1=/opt/soft/spark1
export SPARK_HOME2=/opt/soft/spark2
export PYTHON_HOME=/opt/soft/python
export HIVE_HOME=/opt/soft/hive
export FLINK_HOME=/opt/soft/flink
export DATAX_HOME=/opt/soft/datax

export PATH=$HADOOP_HOME/bin:$SPARK_HOME1/bin:$SPARK_HOME2/bin:$PYTHON_HOME/bin:$JAVA_HOME/bin:$HIVE_HOME/bin:$FLINK_HOME/bin:$DATAX_HOME/bin:$PATH
