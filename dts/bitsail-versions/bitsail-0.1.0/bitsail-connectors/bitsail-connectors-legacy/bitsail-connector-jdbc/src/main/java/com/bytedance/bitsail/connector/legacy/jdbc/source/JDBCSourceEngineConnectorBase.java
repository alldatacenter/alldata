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

package com.bytedance.bitsail.connector.legacy.jdbc.source;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.ddl.source.SourceEngineConnector;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.exception.FrameworkErrorCode;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.type.TypeInfoConverter;
import com.bytedance.bitsail.common.type.filemapping.JdbcTypeInfoConverter;
import com.bytedance.bitsail.common.util.TypeConvertUtil.StorageEngine;
import com.bytedance.bitsail.connector.legacy.jdbc.model.ClusterInfo;
import com.bytedance.bitsail.connector.legacy.jdbc.model.TableInfo;
import com.bytedance.bitsail.connector.legacy.jdbc.options.JdbcReaderOptions;
import com.bytedance.bitsail.connector.legacy.jdbc.utils.AbstractJdbcUtil;
import com.bytedance.bitsail.connector.legacy.jdbc.utils.MysqlUtil;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class JDBCSourceEngineConnectorBase extends SourceEngineConnector {

  protected String database;
  protected String table;
  protected String schema;
  protected String username;
  protected String password;
  protected String url;
  protected String customizedSQL;
  protected boolean useCustomizedSQL;

  public JDBCSourceEngineConnectorBase(BitSailConfiguration commonConfiguration, BitSailConfiguration readerConfiguration) {
    super(commonConfiguration, readerConfiguration);
    username = readerConfiguration.getNecessaryOption(JdbcReaderOptions.USER_NAME,
        CommonErrorCode.CONFIG_ERROR);
    password = readerConfiguration.getNecessaryOption(JdbcReaderOptions.PASSWORD,
        FrameworkErrorCode.REQUIRED_VALUE);
    database = readerConfiguration.get(JdbcReaderOptions.DB_NAME);
    schema = readerConfiguration.get(JdbcReaderOptions.TABLE_SCHEMA);
    customizedSQL = readerConfiguration.get(JdbcReaderOptions.CUSTOMIZED_SQL);
    useCustomizedSQL = StringUtils.isNotEmpty(customizedSQL);

    table = StringUtils.isNotBlank(customizedSQL) ? null
        : readerConfiguration.getUnNecessaryOption(JdbcReaderOptions.TABLE_NAME, "");

    List<ClusterInfo> connections = readerConfiguration
        .getNecessaryOption(JdbcReaderOptions.CONNECTIONS,
            FrameworkErrorCode.REQUIRED_VALUE);

    ClusterInfo clusterInfo = connections.get(0);
    table = getSingleTable(table, customizedSQL, clusterInfo);
    url = clusterInfo.getSlaves()
        .get(0).getUrl();
  }

  private static String getSingleTable(String orginTable,
                                       String customizedSQL,
                                       ClusterInfo clusterInfo) {
    if (StringUtils.isNotEmpty(customizedSQL)) {
      return null;
    }
    if (StringUtils.isNotEmpty(clusterInfo.getTableNames())) {
      return StringUtils.split(clusterInfo.getTableNames(), ",")[0];
    }
    if (StringUtils.isEmpty(orginTable)) {
      throw new IllegalArgumentException("Can not found any table info in connections");
    }
    return StringUtils.split(orginTable, ",")[0];
  }

  @Override
  public String getExternalEngineName() {
    return StorageEngine.mysql.name();
  }

  public AbstractJdbcUtil getJdbcUtil() {
    return MysqlUtil.getInstance();
  }

  @Override
  public TypeInfoConverter createTypeInfoConverter() {
    return new JdbcTypeInfoConverter(getExternalEngineName());
  }

  @Override
  public List<ColumnInfo> getExternalColumnInfos() throws Exception {
    // Generate from conf params
    AbstractJdbcUtil jdbcUtil = getJdbcUtil();
    TableInfo tableInfo = null;
    if (useCustomizedSQL) {
      tableInfo = jdbcUtil.getCustomizedSQLTableInfo(url, username, password, database, customizedSQL);
    } else {
      tableInfo = jdbcUtil.getTableInfo(url, username, password, database, schema, table, null, null);
    }

    return tableInfo.getColumnInfoList();
  }
}
