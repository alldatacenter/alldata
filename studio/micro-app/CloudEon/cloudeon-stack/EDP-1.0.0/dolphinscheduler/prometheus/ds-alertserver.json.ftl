[
{
"targets":[<#list serviceRoles['DS_ALERT_SERVER'] as item>"${item.hostname}:${conf['alert.server.port']}"<#sep>,</#list>]
}
]
