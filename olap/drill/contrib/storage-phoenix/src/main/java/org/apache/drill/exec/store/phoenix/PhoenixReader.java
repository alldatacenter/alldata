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

import java.sql.Array;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.vector.accessor.ColumnWriter;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.exec.vector.accessor.writer.ScalarArrayWriter;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;

public class PhoenixReader implements AutoCloseable {

  private final RowSetLoader writer;
  private final ColumnDefn[] columns;
  private final ResultSet results;

  public PhoenixReader(ResultSetLoader loader, ColumnDefn[] columns, ResultSet results) {
    this.writer = loader.writer();
    this.columns = columns;
    this.results = results;
  }

  public RowSetLoader getStorage() {
    return writer;
  }

  public long getRowCount() {
    return writer.loader().totalRowCount();
  }

  public int getBatchCount() {
    return writer.loader().batchCount();
  }

  /**
   * Fetch and process one row.
   * @return return true if one row is processed, return false if there is no next row.
   * @throws SQLException
   */
  public boolean processRow() throws SQLException {
    if (results.next()) {
      writer.start();
      for (ColumnDefn columnDefn : columns) {
        columnDefn.load(results);
      }
      writer.save();
      return true;
    }
    return false;
  }

  protected static final Map<Integer, MinorType> COLUMN_TYPE_MAP = Maps.newHashMap();

  static {
    // text
    COLUMN_TYPE_MAP.put(Types.VARCHAR, MinorType.VARCHAR);
    COLUMN_TYPE_MAP.put(Types.CHAR, MinorType.VARCHAR);
    // numbers
    COLUMN_TYPE_MAP.put(Types.BIGINT, MinorType.BIGINT);
    COLUMN_TYPE_MAP.put(Types.INTEGER, MinorType.INT);
    COLUMN_TYPE_MAP.put(Types.SMALLINT, MinorType.INT);
    COLUMN_TYPE_MAP.put(Types.TINYINT, MinorType.INT);
    COLUMN_TYPE_MAP.put(Types.DOUBLE, MinorType.FLOAT8);
    COLUMN_TYPE_MAP.put(Types.FLOAT, MinorType.FLOAT4);
    COLUMN_TYPE_MAP.put(Types.DECIMAL, MinorType.VARDECIMAL);
    // time
    COLUMN_TYPE_MAP.put(Types.DATE, MinorType.DATE);
    COLUMN_TYPE_MAP.put(Types.TIME, MinorType.TIME);
    COLUMN_TYPE_MAP.put(Types.TIMESTAMP, MinorType.TIMESTAMP);
    // binary
    COLUMN_TYPE_MAP.put(Types.BINARY, MinorType.VARBINARY); // Raw fixed length byte array. Mapped to byte[].
    COLUMN_TYPE_MAP.put(Types.VARBINARY, MinorType.VARBINARY); // Raw variable length byte array.
    // boolean
    COLUMN_TYPE_MAP.put(Types.BOOLEAN, MinorType.BIT);
  }

  @Override
  public void close() throws Exception {
    results.close();
  }

  protected abstract static class ColumnDefn {

    final String name;
    final int index;
    final int sqlType;
    ColumnWriter writer;

    public String getName() {
      return name;
    }

    public int getIndex() {
      return index;
    }

    public int getSqlType() {
      return sqlType;
    }

    public ColumnDefn(String name, int index, int sqlType) {
      this.name = name;
      this.index = index;
      this.sqlType = sqlType;
    }

    public void define(SchemaBuilder builder) {
      builder.addNullable(getName(), COLUMN_TYPE_MAP.get(getSqlType()));
    }

    public void bind(RowSetLoader loader) {
      writer = loader.scalar(getName());
    }

    public abstract void load(ResultSet row) throws SQLException;
  }

  protected static class GenericDefn extends ColumnDefn {

    public GenericDefn(String name, int index, int sqlType) {
      super(name, index, sqlType);
    }

    @Override
    public void load(ResultSet row) throws SQLException {
      Object value = row.getObject(index);
      if (value != null) {
        writer.setObject(value);
      }
    }
  }

  protected static class GenericDateDefn extends GenericDefn {

    public GenericDateDefn(String name, int index, int sqlType) {
      super(name, index, sqlType);
    }

    @Override
    public void load(ResultSet row) throws SQLException {
      Object value = row.getObject(index);
      if (value != null) {
        LocalDate date = ((Date) value).toLocalDate(); // java.sql.Date
        ((ScalarWriter) writer).setDate(date);
      }
    }
  }

  protected static class GenericTimeDefn extends GenericDefn {

    public GenericTimeDefn(String name, int index, int sqlType) {
      super(name, index, sqlType);
    }

    @Override
    public void load(ResultSet row) throws SQLException {
      Object value = row.getObject(index);
      if (value != null) {
        LocalTime time = ((Time) value).toLocalTime(); // java.sql.Time
        ((ScalarWriter) writer).setTime(time);
      }
    }
  }

  protected static class GenericTimestampDefn extends GenericDefn {

    public GenericTimestampDefn(String name, int index, int sqlType) {
      super(name, index, sqlType);
    }

    @Override
    public void load(ResultSet row) throws SQLException {
      Object value = row.getObject(index);
      if (value != null) {
        Instant ts = ((Timestamp) value).toInstant(); // java.sql.Timestamp
        ((ScalarWriter) writer).setTimestamp(ts);
      }
    }
  }

  protected static abstract class ArrayDefn extends ColumnDefn {

    static final String VARCHAR = "VARCHAR ARRAY";
    static final String CHAR = "CHAR ARRAY";
    static final String BIGINT = "BIGINT ARRAY";
    static final String INTEGER = "INTEGER ARRAY";
    static final String DOUBLE = "DOUBLE ARRAY";
    static final String FLOAT = "FLOAT ARRAY";
    static final String SMALLINT = "SMALLINT ARRAY";
    static final String TINYINT = "TINYINT ARRAY";
    static final String BOOLEAN = "BOOLEAN ARRAY";

    final String baseType;

    public ArrayDefn(String name, int index, int sqlType, String baseType) {
      super(name, index, sqlType);
      this.baseType = baseType;
    }

    @Override
    public void bind(RowSetLoader loader) {
      writer = loader.array(getName());
    }

    @Override
    public void load(ResultSet row) throws SQLException {
      Array array = row.getArray(index);
      if (array != null && array.getArray() != null) {
        Object[] values = (Object[]) array.getArray();
        ((ScalarArrayWriter) writer).setObjectArray(values);
      }
    }
  }

  protected static class ArrayVarcharDefn extends ArrayDefn {

    public ArrayVarcharDefn(String name, int index, int sqlType, String baseType) {
      super(name, index, sqlType, baseType);
    }

    @Override
    public void define(SchemaBuilder builder) {
      builder.addArray(getName(), MinorType.VARCHAR);
    }
  }

  protected static class ArrayBigintDefn extends ArrayDefn {

    public ArrayBigintDefn(String name, int index, int sqlType, String baseType) {
      super(name, index, sqlType, baseType);
    }

    @Override
    public void define(SchemaBuilder builder) {
      builder.addArray(getName(), MinorType.BIGINT);
    }
  }

  protected static class ArrayIntegerDefn extends ArrayDefn {

    public ArrayIntegerDefn(String name, int index, int sqlType, String baseType) {
      super(name, index, sqlType, baseType);
    }

    @Override
    public void define(SchemaBuilder builder) {
      builder.addArray(getName(), MinorType.INT);
    }
  }

  protected static class ArraySmallintDefn extends ArrayDefn {

    public ArraySmallintDefn(String name, int index, int sqlType, String baseType) {
      super(name, index, sqlType, baseType);
    }

    @Override
    public void define(SchemaBuilder builder) {
      builder.addArray(getName(), MinorType.SMALLINT);
    }
  }

  protected static class ArrayTinyintDefn extends ArrayDefn {

    public ArrayTinyintDefn(String name, int index, int sqlType, String baseType) {
      super(name, index, sqlType, baseType);
    }

    @Override
    public void define(SchemaBuilder builder) {
      builder.addArray(getName(), MinorType.TINYINT);
    }
  }

  protected static class ArrayDoubleDefn extends ArrayDefn {

    public ArrayDoubleDefn(String name, int index, int sqlType, String baseType) {
      super(name, index, sqlType, baseType);
    }

    @Override
    public void define(SchemaBuilder builder) {
      builder.addArray(getName(), MinorType.FLOAT8);
    }
  }

  protected static class ArrayBooleanDefn extends ArrayDefn {

    public ArrayBooleanDefn(String name, int index, int sqlType, String baseType) {
      super(name, index, sqlType, baseType);
    }

    @Override
    public void define(SchemaBuilder builder) {
      builder.addArray(getName(), MinorType.BIT);
    }
  }
}
