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
package org.apache.drill.metastore.iceberg.write;

import org.apache.drill.metastore.iceberg.exceptions.IcebergMetastoreException;
import org.apache.hadoop.fs.Path;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.Table;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.data.parquet.GenericParquetWriter;
import org.apache.iceberg.exceptions.RuntimeIOException;
import org.apache.iceberg.io.FileAppender;
import org.apache.iceberg.io.OutputFile;
import org.apache.iceberg.parquet.Parquet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Parquet File Writer implementation. Creates Parquet file in given location and name
 * and '.parquet' extension and writes given data into it.
 * Note: if file already exists, write operation will fail.
 */
public class ParquetFileWriter implements FileWriter {

  private final Table table;
  private final List<Record> records = new ArrayList<>();
  private String location;
  private String name;

  public ParquetFileWriter(Table table) {
    this.table = table;
  }

  @Override
  public FileWriter records(List<Record> records) {
    this.records.addAll(records);
    return this;
  }

  @Override
  public FileWriter location(String location) {
    this.location = location;
    return this;
  }

  @Override
  public FileWriter name(String name) {
    this.name = name;
    return this;
  }

  @Override
  public File write() {
    Objects.requireNonNull(location, "File create location must be specified");
    Objects.requireNonNull(name, "File name must be specified");

    OutputFile outputFile = table.io().newOutputFile(new Path(location, FileFormat.PARQUET.addExtension(name)).toUri().getPath());

    FileAppender<Record> fileAppender = null;
    try {
      fileAppender = Parquet.write(outputFile)
        .forTable(table)
        .createWriterFunc(GenericParquetWriter::buildWriter)
        .build();
      fileAppender.addAll(records);
      fileAppender.close();
      // metrics are available only when file was written (i.e. close method was executed)
      return new File(outputFile, fileAppender.metrics());
    } catch (IOException | ClassCastException | RuntimeIOException e) {
      if (fileAppender != null) {
        try {
          fileAppender.close();
        } catch (Exception ex) {
          // write has failed anyway, ignore closing exception if any and throw initial one
        }
      }
      throw new IcebergMetastoreException(String.format("Unable to write data into parquet file [%s]", outputFile.location()), e);
    }
  }
}
