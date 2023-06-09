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
package org.apache.drill.exec.store.easy.text.writer;

import com.univocity.parsers.csv.CsvFormat;
import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.store.EventBasedRecordWriter.FieldConverter;
import org.apache.drill.exec.store.StorageStrategy;
import org.apache.drill.exec.store.StringOutputRecordWriter;
import org.apache.drill.exec.store.easy.text.TextFormatPlugin;
import org.apache.drill.exec.vector.complex.reader.FieldReader;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class TextRecordWriter extends StringOutputRecordWriter {

  private static final Logger logger = LoggerFactory.getLogger(TextRecordWriter.class);

  private final StorageStrategy storageStrategy;
  private final Configuration fsConf;

  private FileSystem fs;
  private Path cleanUpLocation;
  private String location;
  private String prefix;
  private String extension;
  // indicates number of a file created by this writer: 0_0_{fileNumberIndex}.csv (ex: 0_0_0.csv)
  private int fileNumberIndex;

  private CsvWriterSettings writerSettings;
  private CsvWriter writer;

  // record write status: true once the startRecord() is called until endRecord() is called
  private boolean fRecordStarted = false;

  public TextRecordWriter(BufferAllocator allocator, StorageStrategy storageStrategy, Configuration fsConf) {
    super(allocator);
    this.storageStrategy = storageStrategy == null ? StorageStrategy.DEFAULT : storageStrategy;
    this.fsConf = new Configuration(fsConf);
  }

  @Override
  public void init(Map<String, String> writerOptions) throws IOException {
    this.location = writerOptions.get("location");
    this.prefix = writerOptions.get("prefix");

    this.fs = FileSystem.get(fsConf);
    String extension = writerOptions.get("extension");
    this.extension = extension == null ? "" : "." + extension;
    this.fileNumberIndex = 0;

    CsvWriterSettings writerSettings = new CsvWriterSettings();
    writerSettings.setMaxColumns(TextFormatPlugin.MAXIMUM_NUMBER_COLUMNS);
    writerSettings.setMaxCharsPerColumn(TextFormatPlugin.MAX_CHARS_PER_COLUMN);
    writerSettings.setHeaderWritingEnabled(Boolean.parseBoolean(writerOptions.get("addHeader")));
    writerSettings.setQuoteAllFields(Boolean.parseBoolean(writerOptions.get("forceQuotes")));
    CsvFormat format = writerSettings.getFormat();
    format.setLineSeparator(writerOptions.get("lineSeparator"));
    format.setDelimiter(writerOptions.get("fieldDelimiter"));
    format.setQuote(writerOptions.get("quote").charAt(0));
    format.setQuoteEscape(writerOptions.get("escape").charAt(0));
    format.setCharToEscapeQuoteEscaping(TextFormatPlugin.NULL_CHAR); // do not escape "escape" char

    this.writerSettings = writerSettings;

    logger.trace("Text writer settings: {}", this.writerSettings);
  }

  @Override
  public void startNewSchema(BatchSchema schema) throws IOException {
    // wrap up the current file
    cleanup();

    // open a new file for writing data with new schema
    Path fileName = new Path(location, String.format("%s_%s%s", prefix, fileNumberIndex, extension));
    try {
      // Drill text writer does not support partitions, so only one file can be created
      // and thus only one location should be deleted in case of abort
      // to ensure that our writer was the first to create output file,
      // we create empty output file first and fail if file exists
      cleanUpLocation = storageStrategy.createFileAndApply(fs, fileName);

      // since empty output file will be overwritten (some file systems may restrict append option)
      // we need to re-apply file permission
      DataOutputStream fos = fs.create(fileName);
      storageStrategy.applyToFile(fs, fileName);
      logger.debug("Created file: {}.", fileName);

      // increment file number index
      fileNumberIndex++;

      this.writer = new CsvWriter(fos, writerSettings);
    } catch (IOException e) {
      throw new IOException(String.format("Unable to create file: %s.", fileName), e);
    }

    if (writerSettings.isHeaderWritingEnabled()) {
      writer.writeHeaders(StreamSupport.stream(schema.spliterator(), false)
        .map(MaterializedField::getName)
        .collect(Collectors.toList()));
    }
  }

  @Override
  public void addField(int fieldId, String value) {
    writer.addValue(value);
  }

  @Override
  public void startRecord() throws IOException {
    if (fRecordStarted) {
      throw new IOException("Previous record is not written completely");
    }
    fRecordStarted = true;
  }

  @Override
  public void endRecord() throws IOException {
    if (!fRecordStarted) {
      throw new IOException("No record is in writing");
    }

    writer.writeValuesToRow();
    fRecordStarted = false;
  }

  @Override
  public FieldConverter getNewMapConverter(int fieldId, String fieldName, FieldReader reader) {
    return new ComplexStringFieldConverter(fieldId, fieldName, reader);
  }

  @Override
  public FieldConverter getNewRepeatedMapConverter(int fieldId, String fieldName, FieldReader reader) {
    return new ComplexStringFieldConverter(fieldId, fieldName, reader);
  }

  @Override
  public FieldConverter getNewRepeatedListConverter(int fieldId, String fieldName, FieldReader reader) {
    return new ComplexStringFieldConverter(fieldId, fieldName, reader);
  }

  public class ComplexStringFieldConverter extends FieldConverter {

    public ComplexStringFieldConverter(int fieldId, String fieldName, FieldReader reader) {
      super(fieldId, fieldName, reader);
    }

    @Override
    public void writeField() throws IOException {
      addField(fieldId, reader.readObject().toString());
    }
  }

  @Override
  public void cleanup() throws IOException {
    fRecordStarted = false;
    if (writer != null) {
      try {
        writer.close();
        writer = null;
        logger.debug("Closed text writer for file: {}.", cleanUpLocation);
      } catch (IllegalStateException e) {
        throw new IOException(String.format("Unable to close text writer for file %s: %s",
          cleanUpLocation, e.getMessage()), e);
      }
    }
  }

  @Override
  public void abort() throws IOException {
    if (cleanUpLocation != null) {
      fs.delete(cleanUpLocation, true);
      logger.info("Aborting writer. Location [{}] on file system [{}] is deleted.",
          cleanUpLocation.toUri().getPath(), fs.getUri());
    }
  }
}
