#!/bin/bash



HDFS_HOME="$HADOOP_HOME"
HDFS_CONF_DIR="/opt/edp/${service.serviceName}/conf"
YARN_LOGS_DIR="${conf['remote.app.log.dir']}"
MAPREDUCE_STAGE_DIR="${conf['mapreduce.am.staging.dir']}"




 /bin/bash -c "$HDFS_HOME/bin/hadoop --config  $HDFS_CONF_DIR  fs -test -e  $YARN_LOGS_DIR"
if [ $? -eq 0 ] ;then
    echo "$YARN_LOGS_DIR already exists."
else
    echo "$YARN_LOGS_DIR does not exist."
     /bin/bash -c "$HDFS_HOME/bin/hadoop --config  $HDFS_CONF_DIR  fs -mkdir -p $YARN_LOGS_DIR"
     /bin/bash -c "$HDFS_HOME/bin/hadoop --config  $HDFS_CONF_DIR  fs -chmod  -R 777 $YARN_LOGS_DIR"
fi

 /bin/bash -c "$HDFS_HOME/bin/hadoop --config  $HDFS_CONF_DIR  fs -test -e  $MAPREDUCE_STAGE_DIR"
if [ $? -eq 0 ] ;then
    echo "$MAPREDUCE_STAGE_DIR already exists."
else
    echo "$MAPREDUCE_STAGE_DIR does not exist."
     /bin/bash -c "$HDFS_HOME/bin/hadoop --config  $HDFS_CONF_DIR  fs -mkdir -p $MAPREDUCE_STAGE_DIR"
     /bin/bash -c "$HDFS_HOME/bin/hadoop --config  $HDFS_CONF_DIR  fs -chmod  -R 777 $MAPREDUCE_STAGE_DIR"
fi

