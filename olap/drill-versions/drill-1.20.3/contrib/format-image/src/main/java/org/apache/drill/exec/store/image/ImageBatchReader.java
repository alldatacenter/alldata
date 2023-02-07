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
package org.apache.drill.exec.store.image;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;

import org.apache.drill.common.AutoCloseables;
import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.physical.impl.scan.file.FileScanFramework.FileSchemaNegotiator;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedReader;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.exec.store.dfs.easy.EasySubScan;
import org.apache.drill.exec.vector.accessor.ArrayWriter;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.exec.vector.accessor.TupleWriter;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.slf4j.LoggerFactory;

import com.drew.imaging.FileType;
import com.drew.imaging.FileTypeDetector;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.eps.EpsDirectory;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.xmp.XmpDirectory;

public class ImageBatchReader implements ManagedReader<FileSchemaNegotiator> {

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ImageBatchReader.class);

  private final ImageFormatConfig config;
  private final EasySubScan scan;
  private CustomErrorContext errorContext;
  private Path path;
  private FileStatus fileStatus;
  private BufferedInputStream metaInputStream;
  private RowSetLoader loader;
  private LinkedHashMap<String, ColumnDefn> genericColumns;
  private Metadata metadata;

  public ImageBatchReader(final ImageFormatConfig config, final EasySubScan scan) {
    this.config = config;
    this.scan = scan;
  }

  @Override
  public boolean open(FileSchemaNegotiator negotiator) {
    try {
      errorContext = negotiator.parentErrorContext();
      DrillFileSystem dfs = negotiator.fileSystem();
      path = dfs.makeQualified(negotiator.split().getPath());
      fileStatus = dfs.getFileStatus(path);
      metaInputStream = new BufferedInputStream(dfs.openPossiblyCompressedStream(path));
      logger.debug("The config is {}, root is {}, columns has {}", config, scan.getSelectionRoot(), scan.getColumns());
    } catch (IOException e) {
      throw UserException
              .dataReadError(e)
              .message("Failure in initial image inputstream. " + e.getMessage())
              .addContext(errorContext)
              .build(logger);
    }
    // define the schema
    negotiator.tableSchema(defineMetadata(), false);
    ResultSetLoader resultSetLoader = negotiator.build();
    loader = resultSetLoader.writer();
    // bind the writer for generic columns
    bindColumns(loader);
    return true;
  }

  @Override
  public boolean next() {
    try {
      loader.start();
      // process generic metadata
      processGenericMetadata();
      // process external metadata
      processExtenalMetadata();
      loader.save();
    } catch (IOException e) {
      throw UserException
              .dataReadError(e)
              .message("Failed to estimates the file type. " + e.getMessage())
              .addContext(errorContext)
              .build(logger);
    } catch (ImageProcessingException e) {
      throw UserException
              .dataReadError(e)
              .message("Error in reading metadata from inputstream. " + e.getMessage())
              .addContext(errorContext)
              .build(logger);
    } catch (Exception e) {
      throw UserException
              .dataReadError(e)
              .message("Error in processing metadata directory. " + e.getMessage())
              .addContext(errorContext)
              .build(logger);
    }
    return false;
  }

  @Override
  public void close() {
    AutoCloseables.closeSilently(metaInputStream);
  }

  private TupleMetadata defineMetadata() {
    SchemaBuilder builder = new SchemaBuilder();
    genericColumns = new LinkedHashMap<>();
    Collection<String> tags = GenericMetadataDirectory._tagNameMap.values();
    for (String tagName : tags) {
      if (!config.hasFileSystemMetadata() && ImageMetadataUtils.isSkipTag(tagName)) {
        continue;
      }
      ColumnDefn columnDefn = new GenericColumnDefn(tagName);
      if (config.isDescriptive()) {
        columnDefn.defineText(builder);
      } else {
        columnDefn.define(builder);
      }
      genericColumns.put(ImageMetadataUtils.formatName(tagName), columnDefn);
    }
    return builder.buildSchema();
  }

  private void bindColumns(RowSetLoader loader) {
    for (ColumnDefn columnDefn : genericColumns.values()) {
      columnDefn.bind(loader);
    }
  }

  private void processGenericMetadata() throws IOException, ImageProcessingException {
    FileType fileType = FileTypeDetector.detectFileType(metaInputStream);
    metadata = ImageMetadataReader.readMetadata(metaInputStream);
    // Read for generic metadata at first
    new GenericMetadataReader().read(fileType, fileStatus, metadata);
    GenericMetadataDirectory genericMetadata = metadata.getFirstDirectoryOfType(GenericMetadataDirectory.class);
    // Process the `Generic Metadata Directory`
    ImageDirectoryProcessor.processGenericMetadataDirectory(genericMetadata, genericColumns, config);
  }

  private void processExtenalMetadata() {
    boolean skipEPSPreview = false;
    for (Directory directory : metadata.getDirectories()) {
      // Skip the `Generic Metadata Directory`
      String dictName = ImageMetadataUtils.formatName(directory.getName());
      if (directory instanceof GenericMetadataDirectory) {
        continue;
      }
      if (directory instanceof ExifIFD0Directory && skipEPSPreview) {
        skipEPSPreview = false;
        continue;
      }
      if (directory instanceof EpsDirectory) {
        // If an EPS file contains a TIFF preview, skip the next IFD0
        skipEPSPreview = directory.containsTag(EpsDirectory.TAG_TIFF_PREVIEW_SIZE);
      }
      // Process the `External Metadata Directory`
      MapColumnDefn columnDefn = new MapColumnDefn(dictName).builder(loader);
      ImageDirectoryProcessor.processDirectory(columnDefn, directory, metadata, config);
      // Continue to process XmpDirectory if exists
      if (directory instanceof XmpDirectory) {
        ImageDirectoryProcessor.processXmpDirectory(columnDefn, (XmpDirectory) directory);
      }
    }
  }

  /**
   * The class mainly process schema definition, index binding,
   * and set up the vector (Column Writers) values.
   * Because the new vector needs to specify schema
   * depends on data type, must override some methods in derived classes.
   */
  protected abstract static class ColumnDefn {

    private final String name;
    private final String originName; // not format
    private ScalarWriter writer;

    public ColumnDefn(String name) {
      this.originName = name;
      this.name = ImageMetadataUtils.formatName(name);
    }

    public String getName() {
      return name;
    }

    public String getOriginName() {
      return originName;
    }

    public ScalarWriter getWriter() {
      return writer;
    }

    public void bind(RowSetLoader loader) {
      writer = loader.scalar(name);
    }

    public void defineText(SchemaBuilder builder) {
      builder.add(getName(), Types.optional(MinorType.VARCHAR));
    }

    public abstract void define(SchemaBuilder builder);

    public abstract void load(Object value);

    public ScalarWriter addText(String name) {
      throw new UnsupportedOperationException();
    }

    public ArrayWriter addList(String name) {
      throw new UnsupportedOperationException();
    }

    public ArrayWriter addListMap(String name) {
      throw new UnsupportedOperationException();
    }

    public TupleWriter addMap(String name) {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Responsible for process of the image GenericMetadataDirectory
   * metadata and create data type based on different tags.
   * @see org.apache.drill.exec.store.image.GenericMetadataDirectory
   */
  protected static class GenericColumnDefn extends ColumnDefn {

    public GenericColumnDefn(String name) {
      super(name);
    }

    @Override
    public void define(SchemaBuilder builder) {
      if (ImageMetadataUtils.isVarchar(getOriginName())) {
        builder.add(getName(), Types.optional(MinorType.VARCHAR));
      } else if (ImageMetadataUtils.isInt(getOriginName())) {
        builder.add(getName(), Types.optional(MinorType.INT));
      } else if (ImageMetadataUtils.isLong(getOriginName())) {
        builder.add(getName(), Types.optional(MinorType.BIGINT));
      } else if (ImageMetadataUtils.isDouble(getOriginName())) {
        builder.add(getName(), Types.optional(MinorType.FLOAT8));
      } else if (ImageMetadataUtils.isBoolean(getOriginName())) {
        builder.add(getName(), Types.optional(MinorType.BIT));
      } else if (ImageMetadataUtils.isDate(getOriginName())) {
        builder.add(getName(), Types.optional(MinorType.TIMESTAMP));
      }
    }

    @Override
    public void load(Object value) {
      getWriter().setObject(value);
    }
  }

  /**
   * Responsible for process of the map writer (nested structure).
   * Not only work with scalar, but also provide an entry point
   * for create the nested structures, such as List or List-Map in a Map.
   */
  protected static class MapColumnDefn extends ColumnDefn {

    private int index;
    private TupleWriter writer;

    public MapColumnDefn(String name) {
      super(name);
    }

    @Override
    public void bind(RowSetLoader loader) {
      index = loader.tupleSchema().index(getName());
      if (index == -1) {
        index = loader.addColumn(SchemaBuilder.columnSchema(getName(), MinorType.MAP, DataMode.REQUIRED));
      }
      writer = loader.tuple(index);
    }

    @Override
    public void define(SchemaBuilder builder) { }

    @Override
    public void load(Object value) { }

    public MapColumnDefn builder(RowSetLoader loader) {
      bind(loader);
      return this;
    }

    public MapColumnDefn builder(TupleWriter writer) {
      this.writer = writer;
      return this;
    }

    /**
     * example : { a : 1 } > { a : 1, b : "2" }
     */
    @Override
    public ScalarWriter addText(String name) {
      index = writer.tupleSchema().index(name);
      if (index == -1) {
        index = writer.addColumn(SchemaBuilder.columnSchema(name, MinorType.VARCHAR, DataMode.OPTIONAL));
      } else { // rewrite the value
        writer.column(name).events().restartRow();
      }
      return writer.scalar(index);
    }

    /**
     * example : { a : 1 } > { a : 1, [ b : "2" ] }
     */
    @Override
    public ArrayWriter addList(String name) {
      index = writer.tupleSchema().index(name);
      if (index == -1) {
        index = writer.addColumn(SchemaBuilder.columnSchema(name, MinorType.VARCHAR, DataMode.REPEATED));
      }
      return writer.array(index);
    }

    /**
     * example : { a : 1 } > { a : 1, [ { b : 2 } ] }
     */
    @Override
    public ArrayWriter addListMap(String name) {
      index = writer.tupleSchema().index(name);
      if (index == -1) {
        index = writer.addColumn(SchemaBuilder.columnSchema(name, MinorType.MAP, DataMode.REPEATED));
      }
      return writer.array(index);
    }

    /**
     * example : { a : 1 } > { a : 1, { b : 2 } }
     */
    @Override
    public TupleWriter addMap(String name) {
      index = writer.tupleSchema().index(name);
      if (index == -1) {
        index = writer.addColumn(SchemaBuilder.columnSchema(name, MinorType.MAP, DataMode.REQUIRED));
        return writer.tuple(index);
      }
      return writer.tuple(name);
    }

    /**
     * example : { a : 1 } > { a : 1, [ 0, -1, 0, -1 ] }
     */
    public ArrayWriter addListByte(String name) {
      index = writer.tupleSchema().index(name);
      if (index == -1) {
        index = writer.addColumn(SchemaBuilder.columnSchema(name, MinorType.TINYINT, DataMode.REPEATED));
      }
      return writer.array(index);
    }

    /**
     * example : { a : 1 } > { a : 1, b : 2.0 }
     */
    public ScalarWriter addDouble(String name) {
      index = writer.tupleSchema().index(name);
      if (index == -1) {
        index = writer.addColumn(SchemaBuilder.columnSchema(name, MinorType.FLOAT8, DataMode.OPTIONAL));
      }
      return writer.scalar(index);
    }

    /**
     * example : { a : 1 } > { a : 1, b : date() }
     */
    public ScalarWriter addDate(String name) {
      index = writer.tupleSchema().index(name);
      if (index == -1) {
        index = writer.addColumn(SchemaBuilder.columnSchema(name, MinorType.TIMESTAMP, DataMode.OPTIONAL));
      }
      return writer.scalar(index);
    }

    /**
     * example : { a : 1 } > { a : 1, b : object() }
     */
    public ScalarWriter addObject(String name, MinorType type) {
      index = writer.tupleSchema().index(name);
      if (index == -1) {
        index = writer.addColumn(SchemaBuilder.columnSchema(name, type, DataMode.OPTIONAL));
      }
      return writer.scalar(index);
    }

    /**
     * example : { a : 1 } > { a : 1, b : 2 }
     */
    public ScalarWriter addIntToMap(TupleWriter writer, String name) {
      int index = writer.tupleSchema().index(name);
      if (index == -1) {
        index = writer.addColumn(SchemaBuilder.columnSchema(name, MinorType.INT, DataMode.OPTIONAL));
      }
      return writer.scalar(index);
    }
  }

  /**
   * Responsible for process of the list-map with array writer.
   */
  protected static class ListColumnDefn extends ColumnDefn {

    private ArrayWriter writer;

    public ListColumnDefn(String name) {
      super(name);
    }

    @Override
    public void define(SchemaBuilder builder) { }

    @Override
    public void load(Object value) { }

    public ListColumnDefn builder(ArrayWriter writer) {
      this.writer = writer;
      return this;
    }

    /**
     * example : [ ] > [ { a : "1" } ]
     */
    @Override
    public ScalarWriter addText(String name) {
      TupleWriter map = writer.tuple();
      int index = map.tupleSchema().index(name);
      if (index == -1) {
        index = map.addColumn(SchemaBuilder.columnSchema(name, MinorType.VARCHAR, DataMode.OPTIONAL));
      }
      return map.scalar(index);
    }
  }
}