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
import com.bytedance.bitsail.connector.legacy.jdbc.split.DbShardWithConn;
import com.bytedance.bitsail.connector.legacy.jdbc.split.TableRangeInfo;
import com.bytedance.bitsail.connector.legacy.jdbc.split.cache.SplitInfoCache;
import com.bytedance.bitsail.connector.legacy.jdbc.utils.Db2Util;
import com.bytedance.bitsail.connector.legacy.jdbc.utils.MysqlUtil;
import com.bytedance.bitsail.connector.legacy.jdbc.utils.OracleUtil;
import com.bytedance.bitsail.connector.legacy.jdbc.utils.PostgresqlUtil;
import com.bytedance.bitsail.connector.legacy.jdbc.utils.SqlServerUtil;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Use limit to get accurate number from jdbc source.
 * This strategy may consumer more than parallelism strategy, but it will produce the same size file in end.
 */
@Slf4j
@SuppressWarnings("checkstyle:MagicNumber")
public class AccurateSplitOneShardCallable<K extends Comparable> extends SplitOneShardCallable<K> {

  private long getOneSplitMaxPrimaryKeyCount = 0;

  AccurateSplitOneShardCallable(DatabaseInterface databaseInterface, long fetchSize, String driverClassName, String filter,
                                List<DbShardInfo> slaves, DbClusterInfo dbClusterInfo, SplitInfoCache cache,
                                BitSailConfiguration inputSliceConfig, String initSql) {
    super(databaseInterface, fetchSize, driverClassName, filter, slaves, dbClusterInfo, cache, inputSliceConfig, initSql);
  }

  AccurateSplitOneShardCallable(DatabaseInterface databaseInterface, long fetchSize, String driverClassName, String filter,
                                List<DbShardInfo> slaves, DbClusterInfo dbClusterInfo, SplitInfoCache cache,
                                BitSailConfiguration inputSliceConfig) {
    this(databaseInterface, fetchSize, driverClassName, filter, slaves, dbClusterInfo, cache, inputSliceConfig, "");
  }

  @Override
  protected List<TableRangeInfo<K>> calculateRanges(String quoteTableWithSchema, K preMaxPriKey, K maxPriKey) throws IOException, InterruptedException {
    final ArrayList<TableRangeInfo<K>> singleTableRangeList = new ArrayList<>();

    K currentMaxPriKey = preMaxPriKey;
    DbShardWithConn oneShard;
    long currentSplitNum = 0L;
    long st = System.currentTimeMillis();
    final double timeInterval = 60.0;
    boolean statementRefresh = true;
    while (compareSplitKey(currentMaxPriKey, maxPriKey) < 0) {
      oneShard = pickOneShard();
      for (int i = 0; i < RETRY_NUM; i++) {
        try {
          String cacheIndex = quoteTableWithSchema + "_" + preMaxPriKey.toString();
          Pair<K, K> range = cache.get(cacheIndex);

          if (range == null) {
            currentMaxPriKey = getOneSplitMaxPrimaryKey(oneShard, quoteTableWithSchema, preMaxPriKey, statementRefresh);
            statementRefresh = false;
            if (currentMaxPriKey.equals(maxPriKey)) {
              // If we don't do this, we will lost the last record. Because the range for us is [start, end)
              currentMaxPriKey = addDeltaToKey(maxPriKey);
            }

            if (compareSplitKey(currentMaxPriKey, preMaxPriKey) <= 0) {
              log.info("Current max PK {} is not more than previous PK {}. Ignore this range and fetch next slave.",
                  currentMaxPriKey, preMaxPriKey);
              continue;
            }

            range = new Pair<>(preMaxPriKey, currentMaxPriKey);
            cache.set(cacheIndex, range);

            // Have a relax after some times query
            if (currentSplitNum % FETCH_RANGE_SLEEP_SIZE == 0) {
              log.debug("[" + Thread.currentThread().getName() + "] Current split num: " + currentSplitNum + " Have a relax, sleep for 1s...");
              Thread.sleep(FETCH_RANGE_SLEEP_INTERVAL);
              fetchRangeSleepTime++;
            }
          } else {
            currentMaxPriKey = range.getSecond();
            if (currentMaxPriKey.equals(maxPriKey)) {
              // If we don't do this, we will lost the last record. Because the range for us is [start, end)
              currentMaxPriKey = addDeltaToKey(maxPriKey);
            }
          }

          singleTableRangeList.add(new TableRangeInfo<>(quoteTableWithSchema, range));
          preMaxPriKey = currentMaxPriKey;
          currentSplitNum++;
          break;
        } catch (SQLException e) {
          log.error("Fetch primary key range failure, due to " + e.getMessage() + ", sql: " + getFetchSQLFormat()
                  + ", last primary key: " + preMaxPriKey + ", try num " + i + "\tInstance info: " + oneShard.getShardInfo(),
              e);
          if (i == RETRY_NUM - 1) {
            throw new IOException("Fetch primary key range failure! DB instance info: " + oneShard.getShardInfo(), e);
          }
          try {
            Thread.sleep(i * RETRY_BASE_TIME_DURATION);
            fetchRangeSleepTime++;
          } catch (InterruptedException e1) {
            log.error("Retry has been Interrupted...", e);
            throw new IOException("Fetch primary key range failure!", e);
          }

          oneShard.reconnect();
        }
      }
      long currentTime = System.currentTimeMillis();
      getOneSplitMaxPrimaryKeyCount++;
      double time = TimeUnit.MILLISECONDS.toSeconds(currentTime - st);
      if (time >= timeInterval) {
        st = currentTime;
        logCurrentProcess(quoteTableWithSchema, maxPriKey, currentMaxPriKey, oneShard);
      }
    }

    return singleTableRangeList;
  }

  private void logCurrentProcess(String quoteTableWithSchema, K maxPriKey, K currentMaxPriKey, DbShardWithConn oneShard) {
    switch (splitType) {
      case Int:
      case BigInt:
      case Short:
      case Long:
      case BigDecimal:
        double spliteProcess = Double.valueOf(currentMaxPriKey.toString()) / Double.valueOf(maxPriKey.toString());
        log.info("[Fetch range query. Shard: " + oneShard.getShardInfo() + " Table: " + quoteTableWithSchema + "] " +
                "Execute " + getFetchSQLFormat() + " , till now, sleep times: {}, sleep interval: {}ms. " + " Fetch query times " + getOneSplitMaxPrimaryKeyCount +
                ", current primary key is " + currentMaxPriKey + " and max primary key is " + maxPriKey + ", MySQL range split progress {}%",
            fetchRangeSleepTime, FETCH_RANGE_SLEEP_INTERVAL, String.format("%.1f", 100 * spliteProcess));
        break;
      case String:
        log.info("[Fetch range query. Shard: " + oneShard.getShardInfo() + " Table: " + quoteTableWithSchema + "] " +
                "Execute " + getFetchSQLFormat() + " , till now, sleep times: {}, sleep interval: {}ms. " + " Fetch query times " + getOneSplitMaxPrimaryKeyCount +
                ", current primary key is " + currentMaxPriKey + " and max primary key is " + maxPriKey,
            fetchRangeSleepTime, FETCH_RANGE_SLEEP_INTERVAL);
        break;
      default:
        throw new ClassCastException("unsupported class type: " + splitType);
    }
  }

  @Override
  protected String getFetchSQLFormat() {
    StringBuilder sql = new StringBuilder();
    sql.append("SELECT max(")
        .append(dbClusterInfo.getSplitPK())
        .append(") FROM (")
        .append("SELECT ")
        .append(dbClusterInfo.getSplitPK())
        .append(" FROM ")
        .append(" %s ")
        .append(" WHERE ");
    if (null != filter) {
      sql.append("(").append(filter).append(")").append(" AND ");
    }
    sql.append(dbClusterInfo.getSplitPK())
        .append("> ? ") // preMaxPriKey
        .append(" ORDER BY ")
        .append(dbClusterInfo.getSplitPK());
    final String driverClassName = super.getDriverClassName();
    switch (driverClassName) {
      case (MysqlUtil.DRIVER_NAME):
      case (PostgresqlUtil.DRIVER_NAME):
      case (Db2Util.DRIVER_NAME):
        sql.append(" LIMIT ")
            .append(fetchSize)
            .append(") t");
        break;
      case (OracleUtil.DRIVER_NAME):
        sql.append(") WHERE ROWNUM <= ")
            .append(fetchSize);
        break;
      case (SqlServerUtil.DRIVER_NAME):
        sql.append(" OFFSET 0 ROWS FETCH FIRST ")
            .append(fetchSize)
            .append(" ROWS ONLY) t");
        break;
      default:
        throw BitSailException.asBitSailException(JDBCPluginErrorCode.CONNECTION_ERROR, "Error driver name: " + driverClassName);
    }

    return sql.toString();
  }

  public K getOneSplitMaxPrimaryKey(DbShardWithConn shardWithConn, String quoteTableWithSchema, K lastPriKey, Boolean statementRefresh) throws SQLException {
    final PreparedStatement statement = shardWithConn.getStatement(quoteTableWithSchema, statementRefresh);
    if (super.getDriverClassName().equalsIgnoreCase(PostgresqlUtil.DRIVER_NAME)) {
      statement.setObject(1, lastPriKey);
    } else {
      statement.setString(1, lastPriKey.toString());
    }

    ResultSet rs = statement.executeQuery();

    rs.next();
    String sqlValue = rs.getString(1);
    rs.close();

    if (sqlValue == null) {
      return lastPriKey;
    } else {
      return generifyKeyFromSqlResult(sqlValue);
    }
  }
}
