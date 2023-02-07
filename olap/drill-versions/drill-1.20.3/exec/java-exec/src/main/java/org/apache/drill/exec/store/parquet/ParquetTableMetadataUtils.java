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
package org.apache.drill.exec.store.parquet;

import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.planner.common.DrillStatsTable;
import org.apache.drill.exec.record.SchemaUtil;
import org.apache.drill.metastore.metadata.BaseTableMetadata;
import org.apache.drill.metastore.statistics.BaseStatisticsKind;
import org.apache.drill.metastore.metadata.MetadataInfo;
import org.apache.drill.metastore.metadata.MetadataType;
import org.apache.drill.metastore.metadata.TableInfo;
import org.apache.drill.metastore.statistics.TableStatisticsKind;
import org.apache.drill.metastore.statistics.Statistic;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.metadata.TupleSchema;
import org.apache.drill.exec.resolver.TypeCastRules;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.store.ColumnExplorer;
import org.apache.drill.exec.store.parquet.metadata.MetadataBase;
import org.apache.drill.exec.store.parquet.metadata.MetadataVersion;
import org.apache.drill.exec.store.parquet.metadata.Metadata_V4;
import org.apache.drill.metastore.statistics.CollectableColumnStatisticsKind;
import org.apache.drill.metastore.statistics.ColumnStatistics;
import org.apache.drill.metastore.statistics.ColumnStatisticsKind;
import org.apache.drill.metastore.metadata.FileMetadata;
import org.apache.drill.metastore.metadata.NonInterestingColumnsMetadata;
import org.apache.drill.metastore.metadata.PartitionMetadata;
import org.apache.drill.metastore.metadata.RowGroupMetadata;
import org.apache.drill.metastore.statistics.StatisticsHolder;
import org.apache.drill.metastore.util.SchemaPathUtils;
import org.apache.drill.metastore.util.TableMetadataUtils;
import org.apache.drill.metastore.statistics.ExactStatisticsConstants;
import org.apache.drill.exec.expr.StatisticsProvider;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.LinkedListMultimap;
import org.apache.drill.shaded.guava.com.google.common.collect.Multimap;
import org.apache.drill.shaded.guava.com.google.common.primitives.Longs;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.schema.OriginalType;
import org.apache.parquet.schema.PrimitiveType;
import org.apache.parquet.schema.Type;
import org.joda.time.DateTimeConstants;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility class for converting parquet metadata classes to Metastore metadata classes.
 */
@SuppressWarnings("WeakerAccess")
public class ParquetTableMetadataUtils {

  static final List<CollectableColumnStatisticsKind<?>> PARQUET_COLUMN_STATISTICS =
          ImmutableList.of(
              ColumnStatisticsKind.MAX_VALUE,
              ColumnStatisticsKind.MIN_VALUE,
              ColumnStatisticsKind.NULLS_COUNT);

  private ParquetTableMetadataUtils() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Creates new map based on specified {@code columnStatistics} with added statistics
   * for implicit and partition (dir) columns.
   *
   * @param columnsStatistics           map of column statistics to expand
   * @param columns                     list of all columns including implicit or partition ones
   * @param partitionValues             list of partition values
   * @param optionManager               option manager
   * @param location                    location of metadata part
   * @param supportsFileImplicitColumns whether implicit columns are supported
   * @return map with added statistics for implicit and partition (dir) columns
   */
  public static Map<SchemaPath, ColumnStatistics<?>> addImplicitColumnsStatistics(
      Map<SchemaPath, ColumnStatistics<?>> columnsStatistics, List<SchemaPath> columns,
      List<String> partitionValues, OptionManager optionManager, Path location, boolean supportsFileImplicitColumns) {
    ColumnExplorer columnExplorer = new ColumnExplorer(optionManager, columns);

    Map<String, String> implicitColValues = columnExplorer.populateImplicitColumns(
        location, partitionValues, supportsFileImplicitColumns);
    columnsStatistics = new HashMap<>(columnsStatistics);
    for (Map.Entry<String, String> partitionValue : implicitColValues.entrySet()) {
      columnsStatistics.put(SchemaPath.getCompoundPath(partitionValue.getKey()),
          StatisticsProvider.getConstantColumnStatistics(partitionValue.getValue(), TypeProtos.MinorType.VARCHAR));
    }
    return columnsStatistics;
  }

  /**
   * Returns list of {@link RowGroupMetadata} received by converting parquet row groups metadata
   * taken from the specified tableMetadata.
   * Assigns index to row groups based on their position in files metadata.
   * For empty / fake row groups assigns '-1' index.
   *
   * @param tableMetadata the source of row groups to be converted
   * @return list of {@link RowGroupMetadata}
   */
  public static Multimap<Path, RowGroupMetadata> getRowGroupsMetadata(MetadataBase.ParquetTableMetadataBase tableMetadata) {
    Multimap<Path, RowGroupMetadata> rowGroups = LinkedListMultimap.create();
    for (MetadataBase.ParquetFileMetadata file : tableMetadata.getFiles()) {
      int index = 0;
      for (MetadataBase.RowGroupMetadata rowGroupMetadata : file.getRowGroups()) {
        int newIndex;
        if (rowGroupMetadata.isEmpty()) {
          Preconditions.checkState(file.getRowGroups().size() == 1, "Only one empty / fake row group is allowed per file");
          newIndex = -1;
        } else {
          newIndex = index++;
        }
        Path filePath = Path.getPathWithoutSchemeAndAuthority(file.getPath());
        rowGroups.put(filePath, getRowGroupMetadata(tableMetadata, rowGroupMetadata, newIndex, filePath));
      }
    }

    return rowGroups;
  }

  /**
   * Returns {@link RowGroupMetadata} instance converted from specified parquet {@code rowGroupMetadata}.
   *
   * @param tableMetadata    table metadata which contains row group metadata to convert
   * @param rowGroupMetadata row group metadata to convert
   * @param rgIndexInFile    index of current row group within the file
   * @param location         location of file with current row group
   * @return {@link RowGroupMetadata} instance converted from specified parquet {@code rowGroupMetadata}
   */
  public static RowGroupMetadata getRowGroupMetadata(MetadataBase.ParquetTableMetadataBase tableMetadata,
      MetadataBase.RowGroupMetadata rowGroupMetadata, int rgIndexInFile, Path location) {
    Map<SchemaPath, ColumnStatistics<?>> columnsStatistics = getRowGroupColumnStatistics(tableMetadata, rowGroupMetadata);
    List<StatisticsHolder<?>> rowGroupStatistics = new ArrayList<>();
    rowGroupStatistics.add(new StatisticsHolder<>(rowGroupMetadata.getRowCount(), TableStatisticsKind.ROW_COUNT));
    rowGroupStatistics.add(new StatisticsHolder<>(rowGroupMetadata.getStart(), new BaseStatisticsKind<>(ExactStatisticsConstants.START, true)));
    rowGroupStatistics.add(new StatisticsHolder<>(rowGroupMetadata.getLength(), new BaseStatisticsKind<>(ExactStatisticsConstants.LENGTH, true)));

    Map<SchemaPath, TypeProtos.MajorType> columns = getRowGroupFields(tableMetadata, rowGroupMetadata);
    Map<SchemaPath, TypeProtos.MajorType> intermediateColumns = getIntermediateFields(tableMetadata, rowGroupMetadata);

    TupleMetadata schema = new TupleSchema();
    columns.forEach(
        (schemaPath, majorType) -> SchemaPathUtils.addColumnMetadata(schema, schemaPath, majorType, intermediateColumns)
    );

    MetadataInfo metadataInfo = MetadataInfo.builder().type(MetadataType.ROW_GROUP).build();

    return RowGroupMetadata.builder()
        .tableInfo(TableInfo.UNKNOWN_TABLE_INFO)
        .metadataInfo(metadataInfo)
        .schema(schema)
        .columnsStatistics(columnsStatistics)
        .metadataStatistics(rowGroupStatistics)
        .hostAffinity(rowGroupMetadata.getHostAffinity())
        .rowGroupIndex(rgIndexInFile)
        .path(location)
        .build();
  }

  /**
   * Returns {@link FileMetadata} instance received by merging specified {@link RowGroupMetadata} list.
   *
   * @param rowGroups collection of {@link RowGroupMetadata} to be merged
   * @return {@link FileMetadata} instance
   */
  public static FileMetadata getFileMetadata(Collection<RowGroupMetadata> rowGroups) {
    if (rowGroups.isEmpty()) {
      return null;
    }
    List<StatisticsHolder<?>> fileStatistics = new ArrayList<>();
    fileStatistics.add(new StatisticsHolder<>(TableStatisticsKind.ROW_COUNT.mergeStatistics(rowGroups), TableStatisticsKind.ROW_COUNT));

    RowGroupMetadata rowGroupMetadata = rowGroups.iterator().next();
    TupleMetadata schema = rowGroupMetadata.getSchema();

    Set<SchemaPath> columns = rowGroupMetadata.getColumnsStatistics().keySet();

    MetadataInfo metadataInfo = MetadataInfo.builder().type(MetadataType.FILE).build();

    return FileMetadata.builder()
        .tableInfo(rowGroupMetadata.getTableInfo())
        .metadataInfo(metadataInfo)
        .path(rowGroupMetadata.getPath())
        .schema(schema)
        .columnsStatistics(TableMetadataUtils.mergeColumnsStatistics(rowGroups, columns, PARQUET_COLUMN_STATISTICS))
        .metadataStatistics(fileStatistics)
        .build();
  }

  /**
   * Returns {@link PartitionMetadata} instance received by merging specified {@link FileMetadata} list.
   *
   * @param partitionColumn partition column
   * @param files           list of files to be merged
   * @return {@link PartitionMetadata} instance
   */
  public static PartitionMetadata getPartitionMetadata(SchemaPath partitionColumn, List<FileMetadata> files) {
    Set<Path> locations = new HashSet<>();
    Set<SchemaPath> columns = new HashSet<>();

    for (FileMetadata file : files) {
      columns.addAll(file.getColumnsStatistics().keySet());
      locations.add(file.getPath());
    }

    FileMetadata fileMetadata = files.iterator().next();

    MetadataInfo metadataInfo = MetadataInfo.builder().type(MetadataType.PARTITION).build();

    return PartitionMetadata.builder()
        .tableInfo(fileMetadata.getTableInfo())
        .metadataInfo(metadataInfo)
        .column(partitionColumn)
        .schema(fileMetadata.getSchema())
        .columnsStatistics(TableMetadataUtils.mergeColumnsStatistics(files, columns, PARQUET_COLUMN_STATISTICS))
        .metadataStatistics(Collections.singletonList(new StatisticsHolder<>(TableStatisticsKind.ROW_COUNT.mergeStatistics(files), TableStatisticsKind.ROW_COUNT)))
        .partitionValues(Collections.emptyList())
        .locations(locations)
        .build();
  }

  /**
   * Converts specified {@link MetadataBase.RowGroupMetadata} into the map of {@link ColumnStatistics}
   * instances with column names as keys.
   *
   * @param tableMetadata    the source of column types
   * @param rowGroupMetadata metadata to convert
   * @return map with converted row group metadata
   */
  public static Map<SchemaPath, ColumnStatistics<?>> getRowGroupColumnStatistics(
      MetadataBase.ParquetTableMetadataBase tableMetadata, MetadataBase.RowGroupMetadata rowGroupMetadata) {

    Map<SchemaPath, ColumnStatistics<?>> columnsStatistics = new HashMap<>();

    for (MetadataBase.ColumnMetadata column : rowGroupMetadata.getColumns()) {
      SchemaPath colPath = SchemaPath.getCompoundPath(column.getName());

      Long nulls = column.getNulls();
      if (hasInvalidStatistics(column, tableMetadata)) {
        nulls = Statistic.NO_COLUMN_STATS;
      }
      PrimitiveType.PrimitiveTypeName primitiveType = getPrimitiveTypeName(tableMetadata, column);
      OriginalType originalType = getOriginalType(tableMetadata, column);
      TypeProtos.MinorType type = ParquetReaderUtility.getMinorType(primitiveType, originalType);

      List<StatisticsHolder<?>> statistics = new ArrayList<>();
      statistics.add(new StatisticsHolder<>(getValue(column.getMinValue(), primitiveType, originalType), ColumnStatisticsKind.MIN_VALUE));
      statistics.add(new StatisticsHolder<>(getValue(column.getMaxValue(), primitiveType, originalType), ColumnStatisticsKind.MAX_VALUE));
      statistics.add(new StatisticsHolder<>(nulls, ColumnStatisticsKind.NULLS_COUNT));
      columnsStatistics.put(colPath, new ColumnStatistics<>(statistics, type));
    }
    return columnsStatistics;
  }

  private static boolean hasInvalidStatistics(MetadataBase.ColumnMetadata column,
        MetadataBase.ParquetTableMetadataBase tableMetadata) {
    return !column.isNumNullsSet() || ((column.getMinValue() == null || column.getMaxValue() == null)
        && column.getNulls() == 0
        && tableMetadata.getRepetition(column.getName()) == Type.Repetition.REQUIRED);
  }

  /**
   * Returns the non-interesting column's metadata
   * @param parquetTableMetadata the source of column metadata for non-interesting column's statistics
   * @return returns non-interesting columns metadata
   */
  public static NonInterestingColumnsMetadata getNonInterestingColumnsMeta(MetadataBase.ParquetTableMetadataBase parquetTableMetadata) {
    Map<SchemaPath, ColumnStatistics<?>> columnsStatistics = new HashMap<>();
    if (parquetTableMetadata instanceof Metadata_V4.ParquetTableMetadata_v4) {
      Map<Metadata_V4.ColumnTypeMetadata_v4.Key, Metadata_V4.ColumnTypeMetadata_v4> columnTypeInfoMap =
              ((Metadata_V4.ParquetTableMetadata_v4) parquetTableMetadata).getColumnTypeInfoMap();

      if (columnTypeInfoMap == null) {
        return new NonInterestingColumnsMetadata(columnsStatistics);
      } // in some cases for runtime pruning

      for (Metadata_V4.ColumnTypeMetadata_v4 columnTypeMetadata : columnTypeInfoMap.values()) {
        if (!columnTypeMetadata.isInteresting) {
          SchemaPath schemaPath = SchemaPath.getCompoundPath(columnTypeMetadata.name);
          List<StatisticsHolder<?>> statistics = new ArrayList<>();
          statistics.add(new StatisticsHolder<>(Statistic.NO_COLUMN_STATS, ColumnStatisticsKind.NULLS_COUNT));
          PrimitiveType.PrimitiveTypeName primitiveType = columnTypeMetadata.primitiveType;
          OriginalType originalType = columnTypeMetadata.originalType;
          TypeProtos.MinorType type = ParquetReaderUtility.getMinorType(primitiveType, originalType);
          columnsStatistics.put(schemaPath, new ColumnStatistics<>(statistics, type));
        }
      }
      return new NonInterestingColumnsMetadata(columnsStatistics);
    }
    return new NonInterestingColumnsMetadata(columnsStatistics);
  }

  /**
   * Handles passed value considering its type and specified {@code primitiveType} with {@code originalType}.
   *
   * @param value         value to handle
   * @param primitiveType primitive type of the column whose value should be handled
   * @param originalType  original type of the column whose value should be handled
   * @return handled value
   */
  public static Object getValue(Object value, PrimitiveType.PrimitiveTypeName primitiveType, OriginalType originalType) {
    if (value != null) {
      switch (primitiveType) {
        case BOOLEAN:
          return Boolean.parseBoolean(value.toString());

        case INT32:
          if (originalType == OriginalType.DATE) {
            return convertToDrillDateValue(getInt(value));
          } else if (originalType == OriginalType.DECIMAL) {
            return BigInteger.valueOf(getInt(value));
          }
          return getInt(value);

        case INT64:
          if (originalType == OriginalType.DECIMAL) {
            return BigInteger.valueOf(getLong(value));
          } else {
            return getLong(value);
          }

        case FLOAT:
          return getFloat(value);

        case DOUBLE:
          return getDouble(value);

        case INT96:
          return new String(getBytes(value));

        case BINARY:
        case FIXED_LEN_BYTE_ARRAY:
          if (originalType == OriginalType.DECIMAL) {
            byte[] bytes = getBytes(value);
            return bytes.length == 0 ? BigInteger.ZERO : new BigInteger(bytes);
          } else if (originalType == OriginalType.INTERVAL) {
            return getBytes(value);
          } else {
            return new String(getBytes(value));
          }
      }
    }
    return null;
  }

  private static byte[] getBytes(Object value) {
    if (value instanceof Binary) {
      return ((Binary) value).getBytes();
    } else if (value instanceof byte[]) {
      return (byte[]) value;
    } else if (value instanceof String) { // value is obtained from metadata cache v2+
      return ((String) value).getBytes();
    } else if (value instanceof Map) { // value is obtained from metadata cache v1
      @SuppressWarnings("unchecked")
      String bytesString = ((Map<String,String>) value).get("bytes");
      if (bytesString != null) {
        return bytesString.getBytes();
      }
    } else if (value instanceof Long) {
      return Longs.toByteArray((Long) value);
    } else if (value instanceof Integer) {
      return Longs.toByteArray((Integer) value);
    } else if (value instanceof Float) {
      return BigDecimal.valueOf((Float) value).unscaledValue().toByteArray();
    } else if (value instanceof Double) {
      return BigDecimal.valueOf((Double) value).unscaledValue().toByteArray();
    }
    throw new UnsupportedOperationException(String.format("Cannot obtain bytes using value %s", value));
  }

  private static Integer getInt(Object value) {
    if (value instanceof Number) {
      return ((Number) value).intValue();
    } else if (value instanceof String) {
      return Integer.parseInt(value.toString());
    } else if (value instanceof byte[]) {
      byte[] bytes = (byte[]) value;
      return bytes.length == 0 ? 0 : new BigInteger(bytes).intValue();
    } else if (value instanceof Binary) {
      byte[] bytes = ((Binary) value).getBytes();
      return bytes.length == 0 ? 0 : new BigInteger(bytes).intValue();
    }
    throw new UnsupportedOperationException(String.format("Cannot obtain Integer using value %s", value));
  }

  private static Long getLong(Object value) {
    if (value instanceof Number) {
      return ((Number) value).longValue();
    } else if (value instanceof String) {
      return Long.parseLong(value.toString());
    } else if (value instanceof byte[]) {
      byte[] bytes = (byte[]) value;
      return bytes.length == 0 ? 0L : new BigInteger(bytes).longValue();
    } else if (value instanceof Binary) {
      byte[] bytes = ((Binary) value).getBytes();
      return bytes.length == 0 ? 0L : new BigInteger(bytes).longValue();
    }
    throw new UnsupportedOperationException(String.format("Cannot obtain Integer using value %s", value));
  }

  private static Float getFloat(Object value) {
    if (value instanceof Number) {
      return ((Number) value).floatValue();
    } else if (value instanceof String) {
      return Float.parseFloat(value.toString());
    }
    // TODO: allow conversion form bytes only when actual type of data is known (to obtain scale)
    /* else if (value instanceof byte[]) {
      return new BigInteger((byte[]) value).floatValue();
    } else if (value instanceof Binary) {
      return new BigInteger(((Binary) value).getBytes()).floatValue();
    }*/
    throw new UnsupportedOperationException(String.format("Cannot obtain Integer using value %s", value));
  }

  private static Double getDouble(Object value) {
    if (value instanceof Number) {
      return ((Number) value).doubleValue();
    } else if (value instanceof String) {
      return Double.parseDouble(value.toString());
    }
    // TODO: allow conversion form bytes only when actual type of data is known (to obtain scale)
    /* else if (value instanceof byte[]) {
      return new BigInteger((byte[]) value).doubleValue();
    } else if (value instanceof Binary) {
      return new BigInteger(((Binary) value).getBytes()).doubleValue();
    }*/
    throw new UnsupportedOperationException(String.format("Cannot obtain Integer using value %s", value));
  }

  private static long convertToDrillDateValue(int dateValue) {
    return dateValue * (long) DateTimeConstants.MILLIS_PER_DAY;
  }

  /**
   * Returns map of column names with their drill types for specified {@code file}.
   *
   * @param parquetTableMetadata the source of primitive and original column types
   * @param file                 file whose columns should be discovered
   * @return map of column names with their drill types
   */
  public static Map<SchemaPath, TypeProtos.MajorType> getFileFields(
    MetadataBase.ParquetTableMetadataBase parquetTableMetadata, MetadataBase.ParquetFileMetadata file) {

    // does not resolve types considering all row groups, just takes type from the first row group.
    return getRowGroupFields(parquetTableMetadata, file.getRowGroups().iterator().next());
  }

  /**
   * Returns map of column names with their drill types for specified {@code rowGroup}.
   *
   * @param parquetTableMetadata the source of primitive and original column types
   * @param rowGroup             row group whose columns should be discovered
   * @return map of column names with their drill types
   */
  public static Map<SchemaPath, TypeProtos.MajorType> getRowGroupFields(
      MetadataBase.ParquetTableMetadataBase parquetTableMetadata, MetadataBase.RowGroupMetadata rowGroup) {
    Map<SchemaPath, TypeProtos.MajorType> columns = new LinkedHashMap<>();
    if (new MetadataVersion(parquetTableMetadata.getMetadataVersion()).isHigherThan(4, 0)
        && !((Metadata_V4.ParquetTableMetadata_v4) parquetTableMetadata).isAllColumnsInteresting()) {
      // adds non-interesting fields from table metadata
      for (MetadataBase.ColumnTypeMetadata columnTypeMetadata : parquetTableMetadata.getColumnTypeInfoList()) {
        Metadata_V4.ColumnTypeMetadata_v4 metadata = (Metadata_V4.ColumnTypeMetadata_v4) columnTypeMetadata;
        if (!metadata.isInteresting) {
          TypeProtos.MajorType columnType = getColumnType(metadata.name, metadata.primitiveType, metadata.originalType, parquetTableMetadata);
          SchemaPath columnPath = SchemaPath.getCompoundPath(metadata.name);
          putType(columns, columnPath, columnType);
        }
      }
    }
    for (MetadataBase.ColumnMetadata column : rowGroup.getColumns()) {

      TypeProtos.MajorType columnType = getColumnType(parquetTableMetadata, column);

      SchemaPath columnPath = SchemaPath.getCompoundPath(column.getName());
      putType(columns, columnPath, columnType);
    }
    return columns;
  }

  private static TypeProtos.MajorType getColumnType(
      MetadataBase.ParquetTableMetadataBase parquetTableMetadata,MetadataBase.ColumnMetadata column) {
    PrimitiveType.PrimitiveTypeName primitiveType = getPrimitiveTypeName(parquetTableMetadata, column);
    OriginalType originalType = getOriginalType(parquetTableMetadata, column);
    String[] name = column.getName();
    return getColumnType(name, primitiveType, originalType, parquetTableMetadata);
  }

  private static TypeProtos.MajorType getColumnType(String[] name,
      PrimitiveType.PrimitiveTypeName primitiveType, OriginalType originalType,
      MetadataBase.ParquetTableMetadataBase parquetTableMetadata) {
    int precision = 0;
    int scale = 0;
    MetadataVersion metadataVersion = new MetadataVersion(parquetTableMetadata.getMetadataVersion());
    // only ColumnTypeMetadata_v3 and ColumnTypeMetadata_v4 store information about scale, precision, repetition level and definition level
    if (metadataVersion.isAtLeast(3, 0)) {
      scale = parquetTableMetadata.getScale(name);
      precision = parquetTableMetadata.getPrecision(name);
    }

    TypeProtos.DataMode mode = getDataMode(parquetTableMetadata, metadataVersion, name);
    return TypeProtos.MajorType.newBuilder(ParquetReaderUtility.getType(primitiveType, originalType, precision, scale))
        .setMode(mode)
        .build();
  }

  /**
   * Obtain data mode from table metadata for a column. Algorithm for retrieving data mode depends on metadata version:
   * <ul>
   *   <li>starting from version {@code 4.2}, Parquet's {@link org.apache.parquet.schema.Type.Repetition}
   *   is stored in table metadata itself;</li>
   *   <li>starting from {@code 3.0} to {@code 4.2} (exclusively) the data mode is
   *   computed based on max {@code definition} and {@code repetition} levels
   *   ({@link MetadataBase.ParquetTableMetadataBase#getDefinitionLevel(String[])} and
   *   {@link MetadataBase.ParquetTableMetadataBase#getRepetitionLevel(String[])} respectively)
   *   obtained from Parquet's schema;
   *
   *   <p><strong>Note:</strong> this computation may lead to erroneous results,
   *   when there are few nesting levels.</p>
   *   </li>
   *   <li>prior to {@code 3.0} {@code DataMode.OPTIONAL} is returned.</li>
   * </ul>
   * @param tableMetadata Parquet table metadata
   * @param metadataVersion version of Parquet table metadata
   * @param name (leaf) column to obtain data mode for
   * @return data mode of the specified column
   */
  private static TypeProtos.DataMode getDataMode(MetadataBase.ParquetTableMetadataBase tableMetadata,
      MetadataVersion metadataVersion, String[] name) {
    TypeProtos.DataMode mode;
    if (metadataVersion.isAtLeast(4, 2)) {
      mode = ParquetReaderUtility.getDataMode(tableMetadata.getRepetition(name));
    } else if (metadataVersion.isAtLeast(3, 0)) {
      int definitionLevel = tableMetadata.getDefinitionLevel(name);
      int repetitionLevel = tableMetadata.getRepetitionLevel(name);

      if (repetitionLevel >= 1) {
        mode = TypeProtos.DataMode.REPEATED;
      } else if (repetitionLevel == 0 && definitionLevel == 0) {
        mode = TypeProtos.DataMode.REQUIRED;
      } else {
        mode = TypeProtos.DataMode.OPTIONAL;
      }
    } else {
      mode = TypeProtos.DataMode.OPTIONAL;
    }

    return mode;
  }

  /**
   * Returns map of column names with their Drill types for every {@code NameSegment} in {@code SchemaPath}
   * in specified {@code rowGroup}. The type for a {@code SchemaPath} can be {@code null} in case when
   * it is not possible to determine its type. Actually, as of now this hierarchy is of interest solely
   * because there is a need to account for {@link org.apache.drill.common.types.TypeProtos.MinorType#DICT}
   * to make sure filters used on {@code DICT}'s values (get by key) are not pruned out before actual filtering
   * happens.
   *
   * @param parquetTableMetadata the source of column types
   * @param rowGroup row group whose columns should be discovered
   * @return map of column names with their drill types
   */
  public static Map<SchemaPath, TypeProtos.MajorType> getIntermediateFields(
      MetadataBase.ParquetTableMetadataBase parquetTableMetadata, MetadataBase.RowGroupMetadata rowGroup) {
    Map<SchemaPath, TypeProtos.MajorType> columns = new LinkedHashMap<>();

    MetadataVersion metadataVersion = new MetadataVersion(parquetTableMetadata.getMetadataVersion());
    boolean hasParentTypes = metadataVersion.isAtLeast(4, 1);

    if (!hasParentTypes) {
      return Collections.emptyMap();
    }

    for (MetadataBase.ColumnMetadata column : rowGroup.getColumns()) {
      Metadata_V4.ColumnTypeMetadata_v4 columnTypeMetadata =
          ((Metadata_V4.ParquetTableMetadata_v4) parquetTableMetadata).getColumnTypeInfo(column.getName());
      List<OriginalType> parentTypes = columnTypeMetadata.parentTypes;
      List<TypeProtos.MajorType> drillTypes = ParquetReaderUtility.getComplexTypes(parentTypes);

      for (int i = 0; i < drillTypes.size(); i++) {
        SchemaPath columnPath = SchemaPath.getCompoundPath(i + 1, column.getName());
        TypeProtos.MajorType drillType = drillTypes.get(i);
        putType(columns, columnPath, drillType);
      }
    }
    return columns;
  }

  /**
   * Returns {@link OriginalType} type for the specified column.
   *
   * @param parquetTableMetadata the source of column type
   * @param column               column whose {@link OriginalType} should be returned
   * @return {@link OriginalType} type for the specified column
   */
  public static OriginalType getOriginalType(MetadataBase.ParquetTableMetadataBase parquetTableMetadata, MetadataBase.ColumnMetadata column) {
    OriginalType originalType = column.getOriginalType();
    // for the case of parquet metadata v1 version, type information isn't stored in parquetTableMetadata, but in ColumnMetadata
    if (originalType == null) {
      originalType = parquetTableMetadata.getOriginalType(column.getName());
    }
    return originalType;
  }

  /**
   * Returns {@link PrimitiveType.PrimitiveTypeName} type for the specified column.
   *
   * @param parquetTableMetadata the source of column type
   * @param column               column whose {@link PrimitiveType.PrimitiveTypeName} should be returned
   * @return {@link PrimitiveType.PrimitiveTypeName} type for the specified column
   */
  public static PrimitiveType.PrimitiveTypeName getPrimitiveTypeName(MetadataBase.ParquetTableMetadataBase parquetTableMetadata, MetadataBase.ColumnMetadata column) {
    PrimitiveType.PrimitiveTypeName primitiveType = column.getPrimitiveType();
    // for the case of parquet metadata v1 version, type information isn't stored in parquetTableMetadata, but in ColumnMetadata
    if (primitiveType == null) {
      primitiveType = parquetTableMetadata.getPrimitiveType(column.getName());
    }
    return primitiveType;
  }

  /**
   * Returns map of column names with their drill types for specified {@code parquetTableMetadata}
   * with resolved types for the case of schema evolution.
   *
   * @param parquetTableMetadata table metadata whose columns should be discovered
   * @return map of column names with their drill types
   */
  static Map<SchemaPath, TypeProtos.MajorType> resolveFields(MetadataBase.ParquetTableMetadataBase parquetTableMetadata) {
    Map<SchemaPath, TypeProtos.MajorType> columns = new LinkedHashMap<>();
    for (MetadataBase.ParquetFileMetadata file : parquetTableMetadata.getFiles()) {
      // row groups in the file have the same schema, so using the first one
      Map<SchemaPath, TypeProtos.MajorType> fileColumns = getFileFields(parquetTableMetadata, file);
      fileColumns.forEach((columnPath, type) -> putType(columns, columnPath, type));
    }
    return columns;
  }

  static Map<SchemaPath, TypeProtos.MajorType> resolveIntermediateFields(MetadataBase.ParquetTableMetadataBase parquetTableMetadata) {
    Map<SchemaPath, TypeProtos.MajorType> columns = new LinkedHashMap<>();
    for (MetadataBase.ParquetFileMetadata file : parquetTableMetadata.getFiles()) {
      // row groups in the file have the same schema, so using the first one
      Map<SchemaPath, TypeProtos.MajorType> fileColumns = getIntermediateFields(parquetTableMetadata, file.getRowGroups().iterator().next());
      fileColumns.forEach((columnPath, type) -> putType(columns, columnPath, type));
    }
    return columns;
  }

  private static void putType(Map<SchemaPath, TypeProtos.MajorType> columns, SchemaPath columnPath, TypeProtos.MajorType type) {
    TypeProtos.MajorType majorType = columns.get(columnPath);
    if (majorType == null) {
      columns.put(columnPath, type);
    } else if (!majorType.equals(type)) {
      TypeProtos.MinorType leastRestrictiveType = TypeCastRules.getLeastRestrictiveType(Arrays.asList(majorType.getMinorType(), type.getMinorType()));
      if (leastRestrictiveType != majorType.getMinorType()) {
        columns.put(columnPath, type);
      }
    }
  }

  /**
   * Returns map with schema path and {@link ColumnStatistics} obtained from specified {@link DrillStatsTable}
   * for all columns from specified {@link BaseTableMetadata}.
   *
   * @param schema     source of column names
   * @param statistics source of column statistics
   * @return map with schema path and {@link ColumnStatistics}
   */
  public static Map<SchemaPath, ColumnStatistics<?>> getColumnStatistics(TupleMetadata schema, DrillStatsTable statistics) {
    List<SchemaPath> schemaPaths = SchemaUtil.getSchemaPaths(schema);

    return schemaPaths.stream()
        .collect(
            Collectors.toMap(
                Function.identity(),
                schemaPath -> new ColumnStatistics<>(
                    DrillStatsTable.getEstimatedColumnStats(statistics, schemaPath),
                    SchemaPathUtils.getColumnMetadata(schemaPath, schema).type())));
  }
}
