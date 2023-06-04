#!/usr/bin/env bash

set -o errexit
set -o errtrace
set -o pipefail
set -o xtrace

source /opt/edp/${service.serviceName}/conf/hadoop-hdfs-env.sh
_METADATA_DIR=/opt/edp/${service.serviceName}/data/namenode/current



<#assign namenodes=serviceRoles["HDFS_NAMENODE"]>

<#if namenodes?size == 2>
    <#if namenodes[0].id lt namenodes[1].id>
        <#assign nn1=namenodes[0]>
        <#assign nn2=namenodes[1]>
    <#else>
        <#assign nn1=namenodes[1]>
        <#assign nn2=namenodes[0]>
    </#if>

    <#if nn1.hostname == .data_model["localhostname"]>
 if [[ ! -d ${r"$_METADATA_DIR"} ]]; then
     echo "无法检测到namenode元数据文件夹，开始进行namenode格式化。。。。。。。。。。"
     yes Y|  hdfs --config /opt/edp/${service.serviceName}/conf namenode -format ${conf['nameservices']}
 fi
    <#elseif nn2.hostname == .data_model["localhostname"]>
 if [[ ! -d ${r"$_METADATA_DIR"} ]]; then
   echo "检测到没有namenode的元数据文件夹，开始进行namenode的初始化操作，从checkpoint中加载。。。。。。。"
   yes Y|   hdfs --config /opt/edp/${service.serviceName}/conf namenode -bootstrapStandby
 fi
    </#if>

</#if>
