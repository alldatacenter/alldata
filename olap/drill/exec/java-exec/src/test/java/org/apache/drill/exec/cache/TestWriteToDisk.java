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
package org.apache.drill.exec.cache;

import java.io.File;

import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSets;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.test.SubOperatorTest;
import org.apache.drill.exec.ExecTest;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.WritableBatch;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.Test;

public class TestWriteToDisk extends SubOperatorTest {

  @Test
  public void test() throws Exception {
    VectorContainer container = expectedRowSet().container();

    WritableBatch batch = WritableBatch.getBatchNoHVWrap(container.getRecordCount(), container, false);

    VectorAccessibleSerializable wrap = new VectorAccessibleSerializable(batch, fixture.allocator());

    VectorAccessibleSerializable newWrap = new VectorAccessibleSerializable(fixture.allocator());
    try (FileSystem fs = ExecTest.getLocalFileSystem()) {
      File tempDir = dirTestWatcher.getTmpDir();
      tempDir.deleteOnExit();
      Path path = new Path(tempDir.getAbsolutePath(), "drillSerializable");
      try (FSDataOutputStream out = fs.create(path)) {
        wrap.writeToStream(out);
      }

      try (FSDataInputStream in = fs.open(path)) {
        newWrap.readFromStream(in);
      }
    }

    RowSetUtilities.verify(expectedRowSet(), RowSets.wrap(newWrap.get()));
  }

  private RowSet expectedRowSet() {
    TupleMetadata schema = new SchemaBuilder()
        .add("int", TypeProtos.MinorType.INT)
        .add("binary", TypeProtos.MinorType.VARBINARY)
        .build();

    return fixture.rowSetBuilder(schema)
        .addRow(0, "ZERO".getBytes())
        .addRow(1, "ONE".getBytes())
        .addRow(2, "TWO".getBytes())
        .addRow(3, "THREE".getBytes())
        .build();
  }
}
