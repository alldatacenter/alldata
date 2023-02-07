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
package org.apache.drill.exec.store.ltsv;

import io.netty.buffer.DrillBuf;
import org.apache.drill.common.AutoCloseables;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.impl.OutputMutator;
import org.apache.drill.exec.store.AbstractRecordReader;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.exec.vector.complex.impl.VectorContainerWriter;
import org.apache.drill.exec.vector.complex.writer.BaseWriter;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class LTSVRecordReader extends AbstractRecordReader {

  private static final Logger logger = LoggerFactory.getLogger(LTSVRecordReader.class);

  private static final int MAX_RECORDS_PER_BATCH = 8096;

  private final String inputPath;

  private final InputStream fsStream;

  private final BufferedReader reader;

  private DrillBuf buffer;

  private VectorContainerWriter writer;

  public LTSVRecordReader(FragmentContext fragmentContext, Path path, DrillFileSystem fileSystem,
                          List<SchemaPath> columns) throws OutOfMemoryException {
    this.inputPath = path.toUri().getPath();
    try {
      this.fsStream = fileSystem.openPossiblyCompressedStream(path);
      this.reader = new BufferedReader(new InputStreamReader(fsStream, StandardCharsets.UTF_8));
      this.buffer = fragmentContext.getManagedBuffer();
      setColumns(columns);
    } catch (IOException e) {
      throw UserException.dataReadError(e)
        .message(String.format("Failed to open input file: %s", inputPath))
        .build(logger);
    }
  }

  @Override
  protected Collection<SchemaPath> transformColumns(Collection<SchemaPath> projected) {
    Set<SchemaPath> transformed = new LinkedHashSet<>();
    if (!isStarQuery()) {
      transformed.addAll(projected);
    } else {
      transformed.add(SchemaPath.STAR_COLUMN);
    }
    return transformed;
  }

  public void setup(final OperatorContext context, final OutputMutator output) {
    this.writer = new VectorContainerWriter(output);
  }

  public int next() {
    this.writer.allocate();
    this.writer.reset();

    int recordCount = 0;

    try {
      BaseWriter.MapWriter map = this.writer.rootAsMap();
      String line;

      while (recordCount < MAX_RECORDS_PER_BATCH && (line = this.reader.readLine()) != null) {
        // Skip empty lines
        if (line.trim().length() == 0) {
          continue;
        }

        List<String[]> fields = new ArrayList<>();
        for (String field : line.split("\t")) {
          int index = field.indexOf(":");
          if (index <= 0) {
            throw new ParseException(String.format("Invalid LTSV format: %s\n%d:%s", inputPath, recordCount + 1, line), 0);
          }

          String fieldName = field.substring(0, index);
          String fieldValue = field.substring(index + 1);
          if (selectedColumn(fieldName)) {
            fields.add(new String[]{fieldName, fieldValue});
          }
        }

        if (fields.size() == 0) {
          continue;
        }

        this.writer.setPosition(recordCount);
        map.start();

        for (String[] field : fields) {
          byte[] bytes = field[1].getBytes(StandardCharsets.UTF_8);
          this.buffer = this.buffer.reallocIfNeeded(bytes.length);
          this.buffer.setBytes(0, bytes, 0, bytes.length);
          map.varChar(field[0]).writeVarChar(0, bytes.length, buffer);
        }

        map.end();
        recordCount++;
      }

      this.writer.setValueCount(recordCount);
      return recordCount;

    } catch (final Exception e) {
      String msg = String.format("Failure while reading messages from %s. Record reader was at record: %d", inputPath, recordCount + 1);
      throw UserException.dataReadError(e)
        .message(msg)
        .build(logger);
    }
  }

  private boolean selectedColumn(String fieldName) {
    for (SchemaPath col : getColumns()) {
      if (col.equals(SchemaPath.STAR_COLUMN) || col.getRootSegment().getPath().equals(fieldName)) {
        return true;
      }
    }
    return false;
  }

  public void close() throws Exception {
    AutoCloseables.close(reader, fsStream);
  }
}
