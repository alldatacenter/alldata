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
package org.apache.drill.exec.store.easy.json;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import org.apache.drill.exec.store.StorageStrategy;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.store.EventBasedRecordWriter;
import org.apache.drill.exec.store.EventBasedRecordWriter.FieldConverter;
import org.apache.drill.exec.store.JSONOutputRecordWriter;
import org.apache.drill.exec.store.RecordWriter;
import org.apache.drill.exec.vector.complex.fn.BasicJsonOutput;
import org.apache.drill.exec.vector.complex.fn.ExtendedJsonOutput;
import org.apache.drill.exec.vector.complex.fn.JsonWriter;
import org.apache.drill.exec.vector.complex.reader.FieldReader;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

public class JsonRecordWriter extends JSONOutputRecordWriter implements RecordWriter {

  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(JsonRecordWriter.class);
  private static final String LINE_FEED = String.format("%n");

  private Path cleanUpLocation;
  private String location;
  private String prefix;

  private String extension;
  private boolean useExtendedOutput;

  private int index;
  private FileSystem fs = null;
  private OutputStream stream = null;

  private final JsonFactory factory = new JsonFactory();
  private final StorageStrategy storageStrategy;

  private final Configuration fsConf;

  public JsonRecordWriter(StorageStrategy storageStrategy, Configuration fsConf) {
    this.storageStrategy = storageStrategy == null ? StorageStrategy.DEFAULT : storageStrategy;
    this.fsConf = new Configuration(fsConf);
  }

  @Override
  public void init(Map<String, String> writerOptions) throws IOException {
    this.location = writerOptions.get("location");
    this.prefix = writerOptions.get("prefix");
    this.extension = writerOptions.get("extension");
    this.useExtendedOutput = Boolean.parseBoolean(writerOptions.get("extended"));
    this.skipNullFields = Boolean.parseBoolean(writerOptions.get("skipnulls"));
    final boolean uglify = Boolean.parseBoolean(writerOptions.get("uglify"));

    this.fs = FileSystem.get(fsConf);

    Path fileName = new Path(location, prefix + "_" + index + "." + extension);
    try {
      // json writer does not support partitions, so only one file can be created
      // and thus only one location should be deleted in case of abort
      // to ensure that our writer was the first to create output file,
      // we create empty output file first and fail if file exists
      cleanUpLocation = storageStrategy.createFileAndApply(fs, fileName);

      // since empty output file will be overwritten (some file systems may restrict append option)
      // we need to re-apply file permission
      stream = fs.create(fileName);
      storageStrategy.applyToFile(fs, fileName);

      JsonGenerator generator = factory.createGenerator(stream).useDefaultPrettyPrinter()
          .configure(JsonGenerator.Feature.QUOTE_NON_NUMERIC_NUMBERS,
              !Boolean.parseBoolean(writerOptions.get("enableNanInf")));
      if (uglify) {
        generator = generator.setPrettyPrinter(new MinimalPrettyPrinter(LINE_FEED));
      }
      if(useExtendedOutput){
        gen = new ExtendedJsonOutput(generator);
      }else{
        gen = new BasicJsonOutput(generator);
      }
      logger.debug("Created file: {}", fileName);
    } catch (IOException ex) {
      logger.error("Unable to create file: " + fileName, ex);
      throw ex;
    }
  }

  @Override
  public void updateSchema(VectorAccessible batch) throws IOException {
    // no op
  }

  @Override
  public FieldConverter getNewMapConverter(int fieldId, String fieldName, FieldReader reader) {
    return new MapJsonConverter(fieldId, fieldName, reader);
  }

  public class MapJsonConverter extends FieldConverter {
    List<FieldConverter> converters = Lists.newArrayList();

    public MapJsonConverter(int fieldId, String fieldName, FieldReader reader) {
      super(fieldId, fieldName, reader);
      int i = 0;
      for (String name : reader) {
        FieldConverter converter = EventBasedRecordWriter.getConverter(JsonRecordWriter.this, i++, name, reader.reader(name));
        converters.add(converter);
      }
    }

    @Override
    public void startField() throws IOException {
      gen.writeFieldName(fieldName);
    }

    @Override
    public void writeField() throws IOException {
      gen.writeStartObject();
      for (FieldConverter converter : converters) {
        converter.startField();
        converter.writeField();
      }
      gen.writeEndObject();
    }
  }

  @Override
  public FieldConverter getNewUnionConverter(int fieldId, String fieldName, FieldReader reader) {
    return new UnionJsonConverter(fieldId, fieldName, reader);
  }

  public class UnionJsonConverter extends FieldConverter {

    public UnionJsonConverter(int fieldId, String fieldName, FieldReader reader) {
      super(fieldId, fieldName, reader);
    }

    @Override
    public void startField() throws IOException {
      gen.writeFieldName(fieldName);
    }

    @Override
    public void writeField() throws IOException {
      JsonWriter writer = new JsonWriter(gen);
      writer.write(reader);
    }
  }

  @Override
  public FieldConverter getNewRepeatedMapConverter(int fieldId, String fieldName, FieldReader reader) {
    return new RepeatedMapJsonConverter(fieldId, fieldName, reader);
  }

  public class RepeatedMapJsonConverter extends FieldConverter {
    List<FieldConverter> converters = Lists.newArrayList();

    public RepeatedMapJsonConverter(int fieldId, String fieldName, FieldReader reader) {
      super(fieldId, fieldName, reader);
      int i = 0;
      for (String name : reader) {
        FieldConverter converter = EventBasedRecordWriter.getConverter(JsonRecordWriter.this, i++, name, reader.reader(name));
        converters.add(converter);
      }
    }

    @Override
    public void startField() throws IOException {
      gen.writeFieldName(fieldName);
    }

    @Override
    public void writeField() throws IOException {
      gen.writeStartArray();
      while (reader.next()) {
        gen.writeStartObject();
        for (FieldConverter converter : converters) {
          converter.startField();
          converter.writeField();
        }
        gen.writeEndObject();
      }
      gen.writeEndArray();
    }
  }

  @Override
  public FieldConverter getNewRepeatedListConverter(int fieldId, String fieldName, FieldReader reader) {
    return new RepeatedListJsonConverter(fieldId, fieldName, reader);
  }

  public class RepeatedListJsonConverter extends FieldConverter {
    FieldConverter converter;

    public RepeatedListJsonConverter(int fieldId, String fieldName, FieldReader reader) {
      super(fieldId, fieldName, reader);
      converter = EventBasedRecordWriter.getConverter(JsonRecordWriter.this, fieldId, fieldName, reader.reader());
    }

    @Override
    public void startField() throws IOException {
      gen.writeFieldName(fieldName);
    }

    @Override
    public void writeField() throws IOException {
      gen.writeStartArray();
      while (reader.next()) {
        converter.writeField();
      }
      gen.writeEndArray();
    }
  }

  @Override
  public void startRecord() throws IOException {
    gen.writeStartObject();
  }

  @Override
  public void endRecord() throws IOException {
    gen.writeEndObject();
  }

  @Override
  public void abort() throws IOException {
    if (cleanUpLocation != null) {
      fs.delete(cleanUpLocation, true);
      logger.info("Aborting writer. Location [{}] on file system [{}] is deleted.",
          cleanUpLocation.toUri().getPath(), fs.getUri());
    }
  }

  @Override
  public void cleanup() throws IOException {
    gen.flush();
    stream.close();
  }
}
