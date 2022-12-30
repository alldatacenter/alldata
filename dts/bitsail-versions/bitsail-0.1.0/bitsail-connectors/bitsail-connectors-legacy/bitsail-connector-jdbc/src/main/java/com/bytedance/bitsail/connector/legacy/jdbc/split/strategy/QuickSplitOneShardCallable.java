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

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.util.Pair;
import com.bytedance.bitsail.connector.legacy.jdbc.extension.DatabaseInterface;
import com.bytedance.bitsail.connector.legacy.jdbc.model.DbClusterInfo;
import com.bytedance.bitsail.connector.legacy.jdbc.model.DbShardInfo;
import com.bytedance.bitsail.connector.legacy.jdbc.split.TableRangeInfo;
import com.bytedance.bitsail.connector.legacy.jdbc.split.cache.SplitInfoCache;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Deprecated
public class QuickSplitOneShardCallable extends SplitOneShardCallable<BigInteger> {

  QuickSplitOneShardCallable(DatabaseInterface databaseInterface, long fetchSize, String driverClassName, String filter,
                             List<DbShardInfo> slaves, DbClusterInfo dbClusterInfo, SplitInfoCache cache,
                             BitSailConfiguration inputSliceConfig, String initSql) {
    super(databaseInterface, fetchSize, driverClassName, filter, slaves, dbClusterInfo, cache, inputSliceConfig, initSql);
  }

  QuickSplitOneShardCallable(DatabaseInterface databaseInterface, long fetchSize, String driverClassName, String filter,
                             List<DbShardInfo> slaves, DbClusterInfo dbClusterInfo, SplitInfoCache cache,
                             BitSailConfiguration inputSliceConfig) {
    this(databaseInterface, fetchSize, driverClassName, filter, slaves, dbClusterInfo, cache, inputSliceConfig, "");
  }

  @Override
  protected String getFetchSQLFormat() {
    return null;
  }

  @Override
  protected List<TableRangeInfo<BigInteger>> calculateRanges(String quoteTableWithSchema, BigInteger preMaxPriKey, BigInteger maxPriKey) {
    final ArrayList<TableRangeInfo<BigInteger>> result = new ArrayList<>();

    for (BigInteger start = preMaxPriKey; start.compareTo(maxPriKey) <= 0; start = start.add(BigInteger.valueOf(fetchSize))) {
      BigInteger end = start.add(BigInteger.valueOf(fetchSize));
      if (end.compareTo(maxPriKey) > 0) {
        end = maxPriKey.add(BigInteger.valueOf(1L));
      }
      result.add(new TableRangeInfo<>(quoteTableWithSchema, new Pair<>(start, end)));
    }

    return result;
  }
}
