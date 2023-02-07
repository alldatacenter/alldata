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

import org.apache.commons.codec.binary.Base64;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.store.parquet.metadata.MetadataBase;
import org.apache.drill.exec.store.parquet.metadata.MetadataVersion;
import org.apache.drill.exec.util.Utilities;
import org.apache.drill.exec.work.ExecErrorConstants;
import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;
import org.apache.hadoop.util.VersionUtil;
import org.apache.parquet.SemanticVersion;
import org.apache.parquet.VersionParser;
import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.column.statistics.IntStatistics;
import org.apache.parquet.format.ConvertedType;
import org.apache.parquet.format.FileMetaData;
import org.apache.parquet.format.SchemaElement;
import org.apache.parquet.format.converter.ParquetMetadataConverter;
import org.apache.parquet.hadoop.ParquetFileWriter;
import org.apache.parquet.hadoop.metadata.ColumnChunkMetaData;
import org.apache.parquet.hadoop.metadata.ColumnPath;
import org.apache.parquet.hadoop.metadata.ParquetMetadata;
import org.apache.parquet.schema.GroupType;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.OriginalType;
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName;
import org.apache.parquet.schema.Type;
import org.joda.time.Chronology;
import org.joda.time.DateTimeConstants;
import org.apache.parquet.example.data.simple.NanoTime;
import org.apache.parquet.io.api.Binary;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.apache.drill.exec.store.parquet.metadata.Metadata_V2.ColumnTypeMetadata_v2;
import static org.apache.drill.exec.store.parquet.metadata.Metadata_V2.ParquetTableMetadata_v2;
import static org.apache.drill.exec.store.parquet.metadata.MetadataBase.ColumnMetadata;
import static org.apache.drill.exec.store.parquet.metadata.MetadataBase.ParquetTableMetadataBase;
import static org.apache.drill.exec.store.parquet.metadata.MetadataBase.ParquetFileMetadata;
import static org.apache.drill.exec.store.parquet.metadata.MetadataBase.RowGroupMetadata;

/**
 * Utility class where we can capture common logic between the two parquet readers
 */
public class ParquetReaderUtility {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ParquetReaderUtility.class);

  /**
   * Number of days between Julian day epoch (January 1, 4713 BC) and Unix day epoch (January 1, 1970).
   * The value of this constant is {@value}.
   */
  public static final long JULIAN_DAY_NUMBER_FOR_UNIX_EPOCH = 2440588;
  /**
   * All old parquet files (which haven't "is.date.correct=true" or "parquet-writer.version" properties
   * in metadata) have a corrupt date shift: {@value} days or 2 * {@value #JULIAN_DAY_NUMBER_FOR_UNIX_EPOCH}
   */
  public static final long CORRECT_CORRUPT_DATE_SHIFT = 2 * JULIAN_DAY_NUMBER_FOR_UNIX_EPOCH;
  private static final Chronology UTC = org.joda.time.chrono.ISOChronology.getInstanceUTC();
  /**
   * The year 5000 (or 1106685 day from Unix epoch) is chosen as the threshold for auto-detecting date corruption.
   * This balances two possible cases of bad auto-correction. External tools writing dates in the future will not
   * be shifted unless they are past this threshold (and we cannot identify them as external files based on the metadata).
   * On the other hand, historical dates written with Drill wouldn't risk being incorrectly shifted unless they were
   * something like 10,000 years in the past.
   */
  public static final int DATE_CORRUPTION_THRESHOLD =
      (int) (UTC.getDateTimeMillis(5000, 1, 1, 0) / DateTimeConstants.MILLIS_PER_DAY);
  /**
   * Version 2 (and later) of the Drill Parquet writer uses the date format described in the
   * <a href="https://github.com/Parquet/parquet-format/blob/master/LogicalTypes.md#date">Parquet spec</a>.
   * Prior versions had dates formatted with {@link org.apache.drill.exec.store.parquet.ParquetReaderUtility#CORRECT_CORRUPT_DATE_SHIFT}
   */
  public static final int DRILL_WRITER_VERSION_STD_DATE_FORMAT = 2;

  public static final String ALLOWED_DRILL_VERSION_FOR_BINARY = "1.15.0";

  /**
   * For most recently created parquet files, we can determine if we have corrupted dates (see DRILL-4203)
   * based on the file metadata. For older files that lack statistics we must actually test the values
   * in the data pages themselves to see if they are likely corrupt.
   */
  public enum DateCorruptionStatus {
    META_SHOWS_CORRUPTION {
      @Override
      public String toString() {
        return "It is determined from metadata that the date values are definitely CORRUPT";
      }
    },
    META_SHOWS_NO_CORRUPTION {
      @Override
      public String toString() {
        return "It is determined from metadata that the date values are definitely CORRECT";
      }
    },
    META_UNCLEAR_TEST_VALUES {
      @Override
      public String toString() {
        return "Not enough info in metadata, parquet reader will test individual date values";
      }
    }
  }

  public static void checkDecimalTypeEnabled(OptionManager options) {
    if (! options.getOption(PlannerSettings.ENABLE_DECIMAL_DATA_TYPE)) {
      throw UserException.unsupportedError()
        .message(ExecErrorConstants.DECIMAL_DISABLE_ERR_MSG)
        .build(logger);
    }
  }

  public static int getIntFromLEBytes(byte[] input, int start) {
    int out = 0;
    int shiftOrder = 0;
    for (int i = start; i < start + 4; i++) {
      out |= (((input[i]) & 0xFF) << shiftOrder);
      shiftOrder += 8;
    }
    return out;
  }

  /**
   * Map full schema paths in format `a`.`b`.`c` to respective SchemaElement objects.
   *
   * @param footer Parquet file metadata
   * @return       schema full path to SchemaElement map
   */
  public static Map<String, SchemaElement> getColNameToSchemaElementMapping(ParquetMetadata footer) {
    Map<String, SchemaElement> schemaElements = new HashMap<>();
    FileMetaData fileMetaData = new ParquetMetadataConverter().toParquetMetadata(ParquetFileWriter.CURRENT_VERSION, footer);

    Iterator<SchemaElement> iter = fileMetaData.getSchema().iterator();

    // First element in collection is default `root` element. We skip it to maintain key in `a` format instead of `root`.`a`,
    // and thus to avoid the need to cut it out again when comparing with SchemaPath string representation
    if (iter.hasNext()) {
      iter.next();
    }
    while (iter.hasNext()) {
      addSchemaElementMapping(iter, new StringBuilder(), schemaElements);
    }
    return schemaElements;
  }

  /**
   * Populate full path to SchemaElement map by recursively traversing schema elements referenced by the given iterator
   *
   * @param iter file schema values iterator
   * @param path parent schema element path
   * @param schemaElements schema elements map to insert next iterator element into
   */
  private static void addSchemaElementMapping(Iterator<SchemaElement> iter, StringBuilder path,
      Map<String, SchemaElement> schemaElements) {
    SchemaElement schemaElement = iter.next();
    path.append('`').append(schemaElement.getName().toLowerCase()).append('`');
    schemaElements.put(path.toString(), schemaElement);

    // for each element that has children we need to maintain remaining children count
    // to exit current recursion level when no more children is left
    int remainingChildren = schemaElement.getNum_children();

    while (remainingChildren > 0 && iter.hasNext()) {
      addSchemaElementMapping(iter, new StringBuilder(path).append('.'), schemaElements);
      remainingChildren--;
    }
    return;
  }

  /**
   * generate full path of the column in format `a`.`b`.`c`
   *
   * @param column ColumnDescriptor object
   * @return       full path in format `a`.`b`.`c`
   */
  public static String getFullColumnPath(ColumnDescriptor column) {
    StringBuilder sb = new StringBuilder();
    String[] path = column.getPath();
    for (int i = 0; i < path.length; i++) {
      sb.append("`").append(path[i].toLowerCase()).append("`").append(".");
    }

    // remove trailing dot
    if (sb.length() > 0) {
      sb.deleteCharAt(sb.length() - 1);
    }

    return sb.toString();
  }

  /**
   * Map full column paths to all ColumnDescriptors in file schema
   *
   * @param footer Parquet file metadata
   * @return       column full path to ColumnDescriptor object map
   */
  public static Map<String, ColumnDescriptor> getColNameToColumnDescriptorMapping(ParquetMetadata footer) {
    Map<String, ColumnDescriptor> colDescMap = new HashMap<>();
    List<ColumnDescriptor> columns = footer.getFileMetaData().getSchema().getColumns();

    for (ColumnDescriptor column : columns) {
      colDescMap.put(getFullColumnPath(column), column);
    }
    return colDescMap;
  }

  public static int autoCorrectCorruptedDate(int corruptedDate) {
    return (int) (corruptedDate - CORRECT_CORRUPT_DATE_SHIFT);
  }

  public static void correctDatesInMetadataCache(ParquetTableMetadataBase parquetTableMetadata) {
    MetadataVersion metadataVersion = new MetadataVersion(parquetTableMetadata.getMetadataVersion());
    DateCorruptionStatus cacheFileCanContainsCorruptDates =
        metadataVersion.isAtLeast(3, 0) ?
        DateCorruptionStatus.META_SHOWS_NO_CORRUPTION : DateCorruptionStatus.META_UNCLEAR_TEST_VALUES;
    if (cacheFileCanContainsCorruptDates == DateCorruptionStatus.META_UNCLEAR_TEST_VALUES) {
      // Looking for the DATE data type of column names in the metadata cache file ("metadata_version" : "v2")
      String[] names = new String[0];
      if (metadataVersion.isEqualTo(2, 0)) {
        for (ColumnTypeMetadata_v2 columnTypeMetadata :
            ((ParquetTableMetadata_v2) parquetTableMetadata).columnTypeInfo.values()) {
          if (OriginalType.DATE.equals(columnTypeMetadata.originalType)) {
            names = columnTypeMetadata.name;
          }
        }
      }
      for (ParquetFileMetadata file : parquetTableMetadata.getFiles()) {
        // Drill has only ever written a single row group per file, only need to correct the statistics
        // on the first row group
        RowGroupMetadata rowGroupMetadata = file.getRowGroups().get(0);
        Long rowCount = rowGroupMetadata.getRowCount();
        for (ColumnMetadata columnMetadata : rowGroupMetadata.getColumns()) {
          // Setting Min/Max values for ParquetTableMetadata_v1
          if (metadataVersion.isEqualTo(1, 0)) {
            OriginalType originalType = columnMetadata.getOriginalType();
            if (OriginalType.DATE.equals(originalType) && columnMetadata.hasSingleValue(rowCount) &&
                (Integer) columnMetadata.getMaxValue() > ParquetReaderUtility.DATE_CORRUPTION_THRESHOLD) {
              int newMinMax = ParquetReaderUtility.autoCorrectCorruptedDate((Integer) columnMetadata.getMaxValue());
              columnMetadata.setMax(newMinMax);
              columnMetadata.setMin(newMinMax);
            }
          }
          // Setting Max values for ParquetTableMetadata_v2
          else if (metadataVersion.isEqualTo(2, 0) &&
                   columnMetadata.getName() != null &&
                   Arrays.equals(columnMetadata.getName(), names) &&
                   columnMetadata.hasSingleValue(rowCount) &&
                   (Integer) columnMetadata.getMaxValue() > ParquetReaderUtility.DATE_CORRUPTION_THRESHOLD) {
            int newMax = ParquetReaderUtility.autoCorrectCorruptedDate((Integer) columnMetadata.getMaxValue());
            columnMetadata.setMax(newMax);
          }
        }
      }
    }
  }

  /**
   *
   * Transforms values for min / max binary statistics to byte array.
   * Transformation logic depends on metadata file version.
   *
   * @param parquetTableMetadata table metadata that should be corrected
   * @param readerConfig parquet reader config
   */
  public static void transformBinaryInMetadataCache(ParquetTableMetadataBase parquetTableMetadata, ParquetReaderConfig readerConfig) {
    // Looking for the names of the columns with BINARY data type
    // in the metadata cache file for V2 and all v3 versions
    Set<List<String>> columnsNames = getBinaryColumnsNames(parquetTableMetadata);
    boolean allowBinaryMetadata = allowBinaryMetadata(parquetTableMetadata.getDrillVersion(), readerConfig);

    // Setting Min / Max values for ParquetTableMetadata_v1
    MetadataVersion metadataVersion = new MetadataVersion(parquetTableMetadata.getMetadataVersion());
    if (metadataVersion.isEqualTo(1, 0)) {
      for (ParquetFileMetadata file : parquetTableMetadata.getFiles()) {
        for (RowGroupMetadata rowGroupMetadata : file.getRowGroups()) {
          Long rowCount = rowGroupMetadata.getRowCount();
          for (ColumnMetadata columnMetadata : rowGroupMetadata.getColumns()) {
            if (columnMetadata.getPrimitiveType() == PrimitiveTypeName.BINARY || columnMetadata.getPrimitiveType() == PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY) {
              setMinMaxValues(columnMetadata, rowCount, allowBinaryMetadata, false);
            }
          }
        }
      }
      return;
    }

    // Variables needed for debugging only
    Stopwatch timer = logger.isDebugEnabled() ? Stopwatch.createStarted() : null;
    int maxRowGroups = 0;
    int minRowGroups = Integer.MAX_VALUE;
    int maxNumColumns = 0;

    // Setting Min / Max values for V2, V3 and V4 versions; for versions V3_3 and above need to do decoding
    boolean needDecoding = metadataVersion.isAtLeast(3, 3);
    for (ParquetFileMetadata file : parquetTableMetadata.getFiles()) {
      if ( timer != null ) { // for debugging only
        maxRowGroups = Math.max(maxRowGroups, file.getRowGroups().size());
        minRowGroups = Math.min(minRowGroups, file.getRowGroups().size());
      }
      for (RowGroupMetadata rowGroupMetadata : file.getRowGroups()) {
        Long rowCount = rowGroupMetadata.getRowCount();
        if ( timer != null ) { // for debugging only
          maxNumColumns = Math.max(maxNumColumns, rowGroupMetadata.getColumns().size());
        }
        for (ColumnMetadata columnMetadata : rowGroupMetadata.getColumns()) {
           if (columnsNames.contains(Arrays.asList(columnMetadata.getName()))) {
            setMinMaxValues(columnMetadata, rowCount, allowBinaryMetadata, needDecoding);
          }
        }
      }
    }

    if (timer != null) { // log a debug message and stop the timer
      String reportRG = 1 == maxRowGroups ? "1 rowgroup" : "between " + minRowGroups + "-" + maxRowGroups + "rowgroups";
      logger.debug("Transforming binary in metadata cache took {} ms ({} files, {} per file, max {} columns)", timer.elapsed(TimeUnit.MILLISECONDS),
        parquetTableMetadata.getFiles().size(), reportRG, maxNumColumns);
      timer.stop();
    }
  }

  /**
   * If binary metadata was stored prior to Drill version {@link #ALLOWED_DRILL_VERSION_FOR_BINARY},
   * it might have incorrectly defined min / max values.
   * In case if given version is null, we assume this version is prior to {@link #ALLOWED_DRILL_VERSION_FOR_BINARY}.
   * In this case we allow reading such metadata only if {@link ParquetReaderConfig#enableStringsSignedMinMax()} is true.
   *
   * @param drillVersion drill version used to create metadata file
   * @param readerConfig parquet reader configuration
   * @return true if reading binary min / max values are allowed, false otherwise
   */
  private static boolean allowBinaryMetadata(String drillVersion, ParquetReaderConfig readerConfig) {
    return readerConfig.enableStringsSignedMinMax() ||
      (drillVersion != null && VersionUtil.compareVersions(ALLOWED_DRILL_VERSION_FOR_BINARY, drillVersion) <= 0);
  }

  /**
   * Returns the set of the lists with names of the columns with BINARY or
   * FIXED_LEN_BYTE_ARRAY data type from {@code ParquetTableMetadataBase columnTypeMetadataCollection}
   * if parquetTableMetadata has version v2 or v3 (including minor versions).
   *
   * @param parquetTableMetadata table metadata the source of the columns to check
   * @return set of the lists with column names
   */
  private static Set<List<String>> getBinaryColumnsNames(ParquetTableMetadataBase parquetTableMetadata) {
    Set<List<String>> names = new HashSet<>();
    List<? extends MetadataBase.ColumnTypeMetadata> columnTypeMetadataList = parquetTableMetadata.getColumnTypeInfoList();
    if (columnTypeMetadataList != null) {
      for (MetadataBase.ColumnTypeMetadata columnTypeMetadata : columnTypeMetadataList) {
        if (columnTypeMetadata.getPrimitiveType() == PrimitiveTypeName.BINARY
                || columnTypeMetadata.getPrimitiveType() == PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY) {
          names.add(Arrays.asList(columnTypeMetadata.getName()));
        }
      }
    }
    return names;
  }

  /**
   * If binary metadata is not allowed (logic is defined in
   * {@link ParquetReaderUtility#allowBinaryMetadata(String, ParquetReaderConfig)}),
   * set min max values for binary column, only if min and max values are the same,
   * otherwise set min and max as nulls.
   *
   * @param columnMetadata column metadata that should be changed
   * @param rowCount rows count in column chunk
   * @param allowBinaryMetadata if reading binary metadata is allowed
   * @param needsDecoding if min and max values is Base64 data and should be decoded
   */
  private static void setMinMaxValues(ColumnMetadata columnMetadata, long rowCount, boolean allowBinaryMetadata, boolean needsDecoding) {
    byte[] minBytes = null;
    byte[] maxBytes = null;

    boolean hasSingleValue = false;
    if (allowBinaryMetadata || (hasSingleValue = columnMetadata.hasSingleValue(rowCount))) {
      Object minValue = columnMetadata.getMinValue();
      Object maxValue = columnMetadata.getMaxValue();
      if (minValue instanceof String && maxValue instanceof String) {
        minBytes = ((String) minValue).getBytes();
        maxBytes = ((String) maxValue).getBytes();
        if (needsDecoding) {
          minBytes = Base64.decodeBase64(minBytes);
          maxBytes = hasSingleValue ? minBytes : Base64.decodeBase64(maxBytes);
        }
      } else if (minValue instanceof Binary && maxValue instanceof Binary) {
        // for the case when cache file was auto-refreshed, values from parquet footers are used,
        // so there is no need to convert values, but they should be set in ColumnMetadata
        minBytes = ((Binary) minValue).getBytes();
        maxBytes = ((Binary) maxValue).getBytes();
      }
    }

    columnMetadata.setMin(minBytes);
    columnMetadata.setMax(maxBytes);
  }

  /**
   * Check for corrupted dates in a parquet file. See Drill-4203
   */
  public static DateCorruptionStatus detectCorruptDates(ParquetMetadata footer,
                                           List<SchemaPath> columns,
                                           boolean autoCorrectCorruptDates) {
    // old drill files have "parquet-mr" as created by string, and no drill version, need to check min/max values to see
    // if they look corrupt
    //  - option to disable this auto-correction based on the date values, in case users are storing these
    //    dates intentionally

    // migrated parquet files have 1.8.1 parquet-mr version with drill-r0 in the part of the name usually containing "SNAPSHOT"

    // new parquet files are generated with "is.date.correct" property have no corruption dates

    String createdBy = footer.getFileMetaData().getCreatedBy();
    String drillVersion = footer.getFileMetaData().getKeyValueMetaData().get(ParquetRecordWriter.DRILL_VERSION_PROPERTY);
    String writerVersionValue = footer.getFileMetaData().getKeyValueMetaData().get(ParquetRecordWriter.WRITER_VERSION_PROPERTY);
    // This flag can be present in parquet files which were generated with 1.9.0-SNAPSHOT and 1.9.0 drill versions.
    // If this flag is present it means that the version of the drill parquet writer is 2
    final String isDateCorrectFlag = "is.date.correct";
    String isDateCorrect = footer.getFileMetaData().getKeyValueMetaData().get(isDateCorrectFlag);
    if (drillVersion != null) {
      int writerVersion = 1;
      if (writerVersionValue != null) {
        writerVersion = Integer.parseInt(writerVersionValue);
      }
      else if (Boolean.valueOf(isDateCorrect)) {
        writerVersion = DRILL_WRITER_VERSION_STD_DATE_FORMAT;
      }
      return writerVersion >= DRILL_WRITER_VERSION_STD_DATE_FORMAT ? DateCorruptionStatus.META_SHOWS_NO_CORRUPTION
          // loop through parquet column metadata to find date columns, check for corrupt values
          : checkForCorruptDateValuesInStatistics(footer, columns, autoCorrectCorruptDates);
    } else {
      // Possibly an old, un-migrated Drill file, check the column statistics to see if min/max values look corrupt
      // only applies if there is a date column selected
      if (createdBy == null || createdBy.equals("parquet-mr")) {
        return checkForCorruptDateValuesInStatistics(footer, columns, autoCorrectCorruptDates);
      } else {
        // check the created by to see if it is a migrated Drill file
        try {
          VersionParser.ParsedVersion parsedCreatedByVersion = VersionParser.parse(createdBy);
          // check if this is a migrated Drill file, lacking a Drill version number, but with
          // "drill" in the parquet created-by string
          if (parsedCreatedByVersion.hasSemanticVersion()) {
            SemanticVersion semVer = parsedCreatedByVersion.getSemanticVersion();
            String pre = semVer.pre + "";
            if (semVer.major == 1 && semVer.minor == 8 && semVer.patch == 1 && pre.contains("drill")) {
              return checkForCorruptDateValuesInStatistics(footer, columns, autoCorrectCorruptDates);
            }
          }
          // written by a tool that wasn't Drill, the dates are not corrupted
          return DateCorruptionStatus.META_SHOWS_NO_CORRUPTION;
        } catch (VersionParser.VersionParseException e) {
          // If we couldn't parse "created by" field, check column metadata of date columns
          return checkForCorruptDateValuesInStatistics(footer, columns, autoCorrectCorruptDates);
        }
      }
    }
  }

  /**
   * Detect corrupt date values by looking at the min/max values in the metadata.
   *
   * This should only be used when a file does not have enough metadata to determine if
   * the data was written with an external tool or an older version of Drill
   * ({@link org.apache.drill.exec.store.parquet.ParquetRecordWriter#WRITER_VERSION_PROPERTY} <
   * {@link org.apache.drill.exec.store.parquet.ParquetReaderUtility#DRILL_WRITER_VERSION_STD_DATE_FORMAT})
   *
   * This method only checks the first Row Group, because Drill has only ever written
   * a single Row Group per file.
   *
   * @param footer parquet footer
   * @param columns list of columns schema path
   * @param autoCorrectCorruptDates user setting to allow enabling/disabling of auto-correction
   *                                of corrupt dates. There are some rare cases (storing dates thousands
   *                                of years into the future, with tools other than Drill writing files)
   *                                that would result in the date values being "corrected" into bad values.
   */
  public static DateCorruptionStatus checkForCorruptDateValuesInStatistics(ParquetMetadata footer,
                                                              List<SchemaPath> columns,
                                                              boolean autoCorrectCorruptDates) {
    // Users can turn-off date correction in cases where we are detecting corruption based on the date values
    // that are unlikely to appear in common datasets. In this case report that no correction needs to happen
    // during the file read
    if (! autoCorrectCorruptDates) {
      return DateCorruptionStatus.META_SHOWS_NO_CORRUPTION;
    }
    // Drill produced files have only ever have a single row group, if this changes in the future it won't matter
    // as we will know from the Drill version written in the files that the dates are correct
    int rowGroupIndex = 0;
    Map<String, SchemaElement> schemaElements = ParquetReaderUtility.getColNameToSchemaElementMapping(footer);
    findDateColWithStatsLoop : for (SchemaPath schemaPath : columns) {
      List<ColumnDescriptor> parquetColumns = footer.getFileMetaData().getSchema().getColumns();
      for (int i = 0; i < parquetColumns.size(); ++i) {
        ColumnDescriptor column = parquetColumns.get(i);
        // this reader only supports flat data, this is restricted in the ParquetScanBatchCreator
        // creating a NameSegment makes sure we are using the standard code for comparing names,
        // currently it is all case-insensitive
        if (Utilities.isStarQuery(columns)
            || getFullColumnPath(column).equalsIgnoreCase(schemaPath.getUnIndexed().toString())) {
          int colIndex = -1;
          ConvertedType convertedType = schemaElements.get(getFullColumnPath(column)).getConverted_type();
          if (convertedType != null && convertedType.equals(ConvertedType.DATE)) {
            List<ColumnChunkMetaData> colChunkList = footer.getBlocks().get(rowGroupIndex).getColumns();
            for (int j = 0; j < colChunkList.size(); j++) {
              if (colChunkList.get(j).getPath().equals(ColumnPath.get(column.getPath()))) {
                colIndex = j;
                break;
              }
            }
          }
          if (colIndex == -1) {
            // column does not appear in this file, skip it
            continue;
          }
          IntStatistics statistics = (IntStatistics) footer.getBlocks().get(rowGroupIndex).getColumns().get(colIndex).getStatistics();
          return (statistics.hasNonNullValue() && statistics.compareMaxToValue(ParquetReaderUtility.DATE_CORRUPTION_THRESHOLD) > 0) ?
              DateCorruptionStatus.META_SHOWS_CORRUPTION : DateCorruptionStatus.META_UNCLEAR_TEST_VALUES;
        }
      }
    }
    return DateCorruptionStatus.META_SHOWS_NO_CORRUPTION;
  }

  /**
   * Utilities for converting from parquet INT96 binary (impala, hive timestamp)
   * to date time value. This utilizes the Joda library.
   */
  public static class NanoTimeUtils {

    public static final long NANOS_PER_MILLISECOND = 1000000;

  /**
   * @param binaryTimeStampValue
   *          hive, impala timestamp values with nanoseconds precision
   *          are stored in parquet Binary as INT96 (12 constant bytes)
   * @param retainLocalTimezone
   *          parquet files don't keep local timeZone according to the
   *          <a href="https://github.com/Parquet/parquet-format/blob/master/LogicalTypes.md#timestamp">Parquet spec</a>,
   *          but some tools (hive, for example) retain local timezone for parquet files by default
   *          Note: Impala doesn't retain local timezone by default
   * @return  Timestamp in milliseconds - the number of milliseconds since January 1, 1970, 00:00:00 GMT
   *          represented by @param binaryTimeStampValue.
   *          The nanos precision is cut to millis. Therefore the length of single timestamp value is
   *          {@value org.apache.drill.exec.expr.holders.NullableTimeStampHolder#WIDTH} bytes instead of 12 bytes.
   */
    public static long getDateTimeValueFromBinary(Binary binaryTimeStampValue, boolean retainLocalTimezone) {
      // This method represents binaryTimeStampValue as ByteBuffer, where timestamp is stored as sum of
      // julian day number (4 bytes) and nanos of day (8 bytes)
      NanoTime nt = NanoTime.fromBinary(binaryTimeStampValue);
      int julianDay = nt.getJulianDay();
      long nanosOfDay = nt.getTimeOfDayNanos();
      long dateTime = (julianDay - JULIAN_DAY_NUMBER_FOR_UNIX_EPOCH) * DateTimeConstants.MILLIS_PER_DAY
          + nanosOfDay / NANOS_PER_MILLISECOND;
      if (retainLocalTimezone) {
        return DateTimeZone.getDefault().convertUTCToLocal(dateTime);
      } else {
        return dateTime;
      }
    }
  }

  /**
   * Builds major type using given {@code OriginalType originalType} or {@code PrimitiveTypeName type}.
   * For DECIMAL will be returned major type with scale and precision.
   *
   * @param type         parquet primitive type
   * @param originalType parquet original type
   * @param scale        type scale (used for DECIMAL type)
   * @param precision    type precision (used for DECIMAL type)
   * @return major type
   */
  public static TypeProtos.MajorType getType(PrimitiveTypeName type, OriginalType originalType, int precision, int scale) {
    TypeProtos.MinorType minorType = getMinorType(type, originalType);
    if (originalType == OriginalType.DECIMAL) {
      return Types.withPrecisionAndScale(minorType, TypeProtos.DataMode.OPTIONAL, precision, scale);
    }

    return Types.optional(minorType);
  }

  /**
   * Builds minor type using given {@code OriginalType originalType} or {@code PrimitiveTypeName type}.
   *
   * @param type         parquet primitive type
   * @param originalType parquet original type
   * @return minor type
   */
  public static TypeProtos.MinorType getMinorType(PrimitiveTypeName type, OriginalType originalType) {
    if (originalType != null) {
      switch (originalType) {
        case DECIMAL:
          return TypeProtos.MinorType.VARDECIMAL;
        case DATE:
          return TypeProtos.MinorType.DATE;
        case TIME_MILLIS:
        case TIME_MICROS:
          return TypeProtos.MinorType.TIME;
        case TIMESTAMP_MILLIS:
        case TIMESTAMP_MICROS:
          return TypeProtos.MinorType.TIMESTAMP;
        case UTF8:
          return TypeProtos.MinorType.VARCHAR;
        case UINT_8:
          return TypeProtos.MinorType.UINT1;
        case UINT_16:
          return TypeProtos.MinorType.UINT2;
        case UINT_32:
          return TypeProtos.MinorType.UINT4;
        case UINT_64:
          return TypeProtos.MinorType.UINT8;
        case INT_8:
          return TypeProtos.MinorType.TINYINT;
        case INT_16:
          return TypeProtos.MinorType.SMALLINT;
        case INTERVAL:
          return TypeProtos.MinorType.INTERVAL;
      }
    }

    switch (type) {
      case BOOLEAN:
        return TypeProtos.MinorType.BIT;
      case INT32:
        return TypeProtos.MinorType.INT;
      case INT64:
        return TypeProtos.MinorType.BIGINT;
      case FLOAT:
        return TypeProtos.MinorType.FLOAT4;
      case DOUBLE:
        return TypeProtos.MinorType.FLOAT8;
      case BINARY:
      case FIXED_LEN_BYTE_ARRAY:
      case INT96:
        return TypeProtos.MinorType.VARBINARY;
      default:
        // Should never hit this
        throw new UnsupportedOperationException("Unsupported type:" + type);
    }
  }

  /**
   * Check whether any of columns in the given list is either nested or repetitive.
   *
   * @param footer  Parquet file schema
   * @param columns list of query SchemaPath objects
   */
  public static boolean containsComplexColumn(ParquetMetadata footer, List<SchemaPath> columns) {

    MessageType schema = footer.getFileMetaData().getSchema();

    if (Utilities.isStarQuery(columns)) {
      for (Type type : schema.getFields()) {
        if (!type.isPrimitive()) {
          return true;
        }
      }
      for (ColumnDescriptor col : schema.getColumns()) {
        if (col.getMaxRepetitionLevel() > 0) {
          return true;
        }
      }
      return false;
    } else {
      Map<String, ColumnDescriptor> colDescMap = ParquetReaderUtility.getColNameToColumnDescriptorMapping(footer);
      Map<String, SchemaElement> schemaElements = ParquetReaderUtility.getColNameToSchemaElementMapping(footer);

      for (SchemaPath schemaPath : columns) {
        // Schema path which is non-leaf is complex column
        if (!schemaPath.isLeaf()) {
          logger.trace("rowGroupScan contains complex column: {}", schemaPath.getUnIndexed().toString());
          return true;
        }

        // following column descriptor lookup failure may mean two cases, depending on subsequent SchemaElement lookup:
        // 1. success: queried column is complex, i.e. GroupType
        // 2. failure: queried column is not in schema and thus is non-complex
        ColumnDescriptor column = colDescMap.get(schemaPath.getUnIndexed().toString().toLowerCase());

        if (column == null) {
          SchemaElement schemaElement = schemaElements.get(schemaPath.getUnIndexed().toString().toLowerCase());
          if (schemaElement != null) {
            return true;
          }
        } else {
          if (column.getMaxRepetitionLevel() > 0) {
            logger.trace("rowGroupScan contains repetitive column: {}", schemaPath.getUnIndexed().toString());
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Converts list of {@link OriginalType}s to list of {@link org.apache.drill.common.types.TypeProtos.MajorType}s.
   * <b>NOTE</b>: current implementation cares about {@link OriginalType#MAP} and {@link OriginalType#LIST} only
   * converting it to {@link org.apache.drill.common.types.TypeProtos.MinorType#DICT}
   * and {@link org.apache.drill.common.types.TypeProtos.MinorType#LIST} respectively.
   * Other original types are converted to {@code null}, because there is no certain correspondence
   * (and, actually, a need because these types are used to differentiate between Drill's MAP and DICT (and arrays of thereof) types
   * when constructing {@link org.apache.drill.exec.record.metadata.TupleSchema}) between these two.
   *
   * @param originalTypes list of Parquet's types
   * @return list containing either {@code null} or type with minor
   *         type {@link org.apache.drill.common.types.TypeProtos.MinorType#DICT} or
   *         {@link org.apache.drill.common.types.TypeProtos.MinorType#LIST} values
   */
  public static List<TypeProtos.MajorType> getComplexTypes(List<OriginalType> originalTypes) {
    List<TypeProtos.MajorType> result = new ArrayList<>();
    if (originalTypes == null) {
      return result;
    }
    for (OriginalType type : originalTypes) {
      if (type == OriginalType.MAP) {
        result.add(Types.required(TypeProtos.MinorType.DICT));
      } else if (type == OriginalType.LIST) {
        result.add(Types.required(TypeProtos.MinorType.LIST));
      } else {
        result.add(null);
      }
    }

    return result;
  }

  /**
   * Checks whether group field approximately matches pattern for Logical Lists:
   * <pre>
   * &lt;list-repetition&gt; group &lt;name&gt; (LIST) {
   *   repeated group list {
   *     &lt;element-repetition&gt; &lt;element-type&gt; element;
   *   }
   * }
   * </pre>
   * (See for more details: https://github.com/apache/parquet-format/blob/master/LogicalTypes.md#lists)
   *
   * Note, that standard field names 'list' and 'element' aren't checked intentionally,
   * because Hive lists have 'bag' and 'array_element' names instead.
   *
   * @param groupType type which may have LIST original type
   * @return whether the type is LIST and nested field is repeated group
   * @see <a href="https://github.com/apache/parquet-format/blob/master/LogicalTypes.md#lists">Parquet List logical type</a>
   */
  public static boolean isLogicalListType(GroupType groupType) {
    if (groupType.getOriginalType() == OriginalType.LIST && groupType.getFieldCount() == 1) {
      Type nestedField = groupType.getFields().get(0);
      return nestedField.isRepetition(Type.Repetition.REPEATED)
          && !nestedField.isPrimitive()
          && nestedField.getOriginalType() == null
          && nestedField.asGroupType().getFieldCount() == 1;
    }
    return false;
  }

  /**
   * Checks whether group field matches pattern for Logical Map type:
   *
   * <pre>
   * &lt;map-repetition&gt; group &lt;name&gt; (MAP) {
   *   repeated group key_value {
   *     required &lt;key-type&gt; key;
   *     &lt;value-repetition&gt; &lt;value-type&gt; value;
   *   }
   * }
   * </pre>
   *
   * Note, that actual group names are not checked specifically.
   *
   * @param groupType parquet type which may be of MAP type
   * @return whether the type is MAP
   * @see <a href="https://github.com/apache/parquet-format/blob/master/LogicalTypes.md#maps">Parquet Map logical type</a>
   */
  public static boolean isLogicalMapType(GroupType groupType) {
    OriginalType type = groupType.getOriginalType();
    // MAP_KEY_VALUE is here for backward-compatibility reasons
    if ((type == OriginalType.MAP || type == OriginalType.MAP_KEY_VALUE)
        && groupType.getFieldCount() == 1) {
      Type nestedField = groupType.getFields().get(0);
      return nestedField.isRepetition(Type.Repetition.REPEATED)
          && !nestedField.isPrimitive()
          && nestedField.asGroupType().getFieldCount() == 2;
    }
    return false;
  }

  /**
   * Converts Parquet's {@link Type.Repetition} to Drill's {@link TypeProtos.DataMode}.
   * @param repetition repetition to be converted
   * @return data mode corresponding to Parquet's repetition
   */
  public static TypeProtos.DataMode getDataMode(Type.Repetition repetition) {
    TypeProtos.DataMode mode;
    switch (repetition) {
      case REPEATED:
        mode = TypeProtos.DataMode.REPEATED;
        break;
      case OPTIONAL:
        mode = TypeProtos.DataMode.OPTIONAL;
        break;
      case REQUIRED:
        mode = TypeProtos.DataMode.REQUIRED;
        break;
      default:
        throw new IllegalArgumentException(String.format("Unknown Repetition: %s.", repetition));
    }
    return mode;
  }
}
