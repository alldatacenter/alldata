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

import org.apache.calcite.avatica.util.TimeUnit;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.planner.types.DrillRelDataTypeSystem;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.store.dfs.WorkspaceSchemaFactory;
import org.apache.drill.metastore.metadata.BaseTableMetadata;
import org.apache.drill.metastore.metadata.PartitionMetadata;
import org.apache.drill.metastore.metadata.SegmentMetadata;
import org.apache.drill.metastore.statistics.ColumnStatistics;
import org.apache.drill.metastore.statistics.ColumnStatisticsKind;
import org.apache.drill.metastore.statistics.Statistic;
import org.apache.drill.metastore.statistics.TableStatisticsKind;
import org.apache.drill.shaded.guava.com.google.common.base.MoreObjects;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

public class Records {

  public static final String YES = "YES";
  public static final String NO = "NO";

  /**
   * Converts boolean value into its String representation.
   *
   * @param value boolean value
   * @return boolean value String representation
   */
  public static String convertToString(boolean value) {
    return value ? YES : NO;
  }

  /**
   * Converts given time millis into {@link Timestamp} instance.
   *
   * @param millis time millis
   * @return {@link Timestamp} instance
   */
  public static Timestamp convertToTimestamp(long millis) {
    return millis == BaseTableMetadata.UNDEFINED_TIME
      ? null
      : Timestamp.from(Instant.ofEpochMilli(millis));
  }

  /**
   * Pojo object for a record in INFORMATION_SCHEMA.TABLES
   */
  public static class Table {

    public final String TABLE_CATALOG;
    public final String TABLE_SCHEMA;
    public final String TABLE_NAME;
    public final String TABLE_TYPE;
    public final String TABLE_SOURCE;
    public final String LOCATION;
    public final Long NUM_ROWS;
    public final Timestamp LAST_MODIFIED_TIME;

    public Table(String catalog, String schema, String name, String type) {
      this.TABLE_CATALOG = catalog;
      this.TABLE_SCHEMA = schema;
      this.TABLE_NAME = name;
      this.TABLE_TYPE = type;
      this.TABLE_SOURCE = null;
      this.LOCATION = null;
      this.NUM_ROWS = null;
      this.LAST_MODIFIED_TIME = null;
    }

    public Table(String catalog, String schema, String type, BaseTableMetadata tableMetadata) {
      this.TABLE_CATALOG = catalog;
      this.TABLE_SCHEMA = schema;
      this.TABLE_NAME = tableMetadata.getTableInfo().name();
      this.TABLE_TYPE = type;
      this.TABLE_SOURCE = tableMetadata.getTableInfo().type();
      this.LOCATION = tableMetadata.getLocation().toString();
      this.NUM_ROWS = tableMetadata.getStatistic(TableStatisticsKind.ROW_COUNT);
      this.LAST_MODIFIED_TIME = convertToTimestamp(tableMetadata.getLastModifiedTime());
    }
  }

  /**
   * Pojo object for a record in INFORMATION_SCHEMA.COLUMNS
   */
  public static class Column {

    private static final Logger logger = getLogger(Column.class);
    private static final int MAX_UTF8_BYTES_PER_CHARACTER = 4;

    public final String TABLE_CATALOG;
    public final String TABLE_SCHEMA;
    public final String TABLE_NAME;
    public final String COLUMN_NAME;
    public final int ORDINAL_POSITION;
    public final String COLUMN_DEFAULT;
    public final String IS_NULLABLE;
    public final String DATA_TYPE;
    public final Integer CHARACTER_MAXIMUM_LENGTH;
    public final Integer CHARACTER_OCTET_LENGTH;
    public final Integer NUMERIC_PRECISION;
    public final Integer NUMERIC_PRECISION_RADIX;
    public final Integer NUMERIC_SCALE;
    public final Integer DATETIME_PRECISION;
    public final String INTERVAL_TYPE;
    public final Integer INTERVAL_PRECISION;
    public final Integer COLUMN_SIZE;
    public final String COLUMN_FORMAT;
    public final Long NUM_NULLS;
    public final String MIN_VAL;
    public final String MAX_VAL;
    public final Double NDV;
    public final Double EST_NUM_NON_NULLS;
    public final boolean IS_NESTED;

    // See:
    // - ISO/IEC 9075-11:2011(E) 5.21 COLUMNS view
    // - ISO/IEC 9075-11:2011(E) 6.22 DATA_TYPE_DESCRIPTOR base table
    public Column(String catalog, String schemaName, String tableName, RelDataTypeField field) {
      this.TABLE_CATALOG = catalog;
      this.TABLE_SCHEMA = schemaName;
      this.TABLE_NAME = tableName;

      this.COLUMN_NAME = field.getName();
      final RelDataType relDataType = field.getType();

      // (Like SQL data type names, but not standard ones.)
      final SqlTypeName sqlTypeName = relDataType.getSqlTypeName();

      // Get 1-based column ordinal position from 0-based field/column index:
      this.ORDINAL_POSITION = 1 + field.getIndex();

      this.COLUMN_DEFAULT = null;
      this.IS_NULLABLE = convertToString(relDataType.isNullable());
      this.IS_NESTED = false;

      switch (sqlTypeName) {
        // 1. SqlTypeName enumerators whose names (currently) match SQL's values
        //    for DATA_TYPE (those which have been seen in tests and verified):
        case BOOLEAN:
        case TINYINT:
        case SMALLINT:
        case INTEGER:
        case BIGINT:
        case DECIMAL:
        case FLOAT:
        case REAL:
        case DOUBLE:
        case DATE:
        case TIME:
        case TIMESTAMP:
        //   INTERVAL_YEAR_MONTH - Not identical; see below.
        //   INTERVAL_DAY_TIME   - Not identical; see below.
        //   CHAR                - Not identical; see below.
        //   VARCHAR             - Not identical; see below.
        case BINARY:
        //   VARBINARY           - Not identical; see below.
        // TODO(DRILL-3253): Update these once we have test plugin supporting
        // all needed types:
        //   NULL        - Not seen/explicitly addressed.
        //   ANY         -  " "
        //   SYMBOL      -  " "
        //   MULTISET    -  " "
        case ARRAY:
        case MAP:
        //   DISTINCT    - Not seen/explicitly addressed.
        //   STRUCTURED  -  " "
        //   ROW         -  " "
        //   OTHER       -  " "
        //   CURSOR      -  " "
        //   COLUMN_LIST -  " "
          this.DATA_TYPE = sqlTypeName.name();
          break;
        // 2.  SqlTypeName enumerators whose names (currently) do not match SQL's
        //     values for DATA_TYPE:
        case CHAR:
          this.DATA_TYPE = "CHARACTER";
          break;
        case VARCHAR:
          this.DATA_TYPE = "CHARACTER VARYING";
          break;
        case VARBINARY:
          this.DATA_TYPE = "BINARY VARYING";
          break;
        case INTERVAL_YEAR:
        case INTERVAL_YEAR_MONTH:
        case INTERVAL_MONTH:
        case INTERVAL_DAY:
        case INTERVAL_DAY_HOUR:
        case INTERVAL_DAY_MINUTE:
        case INTERVAL_DAY_SECOND:
        case INTERVAL_HOUR:
        case INTERVAL_HOUR_MINUTE:
        case INTERVAL_HOUR_SECOND:
        case INTERVAL_MINUTE:
        case INTERVAL_MINUTE_SECOND:
        case INTERVAL_SECOND:
          this.DATA_TYPE = "INTERVAL";
          break;
        // 3:  SqlTypeName enumerators not yet seen and confirmed or handled.
        default:
          logger.warn("Type not handled explicitly (code needs review): "
                       + sqlTypeName);
          this.DATA_TYPE = sqlTypeName.toString();
          break;
      }

      // Note: The branches are in the same order as SQL constraint
      // DATA_TYPE_DESCRIPTOR_DATA_TYPE_CHECK_COMBINATIONS.
      switch (sqlTypeName) {
        case CHAR:
        case VARCHAR:
          this.CHARACTER_MAXIMUM_LENGTH = relDataType.getPrecision();
          if (this.CHARACTER_MAXIMUM_LENGTH
              < Integer.MAX_VALUE / MAX_UTF8_BYTES_PER_CHARACTER) {
            this.CHARACTER_OCTET_LENGTH =
                this.CHARACTER_MAXIMUM_LENGTH * MAX_UTF8_BYTES_PER_CHARACTER;
          }
          else {
            this.CHARACTER_OCTET_LENGTH = Integer.MAX_VALUE;
          }
          // Column size is the number of characters
          this.COLUMN_SIZE = this.CHARACTER_MAXIMUM_LENGTH;
          this.NUMERIC_PRECISION = null;
          this.NUMERIC_PRECISION_RADIX = null;
          this.NUMERIC_SCALE = null;
          this.DATETIME_PRECISION = null;
          this.INTERVAL_TYPE = null;
          this.INTERVAL_PRECISION = null;
          break;

        case BINARY:
        case VARBINARY:
          this.CHARACTER_MAXIMUM_LENGTH = relDataType.getPrecision();
          this.CHARACTER_OCTET_LENGTH = this.CHARACTER_MAXIMUM_LENGTH;
          // Column size is the number of bytes
          this.COLUMN_SIZE = this.CHARACTER_MAXIMUM_LENGTH;
          this.NUMERIC_PRECISION = null;
          this.NUMERIC_PRECISION_RADIX = null;
          this.NUMERIC_SCALE = null;
          this.DATETIME_PRECISION = null;
          this.INTERVAL_TYPE = null;
          this.INTERVAL_PRECISION = null;
          break;

        case BOOLEAN:
          this.COLUMN_SIZE = 1;
          this.CHARACTER_MAXIMUM_LENGTH = null;
          this.CHARACTER_OCTET_LENGTH = null;
          this.NUMERIC_PRECISION = null;
          this.NUMERIC_PRECISION_RADIX = null;
          this.NUMERIC_SCALE = null;
          this.DATETIME_PRECISION = null;
          this.INTERVAL_TYPE = null;
          this.INTERVAL_PRECISION = null;
          break;

        case TINYINT:
        case SMALLINT:
        case INTEGER:
        case BIGINT:
          this.CHARACTER_MAXIMUM_LENGTH = null;
          this.CHARACTER_OCTET_LENGTH = null;
          // This NUMERIC_PRECISION is in bits since NUMERIC_PRECISION_RADIX is 2.
          switch (sqlTypeName) {
            case TINYINT:
              NUMERIC_PRECISION = 8;
              break;
            case SMALLINT:
              NUMERIC_PRECISION = 16;
              break;
            case INTEGER:
              NUMERIC_PRECISION = 32;
              break;
            case BIGINT:
              NUMERIC_PRECISION = 64;
              break;
            default:
              throw new AssertionError(
                  "Unexpected " + sqlTypeName.getClass().getName() + " value "
                  + sqlTypeName );
              //break;
          }
          this.NUMERIC_PRECISION_RADIX = 2;
          // Column size is the number of digits, based on the precision radix
          this.COLUMN_SIZE = NUMERIC_PRECISION;
          this.NUMERIC_SCALE = 0;
          this.DATETIME_PRECISION = null;
          this.INTERVAL_TYPE = null;
          this.INTERVAL_PRECISION = null;
          break;

        case DECIMAL:
          this.CHARACTER_MAXIMUM_LENGTH = null;
          this.CHARACTER_OCTET_LENGTH = null;
          // This NUMERIC_PRECISION is in decimal digits since
          // NUMERIC_PRECISION_RADIX is 10.
          this.NUMERIC_PRECISION = relDataType.getPrecision();
          this.NUMERIC_PRECISION_RADIX = 10;
          // Column size is the number of digits, based on the precision radix
          this.COLUMN_SIZE = NUMERIC_PRECISION;
          this.NUMERIC_SCALE = relDataType.getScale();
          this.DATETIME_PRECISION = null;
          this.INTERVAL_TYPE = null;
          this.INTERVAL_PRECISION = null;
          break;

        case REAL:
        case FLOAT:
        case DOUBLE:
          this.CHARACTER_MAXIMUM_LENGTH = null;
          this.CHARACTER_OCTET_LENGTH = null;
          // This NUMERIC_PRECISION is in bits since NUMERIC_PRECISION_RADIX is 2.
          switch (sqlTypeName) {
            case REAL:
              NUMERIC_PRECISION = 24;
              break;
            case FLOAT:
              NUMERIC_PRECISION = 24;
              break;
            case DOUBLE:
              NUMERIC_PRECISION = 53;
              break;
            default:
              throw new AssertionError(
                  "Unexpected type " + sqlTypeName + " in approximate-types branch" );
              //break;
          }
          this.NUMERIC_PRECISION_RADIX = 2;
          // Column size is the number of digits, based on the precision radix
          this.COLUMN_SIZE = NUMERIC_PRECISION;
          this.NUMERIC_SCALE = null;
          this.DATETIME_PRECISION = null;
          this.INTERVAL_TYPE = null;
          this.INTERVAL_PRECISION = null;
          break;

        case DATE:
        case TIME:
        case TIMESTAMP:
          this.CHARACTER_MAXIMUM_LENGTH = null;
          this.CHARACTER_OCTET_LENGTH = null;
          this.NUMERIC_PRECISION = null;
          this.NUMERIC_PRECISION_RADIX = null;
          this.NUMERIC_SCALE = null;
          // TODO:  Resolve whether this gets _SQL_-defined precision.
          // (RelDataType.getPrecision()'s doc. says "JDBC-defined
          // precision.")
          this.DATETIME_PRECISION = relDataType.getPrecision();
          this.INTERVAL_TYPE = null;
          this.INTERVAL_PRECISION = null;
          switch (sqlTypeName) {
          case DATE:
            this.COLUMN_SIZE = 10;
            break;// yyyy-MM-dd
          case TIME: this.COLUMN_SIZE = this.DATETIME_PRECISION == 0
              ? 8 // HH::mm::ss
              : 8 + 1 + this.DATETIME_PRECISION;
            break;

          case TIMESTAMP: this.COLUMN_SIZE = this.DATETIME_PRECISION == 0
              ? 10 + 1 + 8 // date + "T" + time
              : 10 + 1 + 8 + 1 + this.DATETIME_PRECISION;
            break;

          default:
            throw new AssertionError(
                "Unexpected type " + sqlTypeName + " in approximate-types branch" );

          }
          break;
        case INTERVAL_YEAR:
        case INTERVAL_YEAR_MONTH:
        case INTERVAL_MONTH:
        case INTERVAL_DAY:
        case INTERVAL_DAY_HOUR:
        case INTERVAL_DAY_MINUTE:
        case INTERVAL_DAY_SECOND:
        case INTERVAL_HOUR:
        case INTERVAL_HOUR_MINUTE:
        case INTERVAL_HOUR_SECOND:
        case INTERVAL_MINUTE:
        case INTERVAL_MINUTE_SECOND:
        case INTERVAL_SECOND:
          this.CHARACTER_MAXIMUM_LENGTH = null;
          this.CHARACTER_OCTET_LENGTH = null;
          this.NUMERIC_PRECISION = null;
          this.NUMERIC_PRECISION_RADIX = null;
          this.NUMERIC_SCALE = null;
          switch (sqlTypeName) {
            case INTERVAL_YEAR:
            case INTERVAL_YEAR_MONTH:
            case INTERVAL_MONTH:
              // NOTE:  Apparently can't get use RelDataType, etc.; it seems to
              // apply a default fractional seconds precision of 6 for SECOND,
              // even though SECOND does not exist for this case.
              this.DATETIME_PRECISION = 0;
              break;
            case INTERVAL_DAY:
            case INTERVAL_DAY_HOUR:
            case INTERVAL_DAY_MINUTE:
            case INTERVAL_DAY_SECOND:
            case INTERVAL_HOUR:
            case INTERVAL_HOUR_MINUTE:
            case INTERVAL_HOUR_SECOND:
            case INTERVAL_MINUTE:
            case INTERVAL_MINUTE_SECOND:
            case INTERVAL_SECOND:
              this.DATETIME_PRECISION =
                  relDataType
                  .getIntervalQualifier()
                  .getFractionalSecondPrecision(
                      DrillRelDataTypeSystem.DRILL_REL_DATATYPE_SYSTEM);
              break;
            default:
              throw new AssertionError(
                "Unexpected type " + sqlTypeName + " in interval-types branch");
          }
          this.INTERVAL_PRECISION =
              relDataType
              .getIntervalQualifier()
              .getStartPrecision(DrillRelDataTypeSystem.DRILL_REL_DATATYPE_SYSTEM);
          {
            final TimeUnit start = relDataType.getIntervalQualifier().getStartUnit();
            // NOTE: getEndUnit() returns null instead of YEAR for "INTERVAL YEAR".
            final TimeUnit end = MoreObjects.firstNonNull(relDataType.getIntervalQualifier().getEndUnit(), start);
            if (start == end) {
              this.INTERVAL_TYPE = start.name();
            }
            else {
              this.INTERVAL_TYPE = start + " TO " + end;
            }

            // extra size for fractional types
            final int extraSecondIntervalSize = this.DATETIME_PRECISION > 0
              ? DATETIME_PRECISION + 1 // add 1 for decimal point
              : 0;

            switch (start) {
            case YEAR:
              switch(end) {
              case YEAR:
                this.COLUMN_SIZE = INTERVAL_PRECISION + 2;
                break;// P..Y
              case MONTH:
                this.COLUMN_SIZE = this.INTERVAL_PRECISION + 5;
                break; // P..Y12M
              default:
                throw new AssertionError("Unexpected interval type " + this.INTERVAL_TYPE + " in interval-types branch" );
              }
              break;

            case MONTH:
              switch (end) {
              case MONTH:
                this.COLUMN_SIZE = this.INTERVAL_PRECISION + 2;
                break; // P..M
              default:
                throw new AssertionError("Unexpected interval type " + this.INTERVAL_TYPE + " in interval-types branch" );
              }
              break;

            case DAY:
              switch (end) {
              case DAY:
                this.COLUMN_SIZE = this.INTERVAL_PRECISION + 2;
                break; // P..D
              case HOUR:
                this.COLUMN_SIZE = this.INTERVAL_PRECISION + 6;
                break; // P..DT12H
              case MINUTE:
                this.COLUMN_SIZE = this.INTERVAL_PRECISION + 9;
                break; // P..DT12H60M
              case SECOND:
                this.COLUMN_SIZE = this.INTERVAL_PRECISION + 12 + extraSecondIntervalSize;
                break; // P..DT12H60M60....S
              default:
                throw new AssertionError("Unexpected interval type " + this.INTERVAL_TYPE + " in interval-types branch" );
              }
              break;

            case HOUR:
              switch (end) {
              case HOUR:
                this.COLUMN_SIZE = this.INTERVAL_PRECISION + 3;
                break; // PT..H
              case MINUTE:
                this.COLUMN_SIZE = this.INTERVAL_PRECISION + 6;
                break; // PT..H60M
              case SECOND:
                this.COLUMN_SIZE = this.INTERVAL_PRECISION + 9 + extraSecondIntervalSize;
                break; // PT..H12M60....S
              default:
                throw new AssertionError("Unexpected interval type " + this.INTERVAL_TYPE + " in interval-types branch" );
              }
              break;

            case MINUTE:
              switch (end) {
              case MINUTE:
                this.COLUMN_SIZE = this.INTERVAL_PRECISION + 3;
                break; // PT...M
              case SECOND:
                this.COLUMN_SIZE = this.INTERVAL_PRECISION + 6 + extraSecondIntervalSize;
                break; // PT..M60....S
              default:
                throw new AssertionError("Unexpected interval type " + this.INTERVAL_TYPE + " in interval-types branch" );
              }
              break;


            case SECOND:
              switch (end) {
              case SECOND:
                this.COLUMN_SIZE = this.INTERVAL_PRECISION + 3 + extraSecondIntervalSize;
                break; // PT....S
              default:
                throw new AssertionError("Unexpected interval type " + this.INTERVAL_TYPE + " in interval-types branch" );
              }
              break;

            default:
              throw new AssertionError("Unexpected interval type " + this.INTERVAL_TYPE + " in interval-types branch" );
            }
          }
          break;

        default:
          this.NUMERIC_PRECISION_RADIX = null;
          this.CHARACTER_MAXIMUM_LENGTH = null;
          this.CHARACTER_OCTET_LENGTH = null;
          this.NUMERIC_PRECISION = null;
          this.NUMERIC_SCALE = null;
          this.DATETIME_PRECISION = null;
          this.INTERVAL_TYPE = null;
          this.INTERVAL_PRECISION = null;
          this.COLUMN_SIZE = null;
        break;
      }
      this.COLUMN_FORMAT = null;
      this.NUM_NULLS = null;
      this.MIN_VAL = null;
      this.MAX_VAL = null;
      this.NDV = null;
      this.EST_NUM_NON_NULLS = null;
    }

    public Column(String catalog, String schemaName, String tableName, String columnName,
                  ColumnMetadata columnMetadata, ColumnStatistics<?> columnStatistics, int index,
                  boolean isNested) {
      this.TABLE_CATALOG = catalog;
      this.TABLE_SCHEMA = schemaName;
      this.TABLE_NAME = tableName;
      this.COLUMN_NAME = columnName;
      this.ORDINAL_POSITION = index + 1;
      this.COLUMN_DEFAULT = columnMetadata.defaultValue();
      this.IS_NULLABLE = convertToString(columnMetadata.isNullable());
      this.COLUMN_FORMAT = columnMetadata.format();
      this.IS_NESTED = isNested;

      TypeProtos.MajorType type = columnMetadata.majorType();
      switch (type.getMinorType()) {
        case INTERVAL:
        case INTERVALDAY:
        case INTERVALYEAR:
          this.DATA_TYPE = TypeProtos.MinorType.INTERVAL.name();
          break;
        default:
          this.DATA_TYPE = Types.getSqlTypeName(type);
      }

      int columnSize = Types.getJdbcDisplaySize(type);
      this.COLUMN_SIZE = columnSize == 0 && Types.isScalarStringType(type) ? Types.MAX_VARCHAR_LENGTH : columnSize;

      if (Types.isScalarStringType(type)) {
        this.CHARACTER_MAXIMUM_LENGTH = COLUMN_SIZE;
        this.CHARACTER_OCTET_LENGTH = COLUMN_SIZE;
      } else {
        this.CHARACTER_MAXIMUM_LENGTH = null;
        this.CHARACTER_OCTET_LENGTH = null;
      }

      if (Types.isNumericType(type)) {
        this.NUMERIC_PRECISION = type.getPrecision();
        this.NUMERIC_PRECISION_RADIX = Types.isDecimalType(type) ? 10 : 2;
        this.NUMERIC_SCALE = type.getScale();
      } else {
        this.NUMERIC_PRECISION = null;
        this.NUMERIC_PRECISION_RADIX = null;
        this.NUMERIC_SCALE = null;
      }

      if (Types.isDateTimeType(type)) {
        this.DATETIME_PRECISION = COLUMN_SIZE;
      } else {
        this.DATETIME_PRECISION = null;
      }

      if (Types.isIntervalType(type)) {
        this.INTERVAL_TYPE = Types.getSqlTypeName(type);
        this.INTERVAL_PRECISION = 0;
      } else {
        this.INTERVAL_TYPE = null;
        this.INTERVAL_PRECISION = null;
      }

      if (columnStatistics == null) {
        this.NUM_NULLS = null;
        this.MIN_VAL = null;
        this.MAX_VAL = null;
        this.NDV = null;
        this.EST_NUM_NON_NULLS = null;
      } else {
        Long numNulls = ColumnStatisticsKind.NULLS_COUNT.getFrom(columnStatistics);
        this.NUM_NULLS = numNulls == Statistic.NO_COLUMN_STATS ? null : numNulls;
        Object minVal = ColumnStatisticsKind.MIN_VALUE.getFrom(columnStatistics);
        this.MIN_VAL = minVal == null ? null : minVal.toString();
        Object maxVal = ColumnStatisticsKind.MAX_VALUE.getFrom(columnStatistics);
        this.MAX_VAL = maxVal == null ? null : maxVal.toString();
        this.NDV = ColumnStatisticsKind.NDV.getFrom(columnStatistics);
        this.EST_NUM_NON_NULLS = ColumnStatisticsKind.NON_NULL_COUNT.getFrom(columnStatistics);
      }
    }
  }

  /**
   * Pojo object for a record in INFORMATION_SCHEMA.VIEWS
   */
  public static class View {

    public final String TABLE_CATALOG;
    public final String TABLE_SCHEMA;
    public final String TABLE_NAME;
    public final String VIEW_DEFINITION;

    public View(String catalog, String schema, String name, String definition) {
      this.TABLE_CATALOG = catalog;
      this.TABLE_SCHEMA = schema;
      this.TABLE_NAME = name;
      this.VIEW_DEFINITION = definition;
    }
  }

  /**
   * Pojo object for a record in INFORMATION_SCHEMA.CATALOGS
   */
  public static class Catalog {

    public final String CATALOG_NAME;
    public final String CATALOG_DESCRIPTION;
    public final String CATALOG_CONNECT;

    public Catalog(String name, String description, String connect) {
      this.CATALOG_NAME = name;
      this.CATALOG_DESCRIPTION = description;
      this.CATALOG_CONNECT = connect;
    }
  }

  /**
   * Pojo object for a record in INFORMATION_SCHEMA.SCHEMATA
   */
  public static class Schema {

    public final String CATALOG_NAME;
    public final String SCHEMA_NAME;
    public final String SCHEMA_OWNER;
    public final String TYPE;
    public final String IS_MUTABLE;

    public Schema(String catalog, String name, String owner, String type, boolean isMutable) {
      this.CATALOG_NAME = catalog;
      this.SCHEMA_NAME = name;
      this.SCHEMA_OWNER = owner;
      this.TYPE = type;
      this.IS_MUTABLE = convertToString(isMutable);
    }
  }

  /**
   * Pojo object for a record in INFORMATION_SCHEMA.PARTITIONS
   */
  public static class Partition {

    public final String TABLE_CATALOG;
    public final String TABLE_SCHEMA;
    public final String TABLE_NAME;
    public final String METADATA_KEY;
    public final String METADATA_TYPE;
    public final String METADATA_IDENTIFIER;
    public final String PARTITION_COLUMN;
    public final String PARTITION_VALUE;
    public final String LOCATION;
    public final Timestamp LAST_MODIFIED_TIME;

    public Partition(String catalog, String schemaName, String value, SegmentMetadata segmentMetadata) {
      this.TABLE_CATALOG = catalog;
      this.TABLE_SCHEMA = schemaName;
      this.TABLE_NAME = segmentMetadata.getTableInfo().name();
      this.METADATA_KEY = segmentMetadata.getMetadataInfo().key();
      this.METADATA_TYPE = segmentMetadata.getMetadataInfo().type().name();
      this.METADATA_IDENTIFIER = segmentMetadata.getMetadataInfo().identifier();
      this.PARTITION_COLUMN = segmentMetadata.getColumn().toString();
      this.PARTITION_VALUE = value;
      this.LOCATION = segmentMetadata.getLocation().toString();
      this.LAST_MODIFIED_TIME = convertToTimestamp(segmentMetadata.getLastModifiedTime());
    }

    public Partition(String catalog, String schemaName, String value, PartitionMetadata partitionMetadata) {
      this.TABLE_CATALOG = catalog;
      this.TABLE_SCHEMA = schemaName;
      this.TABLE_NAME = partitionMetadata.getTableInfo().name();
      this.METADATA_KEY = partitionMetadata.getMetadataInfo().key();
      this.METADATA_TYPE = partitionMetadata.getMetadataInfo().type().name();
      this.METADATA_IDENTIFIER = partitionMetadata.getMetadataInfo().identifier();
      this.PARTITION_COLUMN = partitionMetadata.getColumn().toString();
      this.PARTITION_VALUE = value;
      this.LOCATION = null;
      this.LAST_MODIFIED_TIME = convertToTimestamp(partitionMetadata.getLastModifiedTime());
    }

    public static List<Partition> fromSegment(String catalog, String schemaName, SegmentMetadata segmentMetadata) {
      return segmentMetadata.getPartitionValues().stream()
        .map(value -> new Partition(catalog, schemaName, value, segmentMetadata))
        .collect(Collectors.toList());
    }

    public static List<Partition> fromPartition(String catalog, String schemaName, PartitionMetadata partitionMetadata) {
      return partitionMetadata.getPartitionValues().stream()
        .map(value -> new Partition(catalog, schemaName, value, partitionMetadata))
        .collect(Collectors.toList());
    }
  }

  /**
   * Pojo object for a record in INFORMATION_SCHEMA.FILES
   */
  public static class File {

    public final String SCHEMA_NAME;
    public final String ROOT_SCHEMA_NAME;
    public final String WORKSPACE_NAME;
    public final String FILE_NAME;
    public final String RELATIVE_PATH;
    public final boolean IS_DIRECTORY;
    public final boolean IS_FILE;
    public final long LENGTH;
    public final String OWNER;
    public final String GROUP;
    public final String PERMISSION;
    public final Timestamp ACCESS_TIME;
    public final Timestamp MODIFICATION_TIME;

    public File(String schemaName, WorkspaceSchemaFactory.WorkspaceSchema wsSchema, FileStatus fileStatus) {
      this.SCHEMA_NAME = schemaName;
      this.ROOT_SCHEMA_NAME = wsSchema.getSchemaPath().get(0);
      this.WORKSPACE_NAME = wsSchema.getName();
      this.FILE_NAME = fileStatus.getPath().getName();
      this.RELATIVE_PATH = Path.getPathWithoutSchemeAndAuthority(new Path(wsSchema.getDefaultLocation())).toUri()
        .relativize(Path.getPathWithoutSchemeAndAuthority(fileStatus.getPath()).toUri()).getPath();
      this.IS_DIRECTORY = fileStatus.isDirectory();
      this.IS_FILE = fileStatus.isFile();
      this.LENGTH = fileStatus.getLen();
      this.OWNER = fileStatus.getOwner();
      this.GROUP = fileStatus.getGroup();
      this.PERMISSION = fileStatus.getPermission().toString();
      this.ACCESS_TIME = getTimestampWithReplacedZone(fileStatus.getAccessTime());
      this.MODIFICATION_TIME = getTimestampWithReplacedZone(fileStatus.getModificationTime());
    }

    /**
     * Convert milliseconds into sql timestamp.
     * Get the timestamp in UTC because Drill's internal TIMESTAMP stores time in UTC.
     *
     * @param ms milliseconds
     * @return sql timestamp instance
     */
    private Timestamp getTimestampWithReplacedZone(long ms) {
      return Timestamp.from(Instant.ofEpochMilli(ms)
          .atZone(ZoneId.systemDefault())
          .withZoneSameLocal(ZoneOffset.UTC)
          .toInstant());
    }
  }
}
