<#list confFiles['be.conf']?keys as key>
    ${key}=${confFiles['be.conf'][key]}
</#list>
PPROF_TMPDIR="/opt/edp/${service.serviceName}/log"

# since 1.2, the JAVA_HOME need to be set to run BE process.
# JAVA_HOME=/path/to/jdk/

priority_networks = ${localhostip}


storage_root_path = /opt/edp/${service.serviceName}/data


sys_log_dir = /opt/edp/${service.serviceName}/log