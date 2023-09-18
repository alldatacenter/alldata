<?xml version="1.0" encoding="utf-8"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>

<#macro property key value>
    <property>
        <name>${key}</name>
        <value>${value}</value>
    </property>
</#macro>

<configuration>
    <#--    hive.metastore.warehouse.dir-->
    <#--    hive.metastore.uris-->
    <#if dependencies.HIVE??>
        <#assign hive=dependencies.HIVE>
        <#assign metastore=hive.serviceRoles['HIVE_METASTORE'] metastore_uri=[]>
        <#list metastore as role>
            <#assign metastore_uri += ["thrift://"+role.hostname + ":" + hive.conf["hive.metastore.thrift.port"]]>
        </#list>
        <@property "hive.metastore.uris" metastore_uri?join(",")/>
        <@property "hive.metastore.warehouse.dir" hive.conf['hive.metastore.warehouse.dir']/>
    </#if>

</configuration>

