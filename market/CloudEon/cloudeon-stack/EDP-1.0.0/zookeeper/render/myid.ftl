<#assign hosts=serviceRoles['ZOOKEEPER_SERVER']>
<#list hosts as host>
<#if host.hostname == localhostname>
${host.id % 254 + 1}
</#if>
</#list>
