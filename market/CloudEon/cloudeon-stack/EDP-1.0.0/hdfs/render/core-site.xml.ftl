<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<#--Simple macro definition-->
<#macro property key value>
<property>
    <name>${key}</name>
    <value>${value}</value>
</property>
</#macro>


<#--------------------------->
<#assign
    serviceName=service.serviceName
>
<configuration>
<#if serviceRoles['HDFS_NAMENODE']?size gt 1>
    <#assign fs_default_uri = "hdfs://" + conf['nameservices']>
<#else >
    <#assign
    namenode=serviceRoles['HDFS_NAMENODE'][0].hostname
    namenodeport=conf['namenode.rpc-port']
    fs_default_uri = "hdfs://" + namenode + ":" + namenodeport
    >
</#if>

<@property "fs.defaultFS" fs_default_uri/>

<#--handle dependent.zookeeper-->
<#if dependencies.ZOOKEEPER??>
    <#assign zookeeper=dependencies.ZOOKEEPER quorum=[]>
    <#list zookeeper.serviceRoles['ZOOKEEPER_SERVER'] as role>
        <#assign quorum += [role.hostname + ":" + zookeeper.conf["zookeeper.client.port"]]>
    </#list>
    <@property "ha.zookeeper.quorum" quorum?join(",")/>
    <@property "ha.zookeeper.parent-znode" "/" + serviceName + "-ha"/>
</#if>

<#--hadoop.proxyuser.[hive, hue, httpfs, oozie].[hosts,groups]-->
<#assign services=["root","yarn","hadoop","spark","zookeeper","kyuubi","flink","hdfs","hbase","hive", "hue", "httpfs"]>
<#list services as s>
    <@property "hadoop.proxyuser." + s + ".hosts" "*"/>
    <@property "hadoop.proxyuser." + s + ".groups" "*"/>
</#list>
<#--<@property "net.topology.node.switch.mapping.impl" "org.apache.hadoop.net.ScriptBasedMapping"/>-->
<#--<@property "net.topology.script.file.name" "/opt/rack_map.sh"/>-->
<#--Take properties from the context-->
<#list confFiles['core-site.xml'] as key, value>
    <@property key value/>
</#list>
</configuration>
