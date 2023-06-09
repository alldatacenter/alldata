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

import org.apache.drill.common.AutoCloseables;
import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedReader;
import org.apache.drill.exec.physical.impl.scan.framework.SchemaNegotiator;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.jdbc.writers.JdbcBigintWriter;
import org.apache.drill.exec.store.jdbc.writers.JdbcBitWriter;
import org.apache.drill.exec.store.jdbc.writers.JdbcColumnWriter;
import org.apache.drill.exec.store.jdbc.writers.JdbcDateWriter;
import org.apache.drill.exec.store.jdbc.writers.JdbcDoubleWriter;
import org.apache.drill.exec.store.jdbc.writers.JdbcFloatWriter;
import org.apache.drill.exec.store.jdbc.writers.JdbcIntWriter;
import org.apache.drill.exec.store.jdbc.writers.JdbcTimeWriter;
import org.apache.drill.exec.store.jdbc.writers.JdbcTimestampWriter;
import org.apache.drill.exec.store.jdbc.writers.JdbcVarbinaryWriter;
import org.apache.drill.exec.store.jdbc.writers.JdbcVarcharWriter;
import org.apache.drill.exec.store.jdbc.writers.JdbcVardecimalWriter;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.apache.drill.exec.planner.types.DrillRelDataTypeSystem.DRILL_REL_DATATYPE_SYSTEM;

public class JdbcBatchReader implements ManagedReader<SchemaNegotiator> {

  private static final Logger logger = LoggerFactory.getLogger(JdbcBatchReader.class);
  private static final ImmutableMap<Integer, MinorType> JDBC_TYPE_MAPPINGS;
  private final DataSource source;
  private final String sql;
  private final List<SchemaPath> columns;
  private Connection connection;
  private PreparedStatement statement;
  private ResultSet resultSet;
  private RowSetLoader rowWriter;
  private CustomErrorContext errorContext;
  private List<JdbcColumnWriter> columnWriters;
  private List<JdbcColumn> jdbcColumns;


  public JdbcBatchReader(DataSource source, String sql, List<SchemaPath> columns) {
    this.source = source;
    this.sql = sql;
    this.columns = columns;
  }

  /*
   * This map maps JDBC data types to their Drill equivalents.  The basic strategy is that if there
   * is a Drill equivalent, then do the mapping as expected.  All flavors of INT (SMALLINT, TINYINT etc)
   * are mapped to INT in Drill, with the exception of BIGINT.
   *
   * All flavors of character fields are mapped to VARCHAR in Drill. All versions of binary fields are
   * mapped to VARBINARY.
   *
   */
  static {
    JDBC_TYPE_MAPPINGS = ImmutableMap.<Integer, MinorType>builder()
      .put(java.sql.Types.DOUBLE, MinorType.FLOAT8)
      .put(java.sql.Types.FLOAT, MinorType.FLOAT4)
      .put(java.sql.Types.TINYINT, MinorType.INT)
      .put(java.sql.Types.SMALLINT, MinorType.INT)
      .put(java.sql.Types.INTEGER, MinorType.INT)
      .put(java.sql.Types.BIGINT, MinorType.BIGINT)

      .put(java.sql.Types.CHAR, MinorType.VARCHAR)
      .put(java.sql.Types.VARCHAR, MinorType.VARCHAR)
      .put(java.sql.Types.LONGVARCHAR, MinorType.VARCHAR)
      .put(java.sql.Types.CLOB, MinorType.VARCHAR)

      .put(java.sql.Types.NCHAR, MinorType.VARCHAR)
      .put(java.sql.Types.NVARCHAR, MinorType.VARCHAR)
      .put(java.sql.Types.LONGNVARCHAR, MinorType.VARCHAR)

      .put(java.sql.Types.VARBINARY, MinorType.VARBINARY)
      .put(java.sql.Types.LONGVARBINARY, MinorType.VARBINARY)
      .put(java.sql.Types.BLOB, MinorType.VARBINARY)

      .put(java.sql.Types.NUMERIC, MinorType.FLOAT8)
      .put(java.sql.Types.DECIMAL, MinorType.VARDECIMAL)
      .put(java.sql.Types.REAL, MinorType.FLOAT8)

      .put(java.sql.Types.DATE, MinorType.DATE)
      .put(java.sql.Types.TIME, MinorType.TIME)
      .put(java.sql.Types.TIMESTAMP, MinorType.TIMESTAMP)

      .put(java.sql.Types.BOOLEAN, MinorType.BIT)
      .put(java.sql.Types.BIT, MinorType.BIT)

      .build();
  }

  @Override
  public boolean open(SchemaNegotiator negotiator) {

    this.errorContext = negotiator.parentErrorContext();
    try {
      connection = source.getConnection();
      statement = connection.prepareStatement(sql);
      resultSet = statement.executeQuery();

      TupleMetadata drillSchema = buildSchema();
      negotiator.tableSchema(drillSchema, true);
      ResultSetLoader resultSetLoader = negotiator.build();

      // Create ScalarWriters
      rowWriter = resultSetLoader.writer();
      populateWriterArray();

    } catch (SQLException e) {
      throw UserException.dataReadError(e)
        .message("The JDBC storage plugin failed while trying setup the SQL query. ")
        .addContext("Sql", sql)
        .addContext(errorContext)
        .build(logger);
    }

    return true;
  }

  @Override
  public boolean next() {
    while (!rowWriter.isFull()) {
      if (!processRow()) {
        return false;
      }
    }
    return true;
  }

  private boolean processRow() {
    try {
      if (!resultSet.next()) {
        return false;
      }
      rowWriter.start();
      // Process results
      for (JdbcColumnWriter writer : columnWriters) {
        writer.load(resultSet);
      }
      rowWriter.save();
    } catch (SQLException e) {
      throw UserException
        .dataReadError(e)
        .message("Failure while attempting to read from database.")
        .addContext("Sql", sql)
        .addContext(errorContext)
        .build(logger);
    }

    return true;
  }

  @Override
  public void close() {
    AutoCloseables.closeSilently(resultSet, statement, connection);
  }

  private TupleMetadata buildSchema() throws SQLException {
    SchemaBuilder builder = new SchemaBuilder();
    ResultSetMetaData meta = resultSet.getMetaData();
    jdbcColumns = new ArrayList<>();

    int columnsCount = meta.getColumnCount();

    if (columns.size() != columnsCount) {
      throw UserException
        .validationError()
        .message(
          "Expected columns count differs from the returned one.\n" +
            "Expected columns: %s\n" +
            "Returned columns count: %s",
          columns, columnsCount)
        .addContext("Sql", sql)
        .addContext(errorContext)
        .build(logger);
    }

    for (int i = 1; i <= columnsCount; i++) {
      String name = columns.get(i - 1).getRootSegmentPath();
      // column index in ResultSetMetaData starts from 1
      int jdbcType = meta.getColumnType(i);
      int width = Math.min(meta.getPrecision(i), DRILL_REL_DATATYPE_SYSTEM.getMaxNumericPrecision());

      // Note, if both the precision and scale are not defined in the query, Drill defaults to 38 for both
      // Which causes an overflow exception.  We reduce the scale by one here to avoid this.  The better solution
      // would be for the user to provide the precision and scale.
      int scale = Math.min(meta.getScale(i), DRILL_REL_DATATYPE_SYSTEM.getMaxNumericScale() - 1);

      MinorType minorType = JDBC_TYPE_MAPPINGS.get(jdbcType);
      if (minorType == null) {
        logger.warn("Ignoring column that is unsupported.", UserException
          .unsupportedError()
          .message(
            "A column you queried has a data type that is not currently supported by the JDBC storage plugin. "
              + "The column's name was %s and its JDBC data type was %s. ",
            name,
            nameFromType(jdbcType))
          .addContext("Sql", sql)
          .addContext("Column Name", name)
          .addContext(errorContext)
          .build(logger));
        continue;
      }

      jdbcColumns.add(new JdbcColumn(name, minorType, i, scale, width));
      // Precision and scale are passed for all readers whether they are needed or not.
      builder.addNullable(name, minorType, width, scale);
    }

    return builder.buildSchema();
  }

  private void populateWriterArray() {
    columnWriters = new ArrayList<>();

    for (JdbcColumn col : jdbcColumns) {
      switch (col.type) {
        case VARCHAR:
          columnWriters.add(new JdbcVarcharWriter(col.colName, rowWriter, col.colPosition));
          break;
        case FLOAT4:
          columnWriters.add(new JdbcFloatWriter(col.colName, rowWriter, col.colPosition));
          break;
        case FLOAT8:
          columnWriters.add(new JdbcDoubleWriter(col.colName, rowWriter, col.colPosition));
          break;
        case INT:
          columnWriters.add(new JdbcIntWriter(col.colName, rowWriter, col.colPosition));
          break;
        case BIGINT:
          columnWriters.add(new JdbcBigintWriter(col.colName, rowWriter, col.colPosition));
          break;
        case DATE:
          columnWriters.add(new JdbcDateWriter(col.colName, rowWriter, col.colPosition));
          break;
        case TIME:
          columnWriters.add(new JdbcTimeWriter(col.colName, rowWriter, col.colPosition));
          break;
        case TIMESTAMP:
          columnWriters.add(new JdbcTimestampWriter(col.colName, rowWriter, col.colPosition));
          break;
        case VARBINARY:
          columnWriters.add(new JdbcVarbinaryWriter(col.colName, rowWriter, col.colPosition));
          break;
        case BIT:
          columnWriters.add(new JdbcBitWriter(col.colName, rowWriter, col.colPosition));
          break;
        case VARDECIMAL:
          columnWriters.add(new JdbcVardecimalWriter(col.colName, rowWriter, col.colPosition, col.scale, col.precision));
          break;
        default:
          logger.warn("Unsupported data type {} found at column {}", col.type.getDescriptorForType(), col.colName);
      }
    }
  }

  private static String nameFromType(int javaSqlType) {
    try {
      for (Field f : java.sql.Types.class.getFields()) {
        if (java.lang.reflect.Modifier.isStatic(f.getModifiers()) &&
          f.getType() == int.class &&
          f.getInt(null) == javaSqlType) {
          return f.getName();
        }
      }
    } catch (IllegalArgumentException | IllegalAccessException e) {
      logger.debug("Unable to SQL type {} into String: {}", javaSqlType, e.getMessage());
    }

    return Integer.toString(javaSqlType);
  }

  public static class JdbcColumn {
    final String colName;
    final MinorType type;
    final int colPosition;
    final int scale;
    final int precision;

    public JdbcColumn (String colName, MinorType type, int colPosition, int scale, int precision) {
      this.colName = colName;
      this.type = type;
      this.colPosition = colPosition;
      this.scale = scale;
      this.precision = precision;
    }
  }
}
