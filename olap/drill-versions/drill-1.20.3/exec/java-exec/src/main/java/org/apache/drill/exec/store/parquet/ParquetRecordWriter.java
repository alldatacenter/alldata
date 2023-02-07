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

import static java.lang.Math.ceil;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.common.util.DrillVersionInfo;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.store.StorageStrategy;
import org.apache.drill.exec.store.parquet.compression.DrillCompressionCodecFactory;
import org.apache.drill.exec.planner.physical.WriterPrel;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.TypedFieldId;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.store.EventBasedRecordWriter;
import org.apache.drill.exec.store.EventBasedRecordWriter.FieldConverter;
import org.apache.drill.exec.store.ParquetOutputRecordWriter;
import org.apache.drill.exec.util.DecimalUtility;
import org.apache.drill.exec.vector.BitVector;
import org.apache.drill.exec.vector.complex.BaseRepeatedValueVector;
import org.apache.drill.exec.vector.complex.reader.FieldReader;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.column.ColumnWriteStore;
import org.apache.parquet.column.ParquetProperties;
import org.apache.parquet.column.ParquetProperties.WriterVersion;
import org.apache.parquet.column.impl.ColumnWriteStoreV1;
import org.apache.parquet.column.impl.ColumnWriteStoreV2;
import org.apache.parquet.column.values.factory.DefaultV1ValuesWriterFactory;
import org.apache.parquet.column.values.factory.DefaultV2ValuesWriterFactory;
import org.apache.parquet.column.values.factory.ValuesWriterFactory;
import org.apache.parquet.compression.CompressionCodecFactory;
import org.apache.parquet.hadoop.ParquetColumnChunkPageWriteStore;
import org.apache.parquet.hadoop.ParquetFileWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.io.ColumnIOFactory;
import org.apache.parquet.io.MessageColumnIO;
import org.apache.parquet.io.api.RecordConsumer;
import org.apache.parquet.schema.DecimalMetadata;
import org.apache.parquet.schema.GroupType;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.OriginalType;
import org.apache.parquet.schema.PrimitiveType;
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName;
import org.apache.parquet.schema.Type;
import org.apache.parquet.schema.Type.Repetition;

import org.apache.parquet.schema.Types.ListBuilder;

public class ParquetRecordWriter extends ParquetOutputRecordWriter {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ParquetRecordWriter.class);

  private static final int MINIMUM_BUFFER_SIZE = 64 * 1024;
  private static final int MINIMUM_RECORD_COUNT_FOR_CHECK = 100;
  private static final int MAXIMUM_RECORD_COUNT_FOR_CHECK = 10000;
  private static final int BLOCKSIZE_MULTIPLE = 64 * 1024;

  /**
   * Name of nested group for Parquet's {@code MAP} type.
   * @see <a href="https://github.com/apache/parquet-format/blob/master/LogicalTypes.md#maps">MAP logical type</a>
   */
  private static final String GROUP_KEY_VALUE_NAME = "key_value";

  public static final String DRILL_VERSION_PROPERTY = "drill.version";
  public static final String WRITER_VERSION_PROPERTY = "drill-writer.version";

  private final StorageStrategy storageStrategy;
  private ParquetFileWriter parquetFileWriter;
  private MessageType schema;
  private Map<String, String> extraMetaData = new HashMap<>();
  private int blockSize;
  private int pageSize;
  private int dictionaryPageSize;
  private boolean enableDictionary = false;
  private boolean useSingleFSBlock = false;
  private CompressionCodecName codec = CompressionCodecName.SNAPPY;
  private WriterVersion writerVersion = WriterVersion.PARQUET_1_0;
  private CompressionCodecFactory codecFactory;

  private long recordCount = 0;
  private long recordCountForNextMemCheck = MINIMUM_RECORD_COUNT_FOR_CHECK;

  private ColumnWriteStore store;
  private ParquetColumnChunkPageWriteStore pageStore;

  private RecordConsumer consumer;
  private BatchSchema batchSchema;

  private Configuration conf;
  private FileSystem fs;
  private String location;
  private List<Path> cleanUpLocations;
  private String prefix;
  private int index = 0;
  private OperatorContext oContext;
  private List<String> partitionColumns;
  private boolean hasPartitions;
  private PrimitiveTypeName logicalTypeForDecimals;
  private boolean usePrimitiveTypesForDecimals;

  /** Is used to ensure that empty Parquet file will be written if no rows were provided. */
  private boolean empty = true;

  public ParquetRecordWriter(FragmentContext context, ParquetWriter writer) throws OutOfMemoryException {
    this.oContext = context.newOperatorContext(writer);
    this.codecFactory = DrillCompressionCodecFactory.createDirectCodecFactory(
        writer.getFormatPlugin().getFsConf(),
        new ParquetDirectByteBufferAllocator(oContext.getAllocator()),
        pageSize
    );
    this.partitionColumns = writer.getPartitionColumns();
    this.hasPartitions = partitionColumns != null && partitionColumns.size() > 0;
    this.extraMetaData.put(DRILL_VERSION_PROPERTY, DrillVersionInfo.getVersion());
    this.extraMetaData.put(WRITER_VERSION_PROPERTY, String.valueOf(ParquetWriter.WRITER_VERSION));
    this.storageStrategy = writer.getStorageStrategy() == null ? StorageStrategy.DEFAULT : writer.getStorageStrategy();
    this.cleanUpLocations = new ArrayList<>();
    this.conf = new Configuration(writer.getFormatPlugin().getFsConf());
  }

  @Override
  public void init(Map<String, String> writerOptions) throws IOException {
    this.location = writerOptions.get("location");
    this.prefix = writerOptions.get("prefix");

    fs = FileSystem.get(conf);
    blockSize = Integer.parseInt(writerOptions.get(ExecConstants.PARQUET_BLOCK_SIZE));
    pageSize = Integer.parseInt(writerOptions.get(ExecConstants.PARQUET_PAGE_SIZE));
    dictionaryPageSize= Integer.parseInt(writerOptions.get(ExecConstants.PARQUET_DICT_PAGE_SIZE));
    String codecName = writerOptions.get(ExecConstants.PARQUET_WRITER_COMPRESSION_TYPE).toLowerCase();
    switch(codecName) {
    case "none":
    case "uncompressed":
      codec = CompressionCodecName.UNCOMPRESSED;
      break;
    case "brotli":
      codec = CompressionCodecName.BROTLI;
      break;
    case "gzip":
      codec = CompressionCodecName.GZIP;
      break;
    case "lz4":
      codec = CompressionCodecName.LZ4;
      break;
    case "lzo":
      codec = CompressionCodecName.LZO;
      break;
    case "snappy":
      codec = CompressionCodecName.SNAPPY;
      break;
    case "zstd":
      codec = CompressionCodecName.ZSTD;
      break;
    default:
      throw new UnsupportedOperationException(String.format("Unknown compression type: %s", codecName));
    }

    String logicalTypeNameForDecimals = writerOptions.get(ExecConstants.PARQUET_WRITER_LOGICAL_TYPE_FOR_DECIMALS).toLowerCase();
    switch (logicalTypeNameForDecimals) {
      case "fixed_len_byte_array":
        logicalTypeForDecimals = PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY;
        break;
      case "binary":
        logicalTypeForDecimals = PrimitiveTypeName.BINARY;
        break;
      default:
        throw new UnsupportedOperationException(
            String.format(
                "Unsupported logical type for decimals: %s\n" +
                "Supported types: ['fixed_len_byte_array', 'binary']", codecName));
    }

    enableDictionary = Boolean.parseBoolean(writerOptions.get(ExecConstants.PARQUET_WRITER_ENABLE_DICTIONARY_ENCODING));
    useSingleFSBlock = Boolean.parseBoolean(writerOptions.get(ExecConstants.PARQUET_WRITER_USE_SINGLE_FS_BLOCK));
    usePrimitiveTypesForDecimals = Boolean.parseBoolean(writerOptions.get(ExecConstants.PARQUET_WRITER_USE_PRIMITIVE_TYPES_FOR_DECIMALS));
    writerVersion = WriterVersion.fromString(
      writerOptions.get(ExecConstants.PARQUET_WRITER_FORMAT_VERSION)
    );

    if (useSingleFSBlock) {
      // Round up blockSize to multiple of 64K.
      blockSize = (int)ceil((double)blockSize/BLOCKSIZE_MULTIPLE) * BLOCKSIZE_MULTIPLE;
    }
  }

  private boolean containsComplexVectors(BatchSchema schema) {
    for (MaterializedField field : schema) {
      MinorType type = field.getType().getMinorType();
      switch (type) {
      case MAP:
      case DICT:
      case LIST:
        return true;
      default:
      }
    }
    return false;
  }

  @Override
  public void updateSchema(VectorAccessible batch) throws IOException {
    if (this.batchSchema == null || !this.batchSchema.equals(batch.getSchema()) || containsComplexVectors(this.batchSchema)) {
      if (this.batchSchema != null) {
        flush(false);
      }
      this.batchSchema = batch.getSchema();
      newSchema();
    }
    TypedFieldId fieldId = batch.getValueVectorId(SchemaPath.getSimplePath(WriterPrel.PARTITION_COMPARATOR_FIELD));
    if (fieldId != null) {
      VectorWrapper w = batch.getValueAccessorById(BitVector.class, fieldId.getFieldIds());
      setPartitionVector((BitVector) w.getValueVector());
    }
  }

  private void newSchema() {
    List<Type> types = new ArrayList<>();
    for (MaterializedField field : batchSchema) {
      if (!supportsField(field)) {
        continue;
      }
      types.add(getType(field));
    }
    schema = new MessageType("root", types);

    // We don't want this number to be too small, ideally we divide the block equally across the columns.
    // It is unlikely all columns are going to be the same size.
    // Its value is likely below Integer.MAX_VALUE (2GB), although rowGroupSize is a long type.
    // Therefore this size is cast to int, since allocating byte array in under layer needs to
    // limit the array size in an int scope.
    int initialBlockBufferSize = this.schema.getColumns().size() > 0 ?
        max(MINIMUM_BUFFER_SIZE, blockSize / this.schema.getColumns().size() / 5) : MINIMUM_BUFFER_SIZE;
    // We don't want this number to be too small either. Ideally, slightly bigger than the page size,
    // but not bigger than the block buffer
    int initialPageBufferSize = max(MINIMUM_BUFFER_SIZE, min(pageSize + pageSize / 10, initialBlockBufferSize));
    ValuesWriterFactory valWriterFactory = writerVersion == WriterVersion.PARQUET_1_0
      ? new DefaultV1ValuesWriterFactory()
      : new DefaultV2ValuesWriterFactory();

    ParquetProperties parquetProperties = ParquetProperties.builder()
        .withPageSize(pageSize)
        .withDictionaryEncoding(enableDictionary)
        .withDictionaryPageSize(initialPageBufferSize)
        .withAllocator(new ParquetDirectByteBufferAllocator(oContext))
        .withValuesWriterFactory(valWriterFactory)
        .withWriterVersion(writerVersion)
        .build();

    // TODO: Replace ParquetColumnChunkPageWriteStore with ColumnChunkPageWriteStore from parquet library
    //   once DRILL-7906 (PARQUET-1006) will be resolved
    pageStore = new ParquetColumnChunkPageWriteStore(codecFactory.getCompressor(codec), schema,
            parquetProperties.getInitialSlabSize(), pageSize, parquetProperties.getAllocator(),
            parquetProperties.getColumnIndexTruncateLength(), parquetProperties.getPageWriteChecksumEnabled());

    store = writerVersion == WriterVersion.PARQUET_1_0
      ? new ColumnWriteStoreV1(schema, pageStore, parquetProperties)
      : new ColumnWriteStoreV2(schema, pageStore, parquetProperties);

    MessageColumnIO columnIO = new ColumnIOFactory(false).getColumnIO(this.schema);
    consumer = columnIO.getRecordWriter(store);
    setUp(schema, consumer);
  }

  @Override
  public boolean supportsField(MaterializedField field) {
    return super.supportsField(field)
      && (field.getType().getMinorType() != MinorType.MAP || field.getChildCount() > 0);
  }

  @Override
  protected PrimitiveType getPrimitiveType(MaterializedField field) {
    MinorType minorType = field.getType().getMinorType();
    String name = field.getName();
    int length = ParquetTypeHelper.getLengthForMinorType(minorType);
    PrimitiveTypeName primitiveTypeName = ParquetTypeHelper.getPrimitiveTypeNameForMinorType(minorType);
    if (Types.isDecimalType(minorType)) {
      primitiveTypeName = logicalTypeForDecimals;
      if (usePrimitiveTypesForDecimals) {
        if (field.getPrecision() <= ParquetTypeHelper.getMaxPrecisionForPrimitiveType(PrimitiveTypeName.INT32)) {
          primitiveTypeName = PrimitiveTypeName.INT32;
        } else if (field.getPrecision() <= ParquetTypeHelper.getMaxPrecisionForPrimitiveType(PrimitiveTypeName.INT64)) {
          primitiveTypeName = PrimitiveTypeName.INT64;
        }
      }

      length = DecimalUtility.getMaxBytesSizeForPrecision(field.getPrecision());
    }

    Repetition repetition = ParquetTypeHelper.getRepetitionForDataMode(field.getDataMode());
    OriginalType originalType = ParquetTypeHelper.getOriginalTypeForMinorType(minorType);
    DecimalMetadata decimalMetadata = ParquetTypeHelper.getDecimalMetadataForField(field);
    return new PrimitiveType(repetition, primitiveTypeName, length, name, originalType, decimalMetadata, null);
  }

  private Type getType(MaterializedField field) {
    MinorType minorType = field.getType().getMinorType();
    DataMode dataMode = field.getType().getMode();
    switch (minorType) {
      case MAP:
        List<Type> types = getChildrenTypes(field);
        return new GroupType(dataMode == DataMode.REPEATED ? Repetition.REPEATED : Repetition.OPTIONAL, field.getName(), types);
      case DICT:
        // RepeatedDictVector has DictVector as data vector hence the need to get the first child
        // for REPEATED case to be able to access map's key and value fields
        MaterializedField dictField = dataMode != DataMode.REPEATED
            ? field : ((List<MaterializedField>) field.getChildren()).get(0);
        List<Type> keyValueTypes = getChildrenTypes(dictField);

        GroupType keyValueGroup = new GroupType(Repetition.REPEATED, GROUP_KEY_VALUE_NAME, keyValueTypes);
        if (dataMode == DataMode.REPEATED) {
          // Parquet's MAP repetition must be either optional or required, so nest it inside Parquet's LIST type
          GroupType elementType = org.apache.parquet.schema.Types.buildGroup(Repetition.OPTIONAL)
              .as(OriginalType.MAP)
              .addField(keyValueGroup)
              .named(LIST);
          GroupType listGroup = new GroupType(Repetition.REPEATED, LIST, elementType);
          return org.apache.parquet.schema.Types.buildGroup(Repetition.OPTIONAL)
              .as(OriginalType.LIST)
              .addField(listGroup)
              .named(field.getName());
        } else {
          return org.apache.parquet.schema.Types.buildGroup(Repetition.OPTIONAL)
              .as(OriginalType.MAP)
              .addField(keyValueGroup)
              .named(field.getName());
        }
      case LIST:
        MaterializedField elementField = getDataField(field);
        ListBuilder<GroupType> listBuilder = org.apache.parquet.schema.Types
            .list(dataMode == DataMode.OPTIONAL ? Repetition.OPTIONAL : Repetition.REQUIRED);
        addElementType(listBuilder, elementField);
        GroupType listType = listBuilder.named(field.getName());
        return listType;
      case NULL:
        MaterializedField newField = field.withType(
            TypeProtos.MajorType.newBuilder().setMinorType(MinorType.INT).setMode(DataMode.OPTIONAL).build());
        return getPrimitiveType(newField);
      default:
        return getPrimitiveType(field);
    }
  }

  /**
   * Helper method for conversion of map child
   * fields.
   *
   * @param field map
   * @return converted child fields
   */
  private List<Type> getChildrenTypes(MaterializedField field) {
    return field.getChildren().stream()
        .map(this::getType)
        .collect(Collectors.toList());
  }

  /**
   * For list or repeated type possible child fields are {@link BaseRepeatedValueVector#DATA_VECTOR_NAME}
   * and {@link BaseRepeatedValueVector#OFFSETS_VECTOR_NAME}. This method used to find the data field.
   *
   * @param field parent repeated field
   * @return child data field
   */
  private MaterializedField getDataField(MaterializedField field) {
    return field.getChildren().stream()
        .filter(child -> BaseRepeatedValueVector.DATA_VECTOR_NAME.equals(child.getName()))
        .findAny()
        .orElseThrow(() -> new NoSuchElementException(String.format(
            "Failed to get elementField '%s' from list: %s",
            BaseRepeatedValueVector.DATA_VECTOR_NAME, field.getChildren())));
  }

  /**
   * Adds element type to {@code listBuilder} based on Drill's
   * {@code elementField}.
   *
   * @param listBuilder  list schema builder
   * @param elementField Drill's type of list elements
   */
  private void addElementType(ListBuilder<GroupType> listBuilder, MaterializedField elementField) {
    if (elementField.getDataMode() == DataMode.REPEATED) {
      ListBuilder<GroupType> inner = org.apache.parquet.schema.Types.requiredList();
      if (elementField.getType().getMinorType() == MinorType.MAP) {
        GroupType mapGroupType = new GroupType(Repetition.REQUIRED, ELEMENT, getChildrenTypes(elementField));
        inner.element(mapGroupType);
      } else {
        MaterializedField child2 = getDataField(elementField);
        addElementType(inner, child2);
      }
      listBuilder.setElementType(inner.named(ELEMENT));
    } else {
      Type element = getType(elementField);
      // element may have internal name '$data$',
      // rename it to 'element' according to Parquet list schema
      if (element.isPrimitive()) {
        PrimitiveType primitiveElement = element.asPrimitiveType();
        element = new PrimitiveType(
            primitiveElement.getRepetition(),
            primitiveElement.getPrimitiveTypeName(),
            ELEMENT,
            primitiveElement.getOriginalType()
        );
      } else {
        GroupType groupElement = element.asGroupType();
        element = new GroupType(groupElement.getRepetition(),
            ELEMENT, groupElement.getFields());
      }
      listBuilder.element(element);
    }
  }

  @Override
  public void checkForNewPartition(int index) {
    if (!hasPartitions) {
      return;
    }
    try {
      boolean newPartition = newPartition(index);
      if (newPartition) {
        flush(false);
        newSchema();
      }
    } catch (Exception e) {
      throw new DrillRuntimeException(e);
    }
  }

  private void flush(boolean cleanUp) throws IOException {
    try {
      if (recordCount > 0) {
        flushParquetFileWriter();
      } else if (cleanUp && empty && schema != null && schema.getFieldCount() > 0) {
        // Write empty parquet if:
        // 1) This is a cleanup - no any additional records can be written
        // 2) No file was written until this moment
        // 3) Schema is set
        // 4) Schema is not empty
        createParquetFileWriter();
        flushParquetFileWriter();
      }
    } finally {
      store.close();
      pageStore.close();
      codecFactory.release();

      store = null;
      pageStore = null;
      index++;
    }
  }

  private void checkBlockSizeReached() throws IOException {
    if (recordCount >= recordCountForNextMemCheck) { // checking the memory size is relatively expensive, so let's not do it for every record.
      long memSize = store.getBufferedSize();
      if (memSize > blockSize) {
        logger.debug("Reached block size " + blockSize);
        flush(false);
        newSchema();
        recordCountForNextMemCheck = min(max(MINIMUM_RECORD_COUNT_FOR_CHECK, recordCount / 2), MAXIMUM_RECORD_COUNT_FOR_CHECK);
      } else {
        float recordSize = (float) memSize / recordCount;
        recordCountForNextMemCheck = min(
                max(MINIMUM_RECORD_COUNT_FOR_CHECK, (recordCount + (long)(blockSize / recordSize)) / 2), // will check halfway
                recordCount + MAXIMUM_RECORD_COUNT_FOR_CHECK // will not look more than max records ahead
        );
      }
    }
  }

  @Override
  public FieldConverter getNewMapConverter(int fieldId, String fieldName, FieldReader reader) {
    return new MapParquetConverter(fieldId, fieldName, reader);
  }

  public class MapParquetConverter extends FieldConverter {
    List<FieldConverter> converters = new ArrayList<>();

    public MapParquetConverter(int fieldId, String fieldName, FieldReader reader) {
      super(fieldId, fieldName, reader);
      int i = 0;
      for (String name : reader) {
        FieldConverter converter = EventBasedRecordWriter.getConverter(ParquetRecordWriter.this, i++, name, reader.reader(name));
        converters.add(converter);
      }
    }

    @Override
    public void writeField() throws IOException {
      if (!converters.isEmpty()) {
        consumer.startField(fieldName, fieldId);
        consumer.startGroup();
        for (FieldConverter converter : converters) {
          converter.writeField();
        }
        consumer.endGroup();
        consumer.endField(fieldName, fieldId);
      }
    }
  }

  @Override
  public FieldConverter getNewRepeatedMapConverter(int fieldId, String fieldName, FieldReader reader) {
    return new RepeatedMapParquetConverter(fieldId, fieldName, reader);
  }

  public class RepeatedMapParquetConverter extends FieldConverter {
    List<FieldConverter> converters = new ArrayList<>();

    public RepeatedMapParquetConverter(int fieldId, String fieldName, FieldReader reader) {
      super(fieldId, fieldName, reader);
      int i = 0;
      for (String name : reader) {
        FieldConverter converter = EventBasedRecordWriter.getConverter(ParquetRecordWriter.this, i++, name, reader.reader(name));
        converters.add(converter);
      }
    }

    @Override
    public void writeField() throws IOException {
      if (reader.size() == 0) {
        return;
      }
      consumer.startField(fieldName, fieldId);
      while (reader.next()) {
        consumer.startGroup();
        for (FieldConverter converter : converters) {
          converter.writeField();
        }
        consumer.endGroup();
      }
      consumer.endField(fieldName, fieldId);
    }

    @Override
    public void writeListField() throws IOException {
      if (reader.size() == 0) {
        return;
      }
      consumer.startField(LIST, ZERO_IDX);
      while (reader.next()) {
        consumer.startGroup();
        consumer.startField(ELEMENT, ZERO_IDX);

        consumer.startGroup();
        for (FieldConverter converter : converters) {
          converter.writeField();
        }
        consumer.endGroup();

        consumer.endField(ELEMENT, ZERO_IDX);
        consumer.endGroup();
      }
      consumer.endField(LIST, ZERO_IDX);
    }
  }

  @Override
  public FieldConverter getNewRepeatedListConverter(int fieldId, String fieldName, FieldReader reader) {
    return new RepeatedListParquetConverter(fieldId, fieldName, reader);
  }

  public class RepeatedListParquetConverter extends FieldConverter {
    private final FieldConverter converter;

    RepeatedListParquetConverter(int fieldId, String fieldName, FieldReader reader) {
      super(fieldId, fieldName, reader);
      converter = EventBasedRecordWriter.getConverter(ParquetRecordWriter.this, 0, "", reader.reader());
    }

    @Override
    public void writeField() throws IOException {
      consumer.startField(fieldName, fieldId);
      consumer.startField(LIST, ZERO_IDX);
      while (reader.next()) {
        consumer.startGroup();
        consumer.startField(ELEMENT, ZERO_IDX);
        converter.writeListField();
        consumer.endField(ELEMENT, ZERO_IDX);
        consumer.endGroup();
      }
      consumer.endField(LIST, ZERO_IDX);
      consumer.endField(fieldName, fieldId);
    }
  }

  @Override
  public FieldConverter getNewDictConverter(int fieldId, String fieldName, FieldReader reader) {
    return new DictParquetConverter(fieldId, fieldName, reader);
  }

  public class DictParquetConverter extends FieldConverter {
    List<FieldConverter> converters = new ArrayList<>();

    public DictParquetConverter(int fieldId, String fieldName, FieldReader reader) {
      super(fieldId, fieldName, reader);
      int i = 0;
      for (String name : reader) {
        FieldConverter converter = EventBasedRecordWriter.getConverter(
            ParquetRecordWriter.this, i++, name, reader.reader(name));
        converters.add(converter);
      }
    }

    @Override
    public void writeField() throws IOException {
      if (reader.size() == 0) {
        return;
      }

      consumer.startField(fieldName, fieldId);
      consumer.startGroup();
      consumer.startField(GROUP_KEY_VALUE_NAME, 0);
      while (reader.next()) {
        consumer.startGroup();
        for (FieldConverter converter : converters) {
          converter.writeField();
        }
        consumer.endGroup();
      }
      consumer.endField(GROUP_KEY_VALUE_NAME, 0);
      consumer.endGroup();
      consumer.endField(fieldName, fieldId);
    }
  }

  @Override
  public FieldConverter getNewRepeatedDictConverter(int fieldId, String fieldName, FieldReader reader) {
    return new RepeatedDictParquetConverter(fieldId, fieldName, reader);
  }

  public class RepeatedDictParquetConverter extends FieldConverter {
    private final FieldConverter dictConverter;

    public RepeatedDictParquetConverter(int fieldId, String fieldName, FieldReader reader) {
      super(fieldId, fieldName, reader);
      dictConverter = new DictParquetConverter(0, ELEMENT, reader.reader());
    }

    @Override
    public void writeField() throws IOException {
      if (reader.size() == 0) {
        return;
      }

      consumer.startField(fieldName, fieldId);
      consumer.startGroup();
      consumer.startField(LIST, 0);
      while (reader.next()) {
        consumer.startGroup();
        dictConverter.writeField();
        consumer.endGroup();
      }
      consumer.endField(LIST, 0);
      consumer.endGroup();
      consumer.endField(fieldName, fieldId);
    }
  }

  @Override
  public void startRecord() throws IOException {
    if (CollectionUtils.isEmpty(schema.getFields())) {
      return;
    }
    consumer.startMessage();
  }

  @Override
  public void endRecord() throws IOException {
    if (CollectionUtils.isEmpty(schema.getFields())) {
      return;
    }
    consumer.endMessage();

    // we wait until there is at least one record before creating the parquet file
    if (parquetFileWriter == null) {
      createParquetFileWriter();
    }

    empty = false;
    recordCount++;
    checkBlockSizeReached();
  }

  @Override
  public void abort() throws IOException {
    List<String> errors = new ArrayList<>();
    for (Path location : cleanUpLocations) {
      try {
        if (fs.exists(location)) {
          fs.delete(location, true);
          logger.info("Aborting writer. Location [{}] on file system [{}] is deleted.",
              location.toUri().getPath(), fs.getUri());
        }
      } catch (IOException e) {
        errors.add(location.toUri().getPath());
        logger.error("Failed to delete location [{}] on file system [{}].",
            location, fs.getUri(), e);
      }
    }
    if (!errors.isEmpty()) {
      throw new IOException(String.format("Failed to delete the following locations %s on file system [%s]" +
          " during aborting writer", errors, fs.getUri()));
    }
  }

  @Override
  public void cleanup() throws IOException {
    flush(true);
  }

  private void createParquetFileWriter() throws IOException {
    Path path = new Path(location, prefix + "_" + index + ".parquet");
    // to ensure that our writer was the first to create output file, we create empty file first and fail if file exists
    Path firstCreatedPath = storageStrategy.createFileAndApply(fs, path);

    // since parquet reader supports partitions, it means that several output files may be created
    // if this writer was the one to create table folder, we store only folder and delete it with its content in case of abort
    // if table location was created before, we store only files created by this writer and delete them in case of abort
    addCleanUpLocation(fs, firstCreatedPath);

    // since ParquetFileWriter will overwrite empty output file (append is not supported)
    // we need to re-apply file permission
    if (useSingleFSBlock) {
      // Passing blockSize creates files with this blockSize instead of filesystem default blockSize.
      // Currently, this is supported only by filesystems included in
      // BLOCK_FS_SCHEMES (ParquetFileWriter.java in parquet-mr), which includes HDFS.
      // For other filesystems, it uses default blockSize configured for the file system.
      parquetFileWriter = new ParquetFileWriter(conf, schema, path, ParquetFileWriter.Mode.OVERWRITE, blockSize, 0);
    } else {
      parquetFileWriter = new ParquetFileWriter(conf, schema, path, ParquetFileWriter.Mode.OVERWRITE);
    }
    storageStrategy.applyToFile(fs, path);
    parquetFileWriter.start();
  }

  private void flushParquetFileWriter() throws IOException {
    parquetFileWriter.startBlock(recordCount);
    consumer.flush();
    store.flush();
    pageStore.flushToFileWriter(parquetFileWriter);
    recordCount = 0;
    parquetFileWriter.endBlock();

    // we are writing one single block per file
    parquetFileWriter.end(extraMetaData);
    parquetFileWriter = null;
  }

  /**
   * Adds passed location to the list of locations to be cleaned up in case of abort.
   * Add locations if:
   * <li>if no locations were added before</li>
   * <li>if first location is a file</li>
   *
   * If first added location is a folder, we don't add other locations (which can be only files),
   * since this writer was the one to create main folder where files are located,
   * on abort we'll delete this folder with its content.
   *
   * If first location is a file, then we add other files, since this writer didn't create main folder
   * and on abort we need to delete only created files but not the whole folder.
   *
   * @param fs file system where location is created
   * @param location passed location
   * @throws IOException in case of errors during check if passed location is a file
   */
  private void addCleanUpLocation(FileSystem fs, Path location) throws IOException {
    if (cleanUpLocations.isEmpty() || fs.isFile(cleanUpLocations.get(0))) {
      cleanUpLocations.add(location);
    }
  }
}
