package com.test.datasync;

import com.google.common.base.Preconditions;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.execution.datasources.jdbc.JDBCOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.clickhouse.settings.ClickHouseQueryParam;
import scala.collection.JavaConverters;
import scala.collection.Seq;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * clickhouse spark write
 * * spark写入clickhouse本地表，写入之前对数据进行排序后导入
 * * 排序好的数据再导入有利于ck文件合并
 */
public class SparkToClickhouseWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SparkToClickhouseWriter.class);

    private static final String CLICKHOUSE_JDBC_URL_TEMPLATE = "jdbc:clickhouse://%hosts%/%database%";

    private final ClickhouseJdbcConfig chConfig;
    private final SparkToClickhouseWriterSettings settings;
    private final ClickhouseQueryService clickhouseQueryService;
    private static final Integer MAX_NUMBER = 10000;

    public SparkToClickhouseWriter(ClickhouseJdbcConfig chConfig,
                                   SparkToClickhouseWriterSettings settings,
                                   ClickhouseQueryService clickhouseQueryService) {
        this.chConfig = chConfig;
        this.settings = settings;
        this.clickhouseQueryService = clickhouseQueryService;
    }

    /**
     * DataFrame to Clickhouse.
     *
     * @param df Data frame.
     */
    public void save(Dataset<Row> df) throws InterruptedException {
        saveDataset(df);
    }

    private void saveDataset(final Dataset<Row> df) throws InterruptedException {
        Map<Integer, String> activeServersForEachShard = clickhouseQueryService
                .findActiveServersForEachShard(chConfig.getClusterName());

        checkShardsNumbersAreValid(activeServersForEachShard.keySet());
        int shardsNum = activeServersForEachShard.size();
        Dataset<Row>[] datasets = df.randomSplit(shardsWeight(shardsNum), System.currentTimeMillis());
        List<Callable<Boolean>> tasks = activeServersForEachShard.entrySet().stream()
                .map(shardNumToServer -> (Callable<Boolean>) () ->
                        saveDatasetIntoShard(datasets[shardNumToServer.getKey() - 1], shardNumToServer))
                .collect(Collectors.toList());

        runInParallel(tasks);
    }


    public double[] shardsWeight(int shardsNum) {
        if (shardsNum == 0) {
            LOGGER.error("分片数为0，不在[1-N]的范围内");
            throw new RuntimeException("分片数为0，不在[1-N]的范围内");
        }
        //假设weight为10000,实际权重需要除以10000
        int number = MAX_NUMBER;
        Double[] weight = new Double[shardsNum];
        //初始值，所有权重相同
        Arrays.fill(weight, 1D);
        //已分配权重
        double[] count = new double[weight.length];
        for (int i = 0; i < number; i++) {
            //当前权重
            Double[] current = new Double[weight.length];
            for (int w = 0; w < weight.length; w++) {
                current[w] = weight[w] / (count[w] == 0D ? 1 : count[w]);
            }
            int index = 0;
            Double currentMax = current[0];
            for (int d = 1; d < current.length; d++) {
                //考虑全等的情况
                boolean isTrue = true;
                while (isTrue) {
                    Set<Double> set = new HashSet<>(Arrays.asList(current));
                    //代表全等
                    if (set.size() == 1) {
                        for (int e = 0; e < current.length; e++) {
                            current[e] = current[e] * Math.random();
                        }
                    } else {
                        isTrue = false;
                    }
                }
                //比较所有的数,寻找出下标最大的哪一位
                if (currentMax < current[d]) {
                    currentMax = current[d];
                    index = d;
                }
            }
            count[index] = count[index] == 0D ? 1 : count[index] + 1;
        }
        //打乱权重并恢复权重总和为1,避免每次同步导致单节点的权重过高
        return shuffle(count);
    }

    public double[] shuffle(double[] arr) {
        double[] arr2 = new double[arr.length];
        int count = arr.length;
        // 索引
        int cbRandCount = 0;
        // 位置
        int cbPosition = 0;
        int k = 0;
        Random rand;
        do {
            rand = new Random();
            int r = count - cbRandCount;
            cbPosition = rand.nextInt(r);
            arr2[k++] = arr[cbPosition]/MAX_NUMBER;
            cbRandCount++;
            arr[cbPosition] = arr[r - 1];
        } while (cbRandCount < count);
        return arr2;
    }


    private void runInParallel(List<Callable<Boolean>> tasks) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(tasks.size(),
                new NamedDefaultThreadFactory("saveToChShards"));
        try {
            List<Future<Boolean>> futures = executorService.invokeAll(tasks);

            futures.forEach(f -> {
                try {
                    f.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });
        } finally {
            executorService.shutdown();
        }
    }
    private void checkShardsNumbersAreValid(Set<Integer> shards) {
        Preconditions.checkArgument(shards.size() > 0, "空的分片信息.");
        String error = "分片值处于[0,size-1]" +
                "Shards=" + shards.stream().sorted().collect(Collectors.toList());
        Preconditions.checkArgument(shards.stream().mapToInt(Integer::intValue)
                .min().getAsInt() == 1, error);
        Preconditions.checkArgument(shards.stream().mapToInt(Integer::intValue)
                .max().getAsInt() == shards.size(), error);
    }

    private Boolean saveDatasetIntoShard(final Dataset<Row> df,
                                         Map.Entry<Integer, String> shardNumToServer) {

//        重新分配分区
//        df.repartition(shardsNum * shardInsertParallelism(), shardingColumn())
//                .sortWithinPartitions(sortingDeduplicationKey);
//           .foreachPartition(new ForeachPartitionFunction(...))
//        Column shardCondition = lit(random.nextInt(MAX_NUMBER))
//                .mod(shardsNum)
//                .equalTo(shardNumToServer.getKey() - 1);
        Dataset<Row> shardDf;
        if (settings.getShardInsertParallelism() != 0) {
            shardDf = df
                    .repartition(settings.getShardInsertParallelism());
        } else {
            shardDf = df;
        }
        if (isDeduplicationEnabled()) {
            shardDf = shardDf.sortWithinPartitions(listToSeq(settings.getDeduplicationSortingKey()));
        }
        shardDf.write()
                .mode(SaveMode.Append)
                .option(JDBCOptions.JDBC_BATCH_INSERT_SIZE(), String.valueOf(chConfig.getBatchSize()))
                .option("socket_timeout", "300000")
                .jdbc(getUrlForHost(shardNumToServer.getValue()),
                        chConfig.getDatabase() + "." + chConfig.getLocalTable(),
                        jdbcWriteOptions());
        return true;
    }

    private static Seq<Column> listToSeq(List<Column> list) {
        return JavaConverters.iterableAsScalaIterableConverter(list).asScala().toSeq();
    }

    private boolean isDeduplicationEnabled() {
        return !settings.getDeduplicationSortingKey().isEmpty();
    }

    private Properties jdbcWriteOptions() {
        Properties properties = new Properties();
        properties.put("driver", chConfig.getDriver());
        properties.put("autocommit", chConfig.getAutoCommit());
        properties.put("rewriteBatchedStatements", "true");
        properties.put("user", chConfig.getUser());
        properties.put("password", chConfig.getPassword());
        if (isDeduplicationEnabled()) {
            properties.put(ClickHouseQueryParam.INSERT_DEDUPLICATE.getKey(), "1");
        }
        return properties;
    }

    private String getUrlForHost(String host) {
        return CLICKHOUSE_JDBC_URL_TEMPLATE
                .replace("%hosts%", host + ":" + chConfig.getPort())
                .replace("%database%", chConfig.getDatabase());
    }

    public void execute(String sql, HiveToCkSparkParams hiveToCkSparkParams) throws SQLException, ClassNotFoundException {

        Connection connection = null;
        Statement statement = null;
        Class.forName(hiveToCkSparkParams.getDriver());
        connection = DriverManager.getConnection(getUrlForHost(hiveToCkSparkParams.getActiveServers().get(0)),
                chConfig.getUser(), chConfig.getPassword());
        statement = connection.createStatement();
        statement.execute(sql);
        try{
            statement.close();
            connection.close();
        }catch (Exception e){
        }
    }
    /**
     * 线程工厂
     */
    private static class NamedDefaultThreadFactory implements ThreadFactory {

        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        NamedDefaultThreadFactory(String prefix) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            namePrefix = prefix + "-pool-" +
                    poolNumber.getAndIncrement() +
                    "-thread-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }
}

SparkToClickhouseWriterSettings:
        package com.test.datasync;

        import org.apache.spark.sql.Column;
        import org.apache.spark.sql.functions;

        import java.util.Collections;
        import java.util.List;
        import java.util.stream.Collectors;

        import static org.apache.spark.sql.functions.*;


public class SparkToClickhouseWriterSettings {
    private final Integer shardInsertParallelism;
    private final Column shardingColumn;
    private final List<Column> deduplicationSortingKey;

    public SparkToClickhouseWriterSettings(
            Integer shardInsertParallelism,
            String shardingColumn,
            List<String> sortingKey) {
        this.shardingColumn = abs(hash(col(shardingColumn)));
        this.shardInsertParallelism = shardInsertParallelism;
        this.deduplicationSortingKey = sortingKey != null ?
                sortingKey.stream().map(functions::col).collect(Collectors.toList()) :
                Collections.emptyList();
    }

    public Column getShardingColumn() {
        return shardingColumn;
    }

    public int getShardInsertParallelism() {
        return shardInsertParallelism;
    }

    public List<Column> getDeduplicationSortingKey() {
        return deduplicationSortingKey;
    }
}