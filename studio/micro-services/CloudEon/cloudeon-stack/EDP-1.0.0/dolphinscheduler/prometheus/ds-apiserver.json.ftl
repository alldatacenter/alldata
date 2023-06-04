[
{
"targets":[<#list serviceRoles['DS_API_SERVER'] as item>"${item.hostname}:${conf['api.server.port']}"<#sep>,</#list>]
}
]
