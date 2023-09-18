export SPARK_DIST_CLASSPATH=${SPARK_DIST_CLASSPATH}
export HADOOP_CONF_DIR=${HADOOP_CONF_DIR}
export YARN_CONF_DIR=${YARN_CONF_DIR}
<#list itemList as item>
export ${item.name}=${item.value}
</#list>