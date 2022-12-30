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

package com.bytedance.bitsail.connector.legacy.hive.common;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.ddl.source.SourceEngineConnector;
import com.bytedance.bitsail.common.exception.FrameworkErrorCode;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.type.TypeInfoConverter;
import com.bytedance.bitsail.common.type.filemapping.HiveTypeInfoConverter;
import com.bytedance.bitsail.common.util.TypeConvertUtil.StorageEngine;
import com.bytedance.bitsail.connector.legacy.hive.option.HiveReaderOptions;

import com.bytedance.bitsail.shaded.hive.client.HiveMetaClientUtil;

import org.apache.hadoop.hive.conf.HiveConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class HiveSourceEngineConnector extends SourceEngineConnector {
  private static final Logger LOG = LoggerFactory.getLogger(HiveSourceEngineConnector.class);

  private final String database;
  private final String table;
  private final HiveConf hiveConf;

  public HiveSourceEngineConnector(BitSailConfiguration commonConfiguration,
                                   BitSailConfiguration readerConfiguration,
                                   HiveConf hiveConf) {
    super(commonConfiguration, readerConfiguration);
    this.database = readerConfiguration.getNecessaryOption(HiveReaderOptions.DB_NAME, FrameworkErrorCode.REQUIRED_VALUE);
    this.table = readerConfiguration.getNecessaryOption(HiveReaderOptions.TABLE_NAME, FrameworkErrorCode.REQUIRED_VALUE);
    this.hiveConf = hiveConf;

    LOG.info("Hive metastore engine source connector database: {}, table: {}.", database, table);
  }

  @Override
  public List<ColumnInfo> getExternalColumnInfos() throws Exception {
    return HiveMetaClientUtil.getColumnInfo(hiveConf, database, table);
  }

  @Override
  public TypeInfoConverter createTypeInfoConverter() {
    return new HiveTypeInfoConverter();
  }

  @Override
  public String getExternalEngineName() {
    return StorageEngine.hive.name();
  }
}
