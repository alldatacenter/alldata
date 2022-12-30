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
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.util.TypeConvertUtil.StorageEngine;
import com.bytedance.bitsail.connector.legacy.jdbc.converter.JdbcValueConverter;
import com.bytedance.bitsail.connector.legacy.jdbc.converter.OracleValueConverter;
import com.bytedance.bitsail.connector.legacy.jdbc.options.OracleReaderOptions;
import com.bytedance.bitsail.connector.legacy.jdbc.utils.OracleUtil;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
public class OracleInputFormat extends JDBCInputFormat {
  private static final long serialVersionUID = 1L;
  private String intervalHandlingMode;

  public OracleInputFormat() {
  }

  @Override
  public void initPlugin() throws ExecutionException, InterruptedException {
    super.initPlugin();
    this.intervalHandlingMode = inputSliceConfig.getUnNecessaryOption(
            OracleReaderOptions.INTERVAL_HANDLE_MODE, OracleReaderOptions.INTERVAL_HANDLE_MODE.defaultValue());
  }

  @Override
  public String getType() {
    return "Oracle";
  }

  @Override
  String genSqlTemplate(String splitPK, List<ColumnInfo> columns, String filter) {
    StringBuilder sql = new StringBuilder("SELECT ");
    final String tableAlias = "t";
    for (ColumnInfo column : columns) {
      String columnName = column.getName();
      String columnType = column.getType();
      // xmltype is handled as clob
      // SQL example: 'select t."DOC".getClobVal() as "DOC", "TEST" from po_xml_tab t'
      // Reference: Oracle xmltype document https://docs.oracle.com/cd/B14117_01/appdev.101/b10790/xdb11jav.htm
      if ("xmltype".equalsIgnoreCase(columnType)) {
        sql.append(tableAlias).append(".").append(getQuoteColumn(columnName.toUpperCase())).append(".getClobVal() as ")
          .append(getQuoteColumn(columnName.toUpperCase())).append(",");
      } else {
        sql.append(getQuoteColumn(columnName.toUpperCase())).append(",");
      }
    }
    sql.deleteCharAt(sql.length() - 1);
    sql.append(" FROM ").append("%s").append(" ").append(tableAlias).append(" WHERE ");
    if (null != filter) {
      sql.append("(").append(filter).append(")");
      sql.append(" AND ");
    }
    sql.append("(").append(splitPK).append(" >= ? AND ").append(splitPK).append(" < ?)");
    return sql.toString();
  }

  @Override
  public String getDriverName() {
    return OracleUtil.DRIVER_NAME;
  }

  @Override
  public String getFieldQuote() {
    return OracleUtil.DB_QUOTE;
  }

  @Override
  public String getValueQuote() {
    return OracleUtil.VALUE_QUOTE;
  }

  @Override
  public StorageEngine getStorageEngine() {
    return StorageEngine.oracle;
  }

  @Override
  public SourceEngineConnector initSourceSchemaManager(BitSailConfiguration commonConf, BitSailConfiguration readerConf) throws Exception {
    return new OracleSourceEngineConnector(commonConf, readerConf);
  }

  @Override
  protected JdbcValueConverter createJdbcValueConverter() {
    return new OracleValueConverter(OracleValueConverter.IntervalHandlingMode.parse(this.intervalHandlingMode));
  }
}
