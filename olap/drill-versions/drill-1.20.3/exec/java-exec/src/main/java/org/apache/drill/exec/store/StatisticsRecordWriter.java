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
package org.apache.drill.exec.store;

import org.apache.drill.exec.record.VectorAccessible;

import java.io.IOException;
import java.util.Map;

public interface StatisticsRecordWriter extends StatisticsRecordCollector {

  /**
   * Initialize the writer.
   *
   * @param writerOptions Contains key, value pair of settings.
   * @throws IOException
   */
  void init(Map<String, String> writerOptions);

  /**
   * Update the schema in RecordWriter. Called at least once before starting writing the records.
   * @param batch
   * @throws IOException
   */
  void updateSchema(VectorAccessible batch);

  /**
   * Check if the writer should start a new partition, and if so, start a new partition
   */
  public void checkForNewPartition(int index);

  /**
   * Returns if the writer is a blocking writer i.e. consumes all input before writing it out
   * @return TRUE, if writer is blocking. FALSE, otherwise
   */
  boolean isBlockingWriter();

  /**
   * For a blocking writer, called after processing all the records to flush out the writes
   * @throws IOException
   */
  void flushBlockingWriter() throws IOException;
  void abort();
  void cleanup();
}
