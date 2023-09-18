<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<#--Simple macro definition-->
<#macro property key value>
<property>
    <name>${key}</name>
    <value>${value}</value>
</property>
</#macro>
<configuration>
<#--Take properties from the context-->
<#list confFiles['capacity-scheduler.xml'] as key, value>
    <@property key value/>
</#list>
</configuration>
