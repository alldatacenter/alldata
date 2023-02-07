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
package org.apache.drill.jdbc.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.calcite.avatica.AvaticaParameter;
import org.apache.calcite.avatica.AvaticaStatement;
import org.apache.calcite.avatica.AvaticaUtils;
import org.apache.calcite.avatica.ColumnMetaData;
import org.apache.calcite.avatica.ColumnMetaData.StructType;
import org.apache.calcite.avatica.Meta;
import org.apache.calcite.avatica.MetaImpl;
import org.apache.calcite.avatica.MissingResultsException;
import org.apache.calcite.avatica.NoSuchStatementException;
import org.apache.calcite.avatica.QueryState;
import org.apache.calcite.avatica.remote.TypedValue;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.util.DrillStringUtils;
import org.apache.drill.exec.client.ServerMethod;
import org.apache.drill.exec.proto.UserBitShared.DrillPBError;
import org.apache.drill.exec.proto.UserProtos.CatalogMetadata;
import org.apache.drill.exec.proto.UserProtos.ColumnMetadata;
import org.apache.drill.exec.proto.UserProtos.GetCatalogsResp;
import org.apache.drill.exec.proto.UserProtos.GetColumnsResp;
import org.apache.drill.exec.proto.UserProtos.GetSchemasResp;
import org.apache.drill.exec.proto.UserProtos.GetTablesResp;
import org.apache.drill.exec.proto.UserProtos.LikeFilter;
import org.apache.drill.exec.proto.UserProtos.RequestStatus;
import org.apache.drill.exec.proto.UserProtos.SchemaMetadata;
import org.apache.drill.exec.proto.UserProtos.TableMetadata;
import org.apache.drill.exec.rpc.DrillRpcFuture;
import org.apache.drill.exec.rpc.RpcException;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;

public class DrillMetaImpl extends MetaImpl {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DrillMetaImpl.class);

  // TODO:  Use more central version of these constants if available.

  /** JDBC conventional(?) number of fractional decimal digits for REAL. */
  private static final int DECIMAL_DIGITS_REAL = 7;
  /** JDBC conventional(?) number of fractional decimal digits for FLOAT. */
  private static final int DECIMAL_DIGITS_FLOAT = DECIMAL_DIGITS_REAL;
  /** JDBC conventional(?) number of fractional decimal digits for DOUBLE. */
  private static final int DECIMAL_DIGITS_DOUBLE = 15;

  /** Radix used to report precisions of "datetime" types. */
  private static final int RADIX_DATETIME = 10;
  /** Radix used to report precisions of interval types. */
  private static final int RADIX_INTERVAL = 10;


  final DrillConnectionImpl connection;

  DrillMetaImpl(DrillConnectionImpl connection) {
    super(connection);
    this.connection = connection;
  }

  private static Signature newSignature(String sql) {
    return new Signature(
        new DrillColumnMetaDataList(),
        sql,
        Collections.<AvaticaParameter> emptyList(),
        Collections.<String, Object>emptyMap(),
        null, // CursorFactory set to null, as SQL requests use DrillCursor
        Meta.StatementType.SELECT);
  }

  private MetaResultSet s(String s) {
    try {
      logger.debug("Running {}", s);

      AvaticaStatement statement = connection.createStatement();
      return MetaResultSet.create(connection.id, statement.getId(), true,
          newSignature(s), null);
    } catch (Exception e) {
      // Wrap in RuntimeException because Avatica's abstract method declarations
      // didn't allow for SQLException!
      throw new DrillRuntimeException("Failure while attempting to get DatabaseMetadata.", e);
    }
  }

  /** Information about type mapping. */
  private static class TypeInfo {
    private static final Map<Class<?>, TypeInfo> MAPPING = ImmutableMap.<Class<?>, TypeInfo> builder()
        .put(boolean.class, of(Types.BOOLEAN, "BOOLEAN"))
        .put(Boolean.class, of(Types.BOOLEAN, "BOOLEAN"))
        .put(Byte.TYPE, of(Types.TINYINT, "TINYINT"))
        .put(Byte.class, of(Types.TINYINT, "TINYINT"))
        .put(Short.TYPE, of(Types.SMALLINT, "SMALLINT"))
        .put(Short.class, of(Types.SMALLINT, "SMALLINT"))
        .put(Integer.TYPE, of(Types.INTEGER, "INTEGER"))
        .put(Integer.class, of(Types.INTEGER, "INTEGER"))
        .put(Long.TYPE,  of(Types.BIGINT, "BIGINT"))
        .put(Long.class, of(Types.BIGINT, "BIGINT"))
        .put(Float.TYPE, of(Types.FLOAT, "FLOAT"))
        .put(Float.class,  of(Types.FLOAT, "FLOAT"))
        .put(Double.TYPE,  of(Types.DOUBLE, "DOUBLE"))
        .put(Double.class, of(Types.DOUBLE, "DOUBLE"))
        .put(String.class, of(Types.VARCHAR, "CHARACTER VARYING"))
        .put(BigDecimal.class, of(Types.DECIMAL, "DECIMAL"))
        .put(java.sql.Date.class, of(Types.DATE, "DATE"))
        .put(Time.class, of(Types.TIME, "TIME"))
        .put(Timestamp.class, of(Types.TIMESTAMP, "TIMESTAMP"))
        .build();

    private final int sqlType;
    private final String sqlTypeName;

    public TypeInfo(int sqlType, String sqlTypeName) {
      this.sqlType = sqlType;
      this.sqlTypeName = sqlTypeName;
    }

    private static TypeInfo of(int sqlType, String sqlTypeName) {
      return new TypeInfo(sqlType, sqlTypeName);
    }

    public static TypeInfo get(Class<?> clazz) {
      return MAPPING.get(clazz);
    }
  }

  /** Metadata describing a column.
   * Copied from Avatica with several fixes
   * */
  public static class MetaColumn implements Named {
    public final String tableCat;
    public final String tableSchem;
    public final String tableName;
    public final String columnName;
    public final int dataType;
    public final String typeName;
    public final Integer columnSize;
    public final Integer bufferLength = null;
    public final Integer decimalDigits;
    public final Integer numPrecRadix;
    public final int nullable;
    public final String remarks = null;
    public final String columnDef = null;
    public final Integer sqlDataType = null;
    public final Integer sqlDatetimeSub = null;
    public final Integer charOctetLength;
    public final int ordinalPosition;
    @NotNull
    public final String isNullable;
    public final String scopeCatalog = null;
    public final String scopeSchema = null;
    public final String scopeTable = null;
    public final Short sourceDataType = null;
    @NotNull
    public final String isAutoincrement = "";
    @NotNull
    public final String isGeneratedcolumn = "";

    public MetaColumn(
        String tableCat,
        String tableSchem,
        String tableName,
        String columnName,
        int dataType,
        String typeName,
        Integer columnSize,
        Integer decimalDigits,
        Integer numPrecRadix,
        int nullable,
        Integer charOctetLength,
        int ordinalPosition,
        String isNullable) {
      this.tableCat = tableCat;
      this.tableSchem = tableSchem;
      this.tableName = tableName;
      this.columnName = columnName;
      this.dataType = dataType;
      this.typeName = typeName;
      this.columnSize = columnSize;
      this.decimalDigits = decimalDigits;
      this.numPrecRadix = numPrecRadix;
      this.nullable = nullable;
      this.charOctetLength = charOctetLength;
      this.ordinalPosition = ordinalPosition;
      this.isNullable = isNullable;
    }

    @Override
    public String getName() {
      return columnName;
    }
  }

  private static LikeFilter newLikeFilter(final Pat pattern) {
    if (pattern == null || pattern.s == null) {
      return null;
    }

    return LikeFilter.newBuilder().setPattern(pattern.s).setEscape("\\").build();
  }

  /**
   * Quote the provided string as a LIKE pattern
   *
   * @param v the value to quote
   * @return a LIKE pattern matching exactly v, or {@code null} if v is {@code null}
   */
  private static Pat quote(String v) {
    if (v == null) {
      return null;
    }

    StringBuilder sb = new StringBuilder(v.length());
    for(int index = 0; index<v.length(); index++) {
      char c = v.charAt(index);
      switch(c) {
      case '%':
      case '_':
      case '\\':
        sb.append('\\').append(c);
        break;

      default:
        sb.append(c);
      }
    }

    return Pat.of(sb.toString());
  }

  // Overriding fieldMetaData as Calcite version create ColumnMetaData with invalid offset
  protected static ColumnMetaData.StructType drillFieldMetaData(Class<?> clazz) {
    final List<ColumnMetaData> list = new ArrayList<>();
    for (Field field : clazz.getFields()) {
      if (Modifier.isPublic(field.getModifiers())
          && !Modifier.isStatic(field.getModifiers())) {
        NotNull notNull = field.getAnnotation(NotNull.class);
        boolean notNullable = (notNull != null || field.getType().isPrimitive());
        list.add(
            drillColumnMetaData(
                AvaticaUtils.camelToUpper(field.getName()),
                list.size(), field.getType(), notNullable));
      }
    }
    return ColumnMetaData.struct(list);
  }


  protected static ColumnMetaData drillColumnMetaData(String name, int index,
      Class<?> type, boolean notNullable) {
    TypeInfo pair = TypeInfo.get(type);
    ColumnMetaData.Rep rep =
        ColumnMetaData.Rep.VALUE_MAP.get(type);
    ColumnMetaData.AvaticaType scalarType =
        ColumnMetaData.scalar(pair.sqlType, pair.sqlTypeName, rep);
    return new ColumnMetaData(
        index, false, true, false, false,
        notNullable
            ? DatabaseMetaData.columnNoNulls
            : DatabaseMetaData.columnNullable,
        true, -1, name, name, null,
        0, 0, null, null, scalarType, true, false, false,
        scalarType.columnClassName());
  }

  abstract private class MetadataAdapter<CalciteMetaType, Response, ResponseValue> {
    private final Class<? extends CalciteMetaType> clazz;

    public MetadataAdapter(Class<? extends CalciteMetaType> clazz) {
      this.clazz = clazz;
    }

    MetaResultSet getMeta(DrillRpcFuture<Response> future) {
      Response response;
      try {
        response = future.checkedGet();
      } catch (RpcException e) {
        throw new DrillRuntimeException(new SQLException("Failure getting metadata", e));
      }

      // Manage errors
      if (getStatus(response) != RequestStatus.OK) {
        DrillPBError error = getError(response);
        throw new DrillRuntimeException(new SQLException("Failure getting metadata: " + error.getMessage()));
      }

      try {
        List<Object> tables = getResult(response).stream()
            .map(this::adapt)
            .collect(Collectors.toList());

        Meta.Frame frame = Meta.Frame.create(0, true, tables);
        StructType fieldMetaData = drillFieldMetaData(clazz);
        Meta.Signature signature = Meta.Signature.create(fieldMetaData.columns, "", Collections.emptyList(),
            CursorFactory.record(clazz), Meta.StatementType.SELECT);

        AvaticaStatement statement = connection.createStatement();
        return MetaResultSet.create(connection.id, statement.getId(), true,
            signature, frame);
      } catch (SQLException e) {
        // Wrap in RuntimeException because Avatica's abstract method declarations
        // didn't allow for SQLException!
        throw new DrillRuntimeException(new SQLException("Failure while attempting to get DatabaseMetadata.", e));
      }
    }

    abstract protected RequestStatus getStatus(Response response);
    abstract protected DrillPBError getError(Response response);
    abstract protected List<ResponseValue> getResult(Response response);
    abstract protected CalciteMetaType adapt(ResponseValue protoValue);
  }

  private MetaResultSet clientGetTables(String catalog, final Pat schemaPattern, final Pat tableNamePattern,
      final List<String> typeList) {
    StringBuilder sb = new StringBuilder();
    sb.append("select "
        + "TABLE_CATALOG as TABLE_CAT, "
        + "TABLE_SCHEMA as TABLE_SCHEM, "
        + "TABLE_NAME, "
        + "TABLE_TYPE, "
        + "'' as REMARKS, "
        + "'' as TYPE_CAT, "
        + "'' as TYPE_SCHEM, "
        + "'' as TYPE_NAME, "
        + "'' as SELF_REFERENCING_COL_NAME, "
        + "'' as REF_GENERATION "
        + "FROM INFORMATION_SCHEMA.`TABLES` WHERE 1=1 ");

    if (catalog != null) {
      sb.append(" AND TABLE_CATALOG = '" + DrillStringUtils.escapeSql(catalog) + "' ");
    }

    if (schemaPattern.s != null) {
      sb.append(" AND TABLE_SCHEMA like '" + DrillStringUtils.escapeSql(schemaPattern.s) + "'");
    }

    if (tableNamePattern.s != null) {
      sb.append(" AND TABLE_NAME like '" + DrillStringUtils.escapeSql(tableNamePattern.s) + "'");
    }

    if (typeList != null && typeList.size() > 0) {
      sb.append("AND (");
      for (int t = 0; t < typeList.size(); t++) {
        if (t != 0) {
          sb.append(" OR ");
        }
        sb.append(" TABLE_TYPE LIKE '" + DrillStringUtils.escapeSql(typeList.get(t)) + "' ");
      }
      sb.append(")");
    }

    sb.append(" ORDER BY TABLE_TYPE, TABLE_CATALOG, TABLE_SCHEMA, TABLE_NAME");

    return s(sb.toString());
  }

  private MetaResultSet serverGetTables(String catalog, final Pat schemaPattern, final Pat tableNamePattern,
      final List<String> typeList) {
    // Catalog is not a pattern
    final LikeFilter catalogNameFilter = newLikeFilter(quote(catalog));
    final LikeFilter schemaNameFilter = newLikeFilter(schemaPattern);
    final LikeFilter tableNameFilter = newLikeFilter(tableNamePattern);

    return new MetadataAdapter<MetaImpl.MetaTable, GetTablesResp, TableMetadata>(MetaTable.class) {

      @Override
      protected RequestStatus getStatus(GetTablesResp response) {
        return response.getStatus();
      };

      @Override
      protected DrillPBError getError(GetTablesResp response) {
        return response.getError();
      };

      @Override
      protected List<TableMetadata> getResult(GetTablesResp response) {
        return response.getTablesList();
      }

      @Override
      protected MetaImpl.MetaTable adapt(TableMetadata protoValue) {
        return new MetaImpl.MetaTable(protoValue.getCatalogName(), protoValue.getSchemaName(), protoValue.getTableName(), protoValue.getType());
      };
    }.getMeta(connection.getClient().getTables(catalogNameFilter, schemaNameFilter, tableNameFilter, typeList));
  }

  /**
   * Implements {@link DatabaseMetaData#getTables}.
   */
  @Override
  public MetaResultSet getTables(ConnectionHandle ch,
                                 String catalog,
                                 Pat schemaPattern,
                                 Pat tableNamePattern,
                                 List<String> typeList) {
    if (connection.getConfig().isServerMetadataDisabled() || ! connection.getClient().getSupportedMethods().contains(ServerMethod.GET_TABLES)) {
      return clientGetTables(catalog, schemaPattern, tableNamePattern, typeList);
    }

    return serverGetTables(catalog, schemaPattern, tableNamePattern, typeList);
  }

  private MetaResultSet clientGetColumns(String catalog, Pat schemaPattern,
                              Pat tableNamePattern, Pat columnNamePattern) {
    StringBuilder sb = new StringBuilder();
    // TODO:  Resolve the various questions noted below.
    sb.append(
        "SELECT "
        // getColumns   INFORMATION_SCHEMA.COLUMNS        getColumns()
        // column       source column or                  column name
        // number       expression
        // -------      ------------------------          -------------
        + /*  1 */ "\n  TABLE_CATALOG                 as  TABLE_CAT, "
        + /*  2 */ "\n  TABLE_SCHEMA                  as  TABLE_SCHEM, "
        + /*  3 */ "\n  TABLE_NAME                    as  TABLE_NAME, "
        + /*  4 */ "\n  COLUMN_NAME                   as  COLUMN_NAME, "

        /*    5                                           DATA_TYPE */
        // TODO:  Resolve the various questions noted below for DATA_TYPE.
        + "\n  CASE DATA_TYPE "
        // (All values in JDBC 4.0/Java 7 java.sql.Types except for types.NULL:)

        + "\n    WHEN 'ARRAY'                       THEN " + Types.ARRAY

        + "\n    WHEN 'BIGINT'                      THEN " + Types.BIGINT
        + "\n    WHEN 'BINARY'                      THEN " + Types.BINARY
        // Resolve:  Not seen in Drill yet.  Can it appear?:
        + "\n    WHEN 'BINARY LARGE OBJECT'         THEN " + Types.BLOB
        + "\n    WHEN 'BINARY VARYING'              THEN " + Types.VARBINARY
        // Resolve:  Not seen in Drill yet.  Can it appear?:
        + "\n    WHEN 'BIT'                         THEN " + Types.BIT
        + "\n    WHEN 'BOOLEAN'                     THEN " + Types.BOOLEAN

        + "\n    WHEN 'CHARACTER'                   THEN " + Types.CHAR
        // Resolve:  Not seen in Drill yet.  Can it appear?:
        + "\n    WHEN 'CHARACTER LARGE OBJECT'      THEN " + Types.CLOB
        + "\n    WHEN 'CHARACTER VARYING'           THEN " + Types.VARCHAR

        // Resolve:  Not seen in Drill yet.  Can it appear?:
        + "\n    WHEN 'DATALINK'                    THEN " + Types.DATALINK
        + "\n    WHEN 'DATE'                        THEN " + Types.DATE
        + "\n    WHEN 'DECIMAL'                     THEN " + Types.DECIMAL
        // Resolve:  Not seen in Drill yet.  Can it appear?:
        + "\n    WHEN 'DISTINCT'                    THEN " + Types.DISTINCT
        + "\n    WHEN 'DOUBLE', 'DOUBLE PRECISION'  THEN " + Types.DOUBLE

        + "\n    WHEN 'FLOAT'                       THEN " + Types.FLOAT

        + "\n    WHEN 'INTEGER'                     THEN " + Types.INTEGER
        + "\n    WHEN 'INTERVAL'                    THEN " + Types.OTHER

        // Resolve:  Not seen in Drill yet.  Can it ever appear?:
        + "\n    WHEN 'JAVA_OBJECT'                 THEN " + Types.JAVA_OBJECT

        // Resolve:  Not seen in Drill yet.  Can it appear?:
        + "\n    WHEN 'LONGNVARCHAR'                THEN " + Types.LONGNVARCHAR
        // Resolve:  Not seen in Drill yet.  Can it appear?:
        + "\n    WHEN 'LONGVARBINARY'               THEN " + Types.LONGVARBINARY
        // Resolve:  Not seen in Drill yet.  Can it appear?:
        + "\n    WHEN 'LONGVARCHAR'                 THEN " + Types.LONGVARCHAR

        + "\n    WHEN 'MAP'                         THEN " + Types.OTHER

        // Resolve:  Not seen in Drill yet.  Can it appear?:
        + "\n    WHEN 'NATIONAL CHARACTER'          THEN " + Types.NCHAR
        // Resolve:  Not seen in Drill yet.  Can it appear?:
        + "\n    WHEN 'NATIONAL CHARACTER LARGE OBJECT' "
        + "\n                                       THEN " + Types.NCLOB
        // TODO:  Resolve following about NULL (and then update comment and code):
        // It is not clear whether Types.NULL can represent a type (perhaps the
        // type of the literal NULL when no further type information is known?) or
        // whether 'NULL' can appear in INFORMATION_SCHEMA.COLUMNS.DATA_TYPE.
        // For now, since it shouldn't hurt, include 'NULL'/Types.NULL in mapping.
        + "\n    WHEN 'NULL'                        THEN " + Types.NULL
        // (No NUMERIC--Drill seems to map any to DECIMAL currently.)
        + "\n    WHEN 'NUMERIC'                     THEN " + Types.NUMERIC
        // Resolve:  Not seen in Drill yet.  Can it appear?:
        + "\n    WHEN 'NATIONAL CHARACTER'          THEN " + Types.NCHAR
        // Resolve:  Not seen in Drill yet.  Can it appear?:
        + "\n    WHEN 'NATIONAL CHARACTER VARYING'  THEN " + Types.NVARCHAR

        // Resolve:  Unexpectedly, has appeared in Drill.  Should it?
        + "\n    WHEN 'OTHER'                       THEN " + Types.OTHER

        + "\n    WHEN 'REAL'                        THEN " + Types.REAL
        // Resolve:  Not seen in Drill yet.  Can it appear?:
        + "\n    WHEN 'REF'                         THEN " + Types.REF
        // Resolve:  Not seen in Drill yet.  Can it appear?:
        + "\n    WHEN 'ROWID'                       THEN " + Types.ROWID

        + "\n    WHEN 'SMALLINT'                    THEN " + Types.SMALLINT
        // Resolve:  Not seen in Drill yet.  Can it appear?:
        + "\n    WHEN 'SQLXML'                      THEN " + Types.SQLXML
        + "\n    WHEN 'STRUCT'                      THEN " + Types.STRUCT

        + "\n    WHEN 'TIME'                        THEN " + Types.TIME
        + "\n    WHEN 'TIMESTAMP'                   THEN " + Types.TIMESTAMP
        + "\n    WHEN 'TINYINT'                     THEN " + Types.TINYINT

        + "\n    ELSE                                    " + Types.OTHER
        + "\n  END                                    as  DATA_TYPE, "

        + /*  6 */ "\n  DATA_TYPE                     as  TYPE_NAME, "

        /*    7                                           COLUMN_SIZE */
        /* "... COLUMN_SIZE ....
         * For numeric data, this is the maximum precision.
         * For character data, this is the length in characters.
         * For datetime datatypes, this is the length in characters of the String
         *   representation (assuming the maximum allowed precision of the
         *   fractional seconds component).
         * For binary data, this is the length in bytes.
         * For the ROWID datatype, this is the length in bytes.
         * Null is returned for data types where the column size is not applicable."
         *
         * Note:  "Maximum precision" seems to mean the maximum number of
         * significant digits that can appear (not the number of decimal digits
         * that can be counted on, and not the maximum number of (decimal)
         * characters needed to display a value).
         */
        + "\n  CASE DATA_TYPE "
        // 0. "For boolean and bit ... 1":
        + "\n    WHEN 'BOOLEAN', 'BIT'"
        + "\n                         THEN 1 "

        // 1. "For numeric data, ... the maximum precision":
        + "\n    WHEN 'TINYINT', 'SMALLINT', 'INTEGER', 'BIGINT', "
        + "\n         'DECIMAL', 'NUMERIC', "
        + "\n         'REAL', 'FLOAT', 'DOUBLE' "
        + "\n                         THEN NUMERIC_PRECISION "

        // 2. "For character data, ... the length in characters":
        + "\n    WHEN 'CHARACTER', 'CHARACTER VARYING' "
        + "\n                         THEN CHARACTER_MAXIMUM_LENGTH "

        // 3. "For datetime datatypes ... length ... String representation
        //    (assuming the maximum ... precision of ... fractional seconds ...)":
        // SQL datetime types:
        + "\n    WHEN 'DATE'          THEN 10 "            // YYYY-MM-DD
        + "\n    WHEN 'TIME'          THEN "
        + "\n      CASE "
        + "\n        WHEN DATETIME_PRECISION > 0 "         // HH:MM:SS.sss
        + "\n                         THEN          8 + 1 + DATETIME_PRECISION"
        + "\n        ELSE                           8"     // HH:MM:SS
        + "\n      END "
        + "\n    WHEN 'TIMESTAMP'     THEN "
        + "\n      CASE "                                  // date + "T" + time
        + "\n        WHEN DATETIME_PRECISION > 0 "
        + "                           THEN 10 + 1 + 8 + 1 + DATETIME_PRECISION"
        + "\n        ELSE                  10 + 1 + 8"
        + "\n      END "
        // SQL interval types:
        // Note:  Not addressed by JDBC 4.1; providing length of current string
        // representation (not length of, say, interval literal).
        + "\n    WHEN 'INTERVAL'      THEN "
        + "\n      INTERVAL_PRECISION "
        + "\n      + "
        + "\n      CASE INTERVAL_TYPE "
        // a. Single field, not SECOND:
        + "\n        WHEN 'YEAR', 'MONTH', 'DAY' THEN 2 "  // like P...Y
        + "\n        WHEN 'HOUR', 'MINUTE'       THEN 3 "  // like PT...M
        // b. Two adjacent fields, no SECOND:
        + "\n        WHEN 'YEAR TO MONTH'        THEN 5 "  // P...Y12M
        + "\n        WHEN 'DAY TO HOUR'          THEN 6 "  // P...DT12H
        + "\n        WHEN 'HOUR TO MINUTE'       THEN 6 "  // PT...H12M
        // c. Three contiguous fields, no SECOND:
        + "\n        WHEN 'DAY TO MINUTE'        THEN 9 "  // P...DT12H12M
        // d. With SECOND field:
        + "\n        ELSE "
        + "\n          CASE INTERVAL_TYPE "
        + "\n            WHEN 'DAY TO SECOND'    THEN 12 " // P...DT12H12M12...S
        + "\n            WHEN 'HOUR TO SECOND'   THEN  9 " // PT...H12M12...S
        + "\n            WHEN 'MINUTE TO SECOND' THEN  6 " // PT...M12...S
        + "\n            WHEN 'SECOND'           THEN  3 " // PT......S
        + "\n            ELSE "                  // Make net result be -1:
        // WORKAROUND:  This "0" is to work around Drill's failure to support
        // unary minus syntax (negation):
        + "\n                                    0-INTERVAL_PRECISION - 1 "
        + "\n          END "
        + "\n          + "
        + "\n          DATETIME_PRECISION"
        + "\n          + "
        + "\n          CASE " // If frac. digits, also add 1 for decimal point.
        + "\n            WHEN DATETIME_PRECISION > 0 THEN 1"
        + "\n            ELSE                             0 "
        + "\n          END"
        // - For INTERVAL ... TO SECOND(0): "P...DT12H12M12S"
        + "\n      END "

        // 4. "For binary data, ... the length in bytes":
        + "\n    WHEN 'BINARY', 'BINARY VARYING' "
        + "\n                         THEN CHARACTER_MAXIMUM_LENGTH "

        // 5. "For ... ROWID datatype...": Not in Drill?

        // 6. "Null ... for data types [for which] ... not applicable.":
        + "\n    ELSE                      NULL "
        + "\n  END                                    as  COLUMN_SIZE, "

        + /*  8 */ "\n  CHARACTER_MAXIMUM_LENGTH      as  BUFFER_LENGTH, "

        /*    9                                           DECIMAL_DIGITS */
        + "\n  CASE  DATA_TYPE"
        + "\n    WHEN 'TINYINT', 'SMALLINT', 'INTEGER', 'BIGINT', "
        + "\n         'DECIMAL', 'NUMERIC'        THEN NUMERIC_SCALE "
        + "\n    WHEN 'REAL'                      THEN " + DECIMAL_DIGITS_REAL
        + "\n    WHEN 'FLOAT'                     THEN " + DECIMAL_DIGITS_FLOAT
        + "\n    WHEN 'DOUBLE'                    THEN " + DECIMAL_DIGITS_DOUBLE
        + "\n    WHEN 'DATE', 'TIME', 'TIMESTAMP' THEN DATETIME_PRECISION "
        + "\n    WHEN 'INTERVAL'                  THEN DATETIME_PRECISION "
        + "\n  END                                    as  DECIMAL_DIGITS, "

        /*   10                                           NUM_PREC_RADIX */
        + "\n  CASE DATA_TYPE "
        + "\n    WHEN 'TINYINT', 'SMALLINT', 'INTEGER', 'BIGINT', "
        + "\n         'DECIMAL', 'NUMERIC', "
        + "\n         'REAL', 'FLOAT', 'DOUBLE'   THEN NUMERIC_PRECISION_RADIX "
        // (NUMERIC_PRECISION_RADIX is NULL for these:)
        + "\n    WHEN 'INTERVAL'                  THEN " + RADIX_INTERVAL
        + "\n    WHEN 'DATE', 'TIME', 'TIMESTAMP' THEN " + RADIX_DATETIME
        + "\n    ELSE                                  NULL"
        + "\n  END                                    as  NUM_PREC_RADIX, "

        /*   11                                           NULLABLE */
        + "\n  CASE IS_NULLABLE "
        + "\n    WHEN 'YES'      THEN " + DatabaseMetaData.columnNullable
        + "\n    WHEN 'NO'       THEN " + DatabaseMetaData.columnNoNulls
        + "\n    WHEN ''         THEN " + DatabaseMetaData.columnNullableUnknown
        + "\n    ELSE                 -1"
        + "\n  END                                    as  NULLABLE, "

        + /* 12 */ "\n  CAST( NULL as VARCHAR )       as  REMARKS, "
        + /* 13 */ "\n  COLUMN_DEFAULT                as  COLUMN_DEF, "
        + /* 14 */ "\n  0                             as  SQL_DATA_TYPE, "
        + /* 15 */ "\n  0                             as  SQL_DATETIME_SUB, "

        /*   16                                           CHAR_OCTET_LENGTH */
        + "\n  CASE DATA_TYPE"
        + "\n    WHEN 'CHARACTER', "
        + "\n         'CHARACTER VARYING', "
        + "\n         'NATIONAL CHARACTER', "
        + "\n         'NATIONAL CHARACTER VARYING' "
        + "\n                                 THEN CHARACTER_OCTET_LENGTH "
        + "\n    ELSE                              NULL "
        + "\n  END                                    as  CHAR_OCTET_LENGTH, "

        + /* 17 */ "\n  ORDINAL_POSITION              as  ORDINAL_POSITION, "
        + /* 18 */ "\n  IS_NULLABLE                   as  IS_NULLABLE, "
        + /* 19 */ "\n  CAST( NULL as VARCHAR )       as  SCOPE_CATALOG, "
        + /* 20 */ "\n  CAST( NULL as VARCHAR )       as  SCOPE_SCHEMA, "
        + /* 21 */ "\n  CAST( NULL as VARCHAR )       as  SCOPE_TABLE, "
        // TODO:  Change to SMALLINT when it's implemented (DRILL-2470):
        + /* 22 */ "\n  CAST( NULL as INTEGER )       as  SOURCE_DATA_TYPE, "
        + /* 23 */ "\n  ''                            as  IS_AUTOINCREMENT, "
        + /* 24 */ "\n  ''                            as  IS_GENERATEDCOLUMN "

        + "\n  FROM INFORMATION_SCHEMA.COLUMNS "
        + "\n  WHERE 1=1 ");

    if (catalog != null) {
      sb.append("\n  AND TABLE_CATALOG = '" + DrillStringUtils.escapeSql(catalog) + "'");
    }
    if (schemaPattern.s != null) {
      sb.append("\n  AND TABLE_SCHEMA like '" + DrillStringUtils.escapeSql(schemaPattern.s) + "'");
    }
    if (tableNamePattern.s != null) {
      sb.append("\n  AND TABLE_NAME like '" + DrillStringUtils.escapeSql(tableNamePattern.s) + "'");
    }
    if (columnNamePattern.s != null) {
      sb.append("\n  AND COLUMN_NAME like '" + DrillStringUtils.escapeSql(columnNamePattern.s) + "'");
    }

    sb.append("\n ORDER BY TABLE_CATALOG, TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME");

    return s(sb.toString());
  }

  private MetaResultSet serverGetColumns(String catalog, Pat schemaPattern,
                              Pat tableNamePattern, Pat columnNamePattern) {
    final LikeFilter catalogNameFilter = newLikeFilter(quote(catalog));
    final LikeFilter schemaNameFilter = newLikeFilter(schemaPattern);
    final LikeFilter tableNameFilter = newLikeFilter(tableNamePattern);
    final LikeFilter columnNameFilter = newLikeFilter(columnNamePattern);

    return new MetadataAdapter<MetaColumn, GetColumnsResp, ColumnMetadata>(MetaColumn.class) {
      @Override
      protected RequestStatus getStatus(GetColumnsResp response) {
        return response.getStatus();
      }

      @Override
      protected DrillPBError getError(GetColumnsResp response) {
        return response.getError();
      }

      @Override
      protected List<ColumnMetadata> getResult(GetColumnsResp response) {
        return response.getColumnsList();
      };

      private int getDataType(ColumnMetadata value) {
        switch (value.getDataType()) {
        case "ARRAY":
          return Types.ARRAY;

        case "BIGINT":
          return Types.BIGINT;
        case "BINARY":
          return Types.BINARY;
        case "BINARY LARGE OBJECT":
          return Types.BLOB;
        case "BINARY VARYING":
          return Types.VARBINARY;
        case "BIT":
          return Types.BIT;
        case "BOOLEAN":
          return Types.BOOLEAN;
        case "CHARACTER":
          return Types.CHAR;
        // Resolve: Not seen in Drill yet. Can it appear?:
        case "CHARACTER LARGE OBJECT":
          return Types.CLOB;
        case "CHARACTER VARYING":
          return Types.VARCHAR;

        // Resolve: Not seen in Drill yet. Can it appear?:
        case "DATALINK":
          return Types.DATALINK;
        case "DATE":
          return Types.DATE;
        case "DECIMAL":
          return Types.DECIMAL;
        // Resolve: Not seen in Drill yet. Can it appear?:
        case "DISTINCT":
          return Types.DISTINCT;
        case "DOUBLE":
        case "DOUBLE PRECISION":
          return Types.DOUBLE;

        case "FLOAT":
          return Types.FLOAT;

        case "INTEGER":
          return Types.INTEGER;
        case "INTERVAL":
          return Types.OTHER;

        // Resolve: Not seen in Drill yet. Can it ever appear?:
        case "JAVA_OBJECT":
          return Types.JAVA_OBJECT;

        // Resolve: Not seen in Drill yet. Can it appear?:
        case "LONGNVARCHAR":
          return Types.LONGNVARCHAR;
        // Resolve: Not seen in Drill yet. Can it appear?:
        case "LONGVARBINARY":
          return Types.LONGVARBINARY;
        // Resolve: Not seen in Drill yet. Can it appear?:
        case "LONGVARCHAR":
          return Types.LONGVARCHAR;

        case "MAP":
          return Types.OTHER;

        // Resolve: Not seen in Drill yet. Can it appear?:
        case "NATIONAL CHARACTER":
          return Types.NCHAR;
        // Resolve: Not seen in Drill yet. Can it appear?:
        case "NATIONAL CHARACTER LARGE OBJECT":
          return Types.NCLOB;
        // Resolve: Not seen in Drill yet. Can it appear?:
        case "NATIONAL CHARACTER VARYING":
          return Types.NVARCHAR;

        // TODO: Resolve following about NULL (and then update comment and
        // code):
        // It is not clear whether Types.NULL can represent a type (perhaps the
        // type of the literal NULL when no further type information is known?)
        // or
        // whether 'NULL' can appear in INFORMATION_SCHEMA.COLUMNS.DATA_TYPE.
        // For now, since it shouldn't hurt, include 'NULL'/Types.NULL in
        // mapping.
        case "NULL":
          return Types.NULL;
        // (No NUMERIC--Drill seems to map any to DECIMAL currently.)
        case "NUMERIC":
          return Types.NUMERIC;

        // Resolve: Unexpectedly, has appeared in Drill. Should it?
        case "OTHER":
          return Types.OTHER;

        case "REAL":
          return Types.REAL;
        // Resolve: Not seen in Drill yet. Can it appear?:
        case "REF":
          return Types.REF;
        // Resolve: Not seen in Drill yet. Can it appear?:
        case "ROWID":
          return Types.ROWID;

        case "SMALLINT":
          return Types.SMALLINT;
        // Resolve: Not seen in Drill yet. Can it appear?:
        case "SQLXML":
          return Types.SQLXML;
        case "STRUCT":
          return Types.STRUCT;

        case "TIME":
          return Types.TIME;
        case "TIMESTAMP":
          return Types.TIMESTAMP;
        case "TINYINT":
          return Types.TINYINT;

        default:
          return Types.OTHER;
        }
      }

      Integer getDecimalDigits(ColumnMetadata value) {
        switch(value.getDataType()) {
        case "TINYINT":
        case "SMALLINT":
        case "INTEGER":
        case "BIGINT":
        case "DECIMAL":
        case "NUMERIC":
          return value.hasNumericScale() ? value.getNumericScale() : null;

        case "REAL":
          return DECIMAL_DIGITS_REAL;

        case "FLOAT":
          return DECIMAL_DIGITS_FLOAT;

        case "DOUBLE":
          return DECIMAL_DIGITS_DOUBLE;

        case "DATE":
        case "TIME":
        case "TIMESTAMP":
        case "INTERVAL":
          return value.getDateTimePrecision();

        default:
          return null;
        }
      }

      private Integer getNumPrecRadix(ColumnMetadata value) {
        switch(value.getDataType()) {
        case "TINYINT":
        case "SMALLINT":
        case "INTEGER":
        case "BIGINT":
        case "DECIMAL":
        case "NUMERIC":
        case "REAL":
        case "FLOAT":
        case "DOUBLE":
          return value.getNumericPrecisionRadix();

        case "INTERVAL":
          return RADIX_INTERVAL;

        case "DATE":
        case "TIME":
        case "TIMESTAMP":
          return RADIX_DATETIME;

        default:
          return null;
        }
      }

      private int getNullable(ColumnMetadata value) {
        if (!value.hasIsNullable()) {
          return DatabaseMetaData.columnNullableUnknown;
        }
        return  value.getIsNullable() ? DatabaseMetaData.columnNullable : DatabaseMetaData.columnNoNulls;
      }

      private String getIsNullable(ColumnMetadata value) {
        if (!value.hasIsNullable()) {
          return "";
        }
        return  value.getIsNullable() ? "YES" : "NO";
      }

      private Integer getCharOctetLength(ColumnMetadata value) {
        if (!value.hasCharMaxLength()) {
          return null;
        }

        switch(value.getDataType()) {
        case "CHARACTER":
        case "CHARACTER LARGE OBJECT":
        case "CHARACTER VARYING":
        case "LONGVARCHAR":
        case "LONGNVARCHAR":
        case "NATIONAL CHARACTER":
        case "NATIONAL CHARACTER LARGE OBJECT":
        case "NATIONAL CHARACTER VARYING":
          return value.getCharOctetLength();

        default:
          return null;
        }
      }

      @Override
      protected MetaColumn adapt(ColumnMetadata value) {
        return new MetaColumn(
            value.getCatalogName(),
            value.getSchemaName(),
            value.getTableName(),
            value.getColumnName(),
            getDataType(value), // It might require the full SQL type
            value.getDataType(),
            value.getColumnSize(),
            getDecimalDigits(value),
            getNumPrecRadix(value),
            getNullable(value),
            getCharOctetLength(value),
            value.getOrdinalPosition(),
            getIsNullable(value));
      }
    }.getMeta(connection.getClient().getColumns(catalogNameFilter, schemaNameFilter, tableNameFilter, columnNameFilter));
  }

  /**
   * Implements {@link DatabaseMetaData#getColumns}.
   */
  @Override
  public MetaResultSet getColumns(ConnectionHandle ch, String catalog, Pat schemaPattern, Pat tableNamePattern, Pat columnNamePattern) {
    if (connection.getConfig().isServerMetadataDisabled() || ! connection.getClient().getSupportedMethods().contains(ServerMethod.GET_COLUMNS)) {
      return clientGetColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern);
    }

    return serverGetColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern);
  }


  private MetaResultSet serverGetSchemas(String catalog, Pat schemaPattern) {
    final LikeFilter catalogNameFilter = newLikeFilter(quote(catalog));
    final LikeFilter schemaNameFilter = newLikeFilter(schemaPattern);

    return new MetadataAdapter<MetaImpl.MetaSchema, GetSchemasResp, SchemaMetadata>(MetaImpl.MetaSchema.class) {
      @Override
      protected RequestStatus getStatus(GetSchemasResp response) {
        return response.getStatus();
      }

      @Override
      protected List<SchemaMetadata> getResult(GetSchemasResp response) {
        return response.getSchemasList();
      }

      @Override
      protected DrillPBError getError(GetSchemasResp response) {
        return response.getError();
      }

      @Override
      protected MetaSchema adapt(SchemaMetadata value) {
        return new MetaImpl.MetaSchema(value.getCatalogName(), value.getSchemaName());
      }
    }.getMeta(connection.getClient().getSchemas(catalogNameFilter, schemaNameFilter));
  }


  private MetaResultSet clientGetSchemas(String catalog, Pat schemaPattern) {
    StringBuilder sb = new StringBuilder();
    sb.append("select "
        + "SCHEMA_NAME as TABLE_SCHEM, "
        + "CATALOG_NAME as TABLE_CAT "
        + " FROM INFORMATION_SCHEMA.SCHEMATA WHERE 1=1 ");

    if (catalog != null) {
      sb.append(" AND CATALOG_NAME = '" + DrillStringUtils.escapeSql(catalog) + "' ");
    }
    if (schemaPattern.s != null) {
      sb.append(" AND SCHEMA_NAME like '" + DrillStringUtils.escapeSql(schemaPattern.s) + "'");
    }
    sb.append(" ORDER BY CATALOG_NAME, SCHEMA_NAME");

    return s(sb.toString());
  }

  /**
   * Implements {@link DatabaseMetaData#getSchemas}.
   */
  @Override
  public MetaResultSet getSchemas(ConnectionHandle ch, String catalog, Pat schemaPattern) {
    if (connection.getConfig().isServerMetadataDisabled() || ! connection.getClient().getSupportedMethods().contains(ServerMethod.GET_SCHEMAS)) {
      return clientGetSchemas(catalog, schemaPattern);
    }

    return serverGetSchemas(catalog, schemaPattern);
  }

  private MetaResultSet serverGetCatalogs() {
    return new MetadataAdapter<MetaImpl.MetaCatalog, GetCatalogsResp, CatalogMetadata>(MetaImpl.MetaCatalog.class) {
      @Override
      protected RequestStatus getStatus(GetCatalogsResp response) {
        return response.getStatus();
      }

      @Override
      protected List<CatalogMetadata> getResult(GetCatalogsResp response) {
        return response.getCatalogsList();
      }

      @Override
      protected DrillPBError getError(GetCatalogsResp response) {
        return response.getError();
      }

      @Override
      protected MetaImpl.MetaCatalog adapt(CatalogMetadata protoValue) {
        return new MetaImpl.MetaCatalog(protoValue.getCatalogName());
      }
    }.getMeta(connection.getClient().getCatalogs(null));
  }

  private MetaResultSet clientGetCatalogs() {
    StringBuilder sb = new StringBuilder();
    sb.append("select "
        + "CATALOG_NAME as TABLE_CAT "
        + " FROM INFORMATION_SCHEMA.CATALOGS ");

    sb.append(" ORDER BY CATALOG_NAME");

    return s(sb.toString());
  }

  /**
   * Implements {@link DatabaseMetaData#getCatalogs}.
   */
  @Override
  public MetaResultSet getCatalogs(ConnectionHandle ch) {
    if (connection.getConfig().isServerMetadataDisabled() || ! connection.getClient().getSupportedMethods().contains(ServerMethod.GET_CATALOGS)) {
      return clientGetCatalogs();
    }

    return serverGetCatalogs();
  }

  interface Named {
    String getName();
  }

  @Override
  public StatementHandle prepare(ConnectionHandle ch, String sql, long maxRowCount) {
    StatementHandle result = super.createStatement(ch);
    result.signature = newSignature(sql);

    return result;
  }

  @Override
  @SuppressWarnings("deprecation")
  public ExecuteResult prepareAndExecute(StatementHandle h, String sql, long maxRowCount, PrepareCallback callback) {
    final Signature signature = newSignature(sql);
    try {
      synchronized (callback.getMonitor()) {
        callback.clear();
        callback.assign(signature, null, -1);
      }
      callback.execute();
      final MetaResultSet metaResultSet = MetaResultSet.create(h.connectionId, h.id, false, signature, null);
      return new ExecuteResult(Collections.singletonList(metaResultSet));
    } catch(SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public ExecuteResult prepareAndExecute(final StatementHandle handle, final String sql, final long maxRowCount,
        int maxRowsInFirstFrame, final PrepareCallback callback) throws NoSuchStatementException {
    return prepareAndExecute(handle, sql, maxRowCount, callback);
  }

  @Override
  public ExecuteBatchResult prepareAndExecuteBatch(StatementHandle statementHandle, List<String> list) throws NoSuchStatementException {
    throw new UnsupportedOperationException(this.getClass().getSimpleName());
  }

  @Override
  public ExecuteBatchResult executeBatch(StatementHandle statementHandle, List<List<TypedValue>> list) throws NoSuchStatementException {
    throw new UnsupportedOperationException(this.getClass().getSimpleName());
  }

  @Override
  public Frame fetch(StatementHandle statementHandle, long l, int i) throws NoSuchStatementException, MissingResultsException {
    throw new UnsupportedOperationException(this.getClass().getSimpleName());
  }

  @Override
  @SuppressWarnings("deprecation")
  public ExecuteResult execute(StatementHandle statementHandle,
        List<TypedValue> list, long l) throws NoSuchStatementException {
    return new ExecuteResult(Collections.singletonList(
        MetaResultSet.create(statementHandle.connectionId, statementHandle.id,
            true, statementHandle.signature, null)));
  }

  @Override
  public ExecuteResult execute(StatementHandle statementHandle,
      List<TypedValue> list, int i) throws NoSuchStatementException {
    return execute(statementHandle, list, (long) i);
  }

  @Override
  public void closeStatement(StatementHandle h) {
    // Nothing
  }

  @Override
  public boolean syncResults(StatementHandle statementHandle, QueryState queryState, long l) throws NoSuchStatementException {
    throw new UnsupportedOperationException(this.getClass().getSimpleName());
  }

  @Override
  public void commit(ConnectionHandle connectionHandle) {
    throw new UnsupportedOperationException(this.getClass().getSimpleName());
  }

  @Override
  public void rollback(ConnectionHandle connectionHandle) {
    throw new UnsupportedOperationException(this.getClass().getSimpleName());
  }

}
