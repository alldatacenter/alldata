<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<#--Simple macro definition-->
<#macro property key value>
<property>
  <name>${key}</name>
  <value>${value}</value>
</property>
</#macro>


<configuration>


  <@property "hbase.cluster.distributed" "true"/>
  <@property "hbase.rootdir" "/${service.serviceName}"/>

  <#--handle dependent.zookeeper-->
  <#if dependencies.ZOOKEEPER??>
    <#assign zookeeper=dependencies.ZOOKEEPER quorum=[]>
    <#list zookeeper.serviceRoles['ZOOKEEPER_SERVER'] as role>
      <#assign quorum += [role.hostname + ":" + zookeeper.conf["zookeeper.client.port"]]>
    </#list>
    <@property "hbase.zookeeper.quorum" quorum?join(",")/>
    <@property "zookeeper.znode.parent" "/${service.serviceName}"  />
  </#if>


  <#list confFiles['hbase-site.xml'] as key, value>
    <@property key value/>
  </#list>
</configuration>
