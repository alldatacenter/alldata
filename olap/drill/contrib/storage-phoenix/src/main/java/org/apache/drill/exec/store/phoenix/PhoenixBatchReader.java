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
package org.apache.drill.exec.store.phoenix;

import java.security.PrivilegedAction;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.drill.common.AutoCloseables;
import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedReader;
import org.apache.drill.exec.physical.impl.scan.framework.SchemaNegotiator;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.phoenix.PhoenixReader.ArrayBigintDefn;
import org.apache.drill.exec.store.phoenix.PhoenixReader.ArrayBooleanDefn;
import org.apache.drill.exec.store.phoenix.PhoenixReader.ArrayDefn;
import org.apache.drill.exec.store.phoenix.PhoenixReader.ArrayDoubleDefn;
import org.apache.drill.exec.store.phoenix.PhoenixReader.ArrayIntegerDefn;
import org.apache.drill.exec.store.phoenix.PhoenixReader.ArraySmallintDefn;
import org.apache.drill.exec.store.phoenix.PhoenixReader.ArrayTinyintDefn;
import org.apache.drill.exec.store.phoenix.PhoenixReader.ArrayVarcharDefn;
import org.apache.drill.exec.store.phoenix.PhoenixReader.ColumnDefn;
import org.apache.drill.exec.store.phoenix.PhoenixReader.GenericDateDefn;
import org.apache.drill.exec.store.phoenix.PhoenixReader.GenericDefn;
import org.apache.drill.exec.store.phoenix.PhoenixReader.GenericTimeDefn;
import org.apache.drill.exec.store.phoenix.PhoenixReader.GenericTimestampDefn;
import org.apache.drill.exec.util.ImpersonationUtil;
import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;


public class PhoenixBatchReader implements ManagedReader<SchemaNegotiator> {

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(PhoenixBatchReader.class);

  private final PhoenixSubScan subScan;
  private final boolean impersonationEnabled;
  private final UserGroupInformation ugi = ImpersonationUtil.getProcessUserUGI();
  private CustomErrorContext errorContext;
  private PhoenixReader reader;
  private PreparedStatement pstmt;
  private ResultSet results;
  private ResultSetMetaData meta;
  private ColumnDefn[] columns;
  private Stopwatch watch;

  public PhoenixBatchReader(PhoenixSubScan subScan) {
    this.subScan = subScan;
    this.impersonationEnabled = subScan.getPlugin().getContext().getConfig().getBoolean(ExecConstants.IMPERSONATION_ENABLED);
  }

  @Override
  public boolean open(SchemaNegotiator negotiator) {
    return impersonationEnabled
      ? ugi.doAs((PrivilegedAction<Boolean>) () -> processOpen(negotiator))
      : processOpen(negotiator);
  }

  private boolean processOpen(SchemaNegotiator negotiator) {
    try {
      errorContext = negotiator.parentErrorContext();
      DataSource ds = subScan.getPlugin().getDataSource(negotiator.userName());
      pstmt = ds.getConnection().prepareStatement(subScan.getSql());
      results = pstmt.executeQuery();
      meta = pstmt.getMetaData();
    } catch (SQLException e) {
      throw UserException
        .dataReadError(e)
        .message("Failed to execute the phoenix sql query. " + e.getMessage())
        .addContext(errorContext)
        .build(logger);
    }
    try {
      negotiator.tableSchema(defineMetadata(), true);
      reader = new PhoenixReader(negotiator.build(), columns, results);
      bindColumns(reader.getStorage());
    } catch (SQLException e) {
      throw UserException
        .dataReadError(e)
        .message("Failed to get type of columns from metadata. " + e.getMessage())
        .addContext(errorContext)
        .build(logger);
    }
    watch = Stopwatch.createStarted();
    return true;
  }

  @Override
  public boolean next() {
    return impersonationEnabled
      ? ugi.doAs((PrivilegedAction<Boolean>) this::processNext)
      : processNext();
  }

  private boolean processNext() {
    try {
      while (!reader.getStorage().isFull()) {
        if (!reader.processRow()) { // return true if one row is processed.
          watch.stop();
          logger.debug("Phoenix fetch total record numbers : {}", reader.getRowCount());
          return false; // the EOF is reached.
        }
      }
      return true; // batch full but not reached the EOF.
    } catch (SQLException e) {
      throw UserException
        .dataReadError(e)
        .message("Failed to get the data from the result set. " + e.getMessage())
        .addContext(errorContext)
        .build(logger);
    }
  }

  @Override
  public void close() {
    logger.debug("Phoenix fetch batch size : {}, took {} ms. ", reader.getBatchCount(), watch.elapsed(TimeUnit.MILLISECONDS));
    AutoCloseables.closeSilently(results, pstmt, reader);
  }

  private TupleMetadata defineMetadata() throws SQLException {
    List<SchemaPath> cols = subScan.getColumns();
    columns = new ColumnDefn[cols.size()];
    SchemaBuilder builder = new SchemaBuilder();
    for (int index = 0; index < cols.size(); index++) {
      int columnIndex = index + 1; // column the first column is 1
      int sqlType = meta.getColumnType(columnIndex);
      String columnName = cols.get(index).rootName();
      columns[index] = makeColumn(columnName, sqlType, meta.getColumnTypeName(columnIndex), columnIndex);
      columns[index].define(builder);
    }
    return builder.buildSchema();
  }

  private ColumnDefn makeColumn(String name, int sqlType, String baseType, int index) {
    if (sqlType == Types.ARRAY) { // supports the array data type
      if (baseType.equals(ArrayDefn.VARCHAR) || baseType.equals(ArrayDefn.CHAR)) {
        return new ArrayVarcharDefn(name, index, sqlType, baseType);
      } else if (baseType.equals(ArrayDefn.BIGINT)) {
        return new ArrayBigintDefn(name, index, sqlType, baseType);
      } else if (baseType.equals(ArrayDefn.INTEGER)) {
        return new ArrayIntegerDefn(name, index, sqlType, baseType);
      } else if (baseType.equals(ArrayDefn.SMALLINT)) {
        return new ArraySmallintDefn(name, index, sqlType, baseType);
      } else if (baseType.equals(ArrayDefn.TINYINT)) {
        return new ArrayTinyintDefn(name, index, sqlType, baseType);
      } else if (baseType.equals(ArrayDefn.DOUBLE) || baseType.equals(ArrayDefn.FLOAT)) {
        return new ArrayDoubleDefn(name, index, sqlType, baseType);
      } else if (baseType.equals(ArrayDefn.BOOLEAN)) {
        return new ArrayBooleanDefn(name, index, sqlType, baseType);
      } else {
        throw UserException.dataReadError()
        .message("The Phoenix reader does not support this data type : " + baseType)
        .addContext(errorContext)
        .build(logger);
      }
    }
    // supports the generic data type
    if (sqlType == Types.DATE) {
      return new GenericDateDefn(name, index, sqlType);
    } else if (sqlType == Types.TIME) {
      return new GenericTimeDefn(name, index, sqlType);
    } else if (sqlType == Types.TIMESTAMP) {
      return new GenericTimestampDefn(name, index, sqlType);
    } else if (sqlType == Types.VARCHAR
        || sqlType == Types.CHAR
        || sqlType == Types.BIGINT
        || sqlType == Types.INTEGER
        || sqlType == Types.SMALLINT
        || sqlType == Types.TINYINT
        || sqlType == Types.DOUBLE
        || sqlType == Types.FLOAT
        || sqlType == Types.DECIMAL
        || sqlType == Types.BINARY
        || sqlType == Types.VARBINARY
        || sqlType == Types.BOOLEAN) {
      return new GenericDefn(name, index, sqlType);
    } else {
      throw UserException.dataReadError()
      .message("The Phoenix reader does not support this data type : java.sql.Types : " + sqlType)
      .addContext(errorContext)
      .build(logger);
    }
  }

  private void bindColumns(RowSetLoader loader) {
    for (int i = 0; i < columns.length; i++) {
      columns[i].bind(loader);
    }
  }
}
