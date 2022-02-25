package com.test.datasync;

import com.google.gson.Gson;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import ru.yandex.clickhouse.util.apache.StringUtils;

import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据同步主类
 * {@link HiveToCkSparkParams}中的activeServers是多节点配置
 */
public class ClickHouseDataSync {

    /***
     * 默认一批多少数据
     */
    private static final int DEFAULT_SIZE = 50000;

    /***
     * 默认用户名
     */
    private static final String DEFAULT_USER = "default";

    /***
     * 默认数据库名
     */
    private static final String DEFAULT_DATABASES = "default";

    /***
     * 默认驱动名称
     */
    private static final String DEFAULT_DRIVER = "ru.yandex.clickhouse.ClickHouseDriver";

    /***
     * 参数示例
     * "{\"appName\":\"Spark Hive To Clickhouse\",\"hiveSqls\":[\"select * from default.lineitem_hdfs_test1_local3\"],
     * \"clusterName\":\"test_cluster\",\"database\":\"default\",\"localTable\":\"lineitem_hdfs_test1_local_load5\",
     * \"driver\":\"ru.yandex.clickhouse.ClickHouseDriver\",\"user\":\"default\",\"password\":\"test\",\"batchSize\":50000,
     * \"port\":\"20842\",\"shardInsertParallelism\":4,\"sortKey\":[\"l_partkey\"],\"activeServers\":[\"test-test-node1\"]}"
     * shardInsertParallelism为0表示不进行重分区,避免不必要的shuffle过程
     * 提交脚本
     * /usr/hdp/current/spark2-client/bin/spark-submit
     * --master yarn --deploy-mode cluster
     * --class com.test.datasync.ClickHouseDataSync
     * --driver-cores 1 --driver-memory 2G
     * --num-executors 2 --executor-cores 2
     * --executor-memory 4G --queue default
     * kylin-spark-clickhouse-4.0.0-beta.jar
     * "{\"appName\":\"Spark Hive To Clickhouse\",\"shardKey\":\"l_partkey\",
     * \"hiveSqls\":[\"select * from default.lineitem_hdfs_test1_local3\"],
     * \"clusterName\":\"test_cluster\",\"database\":\"default\",
     * \"localTable\":\"lineitem_hdfs_test1_local_load8\",
     * \"driver\":\"ru.yandex.clickhouse.ClickHouseDriver\",
     * \"user\":\"default\",\"password\":\"test1234\",
     * \"batchSize\":50000,\"port\":\"20840\",\"shardInsertParallelism\":1,
     * \"sortKey\":[\"l_partkey\"],\"activeServers\":[\"test-test-node2\",
     * \"test-test-node3\"]}"
     */
    public static void main(String[] args) throws SQLException, ClassNotFoundException, InterruptedException {
        if (args.length == 0) {
            throw new RuntimeException("必须包含一个参数信息");
        }
        String arg = args[0];
        Gson gson = new Gson();
        if (StringUtils.isBlank(arg)) {
            throw new RuntimeException("参数信息不能为空");
        }
        HiveToCkSparkParams hiveToCkSparkParams = gson.fromJson(arg, HiveToCkSparkParams.class);
        SparkSession spark = SparkSession
                .builder()
                //数据同步中不允许task级别的重试，因为ck没有事务，所以会导致数据重复
                .config("spark.task.maxFailures", "1")
                .appName(hiveToCkSparkParams.getAppName())
                .enableHiveSupport()
                .getOrCreate();
        List<String> sqlList = hiveToCkSparkParams.getHiveSqls();
        for (String sql : sqlList) {
            sql = URLDecoder.decode(sql);
            if (sql.toLowerCase().startsWith("truncate") || sql.toLowerCase().startsWith("drop")) {
                if (sql.toLowerCase().startsWith("truncate")) {
                    sql = sql.replaceAll("\\$cluster", hiveToCkSparkParams.getClusterName());
                }
                //执行truncate、drop
                exe(hiveToCkSparkParams, sql);
            } else {
                //执行insert
                exeSave(hiveToCkSparkParams, sql, spark);
            }
        }
    }
    private static void exe(HiveToCkSparkParams hiveToCkSparkParams, String sql) throws SQLException, ClassNotFoundException {
        SparkToClickhouseWriter sparkToClickhouseWriter = getSparkToClickhouseWriter(hiveToCkSparkParams);
        sparkToClickhouseWriter.execute(sql, hiveToCkSparkParams);
    }

    private static void exeSave(HiveToCkSparkParams hiveToCkSparkParams, String sql, SparkSession spark) throws InterruptedException {
        Dataset<Row> streamDF = spark.sql(sql);
        SparkToClickhouseWriter sparkToClickhouseWriter = getSparkToClickhouseWriter(hiveToCkSparkParams);
        sparkToClickhouseWriter.save(streamDF);
    }

    private static SparkToClickhouseWriter getSparkToClickhouseWriter(HiveToCkSparkParams hiveToCkSparkParams) {

        List<String> activeServers = hiveToCkSparkParams.getActiveServers();
        Map<Integer, String> cluster = new HashMap<>(activeServers.size());
        for (int i = 0; i < activeServers.size(); i++) {
            cluster.put(i + 1, activeServers.get(i));
        }
        return new SparkToClickhouseWriter(new ClickhouseJdbcConfig() {
            @Override
            public String getClusterName() {
                return hiveToCkSparkParams.getClusterName();
            }

            @Override
            public String getDatabase() {
                String database = hiveToCkSparkParams.getDatabase();
                return StringUtils.isBlank(database) ? DEFAULT_DATABASES : database;
            }

            @Override
            public String getLocalTable() {
                return hiveToCkSparkParams.getLocalTable();
            }

            @Override
            public String getDriver() {
                String driver = hiveToCkSparkParams.getDriver();
                return StringUtils.isBlank(driver) ? DEFAULT_DRIVER : driver;
            }

            @Override
            public String getAutoCommit() {
                return "false";
            }

            @Override
            public String getUser() {
                String user = hiveToCkSparkParams.getUser();
                return StringUtils.isBlank(user) ? DEFAULT_USER : user;
            }

            @Override
            public String getPassword() {
                return hiveToCkSparkParams.getPassword();
            }

            @Override
            public int getBatchSize() {
                return hiveToCkSparkParams.getBatchSize() <= 0 ? DEFAULT_SIZE : hiveToCkSparkParams.getBatchSize();
            }

            @Override
            public String getPort() {
                return hiveToCkSparkParams.getPort();
            }
        }, new SparkToClickhouseWriterSettings(hiveToCkSparkParams.getShardInsertParallelism(), hiveToCkSparkParams.getShardKey(), hiveToCkSparkParams.getSortKey()), new ClickhouseQueryService() {
            @Override
            public Map<Integer, String> findActiveServersForEachShard(String clusterName) {
                //后续拓展，方便直接在spark任务中根据集群名称判断集群信息
                return cluster;
            }
        });
    }


}