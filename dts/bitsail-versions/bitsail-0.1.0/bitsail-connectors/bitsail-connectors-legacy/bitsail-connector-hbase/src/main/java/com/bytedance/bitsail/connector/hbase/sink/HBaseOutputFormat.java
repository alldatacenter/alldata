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

package com.bytedance.bitsail.connector.hbase.sink;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.column.Column;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.type.filemapping.FileMappingTypeInfoConverter;
import com.bytedance.bitsail.connector.hbase.HBaseHelper;
import com.bytedance.bitsail.connector.hbase.auth.KerberosAuthenticator;
import com.bytedance.bitsail.connector.hbase.constant.NullMode;
import com.bytedance.bitsail.connector.hbase.error.HBasePluginErrorCode;
import com.bytedance.bitsail.connector.hbase.option.HBaseWriterOptions;
import com.bytedance.bitsail.connector.hbase.sink.function.FunctionParser;
import com.bytedance.bitsail.connector.hbase.sink.function.FunctionTree;
import com.bytedance.bitsail.flink.core.constants.TypeSystem;
import com.bytedance.bitsail.flink.core.legacy.connector.OutputFormatPlugin;
import com.bytedance.bitsail.flink.core.typeutils.NativeFlinkTypeInfoUtil;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.types.Row;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.BufferedMutator;
import org.apache.hadoop.hbase.client.BufferedMutatorParams;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PrivilegedAction;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.bytedance.bitsail.connector.hbase.constant.HBaseConstants.MAX_PARALLELISM_OUTPUT_HBASE;

public class HBaseOutputFormat extends OutputFormatPlugin<Row> implements ResultTypeQueryable<Row> {
  private static final Logger LOG = LoggerFactory.getLogger(HBaseOutputFormat.class);

  private static final String COLUMN_INDEX_KEY = "index";
  private static final String COLUMN_VALUE_KEY = "value";

  protected String tableName;

  protected String encoding;

  protected NullMode nullMode;

  protected boolean walFlag;

  protected long writeBufferSize;

  protected List<String> columnTypes;

  protected List<String> columnNames;

  protected String rowKeyExpress;

  protected Integer versionColumnIndex;

  protected Long versionColumnValue;

  protected List<String> rowKeyColumns = Lists.newArrayList();

  protected List<Integer> rowKeyColumnIndex = Lists.newArrayList();

  private Map<String, Object> hbaseConf;

  private RowTypeInfo rowTypeInfo;

  private boolean openUserKerberos = false;

  private boolean enableKerberos = false;

  private transient Connection connection;

  private transient BufferedMutator bufferedMutator;

  private transient FunctionTree functionTree;

  private transient Map<String, String[]> nameMaps;

  private transient Map<String, byte[][]> nameByteMaps;

  private transient ThreadLocal<SimpleDateFormat> timeSecondFormatThreadLocal;

  private transient ThreadLocal<SimpleDateFormat> timeMillisecondFormatThreadLocal;

  /**
   * Connects to the target database and initializes the prepared statement.
   *
   * @param taskNumber The number of the parallel instance.
   *                   I/O problem.
   */
  @Override
  public void open(int taskNumber, int numTasks) throws IOException {
    super.open(taskNumber, numTasks);
    if (enableKerberos) {
      sleepRandomTime();
      UserGroupInformation ugi;
      if (openUserKerberos) {
        ugi = KerberosAuthenticator.getUgi(hbaseConf);
      } else {
        ugi = UserGroupInformation.getCurrentUser();
      }
      ugi.doAs((PrivilegedAction<Object>) () -> {
        openConnection();
        return null;
      });
    } else {
      openConnection();
    }
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  private void sleepRandomTime() {
    try {
      Thread.sleep(5000L + (long) (10000 * Math.random()));
    } catch (Exception exception) {
      LOG.warn("", exception);
    }
  }

  public void openConnection() {
    LOG.info("Start configuring ...");
    nameMaps = Maps.newConcurrentMap();
    nameByteMaps = Maps.newConcurrentMap();
    timeSecondFormatThreadLocal = new ThreadLocal();
    timeMillisecondFormatThreadLocal = new ThreadLocal();

    try {
      org.apache.hadoop.conf.Configuration hbaseConfiguration = HBaseHelper.getConfig(hbaseConf);
      connection = ConnectionFactory.createConnection(hbaseConfiguration);

      bufferedMutator = connection.getBufferedMutator(
          new BufferedMutatorParams(TableName.valueOf(tableName))
              .pool(HTable.getDefaultExecutor(hbaseConfiguration))
              .writeBufferSize(writeBufferSize));
    } catch (Exception e) {
      HBaseHelper.closeBufferedMutator(bufferedMutator);
      HBaseHelper.closeConnection(connection);
      throw new IllegalArgumentException(e);
    }

    functionTree = FunctionParser.parse(rowKeyExpress);
    rowKeyColumns = FunctionParser.parseRowKeyCol(rowKeyExpress);
    for (String rowKeyColumn : rowKeyColumns) {
      int index = columnNames.indexOf(rowKeyColumn);
      if (index == -1) {
        throw new RuntimeException("Can not get row key column from columns:" + rowKeyColumn);
      }
      rowKeyColumnIndex.add(index);
    }

    LOG.info("End configuring.");
  }

  @Override
  public void writeRecordInternal(Row record) throws Exception {
    int i = 0;
    try {
      byte[] rowkey = getRowkey(record);
      Put put;
      if (versionColumnIndex == null) {
        put = new Put(rowkey);
        if (!walFlag) {
          put.setDurability(Durability.SKIP_WAL);
        }
      } else {
        long timestamp = getVersion(record);
        put = new Put(rowkey, timestamp);
      }

      for (; i < record.getArity(); ++i) {
        if (rowKeyColumnIndex.contains(i)) {
          continue;
        }

        String type = columnTypes.get(i);
        String name = columnNames.get(i);
        String[] cfAndQualifier = nameMaps.get(name);
        byte[][] cfAndQualifierBytes = nameByteMaps.get(name);
        if (cfAndQualifier == null || cfAndQualifierBytes == null) {
          cfAndQualifier = name.split(":");
          if (cfAndQualifier.length == 2
              && StringUtils.isNotBlank(cfAndQualifier[0])
              && StringUtils.isNotBlank(cfAndQualifier[1])) {
            nameMaps.put(name, cfAndQualifier);
            cfAndQualifierBytes = new byte[2][];
            cfAndQualifierBytes[0] = Bytes.toBytes(cfAndQualifier[0]);
            cfAndQualifierBytes[1] = Bytes.toBytes(cfAndQualifier[1]);
            nameByteMaps.put(name, cfAndQualifierBytes);
          } else {
            throw new IllegalArgumentException("Wrong column format. Please make sure the column name is: " +
                "[columnFamily:columnName]. Wrong column is: " + name);
          }
        }
        byte[] columnBytes = getColumnByte(type, (Column) record.getField(i));
        // skip null column
        if (null != columnBytes) {
          put.addColumn(
              cfAndQualifierBytes[0],
              cfAndQualifierBytes[1],
              columnBytes);
        }
      }

      bufferedMutator.mutate(put);
    } catch (Exception ex) {
      if (i < record.getArity()) {
        throw BitSailException.asBitSailException(CommonErrorCode.RUNTIME_ERROR,
            new RuntimeException(recordConvertDetailErrorMessage(i, record), ex));
      }
      throw BitSailException.asBitSailException(CommonErrorCode.RUNTIME_ERROR, ex);
    }
  }

  protected String recordConvertDetailErrorMessage(int pos, Row row) {
    return "HBaseOutputFormat writeRecord error: when converting field[" + columnNames.get(pos) + "] in Row(" + row + ")";
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  private byte[] getRowkey(Row record) throws Exception {
    Map<String, Object> nameValueMap = new HashMap<>((rowKeyColumnIndex.size() << 2) / 3);
    for (Integer keyColumnIndex : rowKeyColumnIndex) {
      nameValueMap.put(columnNames.get(keyColumnIndex), ((Column) record.getField(keyColumnIndex)).asString());
    }
    String rowKeyStr = functionTree.evaluate(nameValueMap);
    return rowKeyStr.getBytes(StandardCharsets.UTF_8);
  }

  public long getVersion(Row record) {
    Integer index = versionColumnIndex.intValue();
    long timestamp;
    if (index == null) {
      timestamp = Long.valueOf(versionColumnValue);
      if (timestamp < 0) {
        throw new IllegalArgumentException("Illegal timestamp to construct versionClumn: " + timestamp);
      }
    } else {
      if (index >= record.getArity() || index < 0) {
        throw new IllegalArgumentException("version column index out of range: " + index);
      }
      if (record.getField(index) == null) {
        throw new IllegalArgumentException("null verison column!");
      }
      Column col = (Column) record.getField(index);
      timestamp = col.asLong();
    }
    return timestamp;
  }

  public byte[] getColumnByte(String columnType, Column column) {
    byte[] bytes;
    if (column != null && column.getRawData() != null) {
      switch (columnType) {
        case "int":
          bytes = Bytes.toBytes((int) (long) column.asLong());
          break;
        case "long":
        case "bigint":
          bytes = Bytes.toBytes(column.asLong());
          break;
        case "date":
        case "timestamp":
          bytes = Bytes.toBytes(column.asLong());
          break;
        case "double":
          bytes = Bytes.toBytes(column.asDouble());
          break;
        case "float":
          bytes = Bytes.toBytes((float) (double) column.asDouble());
          break;
        case "decimal":
          bytes = Bytes.toBytes(column.asBigDecimal());
          break;
        case "short":
          bytes = Bytes.toBytes((short) (long) column.asLong());
          break;
        case "boolean":
          bytes = Bytes.toBytes(column.asBoolean());
          break;
        case "varchar":
        case "string":
          bytes = Bytes.toBytes(column.asString());
          break;
        case "binary":
          bytes = column.asBytes();
          break;
        default:
          throw new IllegalArgumentException("Unsupported column type: " + columnType);
      }
    } else {
      switch (nullMode) {
        case SKIP:
          bytes = null;
          break;
        case EMPTY:
          bytes = HConstants.EMPTY_BYTE_ARRAY;
          break;
        default:
          throw new IllegalArgumentException("Unsupported null mode: " + nullMode);
      }
    }
    return bytes;
  }

  @Override
  public TypeSystem getTypeSystem() {
    return TypeSystem.BitSail;
  }

  @Override
  public void close() throws IOException {
    super.close();
    if (null != timeSecondFormatThreadLocal) {
      timeSecondFormatThreadLocal.remove();
    }

    if (null != timeMillisecondFormatThreadLocal) {
      timeMillisecondFormatThreadLocal.remove();
    }

    HBaseHelper.closeBufferedMutator(bufferedMutator);
    HBaseHelper.closeConnection(connection);
  }

  @Override
  public int getMaxParallelism() {
    return MAX_PARALLELISM_OUTPUT_HBASE;
  }

  @Override
  public void initPlugin() throws Exception {
    tableName = outputSliceConfig.getNecessaryOption(HBaseWriterOptions.TABLE_NAME, HBasePluginErrorCode.REQUIRED_VALUE);
    encoding = outputSliceConfig.get(HBaseWriterOptions.ENCODING);
    nullMode = NullMode.valueOf(outputSliceConfig.get(HBaseWriterOptions.NULL_MODE).toUpperCase());
    walFlag = outputSliceConfig.get(HBaseWriterOptions.WAL_FLAG);
    writeBufferSize = outputSliceConfig.get(HBaseWriterOptions.WRITE_BUFFER_SIZE);
    rowKeyExpress = buildRowKeyExpress(outputSliceConfig.get(HBaseWriterOptions.ROW_KEY_COLUMN));
    Map<String, Object> versionColumn = outputSliceConfig.get(HBaseWriterOptions.VERSION_COLUMN);
    if (versionColumn != null) {
      if (versionColumn.get(COLUMN_INDEX_KEY) != null) {
        versionColumnIndex = Integer.valueOf(versionColumn.get(COLUMN_INDEX_KEY).toString());
      } else if (versionColumn.get(COLUMN_VALUE_KEY) != null) {
        versionColumnValue = Long.valueOf(versionColumn.get(COLUMN_VALUE_KEY).toString());
        if (versionColumnValue < 0) {
          throw BitSailException.asBitSailException(HBasePluginErrorCode.ILLEGAL_VALUE,
              "Illegal timestamp to construct versionClumn: " + versionColumnValue);
        }
      }
    }

    hbaseConf = outputSliceConfig.getNecessaryOption(HBaseWriterOptions.HBASE_CONF, HBasePluginErrorCode.REQUIRED_VALUE);

    boolean openPlatformKerberos = KerberosAuthenticator.enableKerberosConfig();
    openUserKerberos = KerberosAuthenticator.openKerberos(hbaseConf);
    enableKerberos = openPlatformKerberos || openUserKerberos;
    if (openPlatformKerberos && !openUserKerberos) {
      KerberosAuthenticator.addHBaseKerberosConf(hbaseConf);
    }

    List<ColumnInfo> columns = outputSliceConfig.getNecessaryOption(HBaseWriterOptions.COLUMNS, HBasePluginErrorCode.REQUIRED_VALUE);
    columnNames = columns.stream().map(ColumnInfo::getName).collect(Collectors.toList());
    columnTypes = columns.stream().map(ColumnInfo::getType).collect(Collectors.toList());
    rowTypeInfo = NativeFlinkTypeInfoUtil.getRowTypeInformation(columns, new FileMappingTypeInfoConverter(StringUtils.lowerCase(getType())));
  }

  @Override
  public String getType() {
    return "HBase";
  }

  @Override
  public void tryCleanupOnError() throws Exception {

  }

  @Override
  public RowTypeInfo getProducedType() {
    return rowTypeInfo;
  }

  /**
   * Compatible with old formats
   */
  private String buildRowKeyExpress(Object rowKeyInfo) {
    if (rowKeyInfo == null) {
      return null;
    }

    if (rowKeyInfo instanceof String) {
      return rowKeyInfo.toString();
    }

    if (!(rowKeyInfo instanceof List)) {
      return null;
    }

    StringBuilder expressBuilder = new StringBuilder();

    for (Map item : ((List<Map>) rowKeyInfo)) {
      Object indexObj = item.get(COLUMN_INDEX_KEY);
      if (indexObj != null) {
        int index = Integer.parseInt(String.valueOf(indexObj));
        if (index >= 0) {
          expressBuilder.append(String.format("$(%s)", columnNames.get(index)));
          continue;
        }
      }

      String value = (String) item.get(COLUMN_VALUE_KEY);
      if (StringUtils.isNotEmpty(value)) {
        expressBuilder.append(value);
      }
    }

    return expressBuilder.toString();
  }

}
