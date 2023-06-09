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
package org.apache.drill.exec.record.metadata.schema;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.drill.exec.util.ImpersonationUtil;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;

/**
 * Is used to provide schema using given schema file name and path.
 */
public class PathSchemaProvider implements SchemaProvider {

  /**
   * Reader used to read JSON schema from file into into {@link SchemaContainer}.
   * Allows comment inside the JSON file.
   */
  public static final ObjectReader READER;

  /**
   * Writer used to write content from {@link SchemaContainer} into JSON file.
   */
  public static final ObjectWriter WRITER;

  static {
    ObjectMapper mapper = new ObjectMapper().enable(INDENT_OUTPUT).configure(JsonParser.Feature.ALLOW_COMMENTS, true);

    READER = mapper.readerFor(SchemaContainer.class);

    DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
    prettyPrinter = prettyPrinter.withArrayIndenter(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
    WRITER = mapper.writer(prettyPrinter);
  }

  private final Path path;
  private final FileSystem fs;

  public PathSchemaProvider(Path path) throws IOException {
    this(createFsFromPath(path), path);
  }

  public PathSchemaProvider(FileSystem fs, Path path) throws IOException {
    this.fs = ImpersonationUtil.createFileSystem(ImpersonationUtil.getProcessUserName(), fs.getConf());

    if (!fs.exists(path.getParent())) {
      throw new IOException(String.format("Parent path for schema file [%s] does not exist", path.toUri().getPath()));
    }

    this.path = path;
  }

  private static FileSystem createFsFromPath(Path path) throws IOException {
    return path.getFileSystem(new Configuration());
  }

  @Override
  public void delete() throws IOException {
    try {
      if (!fs.delete(path, false)) {
        throw new IOException(String.format("Error while deleting schema file [%s]", path.toUri().getPath()));
      }
    } catch (IOException e1) {
      // re-check file existence to cover concurrent deletion case
      try {
        if (exists()) {
          throw e1;
        }
      } catch (IOException e2) {
        // ignore new exception and throw original one
        throw e1;
      }
    }
  }

  @Override
  public void store(String schema, Map<String, String> properties, StorageProperties storageProperties) throws IOException {
    SchemaContainer tableSchema = createTableSchema(schema, properties);

    try (OutputStream stream = fs.create(path, storageProperties.isOverwrite())) {
      WRITER.writeValue(stream, tableSchema);
    }
    storageProperties.getStorageStrategy().applyToFile(fs, path);
  }

  @Override
  public SchemaContainer read() throws IOException {
  try (InputStream stream = fs.open(path)) {
      return READER.readValue(stream);
    }
  }

  @Override
  public boolean exists() throws IOException {
    return fs.exists(path);
  }

  protected SchemaContainer createTableSchema(String schema, Map<String, String> properties) throws IOException {
    return new SchemaContainer(null, schema, properties);
  }

}

