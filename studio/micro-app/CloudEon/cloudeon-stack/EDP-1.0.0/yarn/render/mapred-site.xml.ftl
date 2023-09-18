<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<#--Simple macro definition-->
<#macro property key value>
<property>
    <name>${key}</name>
    <value>${value}</value>
</property>
</#macro>
<#--------------------------->
<configuration>
    <#if serviceRoles['YARN_HISTORYSERVER']??>
    <#assign historyServer=serviceRoles['YARN_HISTORYSERVER'][0]['hostname']>
    <@property "mapreduce.jobhistory.address" historyServer + ":${conf['historyserver.port']}"/>
    <@property "mapreduce.jobhistory.webapp.address" historyServer + ":${conf['historyserver.http-port']}"/>
    <@property "mapreduce.jobhistory.admin.address" "0.0.0.0" + ":${conf['historyserver.admin-port']}"/>
    </#if>


<#--Take properties from the context-->
<#list confFiles['mapred-site.xml'] as key, value>
    <@property key value/>
</#list>
</configuration>
