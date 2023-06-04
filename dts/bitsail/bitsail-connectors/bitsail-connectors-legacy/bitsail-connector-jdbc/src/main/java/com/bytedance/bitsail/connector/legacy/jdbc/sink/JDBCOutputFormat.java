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

package com.bytedance.bitsail.connector.legacy.jdbc.sink;

import com.bytedance.bitsail.base.execution.ProcessResult;
import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.option.AdapterOptions;
import com.bytedance.bitsail.common.type.TypeInfoConverter;
import com.bytedance.bitsail.common.type.filemapping.JdbcTypeInfoConverter;
import com.bytedance.bitsail.common.util.Pair;
import com.bytedance.bitsail.common.util.TypeConvertUtil.StorageEngine;
import com.bytedance.bitsail.connector.legacy.jdbc.constants.Key;
import com.bytedance.bitsail.connector.legacy.jdbc.constants.WriteModeProxy;
import com.bytedance.bitsail.connector.legacy.jdbc.exception.JDBCPluginErrorCode;
import com.bytedance.bitsail.connector.legacy.jdbc.extension.DatabaseInterface;
import com.bytedance.bitsail.connector.legacy.jdbc.model.SqlType;
import com.bytedance.bitsail.connector.legacy.jdbc.options.JdbcWriterOptions;
import com.bytedance.bitsail.connector.legacy.jdbc.utils.JDBCConnHolder;
import com.bytedance.bitsail.connector.legacy.jdbc.utils.JdbcQueryHelper;
import com.bytedance.bitsail.connector.legacy.jdbc.utils.MysqlUtil;
import com.bytedance.bitsail.connector.legacy.jdbc.utils.upsert.JDBCUpsertUtil;
import com.bytedance.bitsail.connector.legacy.jdbc.utils.upsert.MysqlUpsertUtil;
import com.bytedance.bitsail.flink.core.constants.TypeSystem;
import com.bytedance.bitsail.flink.core.legacy.connector.OutputFormatPlugin;
import com.bytedance.bitsail.flink.core.typeutils.NativeFlinkTypeInfoUtil;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.types.Row;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.io.IOException;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.bytedance.bitsail.connector.legacy.jdbc.constants.WriteModeProxy.WriteMode;

/**
 * OutputFormat to write Rows into a JDBC database.
 */
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class JDBCOutputFormat extends OutputFormatPlugin<Row> implements ResultTypeQueryable<Row>, DatabaseInterface {
  private static final long serialVersionUID = 1L;
  private static final int DEFAULT_BATCH_SIZE = 10;

  private static final String COMPARE_EQUAL = "=";
  private static final String COMPARE_LTE = "<=";
  protected String username;
  protected String password;
  protected String dbURL;
  protected String table;
  protected WriteMode writeMode;
  protected String partitionName;
  protected String partitionType;
  protected String partitionPatternFormat;
  protected JDBCOutputExtraPartitions extraPartitions;
  protected int deleteThreshold;
  protected String[] shardKeys;
  protected Map<String, List<String>> upsertKeys;
  protected JDBCUpsertUtil jdbcUpsertUtil;
  /**
   * Writer batch size
   */
  private int batchInterval;
  /**
   * Per query before the job execution, most use for the init or delete history data.
   */
  private String preQuery;
  /**
   * Most use for verify insert number is match for the expected.
   */
  private String verifyQuery;
  private Set<String> curBatchPartitions = new HashSet<>();
  private RowTypeInfo rowTypeInfo;
  private JdbcQueryHelper jdbcQueryHelper;
  private List<ColumnInfo> columns;
  private WriteModeProxy writeModeProxy;
  private List<Row> batchBuffer;
  private int writeRetryTimes;
  private int retryIntervalSeconds;
  private String partitionValue;
  private Integer mysqlDataTtl;
  @Setter
  @Getter
  private JDBCConnHolder jdbcConnHolder;
  @Setter
  private PreparedStatement upload;
  private boolean flushBufferInClose = true;
  @Setter
  private boolean isSupportTransaction;
  @Getter
  private String query;
  private String clearQuery;
  private int deleteIntervalMs;

  @SuppressWarnings("checkstyle:MagicNumber")
  @Override
  public void initPlugin() throws IOException {
    // Necessary values
    username = outputSliceConfig.getNecessaryOption(JdbcWriterOptions.USER_NAME, JDBCPluginErrorCode.REQUIRED_VALUE);
    password = outputSliceConfig.getNecessaryOption(JdbcWriterOptions.PASSWORD, JDBCPluginErrorCode.REQUIRED_VALUE);
    table = outputSliceConfig.getNecessaryOption(JdbcWriterOptions.TABLE_NAME, JDBCPluginErrorCode.REQUIRED_VALUE);
    columns = outputSliceConfig.getNecessaryOption(JdbcWriterOptions.COLUMNS, JDBCPluginErrorCode.REQUIRED_VALUE);
    mysqlDataTtl = outputSliceConfig.get(JdbcWriterOptions.MYSQL_DATA_TTL);
    deleteThreshold = outputSliceConfig.get(JdbcWriterOptions.DELETE_THRESHOLD);
    deleteIntervalMs = outputSliceConfig.get(JdbcWriterOptions.DELETE_INTERVAL_MS);

    List<Map<String, Object>> connectionList = outputSliceConfig.getNecessaryOption(JdbcWriterOptions.CONNECTIONS,
        JDBCPluginErrorCode.REQUIRED_VALUE);
    List<JSONObject> connections = connectionList.stream().map(JSONObject::new).collect(Collectors.toList());
    dbURL = (connections.get(0)).getString(Key.DB_URL);

    String connectionParameters = outputSliceConfig.getUnNecessaryOption(JdbcWriterOptions.CONNECTION_PARAMETERS, null);
    dbURL = getDbURL(dbURL, connectionParameters);

    jdbcQueryHelper = new JdbcQueryHelper(dbURL, username, password, deleteIntervalMs, getDriverName());
    shardKeys = outputSliceConfig.getUnNecessaryOption(JdbcWriterOptions.SHARD_KEY, "").replaceAll(" ", "").split(",");
    // Unnecessary values
    writeMode = WriteMode.valueOf(outputSliceConfig.getUnNecessaryOption(JdbcWriterOptions.WRITE_MODE, WriteMode.insert.name()));

    if (writeMode == WriteMode.overwrite) {
      upsertKeys = initUniqueIndexColumnsMap();
      jdbcUpsertUtil = initUpsertUtils();
    }

    partitionName = outputSliceConfig.getUnNecessaryOption(JdbcWriterOptions.PARTITION_NAME, null);
    partitionValue = outputSliceConfig.getUnNecessaryOption(JdbcWriterOptions.PARTITION_VALUE, null);
    partitionPatternFormat = outputSliceConfig.getUnNecessaryOption(JdbcWriterOptions.PARTITION_PATTERN_FORMAT, null);
    log.info("Partition Column: " + partitionName + " Partition Value: " + partitionValue + " Partition Format: " + partitionPatternFormat);
    extraPartitions = new JDBCOutputExtraPartitions(getDriverName(), getFieldQuote(), getValueQuote());
    extraPartitions.initExtraPartitions(outputSliceConfig, writeMode);
    preQuery = outputSliceConfig.get(JdbcWriterOptions.PRE_QUERY);
    verifyQuery = outputSliceConfig.get(JdbcWriterOptions.VERIFY_QUERY);
    batchInterval = outputSliceConfig.getUnNecessaryOption(JdbcWriterOptions.WRITE_BATCH_INTERVAL, 100);
    writeRetryTimes = outputSliceConfig.getUnNecessaryOption(JdbcWriterOptions.WRITE_RETRY_TIMES, 3);
    isSupportTransaction = outputSliceConfig.getUnNecessaryOption(JdbcWriterOptions.IS_SUPPORT_TRANSACTION, true);
    // default to wait 10s per 100 batch records
    int defaultRetryIntervalSeconds = batchInterval / DEFAULT_BATCH_SIZE > 0 ? batchInterval / DEFAULT_BATCH_SIZE : DEFAULT_BATCH_SIZE;
    retryIntervalSeconds = outputSliceConfig.getUnNecessaryOption(JdbcWriterOptions.RETRY_INTERVAL_SECONDS, defaultRetryIntervalSeconds);
    if (retryIntervalSeconds <= 0) {
      log.warn("retry_interval_seconds is invalid, and is modified to default " + defaultRetryIntervalSeconds);
      retryIntervalSeconds = defaultRetryIntervalSeconds;
    }
    writeModeProxy = buildWriteModeProxy(writeMode);
    writeModeProxy.prepareOnClient();

    rowTypeInfo = NativeFlinkTypeInfoUtil.getRowTypeInformation(columns, getTypeConverter());
    log.info("Output Row Type Info: " + rowTypeInfo);

    // generated values
    query = genInsertQuery(table, columns, writeMode);
    log.info("Clear query generated: " + clearQuery);
    log.info("Insert query generated: " + query);
    log.info("Validate plugin configuration parameters finished.");
  }

  public TypeInfoConverter getTypeConverter() {
    return new JdbcTypeInfoConverter(getStorageEngine().name());
  }

  public String getDbURL(String dbURL, String connectionParameters) {
    if (StringUtils.isEmpty(connectionParameters)) {
      return dbURL;
    }

    String dbConnection = dbURL.split("\\?")[0];
    return dbConnection + "?" + connectionParameters;
  }

  @Override
  public String getDriverName() {
    return MysqlUtil.DRIVER_NAME;
  }

  public StorageEngine getStorageEngine() {
    return StorageEngine.mysql;
  }

  @Override
  public String getFieldQuote() {
    return MysqlUtil.DB_QUOTE;
  }

  @Override
  public String getValueQuote() {
    return MysqlUtil.VALUE_QUOTE;
  }

  String genInsertQuery(String table, List<ColumnInfo> columns, WriteMode writeMode) {
    List<String> columnNames = columns.stream()
        .map(ColumnInfo::getName)
        .collect(Collectors.toList());

    String insertQuery;

    switch (writeMode) {
      case insert:
        columnNames = addPartitionColumns(columnNames, partitionName, extraPartitions);
        insertQuery = genInsertTemplate(table, columnNames);
        break;
      case overwrite:
        insertQuery = genUpsertTemplate(table, columnNames);
        break;
      default:
        throw BitSailException.asBitSailException(JDBCPluginErrorCode.INTERNAL_ERROR, "Unsupported write mode: " + writeMode);

    }

    return insertQuery;
  }

  /**
   * Generate the insert template for the write action.
   */
  protected String genInsertTemplate(String table, List<String> columnNames) {
    return String.format("INSERT INTO %s (%s)\n VALUES (%s) ",
        table,
        columnNames.stream().map(this::getQuoteColumn).collect(Collectors.joining(",")),
        columnNames.stream().map(col -> "?").collect(Collectors.joining(","))
    );
  }

  /**
   * Generate the update template for the upsert action.
   */
  protected String genUpsertTemplate(String table, List<String> columnNames) {
    return jdbcUpsertUtil.genUpsertTemplate(table, columnNames, "");
  }

  protected Map<String, List<String>> initUniqueIndexColumnsMap() throws IOException {
    return null;
  }

  protected JDBCUpsertUtil initUpsertUtils() {
    return new MysqlUpsertUtil(this, shardKeys, upsertKeys);
  }

  protected List<String> addPartitionColumns(List<String> columnNames, String partitionName, JDBCOutputExtraPartitions extraPartitions) {
    columnNames.add(partitionName);
    columnNames.addAll(extraPartitions.getFieldNames());
    return columnNames;
  }

  protected String genClearQuery(String partitionValue, String compare, String extraPartitionsSql) {
    //int or string for "yyyyMMdd" format
    if (partitionPatternFormat.equals("yyyyMMdd")) {
      return "delete from " + getQuoteTable(table) + " where " + getQuoteColumn(partitionName) + compare +
          wrapPartitionValueWithQuota(partitionValue) + extraPartitionsSql + " limit " + deleteThreshold;
    }
    return "delete from " + getQuoteTable(table) + " where " + getQuoteColumn(partitionName) + compare +
        getQuoteValue(partitionValue) + extraPartitionsSql + " limit " + deleteThreshold;
  }

  private String genClearQuery(String partitionValue) {
    final String extraPartitionsSql = extraPartitions.genSqlString();
    return genClearQuery(partitionValue, COMPARE_EQUAL, extraPartitionsSql);
  }

  /**
   * support varchar partition type for yyyyMMdd format
   *
   * @param value partition value
   * @return if format is yyyyMMdd and partition type is string, then return value wrapped with quota
   */
  protected String wrapPartitionValueWithQuota(String value) {
    if (partitionPatternFormat.equals("yyyyMMdd") && partitionType != null) {
      SqlType.SqlTypes sqlType = SqlType.getSqlType(partitionType, getDriverName());
      switch (sqlType) {
        case String:
          return getQuoteValue(value);
        default:
          return value;
      }
    }
    return value;
  }

  public String getMySQLTtlQuery() {
    if (mysqlDataTtl < 1 || partitionValue == null || partitionPatternFormat == null) {
      log.info("Delete mysql ttl data query is null, mysql ttl: {}, partitionValue: {}, partitionPatternFormat: {}",
          mysqlDataTtl, partitionValue, partitionPatternFormat);
      return null;
    }

    String deletePartition;
    try {
      DateTime dateTime = DateTimeFormat.forPattern(partitionPatternFormat).parseDateTime(partitionValue);
      dateTime = dateTime.minusDays(mysqlDataTtl);
      deletePartition = dateTime.toString(partitionPatternFormat);
    } catch (Exception e) {
      throw BitSailException.asBitSailException(CommonErrorCode.RUNTIME_ERROR, String.format("partition value is not support, " +
          "value: [%s], format: [%s]", partitionValue, partitionPatternFormat));
    }

    final String extraPartitionsSql = extraPartitions.genSqlString();
    return genClearQuery(deletePartition, COMPARE_LTE, extraPartitionsSql);
  }

  private void deleteMySQLDataTtl() {
    String deleteQuery = getMySQLTtlQuery();

    if (deleteQuery == null) {
      return;
    }
    log.info("Delete mysql ttl data query: {}", deleteQuery);
    jdbcQueryHelper.executeLimitDeleteInNewConnection(deleteQuery);
  }

  /**
   * Connects to the target database and initializes the prepared statement.
   *
   * @param taskNumber The number of the parallel instance.
   *                   I/O problem.
   */
  @Override
  public void open(int taskNumber, int numTasks) throws IOException {
    super.open(taskNumber, numTasks);
    if (batchBuffer == null) {
      batchBuffer = new ArrayList<>(batchInterval);
    }
    writeModeProxy.prepareOnTM();
    try {
      jdbcConnHolder = new JDBCConnHolder(getDriverName(), dbURL, username, password, isSupportTransaction);
      jdbcConnHolder.establishConnection();
      upload = jdbcConnHolder.prepareStatement(query);
    } catch (SQLException sqe) {
      throw new IllegalArgumentException("open() failed.", sqe);
    } catch (ClassNotFoundException cnfe) {
      throw new IllegalArgumentException("JDBC driver class not found.", cnfe);
    }
  }

  @Override
  public void onSuccessComplete(ProcessResult result) throws Exception {
    super.onSuccessComplete(result);
    if (verifyQuery != null) {
      log.info("Executing verify query: " + verifyQuery);

      final String verifyResult = jdbcQueryHelper.executeQueryInNewConnection(verifyQuery, false);
      if (!verifyResult.equals("0")) {
        throw BitSailException.asBitSailException(CommonErrorCode.RUNTIME_ERROR, "Verify output result fail. Result: " + verifyResult);
      }
    }
  }

  @Override
  public void onFailureComplete() throws IOException {
    writeModeProxy.onFailureComplete();
  }

  @Override
  public String getType() {
    return "JDBC";
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  @Override
  public int getMaxParallelism() {
    return 3;
  }

  /**
   * Adds a record to the prepared statement.
   *
   * <p>When this method is called, the output format is guaranteed to be opened.
   *
   * @param row The records to add to the output.
   * @see PreparedStatement
   */
  @Override
  public void writeRecordInternal(Row row) throws Exception {
    batchBuffer.add(row);
    fillPrepareStatement(row);
    upload.addBatch();
    if (batchBuffer.size() >= batchInterval) {
      flush();
    }
  }

  public void fillPrepareStatement(Row row) throws SQLException, BitSailException {
    String columnName = "";
    int index = 0;
    try {
      for (; index < row.getArity(); index++) {
        columnName = columns.get(index).getName();
        final Object fieldValue = row.getField(index);

        // handle null
        if (fieldValue == null) {
          upload.setNull(index + 1, 0);
          continue;
        }
        // use type info
        final String columnType = columns.get(index).getType();
        SqlType.setSqlValue(upload, index + 1, columnType, fieldValue, getDriverName());
      }
      writeModeProxy.afterWriteRecord(index);
    } catch (BitSailException e) {
      throw BitSailException.asBitSailException(e.getErrorCode(), String.format("column[%s] %s", columnName, e.getErrorMessage()));
    }
  }

  /*
   * Fill PrepareStatement with the records in buffer
   */
  public void refillBatchBufferPrepareStatement(List<Row> batchBuffer) throws SQLException, BitSailException {
    for (int i = 0; i < batchBuffer.size(); i++) {
      fillPrepareStatement(batchBuffer.get(i));
      upload.addBatch();
    }
  }

  public void flush() throws IOException {
    Pair<Boolean, String> executeResult = retryExecute(writeRetryTimes, 0, (Integer index) -> {
      try {
        upload.executeBatch();
        jdbcConnHolder.commit();
        return new Pair<>(true, "");
      } catch (SQLException e) {
        String errMsg = String.format("Failed to insert batch record. Will retry to insert batch:, State = [%s], ErrorCode = [%s], Message = [%s]",
            e.getSQLState(), e.getErrorCode(), e.getMessage());
        log.warn(errMsg);
        try {
          handleSqlException(e);
          refillBatchBufferPrepareStatement(batchBuffer);
        } catch (Exception ex) {
          log.warn("Failed to handle SQLException. Error Message: " + ex.getMessage());
          throw new IOException(ex.getMessage());
        }
        return new Pair<>(false, errMsg);
      }
    });

    // If it failed to batch insert, then degrade to insert separately
    if (!executeResult.getFirst()) {
      reconstructDBResource();
      doSingleInsert();
    }
    batchBuffer.clear();
    curBatchPartitions.clear();
  }

  /**
   * Support retry operations
   *
   * @param retryTimes: the times of retry
   * @param index:      current index of record
   * @param function:   function to execute
   * @return Pair<isSuccess, errMsg>
   */
  private Pair<Boolean, String> retryExecute(int retryTimes, int index, PrepareStatementSqlFunction<Integer, Pair<Boolean, String>> function) throws IOException {
    Pair<Boolean, String> executeResult = new Pair<>(false, "");
    for (int i = 0; i < retryTimes; i++) {
      executeResult = function.apply(index);
      if (executeResult.getFirst()) {
        return executeResult;
      }
    }
    return executeResult;
  }

  /**
   * handle SQLException
   *
   * @param sqe : sqlException
   */
  @SuppressWarnings("checkstyle:MagicNumber")
  private void handleSqlException(SQLException sqe) throws SQLException, IOException {
    if (getDriverName().equals(MysqlUtil.DRIVER_NAME) && sqe.getErrorCode() == 1105 && sqe.getMessage().equals("distributed transaction not supported")) {
      log.warn("Transaction is not supported here, it will be closed");
      //Transaction may not be supported in split database
      jdbcConnHolder.setIsSupportTransaction(false);
      jdbcConnHolder.reconnect(upload);
    }
    jdbcConnHolder.rollBackTransaction();
    reconstructDBResource();
    jdbcConnHolder.sleep(retryIntervalSeconds * 1000);
  }

  /**
   * reconstruct upload
   */
  private void reconstructDBResource() throws IOException {
    try {
      if (!jdbcConnHolder.isValid()) {
        jdbcConnHolder.reconnect(upload);
      }
      if (!upload.isClosed()) {
        upload.close();
      }
      upload = jdbcConnHolder.prepareStatement(query);
    } catch (SQLException e) {
      throw new IOException(String.format("ERROR: fail to reconstruct db resource . State = [%s], ErrorCode = [%s], Message = [%s]",
          e.getSQLState(), e.getErrorCode(), e.getMessage()));
    }
  }

  private void doSingleInsert() throws IOException {
    log.info("Do single insert, batchBuffer size " + batchBuffer.size());
    for (int i = 0; i < batchBuffer.size(); i++) {
      Pair<Boolean, String> executeResult = retryExecute(writeRetryTimes, i, (Integer index) -> {
        try {
          fillPrepareStatement(batchBuffer.get(index));
          upload.execute();
          jdbcConnHolder.commit();
          return new Pair<>(true, "");
        } catch (SQLException e) {
          //Print and report error record
          String errMsg = String.format("State = [%s], ErrorCode = [%s], Message = [%s]",
              e.getSQLState(), e.getErrorCode(), e.getMessage());
          log.warn(String.format("Failed to insert record. Will retry to insert: %s, %s ", batchBuffer.get(index).toString(), errMsg));
          try {
            handleSqlException(e);
          } catch (Exception ex) {
            throw new IOException(ex.getMessage());
          }
          return new Pair<>(false, errMsg);
        }
      });
      if (!executeResult.getFirst()) {
        throw new IOException("Failed to insert record: " + batchBuffer.get(i).toString() + " ErrMsg: " + executeResult.getSecond());
      }
    }
    log.info("Finished single insert");
  }

  protected void addPartitionValue(int index, String partitionKey, String columnType) throws BitSailException {
    try {
      if (partitionPatternFormat.equals("yyyyMMdd")) {
        try {
          if (columnType != null) {
            SqlType.SqlTypes sqlType = SqlType.getSqlType(columnType, getDriverName());
            switch (sqlType) {
              case String:
                upload.setString(index, partitionKey);
                break;
              default:
                upload.setInt(index, Integer.valueOf(partitionKey));
            }
          } else {
            upload.setInt(index, Integer.valueOf(partitionKey));
          }
        } catch (NumberFormatException e) {
          throw BitSailException.asBitSailException(CommonErrorCode.RUNTIME_ERROR, String.format("partition value is not support, " +
              "value: [%s], format: [%s]", partitionKey, partitionPatternFormat));
        }
      } else {
        try {
          DateFormat dateFormat = new SimpleDateFormat(partitionPatternFormat);
          if (partitionPatternFormat.equals("yyyy-MM-dd")) {
            upload.setDate(index, new Date(dateFormat.parse(partitionKey).getTime()));
          } else {
            upload.setTimestamp(index, new Timestamp(dateFormat.parse(partitionKey).getTime()));
          }
        } catch (ParseException e) {
          throw BitSailException.asBitSailException(CommonErrorCode.RUNTIME_ERROR, String.format("partition value is not support, " +
              "value: [%s], format: [%s]", partitionKey, partitionPatternFormat));
        }
      }
    } catch (SQLException e) {
      throw BitSailException.asBitSailException(CommonErrorCode.RUNTIME_ERROR, "Preparation of JDBC statement failed.", e);
    }
  }

  /**
   * Executes prepared statement and closes all resources of this instance.
   */
  @Override
  public void close() throws IOException {
    if (flushBufferInClose) {
      flush();
      flushBufferInClose = false;
    }
    jdbcConnHolder.closeDBResource(upload);
    super.close();
  }

  @Override
  public void tryCleanupOnError() {
    if (clearQuery != null) {
      log.info("Error occurred while writing to JDBC. Executing clean up query: " + clearQuery);
      jdbcQueryHelper.executeLimitDeleteInNewConnection(clearQuery);
    }
    //Do not flush records buffer when exception occurred in task
    flushBufferInClose = false;
  }

  void preQuery() {
    if (preQuery != null) {
      log.info("Executing pre query: " + preQuery);
      jdbcQueryHelper.executeQueryInNewConnection(preQuery, true);
    }
  }

  @Override
  public RowTypeInfo getProducedType() {
    return rowTypeInfo;
  }

  @Override
  public TypeSystem getTypeSystem() {
    return TypeSystem.FLINK;
  }

  @Override
  public BitSailConfiguration getAdapterConf() {
    BitSailConfiguration adapterConf = BitSailConfiguration.newDefault();
    adapterConf.set(AdapterOptions.CONVERT_BOOLEAN_TO_TINYINT, true);
    return adapterConf;
  }

  protected WriteModeProxy buildWriteModeProxy(WriteMode writeMode) {
    switch (writeMode) {
      case insert:
        return new InsertProxy();
      case overwrite:
        return new OverwriteProxy();
      default:
        throw BitSailException.asBitSailException(CommonErrorCode.CONFIG_ERROR, "unsupported write mode: " + writeMode);
    }
  }

  public class InsertProxy implements WriteModeProxy {
    @Override
    public void prepareOnClient() throws IOException {
      //MySQL Data TTL
      deleteMySQLDataTtl();
      partitionValue = outputSliceConfig.getUnNecessaryOption(JdbcWriterOptions.PARTITION_VALUE, null);

      validatePartitionValue(partitionValue);

      preQuery();

      clearQuery = genClearQuery(partitionValue);
      log.info("Executing clear query: " + clearQuery);
      jdbcQueryHelper.executeLimitDeleteInNewConnection(clearQuery);
    }

    protected void validatePartitionValue(String partitionValue) {
      if (StringUtils.isEmpty(partitionValue)) {
        throw BitSailException.asBitSailException(JDBCPluginErrorCode.REQUIRED_VALUE, "Partition is required when using write mode insert.");
      }
    }

    @Override
    public void onFailureComplete() throws IOException {
      jdbcQueryHelper.executeLimitDeleteInNewConnection(clearQuery);
    }

    @Override
    public void afterWriteRecord(int index) {
      addPartitionValue(index + 1, partitionValue, null);
      for (int i = 0; i < extraPartitions.getSize(); i++) {
        SqlType.setSqlValue(upload, index + 2 + i, extraPartitions.getFieldType(i), extraPartitions.getFieldValue(i), getDriverName());
      }
    }
  }

  public class OverwriteProxy implements WriteModeProxy {
    @Override
    public void prepareOnClient() {
      preQuery();
    }
  }
}
