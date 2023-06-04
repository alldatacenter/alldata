<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<#--Simple macro definition-->
<#macro property key value>
<property>
    <name>${key}</name>
    <value>${value}</value>
</property>
</#macro>
<configuration>

    <@property "httpfs.hadoop.config.dir" "/opt/edp/${service.serviceName}/conf"/>
    <@property "httpfs.http.port" "${conf['httpfs.http-port']}"/>


    <#assign services=["root","yarn","hadoop","spark","zookeeper","kyuubi","flink","hdfs","hbase","hive", "hue", "httpfs"]>
    <#list services as s>
        <@property "httpfs.proxyuser." + s + ".hosts" "*"/>
        <@property "httpfs.proxyuser." + s + ".groups" "*"/>
    </#list>

    <#if confFiles['httpfs-site.xml']??>
        <#list confFiles['httpfs-site.xml'] as key, value>
            <@property key value/>
        </#list>
    </#if>
</configuration>
