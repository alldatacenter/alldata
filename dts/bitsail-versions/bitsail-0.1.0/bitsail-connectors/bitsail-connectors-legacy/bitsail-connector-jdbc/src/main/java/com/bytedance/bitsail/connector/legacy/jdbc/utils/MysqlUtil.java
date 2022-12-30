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

package com.bytedance.bitsail.connector.legacy.jdbc.utils;

import com.bytedance.bitsail.connector.legacy.jdbc.model.IndexInfo;

import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @desc:
 */
@Slf4j
public class MysqlUtil extends AbstractJdbcUtil {
  public static final String DRIVER_NAME = "org.mariadb.jdbc.Driver";
  public static final String DB_QUOTE = "`";
  public static final String VALUE_QUOTE = "\"";
  private static final Logger LOG = LoggerFactory.getLogger(MysqlUtil.class);
  private static final String COLUMN_NAME = "columnName";
  private static final String DISTINCT_KEYS = "cardinality";
  private static final String COLUMN_POSITION = "seqOrder";
  private static final String INDEX_NAME = "indexKeyName";
  private static final int RETRY_INTERVAL_MULTIPLIER = 10000;
  private static final int MAX_RETRY_INTERVAL_IN_MINUTES = 3;
  private static final int MAX_RETRY_ATTEMPT = 4;   // about 5 min in total
  private static MysqlUtil instance = new MysqlUtil();
  private final Retryer<Boolean> retryer;

  private MysqlUtil() {
    super(DISTINCT_KEYS, COLUMN_NAME, INDEX_NAME, COLUMN_POSITION, DRIVER_NAME);
    try {
      init();
    } catch (Exception e) {
      LOG.error("[Fatal] Init MysqlUtil failed! Due to: " + e.getMessage(), e);
    }
    retryer = RetryerBuilder.<Boolean>newBuilder()
        .retryIfResult(needRetry -> needRetry.equals(true))
        .retryIfException()
        .withWaitStrategy(WaitStrategies.exponentialWait(RETRY_INTERVAL_MULTIPLIER, MAX_RETRY_INTERVAL_IN_MINUTES, TimeUnit.MINUTES))
        .withStopStrategy(StopStrategies.stopAfterAttempt(MAX_RETRY_ATTEMPT))
        .withRetryListener(new RetryListener() {
          @Override
          public <V> void onRetry(Attempt<V> attempt) {
            LOG.info("number of attempts to getDbInfo: " + attempt.getAttemptNumber());
          }
        }).build();
  }

  public static MysqlUtil getInstance() {
    return instance;
  }

  private void init() {
  }

  @Override
  public Connection getConnection(String url, String user, String pwd)
      throws ClassNotFoundException, SQLException {
    Connection conn = null;

    Properties props = new Properties();
    props.put("remarksReporting", "true");
    props.put("user", user);
    props.put("password", pwd);
    Class.forName(DRIVER_NAME);
    conn = DriverManager.getConnection(url, props);

    return conn;
  }

  @Override
  Map<String, IndexInfo> getFirstOrderIndex(Multimap<String, IndexInfo> columnIndexesMap) {

    Map<String, IndexInfo> firstOrderColumnIndex = Maps.newHashMap();
    columnIndexesMap.asMap().forEach((column, indices) -> {
      final Optional<IndexInfo> optionalIndexInfo = indices.stream()
          .filter(indexInfo -> indexInfo.getSeqOrder() == 1)
          .max(Comparator.comparingLong(IndexInfo::getCardinality));

      optionalIndexInfo.ifPresent(index ->
          firstOrderColumnIndex.put(column, index)
      );
    });

    return firstOrderColumnIndex;
  }
}
