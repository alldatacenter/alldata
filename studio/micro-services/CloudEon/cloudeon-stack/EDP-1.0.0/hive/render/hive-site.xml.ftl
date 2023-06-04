<?xml version="1.0" encoding="utf-8"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
<#--Simple macro definition-->
<#macro property key value>
<property>
    <name>${key}</name>
    <value>${value}</value>
</property>
</#macro>

<configuration>
<#--    jmx监控-->
    <@property "hive.server2.metrics.enabled" "true"/>
    <@property "hive.metastore.metrics.enabled" "true"/>
    <@property "hive.service.metrics.class" "org.apache.hadoop.hive.common.metrics.metrics2.CodahaleMetrics"/>
<#--    hive.metastore.uris-->
    <#assign metastore=serviceRoles['HIVE_METASTORE'] metastore_uri=[]>
    <#list metastore as role>
        <#assign metastore_uri += ["thrift://"+role.hostname + ":" + conf["hive.metastore.thrift.port"]]>
    </#list>
    <@property "hive.metastore.uris" metastore_uri?join(",")/>
<#--hiveserver2操作日志-->
    <#assign operationLog=conf['enable.hiveserver2.operation.log']
    >
    <#if operationLog == "true">
    <@property "hive.server2.logging.operation.enabled" operationLog/>
    <@property "hive.server2.logging.operation.log.location" "/opt/edp/${service.serviceName}/log/operation_logs"/>
    <@property "hive.server2.logging.operation.level" "${conf['hive.server2.logging.operation.level']}"/>
    </#if>


    <#list confFiles['hive-site.xml'] as key, value>
        <@property key value/>
    </#list>
</configuration>

