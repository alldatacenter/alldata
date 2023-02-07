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
package org.apache.drill.exec.store.ischema;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.CATS_COL_CATALOG_CONNECT;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.CATS_COL_CATALOG_DESCRIPTION;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.CATS_COL_CATALOG_NAME;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.COLS_COL_CHARACTER_MAXIMUM_LENGTH;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.COLS_COL_CHARACTER_OCTET_LENGTH;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.COLS_COL_COLUMN_DEFAULT;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.COLS_COL_COLUMN_FORMAT;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.COLS_COL_COLUMN_NAME;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.COLS_COL_COLUMN_SIZE;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.COLS_COL_DATA_TYPE;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.COLS_COL_DATETIME_PRECISION;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.COLS_COL_EST_NUM_NON_NULLS;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.COLS_COL_INTERVAL_PRECISION;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.COLS_COL_INTERVAL_TYPE;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.COLS_COL_IS_NULLABLE;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.COLS_COL_MAX_VAL;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.COLS_COL_MIN_VAL;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.COLS_COL_NDV;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.COLS_COL_NUMERIC_PRECISION;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.COLS_COL_NUMERIC_PRECISION_RADIX;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.COLS_COL_NUMERIC_SCALE;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.COLS_COL_NUM_NULLS;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.COLS_COL_ORDINAL_POSITION;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.COLS_COL_IS_NESTED;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.FILES_COL_ACCESS_TIME;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.FILES_COL_FILE_NAME;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.FILES_COL_GROUP;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.FILES_COL_IS_DIRECTORY;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.FILES_COL_IS_FILE;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.FILES_COL_LENGTH;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.FILES_COL_MODIFICATION_TIME;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.FILES_COL_OWNER;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.FILES_COL_PERMISSION;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.FILES_COL_RELATIVE_PATH;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.FILES_COL_ROOT_SCHEMA_NAME;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.FILES_COL_SCHEMA_NAME;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.FILES_COL_WORKSPACE_NAME;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.PARTITIONS_COL_LAST_MODIFIED_TIME;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.PARTITIONS_COL_LOCATION;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.PARTITIONS_COL_METADATA_IDENTIFIER;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.PARTITIONS_COL_METADATA_KEY;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.PARTITIONS_COL_METADATA_TYPE;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.PARTITIONS_COL_PARTITION_COLUMN;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.PARTITIONS_COL_PARTITION_VALUE;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.SCHS_COL_CATALOG_NAME;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.SCHS_COL_IS_MUTABLE;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.SCHS_COL_SCHEMA_NAME;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.SCHS_COL_SCHEMA_OWNER;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.SCHS_COL_TYPE;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.SHRD_COL_TABLE_CATALOG;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.SHRD_COL_TABLE_NAME;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.SHRD_COL_TABLE_SCHEMA;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.TBLS_COL_LAST_MODIFIED_TIME;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.TBLS_COL_LOCATION;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.TBLS_COL_NUM_ROWS;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.TBLS_COL_TABLE_SOURCE;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.TBLS_COL_TABLE_TYPE;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.VIEWS_COL_VIEW_DEFINITION;

/**
 * Base class for tables in INFORMATION_SCHEMA. Defines the table (fields and types).
 */
public abstract class InfoSchemaTable<S> {

  public static final MajorType INT = Types.required(MinorType.INT);
  public static final MajorType BIGINT = Types.required(MinorType.BIGINT);
  public static final MajorType VARCHAR = Types.required(MinorType.VARCHAR);
  public static final MajorType BIT = Types.required(MinorType.BIT);
  public static final MajorType TIMESTAMP = Types.required(MinorType.TIMESTAMP);
  public static final MajorType FLOAT8 = Types.required(MinorType.FLOAT8);

  private final List<Field> fields;

  public InfoSchemaTable(List<Field> fields) {
    this.fields = fields;
  }

  public RelDataType getRowType(RelDataTypeFactory typeFactory) {
    // Convert the list of Drill types to an list of Optiq types
    List<RelDataType> relTypes = new ArrayList<>();
    List<String> fieldNames = new ArrayList<>();
    for (Field field : fields) {
      relTypes.add(getRelDataType(typeFactory, field.getType()));
      fieldNames.add(field.getName());
    }

    return typeFactory.createStructType(relTypes, fieldNames);
  }

  private RelDataType getRelDataType(RelDataTypeFactory typeFactory, MajorType type) {
    switch (type.getMinorType()) {
      case INT:
        return typeFactory.createSqlType(SqlTypeName.INTEGER);
      case BIGINT:
        return typeFactory.createSqlType(SqlTypeName.BIGINT);
      case VARCHAR:
        // Note:  Remember to not default to "VARCHAR(1)":
        return typeFactory.createSqlType(SqlTypeName.VARCHAR, Integer.MAX_VALUE);
      case BIT:
        return typeFactory.createSqlType(SqlTypeName.BOOLEAN);
      case TIMESTAMP:
        return typeFactory.createSqlType(SqlTypeName.TIMESTAMP);
      case FLOAT8:
        return typeFactory.createSqlType(SqlTypeName.FLOAT);
      default:
        throw new UnsupportedOperationException("Only INT, BIGINT, VARCHAR, BOOLEAN, TIMESTAMP and DOUBLE types are supported in " +
            InfoSchemaConstants.IS_SCHEMA_NAME);
    }
  }

  public abstract InfoSchemaRecordGenerator<S> getRecordGenerator(FilterEvaluator filterEvaluator);

  /**
   * Layout for the CATALOGS table.
   */
  public static class Catalogs extends InfoSchemaTable<Records.Catalog> {

    private static final List<Field> fields = Arrays.asList(
      Field.create(CATS_COL_CATALOG_NAME, VARCHAR),
      Field.create(CATS_COL_CATALOG_DESCRIPTION, VARCHAR),
      Field.create(CATS_COL_CATALOG_CONNECT, VARCHAR));

    public Catalogs() {
      super(fields);
    }

    @Override
    public InfoSchemaRecordGenerator<Records.Catalog> getRecordGenerator(FilterEvaluator filterEvaluator) {
      return new InfoSchemaRecordGenerator.Catalogs(filterEvaluator);
    }
  }

  /**
   * Layout for the SCHEMATA table.
   */
  public static class Schemata extends InfoSchemaTable<Records.Schema> {

    private static final List<Field> fields = Arrays.asList(
      Field.create(SCHS_COL_CATALOG_NAME, VARCHAR),
      Field.create(SCHS_COL_SCHEMA_NAME, VARCHAR),
      Field.create(SCHS_COL_SCHEMA_OWNER, VARCHAR),
      Field.create(SCHS_COL_TYPE, VARCHAR),
      Field.create(SCHS_COL_IS_MUTABLE, VARCHAR));

    public Schemata() {
      super(fields);
    }

    @Override
    public InfoSchemaRecordGenerator<Records.Schema> getRecordGenerator(FilterEvaluator filterEvaluator) {
      return new InfoSchemaRecordGenerator.Schemata(filterEvaluator);
    }
  }

  /**
   * Layout for the TABLES table.
   */
  public static class Tables extends InfoSchemaTable<Records.Table> {

    private static final List<Field> fields = Arrays.asList(
      Field.create(SHRD_COL_TABLE_CATALOG, VARCHAR),
      Field.create(SHRD_COL_TABLE_SCHEMA, VARCHAR),
      Field.create(SHRD_COL_TABLE_NAME, VARCHAR),
      Field.create(TBLS_COL_TABLE_TYPE, VARCHAR),
      Field.create(TBLS_COL_TABLE_SOURCE, VARCHAR),
      Field.create(TBLS_COL_LOCATION, VARCHAR),
      Field.create(TBLS_COL_NUM_ROWS, BIGINT),
      Field.create(TBLS_COL_LAST_MODIFIED_TIME, TIMESTAMP));

    public Tables() {
      super(fields);
    }

    @Override
    public InfoSchemaRecordGenerator<Records.Table> getRecordGenerator(FilterEvaluator filterEvaluator) {
      return new InfoSchemaRecordGenerator.Tables(filterEvaluator);
    }
  }

  /**
   * Layout for the VIEWS table.
   */
  public static class Views extends InfoSchemaTable<Records.View> {

    private static final List<Field> fields = Arrays.asList(
      Field.create(SHRD_COL_TABLE_CATALOG, VARCHAR),
      Field.create(SHRD_COL_TABLE_SCHEMA, VARCHAR),
      Field.create(SHRD_COL_TABLE_NAME, VARCHAR),
      Field.create(VIEWS_COL_VIEW_DEFINITION, VARCHAR));

    public Views() {
      super(fields);
    }

    @Override
    public InfoSchemaRecordGenerator<Records.View> getRecordGenerator(FilterEvaluator filterEvaluator) {
      return new InfoSchemaRecordGenerator.Views(filterEvaluator);
    }
  }

  /**
   * Layout for the COLUMNS table.
   */
  public static class Columns extends InfoSchemaTable<Records.Column> {

    private static final List<Field> fields = Arrays.asList(
      Field.create(SHRD_COL_TABLE_CATALOG, VARCHAR),
      Field.create(SHRD_COL_TABLE_SCHEMA, VARCHAR),
      Field.create(SHRD_COL_TABLE_NAME, VARCHAR),
      Field.create(COLS_COL_COLUMN_NAME, VARCHAR),
      Field.create(COLS_COL_ORDINAL_POSITION, INT),
      Field.create(COLS_COL_COLUMN_DEFAULT, VARCHAR),
      Field.create(COLS_COL_IS_NULLABLE, VARCHAR),
      Field.create(COLS_COL_DATA_TYPE, VARCHAR),
      Field.create(COLS_COL_CHARACTER_MAXIMUM_LENGTH, INT),
      Field.create(COLS_COL_CHARACTER_OCTET_LENGTH, INT),
      Field.create(COLS_COL_NUMERIC_PRECISION, INT),
      Field.create(COLS_COL_NUMERIC_PRECISION_RADIX, INT),
      Field.create(COLS_COL_NUMERIC_SCALE, INT),
      Field.create(COLS_COL_DATETIME_PRECISION, INT),
      Field.create(COLS_COL_INTERVAL_TYPE, VARCHAR),
      Field.create(COLS_COL_INTERVAL_PRECISION, INT),
      Field.create(COLS_COL_COLUMN_SIZE, INT),
      Field.create(COLS_COL_COLUMN_FORMAT, VARCHAR),
      Field.create(COLS_COL_NUM_NULLS, BIGINT),
      Field.create(COLS_COL_MIN_VAL, VARCHAR),
      Field.create(COLS_COL_MAX_VAL, VARCHAR),
      Field.create(COLS_COL_NDV, FLOAT8),
      Field.create(COLS_COL_EST_NUM_NON_NULLS, FLOAT8),
      Field.create(COLS_COL_IS_NESTED, BIT));

    public Columns() {
      super(fields);
    }

    @Override
    public InfoSchemaRecordGenerator<Records.Column> getRecordGenerator(FilterEvaluator filterEvaluator) {
      return new InfoSchemaRecordGenerator.Columns(filterEvaluator);
    }
  }

  /**
   * Layout for the PARTITIONS table.
   */
  public static class Partitions extends InfoSchemaTable<Records.Partition> {

    private static final List<Field> fields = Arrays.asList(
      Field.create(SHRD_COL_TABLE_CATALOG, VARCHAR),
      Field.create(SHRD_COL_TABLE_SCHEMA, VARCHAR),
      Field.create(SHRD_COL_TABLE_NAME, VARCHAR),
      Field.create(PARTITIONS_COL_METADATA_KEY, VARCHAR),
      Field.create(PARTITIONS_COL_METADATA_TYPE, VARCHAR),
      Field.create(PARTITIONS_COL_METADATA_IDENTIFIER, VARCHAR),
      Field.create(PARTITIONS_COL_PARTITION_COLUMN, VARCHAR),
      Field.create(PARTITIONS_COL_PARTITION_VALUE, VARCHAR),
      Field.create(PARTITIONS_COL_LOCATION, VARCHAR),
      Field.create(PARTITIONS_COL_LAST_MODIFIED_TIME, TIMESTAMP));

    public Partitions() {
      super(fields);
    }

    @Override
    public InfoSchemaRecordGenerator<Records.Partition> getRecordGenerator(FilterEvaluator filterEvaluator) {
      return new InfoSchemaRecordGenerator.Partitions(filterEvaluator);
    }
  }

  /**
   * Layout for the FILES table.
   */
  public static class Files extends InfoSchemaTable<Records.File> {

    private static final List<Field> fields = Arrays.asList(
      Field.create(FILES_COL_SCHEMA_NAME, VARCHAR),
      Field.create(FILES_COL_ROOT_SCHEMA_NAME, VARCHAR),
      Field.create(FILES_COL_WORKSPACE_NAME, VARCHAR),
      Field.create(FILES_COL_FILE_NAME, VARCHAR),
      Field.create(FILES_COL_RELATIVE_PATH, VARCHAR),
      Field.create(FILES_COL_IS_DIRECTORY, BIT),
      Field.create(FILES_COL_IS_FILE, BIT),
      Field.create(FILES_COL_LENGTH, BIGINT),
      Field.create(FILES_COL_OWNER, VARCHAR),
      Field.create(FILES_COL_GROUP, VARCHAR),
      Field.create(FILES_COL_PERMISSION, VARCHAR),
      Field.create(FILES_COL_ACCESS_TIME, TIMESTAMP),
      Field.create(FILES_COL_MODIFICATION_TIME, TIMESTAMP));

    public Files() {
      super(fields);
    }

    @Override
    public InfoSchemaRecordGenerator<Records.File> getRecordGenerator(FilterEvaluator filterEvaluator) {
      return new InfoSchemaRecordGenerator.Files(filterEvaluator);
    }
  }

  public static class Field {

    private final String name;
    private final MajorType type;

    private Field(String name, MajorType type) {
      this.name = name;
      this.type = type;
    }

    public String getName() {
      return name;
    }

    public MajorType getType() {
      return type;
    }

    public static Field create(String name, MajorType type) {
      return new Field(name, type);
    }
  }
}
