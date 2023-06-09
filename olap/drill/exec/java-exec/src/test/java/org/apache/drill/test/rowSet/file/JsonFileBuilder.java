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
package org.apache.drill.test.rowSet.file;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.drill.exec.physical.resultSet.util.JsonWriter;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetReader;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

/**
 * <h4>Overview</h4>
 * <p>
 *   Builds a JSON file containing the data in a {@link RowSet}.
 * </p>
 * <h4>Example</h4>
 * <p>
 *   You can find an example of how to use {@link JsonFileBuilder} at
 *   {@link org.apache.drill.test.ExampleTest#secondTest()}.
 * </p>
 */
public class JsonFileBuilder {

  private final RowSet rowSet;
  private boolean pretty = true;
  private boolean useExtendedOutput;

  /**
   * Creates a {@link JsonFileBuilder} that will write the given {@link RowSet} to a file.
   *
   * @param rowSet The {@link RowSet} to be written to a file.
   */
  public JsonFileBuilder(RowSet rowSet) {
    this.rowSet = Preconditions.checkNotNull(rowSet);
    Preconditions.checkArgument(rowSet.rowCount() > 0, "The given rowset is empty.");
  }

  public JsonFileBuilder prettyPrint(boolean flag) {
    this.pretty = flag;
    return this;
  }

  public JsonFileBuilder extended(boolean flag) {
    this.useExtendedOutput = flag;
    return this;
  }

  /**
   * Writes the configured data to the given file in json format.
   * @param tableFile The file to write the json data to.
   * @throws IOException
   */
  public void build(File tableFile) throws IOException {
    tableFile.getParentFile().mkdirs();

    try (BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(tableFile))) {
      JsonWriter jsonWriter = new JsonWriter(os, pretty, useExtendedOutput);
      final RowSetReader reader = rowSet.reader();
      while (reader.next()) {
        jsonWriter.writeRow(reader);
      }
    }
  }
}
