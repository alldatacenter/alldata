mvn test -DreportsDirectory=/tmp/surefire -Dmaven.test.failure.ignore=false  -pl\
 tis-flink-chunjun-postgresql-plugin\
,tis-flink-chunjun-clickhouse-plugin\
,tis-flink-chunjun-doris-plugin\
,tis-flink-chunjun-mysql-plugin\
,tis-flink-chunjun-oracle-plugin
