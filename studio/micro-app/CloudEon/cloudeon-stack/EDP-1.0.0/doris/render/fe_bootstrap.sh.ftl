#!/bin/bash
basedir=$(cd `dirname $0`/..; pwd)
<#if serviceRoles['DORIS_FE'][0].hostname == localhostname>
    $DORIS_HOME/doris-fe/bin/start_fe.sh --daemon
<#else >
cluster_file="/opt/edp/${service.serviceName}/conf/doris-meta/image/VERSION"
first_start_script="$DORIS_HOME/doris-fe/bin/start_fe.sh --daemon --helper  ${serviceRoles['DORIS_FE'][0].hostname}:${conf['edit_log_port']}"
start_script=" $DORIS_HOME/doris-fe/bin/start_fe.sh --daemon"

# 检查 cluster_file 文件是否存在
if [ ! -f "$cluster_file" ]; then
    # 如果 cluster_file 文件不存在，则执行 first_start_script
    echo "cluster_file not found,first starting service..."
    sh $first_start_script
else
    # 如果 cluster_file 文件存在，则执行 start_script
    echo "cluster_file found, starting service..."
    sh $start_script
fi

</#if>



tail -f /dev/null

