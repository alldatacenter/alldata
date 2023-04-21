
# Default system properties included when running spark-submit.
# This is useful for setting default environmental settings.


<#macro property key value>
${key}   ${value}
</#macro>

<#list confFiles['spark-defaults.conf'] as key, value>
    <@property key value/>
</#list>

<@property "spark.yarn.historyServer.address"  "${localhostname}:${conf['spark.history.ui.port']}" />
