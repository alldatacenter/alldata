/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.connector.legacy.jdbc.split.strategy;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.util.Pair;
import com.bytedance.bitsail.connector.legacy.jdbc.exception.JDBCPluginErrorCode;
import com.bytedance.bitsail.connector.legacy.jdbc.extension.DatabaseInterface;
import com.bytedance.bitsail.connector.legacy.jdbc.model.DbClusterInfo;
import com.bytedance.bitsail.connector.legacy.jdbc.model.DbShardInfo;
import com.bytedance.bitsail.connector.legacy.jdbc.model.SqlType;
import com.bytedance.bitsail.connector.legacy.jdbc.options.JdbcReaderOptions;
import com.bytedance.bitsail.connector.legacy.jdbc.split.SplitParameterInfo;
import com.bytedance.bitsail.connector.legacy.jdbc.split.TableRangeInfo;
import com.bytedance.bitsail.connector.legacy.jdbc.split.cache.SplitInfoCache;
import com.bytedance.bitsail.connector.legacy.jdbc.split.cache.SplitInfoNoOpCache;
import com.bytedance.bitsail.flink.core.option.FlinkCommonOptions;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.flink.util.Preconditions.checkArgument;

/**
 * @desc:
 */
public class FixedLenParametersProvider<K extends Comparable> implements ParameterValuesProvider {
  private static final Logger LOG = LoggerFactory.getLogger(FixedLenParametersProvider.class);
  private final BitSailConfiguration inputSliceConfig;
  private final BitSailConfiguration commonConfig;
  private final int parallelismNum;
  private long fetchSize;
  private DbClusterInfo dbClusterInfo;
  private DatabaseInterface databaseInterface;
  private String driverClassName;
  private String initSql;
  private SplitParameterInfo<K> spInfo;
  private String filter;
  private ShardSplitMode shardSplitMode;

  FixedLenParametersProvider(DatabaseInterface databaseInterface, long fetchSize, DbClusterInfo dbClusterInfo, int parallelismNum,
                             String driverClassName, String filter, BitSailConfiguration inputSliceConfig,
                             BitSailConfiguration commonConfig, ShardSplitMode shardSplitMode) {
    this(databaseInterface, fetchSize, dbClusterInfo, parallelismNum, driverClassName, filter, inputSliceConfig, commonConfig,
        shardSplitMode, "");
  }

  public FixedLenParametersProvider(DatabaseInterface databaseInterface, long fetchSize, DbClusterInfo dbClusterInfo, int parallelismNum,
                                    String driverClassName, String filter, BitSailConfiguration inputSliceConfig,
                                    BitSailConfiguration commonConfig, ShardSplitMode shardSplitMode, String initSql) {
    checkArgument(fetchSize > 0, "Fetch size must be greater than 0.");
    this.databaseInterface = databaseInterface;
    this.fetchSize = fetchSize;
    this.dbClusterInfo = dbClusterInfo;
    this.driverClassName = driverClassName;
    this.filter = filter;
    this.inputSliceConfig = inputSliceConfig;
    this.commonConfig = commonConfig;
    this.shardSplitMode = shardSplitMode;
    this.initSql = initSql;

    if (parallelismNum <= 0) {
      this.parallelismNum = Math.min(dbClusterInfo.getDistinctHostsNumber() * 2, commonConfig.get(FlinkCommonOptions.FLINK_MAX_PARALLELISM));
      LOG.info("Parallelism not specified, calculated from distinct DB hosts number: {}", this.parallelismNum);
    } else {
      this.parallelismNum = parallelismNum;
      LOG.info("Use specified parallelism: {}", this.parallelismNum);
    }

    this.spInfo = new SplitParameterInfo<>(this.parallelismNum);
  }

  @Override
  public SplitParameterInfo getParameterValues()
      throws ExecutionException, InterruptedException {
    long st = System.currentTimeMillis();

    List<Callable> tasks = new ArrayList<>();
    HashMap<Integer, List<TableRangeInfo<K>>> splitsMap = new HashMap<>();

    Map<Integer, List<DbShardInfo>> shardsInfo = dbClusterInfo.getShardsInfo();

    ThreadPoolExecutor executor = getThreadPoolExecutor(shardsInfo);

    LOG.info("Start fetching JDBC range, parallelism: {}, shards size: {}", executor.getMaximumPoolSize(), shardsInfo.size());
    SqlType.SqlTypes splitType = SqlType.getSqlType(inputSliceConfig.get(JdbcReaderOptions.SPLIT_PK_JDBC_TYPE).toLowerCase(), driverClassName);
    boolean caseSensitive = inputSliceConfig.get(JdbcReaderOptions.CASE_SENSITIVE);
    for (Map.Entry<Integer, List<DbShardInfo>> entry : shardsInfo.entrySet()) {
      List<DbShardInfo> slaves = entry.getValue();

      SplitInfoCache cache = new SplitInfoNoOpCache();

      Callable task;

      switch (shardSplitMode) {
        case accurate:
          AccurateSplitOneShardCallable accurate = new AccurateSplitOneShardCallable<K>(databaseInterface, fetchSize,
              driverClassName, filter, slaves, dbClusterInfo, cache, inputSliceConfig, initSql);
          accurate.setCaseSensitive(caseSensitive);
          task = accurate;
          break;
        case quick:
          if (splitType == SqlType.SqlTypes.String) {
            throw new UnsupportedOperationException("string split id with quick split not supported yet...");
          }
          task = new QuickSplitOneShardCallable(databaseInterface, fetchSize,
              driverClassName, filter, slaves, dbClusterInfo, cache, inputSliceConfig, initSql);
          break;
        case parallelism:
          if (splitType == SqlType.SqlTypes.String) {
            throw new UnsupportedOperationException("string split id with quick split not supported yet...");
          }
          inputSliceConfig.set(JdbcReaderOptions.READER_PARALLELISM_NUM, this.parallelismNum);
          task = new ParallelismSplitterCallable(databaseInterface, fetchSize,
              driverClassName, filter, slaves, dbClusterInfo, cache, inputSliceConfig, initSql);
          break;
        default:
          throw BitSailException.asBitSailException(JDBCPluginErrorCode.INTERNAL_ERROR, "Invalid shard split mode: " + shardSplitMode);
      }

      tasks.add(task);
    }

    Collections.shuffle(tasks);

    final List<Future> futures = tasks.stream()
        .map((Function<Callable, Future>) executor::submit)
        .collect(Collectors.toList());

    for (Future f : futures) {
      if (null != f.get()) {
        Pair<Integer, List<TableRangeInfo<K>>> splitsInfo = (Pair<Integer, List<TableRangeInfo<K>>>) f.get();

        splitsMap.put(splitsInfo.getFirst(), splitsInfo.getSecond());
      }
    }

    executor.shutdown();

    /** Step 3: Assign the split range task to task groups which used */
    spInfo.assign(splitsMap, dbClusterInfo);

    LOG.info("Fetch split range info finished. Total split num: " + spInfo.getTotalSplitNum() + "\tTaken: " +
        TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - st) + " sec.");
    return spInfo;
  }

  private ThreadPoolExecutor getThreadPoolExecutor(Map<Integer, List<DbShardInfo>> shardsInfo) {

    int threadNum = Math.max(parallelismNum, 2);
    threadNum = Math.min(threadNum, Math.max(1, Runtime.getRuntime().availableProcessors() - 1));

    ThreadFactoryBuilder builder = new ThreadFactoryBuilder();
    builder.setNameFormat("JDBC fetch range info pool");
    builder.setDaemon(true);
    ThreadFactory factory = builder.build();
    return new ThreadPoolExecutor(threadNum, threadNum,
        0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(shardsInfo.size()), factory);
  }

  public enum ShardSplitMode {
    quick,
    accurate,
    parallelism
  }

}
