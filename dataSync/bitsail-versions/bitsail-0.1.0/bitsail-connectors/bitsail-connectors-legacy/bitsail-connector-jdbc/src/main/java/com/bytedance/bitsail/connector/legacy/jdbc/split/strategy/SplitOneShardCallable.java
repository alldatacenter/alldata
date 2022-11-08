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
import com.bytedance.bitsail.connector.legacy.jdbc.split.DbShardWithConn;
import com.bytedance.bitsail.connector.legacy.jdbc.split.TableRangeInfo;
import com.bytedance.bitsail.connector.legacy.jdbc.split.cache.SplitInfoCache;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * @desc:
 */
public abstract class SplitOneShardCallable<K extends Comparable> implements Callable<Pair<Integer, List<TableRangeInfo<K>>>> {
  static final int RETRY_NUM = 3;
  static final long RETRY_BASE_TIME_DURATION = 1000;
  static final int FETCH_RANGE_SLEEP_SIZE = 1000;
  // 1s
  static final long FETCH_RANGE_SLEEP_INTERVAL = 1000;
  private static final  Logger LOG = LoggerFactory.getLogger(SplitOneShardCallable.class);
  private static final  int MAX_UNICODE_CODE_POINT = 65535;
  private static final  BigInteger DEFAULT_BIGINT_IN_EMPTY_TABLE = BigInteger.valueOf(0);
  private static final  String DEFAULT_STRING_IN_EMPTY_TABLE = "0";
  final DbClusterInfo dbClusterInfo;
  final SplitInfoCache cache;
  private final List<DbShardWithConn> slavesWithConn = new ArrayList<>();
  @Getter
  private final String driverClassName;
  protected SqlType.SqlTypes splitType;
  long fetchRangeSleepTime = 0;
  String filter;
  long fetchSize;
  private DatabaseInterface databaseInterface;
  private boolean connInitSucc = false;
  private int currentIndex = 0;
  @Setter
  private boolean caseSensitive = false;

  public SplitOneShardCallable(DatabaseInterface databaseInterface, long fetchSize, String driverClassName,
                               String filter, List<DbShardInfo> slaves, DbClusterInfo dbClusterInfo,
                               SplitInfoCache cache, BitSailConfiguration inputSliceConfig, String initSql) {
    this.databaseInterface = databaseInterface;
    this.fetchSize = fetchSize;
    this.filter = filter;
    this.dbClusterInfo = dbClusterInfo;
    this.cache = cache;
    this.driverClassName = driverClassName;
    this.splitType = SqlType.getSqlType(inputSliceConfig.get(JdbcReaderOptions.SPLIT_PK_JDBC_TYPE).toLowerCase(), driverClassName);

    initConns(slaves, inputSliceConfig, initSql);
  }

  private void initConns(List<DbShardInfo> slaves, BitSailConfiguration inputSliceConfig, String initSql) {
    final String sql = getFetchSQLFormat();

    for (DbShardInfo shardInfo : slaves) {
      DbShardWithConn dbShardWithConn = new DbShardWithConn(shardInfo, dbClusterInfo, sql, inputSliceConfig,
          driverClassName, initSql);
      slavesWithConn.add(dbShardWithConn);
    }
    Collections.shuffle(slavesWithConn);

    connInitSucc = true;
  }

  @Override
  public Pair<Integer, List<TableRangeInfo<K>>> call() throws Exception {
    if (!connInitSucc) {
      return null;
    }
    final int shardNum = slavesWithConn.get(0).getShardInfo().getShardNum();

    long st = System.currentTimeMillis();

    /** For each instance, we should split all data by primary key with fetch size */
    DbShardWithConn oneShard = pickOneShard();
    List<TableRangeInfo<K>> multiTableRangeList = new ArrayList<>();
    for (int i = 0; i < dbClusterInfo.getShardTables(shardNum).size(); i++) {
      String table = dbClusterInfo.getShardTables(shardNum).get(i);

      String quoteTableWithSchema = convertToQuotedTableWithSchema(table);
      LOG.info("Processing table: {}.", quoteTableWithSchema);
      final Pair<K, K> minMaxRangeCache = cache.get(quoteTableWithSchema);
      List<TableRangeInfo<K>> singleTableRangeList = new ArrayList<>();
      boolean emptyOrSingleRecordTableSize = false;

      K minPriKey = null;
      K maxPriKey = null;

      /** Step 1: Get max primary key (Primary key's value type must by numeric) */
      if (minMaxRangeCache != null) {
        minPriKey = minMaxRangeCache.getFirst();
        maxPriKey = minMaxRangeCache.getSecond();
        LOG.info("Found min pk {} and max pk {} for shard {} in cache.", minPriKey, maxPriKey, shardNum);
      } else {
        for (int j = 0; j < RETRY_NUM; j++) {
          try {
            maxPriKey = getMaxOrMinPrimaryKey(oneShard, quoteTableWithSchema, dbClusterInfo.getSplitPK(), filter, true);
            /** Step 1.1: If there is a filter specified, we must get min key also */
            minPriKey = getMaxOrMinPrimaryKey(oneShard, quoteTableWithSchema, dbClusterInfo.getSplitPK(), filter, false);
            LOG.info("min pk {} and max pk {} for shard {} has been calculated.", minPriKey, maxPriKey, shardNum);

            if (compareSplitKey(minPriKey, maxPriKey) == 0) {
              LOG.info("Single record table for shard {}", shardNum);
              maxPriKey = addDeltaToKey(maxPriKey);
              Pair<K, K> keyRangePair = new Pair<>(minPriKey, maxPriKey);
              TableRangeInfo<K> tableRange = new TableRangeInfo<>(quoteTableWithSchema, keyRangePair);
              singleTableRangeList = ImmutableList.of(tableRange);
              multiTableRangeList.addAll(singleTableRangeList);
              emptyOrSingleRecordTableSize = true;
              break;
            }

            cache.set(quoteTableWithSchema, new Pair<>(minPriKey, maxPriKey));
            break;
          } catch (EmptySqlResultException e) {
            LOG.info("Empty table for shard {}", shardNum);
            emptyOrSingleRecordTableSize = true;
            //Empty table.
            singleTableRangeList = generateRangesIfEmpty(quoteTableWithSchema);
            multiTableRangeList.addAll(singleTableRangeList);
            break;
          } catch (SQLException e) {
            LOG.error("Get table " + quoteTableWithSchema + " max primary key encountered error. DB instance info: " + oneShard.getShardInfo()
                + " Try times: " + j + 1, e);
            try {
              Thread.sleep(j * RETRY_BASE_TIME_DURATION);
            } catch (InterruptedException e1) {
              LOG.error("Retry has been interrupted.", e);
            }
            oneShard.reconnect();
          }
        }
      }

      if (emptyOrSingleRecordTableSize) {
        continue;
      }

      if (null == maxPriKey) {
        throw new IOException("Can't get max primary key of db instance " + oneShard.getShardInfo());
      }

      /** Step 2: Get all range pair for a db instance which split by the fetch size */
      singleTableRangeList = new ArrayList<>(calculateRanges(quoteTableWithSchema, minPriKey, maxPriKey));
      multiTableRangeList.addAll(singleTableRangeList);
    }

    LOG.info("Fetch jdbc split finished for shard " + shardNum + ". Taken: " + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - st) + " sec. "
        + "Total sleep times: {}, sleep interval: {}ms.", fetchRangeSleepTime, FETCH_RANGE_SLEEP_INTERVAL);

    cache.close();
    slavesWithConn.forEach(DbShardWithConn::close);

    return new Pair<>(shardNum, multiTableRangeList);
  }

  protected String convertToQuotedTableWithSchema(String tableName) {
    String tableWithSchema = "";
    if (Strings.isNullOrEmpty(this.dbClusterInfo.getSchema())) {
      tableWithSchema = tableName;
    } else {
      tableWithSchema = this.dbClusterInfo.getSchema() + "." + tableName;
    }
    return getQuoteTable(tableWithSchema);
  }

  private String getQuoteTable(String tableWithSchema) {
    return this.databaseInterface.getQuoteTable(tableWithSchema);
  }

  DbShardWithConn pickOneShard() {
    if (currentIndex == slavesWithConn.size() - 1) {
      currentIndex = 0;
    } else {
      currentIndex++;
    }
    return slavesWithConn.get(currentIndex);
  }

  List<TableRangeInfo<K>> generateRangesIfEmpty(String quoteTableWithSchema) {
    K defaultKeyInEmptyTable;
    switch (splitType) {
      case Int:
      case BigInt:
      case Short:
      case Long:
      case BigDecimal:
        defaultKeyInEmptyTable = (K) DEFAULT_BIGINT_IN_EMPTY_TABLE;
        break;
      case String:
        defaultKeyInEmptyTable = (K) DEFAULT_STRING_IN_EMPTY_TABLE;
        break;
      default:
        throw new ClassCastException("unsupported class type: " + splitType);
    }

    Pair<K, K> keyRange = new Pair<>(defaultKeyInEmptyTable, defaultKeyInEmptyTable);
    TableRangeInfo<K> tableRange = new TableRangeInfo<>(quoteTableWithSchema, keyRange);
    return ImmutableList.of(tableRange);
  }

  public String getMaxPrimaryKeySQL(String quoteTableWithSchema, String primaryKey, String filter, boolean isMax) {
    StringBuilder sql = new StringBuilder();
    sql.append("SELECT ");
    if (isMax) {
      sql.append("max(");
    } else {
      sql.append("min(");
    }
    sql.append(primaryKey)
        .append(") FROM ")
        .append(quoteTableWithSchema);
    if (null != filter) {
      sql.append(" WHERE ").append(filter);
    }

    return sql.toString();
  }

  public K getMaxOrMinPrimaryKey(DbShardWithConn shardWithConn, String quoteTableWithSchema, String primaryKey, String filter, boolean isMax) throws SQLException {

    String sql = getMaxPrimaryKeySQL(quoteTableWithSchema, primaryKey, filter, isMax);

    Statement statement = shardWithConn.getConnection().createStatement();
    ResultSet rs = statement.executeQuery(sql);

    K boundPrimaryKey = null;
    while (rs.next()) {
      // Can only execute once
      String sqlValue = rs.getString(1);
      if (sqlValue == null) {
        //for empty table.
        throw new EmptySqlResultException("empty table");
      } else {
        boundPrimaryKey = generifyKeyFromSqlResult(sqlValue);
      }
    }
    return boundPrimaryKey;
  }

  protected abstract List<TableRangeInfo<K>> calculateRanges(String quoteTableWithSchema, K preMaxPriKey, K maxPriKey) throws IOException, InterruptedException;

  protected abstract String getFetchSQLFormat();

  /**
   *
   */
  protected K addDeltaToKey(K key) {
    switch (splitType) {
      case Int:
      case BigInt:
      case Short:
      case Long:
      case BigDecimal:
        return (K) BigInteger.valueOf(1L).add((BigInteger) key);
      case String:
        String maxKeyString = (String) key;
        char[] maxKeyChars = maxKeyString.toCharArray();
        for (int i = 0; i < MAX_UNICODE_CODE_POINT; i++) {
          maxKeyChars[maxKeyString.length() - 1] = (char) (maxKeyString.codePointAt(maxKeyString.length() - 1) + i);
          if (compareSplitKey((K) new String(maxKeyChars), (K) maxKeyString) > 0) {
            break;
          }
        }
        return (K) new String(maxKeyChars);
      default:
        throw new ClassCastException("unsupported class type: " + splitType);
    }
  }

  /**
   *
   */
  protected K generifyKeyFromSqlResult(String sqlResult) {
    switch (splitType) {
      case Int:
      case BigInt:
      case Short:
      case Long:
      case BigDecimal:
        return (K) new BigDecimal(sqlResult).toBigInteger();
      case String:
        return (K) sqlResult;
      default:
        throw new ClassCastException("unsupported class type: " + splitType);
    }
  }

  /**
   * @param firstKey
   * @param secondKey
   * @return a negative integer, zero, or a positive integer as this object is less than, equal to,
   * or greater than the specified object.
   */
  protected int compareSplitKey(K firstKey, K secondKey) {
    switch (splitType) {
      case Int:
      case BigInt:
      case Short:
      case Long:
      case BigDecimal:
        return firstKey.compareTo(secondKey);
      case String:
        if (driverClassName.toUpperCase().contains("MSSQL") || driverClassName.toUpperCase().contains("POSTGRE")) {
          String errorMsg = "split key can't support " + splitType + " type for driver " + driverClassName;
          throw BitSailException.asBitSailException(JDBCPluginErrorCode.ILLEGAL_VALUE, errorMsg);
        }
        if (driverClassName.toUpperCase().contains("ORACLE") || caseSensitive) {
          return firstKey.compareTo(secondKey);
        } else {
          String firstString = ((String) firstKey).toUpperCase();
          String secondString = ((String) secondKey).toUpperCase();
          return firstString.compareTo(secondString);
        }
      default:
        throw new ClassCastException("unsupported class type: " + splitType);
    }
  }

  static class EmptySqlResultException extends RuntimeException {
    EmptySqlResultException(String s) {
      super(s);
    }
  }
}
