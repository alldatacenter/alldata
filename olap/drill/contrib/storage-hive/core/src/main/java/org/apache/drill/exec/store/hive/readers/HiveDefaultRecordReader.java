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
package org.apache.drill.exec.store.hive.readers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.netty.buffer.DrillBuf;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.expression.PathSegment;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.impl.OutputMutator;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.store.AbstractRecordReader;
import org.apache.drill.exec.store.hive.HivePartition;
import org.apache.drill.exec.store.hive.HiveTableWithColumnCache;
import org.apache.drill.exec.store.hive.HiveUtilities;
import org.apache.drill.exec.store.hive.writers.HiveValueWriter;
import org.apache.drill.exec.store.hive.writers.HiveValueWriterFactory;
import org.apache.drill.exec.vector.AllocationHelper;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.complex.impl.VectorContainerWriter;
import org.apache.drill.shaded.guava.com.google.common.util.concurrent.ListenableFuture;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.serde2.ColumnProjectionUtils;
import org.apache.hadoop.hive.serde2.Deserializer;
import org.apache.hadoop.hive.serde2.SerDeException;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorConverters;
import org.apache.hadoop.hive.serde2.objectinspector.StructField;
import org.apache.hadoop.hive.serde2.objectinspector.StructObjectInspector;
import org.apache.hadoop.hive.serde2.typeinfo.StructTypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfoUtils;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reader which uses complex writer underneath to fill in value vectors with data read from Hive.
 * At first glance initialization code in the writer looks cumbersome, but in the end it's main aim is to prepare list of key
 * fields used in next() and readHiveRecordAndInsertIntoRecordBatch(Object rowValue) methods.
 * <p>
 * In a nutshell, the reader is used in two stages:
 * 1) Setup stage configures mapredReader, partitionObjInspector, partitionDeserializer, list of {@link HiveValueWriter}s for each column in record
 * batch, partition vectors and values
 * 2) Reading stage uses objects configured previously to get rows from InputSplits, represent each row as Struct of columns values,
 * and write each row value of column into Drill's value vectors using HiveValueWriter for each specific column
 */
public class HiveDefaultRecordReader extends AbstractRecordReader {

  protected static final Logger logger = LoggerFactory.getLogger(HiveDefaultRecordReader.class);

  /**
   * Max amount of records that can be consumed by one next() method call
   */
  public static final int TARGET_RECORD_COUNT = 4000;

  /**
   * Key of partition columns in grouped map.
   */
  private static final boolean PARTITION_COLUMNS = true;

  /**
   * Manages all writes to value vectors received using OutputMutator
   */
  protected VectorContainerWriter outputWriter;

  /**
   * Before creation of mapredReader Drill creates the object which holds
   * meta about table that should be read.
   */
  private final HiveTableWithColumnCache hiveTable;

  /**
   * Contains info about current user and group. Used to initialize
   * the mapredReader using under the user permissions.
   */
  private final UserGroupInformation proxyUserGroupInfo;

  /**
   * Config to be used for creation of JobConf instance.
   */
  private final HiveConf hiveConf;

  /**
   * Hive partition wrapper with index of column list in ColumnListsCache.
   */
  private final HivePartition partition;

  /**
   * This mapredReader creates JobConf instance to use it's handsome metadata.
   * Actually the job won't be executed.
   */
  private JobConf job;

  /**
   * Deserializer to be used for deserialization of row.
   * Depending on partition presence it may be partition or table deserializer.
   */
  protected Deserializer partitionDeserializer;

  /**
   * Used to inspect rows parsed by partitionDeserializer
   */
  private StructObjectInspector partitionObjInspector;

  /**
   * Converts value deserialized using partitionDeserializer
   */
  protected ObjectInspectorConverters.Converter partitionToTableSchemaConverter;

  /**
   * Used to inspect rowValue of each column
   */
  private StructObjectInspector finalObjInspector;

  /**
   * For each concrete column to be read we assign concrete writer
   * which encapsulates writing of column values read from Hive into
   * specific value vector
   */
  private HiveValueWriter[] columnValueWriters;

  /**
   * At the moment of mapredReader instantiation we can check inputSplits,
   * if splits aren't present than there are no records to read,
   * so mapredReader can finish work early.
   */
  protected boolean empty;

  /**
   * Buffer used for population of partition vectors  and to fill in data into vectors via writers
   */
  private final DrillBuf drillBuf;

  /**
   * The fragmentContext holds different helper objects
   * associated with fragment. In the reader it's used
   * to get options for accurate detection of partition columns types.
   */
  private final FragmentContext fragmentContext;

  /**
   * Partition vectors and values are linked together and gets filled after we know that all records are read.
   * This two arrays must have same sizes.
   */
  private ValueVector[] partitionVectors;

  /**
   * Values to be written into partition vector.
   */
  private Object[] partitionValues;


  /**
   * InputSplits to be processed by mapredReader.
   */
  private final Iterator<InputSplit> inputSplitsIterator;

  /**
   * Reader used to to get data from InputSplits
   */
  protected RecordReader<Object, Object> mapredReader;

  /**
   * Helper object used together with mapredReader to get data from InputSplit.
   */
  private Object key;

  /**
   * Helper object used together with mapredReader to get data from InputSplit.
   */
  protected Object valueHolder;

  /**
   * Array of StructField representing columns to be read by the reader.
   * Used to extract row value of column from final object inspector.
   */
  private StructField[] selectedStructFieldRefs;


  /**
   * Readers constructor called by initializer.
   *
   * @param table            metadata about Hive table being read
   * @param partition        holder of metadata about table partitioning
   * @param inputSplits      input splits for reading data from distributed storage
   * @param projectedColumns target columns for scan
   * @param context          fragmentContext of fragment
   * @param hiveConf         Hive configuration
   * @param proxyUgi         user/group info to be used for initialization
   */
  public HiveDefaultRecordReader(HiveTableWithColumnCache table, HivePartition partition,
                                 Collection<InputSplit> inputSplits, List<SchemaPath> projectedColumns,
                                 FragmentContext context, HiveConf hiveConf, UserGroupInformation proxyUgi) {
    this.hiveTable = table;
    this.partition = partition;
    this.hiveConf = hiveConf;
    this.proxyUserGroupInfo = proxyUgi;
    this.empty = inputSplits == null || inputSplits.isEmpty();
    this.inputSplitsIterator = empty ? Collections.emptyIterator() : inputSplits.iterator();
    this.drillBuf = context.getManagedBuffer().reallocIfNeeded(256);
    this.partitionVectors = new ValueVector[0];
    this.partitionValues = new Object[0];
    setColumns(projectedColumns);
    this.fragmentContext = context;
  }

  @Override
  public void setup(OperatorContext context, OutputMutator output) throws ExecutionSetupException {
    ListenableFuture<Void> initTaskFuture = context.runCallableAs(proxyUserGroupInfo, getInitTask(output));
    try {
      initTaskFuture.get();
    } catch (InterruptedException e) {
      initTaskFuture.cancel(true);
      Thread.currentThread().interrupt();
    } catch (ExecutionException e) {
      throw ExecutionSetupException.fromThrowable(e.getMessage(), e);
    }
  }

  private Callable<Void> getInitTask(OutputMutator output) {
    return () -> {
      this.job = new JobConf(hiveConf);
      Properties hiveTableProperties = HiveUtilities.getTableMetadata(hiveTable);
      final Deserializer tableDeserializer = createDeserializer(job, hiveTable.getSd(), hiveTableProperties);
      final StructObjectInspector tableObjInspector = getStructOI(tableDeserializer);

      if (partition == null) {
        this.partitionDeserializer = tableDeserializer;
        this.partitionObjInspector = tableObjInspector;
        this.partitionToTableSchemaConverter = (obj) -> obj;
        this.finalObjInspector = tableObjInspector;

        HiveUtilities.addConfToJob(job, hiveTableProperties);
        job.setInputFormat(HiveUtilities.getInputFormatClass(job, hiveTable.getSd(), hiveTable));
        HiveUtilities.verifyAndAddTransactionalProperties(job, hiveTable.getSd());
      } else {
        Properties partitionProperties = HiveUtilities.getPartitionMetadata(partition, hiveTable);
        HiveUtilities.addConfToJob(job, partitionProperties);
        this.partitionDeserializer = createDeserializer(job, partition.getSd(), partitionProperties);
        this.partitionObjInspector = getStructOI(partitionDeserializer);

        this.finalObjInspector = (StructObjectInspector) ObjectInspectorConverters.getConvertedOI(partitionObjInspector, tableObjInspector);
        this.partitionToTableSchemaConverter = ObjectInspectorConverters.getConverter(partitionObjInspector, finalObjInspector);

        this.job.setInputFormat(HiveUtilities.getInputFormatClass(job, partition.getSd(), hiveTable));
        HiveUtilities.verifyAndAddTransactionalProperties(job, partition.getSd());
      }


      final List<FieldSchema> partitionKeyFields = hiveTable.getPartitionKeys();
      final List<String> partitionColumnNames = partitionKeyFields.stream()
          .map(FieldSchema::getName)
          .collect(Collectors.toList());
      // We should always get the columns names from ObjectInspector. For some of the tables (ex. avro) metastore
      // may not contain the schema, instead it is derived from other sources such as table properties or external file.
      // Deserializer object knows how to get the schema with all the config and table properties passed in initialization.
      // ObjectInspector created from the Deserializer object has the schema.
      final List<String> allTableColumnNames = ((StructTypeInfo) TypeInfoUtils.getTypeInfoFromObjectInspector(finalObjInspector)).getAllStructFieldNames();

      // Determining which regular/partition column names should be selected
      List<String> selectedColumnNames;
      List<String> selectedPartitionColumnNames;
      List<Integer> idsOfProjectedColumns;

      if (isStarQuery()) {
        selectedColumnNames = allTableColumnNames;
        selectedPartitionColumnNames = partitionColumnNames;
        idsOfProjectedColumns = IntStream.range(0, selectedColumnNames.size())
            .boxed()
            .collect(Collectors.toList());
      } else {
        Map<Boolean, List<String>> groupOfSelectedColumns = getColumns().stream()
            .map(SchemaPath::getRootSegment)
            .map(PathSegment.NameSegment::getPath)
            .distinct()
            .collect(Collectors.groupingBy(partitionColumnNames::contains));

        selectedColumnNames = groupOfSelectedColumns.getOrDefault(!PARTITION_COLUMNS, Collections.emptyList());
        selectedPartitionColumnNames = groupOfSelectedColumns.getOrDefault(PARTITION_COLUMNS, Collections.emptyList());
        idsOfProjectedColumns = selectedColumnNames.stream()
            .map(allTableColumnNames::indexOf)
            .collect(Collectors.toList());
      }

      List<String> nestedColumnPaths = getColumns().stream()
          .map(SchemaPath::getRootSegmentPath)
          .collect(Collectors.toList());
      ColumnProjectionUtils.appendReadColumns(job, idsOfProjectedColumns, selectedColumnNames, nestedColumnPaths);

      // Initialize selectedStructFieldRefs and columnValueWriters, which are two key collections of
      // objects used to read and save columns row data into Drill's value vectors
      this.selectedStructFieldRefs = new StructField[selectedColumnNames.size()];
      this.columnValueWriters = new HiveValueWriter[selectedColumnNames.size()];
      this.outputWriter = new VectorContainerWriter(output, /*enabled union*/ false);
      HiveValueWriterFactory hiveColumnValueWriterFactory = new HiveValueWriterFactory(drillBuf, outputWriter.getWriter());
      for (int refIdx = 0; refIdx < selectedStructFieldRefs.length; refIdx++) {
        String columnName = selectedColumnNames.get(refIdx);
        StructField fieldRef = finalObjInspector.getStructFieldRef(columnName);
        this.selectedStructFieldRefs[refIdx] = fieldRef;
        this.columnValueWriters[refIdx] = hiveColumnValueWriterFactory.createHiveColumnValueWriter(columnName, fieldRef);
      }

      // Defining selected partition vectors and values to be filled into them
      if (partition != null && selectedPartitionColumnNames.size() > 0) {
        List<ValueVector> partitionVectorList = new ArrayList<>(selectedPartitionColumnNames.size());
        List<Object> partitionValueList = new ArrayList<>(selectedPartitionColumnNames.size());
        String defaultPartitionValue = hiveConf.get(HiveConf.ConfVars.DEFAULTPARTITIONNAME.varname);
        OptionManager options = fragmentContext.getOptions();
        for (int i = 0; i < partitionKeyFields.size(); i++) {
          FieldSchema field = partitionKeyFields.get(i);
          String partitionColumnName = field.getName();
          if (selectedPartitionColumnNames.contains(partitionColumnName)) {
            TypeInfo partitionColumnTypeInfo = TypeInfoUtils.getTypeInfoFromTypeString(field.getType());
            TypeProtos.MajorType majorType = HiveUtilities.getMajorTypeFromHiveTypeInfo(partitionColumnTypeInfo, options);
            MaterializedField materializedField = MaterializedField.create(partitionColumnName, majorType);
            Class<? extends ValueVector> partitionVectorClass = TypeHelper.getValueVectorClass(materializedField.getType().getMinorType(),
                materializedField.getDataMode());

            ValueVector partitionVector = output.addField(materializedField, partitionVectorClass);
            partitionVectorList.add(partitionVector);
            Object partitionValue = HiveUtilities.convertPartitionType(partitionColumnTypeInfo, partition.getValues().get(i), defaultPartitionValue);
            partitionValueList.add(partitionValue);
          }
        }
        this.partitionVectors = partitionVectorList.toArray(new ValueVector[0]);
        this.partitionValues = partitionValueList.toArray();
      }

      if (!empty && initNextReader(job)) {
        key = mapredReader.createKey();
        valueHolder = mapredReader.createValue();
        internalInit(hiveTableProperties);
      }
      return null;
    };
  }

  /**
   * Default implementation does nothing, used to apply skip header/footer functionality
   *
   * @param hiveTableProperties hive table properties
   */
  protected void internalInit(Properties hiveTableProperties) {
  }

  @Override
  public int next() {
    outputWriter.allocate();
    outputWriter.reset();
    if (empty) {
      outputWriter.setValueCount(0);
      populatePartitionVectors(0);
      return 0;
    }

    try {
      int recordCount;
      for (recordCount = 0; (recordCount < TARGET_RECORD_COUNT && hasNextValue(valueHolder)); recordCount++) {
        Object deserializedHiveRecord = partitionToTableSchemaConverter.convert(partitionDeserializer.deserialize((Writable) valueHolder));
        outputWriter.setPosition(recordCount);
        readHiveRecordAndInsertIntoRecordBatch(deserializedHiveRecord);
      }
      outputWriter.setValueCount(recordCount);
      populatePartitionVectors(recordCount);
      return recordCount;
    } catch (ExecutionSetupException | IOException | SerDeException e) {
      throw new DrillRuntimeException(e.getMessage(), e);
    }
  }

  protected void readHiveRecordAndInsertIntoRecordBatch(Object rowValue) {
    for (int columnRefIdx = 0; columnRefIdx < selectedStructFieldRefs.length; columnRefIdx++) {
      Object columnValue = finalObjInspector.getStructFieldData(rowValue, selectedStructFieldRefs[columnRefIdx]);
      if (columnValue != null) {
        columnValueWriters[columnRefIdx].write(columnValue);
      }
    }
  }

  /**
   * Checks and reads next value of input split into valueHolder.
   * Note that if current mapredReader doesn't contain data to read from
   * InputSplit, this method will try to initialize reader for next InputSplit
   * and will try to use the new mapredReader.
   *
   * @param valueHolder holder for next row value data
   * @return true if next value present and read into valueHolder
   * @throws IOException             exception which may be thrown in case when mapredReader failed to read next value
   * @throws ExecutionSetupException exception may be thrown when next input split is present but reader
   *                                 initialization for it failed
   */
  protected boolean hasNextValue(Object valueHolder) throws IOException, ExecutionSetupException {
    while (true) {
      if (mapredReader.next(key, valueHolder)) {
        return true;
      } else if (initNextReader(job)) {
        continue;
      }
      return false;
    }
  }


  @Override
  public void close() {
    closeMapredReader();
  }

  private static Deserializer createDeserializer(JobConf job, StorageDescriptor sd, Properties properties) throws Exception {
    final Class<? extends Deserializer> c = Class.forName(sd.getSerdeInfo().getSerializationLib()).asSubclass(Deserializer.class);
    final Deserializer deserializer = c.getConstructor().newInstance();
    deserializer.initialize(job, properties);

    return deserializer;
  }

  /**
   * Get and cast deserializer's objectInspector to StructObjectInspector type
   *
   * @param deserializer hive deserializer
   * @return StructObjectInspector instance
   * @throws SerDeException in case if can't get inspector from deserializer
   */
  private static StructObjectInspector getStructOI(final Deserializer deserializer) throws SerDeException {
    ObjectInspector oi = deserializer.getObjectInspector();
    if (oi.getCategory() != ObjectInspector.Category.STRUCT) {
      throw new UnsupportedOperationException(String.format("%s category not supported", oi.getCategory()));
    }
    return (StructObjectInspector) oi;
  }


  /**
   * Helper method which fills selected partition vectors.
   * Invoked after completion of reading other records.
   *
   * @param recordCount count of records that was read by reader
   */
  private void populatePartitionVectors(int recordCount) {
    if (partition == null) {
      return;
    }
    for (int i = 0; i < partitionVectors.length; i++) {
      final ValueVector vector = partitionVectors[i];
      AllocationHelper.allocateNew(vector, recordCount);
      if (partitionValues[i] != null) {
        HiveUtilities.populateVector(vector, drillBuf, partitionValues[i], 0, recordCount);
      }
      vector.getMutator().setValueCount(recordCount);
    }
  }

  /**
   * Closes previous mapredReader if any, then initializes mapredReader for next present InputSplit,
   * or returns false when there are no more splits.
   *
   * @param job map / reduce job configuration.
   * @return true if new mapredReader initialized
   * @throws ExecutionSetupException if could not init record mapredReader
   */
  @SuppressWarnings("unchecked")
  private boolean initNextReader(JobConf job) throws ExecutionSetupException {
    if (inputSplitsIterator.hasNext()) {
      closeMapredReader();
      InputSplit inputSplit = inputSplitsIterator.next();
      try {
        mapredReader = job.getInputFormat().getRecordReader(inputSplit, job, Reporter.NULL);
        logger.trace("hive mapredReader created: {} for inputSplit {}", mapredReader.getClass().getName(), inputSplit.toString());
        return true;
      } catch (Exception e) {
        throw new ExecutionSetupException("Failed to get o.a.hadoop.mapred.RecordReader from Hive InputFormat", e);
      }
    }
    return false;
  }

  /**
   * Closes and sets mapredReader value to null.
   */
  private void closeMapredReader() {
    if (mapredReader != null) {
      try {
        mapredReader.close();
      } catch (Exception e) {
        logger.warn("Failure while closing Hive Record mapredReader.", e);
      } finally {
        mapredReader = null;
      }
    }
  }

}
