#!/bin/bash



HDFS_HOME="$HADOOP_HOME"
HDFS_CONF_DIR="/opt/edp/${service.serviceName}/conf"
SPARK_HISTORY_LOGS_DIR="${conf['spark.history.fs.logDirectory']}"




 /bin/bash -c "$HDFS_HOME/bin/hadoop --config  $HDFS_CONF_DIR  fs -test -e  $SPARK_HISTORY_LOGS_DIR"
if [ $? -eq 0 ] ;then
    echo "$SPARK_HISTORY_LOGS_DIR already exists."
else
    echo "$SPARK_HISTORY_LOGS_DIR does not exist."
     /bin/bash -c "$HDFS_HOME/bin/hadoop --config  $HDFS_CONF_DIR  fs -mkdir -p $SPARK_HISTORY_LOGS_DIR"
     /bin/bash -c "$HDFS_HOME/bin/hadoop --config  $HDFS_CONF_DIR  fs -chmod  -R 777 $SPARK_HISTORY_LOGS_DIR"
fi

