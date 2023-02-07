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
package org.apache.drill.exec.store.hive;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.planner.sql.TypeInferenceUtils;
import org.apache.drill.exec.planner.types.HiveToRelDataTypeConverter;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.MapColumnMetadata;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.PrimitiveColumnMetadata;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.metadata.TupleSchema;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.base.Strings;
import io.netty.buffer.DrillBuf;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.expr.holders.Decimal18Holder;
import org.apache.drill.exec.expr.holders.Decimal28SparseHolder;
import org.apache.drill.exec.expr.holders.Decimal38SparseHolder;
import org.apache.drill.exec.expr.holders.Decimal9Holder;
import org.apache.drill.exec.planner.logical.DrillScanRel;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.server.options.OptionSet;
import org.apache.drill.exec.util.DecimalUtility;
import org.apache.drill.exec.vector.NullableBigIntVector;
import org.apache.drill.exec.vector.NullableBitVector;
import org.apache.drill.exec.vector.NullableDateVector;
import org.apache.drill.exec.vector.NullableDecimal18Vector;
import org.apache.drill.exec.vector.NullableDecimal28SparseVector;
import org.apache.drill.exec.vector.NullableDecimal38SparseVector;
import org.apache.drill.exec.vector.NullableDecimal9Vector;
import org.apache.drill.exec.vector.NullableFloat4Vector;
import org.apache.drill.exec.vector.NullableFloat8Vector;
import org.apache.drill.exec.vector.NullableIntVector;
import org.apache.drill.exec.vector.NullableTimeStampVector;
import org.apache.drill.exec.vector.NullableVarBinaryVector;
import org.apache.drill.exec.vector.NullableVarCharVector;
import org.apache.drill.exec.vector.NullableVarDecimalVector;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.work.ExecErrorConstants;

import org.apache.hadoop.hive.common.type.HiveDecimal;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.hadoop.hive.ql.exec.Utilities;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.ql.io.AcidUtils;
import org.apache.hadoop.hive.ql.io.IOConstants;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.metadata.HiveStorageHandler;
import org.apache.hadoop.hive.ql.metadata.HiveUtils;
import org.apache.hadoop.hive.ql.plan.TableDesc;
import org.apache.hadoop.hive.serde.serdeConstants;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector.Category;
import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector.PrimitiveCategory;
import org.apache.hadoop.hive.serde2.typeinfo.BaseCharTypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.DecimalTypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.HiveDecimalUtils;
import org.apache.hadoop.hive.serde2.typeinfo.PrimitiveTypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfoUtils;
import org.apache.hadoop.mapred.InputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.apache.hadoop.hive.metastore.api.hive_metastoreConstants.META_TABLE_STORAGE;

public class HiveUtilities {
  private static final Logger logger = LoggerFactory.getLogger(HiveUtilities.class);

  /**
   * Partition value is received in string format. Convert it into appropriate object based on the type.
   *
   * @param typeInfo type info
   * @param value partition values
   * @param defaultPartitionValue default partition value
   * @return converted object
   */
  public static Object convertPartitionType(TypeInfo typeInfo, String value, final String defaultPartitionValue) {
    if (typeInfo.getCategory() != Category.PRIMITIVE) {
      // In Hive only primitive types are allowed as partition column types.
      throw new DrillRuntimeException("Non-Primitive types are not allowed as partition column type in Hive, " +
          "but received one: " + typeInfo.getCategory());
    }

    if (defaultPartitionValue.equals(value)) {
      return null;
    }

    final PrimitiveCategory pCat = ((PrimitiveTypeInfo) typeInfo).getPrimitiveCategory();

    try {
      switch (pCat) {
        case BINARY:
          return value.getBytes();
        case BOOLEAN:
          return Boolean.parseBoolean(value);
        case DECIMAL: {
          DecimalTypeInfo decimalTypeInfo = (DecimalTypeInfo) typeInfo;
          return HiveDecimalUtils.enforcePrecisionScale(HiveDecimal.create(value), decimalTypeInfo);
        }
        case DOUBLE:
          return Double.parseDouble(value);
        case FLOAT:
          return Float.parseFloat(value);
        case BYTE:
        case SHORT:
        case INT:
          return Integer.parseInt(value);
        case LONG:
          return Long.parseLong(value);
        case STRING:
        case VARCHAR:
          return value.getBytes();
        case CHAR:
          return value.trim().getBytes();
        case TIMESTAMP:
          return Timestamp.valueOf(value);
        case DATE:
          return Date.valueOf(value);
      }
    } catch(final Exception e) {
      // In Hive, partition values that can't be converted from string are considered to be NULL.
      logger.trace("Failed to interpret '{}' value from partition value string '{}'", pCat, value);
      return null;
    }

    throwUnsupportedHiveDataTypeError(pCat.toString());
    return null;
  }

  /**
   * Populates vector with given value based on its type.
   *
   * @param vector vector instance
   * @param managedBuffer Drill duffer
   * @param val value
   * @param start start position
   * @param end end position
   */
  public static void populateVector(final ValueVector vector, final DrillBuf managedBuffer, final Object val,
      final int start, final int end) {
    TypeProtos.MinorType type = vector.getField().getType().getMinorType();

    switch(type) {
      case VARBINARY: {
        NullableVarBinaryVector v = (NullableVarBinaryVector) vector;
        byte[] value = (byte[]) val;
        for (int i = start; i < end; i++) {
          v.getMutator().setSafe(i, value, 0, value.length);
        }
        break;
      }
      case BIT: {
        NullableBitVector v = (NullableBitVector) vector;
        Boolean value = (Boolean) val;
        for (int i = start; i < end; i++) {
          v.getMutator().set(i, value ? 1 : 0);
        }
        break;
      }
      case FLOAT8: {
        NullableFloat8Vector v = (NullableFloat8Vector) vector;
        double value = (double) val;
        for (int i = start; i < end; i++) {
          v.getMutator().setSafe(i, value);
        }
        break;
      }
      case FLOAT4: {
        NullableFloat4Vector v = (NullableFloat4Vector) vector;
        float value = (float) val;
        for (int i = start; i < end; i++) {
          v.getMutator().setSafe(i, value);
        }
        break;
      }
      case TINYINT:
      case SMALLINT:
      case INT: {
        NullableIntVector v = (NullableIntVector) vector;
        int value = (int) val;
        for (int i = start; i < end; i++) {
          v.getMutator().setSafe(i, value);
        }
        break;
      }
      case BIGINT: {
        NullableBigIntVector v = (NullableBigIntVector) vector;
        long value = (long) val;
        for (int i = start; i < end; i++) {
          v.getMutator().setSafe(i, value);
        }
        break;
      }
      case VARCHAR: {
        NullableVarCharVector v = (NullableVarCharVector) vector;
        byte[] value = (byte[]) val;
        for (int i = start; i < end; i++) {
          v.getMutator().setSafe(i, value, 0, value.length);
        }
        break;
      }
      case TIMESTAMP: {
        NullableTimeStampVector v = (NullableTimeStampVector) vector;
        DateTime ts = new DateTime(((Timestamp) val).getTime()).withZoneRetainFields(DateTimeZone.UTC);
        long value = ts.getMillis();
        for (int i = start; i < end; i++) {
          v.getMutator().setSafe(i, value);
        }
        break;
      }
      case DATE: {
        NullableDateVector v = (NullableDateVector) vector;
        DateTime date = new DateTime(((Date)val).getTime()).withZoneRetainFields(DateTimeZone.UTC);
        long value = date.getMillis();
        for (int i = start; i < end; i++) {
          v.getMutator().setSafe(i, value);
        }
        break;
      }

      case DECIMAL9: {
        final BigDecimal value = ((HiveDecimal)val).bigDecimalValue();
        final NullableDecimal9Vector v = ((NullableDecimal9Vector) vector);
        final Decimal9Holder holder = new Decimal9Holder();
        holder.scale = v.getField().getScale();
        holder.precision = v.getField().getPrecision();
        holder.value = DecimalUtility.getDecimal9FromBigDecimal(value, holder.scale);
        for (int i = start; i < end; i++) {
          v.getMutator().setSafe(i, holder);
        }
        break;
      }

      case DECIMAL18: {
        final BigDecimal value = ((HiveDecimal)val).bigDecimalValue();
        final NullableDecimal18Vector v = ((NullableDecimal18Vector) vector);
        final Decimal18Holder holder = new Decimal18Holder();
        holder.scale = v.getField().getScale();
        holder.precision = v.getField().getPrecision();
        holder.value = DecimalUtility.getDecimal18FromBigDecimal(value, holder.scale);
        for (int i = start; i < end; i++) {
          v.getMutator().setSafe(i, holder);
        }
        break;
      }

      case DECIMAL28SPARSE: {
      final int needSpace = Decimal28SparseHolder.nDecimalDigits * DecimalUtility.INTEGER_SIZE;
        Preconditions.checkArgument(managedBuffer.capacity() > needSpace,
            String.format("Not sufficient space in given managed buffer. Need %d bytes, buffer has %d bytes",
                needSpace, managedBuffer.capacity()));

        final BigDecimal value = ((HiveDecimal)val).bigDecimalValue();
        final NullableDecimal28SparseVector v = ((NullableDecimal28SparseVector) vector);
        final Decimal28SparseHolder holder = new Decimal28SparseHolder();
        holder.scale = v.getField().getScale();
        holder.precision = v.getField().getPrecision();
        holder.buffer = managedBuffer;
        holder.start = 0;
        DecimalUtility.getSparseFromBigDecimal(value, holder.buffer, 0, holder.scale,
            Decimal28SparseHolder.nDecimalDigits);
        for (int i = start; i < end; i++) {
          v.getMutator().setSafe(i, holder);
        }
        break;
      }

      case DECIMAL38SPARSE: {
      final int needSpace = Decimal38SparseHolder.nDecimalDigits * DecimalUtility.INTEGER_SIZE;
        Preconditions.checkArgument(managedBuffer.capacity() > needSpace,
            String.format("Not sufficient space in given managed buffer. Need %d bytes, buffer has %d bytes",
                needSpace, managedBuffer.capacity()));
        final BigDecimal value = ((HiveDecimal)val).bigDecimalValue();
        final NullableDecimal38SparseVector v = ((NullableDecimal38SparseVector) vector);
        final Decimal38SparseHolder holder = new Decimal38SparseHolder();
        holder.scale = v.getField().getScale();
        holder.precision = v.getField().getPrecision();
        holder.buffer = managedBuffer;
        holder.start = 0;
        DecimalUtility.getSparseFromBigDecimal(value, holder.buffer, 0, holder.scale,
            Decimal38SparseHolder.nDecimalDigits);
        for (int i = start; i < end; i++) {
          v.getMutator().setSafe(i, holder);
        }
        break;
      }
      case VARDECIMAL: {
        final BigDecimal value = ((HiveDecimal) val).bigDecimalValue()
            .setScale(vector.getField().getScale(), RoundingMode.HALF_UP);
        final NullableVarDecimalVector v = ((NullableVarDecimalVector) vector);
        for (int i = start; i < end; i++) {
          v.getMutator().setSafe(i, value);
        }
        break;
      }
    }
  }

  /**
   * Obtains major type from given type info holder.
   *
   * @param typeInfo type info holder
   * @param options session options
   * @return appropriate major type, null otherwise. For some types may throw unsupported exception.
   */
  public static MajorType getMajorTypeFromHiveTypeInfo(final TypeInfo typeInfo, final OptionSet options) {
    switch (typeInfo.getCategory()) {
      case PRIMITIVE: {
        PrimitiveTypeInfo primitiveTypeInfo = (PrimitiveTypeInfo) typeInfo;
        MinorType minorType = HiveUtilities.getMinorTypeFromHivePrimitiveTypeInfo(primitiveTypeInfo, options);
        MajorType.Builder typeBuilder = MajorType.newBuilder().setMinorType(minorType)
            .setMode(DataMode.OPTIONAL); // Hive columns (both regular and partition) could have null values

        switch (primitiveTypeInfo.getPrimitiveCategory()) {
          case CHAR:
          case VARCHAR:
            BaseCharTypeInfo baseCharTypeInfo = (BaseCharTypeInfo) primitiveTypeInfo;
            typeBuilder.setPrecision(baseCharTypeInfo.getLength());
            break;
          case DECIMAL:
            DecimalTypeInfo decimalTypeInfo = (DecimalTypeInfo) primitiveTypeInfo;
            typeBuilder.setPrecision(decimalTypeInfo.getPrecision()).setScale(decimalTypeInfo.getScale());
            break;
          default:
            // do nothing, other primitive categories do not have precision or scale
        }

        return typeBuilder.build();
      }

      case LIST:
      case MAP:
      case STRUCT:
      case UNION:
      default:
        throwUnsupportedHiveDataTypeError(typeInfo.getCategory().toString());
    }

    return null;
  }

  /**
   * Obtains minor type from given primitive type info holder.
   *
   * @param primitiveTypeInfo primitive type info holder
   * @param options session options
   * @return appropriate minor type, otherwise throws unsupported type exception
   */
  public static TypeProtos.MinorType getMinorTypeFromHivePrimitiveTypeInfo(PrimitiveTypeInfo primitiveTypeInfo, OptionSet options) {
    switch(primitiveTypeInfo.getPrimitiveCategory()) {
      case BINARY:
        return TypeProtos.MinorType.VARBINARY;
      case BOOLEAN:
        return TypeProtos.MinorType.BIT;
      case DECIMAL: {

        if (!options.getOption(PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY).bool_val) {
          throw UserException.unsupportedError()
              .message(ExecErrorConstants.DECIMAL_DISABLE_ERR_MSG)
              .build(logger);
        }
        return MinorType.VARDECIMAL;
      }
      case DOUBLE:
        return TypeProtos.MinorType.FLOAT8;
      case FLOAT:
        return TypeProtos.MinorType.FLOAT4;
      // TODO (DRILL-2470)
      // Byte and short (tinyint and smallint in SQL types) are currently read as integers
      // as these smaller integer types are not fully supported in Drill today.
      case SHORT:
      case BYTE:
      case INT:
        return TypeProtos.MinorType.INT;
      case LONG:
        return TypeProtos.MinorType.BIGINT;
      case STRING:
      case VARCHAR:
      case CHAR:
        return TypeProtos.MinorType.VARCHAR;
      case TIMESTAMP:
        return TypeProtos.MinorType.TIMESTAMP;
      case DATE:
        return TypeProtos.MinorType.DATE;
    }
    throwUnsupportedHiveDataTypeError(primitiveTypeInfo.getPrimitiveCategory().toString());
    return null;
  }

  /**
   * Utility method which gets table or partition {@link InputFormat} class. First it
   * tries to get the class name from given StorageDescriptor object. If it doesn't contain it tries to get it from
   * StorageHandler class set in table properties. If not found throws an exception.
   * @param job {@link JobConf} instance needed incase the table is StorageHandler based table.
   * @param sd {@link StorageDescriptor} instance of currently reading partition or table (for non-partitioned tables).
   * @param table Table object
   */
  public static Class<? extends InputFormat<?, ?>> getInputFormatClass(final JobConf job, final StorageDescriptor sd, final Table table) throws Exception {
    final String inputFormatName = sd.getInputFormat();
    if (Strings.isNullOrEmpty(inputFormatName)) {
      final String storageHandlerClass = table.getParameters().get(META_TABLE_STORAGE);
      if (Strings.isNullOrEmpty(storageHandlerClass)) {
        throw new ExecutionSetupException("Unable to get Hive table InputFormat class. There is neither " +
            "InputFormat class explicitly specified nor StorageHandler class");
      }
      final HiveStorageHandler storageHandler = HiveUtils.getStorageHandler(job, storageHandlerClass);
      TableDesc tableDesc = new TableDesc();
      tableDesc.setProperties(new org.apache.hadoop.hive.ql.metadata.Table(table).getMetadata());
      storageHandler.configureInputJobProperties(tableDesc, table.getParameters());
      return (Class<? extends InputFormat<?, ?>>) storageHandler.getInputFormatClass();
    } else {
      return (Class<? extends InputFormat<?, ?>>) Class.forName(inputFormatName);
    }
  }

  /**
   * Utility method which adds give configs to {@link JobConf} object.
   *
   * @param job {@link JobConf} instance.
   * @param properties New config properties
   */
  public static void addConfToJob(final JobConf job, final Properties properties) {
    for (Object obj : properties.keySet()) {
      job.set((String) obj, (String) properties.get(obj));
    }
  }

  /**
   * Wrapper around {@code MetaStoreUtils#getPartitionMetadata(org.apache.hadoop.hive.metastore.api.Partition, Table)}
   * which also adds parameters from table to properties returned by that method.
   *
   * @param partition the source of partition level parameters
   * @param table     the source of table level parameters
   * @return properties
   */
  public static Properties getPartitionMetadata(final HivePartition partition, final HiveTableWithColumnCache table) {
    restoreColumns(table, partition);
    try {
      Properties properties = new org.apache.hadoop.hive.ql.metadata.Partition(new org.apache.hadoop.hive.ql.metadata.Table(table), partition).getMetadataFromPartitionSchema();

      // SerDe expects properties from Table, but above call doesn't add Table properties.
      // Include Table properties in final list in order to not to break SerDes that depend on
      // Table properties. For example AvroSerDe gets the schema from properties (passed as second argument)
      table.getParameters().entrySet().stream()
          .filter(e -> e.getKey() != null && e.getValue() != null)
          .forEach(e -> properties.put(e.getKey(), e.getValue()));

      return properties;
    } catch (HiveException e) {
      throw new DrillRuntimeException(e);
    }
  }

  /**
   * Sets columns from table cache to table and partition.
   *
   * @param table the source of column lists cache
   * @param partition partition which will set column list
   */
  public static void restoreColumns(HiveTableWithColumnCache table, HivePartition partition) {
    // exactly the same column lists for partitions or table
    // stored only one time to reduce physical plan serialization
    if (partition != null && partition.getSd().getCols() == null) {
      partition.getSd().setCols(table.getColumnListsCache().getColumns(partition.getColumnListIndex()));
    }
    if (table.getSd().getCols() == null) {
      table.getSd().setCols(table.getColumnListsCache().getColumns(0));
    }
  }

  /**
   * Wrapper around {@code MetaStoreUtils#getSchema(StorageDescriptor, StorageDescriptor, Map, String, String, List)}
   * which also sets columns from table cache to table and returns properties returned by
   * {@code MetaStoreUtils#getSchema(StorageDescriptor, StorageDescriptor, Map, String, String, List)}.
   *
   * @param table Hive table with cached columns
   * @return Hive table metadata
   */
  public static Properties getTableMetadata(HiveTableWithColumnCache table) {
    restoreColumns(table, null);
    return new org.apache.hadoop.hive.ql.metadata.Table(table).getMetadata();
  }

  /**
   * Generates unsupported types exception message with list of supported types
   * and throws user exception.
   *
   * @param unsupportedType unsupported type
   */
  public static void throwUnsupportedHiveDataTypeError(String unsupportedType) {
    StringBuilder errMsg = new StringBuilder()
        .append("Unsupported Hive data type ").append(unsupportedType).append(". ")
        .append(System.lineSeparator())
        .append("Following Hive data types are supported in Drill for querying: ")
        .append("BOOLEAN, TINYINT, SMALLINT, INT, BIGINT, FLOAT, DOUBLE, DATE, TIMESTAMP, BINARY, DECIMAL, STRING, VARCHAR, CHAR, ARRAY.");

    throw UserException.unsupportedError()
        .message(errMsg.toString())
        .build(logger);
  }

  /**
   * Returns property value. If property is absent, return given default value.
   * If property value is non-numeric will fail.
   *
   * @param tableProperties table properties
   * @param propertyName property name
   * @param defaultValue default value used in case if property is absent
   * @return property value
   * @throws NumberFormatException if property value is not numeric
   */
  public static int retrieveIntProperty(Properties tableProperties, String propertyName, int defaultValue) {
    Object propertyObject = tableProperties.get(propertyName);
    if (propertyObject == null) {
      return defaultValue;
    }

    try {
      return Integer.valueOf(propertyObject.toString());
    } catch (NumberFormatException e) {
      throw new NumberFormatException(String.format("Hive table property %s value '%s' is non-numeric",
          propertyName, propertyObject.toString()));
    }
  }

  /**
   * Checks if given table has header or footer.
   * If at least one of them has value more then zero, method will return true.
   *
   * @param table table with column cache instance
   * @return true if table contains header or footer, false otherwise
   */
  public static boolean hasHeaderOrFooter(HiveTableWithColumnCache table) {
    Properties tableProperties = getTableMetadata(table);
    int skipHeader = retrieveIntProperty(tableProperties, serdeConstants.HEADER_COUNT, -1);
    int skipFooter = retrieveIntProperty(tableProperties, serdeConstants.FOOTER_COUNT, -1);
    return skipHeader > 0 || skipFooter > 0;
  }

  /**
   * This method checks whether the table is transactional and set necessary properties in {@link JobConf}.<br>
   * If schema evolution properties aren't set in job conf for the input format, method sets the column names
   * and types from table/partition properties or storage descriptor.
   *
   * @param job the job to update
   * @param sd storage descriptor
   */
  public static void verifyAndAddTransactionalProperties(JobConf job, StorageDescriptor sd) {

    if (AcidUtils.isTablePropertyTransactional(job)) {
      HiveConf.setBoolVar(job, HiveConf.ConfVars.HIVE_TRANSACTIONAL_TABLE_SCAN, true);

      // No work is needed, if schema evolution is used
      if (Utilities.isSchemaEvolutionEnabled(job, true) && job.get(IOConstants.SCHEMA_EVOLUTION_COLUMNS) != null &&
          job.get(IOConstants.SCHEMA_EVOLUTION_COLUMNS_TYPES) != null) {
        return;
      }

      String colNames;
      String colTypes;

      // Try to get get column names and types from table or partition properties. If they are absent there, get columns
      // data from storage descriptor of the table
      colNames = job.get(serdeConstants.LIST_COLUMNS);
      colTypes = job.get(serdeConstants.LIST_COLUMN_TYPES);

      if (colNames == null || colTypes == null) {
        colNames = sd.getCols().stream()
            .map(FieldSchema::getName)
            .collect(Collectors.joining(","));
        colTypes = sd.getCols().stream()
            .map(FieldSchema::getType)
            .collect(Collectors.joining(","));
      }

      job.set(IOConstants.SCHEMA_EVOLUTION_COLUMNS, colNames);
      job.set(IOConstants.SCHEMA_EVOLUTION_COLUMNS_TYPES, colTypes);
    }
  }

  /**
   * Rule is matched when all of the following match:
   * <ul>
   * <li>GroupScan in given DrillScalRel is an {@link HiveScan}</li>
   * <li> {@link HiveScan} is not already rewritten using Drill's native readers</li>
   * <li> InputFormat in table metadata and all partitions metadata contains the same value {@param tableInputFormatClass}</li>
   * <li> No error occurred while checking for the above conditions. An error is logged as warning.</li>
   *</ul>
   * @param call rule call
   * @return True if the rule can be applied. False otherwise
   */
  public static boolean nativeReadersRuleMatches(RelOptRuleCall call, Class tableInputFormatClass) {
    final DrillScanRel scanRel = call.rel(0);

    if (!(scanRel.getGroupScan() instanceof HiveScan) || ((HiveScan) scanRel.getGroupScan()).isNativeReader()) {
      return false;
    }

    final HiveScan hiveScan = (HiveScan) scanRel.getGroupScan();
    final HiveConf hiveConf = hiveScan.getHiveConf();
    final HiveTableWithColumnCache hiveTable = hiveScan.getHiveReadEntry().getTable();

    if (HiveUtilities.isParquetTableContainsUnsupportedType(hiveTable)) {
      return false;
    }

    final Class<? extends InputFormat<?, ?>> tableInputFormat = getInputFormatFromSD(
        HiveUtilities.getTableMetadata(hiveTable), hiveScan.getHiveReadEntry(), hiveTable.getSd(), hiveConf);
    if (tableInputFormat == null || !tableInputFormat.equals(tableInputFormatClass)) {
      return false;
    }

    final List<HiveTableWrapper.HivePartitionWrapper> partitions = hiveScan.getHiveReadEntry().getHivePartitionWrappers();
    if (partitions == null) {
      return true;
    }

    final List<FieldSchema> tableSchema = hiveTable.getSd().getCols();
    // Make sure all partitions have the same input format as the table input format
    for (HiveTableWrapper.HivePartitionWrapper partition : partitions) {
      final StorageDescriptor partitionSD = partition.getPartition().getSd();
      Class<? extends InputFormat<?, ?>> inputFormat = getInputFormatFromSD(HiveUtilities.getPartitionMetadata(
          partition.getPartition(), hiveTable), hiveScan.getHiveReadEntry(), partitionSD, hiveConf);
      if (inputFormat == null || !inputFormat.equals(tableInputFormat)) {
        return false;
      }

      // Make sure the schema of the table and schema of the partition matches. If not return false. Schema changes
      // between table and partition can happen when table schema is altered using ALTER statements after some
      // partitions are already created. Currently native reader conversion doesn't handle schema changes between
      // partition and table. Hive has extensive list of convert methods to convert from one type to rest of the
      // possible types. Drill doesn't have the similar set of methods yet.
      if (!partitionSD.getCols().equals(tableSchema)) {
        logger.debug("Partitions schema is different from table schema. Currently native reader conversion can't " +
            "handle schema difference between partitions and table");
        return false;
      }
    }

    return true;
  }

  /**
   * Get the input format from given {@link StorageDescriptor}.
   *
   * @param properties table properties
   * @param hiveReadEntry hive read entry
   * @param sd storage descriptor
   * @return {@link InputFormat} class or null if a failure has occurred. Failure is logged as warning.
   */
  private static Class<? extends InputFormat<?, ?>> getInputFormatFromSD(final Properties properties,
                                                                  final HiveReadEntry hiveReadEntry, final StorageDescriptor sd, final HiveConf hiveConf) {
    final Table hiveTable = hiveReadEntry.getTable();
    try {
      final String inputFormatName = sd.getInputFormat();
      if (!Strings.isNullOrEmpty(inputFormatName)) {
        return (Class<? extends InputFormat<?, ?>>) Class.forName(inputFormatName);
      }

      final JobConf job = new JobConf(hiveConf);
      HiveUtilities.addConfToJob(job, properties);
      return HiveUtilities.getInputFormatClass(job, sd, hiveTable);
    } catch (final Exception e) {
      logger.warn("Failed to get InputFormat class from Hive table '{}.{}'. StorageDescriptor [{}]",
          hiveTable.getDbName(), hiveTable.getTableName(), sd.toString(), e);
      return null;
    }
  }

  /**
   * Hive doesn't support union type for parquet tables yet.
   * See <a href="https://github.com/apache/hive/blob/master/ql/src/java/org/apache/hadoop/hive/ql/io/parquet/convert/HiveSchemaConverter.java#L117">HiveSchemaConverter.java<a/>
   *
   * @param hiveTable Thrift table from Hive Metastore
   * @return true if table contains unsupported data types, false otherwise
   */
  private static boolean isParquetTableContainsUnsupportedType(final Table hiveTable) {
    for (FieldSchema hiveField : hiveTable.getSd().getCols()) {
      final Category category = TypeInfoUtils.getTypeInfoFromTypeString(hiveField.getType()).getCategory();
      if (category == Category.UNION) {
        logger.debug("Hive table contains unsupported data type: {}", category);
        return true;
      }
    }
    return false;
  }

  /**
   * Creates HiveConf based on given list of configuration properties.
   *
   * @param properties config properties
   * @return instance of HiveConf
   */
  public static HiveConf generateHiveConf(Map<String, String> properties) {
    logger.trace("Override HiveConf with the following properties {}", properties);
    HiveConf hiveConf = new HiveConf();
    properties.forEach(hiveConf::set);
    return hiveConf;
  }

  /**
   * Creates HiveConf based on properties in given HiveConf and configuration properties.
   *
   * @param hiveConf hive conf
   * @param properties config properties
   * @return instance of HiveConf
   */
  public static HiveConf generateHiveConf(HiveConf hiveConf, Map<String, String> properties) {
    Properties changedProperties = hiveConf.getChangedProperties();
    changedProperties.putAll(properties);
    HiveConf newHiveConf = new HiveConf();
    changedProperties.stringPropertyNames()
        .forEach(name -> newHiveConf.set(name, changedProperties.getProperty(name)));
    return newHiveConf;
  }

  /**
   * Helper method which stores partition columns in table columnListCache. If table columnListCache has exactly the
   * same columns as partition, in partition stores columns index that corresponds to identical column list.
   * If table columnListCache hasn't such column list, the column list adds to table columnListCache and in partition
   * stores columns index that corresponds to column list.
   *
   * @param table     hive table instance
   * @param partition partition instance
   * @return hive partition wrapper
   */
  public static HiveTableWrapper.HivePartitionWrapper createPartitionWithSpecColumns(HiveTableWithColumnCache table, Partition partition) {
    int listIndex = table.getColumnListsCache().addOrGet(partition.getSd().getCols());
    return new HiveTableWrapper.HivePartitionWrapper(new HivePartition(partition, listIndex));
  }

  /**
   * Converts specified {@code RelDataType relDataType} into {@link ColumnMetadata}.
   * For the case when specified relDataType is struct, map with recursively converted children
   * will be created.
   *
   * @param name        filed name
   * @param relDataType filed type
   * @return {@link ColumnMetadata} which corresponds to specified {@code RelDataType relDataType}
   */
  public static ColumnMetadata getColumnMetadata(String name, RelDataType relDataType) {
    switch (relDataType.getSqlTypeName()) {
      case ARRAY:
        return getArrayMetadata(name, relDataType);
      case MAP:
      case OTHER:
        throw new UnsupportedOperationException(String.format("Unsupported data type: %s", relDataType.getSqlTypeName()));
      default:
        if (relDataType.isStruct()) {
          return getStructMetadata(name, relDataType);
        } else {
          return new PrimitiveColumnMetadata(
              MaterializedField.create(name,
                  TypeInferenceUtils.getDrillMajorTypeFromCalciteType(relDataType)));
        }
    }
  }

  /**
   * Returns {@link ColumnMetadata} instance which corresponds to specified array {@code RelDataType relDataType}.
   *
   * @param name        name of the filed
   * @param relDataType the source of type information to construct the schema
   * @return {@link ColumnMetadata} instance
   */
  private static ColumnMetadata getArrayMetadata(String name, RelDataType relDataType) {
    RelDataType componentType = relDataType.getComponentType();
    ColumnMetadata childColumnMetadata = getColumnMetadata(name, componentType);
    switch (componentType.getSqlTypeName()) {
      case ARRAY:
        // for the case when nested type is array, it should be placed into repeated list
        return MetadataUtils.newRepeatedList(name, childColumnMetadata);
      case MAP:
      case OTHER:
        throw new UnsupportedOperationException(String.format("Unsupported data type: %s", relDataType.getSqlTypeName()));
      default:
        if (componentType.isStruct()) {
          // for the case when nested type is struct, it should be placed into repeated map
          return MetadataUtils.newMapArray(name, childColumnMetadata.tupleSchema());
        } else {
          // otherwise creates column metadata with repeated data mode
          return new PrimitiveColumnMetadata(
              MaterializedField.create(name,
                  Types.overrideMode(
                      TypeInferenceUtils.getDrillMajorTypeFromCalciteType(componentType),
                      DataMode.REPEATED)));
        }
    }
  }

  /**
   * Returns {@link MapColumnMetadata} column metadata created based on specified {@code RelDataType relDataType} with
   * converted to {@link ColumnMetadata} {@code relDataType}'s children.
   *
   * @param name        name of the filed
   * @param relDataType {@link RelDataType} the source of the children for resulting schema
   * @return {@link MapColumnMetadata} column metadata
   */
  private static MapColumnMetadata getStructMetadata(String name, RelDataType relDataType) {
    TupleMetadata mapSchema = new TupleSchema();
    for (RelDataTypeField relDataTypeField : relDataType.getFieldList()) {
      mapSchema.addColumn(getColumnMetadata(relDataTypeField.getName(), relDataTypeField.getType()));
    }
    return MetadataUtils.newMap(name, mapSchema);
  }

  /**
   * Converts specified {@code FieldSchema column} into {@link ColumnMetadata}.
   * For the case when specified relDataType is struct, map with recursively converted children
   * will be created.
   *
   * @param dataTypeConverter converter to obtain Calcite's types from Hive's ones
   * @param column            column to convert
   * @return {@link ColumnMetadata} which corresponds to specified {@code FieldSchema column}
   */
  public static ColumnMetadata getColumnMetadata(HiveToRelDataTypeConverter dataTypeConverter, FieldSchema column) {
    RelDataType relDataType = dataTypeConverter.convertToNullableRelDataType(column);
    return getColumnMetadata(column.getName(), relDataType);
  }
}

