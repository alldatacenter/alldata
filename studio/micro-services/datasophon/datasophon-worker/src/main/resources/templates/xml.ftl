<?xml version="1.0" encoding="UTF-8"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
<configuration>
<#list itemList as item>
    <property>
        <name>${item.name}</name>
        <value>${item.value}</value>
    </property>
</#list>
</configuration>