/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.drill.exec.store.jdbc;

import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlDialect.DatabaseProduct;
import org.apache.calcite.sql.util.SqlBuilder;
import org.apache.drill.common.AutoCloseables;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers;
import org.apache.drill.exec.expr.holders.BigIntHolder;
import org.apache.drill.exec.expr.holders.BitHolder;
import org.apache.drill.exec.expr.holders.DateHolder;
import org.apache.drill.exec.expr.holders.Float4Holder;
import org.apache.drill.exec.expr.holders.Float8Holder;
import org.apache.drill.exec.expr.holders.IntHolder;
import org.apache.drill.exec.expr.holders.NullableBigIntHolder;
import org.apache.drill.exec.expr.holders.NullableBitHolder;
import org.apache.drill.exec.expr.holders.NullableDateHolder;
import org.apache.drill.exec.expr.holders.NullableFloat4Holder;
import org.apache.drill.exec.expr.holders.NullableFloat8Holder;
import org.apache.drill.exec.expr.holders.NullableIntHolder;
import org.apache.drill.exec.expr.holders.NullableSmallIntHolder;
import org.apache.drill.exec.expr.holders.NullableTimeHolder;
import org.apache.drill.exec.expr.holders.NullableTimeStampHolder;
import org.apache.drill.exec.expr.holders.NullableTinyIntHolder;
import org.apache.drill.exec.expr.holders.NullableVarCharHolder;
import org.apache.drill.exec.expr.holders.NullableVarDecimalHolder;
import org.apache.drill.exec.expr.holders.SmallIntHolder;
import org.apache.drill.exec.expr.holders.TimeHolder;
import org.apache.drill.exec.expr.holders.TimeStampHolder;
import org.apache.drill.exec.expr.holders.TinyIntHolder;
import org.apache.drill.exec.expr.holders.VarCharHolder;
import org.apache.drill.exec.expr.holders.VarDecimalHolder;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.store.AbstractRecordWriter;
import org.apache.drill.exec.store.EventBasedRecordWriter.FieldConverter;
import org.apache.drill.exec.store.jdbc.utils.JdbcDDLQueryUtils;
import org.apache.drill.exec.store.jdbc.utils.CreateTableStmtBuilder;
import org.apache.drill.exec.util.DecimalUtility;
import org.apache.drill.exec.vector.complex.reader.FieldReader;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.apache.parquet.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class JdbcRecordWriter extends AbstractRecordWriter {

  private static final Logger logger = LoggerFactory.getLogger(JdbcRecordWriter.class);

  private final String tableName;
  private Connection connection;
  private final SqlDialect dialect;
  private final List<Object> rowList;
  private final List<JdbcWriterField> fields;
  private final String rawTableName;
  private final JdbcWriter config;
  private SqlBuilder insertQueryBuilder;
  private boolean firstRecord;
  private int recordCount;

  /*
   * This map maps JDBC data types to their Drill equivalents.  The basic strategy is that if there
   * is a Drill equivalent, then do the mapping as expected.
   *
   * All flavors of character fields are mapped to VARCHAR in Drill. All versions of binary fields are
   * mapped to VARBINARY.
   */
  public static final ImmutableMap<MinorType, Integer> JDBC_TYPE_MAPPINGS = ImmutableMap.<MinorType, Integer>builder()
      .put(MinorType.FLOAT8, java.sql.Types.DOUBLE)
      .put(MinorType.FLOAT4, java.sql.Types.FLOAT)
      .put(MinorType.TINYINT, java.sql.Types.TINYINT)
      .put(MinorType.SMALLINT, java.sql.Types.SMALLINT)
      .put(MinorType.INT, java.sql.Types.INTEGER)
      .put(MinorType.BIGINT, java.sql.Types.BIGINT)
      .put(MinorType.VARCHAR, java.sql.Types.VARCHAR)
      .put(MinorType.VARBINARY, java.sql.Types.VARBINARY)
      .put(MinorType.VARDECIMAL, java.sql.Types.DECIMAL)
      .put(MinorType.DATE, java.sql.Types.DATE)
      .put(MinorType.TIME, java.sql.Types.TIME)
      .put(MinorType.TIMESTAMP, java.sql.Types.TIMESTAMP)
      .put(MinorType.BIT, java.sql.Types.BOOLEAN)
      .build();

  public JdbcRecordWriter(DataSource source, OperatorContext context, String name, JdbcWriter config) {
    this.tableName = JdbcDDLQueryUtils.addBackTicksToTable(name);
    this.rowList = new ArrayList<>();
    this.dialect = config.getPlugin().getDialect();
    this.config = config;
    this.rawTableName = name;
    this.fields = new ArrayList<>();
    this.firstRecord = true;
    this.recordCount = 0;

    this.insertQueryBuilder = initializeInsertQuery();

    try {
      this.connection = source.getConnection();
    } catch (SQLException e) {
      throw UserException.connectionError()
        .message("Unable to open JDBC connection for writing.")
        .addContext(e.getSQLState())
        .build(logger);
    }
  }

  @Override
  public void init(Map<String, String> writerOptions) {
    // Nothing to see here...
  }

  @Override
  public void updateSchema(VectorAccessible batch) {
    BatchSchema schema = batch.getSchema();
    String columnName;
    MinorType type;
    String sql;
    boolean nullable = false;
    CreateTableStmtBuilder queryBuilder = new CreateTableStmtBuilder(tableName, dialect);

    for (MaterializedField field : schema) {
      columnName = JdbcDDLQueryUtils.addBackTicksToField(field.getName());
      type = field.getType().getMinorType();
      logger.debug("Adding column {} of type {}.", columnName, type);

      if (field.getType().getMode() == DataMode.REPEATED) {
        throw UserException.dataWriteError()
          .message("Drill does not yet support writing arrays to JDBC. " + columnName + " is an array.")
          .build(logger);
      }

      if (field.getType().getMode() == DataMode.OPTIONAL) {
        nullable = true;
      }

      int precision = field.getPrecision();
      int scale = field.getScale();

      queryBuilder.addColumn(columnName, field.getType().getMinorType(), nullable, precision, scale);
    }

    sql = queryBuilder.build().getCreateTableQuery();
    sql = JdbcDDLQueryUtils.cleanDDLQuery(sql, dialect);
    logger.debug("Final query: {}", sql);

    // Execute the query to build the schema
    try (Statement statement = connection.createStatement()) {
      logger.debug("Executing CREATE query: {}", sql);
      statement.execute(sql);
    } catch (SQLException e) {
      throw UserException.dataReadError(e)
        .message("The JDBC storage plugin failed while trying to create the schema. ")
        .addContext("Sql", sql)
        .build(logger);
    }
  }

  @Override
  public void startRecord() {
    rowList.clear();

    if (!firstRecord) {
      insertQueryBuilder.append(",");
    }
    insertQueryBuilder.append("(");
    logger.debug("Start record");
  }

  @Override
  public void endRecord() throws IOException {
    logger.debug("Ending record");

    // Add values to rowString
    for (int i = 0; i < rowList.size(); i++) {
      if (i > 0) {
        insertQueryBuilder.append(",");
      }

      // Add null value to rowstring
      if (rowList.get(i) instanceof String && ((String) rowList.get(i)).equalsIgnoreCase("null")) {
        insertQueryBuilder.append("null");
        continue;
      }

      JdbcWriterField currentField = fields.get(i);
      if (currentField.getDataType() == MinorType.VARCHAR) {
        String value = null;
        // Get the string value
        if (currentField.getMode() == DataMode.REQUIRED) {
          VarCharHolder varCharHolder = (VarCharHolder) rowList.get(i);
          value = StringFunctionHelpers.getStringFromVarCharHolder(varCharHolder);
        } else {
          try {
            NullableVarCharHolder nullableVarCharHolder = (NullableVarCharHolder) rowList.get(i);
            value = StringFunctionHelpers.getStringFromVarCharHolder(nullableVarCharHolder);
          } catch (ClassCastException e) {
            logger.error("Unable to read field: {}",  rowList.get(i));
          }
        }

        // Add to value string
        insertQueryBuilder.literal(value);
      } else if (currentField.getDataType() == MinorType.DATE) {
        String dateString = formatDateForInsertQuery((Long) rowList.get(i));
        insertQueryBuilder.literal(dateString);
      } else if (currentField.getDataType() == MinorType.TIME) {
        String timeString = formatTimeForInsertQuery((Integer) rowList.get(i));
        insertQueryBuilder.literal(timeString);
      } else if (currentField.getDataType() == MinorType.TIMESTAMP) {
        String timeString = formatTimeStampForInsertQuery((Long) rowList.get(i));
        insertQueryBuilder.literal(timeString);
      } else {
        if (Strings.isNullOrEmpty(rowList.get(i).toString())) {
          insertQueryBuilder.append("null");
        } else {
          insertQueryBuilder.append(rowList.get(i).toString());
        }
      }
    }

    recordCount++;
    firstRecord = false;
    insertQueryBuilder.append(")");

    if (recordCount >= config.getPlugin().getConfig().getWriterBatchSize()) {
      // Execute the insert query
      String insertQuery = insertQueryBuilder.toString();
      executeInsert(insertQuery);

      // Reset the batch
      recordCount = 0;
      firstRecord = true;
      insertQueryBuilder = initializeInsertQuery();
    }

    rowList.clear();
  }

  @Override
  public void abort() {
    logger.debug("Abort insert.");
  }

  @Override
  public void cleanup() throws IOException {
    logger.debug("Cleanup record");
    // Execute last query
    String insertQuery = insertQueryBuilder.toString();
    if (recordCount != 0) {
      executeInsert(insertQuery);
    }
    AutoCloseables.closeSilently(connection);
  }

  private void executeInsert(String insertQuery) throws IOException {
    try (Statement stmt = connection.createStatement()) {
      logger.debug("Executing insert query: {}", insertQuery);
      stmt.execute(insertQuery);
      logger.debug("Query complete");
      // Close connection
      AutoCloseables.closeSilently(stmt);
    } catch (SQLException e) {
      logger.error("Error: {} {} {}", e.getMessage(), e.getSQLState(), e.getErrorCode());
      AutoCloseables.closeSilently(connection);
      throw new IOException(e.getMessage() + " " + e.getSQLState() + "\n" + insertQuery);
    }
  }

  private SqlBuilder initializeInsertQuery() {
    SqlBuilder builder = new SqlBuilder(this.dialect);

    // Apache Phoenix does not support INSERT but does support UPSERT using the same syntax
    if (dialect == DatabaseProduct.PHOENIX.getDialect()) {
      builder.append("UPSERT INTO ");
    } else {
      builder.append("INSERT INTO ");
    }

    JdbcDDLQueryUtils.addTableToInsertQuery(builder, rawTableName);
    builder.append (" VALUES ");
    return builder;
  }

  /**
   * Drill returns longs for date values. This function converts longs into dates formatted
   * in YYYY-MM-dd format for insertion into a database.
   * @param dateVal long representing a naive date
   * @return A date string formatted YYYY-MM-dd
   */
  private String formatDateForInsertQuery(Long dateVal) {
    Date date=new Date(dateVal);
    SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd");
    return df2.format(date);
  }

  /**
   * Drill returns longs for time values. This function converts longs into times formatted
   * in HH:mm:ss format for insertion into a database.
   * @param millis Milliseconds since the epoch.
   * @return A time string formatted for insertion into a database.
   */
  private String formatTimeForInsertQuery(Integer millis) {
    return String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
      TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
      TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));
  }

  /**
   * Drill returns longs for date times. This function converts
   * @param time An input long that represents a timestamp
   * @return A ISO formatted timestamp.
   */
  private String formatTimeStampForInsertQuery(Long time) {
    Date date = new Date(time);
    Format format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    return format.format(date);
  }

  @Override
  public FieldConverter getNewNullableIntConverter(int fieldId, String fieldName, FieldReader reader) {
    return new NullableIntJDBCConverter(fieldId, fieldName, reader, fields);
  }

  public class NullableIntJDBCConverter extends FieldConverter {
    private final NullableIntHolder holder = new NullableIntHolder();

    public NullableIntJDBCConverter(int fieldID, String fieldName, FieldReader reader, List<JdbcWriterField> fields) {
      super(fieldID, fieldName, reader);
      fields.add(new JdbcWriterField(fieldName, MinorType.INT, DataMode.OPTIONAL));
    }

    @Override
    public void writeField() {
      if (!reader.isSet()) {
        rowList.add("null");
        return;
      }
      reader.read(holder);
      rowList.add(holder.value);
    }
  }

  @Override
  public FieldConverter getNewIntConverter(int fieldId, String fieldName, FieldReader reader) {
    return new IntJDBCConverter(fieldId, fieldName, reader, fields);
  }

  public class IntJDBCConverter extends FieldConverter {
    private final IntHolder holder = new IntHolder();

    public IntJDBCConverter(int fieldID, String fieldName, FieldReader reader, List<JdbcWriterField> fields) {
      super(fieldID, fieldName, reader);
      fields.add(new JdbcWriterField(fieldName, MinorType.INT, DataMode.REQUIRED));
    }

    @Override
    public void writeField() {
      reader.read(holder);
      rowList.add(holder.value);
    }
  }

  @Override
  public FieldConverter getNewNullableBigIntConverter(int fieldId, String fieldName, FieldReader reader) {
    return new NullableBigIntJDBCConverter(fieldId, fieldName, reader, fields);
  }

  public class NullableBigIntJDBCConverter extends FieldConverter {
    private final NullableBigIntHolder holder = new NullableBigIntHolder();

    public NullableBigIntJDBCConverter(int fieldID, String fieldName, FieldReader reader, List<JdbcWriterField> fields) {
      super(fieldID, fieldName, reader);
      fields.add(new JdbcWriterField(fieldName, MinorType.BIGINT, DataMode.OPTIONAL));
    }

    @Override
    public void writeField() {
      if (!reader.isSet()) {
        rowList.add("null");
        return;
      }
      reader.read(holder);
      rowList.add(holder.value);
    }
  }

  @Override
  public FieldConverter getNewBigIntConverter(int fieldId, String fieldName, FieldReader reader) {
    return new BigIntJDBCConverter(fieldId, fieldName, reader, fields);
  }

  public class BigIntJDBCConverter extends FieldConverter {
    private final BigIntHolder holder = new BigIntHolder();

    public BigIntJDBCConverter(int fieldID, String fieldName, FieldReader reader, List<JdbcWriterField> fields) {
      super(fieldID, fieldName, reader);
      fields.add(new JdbcWriterField(fieldName, MinorType.BIGINT, DataMode.REQUIRED));
    }

    @Override
    public void writeField() {
      if (!reader.isSet()) {
        rowList.add("null");
        return;
      }
      reader.read(holder);
      rowList.add(holder.value);
    }
  }

  @Override
  public FieldConverter getNewNullableSmallIntConverter(int fieldId, String fieldName, FieldReader reader) {
    return new NullableSmallIntJDBCConverter(fieldId, fieldName, reader, fields);
  }

  public class NullableSmallIntJDBCConverter extends FieldConverter {
    private final NullableSmallIntHolder holder = new NullableSmallIntHolder();

    public NullableSmallIntJDBCConverter(int fieldID, String fieldName, FieldReader reader, List<JdbcWriterField> fields) {
      super(fieldID, fieldName, reader);
      fields.add(new JdbcWriterField(fieldName, MinorType.SMALLINT, DataMode.OPTIONAL));
    }

    @Override
    public void writeField() {
      if (!reader.isSet()) {
        rowList.add("null");
        return;
      }
      reader.read(holder);
      rowList.add(holder.value);
    }
  }

  @Override
  public FieldConverter getNewSmallIntConverter(int fieldId, String fieldName, FieldReader reader) {
    return new SmallIntJDBCConverter(fieldId, fieldName, reader, fields);
  }

  public class SmallIntJDBCConverter extends FieldConverter {
    private final SmallIntHolder holder = new SmallIntHolder();

    public SmallIntJDBCConverter(int fieldID, String fieldName, FieldReader reader, List<JdbcWriterField> fields) {
      super(fieldID, fieldName, reader);
      fields.add(new JdbcWriterField(fieldName, MinorType.SMALLINT, DataMode.REQUIRED));
    }

    @Override
    public void writeField() {
      reader.read(holder);
      rowList.add(holder.value);
    }
  }

  @Override
  public FieldConverter getNewNullableTinyIntConverter(int fieldId, String fieldName, FieldReader reader) {
    return new NullableTinyIntJDBCConverter(fieldId, fieldName, reader, fields);
  }

  public class NullableTinyIntJDBCConverter extends FieldConverter {
    private final NullableTinyIntHolder holder = new NullableTinyIntHolder();

    public NullableTinyIntJDBCConverter(int fieldID, String fieldName, FieldReader reader, List<JdbcWriterField> fields) {
      super(fieldID, fieldName, reader);
      fields.add(new JdbcWriterField(fieldName, MinorType.TINYINT, DataMode.OPTIONAL));
    }

    @Override
    public void writeField() {
      if (!reader.isSet()) {
        rowList.add("null");
        return;
      }
      reader.read(holder);
      rowList.add(holder.value);
    }
  }

  @Override
  public FieldConverter getNewTinyIntConverter(int fieldId, String fieldName, FieldReader reader) {
    return new TinyIntJDBCConverter(fieldId, fieldName, reader, fields);
  }

  public class TinyIntJDBCConverter extends FieldConverter {
    private final TinyIntHolder holder = new TinyIntHolder();

    public TinyIntJDBCConverter(int fieldID, String fieldName, FieldReader reader, List<JdbcWriterField> fields) {
      super(fieldID, fieldName, reader);
      fields.add(new JdbcWriterField(fieldName, MinorType.TINYINT, DataMode.REQUIRED));
    }

    @Override
    public void writeField() {
      reader.read(holder);
      rowList.add(holder.value);
    }
  }

  @Override
  public FieldConverter getNewNullableFloat4Converter(int fieldId, String fieldName, FieldReader reader) {
    return new NullableFloat4JDBCConverter(fieldId, fieldName, reader, fields);
  }

  public class NullableFloat4JDBCConverter extends FieldConverter {
    private final NullableFloat4Holder holder = new NullableFloat4Holder();

    public NullableFloat4JDBCConverter(int fieldID, String fieldName, FieldReader reader, List<JdbcWriterField> fields) {
      super(fieldID, fieldName, reader);
      fields.add(new JdbcWriterField(fieldName, MinorType.FLOAT4, DataMode.OPTIONAL));
    }

    @Override
    public void writeField() {
      if (!reader.isSet()) {
        rowList.add("null");
        return;
      }
      reader.read(holder);
      rowList.add(holder.value);
    }
  }

  @Override
  public FieldConverter getNewFloat4Converter(int fieldId, String fieldName, FieldReader reader) {
    return new Float4JDBCConverter(fieldId, fieldName, reader, fields);
  }

  public class Float4JDBCConverter extends FieldConverter {
    private final Float4Holder holder = new Float4Holder();

    public Float4JDBCConverter(int fieldID, String fieldName, FieldReader reader, List<JdbcWriterField> fields) {
      super(fieldID, fieldName, reader);
      fields.add(new JdbcWriterField(fieldName, MinorType.FLOAT4, DataMode.REQUIRED));
    }

    @Override
    public void writeField() {
      reader.read(holder);
      rowList.add(holder.value);
    }
  }

  @Override
  public FieldConverter getNewNullableFloat8Converter(int fieldId, String fieldName, FieldReader reader) {
    return new NullableFloat8JDBCConverter(fieldId, fieldName, reader, fields);
  }

  public class NullableFloat8JDBCConverter extends FieldConverter {
    private final NullableFloat8Holder holder = new NullableFloat8Holder();

    public NullableFloat8JDBCConverter(int fieldID, String fieldName, FieldReader reader, List<JdbcWriterField> fields) {
      super(fieldID, fieldName, reader);
      fields.add(new JdbcWriterField(fieldName, MinorType.FLOAT8, DataMode.OPTIONAL));
    }

    @Override
    public void writeField() {
      if (!reader.isSet()) {
        rowList.add("null");
        return;
      }
      reader.read(holder);
      rowList.add(holder.value);
    }
  }

  @Override
  public FieldConverter getNewFloat8Converter(int fieldId, String fieldName, FieldReader reader) {
    return new Float8JDBCConverter(fieldId, fieldName, reader, fields);
  }

  public class Float8JDBCConverter extends FieldConverter {
    private final Float8Holder holder = new Float8Holder();

    public Float8JDBCConverter(int fieldID, String fieldName, FieldReader reader, List<JdbcWriterField> fields) {
      super(fieldID, fieldName, reader);
      fields.add(new JdbcWriterField(fieldName, MinorType.FLOAT8, DataMode.REQUIRED));
    }

    @Override
    public void writeField() {
      reader.read(holder);
      rowList.add(holder.value);
    }
  }

  @Override
  public FieldConverter getNewNullableVarDecimalConverter(int fieldId, String fieldName, FieldReader reader) {
    return new NullableVardecimalJDBCConverter(fieldId, fieldName, reader, fields);
  }

  public class NullableVardecimalJDBCConverter extends FieldConverter {
    private final NullableVarDecimalHolder holder = new NullableVarDecimalHolder();

    public NullableVardecimalJDBCConverter(int fieldID, String fieldName, FieldReader reader, List<JdbcWriterField> fields) {
      super(fieldID, fieldName, reader);
      fields.add(new JdbcWriterField(fieldName, MinorType.VARDECIMAL, DataMode.OPTIONAL));
    }

    @Override
    public void writeField() {
      if (!reader.isSet()) {
        rowList.add("null");
        return;
      }
      reader.read(holder);
      BigDecimal value = DecimalUtility.getBigDecimalFromDrillBuf(holder.buffer,
        holder.start, holder.end - holder.start, holder.scale);
      rowList.add(value);
    }
  }

  @Override
  public FieldConverter getNewVarDecimalConverter(int fieldId, String fieldName, FieldReader reader) {
    return new VardecimalJDBCConverter(fieldId, fieldName, reader, fields);
  }

  public class VardecimalJDBCConverter extends FieldConverter {
    private final VarDecimalHolder holder = new VarDecimalHolder();

    public VardecimalJDBCConverter(int fieldID, String fieldName, FieldReader reader, List<JdbcWriterField> fields) {
      super(fieldID, fieldName, reader);
      fields.add(new JdbcWriterField(fieldName, MinorType.VARDECIMAL, DataMode.REQUIRED));
    }

    @Override
    public void writeField() {
      reader.read(holder);
      BigDecimal value = DecimalUtility.getBigDecimalFromDrillBuf(holder.buffer,
        holder.start, holder.end - holder.start, holder.scale);
      rowList.add(value);
    }
  }

  @Override
  public FieldConverter getNewNullableVarCharConverter(int fieldId, String fieldName, FieldReader reader) {
    return new NullableVarCharJDBCConverter(fieldId, fieldName, reader, fields);
  }

  public class NullableVarCharJDBCConverter extends FieldConverter {
    private final NullableVarCharHolder holder = new NullableVarCharHolder();

    public NullableVarCharJDBCConverter(int fieldID, String fieldName, FieldReader reader, List<JdbcWriterField> fields) {
      super(fieldID, fieldName, reader);
      fields.add(new JdbcWriterField(fieldName, MinorType.VARCHAR, DataMode.OPTIONAL));
    }

    @Override
    public void writeField() {
      reader.read(holder);
      if (reader.isSet()) {
        byte[] bytes = new byte[holder.end - holder.start];
        holder.buffer.getBytes(holder.start, bytes);
      }
      rowList.add(holder);
    }
  }

  @Override
  public FieldConverter getNewVarCharConverter(int fieldId, String fieldName, FieldReader reader) {
    return new VarCharJDBCConverter(fieldId, fieldName, reader, fields);
  }

  public class VarCharJDBCConverter extends FieldConverter {
    private final VarCharHolder holder = new VarCharHolder();

    public VarCharJDBCConverter(int fieldID, String fieldName, FieldReader reader, List<JdbcWriterField> fields) {
      super(fieldID, fieldName, reader);
      fields.add(new JdbcWriterField(fieldName, MinorType.VARCHAR, DataMode.REQUIRED));
    }

    @Override
    public void writeField() {
      reader.read(holder);
      if (reader.isSet()) {
        byte[] bytes = new byte[holder.end - holder.start];
        holder.buffer.getBytes(holder.start, bytes);
        rowList.add(holder);
      }
    }
  }

  @Override
  public FieldConverter getNewNullableDateConverter(int fieldId, String fieldName, FieldReader reader) {
    return new NullableDateJDBCConverter(fieldId, fieldName, reader, fields);
  }

  public class NullableDateJDBCConverter extends FieldConverter {
    private final NullableDateHolder holder = new NullableDateHolder();

    public NullableDateJDBCConverter(int fieldID, String fieldName, FieldReader reader, List<JdbcWriterField> fields) {
      super(fieldID, fieldName, reader);
      fields.add(new JdbcWriterField(fieldName, MinorType.DATE, DataMode.OPTIONAL));
    }

    @Override
    public void writeField() {
      if (!reader.isSet()) {
        rowList.add("null");
        return;
      }
      reader.read(holder);
      rowList.add(holder.value);
    }
  }

  @Override
  public FieldConverter getNewDateConverter(int fieldId, String fieldName, FieldReader reader) {
    return new DateJDBCConverter(fieldId, fieldName, reader, fields);
  }

  public class DateJDBCConverter extends FieldConverter {
    private final DateHolder holder = new DateHolder();

    public DateJDBCConverter(int fieldID, String fieldName, FieldReader reader, List<JdbcWriterField> fields) {
      super(fieldID, fieldName, reader);
      fields.add(new JdbcWriterField(fieldName, MinorType.DATE, DataMode.REQUIRED));
    }

    @Override
    public void writeField() {
      reader.read(holder);
      rowList.add(holder.value);
    }
  }

  @Override
  public FieldConverter getNewNullableTimeConverter(int fieldId, String fieldName, FieldReader reader) {
      return new NullableTimeJDBCConverter(fieldId, fieldName, reader, fields);
    }

    public class NullableTimeJDBCConverter extends FieldConverter {
    private final NullableTimeHolder holder = new NullableTimeHolder();

    public NullableTimeJDBCConverter(int fieldID, String fieldName, FieldReader reader, List<JdbcWriterField> fields) {
      super(fieldID, fieldName, reader);
      fields.add(new JdbcWriterField(fieldName, MinorType.TIME, DataMode.OPTIONAL));
    }

    @Override
    public void writeField() {
      if (!reader.isSet()) {
        rowList.add("null");
        return;
      }
      reader.read(holder);
      rowList.add(holder.value);
    }
  }

  @Override
  public FieldConverter getNewTimeConverter(int fieldId, String fieldName, FieldReader reader) {
    return new TimeJDBCConverter(fieldId, fieldName, reader, fields);
  }

  public class TimeJDBCConverter extends FieldConverter {
    private final TimeHolder holder = new TimeHolder();

    public TimeJDBCConverter(int fieldID, String fieldName, FieldReader reader, List<JdbcWriterField> fields) {
      super(fieldID, fieldName, reader);
      fields.add(new JdbcWriterField(fieldName, MinorType.TIME, DataMode.REQUIRED));
    }

    @Override
    public void writeField() {
      reader.read(holder);
      rowList.add(holder.value);
    }
  }

  @Override
  public FieldConverter getNewNullableTimeStampConverter(int fieldId, String fieldName, FieldReader reader) {
    return new NullableTimeStampJDBCConverter(fieldId, fieldName, reader, fields);
  }

  public class NullableTimeStampJDBCConverter extends FieldConverter {
    private final NullableTimeStampHolder holder = new NullableTimeStampHolder();

    public NullableTimeStampJDBCConverter(int fieldID, String fieldName, FieldReader reader, List<JdbcWriterField> fields) {
      super(fieldID, fieldName, reader);
      fields.add(new JdbcWriterField(fieldName, MinorType.TIMESTAMP, DataMode.OPTIONAL));
    }

    @Override
    public void writeField() {
      if (!reader.isSet()) {
        rowList.add("null");
        return;
      }
      reader.read(holder);
      rowList.add(holder.value);
    }
  }

  @Override
  public FieldConverter getNewTimeStampConverter(int fieldId, String fieldName, FieldReader reader) {
    return new TimeStampJDBCConverter(fieldId, fieldName, reader, fields);
  }

  public class TimeStampJDBCConverter extends FieldConverter {
    private final TimeStampHolder holder = new TimeStampHolder();

    public TimeStampJDBCConverter(int fieldID, String fieldName, FieldReader reader, List<JdbcWriterField> fields) {
      super(fieldID, fieldName, reader);
      fields.add(new JdbcWriterField(fieldName, MinorType.TIMESTAMP, DataMode.REQUIRED));
    }

    @Override
    public void writeField() {
      reader.read(holder);
      rowList.add(holder.value);
    }
  }

  @Override
  public FieldConverter getNewNullableBitConverter(int fieldId, String fieldName, FieldReader reader) {
    return new NullableBitJDBCConverter(fieldId, fieldName, reader, fields);
  }

  public class NullableBitJDBCConverter extends FieldConverter {
    private final NullableBitHolder holder = new NullableBitHolder();

    public NullableBitJDBCConverter(int fieldID, String fieldName, FieldReader reader, List<JdbcWriterField> fields) {
      super(fieldID, fieldName, reader);
      fields.add(new JdbcWriterField(fieldName, MinorType.BIT, DataMode.OPTIONAL));
    }

    @Override
    public void writeField() {
      if (!reader.isSet()) {
        rowList.add("null");
        return;
      }
      reader.read(holder);
      String booleanValue = "false";
      if (holder.value == 1) {
        booleanValue = "true";
      }
      rowList.add(booleanValue);
    }
  }
  @Override
  public FieldConverter getNewBitConverter(int fieldId, String fieldName, FieldReader reader) {
    return new BitJDBCConverter(fieldId, fieldName, reader, fields);
  }

  public class BitJDBCConverter extends FieldConverter {
    private final BitHolder holder = new BitHolder();

    public BitJDBCConverter(int fieldID, String fieldName, FieldReader reader, List<JdbcWriterField> fields) {
      super(fieldID, fieldName, reader);
      fields.add(new JdbcWriterField(fieldName, MinorType.BIT, DataMode.REQUIRED));
    }

    @Override
    public void writeField() {
      reader.read(holder);
      String booleanValue = "false";
      if (holder.value == 1) {
        booleanValue = "true";
      }
      rowList.add(booleanValue);
    }
  }
}
