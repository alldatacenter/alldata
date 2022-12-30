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
import com.bytedance.bitsail.common.type.filemapping.FileMappingTypeInfoConverter;
import com.bytedance.bitsail.common.util.TypeConvertUtil.StorageEngine;
import com.bytedance.bitsail.connector.legacy.jdbc.converter.JdbcValueConverter;
import com.bytedance.bitsail.connector.legacy.jdbc.exception.DBUtilErrorCode;
import com.bytedance.bitsail.connector.legacy.jdbc.exception.JDBCPluginErrorCode;
import com.bytedance.bitsail.connector.legacy.jdbc.extension.DatabaseInterface;
import com.bytedance.bitsail.connector.legacy.jdbc.model.ClusterInfo;
import com.bytedance.bitsail.connector.legacy.jdbc.model.DbClusterInfo;
import com.bytedance.bitsail.connector.legacy.jdbc.model.DbShardInfo;
import com.bytedance.bitsail.connector.legacy.jdbc.model.SqlType;
import com.bytedance.bitsail.connector.legacy.jdbc.options.JdbcReaderOptions;
import com.bytedance.bitsail.connector.legacy.jdbc.split.DbShardWithConn;
import com.bytedance.bitsail.connector.legacy.jdbc.split.SplitParameterInfo;
import com.bytedance.bitsail.connector.legacy.jdbc.split.SplitRangeInfo;
import com.bytedance.bitsail.connector.legacy.jdbc.split.strategy.FixedLenParametersProvider;
import com.bytedance.bitsail.connector.legacy.jdbc.split.strategy.NoSplitParametersProvider;
import com.bytedance.bitsail.connector.legacy.jdbc.split.strategy.ParameterValuesProvider;
import com.bytedance.bitsail.connector.legacy.jdbc.utils.MysqlUtil;
import com.bytedance.bitsail.flink.core.constants.TypeSystem;
import com.bytedance.bitsail.flink.core.legacy.connector.InputFormatPlugin;
import com.bytedance.bitsail.flink.core.typeutils.NativeFlinkTypeInfoUtil;

import com.google.common.base.Strings;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.io.DefaultInputSplitAssigner;
import org.apache.flink.api.common.io.InputFormat;
import org.apache.flink.api.common.io.statistics.BaseStatistics;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.io.GenericInputSplit;
import org.apache.flink.core.io.InputSplit;
import org.apache.flink.core.io.InputSplitAssigner;
import org.apache.flink.types.Row;
import org.apache.flink.util.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * Desc:
 */
public class JDBCInputFormat extends InputFormatPlugin<Row, InputSplit> implements ResultTypeQueryable<Row>, DatabaseInterface {

  private static final long serialVersionUID = 1L;
  private static final Logger LOG = LoggerFactory.getLogger(JDBCInputFormat.class);

  /**
   * Cache of type convert class
   */
  private DbClusterInfo dbClusterInfo;
  private String queryTemplateFormat;
  private RowTypeInfo rowTypeInfo;

  private Map<String, DbShardWithConn> dbShardWithConnMap = new HashMap<>();
  private ResultSet resultSet;
  private int fetchSize;
  private String schema;

  private boolean useCustomizedSQL;

  private String initSql;

  /**
   * One split correspond to one flink's input task, the total number of input task determined by the parallelism parameter setting.
   */
  private int currentSplitNum;
  private String currentTableName = "";

  /**
   * Global range id used for recording job metrics.
   */
  private long inputSplitId;

  private SplitParameterInfo splitParameterInfo;

  private Map<Integer, Integer> jobIdOffsetInfo;

  private ResultSetMetaData metaData;

  private transient JdbcValueConverter jdbcValueConverter;

  public JDBCInputFormat() {
  }

  /**
   * A builder used to set parameters to the output format's configuration in a fluent way.
   *
   * @return builder
   */
  public static JDBCInputFormatBuilder buildJDBCInputFormat() {
    return new JDBCInputFormatBuilder();
  }

  private static void fillClusterInfo(List<ClusterInfo> clusterInfos, String table, String connectionParameters) {
    if (StringUtils.isNotEmpty(table)) {
      for (ClusterInfo connection : clusterInfos) {
        connection.setTableNames(table);
      }
    }
    if (StringUtils.isNotEmpty(connectionParameters)) {
      for (ClusterInfo connection : clusterInfos) {
        connection.setConnectionParameters(connectionParameters);
      }
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
  public void openInputFormat() throws IOException {
    super.openInputFormat();
    // Called once per inputFormat (on open)
    // Init all mysql db instances
    try {
      Class.forName(getDriverName());
      Map<Integer, List<DbShardInfo>> shardsInfo = dbClusterInfo.getShardsInfo();
      for (Map.Entry<Integer, List<DbShardInfo>> entry : shardsInfo.entrySet()) {
        List<DbShardInfo> slaves = entry.getValue();
        for (DbShardInfo host : slaves) {

          val shardWithConn = new DbShardWithConn(host, dbClusterInfo, queryTemplateFormat, inputSliceConfig, getDriverName(), initSql);
          shardWithConn.setStatementReadOnly(true);
          shardWithConn.setStatementFetchSize((getReaderFetchSize(fetchSize)));

          dbShardWithConnMap.put(host.getDbURL(), shardWithConn);
        }
      }
      jobIdOffsetInfo = new ConcurrentHashMap<Integer, Integer>();
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("open() failed." + e.getMessage(), e);
    }
  }

  @Override
  public void configure(Configuration parameters) {
    super.configure(parameters);
  }

  @Override
  public void closeInputFormat() throws IOException {
    super.closeInputFormat();
    // Called once per inputFormat (on close)
    dbShardWithConnMap.values().forEach(DbShardWithConn::close);
  }

  @Override
  public InputSplit[] createSplits(int minNumSplits) {
    if (null == splitParameterInfo) {
      return new GenericInputSplit[] {new GenericInputSplit(0, 1)};
    }
    GenericInputSplit[] ret = new GenericInputSplit[splitParameterInfo.getTaskGroupSize()];
    for (int i = 0; i < ret.length; i++) {
      ret[i] = new GenericInputSplit(i, ret.length);
    }

    setTotalSplitsNum((int) splitParameterInfo.getTotalSplitNum());
    LOG.info("Jdbc Input splits size: {}", splitParameterInfo.getTotalSplitNum());
    return ret;
  }

  /**
   * Connects to the source database and executes the query in a <b>parallel
   * fashion</b> if
   * this {@link InputFormat} is built using a parameterized query (i.e. using
   * a {@link PreparedStatement})
   * and a proper {@link ParameterValuesProvider}, in a <b>non-parallel
   * fashion</b> otherwise.
   *
   * @param inputSplit which is ignored if this InputFormat is executed as a
   *                   non-parallel source,
   *                   a "hook" to the query parameters otherwise (using its
   *                   <i>splitNumber</i>)
   */
  @Override
  public void open(InputSplit inputSplit) {
    if (null == inputSplit) {
      throw new IllegalArgumentException("InputSplit is NULL which is not allowed in BitSail.");
    }

    jdbcValueConverter = createJdbcValueConverter();
    currentSplitNum = inputSplit.getSplitNumber();
    jobIdOffsetInfo.putIfAbsent(currentSplitNum, 0);

    if (getTaskRangeInfos().isEmpty()) {
      LOG.info("Task range list is empty. Will not read any data.");
      return;
    }
    try {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Current split num: " + currentSplitNum + " Split parameter info:\n" + splitParameterInfo.toString());
      }
      fetchNextResultSet();
      metaData = resultSet.getMetaData();
    } catch (SQLException e) {
      LOG.error("open() failed." + e.getMessage());
      throw new IllegalArgumentException("open() failed." + e.getMessage(), e);
    }
  }

  /**
   * Closes all resources used.
   */
  @Override
  public void close() {
    if (null == resultSet) {
      return;
    }
    try {
      resultSet.close();
    } catch (SQLException e) {
      LOG.info("Inputformat ResultSet couldn't be closed - " + e.getMessage());
    }
  }

  @Override
  public BaseStatistics getStatistics(BaseStatistics cachedStatistics) {
    return new BaseStatistics() {
      @Override
      public long getTotalInputSize() {
        return SIZE_UNKNOWN;
      }

      @Override
      public long getNumberOfRecords() {
        if (fetchSize < 0) {
          return NUM_RECORDS_UNKNOWN;
        }
        return splitParameterInfo.getTotalSplitNum() * fetchSize;
      }

      @Override
      public float getAverageRecordWidth() {
        return AVG_RECORD_BYTES_UNKNOWN;
      }
    };
  }

  @Override
  public InputSplitAssigner getInputSplitAssigner(InputSplit[] inputSplits) {
    return new DefaultInputSplitAssigner(inputSplits);
  }

  @Override
  public String getDriverName() {
    return MysqlUtil.DRIVER_NAME;
  }

  @Override
  public String getFieldQuote() {
    return MysqlUtil.DB_QUOTE;
  }

  @Override
  public String getValueQuote() {
    return MysqlUtil.VALUE_QUOTE;
  }

  public StorageEngine getStorageEngine() {
    return StorageEngine.mysql;
  }

  private SplitRangeInfo fetchNextResultSet() throws SQLException {
    SplitRangeInfo splitRangeInfo = getRangeInfo();

    if (resultSet != null) {
      resultSet.close();
    }
    DbShardWithConn shardWithConn = dbShardWithConnMap.get(splitRangeInfo.getDbURL());

    if (useCustomizedSQL) {
      inCustomMode(splitRangeInfo, shardWithConn);
    } else {
      inNormalMode(splitRangeInfo, shardWithConn);
    }
    metaData = resultSet.getMetaData();
    return splitRangeInfo;
  }

  private void inCustomMode(SplitRangeInfo splitRangeInfo,
                            DbShardWithConn shardConnection) {
    String quoteTableWithSchema = splitRangeInfo.getQuoteTableWithSchema();
    resultSet = shardConnection.executeWithRetry(PreparedStatement::executeQuery,
        quoteTableWithSchema, false);
  }

  private void inNormalMode(SplitRangeInfo splitRangeInfo,
                            DbShardWithConn shardConnection) {
    String quoteTableWithSchema = splitRangeInfo.getQuoteTableWithSchema();
    boolean refreshStatement = true;
    if (StringUtils.isNotEmpty(quoteTableWithSchema)) {
      refreshStatement = !StringUtils.equals(quoteTableWithSchema, currentTableName);
    }
    resultSet = shardConnection.executeWithRetry(statement -> {
      setStatementRange(statement, splitRangeInfo);
      return statement.executeQuery();
    }, quoteTableWithSchema, refreshStatement);

    currentTableName = quoteTableWithSchema;
  }

  void setStatementRange(PreparedStatement statement, SplitRangeInfo splitRangeInfo) throws SQLException {
    statement.setString(1, splitRangeInfo.getBeginPos().toString());
    statement.setString(2, splitRangeInfo.getEndPos().toString());
  }

  private synchronized SplitRangeInfo getRangeInfo() {
    List<SplitRangeInfo> oneTaskGroupSplitRangeInfoList = getTaskRangeInfos();
    if (oneTaskGroupSplitRangeInfoList.isEmpty()) {
      throw new IllegalStateException("Task ranges are empty.");
    }
    int currentTaskId = getCurrentTaskId();
    jobIdOffsetInfo.put(currentSplitNum, currentTaskId + 1);
    SplitRangeInfo splitRangeInfo = oneTaskGroupSplitRangeInfoList.get(currentTaskId);

    inputSplitId = splitRangeInfo.getRangeId();
    messenger.recordSplitProgress();
    incCompletedSplits(1);
    return splitRangeInfo;
  }

  @Override
  public void completeSplits() {

  }

  @Override
  public Row buildRow(Row row, String mandatoryEncoding) throws BitSailException {
    Object rawData = null;
    String currentColumnName = null;
    try {
      for (int i = 0; i < row.getArity(); i++) {
        int columnIndex = i + 1;
        currentColumnName = metaData.getColumnName(columnIndex);

        rawData = null;
        rawData = jdbcValueConverter.convert(metaData,
            resultSet,
            columnIndex,
            mandatoryEncoding);

        row.setField(i, rawData);
      }
    } catch (SQLException e) {
      LOG.error("SQL Exception, field: {}, raw data: {} ", currentColumnName, rawData, e);
      throw BitSailException.asBitSailException(DBUtilErrorCode.SQL_EXCEPTION, generateErrorMessage(e, rawData, row), e);
    } catch (UnsupportedEncodingException e) {
      LOG.error("Unsupported encoding exception, field: {}, raw data: {} ", currentColumnName, rawData, e);
      throw BitSailException.asBitSailException(CommonErrorCode.UNSUPPORTED_ENCODING, generateErrorMessage(e, rawData, row), e);
    } catch (Throwable e) {
      LOG.error("Throw exception, field: {}, raw data: {} ", currentColumnName, rawData, e);
      throw BitSailException.asBitSailException(CommonErrorCode.RUNTIME_ERROR, generateErrorMessage(e, rawData, row), e);
    }
    return row;
  }

  protected JdbcValueConverter createJdbcValueConverter() {
    return new JdbcValueConverter();
  }

  private String generateErrorMessage(Throwable throwable, Object rawData, Row row) {
    return String.format("Error: %s. \n Raw data: %s. \n Row: %s.", throwable.getMessage(), rawData, row);
  }

  @Override
  public void initPlugin() throws ExecutionException, InterruptedException {
    validateParameters();
  }

  @Override
  public String getType() {
    return "JDBC";
  }

  /**
   * Checks whether all data has been read.
   *
   * @return boolean value indication whether all data has been read.
   */
  @Override
  public boolean isSplitEnd() {
    if (getTaskRangeInfos().isEmpty()) {
      LOG.info("Task range list is empty. Will not read any data.");
      return true;
    }

    try {
      hasNext = resultSet.next();
    } catch (SQLException e) {
      throw new IllegalArgumentException("reachedEnd() failed.", e);
    }

    while (!hasNext && (getCurrentTaskId() < getTaskRangeInfos().size())) {
      SplitRangeInfo splitRangeInfo = null;

      try {
        splitRangeInfo = fetchNextResultSet();
        hasNext = resultSet.next();
      } catch (SQLException e) {
        final String msg = "reachedEnd() failed. Range info: " + (splitRangeInfo == null ? "null" : splitRangeInfo.toString());
        LOG.error(msg, e);
        throw new IllegalArgumentException(msg, e);
      }
    }

    return !hasNext;
  }

  private Integer getCurrentTaskId() {
    return jobIdOffsetInfo.get(currentSplitNum);
  }

  private List<SplitRangeInfo> getTaskRangeInfos() {
    if (null == splitParameterInfo) {
      return Collections.emptyList();
    }
    final List<SplitRangeInfo> result = splitParameterInfo.getOneTaskGroupParametersInfo(currentSplitNum);
    return result == null ? Collections.emptyList() : result;
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  private void validateParameters() throws ExecutionException, InterruptedException {
    String customizedSQL = inputSliceConfig.getUnNecessaryOption(JdbcReaderOptions.CUSTOMIZED_SQL, null);
    useCustomizedSQL = customizedSQL != null && customizedSQL.length() > 0;

    // Necessary values
    String userName = inputSliceConfig.getNecessaryOption(JdbcReaderOptions.USER_NAME, JDBCPluginErrorCode.REQUIRED_VALUE);
    String password = inputSliceConfig.getNecessaryOption(JdbcReaderOptions.PASSWORD, JDBCPluginErrorCode.REQUIRED_VALUE);

    initSql = inputSliceConfig.get(JdbcReaderOptions.INIT_SQL);
    if (!Strings.isNullOrEmpty(initSql)) {
      LOG.info("Init sql is " + initSql);
    }

    List<ColumnInfo> columns = inputSliceConfig.getNecessaryOption(JdbcReaderOptions.COLUMNS, JDBCPluginErrorCode.REQUIRED_VALUE);
    String splitPK = null;
    String table = null;
    String schema = null;
    String tableWithSchema = null;
    SqlType.SqlTypes splitType = null;
    if (!useCustomizedSQL) {
      splitPK = inputSliceConfig.getNecessaryOption(JdbcReaderOptions.SPLIT_PK, JDBCPluginErrorCode.REQUIRED_VALUE);
      String splitPkType = getOrDefaultSplitType(inputSliceConfig, columns, splitPK);
      inputSliceConfig.set(JdbcReaderOptions.SPLIT_PK_JDBC_TYPE, splitPkType);
      splitType = SqlType.getSqlType(splitPkType, getDriverName());
      table = inputSliceConfig.get(JdbcReaderOptions.TABLE_NAME);
      schema = inputSliceConfig.getUnNecessaryOption(JdbcReaderOptions.TABLE_SCHEMA, JdbcReaderOptions.TABLE_SCHEMA.defaultValue());
    }

    List<ClusterInfo> connections = inputSliceConfig.getNecessaryOption(JdbcReaderOptions.CONNECTIONS, JDBCPluginErrorCode.REQUIRED_VALUE);
    String connectionParameters = inputSliceConfig.getUnNecessaryOption(JdbcReaderOptions.CONNECTION_PARAMETERS, null);
    fillClusterInfo(connections, table, connectionParameters);

    // Unnecessary values
    int fetchSize = inputSliceConfig.getUnNecessaryOption(JdbcReaderOptions.READER_FETCH_SIZE, 10000);
    int readerParallelismNum = useCustomizedSQL ? -1 : inputSliceConfig.getUnNecessaryOption(JdbcReaderOptions.READER_PARALLELISM_NUM, -1);
    String filter = useCustomizedSQL ? null : inputSliceConfig.getUnNecessaryOption(JdbcReaderOptions.FILTER, null);
    List<String> shardKey = useCustomizedSQL ? null : inputSliceConfig.get(JdbcReaderOptions.SHARD_KEY);
    String shardSplitMode = inputSliceConfig.getUnNecessaryOption(JdbcReaderOptions.SHARD_SPLIT_MODE, FixedLenParametersProvider.ShardSplitMode.accurate.toString());
    // Generate from conf params
    DbClusterInfo dbClusterInfo = new DbClusterInfo(userName, password, schema, splitPK, shardKey, 1, 1, 1, 120, connections);

    ParameterValuesProvider paramProvider = null;
    // init paramProvider
    if (!useCustomizedSQL) {
      paramProvider = getFixedLenParametersProvider(splitType, fetchSize,
          readerParallelismNum, filter, shardSplitMode, dbClusterInfo, initSql);

      LOG.info("Shard split mode in FixedLenParametersProvider is: {}", shardSplitMode);
    } else {
      paramProvider = new NoSplitParametersProvider(dbClusterInfo);
      LOG.info("Customized SQL is provided, Use NoSplitParametersProvider");
    }

    String sqlTemplateFormat = useCustomizedSQL ? customizedSQL : genSqlTemplate(splitPK, columns, filter);
    LOG.info("SQL Template Format: " + sqlTemplateFormat);

    // Init member
    this.dbClusterInfo = dbClusterInfo;
    this.queryTemplateFormat = sqlTemplateFormat;
    this.splitParameterInfo = paramProvider.getParameterValues();
    this.fetchSize = fetchSize;
    rowTypeInfo = NativeFlinkTypeInfoUtil.getRowTypeInformation(columns, new FileMappingTypeInfoConverter(getStorageEngine().name()));

    LOG.info("Row Type Info: " + rowTypeInfo);
    LOG.info("Validate plugin configuration parameters finished.");
  }

  FixedLenParametersProvider getFixedLenParametersProvider(SqlType.SqlTypes splitType, int fetchSize,
                                                           int readerParallelismNum, String filter, String shardSplitMode,
                                                           DbClusterInfo dbClusterInfo) {
    return this.getFixedLenParametersProvider(splitType, fetchSize, readerParallelismNum, filter,
        shardSplitMode, dbClusterInfo, "");
  }

  private FixedLenParametersProvider getFixedLenParametersProvider(SqlType.SqlTypes splitType, int fetchSize,
                                                                   int readerParallelismNum, String filter, String shardSplitMode,
                                                                   DbClusterInfo dbClusterInfo, String initSql) {
    FixedLenParametersProvider paramProvider;
    switch (splitType) {
      case Int:
      case Long:
      case Short:
      case BigInt:
        paramProvider = new FixedLenParametersProvider<BigInteger>(this, fetchSize, dbClusterInfo,
            readerParallelismNum, getDriverName(), filter, inputSliceConfig, commonConfig,
            FixedLenParametersProvider.ShardSplitMode.valueOf(shardSplitMode), initSql);
        break;
      case BigDecimal:
        LOG.warn("BigDecimal split is not supported yet, try bigInteger split instead");
        paramProvider = new FixedLenParametersProvider<BigInteger>(this, fetchSize, dbClusterInfo,
            readerParallelismNum, getDriverName(), filter, inputSliceConfig, commonConfig,
            FixedLenParametersProvider.ShardSplitMode.valueOf(shardSplitMode), initSql);
        break;
      case String:
        paramProvider = new FixedLenParametersProvider<String>(this, fetchSize, dbClusterInfo,
            readerParallelismNum, getDriverName(), filter, inputSliceConfig, commonConfig,
            FixedLenParametersProvider.ShardSplitMode.valueOf(shardSplitMode), initSql);
        break;
      default:
        throw BitSailException.asBitSailException(CommonErrorCode.CONFIG_ERROR, "split type: " + splitType + " is not supported yet");
    }
    return paramProvider;
  }

  String getOrDefaultSplitType(BitSailConfiguration inputSliceConfig, List<ColumnInfo> columns, String splitPK) {
    String splitPkType = null;
    if (inputSliceConfig.fieldExists(JdbcReaderOptions.SPLIT_PK_JDBC_TYPE)) {
      splitPkType = inputSliceConfig.get(JdbcReaderOptions.SPLIT_PK_JDBC_TYPE).toLowerCase();
    } else {
      for (ColumnInfo columnInfo : columns) {
        String name = columnInfo.getName();
        String type = columnInfo.getType().toLowerCase();
        if (name.equalsIgnoreCase(splitPK)) {
          splitPkType = type;
          LOG.info("split key is '{}', type is '{}'", splitPK, type);
          break;
        }
      }
      if (splitPkType == null) {
        LOG.warn("split key '{}' is not found in field mapping, try use int split", splitPK);
        return "int";
      }
    }
    return splitPkType;
  }

  /**
   * @param splitPK: split primary key
   * @param columns: columns
   * @param filter:  filter
   */
  String genSqlTemplate(String splitPK, List<ColumnInfo> columns, String filter) {
    String tableToken = "%s";
    StringBuilder sql = new StringBuilder("SELECT ");
    for (ColumnInfo column : columns) {
      String columnName = column.getName();
      sql.append(getQuoteColumn(columnName)).append(",");
    }
    sql.deleteCharAt(sql.length() - 1);
    sql.append(" FROM ").append(tableToken).append(" WHERE ");
    if (null != filter) {
      sql.append("(").append(filter).append(")");
      sql.append(" AND ");
    }
    sql.append("(").append(getQuoteColumn(splitPK)).append(" >= ? AND ").append(getQuoteColumn(splitPK)).append(" < ?)");
    return sql.toString();
  }

  @Override
  public SourceEngineConnector initSourceSchemaManager(BitSailConfiguration commonConf, BitSailConfiguration readerConf) throws Exception {
    return new JDBCSourceEngineConnectorBase(commonConf, readerConf);
  }

  /**
   * In mariadb mysql java client, it enables streaming result set by `setFetchSize({any positive integer})`.<br/>
   * In official mysql jdbc client, it enables streaming result set by `setFetchSize(INT.MIN_VALUE)`, and fetch one row once.<br/>
   * For more details: <a href="https://mariadb.com/kb/en/about-mariadb-connector-j/#streaming-result-sets">MariaDB connector: streaming result sets</a>
   */
  public int getReaderFetchSize(int fetchSize) {
    if (fetchSize < 0) {
      fetchSize = 1;
      LOG.warn("fetch size should not be negative in jdbc reader.");
    }
    return fetchSize;
  }

  /**
   * Builder for a {@link JDBCInputFormat}.
   */
  public static class JDBCInputFormatBuilder {
    private final JDBCInputFormat format;

    public JDBCInputFormatBuilder() {
      this.format = new JDBCInputFormat();
    }

    public JDBCInputFormatBuilder setDbInfo(DbClusterInfo dbClusterInfo) {
      format.dbClusterInfo = dbClusterInfo;
      return this;
    }

    public JDBCInputFormatBuilder setQuery(String query) {
      format.queryTemplateFormat = query;
      return this;
    }

    public JDBCInputFormatBuilder setParametersProvider(ParameterValuesProvider parameterValuesProvider)
        throws IOException, ExecutionException, InterruptedException {
      format.splitParameterInfo = parameterValuesProvider.getParameterValues();
      return this;
    }

    public JDBCInputFormatBuilder setRowTypeInfo(RowTypeInfo rowTypeInfo) {
      format.rowTypeInfo = rowTypeInfo;
      return this;
    }

    public JDBCInputFormatBuilder setFetchSize(int fetchSize) {
      Preconditions
          .checkArgument(fetchSize > 0, "Illegal value %s for fetchSize, has to be positive.", fetchSize);
      format.fetchSize = fetchSize;
      return this;
    }

    public JDBCInputFormat finish() {
      if (null == format.dbClusterInfo) {
        throw new IllegalArgumentException("No DB Info supplied");
      }
      if (null == format.queryTemplateFormat) {
        throw new IllegalArgumentException("No query supplied");
      }
      if (null == format.rowTypeInfo) {
        throw new IllegalArgumentException("No " + RowTypeInfo.class.getSimpleName() + " supplied");
      }
      if (null == format.splitParameterInfo) {
        throw new IllegalArgumentException("No " + ParameterValuesProvider.class.getSimpleName() + " supplied");
      }
      return format;
    }

    public JDBCInputFormat initFromConf(BitSailConfiguration commonConf, BitSailConfiguration inputConf)
        throws Exception {
      format.initFromConf(commonConf, inputConf);
      return format;
    }
  }
}
