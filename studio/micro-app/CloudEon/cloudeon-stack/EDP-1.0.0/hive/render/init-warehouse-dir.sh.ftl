#!/bin/bash


HADOOP_CONF_DIR="/opt/edp/${service.serviceName}/conf"
HDFS_HOME="${r"${HADOOP_HOME}"}"
HIVE_WAREHOUSE_DIR="${conf['hive.metastore.warehouse.dir']}"
HIVE_TMP_DIR="${conf['hive.exec.scratchdir']}"



/bin/bash -c "${r"${HDFS_HOME}"}/bin/hadoop --config ${r"${HADOOP_CONF_DIR}"}   fs -test -e  ${r"${HIVE_WAREHOUSE_DIR}"}"
if [ $? -eq 0 ] ;then
    echo "${r"${HIVE_WAREHOUSE_DIR}"} already exists."
else
    echo "${r"${HIVE_WAREHOUSE_DIR}"} does not exist."
    /bin/bash -c "${r"${HDFS_HOME}"}/bin/hadoop --config ${r"${HADOOP_CONF_DIR}"} fs  -mkdir -p ${r"${HIVE_WAREHOUSE_DIR}"}"
    /bin/bash -c "${r"${HDFS_HOME}"}/bin/hadoop --config ${r"${HADOOP_CONF_DIR}"} fs  -chmod  -R 777 ${r"${HIVE_WAREHOUSE_DIR}"}"
fi

/bin/bash -c "${r"${HDFS_HOME}"}/bin/hadoop --config ${r"${HADOOP_CONF_DIR}"} fs  -test -e  ${r"${HIVE_TMP_DIR}"}"
if [ $? -eq 0 ] ;then
    echo "${r"${HIVE_TMP_DIR}"} already exists."
else
    echo "$HIVE_TMP_DIR does not exist."
    /bin/bash -c "${r"${HDFS_HOME}"}/bin/hadoop --config ${r"${HADOOP_CONF_DIR}"} fs  -mkdir -p ${r"${HIVE_TMP_DIR}"}"
    /bin/bash -c "${r"${HDFS_HOME}"}/bin/hadoop --config ${r"${HADOOP_CONF_DIR}"} fs  -chmod  -R 777 ${r"${HIVE_TMP_DIR}"}"
fi