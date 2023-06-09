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
package org.apache.drill.exec.store.esri;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryCursor;
import com.esri.core.geometry.ShapefileReader;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.ogc.OGCGeometry;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.physical.impl.scan.file.FileScanFramework.FileSchemaNegotiator;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedReader;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.exec.vector.accessor.TupleWriter;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.FileSplit;
import org.jamel.dbf.DbfReader;
import org.jamel.dbf.structure.DbfField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShpBatchReader implements ManagedReader<FileSchemaNegotiator> {

  private static final Logger logger = LoggerFactory.getLogger(ShpBatchReader.class);
  private static final String GID_FIELD_NAME = "gid";
  private static final String SRID_FIELD_NAME = "srid";
  private static final String SHAPE_TYPE_FIELD_NAME = "shapeType";
  private static final String GEOM_FIELD_NAME = "geom";
  private static final String SRID_PATTERN_TEXT = "AUTHORITY\\[\"\\w+\"\\s*,\\s*\"*(\\d+)\"*\\]\\]$";

  private FileSplit split;
  private ResultSetLoader loader;
  private Path hadoopShp;
  private Path hadoopDbf;
  private Path hadoopPrj;
  private InputStream fileReaderShp = null;
  private InputStream fileReaderDbf = null;
  private InputStream fileReaderPrj = null;
  private GeometryCursor geomCursor = null;
  private DbfReader dbfReader = null;
  private ScalarWriter gidWriter;
  private ScalarWriter sridWriter;
  private ScalarWriter shapeTypeWriter;
  private ScalarWriter geomWriter;
  private RowSetLoader rowWriter;
  private int srid;
  private SpatialReference spatialReference;
  private final int maxRecords;

  public ShpBatchReader(int maxRecords) {
    this.maxRecords = maxRecords;
  }

  @Override
  public boolean open(FileSchemaNegotiator negotiator) {
    split = negotiator.split();
    hadoopShp = split.getPath();

    String filePath = split.getPath().toString();
    hadoopDbf = new Path(filePath.replace(".shp", ".dbf"));
    hadoopPrj = new Path(filePath.replace(".shp", ".prj"));

    openFile(negotiator);
    SchemaBuilder builder = new SchemaBuilder()
      .addNullable(GID_FIELD_NAME, TypeProtos.MinorType.INT)
      .addNullable(SRID_FIELD_NAME, TypeProtos.MinorType.INT)
      .addNullable(SHAPE_TYPE_FIELD_NAME, TypeProtos.MinorType.VARCHAR)
      .addNullable(GEOM_FIELD_NAME, TypeProtos.MinorType.VARBINARY);

    negotiator.tableSchema(builder.buildSchema(), false);
    loader = negotiator.build();

    rowWriter = loader.writer();
    gidWriter = rowWriter.scalar(GID_FIELD_NAME);
    sridWriter = rowWriter.scalar(SRID_FIELD_NAME);
    shapeTypeWriter = rowWriter.scalar(SHAPE_TYPE_FIELD_NAME);
    geomWriter = rowWriter.scalar(GEOM_FIELD_NAME);

    return true;
  }

  @Override
  public boolean next() {
    Geometry geom = null;

    while (!rowWriter.isFull()) {
      Object[] dbfRow = dbfReader.nextRecord();
      geom = geomCursor.next();
      if (geom == null) {
        return false;
      }
      processShapefileSet(rowWriter, geomCursor.getGeometryID(), geom, dbfRow);
    }
    return true;
  }

  private void openFile(FileSchemaNegotiator negotiator) {
    try {
      fileReaderShp = negotiator.fileSystem().openPossiblyCompressedStream(split.getPath());
      byte[] shpBuf = new byte[fileReaderShp.available()];
      fileReaderShp.read(shpBuf);

      ByteBuffer byteBuffer = ByteBuffer.wrap(shpBuf);
      byteBuffer.position(byteBuffer.position() + 100);

      ShapefileReader shpReader = new ShapefileReader();
      geomCursor = shpReader.getGeometryCursor(byteBuffer);

      fileReaderDbf = negotiator.fileSystem().openPossiblyCompressedStream(hadoopDbf);
      dbfReader = new DbfReader(fileReaderDbf);

      fileReaderPrj = negotiator.fileSystem().openPossiblyCompressedStream(hadoopPrj);
      byte[] prjBuf = new byte[fileReaderPrj.available()];
      fileReaderPrj.read(prjBuf);
      fileReaderPrj.close();

      String wktReference = new String(prjBuf);

      Pattern pattern = Pattern.compile(SRID_PATTERN_TEXT);
      Matcher matcher = pattern.matcher(wktReference);
      if (matcher.find()) {
        this.srid = Integer.parseInt(matcher.group(1));
        this.spatialReference = SpatialReference.create(srid);
      }

      logger.debug("Processing Shape File: {}", hadoopShp);
    } catch (IOException e) {
      throw UserException
        .dataReadError(e)
        .message("Failed to open open input file: %s", split.getPath())
        .addContext("User name", negotiator.userName())
        .build(logger);
    }
  }

  private void processShapefileSet(RowSetLoader rowWriter, final int gid, final Geometry geom, final Object[] dbfRow) {
    if (rowWriter.limitReached(maxRecords)) {
      return;
    }

    rowWriter.start();

    gidWriter.setInt(gid);
    sridWriter.setInt(srid);
    shapeTypeWriter.setString(geom.getType().toString());
    final ByteBuffer buf = OGCGeometry.createFromEsriGeometry(geom, spatialReference).asBinary();
    final byte[] bytes = buf.array();
    geomWriter.setBytes(bytes, bytes.length);
    writeDbfRow(dbfRow, rowWriter);

    rowWriter.save();
  }

  private void writeDbfRow(final Object[] dbfRow, RowSetLoader rowWriter) {
    int dbfFieldCount = dbfReader.getHeader().getFieldsCount();

    for (int i = 0; i < dbfFieldCount; i++) {
      DbfField field = dbfReader.getHeader().getField(i);

      if (dbfRow[i] == null) {
        continue;
      }

      switch (field.getDataType()) {
        case CHAR:
          byte[] strType = (byte[]) dbfRow[i];
          String stringValue = new String( strType, Charset.forName("utf-8")).trim();
          writeStringColumn(rowWriter, field.getName(), stringValue);
          break;
        case FLOAT:
          final double floatVal = ((Float) dbfRow[i]).doubleValue();
          writeDoubleColumn(rowWriter, field.getName(), floatVal);
          break;
        case DATE:
          final long dataVal = ((java.util.Date) dbfRow[i]).getTime();
          writeTimeColumn(rowWriter, field.getName(), dataVal);
          break;
        case LOGICAL:
          int logicalVal = (Boolean) dbfRow[i] ? 1 : 0;
          writeBooleanColumn(rowWriter, field.getName(), logicalVal);
          break;
        case NUMERIC:
          double numericVal = ((Number) dbfRow[i]).doubleValue();
          if (field.getDecimalCount() == 0) {
            writeIntColumn(rowWriter, field.getName(), (int) numericVal);
          } else {
            writeDoubleColumn(rowWriter, field.getName(), numericVal);
          }
          break;
        default:
          break;
      }
    }
  }

  private void writeStringColumn(TupleWriter rowWriter, String name, String value) {
    int index = rowWriter.tupleSchema().index(name);
    if (index == -1) {
      ColumnMetadata colSchema = MetadataUtils.newScalar(name, TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL);
      index = rowWriter.addColumn(colSchema);
    }
    ScalarWriter colWriter = rowWriter.scalar(index);
    colWriter.setString(value);
  }

  private void writeDoubleColumn(TupleWriter rowWriter, String name, double value) {
    int index = rowWriter.tupleSchema().index(name);
    if (index == -1) {
      ColumnMetadata colSchema = MetadataUtils.newScalar(name, TypeProtos.MinorType.FLOAT8, TypeProtos.DataMode.OPTIONAL);
      index = rowWriter.addColumn(colSchema);
    }
    ScalarWriter colWriter = rowWriter.scalar(index);
    colWriter.setDouble(value);
  }

  private void writeBooleanColumn(TupleWriter rowWriter, String name, int value) {
    int index = rowWriter.tupleSchema().index(name);
    if (index == -1) {
      ColumnMetadata colSchema = MetadataUtils.newScalar(name, TypeProtos.MinorType.INT, TypeProtos.DataMode.OPTIONAL);
      index = rowWriter.addColumn(colSchema);
    }
    boolean boolean_value = true;
    if (value == 0) {
      boolean_value = false;
    }

    ScalarWriter colWriter = rowWriter.scalar(index);
    colWriter.setBoolean(boolean_value);
  }

  private void writeIntColumn(TupleWriter rowWriter, String name, int value) {
    int index = rowWriter.tupleSchema().index(name);
    if (index == -1) {
      ColumnMetadata colSchema = MetadataUtils.newScalar(name, TypeProtos.MinorType.INT, TypeProtos.DataMode.OPTIONAL);
      index = rowWriter.addColumn(colSchema);
    }
    ScalarWriter colWriter = rowWriter.scalar(index);
    colWriter.setInt(value);
  }

  private void writeTimeColumn(TupleWriter rowWriter, String name, long value) {
    int index = rowWriter.tupleSchema().index(name);
    Instant instant = Instant.ofEpochMilli(value);
    if (index == -1) {
      ColumnMetadata colSchema = MetadataUtils.newScalar(name, TypeProtos.MinorType.INT, TypeProtos.DataMode.OPTIONAL);
      index = rowWriter.addColumn(colSchema);
    }
    ScalarWriter colWriter = rowWriter.scalar(index);
    colWriter.setTimestamp(instant);
  }

  @Override
  public void close() {
    closeStream(fileReaderShp, "ESRI Shapefile");
    closeStream(fileReaderDbf, "DBF Shapefile");
    closeStream(fileReaderPrj, "PRJ Shapefile");

    try {
      if (dbfReader != null) {
        dbfReader.close();
      }
    } catch (Exception e) {
      logger.warn("Error when closing DBF Reader: {}", e.getMessage());
    }
    dbfReader = null;
  }


  /**
   * Closes and nullifies given input stream.
   * In case of {@link IOException}, logs it as warning.
   *
   * @param inputStream input stream
   * @param name stream name
   */
  private void closeStream(InputStream inputStream, String name) {
    if (inputStream == null) {
      return;
    }
    try {
      inputStream.close();
    } catch (IOException e) {
      logger.warn("Error when closing {}: {}", name, e.getMessage());
    }
    inputStream = null;
  }
}
