[
{
"targets":[<#list  serviceRoles['DORIS_FE'] as item>"${item.hostname}:${conf['http_port']}"<#sep>,</#list>],
"labels":{"group":"fe"}
},
{
"targets":[<#list  serviceRoles['DORIS_BE'] as item>"${item.hostname}:${conf['webserver_port']}"<#sep>,</#list>],
"labels":{"group":"be"}
}
]
