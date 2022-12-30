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

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.ddl.source.SourceEngineConnector;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.type.TypeInfoConverter;
import com.bytedance.bitsail.common.type.filemapping.JdbcTypeInfoConverter;
import com.bytedance.bitsail.common.util.JsonSerializer;
import com.bytedance.bitsail.connector.legacy.jdbc.model.ClusterInfo;
import com.bytedance.bitsail.connector.legacy.jdbc.model.ConnectionInfo;
import com.bytedance.bitsail.connector.legacy.jdbc.model.TableInfo;
import com.bytedance.bitsail.connector.legacy.jdbc.options.JdbcReaderOptions;
import com.bytedance.bitsail.connector.legacy.jdbc.options.OracleReaderOptions;
import com.bytedance.bitsail.connector.legacy.jdbc.utils.OracleUtil;

import com.google.common.base.Strings;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;

public class OracleSourceEngineConnector extends SourceEngineConnector {
  private final String url;
  private final String schemaName;
  private final String tableName;
  private final String userName;
  private final String password;
  private final String initSql;

  public OracleSourceEngineConnector(BitSailConfiguration commonConfiguration,
                                     BitSailConfiguration readerConfiguration) {
    super(commonConfiguration, readerConfiguration);
    List<ClusterInfo> clusterInfos = readerConfiguration.get(JdbcReaderOptions.CONNECTIONS);
    List<ConnectionInfo> slaves = clusterInfos.get(0).getSlaves();
    if (CollectionUtils.isEmpty(slaves)) {
      throw BitSailException.asBitSailException(CommonErrorCode.CONFIG_ERROR,
        "Get DB information error, the slaves info is null! Connection Json string is " + JsonSerializer.serialize(clusterInfos));
    }
    url = slaves.get(0).getUrl();
    schemaName = readerConfiguration.get(OracleReaderOptions.TABLE_SCHEMA);
    tableName = readerConfiguration.get(OracleReaderOptions.TABLE_NAME);
    userName = readerConfiguration.get(OracleReaderOptions.USER_NAME);
    password = readerConfiguration.get(OracleReaderOptions.PASSWORD);
    initSql = readerConfiguration.get(OracleReaderOptions.INIT_SQL);
  }

  @Override
  public List<ColumnInfo> getExternalColumnInfos() throws Exception {
    OracleUtil oracleUtil = new OracleUtil();
    TableInfo tableInfo;
    if (!Strings.isNullOrEmpty(tableName) && tableName.toUpperCase().split("\\.").length == 2) {
      // TODO The following line supports backward compatibility of signature. Format is 'Schema.table'. Can remove this line after Processor upgrade is complete.
      tableInfo = oracleUtil.getTableInfo(url, userName, password, tableName, initSql);
    } else {
      tableInfo = oracleUtil.getTableInfo(url, userName, password, null, schemaName, tableName, initSql, null);
    }
    return tableInfo.getColumnInfoList();
  }

  @Override
  public TypeInfoConverter createTypeInfoConverter() {
    return new JdbcTypeInfoConverter(getExternalEngineName());
  }

  @Override
  public String getExternalEngineName() {
    return "oracle";
  }
}
