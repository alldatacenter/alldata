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
package org.apache.drill.exec.physical.impl.scan.v3;

import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;

/**
 * Base class for the "mock" readers used in this test. The mock readers
 * follow the normal (enhanced) reader API, but instead of actually reading
 * from a data source, they just generate data with a known schema.
 * They also expose internal state such as identifying which methods
 * were actually called.
 */
public abstract class BaseMockBatchReader implements ManagedReader {
  public boolean closeCalled;
  public int startIndex;
  public int batchCount;
  public int batchLimit;
  protected ResultSetLoader tableLoader;

  protected void makeBatch() {
    RowSetLoader writer = tableLoader.writer();
    int offset = (batchCount - 1) * 20 + startIndex;
    writeRow(writer, offset + 10, "fred");
    writeRow(writer, offset + 20, "wilma");
  }

  protected void writeRow(RowSetLoader writer, int col1, String col2) {
    writer.start();
    if (writer.column(0) != null) {
      writer.scalar(0).setInt(col1);
    }
    if (writer.column(1) != null) {
      writer.scalar(1).setString(col2);
    }
    writer.save();
  }

  @Override
  public void close() {
    closeCalled = true;
  }
}
