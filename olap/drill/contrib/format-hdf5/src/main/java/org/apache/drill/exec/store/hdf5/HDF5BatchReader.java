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

package org.apache.drill.exec.store.hdf5;

import io.jhdf.HdfFile;
import io.jhdf.api.Attribute;
import io.jhdf.api.Dataset;
import io.jhdf.api.Group;
import io.jhdf.api.Node;
import io.jhdf.exceptions.HdfException;
import io.jhdf.links.SoftLink;
import io.jhdf.object.datatype.CompoundDataType;
import io.jhdf.object.datatype.CompoundDataType.CompoundDataMember;
import org.apache.drill.common.AutoCloseables;
import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.impl.scan.file.FileScanFramework.FileSchemaNegotiator;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedReader;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.MapBuilder;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.hdf5.writers.HDF5DataWriter;
import org.apache.drill.exec.store.hdf5.writers.HDF5DoubleDataWriter;
import org.apache.drill.exec.store.hdf5.writers.HDF5FloatDataWriter;
import org.apache.drill.exec.store.hdf5.writers.HDF5IntDataWriter;
import org.apache.drill.exec.store.hdf5.writers.HDF5LongDataWriter;
import org.apache.drill.exec.store.hdf5.writers.HDF5MapDataWriter;
import org.apache.drill.exec.store.hdf5.writers.HDF5StringDataWriter;
import org.apache.drill.exec.store.hdf5.writers.HDF5TimestampDataWriter;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.store.hdf5.writers.WriterSpec;
import org.apache.drill.exec.vector.accessor.ArrayWriter;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.exec.vector.accessor.TupleWriter;

import org.apache.hadoop.mapred.FileSplit;
import org.jvnet.libpam.impl.CLibrary.group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HDF5BatchReader implements ManagedReader<FileSchemaNegotiator> {
  private static final Logger logger = LoggerFactory.getLogger(HDF5BatchReader.class);

  private static final String PATH_COLUMN_NAME = "path";

  private static final String DATA_TYPE_COLUMN_NAME = "data_type";

  private static final String IS_LINK_COLUMN_NAME = "is_link";

  private static final String FILE_NAME_COLUMN_NAME = "file_name";

  private static final String INT_COLUMN_PREFIX = "int_col_";

  private static final String LONG_COLUMN_PREFIX = "long_col_";

  private static final String FLOAT_COLUMN_PREFIX = "float_col_";

  private static final String DOUBLE_COLUMN_PREFIX = "double_col_";

  private static final String INT_COLUMN_NAME = "int_data";

  private static final String FLOAT_COLUMN_NAME = "float_data";

  private static final String DOUBLE_COLUMN_NAME = "double_data";

  private static final String LONG_COLUMN_NAME = "long_data";

  private static final String DATA_SIZE_COLUMN_NAME = "data_size";

  private static final String ELEMENT_COUNT_NAME = "element_count";

  private static final String DATASET_DATA_TYPE_NAME = "dataset_data_type";

  private static final String DIMENSIONS_FIELD_NAME = "dimensions";

  private static final int PREVIEW_ROW_LIMIT = 20;

  private static final int PREVIEW_COL_LIMIT = 100;

  private static final int MAX_DATASET_SIZE = ValueVector.MAX_BUFFER_SIZE;

  private final HDF5ReaderConfig readerConfig;

  private final List<HDF5DataWriter> dataWriters;

  private final int maxRecords;

  private String fileName;

  private FileSplit split;

  private HdfFile hdfFile;

  private BufferedReader reader;

  private RowSetLoader rowWriter;

  private WriterSpec writerSpec;

  private Iterator<HDF5DrillMetadata> metadataIterator;

  private ScalarWriter pathWriter;

  private ScalarWriter dataTypeWriter;

  private ScalarWriter fileNameWriter;

  private ScalarWriter linkWriter;

  private ScalarWriter dataSizeWriter;

  private ScalarWriter elementCountWriter;

  private ScalarWriter datasetTypeWriter;

  private ScalarWriter dimensionsWriter;

  private CustomErrorContext errorContext;

  private boolean showMetadataPreview;

  private int[] dimensions;

  public static class HDF5ReaderConfig {
    final HDF5FormatPlugin plugin;

    final String defaultPath;

    final HDF5FormatConfig formatConfig;

    public HDF5ReaderConfig(HDF5FormatPlugin plugin, HDF5FormatConfig formatConfig) {
      this.plugin = plugin;
      this.formatConfig = formatConfig;
      defaultPath = formatConfig.getDefaultPath();
    }
  }

  public HDF5BatchReader(HDF5ReaderConfig readerConfig, int maxRecords) {
    this.readerConfig = readerConfig;
    this.maxRecords = maxRecords;
    dataWriters = new ArrayList<>();
    this.showMetadataPreview = readerConfig.formatConfig.showPreview();
  }

  @Override
  public boolean open(FileSchemaNegotiator negotiator) {
    split = negotiator.split();
    errorContext = negotiator.parentErrorContext();
    // Since the HDF file reader uses a stream to actually read the file, the file name from the
    // module is incorrect.
    fileName = split.getPath().getName();
    try {
      openFile(negotiator);
    } catch (IOException e) {
      throw UserException
        .dataReadError(e)
        .addContext("Failed to close input file: %s", split.getPath())
        .addContext(errorContext)
        .build(logger);
    }

    ResultSetLoader loader;
    if (readerConfig.defaultPath == null) {
      // Get file metadata
      List<HDF5DrillMetadata> metadata = getFileMetadata(hdfFile, new ArrayList<>());
      metadataIterator = metadata.iterator();

      // Schema for Metadata query
      SchemaBuilder builder = new SchemaBuilder()
        .addNullable(PATH_COLUMN_NAME, MinorType.VARCHAR)
        .addNullable(DATA_TYPE_COLUMN_NAME, MinorType.VARCHAR)
        .addNullable(FILE_NAME_COLUMN_NAME, MinorType.VARCHAR)
        .addNullable(DATA_SIZE_COLUMN_NAME, MinorType.BIGINT)
        .addNullable(IS_LINK_COLUMN_NAME, MinorType.BIT)
        .addNullable(ELEMENT_COUNT_NAME, MinorType.BIGINT)
        .addNullable(DATASET_DATA_TYPE_NAME, MinorType.VARCHAR)
        .addNullable(DIMENSIONS_FIELD_NAME, MinorType.VARCHAR);

      negotiator.tableSchema(builder.buildSchema(), false);

      loader = negotiator.build();
      dimensions = new int[0];
      rowWriter = loader.writer();

    } else {
      // This is the case when the default path is specified. Since the user is explicitly asking for a dataset
      // Drill can obtain the schema by getting the datatypes below and ultimately mapping that schema to columns
      Dataset dataSet = hdfFile.getDatasetByPath(readerConfig.defaultPath);
      dimensions = dataSet.getDimensions();

      loader = negotiator.build();
      rowWriter = loader.writer();
      writerSpec = new WriterSpec(rowWriter, negotiator.providedSchema(),
          negotiator.parentErrorContext());
      if (dimensions.length <= 1) {
        buildSchemaFor1DimensionalDataset(dataSet);
      } else if (dimensions.length == 2) {
        buildSchemaFor2DimensionalDataset(dataSet);
      } else {
        // Case for datasets of greater than 2D
        // These are automatically flattened
        buildSchemaFor2DimensionalDataset(dataSet);
      }
    }
    if (readerConfig.defaultPath == null) {
      pathWriter = rowWriter.scalar(PATH_COLUMN_NAME);
      dataTypeWriter = rowWriter.scalar(DATA_TYPE_COLUMN_NAME);
      fileNameWriter = rowWriter.scalar(FILE_NAME_COLUMN_NAME);
      dataSizeWriter = rowWriter.scalar(DATA_SIZE_COLUMN_NAME);
      linkWriter = rowWriter.scalar(IS_LINK_COLUMN_NAME);
      elementCountWriter = rowWriter.scalar(ELEMENT_COUNT_NAME);
      datasetTypeWriter = rowWriter.scalar(DATASET_DATA_TYPE_NAME);
      dimensionsWriter = rowWriter.scalar(DIMENSIONS_FIELD_NAME);
    }
    return true;
  }

  /**
   * This function is called when the default path is set and the data set is a single dimension.
   * This function will create an array of one dataWriter of the
   * correct datatype
   * @param dataset The HDF5 dataset
   */
  private void buildSchemaFor1DimensionalDataset(Dataset dataset) {
    MinorType currentDataType = HDF5Utils.getDataType(dataset.getDataType());

    // Case for null or unknown data types:
    if (currentDataType == null) {
      logger.warn("Couldn't add {}", dataset.getJavaType().getName());
      return;
    }
    dataWriters.add(buildWriter(currentDataType));
  }

  private HDF5DataWriter buildWriter(MinorType dataType) {
    switch (dataType) {
      /*case GENERIC_OBJECT:
        return new HDF5EnumDataWriter(hdfFile, writerSpec, readerConfig.defaultPath);*/
      case VARCHAR:
        return new HDF5StringDataWriter(hdfFile, writerSpec, readerConfig.defaultPath);
      case TIMESTAMP:
        return new HDF5TimestampDataWriter(hdfFile, writerSpec, readerConfig.defaultPath);
      case INT:
        return new HDF5IntDataWriter(hdfFile, writerSpec, readerConfig.defaultPath);
      case BIGINT:
        return new HDF5LongDataWriter(hdfFile, writerSpec, readerConfig.defaultPath);
      case FLOAT8:
        return new HDF5DoubleDataWriter(hdfFile, writerSpec, readerConfig.defaultPath);
      case FLOAT4:
        return new HDF5FloatDataWriter(hdfFile, writerSpec, readerConfig.defaultPath);
      case MAP:
        return new HDF5MapDataWriter(hdfFile, writerSpec, readerConfig.defaultPath);
       default:
        throw new UnsupportedOperationException(dataType.name());
    }
  }

  /**
   * Builds a Drill schema from a dataset with 2 or more dimensions. HDF5 only
   * supports INT, LONG, DOUBLE and FLOAT for >2 data types so this function is
   * not as inclusive as the 1D function. This function will build the schema
   * by adding DataWriters to the dataWriters array.
   *
   * @param dataset
   *          The dataset which Drill will use to build a schema
   */
  private void buildSchemaFor2DimensionalDataset(Dataset dataset) {
    MinorType currentDataType = HDF5Utils.getDataType(dataset.getDataType());
    // Case for null or unknown data types:
    if (currentDataType == null) {
      logger.warn("Couldn't add {}",dataset.getJavaType().getName());
      return;
    }
    long cols = dimensions[1];

    String tempFieldName;
    for (int i = 0; i < cols; i++) {
      switch (currentDataType) {
        case INT:
          tempFieldName = INT_COLUMN_PREFIX + i;
          dataWriters.add(new HDF5IntDataWriter(hdfFile, writerSpec, readerConfig.defaultPath, tempFieldName, i));
          break;
        case BIGINT:
          tempFieldName = LONG_COLUMN_PREFIX + i;
          dataWriters.add(new HDF5LongDataWriter(hdfFile, writerSpec, readerConfig.defaultPath, tempFieldName, i));
          break;
        case FLOAT8:
          tempFieldName = DOUBLE_COLUMN_PREFIX + i;
          dataWriters.add(new HDF5DoubleDataWriter(hdfFile, writerSpec, readerConfig.defaultPath, tempFieldName, i));
          break;
        case FLOAT4:
          tempFieldName = FLOAT_COLUMN_PREFIX + i;
          dataWriters.add(new HDF5FloatDataWriter(hdfFile, writerSpec, readerConfig.defaultPath, tempFieldName, i));
          break;
        default:
          throw new UnsupportedOperationException(currentDataType.name());
      }
    }
  }
  /**
   * Opens an HDF5 file.
   * @param negotiator The negotiator represents Drill's interface with the file system
   */
  private void openFile(FileSchemaNegotiator negotiator) throws IOException {
    InputStream in = null;
    try {
      /*
       * As a possible future improvement, the jhdf reader has the ability to read hdf5 files from
       * a byte array or byte buffer. This implementation is better in that it does not require creating
       * a temporary file which must be deleted later.  However, it could result in memory issues in the
       * event of large files.
       */

      in = negotiator.fileSystem().openPossiblyCompressedStream(split.getPath());
      hdfFile = HdfFile.fromInputStream(in);
    } catch (Exception e) {
      if (in != null) {
        in.close();
      }
      throw UserException
        .dataReadError(e)
        .message("Failed to open input file: %s", split.getPath())
        .addContext(errorContext)
        .build(logger);
    }
    reader = new BufferedReader(new InputStreamReader(in));
  }

  @Override
  public boolean next() {

    // Limit pushdown
    if (rowWriter.limitReached(maxRecords)) {
      return false;
    }

    while (!rowWriter.isFull()) {
      if (readerConfig.defaultPath == null || readerConfig.defaultPath.isEmpty()) {
        if (!metadataIterator.hasNext()){
          return false;
        }
        projectMetadataRow(rowWriter);
      } else if (dimensions.length <= 1 && dataWriters.get(0).isCompound()) {
        if (!dataWriters.get(0).hasNext()) {
          return false;
        }
        rowWriter.start();
        dataWriters.get(0).write();
        rowWriter.save();
      } else if (dimensions.length <= 1) {
        // Case for Compound Data Type
        if (!dataWriters.get(0).hasNext()) {
          return false;
        }
        rowWriter.start();
        dataWriters.get(0).write();
        rowWriter.save();
      } else {
        int currentRowCount = 0;
        HDF5DataWriter currentDataWriter;
        rowWriter.start();

        for (int i = 0; i < dimensions[1]; i++) {
          currentDataWriter = dataWriters.get(i);
          currentDataWriter.write();
          currentRowCount = currentDataWriter.currentRowCount();
        }
        rowWriter.save();
        if (currentRowCount >= dimensions[0]) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Writes one row of HDF5 metadata.
   * @param rowWriter The input rowWriter object
   */
  private void projectMetadataRow(RowSetLoader rowWriter) {
    HDF5DrillMetadata metadataRow = metadataIterator.next();
    rowWriter.start();

    pathWriter.setString(metadataRow.getPath());
    dataTypeWriter.setString(metadataRow.getDataType());
    fileNameWriter.setString(fileName);
    linkWriter.setBoolean(metadataRow.isLink());

    //Write attributes if present
    if (metadataRow.getAttributes().size() > 0) {
      writeAttributes(rowWriter, metadataRow);
    }

    if (metadataRow.getDataType().equalsIgnoreCase("DATASET")) {
      Dataset dataset = hdfFile.getDatasetByPath(metadataRow.getPath());
      // Project Dataset Metadata
      dataSizeWriter.setLong(dataset.getSizeInBytes());
      elementCountWriter.setLong(dataset.getSize());
      datasetTypeWriter.setString(dataset.getJavaType().getName());
      dimensionsWriter.setString(Arrays.toString(dataset.getDimensions()));

      // Do not project links
      if (! metadataRow.isLink() && showMetadataPreview) {
        projectDataset(rowWriter, metadataRow.getPath());
      }
    }
    rowWriter.save();
  }

  /**
   * Gets the file metadata from a given HDF5 file. It will extract the file
   * name the path, and adds any information to the metadata List.
   *
   * @param group A list of paths from which the metadata will be extracted
   * @param metadata The HDF5 metadata object from which the metadata will be extracted
   * @return A list of metadata from the given file paths
   */
  private List<HDF5DrillMetadata> getFileMetadata(Group group, List<HDF5DrillMetadata> metadata) {
    Map<String, HDF5Attribute> attribs;

    // The JHDF5 library seems to be unable to read certain nodes.  This try/catch block verifies
    // that the group can be read.
    try {
      group.getChildren();
    } catch (HdfException e) {
      logger.warn(e.getMessage());
      return metadata;
    }
    for (Node node : group) {
      HDF5DrillMetadata metadataRow = new HDF5DrillMetadata();

      if (node.isLink()) {
        SoftLink link = (SoftLink) node;
        metadataRow.setPath(link.getTargetPath());
        metadataRow.setLink(true);
      } else {
        metadataRow.setPath(node.getPath());
        metadataRow.setDataType(node.getType().name());
        metadataRow.setLink(false);
        switch (node.getType()) {
          case DATASET:
            attribs = getAttributes(node.getPath());
            metadataRow.setAttributes(attribs);
            metadata.add(metadataRow);
            break;
          case GROUP:
            attribs = getAttributes(node.getPath());
            metadataRow.setAttributes(attribs);
            metadata.add(metadataRow);
            getFileMetadata((Group) node, metadata);
            break;
          default:
            logger.warn("Unknown data type: {}", node.getType());
        }
      }
    }
    return metadata;
  }

  /**
   * Gets the attributes of a HDF5 dataset and returns them into a HashMap
   *
   * @param path The path for which you wish to retrieve attributes
   * @return Map The attributes for the given path.  Empty Map if no attributes present
   */
  private Map<String, HDF5Attribute> getAttributes(String path) {
    Map<String, Attribute> attributeList;
    // Remove trailing slashes
    if (path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }

    logger.debug("Getting attributes for {}", path);
    Map<String, HDF5Attribute> attributes = new HashMap<>();
    Node theNode;
    try {
      theNode = hdfFile.getByPath(path);
    } catch (Exception e) {
      // Couldn't find node
      logger.debug("Couldn't get attributes for path: {}", path);
      logger.debug("Error: {}", e.getMessage());
      return attributes;
    }

    try {
      attributeList = theNode.getAttributes();
    } catch (HdfException e) {
      logger.warn("Unable to get attributes for {}: Only Huge objects BTrees with 1 record are currently supported.", path);
      return attributes;
    }

    logger.debug("Found {} attribtutes for {}", attributeList.size(), path);
    for (Map.Entry<String, Attribute> attributeEntry : attributeList.entrySet()) {
      HDF5Attribute attribute = HDF5Utils.getAttribute(path, attributeEntry.getKey(), hdfFile);

      // Ignore compound attributes.
      if (attribute != null && attributeEntry.getValue().isScalar()) {
        logger.debug("Adding {} to attribute list for {}", attribute.getKey(), path);
        attributes.put(attribute.getKey(), attribute);
      }
    }
    return attributes;
  }

  /**
   * Writes one row of data in a metadata query. The number of dimensions here
   * is n+1. So if the actual dataset is a 1D column, it will be written as a list.
   * This is function is only called in metadata queries as the schema is not
   * known in advance. If the datasize is greater than 16MB, the function does
   * not project the dataset
   *
   * @param rowWriter
   *          The rowWriter to which the data will be written
   * @param datapath
   *          The datapath from which the data will be read
   */

  private void projectDataset(RowSetLoader rowWriter, String datapath) {
    String fieldName = HDF5Utils.getNameFromPath(datapath);
    Dataset dataset= hdfFile.getDatasetByPath(datapath);

    // If the dataset is larger than 16MB, do not project the dataset
    if (dataset.getSizeInBytes() > MAX_DATASET_SIZE) {
      logger.warn("Dataset {} is greater than 16MB.  Data will be truncated in Metadata view.", datapath);
    }

    int[] dimensions = dataset.getDimensions();
    //Case for single dimensional data
    if (dimensions.length == 1) {
      MinorType currentDataType = HDF5Utils.getDataType(dataset.getDataType());

      Object data;
      try {
        data = dataset.getData();
      } catch (Exception e) {
        logger.debug("Error reading {}", datapath);
        return;
      }
      assert currentDataType != null;

      // Skip null datasets
      if (data == null) {
        return;
      }

      switch (currentDataType) {
        case GENERIC_OBJECT:
          logger.warn("Couldn't read {}", datapath );
          break;
        case VARCHAR:
          String[] stringData = (String[])data;
          writeStringListColumn(rowWriter, fieldName, stringData);
          break;
        case TIMESTAMP:
          long[] longList = (long[])data;
          writeTimestampListColumn(rowWriter, fieldName, longList);
          break;
        case INT:
          int[] intList = (int[])data;
          writeIntListColumn(rowWriter, fieldName, intList);
          break;
        case SMALLINT:
          short[] shortList = (short[])data;
          writeSmallIntColumn(rowWriter, fieldName, shortList);
          break;
        case TINYINT:
          byte[] byteList = (byte[])data;
          writeByteListColumn(rowWriter, fieldName, byteList);
          break;
        case FLOAT4:
          float[] tempFloatList = (float[])data;
          writeFloat4ListColumn(rowWriter, fieldName, tempFloatList);
          break;
        case FLOAT8:
          double[] tempDoubleList = (double[])data;
          writeFloat8ListColumn(rowWriter, fieldName, tempDoubleList);
          break;
        case BIGINT:
          long[] tempBigIntList = (long[])data;
          writeLongListColumn(rowWriter, fieldName, tempBigIntList);
          break;
        case MAP:
          try {
            getAndMapCompoundData(datapath, hdfFile, rowWriter);
          } catch (Exception e) {
            throw UserException
              .dataReadError()
              .message("Error writing Compound Field: " + e.getMessage())
              .addContext(errorContext)
              .build(logger);
          }
          break;
        default:
          // Case for data types that cannot be read
          logger.warn("{} not implemented.", currentDataType.name());
      }
    } else if (dimensions.length == 2) {
      // Case for 2D data sets.  These are projected as lists of lists or maps of maps
      int cols = dimensions[1];
      int rows = dimensions[0];

      // TODO Add Boolean, Small and TinyInt data types
      switch (HDF5Utils.getDataType(dataset.getDataType())) {
        case INT:
          int[][] colData = (int[][])dataset.getData();
          mapIntMatrixField(colData, cols, rows, rowWriter);
          break;
        case FLOAT4:
          float[][] floatData = (float[][])dataset.getData();
          mapFloatMatrixField(floatData, cols, rows, rowWriter);
          break;
        case FLOAT8:
          double[][] doubleData = (double[][])dataset.getData();
          mapDoubleMatrixField(doubleData, cols, rows, rowWriter);
          break;
        case BIGINT:
          long[][] longData = (long[][])dataset.getData();
          mapBigIntMatrixField(longData, cols, rows, rowWriter);
          break;
        default:
          logger.warn("{} not implemented.", HDF5Utils.getDataType(dataset.getDataType()));
      }
    } else if (dimensions.length > 2){
      // Case for data sets with dimensions > 2
      int cols = dimensions[1];
      int rows = dimensions[0];
      switch (HDF5Utils.getDataType(dataset.getDataType())) {
        case INT:
          int[][] intMatrix = HDF5Utils.toIntMatrix((Object[]) dataset.getData());
          mapIntMatrixField(intMatrix, cols, rows, rowWriter);
          break;
        case FLOAT4:
          float[][] floatData = HDF5Utils.toFloatMatrix((Object[]) dataset.getData());
          mapFloatMatrixField(floatData, cols, rows, rowWriter);
          break;
        case FLOAT8:
          double[][] doubleData = HDF5Utils.toDoubleMatrix((Object[]) dataset.getData());
          mapDoubleMatrixField(doubleData, cols, rows, rowWriter);
          break;
        case BIGINT:
          long[][] longData = HDF5Utils.toLongMatrix((Object[]) dataset.getData());
          mapBigIntMatrixField(longData, cols, rows, rowWriter);
          break;
        default:
          logger.warn("{} not implemented.", HDF5Utils.getDataType(dataset.getDataType()));
      }
    }
  }

  /**
   * Helper function to write a 1D boolean column
   *
   * @param rowWriter The row to which the data will be written
   * @param name The column name
   * @param value The value to be written
   */
  private void writeBooleanColumn(TupleWriter rowWriter, String name, int value) {
    writeBooleanColumn(rowWriter, name, value != 0);
  }

  /**
   * Helper function to write a 1D boolean column
   *
   * @param rowWriter The row to which the data will be written
   * @param name The column name
   * @param value The value to be written
   */
  private void writeBooleanColumn(TupleWriter rowWriter, String name, boolean value) {
    ScalarWriter colWriter = getColWriter(rowWriter, name, TypeProtos.MinorType.BIT);
    colWriter.setBoolean(value);
  }

  /**
   * Helper function to write a 1D short column
   *
   * @param rowWriter The row to which the data will be written
   * @param name The column name
   * @param value The value to be written
   */
  private void writeSmallIntColumn(TupleWriter rowWriter, String name, short value) {
    ScalarWriter colWriter = getColWriter(rowWriter, name, MinorType.SMALLINT);
    colWriter.setInt(value);
  }

  /**
   * Helper function to write a 2D short list
   * @param rowWriter the row to which the data will be written
   * @param name the name of the outer list
   * @param list the list of data
   */
  private void writeSmallIntColumn(TupleWriter rowWriter, String name, short[] list) {
    int index = rowWriter.tupleSchema().index(name);
    if (index == -1) {
      ColumnMetadata colSchema = MetadataUtils.newScalar(name, MinorType.SMALLINT, TypeProtos.DataMode.REPEATED);
      index = rowWriter.addColumn(colSchema);
    }

    ScalarWriter arrayWriter = rowWriter.column(index).array().scalar();
    int maxElements = Math.min(list.length, PREVIEW_ROW_LIMIT);
    for (int i = 0; i < maxElements; i++) {
      arrayWriter.setInt(list[i]);
    }
  }

  /**
   * Helper function to write a 1D byte column
   *
   * @param rowWriter The row to which the data will be written
   * @param name The column name
   * @param value The value to be written
   */
  private void writeByteColumn(TupleWriter rowWriter, String name, byte value) {
    ScalarWriter colWriter = getColWriter(rowWriter, name, MinorType.TINYINT);
    colWriter.setInt(value);
  }

  /**
   * Helper function to write a 2D byte list
   * @param rowWriter the row to which the data will be written
   * @param name the name of the outer list
   * @param list the list of data
   */
  private void writeByteListColumn(TupleWriter rowWriter, String name, byte[] list) {
    int index = rowWriter.tupleSchema().index(name);
    if (index == -1) {
      ColumnMetadata colSchema = MetadataUtils.newScalar(name, MinorType.TINYINT, TypeProtos.DataMode.REPEATED);
      index = rowWriter.addColumn(colSchema);
    }

    ScalarWriter arrayWriter = rowWriter.column(index).array().scalar();
    int maxElements = Math.min(list.length, PREVIEW_ROW_LIMIT);
    for (int i = 0; i < maxElements; i++) {
      arrayWriter.setInt(list[i]);
    }
  }

  /**
   * Helper function to write a 1D int column
   *
   * @param rowWriter The row to which the data will be written
   * @param name The column name
   * @param value The value to be written
   */
  private void writeIntColumn(TupleWriter rowWriter, String name, int value) {
    ScalarWriter colWriter = getColWriter(rowWriter, name, TypeProtos.MinorType.INT);
    colWriter.setInt(value);
  }

  /**
   * Helper function to write a 2D int list
   * @param rowWriter the row to which the data will be written
   * @param name the name of the outer list
   * @param list the list of data
   */
  private void writeIntListColumn(TupleWriter rowWriter, String name, int[] list) {
    int index = rowWriter.tupleSchema().index(name);
    if (index == -1) {
      ColumnMetadata colSchema = MetadataUtils.newScalar(name, TypeProtos.MinorType.INT, TypeProtos.DataMode.REPEATED);
      index = rowWriter.addColumn(colSchema);
    }

    ScalarWriter arrayWriter = rowWriter.column(index).array().scalar();
    int maxElements = Math.min(list.length, PREVIEW_ROW_LIMIT);
    for (int i = 0; i < maxElements; i++) {
      arrayWriter.setInt(list[i]);
    }
  }

  private void mapIntMatrixField(int[][] colData, int cols, int rows, RowSetLoader rowWriter) {
    // If the default path is not null, auto flatten the data
    // The end result are that a 2D array gets mapped to Drill columns
    if (readerConfig.defaultPath != null) {
      for (int i = 0; i < rows; i++) {
        rowWriter.start();
        for (int k = 0; k < cols; k++) {
          String tempColumnName = INT_COLUMN_PREFIX + k;
          writeIntColumn(rowWriter, tempColumnName, colData[i][k]);
        }
        rowWriter.save();
      }
    } else {
      intMatrixHelper(colData, cols, rows, rowWriter);
    }
  }

  private void intMatrixHelper(int[][] colData, int cols, int rows, RowSetLoader rowWriter) {

    // This is the case where a dataset is projected in a metadata query.
    // The result should be a list of lists
    TupleMetadata nestedSchema = new SchemaBuilder()
      .addRepeatedList(INT_COLUMN_NAME)
      .addArray(TypeProtos.MinorType.INT)
      .resumeSchema()
      .buildSchema();

    int index = rowWriter.tupleSchema().index(INT_COLUMN_NAME);
    if (index == -1) {
      index = rowWriter.addColumn(nestedSchema.column(INT_COLUMN_NAME));
    }

    // The outer array
    ArrayWriter listWriter = rowWriter.column(index).array();
    // The inner array
    ArrayWriter innerWriter = listWriter.array();
    // The strings within the inner array
    ScalarWriter intWriter = innerWriter.scalar();

    int maxElements = Math.min(rows, PREVIEW_ROW_LIMIT);
    for (int i = 0; i < maxElements; i++) {
      for (int k = 0; k < cols; k++) {
        intWriter.setInt(colData[i][k]);
      }
      listWriter.save();
    }
  }

  private void writeLongColumn(TupleWriter rowWriter, String name, long value) {
    ScalarWriter colWriter = getColWriter(rowWriter, name, TypeProtos.MinorType.BIGINT);
    colWriter.setLong(value);
  }

  private void writeLongListColumn(TupleWriter rowWriter, String name, long[] list) {
    int index = rowWriter.tupleSchema().index(name);
    if (index == -1) {
      ColumnMetadata colSchema = MetadataUtils.newScalar(name, TypeProtos.MinorType.BIGINT, TypeProtos.DataMode.REPEATED);
      index = rowWriter.addColumn(colSchema);
    }

    ScalarWriter arrayWriter = rowWriter.column(index).array().scalar();
    int maxElements = Math.min(list.length, PREVIEW_ROW_LIMIT);
    for (int i = 0; i < maxElements; i++) {
      arrayWriter.setLong(list[i]);
    }
  }

  private void writeStringColumn(TupleWriter rowWriter, String name, String value) {
    ScalarWriter colWriter = getColWriter(rowWriter, name, TypeProtos.MinorType.VARCHAR);
    colWriter.setString(value);
  }

  private void writeStringListColumn(TupleWriter rowWriter, String name, String[] list) {
    int index = rowWriter.tupleSchema().index(name);
    if (index == -1) {
      ColumnMetadata colSchema = MetadataUtils.newScalar(name, TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.REPEATED);
      index = rowWriter.addColumn(colSchema);
    }

    ScalarWriter arrayWriter = rowWriter.column(index).array().scalar();
    int maxElements = Math.min(list.length, PREVIEW_ROW_LIMIT);
    for (int i = 0; i < maxElements; i++) {
      arrayWriter.setString(list[i]);
    }
  }

  private void writeFloat8Column(TupleWriter rowWriter, String name, double value) {
    ScalarWriter colWriter = getColWriter(rowWriter, name, TypeProtos.MinorType.FLOAT8);
    colWriter.setDouble(value);
  }

  private void writeFloat8ListColumn(TupleWriter rowWriter, String name, double[] list) {
    int index = rowWriter.tupleSchema().index(name);
    if (index == -1) {
      ColumnMetadata colSchema = MetadataUtils.newScalar(name, TypeProtos.MinorType.FLOAT8, TypeProtos.DataMode.REPEATED);
      index = rowWriter.addColumn(colSchema);
    }

    ScalarWriter arrayWriter = rowWriter.column(index).array().scalar();
    int maxElements = Math.min(list.length, PREVIEW_ROW_LIMIT);
    for (int i = 0; i < maxElements; i++) {
      arrayWriter.setDouble(list[i]);
    }
  }

  private void writeFloat4Column(TupleWriter rowWriter, String name, float value) {
    ScalarWriter colWriter = getColWriter(rowWriter, name, TypeProtos.MinorType.FLOAT4);
    colWriter.setDouble(value);
  }

  private void writeFloat4ListColumn(TupleWriter rowWriter, String name, float[] list) {
    int index = rowWriter.tupleSchema().index(name);
    if (index == -1) {
      ColumnMetadata colSchema = MetadataUtils.newScalar(name, TypeProtos.MinorType.FLOAT4, TypeProtos.DataMode.REPEATED);
      index = rowWriter.addColumn(colSchema);
    }

    ScalarWriter arrayWriter = rowWriter.column(index).array().scalar();
    int maxElements = Math.min(list.length, PREVIEW_ROW_LIMIT);
    for (int i = 0; i < maxElements; i++) {
      arrayWriter.setDouble(list[i]);
    }
  }

  private void mapFloatMatrixField(float[][] colData, int cols, int rows, RowSetLoader rowWriter) {
    // If the default path is not null, auto flatten the data
    // The end result are that a 2D array gets mapped to Drill columns
    if (readerConfig.defaultPath != null) {
      for (int i = 0; i < rows; i++) {
        rowWriter.start();
        for (int k = 0; k < cols; k++) {
          String tempColumnName = FLOAT_COLUMN_PREFIX + k;
          writeFloat4Column(rowWriter, tempColumnName, colData[i][k]);
        }
        rowWriter.save();
      }
    } else {
      floatMatrixHelper(colData, cols, rows, rowWriter);
    }
  }

  private void floatMatrixHelper(float[][] colData, int cols, int rows, RowSetLoader rowWriter) {
    // This is the case where a dataset is projected in a metadata query.  The result should be a list of lists

    TupleMetadata nestedSchema = new SchemaBuilder()
      .addRepeatedList(FLOAT_COLUMN_NAME)
      .addArray(TypeProtos.MinorType.FLOAT4)
      .resumeSchema()
      .buildSchema();

    int index = rowWriter.tupleSchema().index(FLOAT_COLUMN_NAME);
    if (index == -1) {
      index = rowWriter.addColumn(nestedSchema.column(FLOAT_COLUMN_NAME));
    }

    // The outer array
    ArrayWriter listWriter = rowWriter.column(index).array();
    // The inner array
    ArrayWriter innerWriter = listWriter.array();
    // The strings within the inner array
    ScalarWriter floatWriter = innerWriter.scalar();
    int maxElements = Math.min(colData.length, PREVIEW_ROW_LIMIT);
    int maxCols = Math.min(colData[0].length, PREVIEW_COL_LIMIT);
    for (int i = 0; i < maxElements; i++) {
      for (int k = 0; k < maxCols; k++) {
        floatWriter.setDouble(colData[i][k]);
      }
      listWriter.save();
    }
  }

  private void mapDoubleMatrixField(double[][] colData, int cols, int rows, RowSetLoader rowWriter) {
    // If the default path is not null, auto flatten the data
    // The end result are that a 2D array gets mapped to Drill columns
    if (readerConfig.defaultPath != null) {
      for (int i = 0; i < rows; i++) {
        rowWriter.start();
        for (int k = 0; k < cols; k++) {
          String tempColumnName = DOUBLE_COLUMN_PREFIX + k;
          writeFloat8Column(rowWriter, tempColumnName, colData[i][k]);
        }
        rowWriter.save();
      }
    } else {
      doubleMatrixHelper(colData, cols, rows, rowWriter);
    }
  }

  private void doubleMatrixHelper(double[][] colData, int cols, int rows, RowSetLoader rowWriter) {
    // This is the case where a dataset is projected in a metadata query.  The result should be a list of lists

    TupleMetadata nestedSchema = new SchemaBuilder()
      .addRepeatedList(DOUBLE_COLUMN_NAME)
      .addArray(TypeProtos.MinorType.FLOAT8)
      .resumeSchema()
      .buildSchema();

    int index = rowWriter.tupleSchema().index(DOUBLE_COLUMN_NAME);
    if (index == -1) {
      index = rowWriter.addColumn(nestedSchema.column(DOUBLE_COLUMN_NAME));
    }

    // The outer array
    ArrayWriter listWriter = rowWriter.column(index).array();
    // The inner array
    ArrayWriter innerWriter = listWriter.array();
    // The strings within the inner array
    ScalarWriter floatWriter = innerWriter.scalar();

    int maxElements = Math.min(colData.length, PREVIEW_ROW_LIMIT);
    int maxCols = Math.min(colData[0].length, PREVIEW_COL_LIMIT);
    for (int i = 0; i < maxElements; i++) {
      for (int k = 0; k < maxCols; k++) {
        floatWriter.setDouble(colData[i][k]);
      }
      listWriter.save();
    }
  }

  private void mapBigIntMatrixField(long[][] colData, int cols, int rows, RowSetLoader rowWriter) {
    // If the default path is not null, auto flatten the data
    // The end result are that a 2D array gets mapped to Drill columns
    if (readerConfig.defaultPath != null) {
      for (int i = 0; i < rows; i++) {
        rowWriter.start();
        for (int k = 0; k < cols; k++) {
          String tempColumnName = LONG_COLUMN_PREFIX + k;
          writeLongColumn(rowWriter, tempColumnName, colData[i][k]);
        }
        rowWriter.save();
      }
    } else {
      bigIntMatrixHelper(colData, cols, rows, rowWriter);
    }
  }

  private void bigIntMatrixHelper(long[][] colData, int cols, int rows, RowSetLoader rowWriter) {
    // This is the case where a dataset is projected in a metadata query.  The result should be a list of lists

    TupleMetadata nestedSchema = new SchemaBuilder()
      .addRepeatedList(LONG_COLUMN_NAME)
      .addArray(TypeProtos.MinorType.BIGINT)
      .resumeSchema()
      .buildSchema();

    int index = rowWriter.tupleSchema().index(LONG_COLUMN_NAME);
    if (index == -1) {
      index = rowWriter.addColumn(nestedSchema.column(LONG_COLUMN_NAME));
    }

    // The outer array
    ArrayWriter listWriter = rowWriter.column(index).array();
    // The inner array
    ArrayWriter innerWriter = listWriter.array();
    // The strings within the inner array
    ScalarWriter bigintWriter = innerWriter.scalar();

    int maxElements = Math.min(colData.length, PREVIEW_ROW_LIMIT);
    int maxCols = Math.min(colData[0].length, PREVIEW_COL_LIMIT);
    for (int i = 0; i < maxElements; i++) {
      for (int k = 0; k < maxCols; k++) {
        bigintWriter.setLong(colData[i][k]);
      }
      listWriter.save();
    }
  }

  private void writeTimestampColumn(TupleWriter rowWriter, String name, long timestamp) {
    Instant ts = Instant.ofEpochMilli(timestamp);
    ScalarWriter colWriter = getColWriter(rowWriter, name, TypeProtos.MinorType.TIMESTAMP);
    colWriter.setTimestamp(ts);
  }

  private void writeTimestampListColumn(TupleWriter rowWriter, String name, long[] list) {
    int index = rowWriter.tupleSchema().index(name);
    if (index == -1) {
      ColumnMetadata colSchema = MetadataUtils.newScalar(name, TypeProtos.MinorType.TIMESTAMP, TypeProtos.DataMode.REPEATED);
      index = rowWriter.addColumn(colSchema);
    }

    ScalarWriter arrayWriter = rowWriter.column(index).array().scalar();
    for (long l : list) {
      arrayWriter.setTimestamp(Instant.ofEpochMilli(l));
    }
  }

  private ScalarWriter getColWriter(TupleWriter tupleWriter, String fieldName, TypeProtos.MinorType type) {
    int index = tupleWriter.tupleSchema().index(fieldName);
    if (index == -1) {
      ColumnMetadata colSchema = MetadataUtils.newScalar(fieldName, type, TypeProtos.DataMode.OPTIONAL);
      index = tupleWriter.addColumn(colSchema);
    }
    return tupleWriter.scalar(index);
  }

  /**
   * Gets the attributes for an HDF5 datapath. These attributes are projected as
   * a map in select * queries when the defaultPath is null.
   *
   * @param rowWriter
   *          the row to which the data will be written
   * @param record
   *          the record for the attributes
   */
  private void writeAttributes(TupleWriter rowWriter, HDF5DrillMetadata record) {
    Map<String, HDF5Attribute> attribs = getAttributes(record.getPath());
    Iterator<Map.Entry<String, HDF5Attribute>> entries = attribs.entrySet().iterator();

    int index = rowWriter.tupleSchema().index("attributes");
    if (index == -1) {
      index = rowWriter
        .addColumn(SchemaBuilder.columnSchema("attributes", TypeProtos.MinorType.MAP, TypeProtos.DataMode.REQUIRED));
    }
    TupleWriter mapWriter = rowWriter.tuple(index);

    while (entries.hasNext()) {
      Map.Entry<String, HDF5Attribute> entry = entries.next();
      String key = entry.getKey();

      HDF5Attribute attrib = entry.getValue();
      switch (attrib.getDataType()) {
        case BIT:
          writeBooleanColumn(mapWriter, key, (Boolean) attrib.getValue());
          break;
        case BIGINT:
          writeLongColumn(mapWriter, key, (Long) attrib.getValue());
          break;
        case INT:
          writeIntColumn(mapWriter, key, (Integer) attrib.getValue());
          break;
        case SMALLINT:
          writeSmallIntColumn(mapWriter, key, (Short) attrib.getValue());
          break;
        case TINYINT:
          writeByteColumn(mapWriter, key, (Byte) attrib.getValue());
          break;
        case FLOAT8:
          writeFloat8Column(mapWriter, key, (Double) attrib.getValue());
          break;
        case FLOAT4:
          writeFloat4Column(mapWriter, key, (Float) attrib.getValue());
          break;
        case VARCHAR:
          writeStringColumn(mapWriter, key, (String) attrib.getValue());
          break;
        case TIMESTAMP:
          writeTimestampColumn(mapWriter, key, (Long) attrib.getValue());
          break;
        case GENERIC_OBJECT:
          //This is the case for HDF5 enums
          String enumText = attrib.getValue().toString();
          writeStringColumn(mapWriter, key, enumText);
          break;
        default:
          throw new IllegalStateException(attrib.getDataType().name());
      }
    }
  }

  /**
   * Processes the MAP data type which can be found in HDF5 files.
   * It automatically flattens anything greater than 2 dimensions.
   *
   * @param path the HDF5 path tp the compound data
   * @param reader the HDF5 reader for the data file
   * @param rowWriter the rowWriter to write the data
   */
  private void getAndMapCompoundData(String path, HdfFile reader, RowSetLoader rowWriter) {

    final String COMPOUND_DATA_FIELD_NAME = "compound_data";
    List<CompoundDataMember> data = ((CompoundDataType) reader.getDatasetByPath(path).getDataType()).getMembers();
    int index;

    // Add map to schema
    SchemaBuilder innerSchema = new SchemaBuilder();
    MapBuilder mapBuilder = innerSchema.addMap(COMPOUND_DATA_FIELD_NAME);

    // Loop to build schema
    for (CompoundDataMember dataMember : data) {
      String dataType = dataMember.getDataType().getJavaType().getName();
      String fieldName = dataMember.getName();

      switch (dataType) {
        case "byte":
          mapBuilder.add(fieldName, MinorType.TINYINT, DataMode.REPEATED);
          break;
        case "short":
          mapBuilder.add(fieldName, MinorType.SMALLINT, DataMode.REPEATED);
          break;
        case "int":
          mapBuilder.add(fieldName, MinorType.INT, DataMode.REPEATED);
          break;
        case "double":
          mapBuilder.add(fieldName, MinorType.FLOAT8, DataMode.REPEATED);
          break;
        case "float":
          mapBuilder.add(fieldName, MinorType.FLOAT4, DataMode.REPEATED);
          break;
        case "long":
          mapBuilder.add(fieldName, MinorType.BIGINT, DataMode.REPEATED);
          break;
        case "boolean":
          mapBuilder.add(fieldName, MinorType.BIT, DataMode.REPEATED);
          break;
        case "java.lang.String":
          mapBuilder.add(fieldName, MinorType.VARCHAR, DataMode.REPEATED);
          break;
        default:
          logger.warn("Drill cannot process data type {} in compound fields.", dataType);
          break;
      }
    }
    TupleMetadata finalInnerSchema = mapBuilder.resumeSchema().buildSchema();

    index = rowWriter.tupleSchema().index(COMPOUND_DATA_FIELD_NAME);
    if (index == -1) {
      index = rowWriter.addColumn(finalInnerSchema.column(COMPOUND_DATA_FIELD_NAME));
    }
    TupleWriter listWriter = rowWriter.column(index).tuple();

    for (CompoundDataMember dataMember : data) {
      String dataType = dataMember.getDataType().getJavaType().getName();
      String fieldName = dataMember.getName();
      int[] dataLength = reader.getDatasetByPath(path).getDimensions();
      Object rawData = ((LinkedHashMap<String, ?>)reader.getDatasetByPath(path).getData()).get(fieldName);

      ArrayWriter innerWriter = listWriter.array(fieldName);
      for (int i = 0; i < dataLength[0]; i++) {
        switch (dataType) {
          case "byte":
            innerWriter.scalar().setInt(((byte[])rawData)[i]);
            break;
          case "short":
            innerWriter.scalar().setInt(((short[])rawData)[i]);
            break;
          case "int":
            innerWriter.scalar().setInt(((int[])rawData)[i]);
            break;
          case "double":
            innerWriter.scalar().setDouble(((double[])rawData)[i]);
            break;
          case "float":
            innerWriter.scalar().setFloat(((float[])rawData)[i]);
            break;
          case "long":
            innerWriter.scalar().setLong(((long[])rawData)[i]);
            break;
          case "boolean":
            innerWriter.scalar().setBoolean(((boolean[])rawData)[i]);
            break;
          case "java.lang.String":
            if ((((String[])rawData)[i]) != null) {
              innerWriter.scalar().setString(((String[]) rawData)[i]);
            } else {
              innerWriter.scalar().setNull();
            }
            break;
          default:
            logger.warn("Drill cannot process data type {} in compound fields.", dataType);
            break;
        }
      }
    }
  }

  @Override
  public void close() {
    AutoCloseables.closeSilently(hdfFile);
    /*
     * The current implementation of the HDF5 reader creates a temp file which needs to be removed
     * when the batch reader is closed.  A possible future functionality might be to use the
     */
    boolean result = hdfFile.getFile().delete();
    if (!result) {
      logger.warn("Failed to delete HDF5 temp file {}", hdfFile.getFile().getName());
    }
    hdfFile = null;

    AutoCloseables.closeSilently(reader);
    reader = null;
  }
}
