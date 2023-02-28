# hudi relevant
#mvn install -Dmaven.test.skip=true -pl tis-datax/tis-datax-local-embedded-executor,tis-datax/tis-datax-local-executor,tis-datax/tis-hive-flat-table-builder-plugin,tis-datax/tis-ds-mysql-v5-plugin,tis-datax/tis-datax-hudi-plugin,tis-incr/tis-flink-cdc-mysql-plugin,tis-incr/tis-sink-hudi-plugin -am -Dappname=all

# starrocks relevant
mvn package -Dmaven.test.skip=true -pl tis-datax/tis-datax-doris-starrocks-plugin,tis-incr/tis-sink-starrocks-plugin,tis-datax/tis-datax-local-embedded-executor,tis-datax/tis-datax-local-executor,tis-datax/tis-hive-flat-table-builder-plugin,tis-datax/tis-ds-mysql-v5-plugin,tis-incr/tis-flink-cdc-mysql-plugin -am -Dappname=all
