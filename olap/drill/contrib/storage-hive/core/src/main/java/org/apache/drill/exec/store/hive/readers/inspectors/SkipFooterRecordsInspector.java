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
package org.apache.drill.exec.store.hive.readers.inspectors;

import org.apache.hadoop.mapred.RecordReader;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * To implement skip footer logic this records inspector will buffer N number of incoming read records in queue
 * and make sure they are skipped when input is fully processed. FIFO method of queuing is used for these purposes.
 */
public class SkipFooterRecordsInspector extends AbstractRecordsInspector {

  private final int footerCount;
  private Queue<Object> footerBuffer;
  private final List<Object> valueHolders;
  private long readRecordsCount;

  public SkipFooterRecordsInspector(RecordReader<Object, Object> reader, int footerCount) {
    this.footerCount = footerCount;
    this.footerBuffer = new LinkedList<>();
    this.valueHolders = initializeValueHolders(reader, footerCount);
  }

  /**
   * Returns next available value holder where value should be written from the cached value holders.
   * Current available holder is determined by getting mod for actually read records.
   *
   * @return value holder
   */
  @Override
  public Object getValueHolder() {
    int availableHolderIndex = (int) readRecordsCount % valueHolders.size();
    return valueHolders.get(availableHolderIndex);
  }

  /**
   * Buffers current value holder with written value
   * and returns last buffered value if number of buffered values exceeds N records to skip.
   *
   * @return next available value holder with written value, null otherwise
   */
  @Override
  public Object getNextValue() {
    footerBuffer.add(getValueHolder());
    readRecordsCount++;
    if (footerBuffer.size() <= footerCount) {
      return null;
    }
    return footerBuffer.poll();
  }

  /**
   * Creates buffer of value holders, so these holders can be re-used.
   * Holders quantity depends on number of lines to skip in the end of the file plus one.
   *
   * @param reader record reader
   * @param footerCount number of lines to skip at the end of the file
   * @return list of value holders
   */
  private List<Object> initializeValueHolders(RecordReader<Object, Object> reader, int footerCount) {
    List<Object> valueHolder = new ArrayList<>(footerCount + 1);
    for (int i = 0; i <= footerCount; i++) {
      valueHolder.add(reader.createValue());
    }
    return valueHolder;
  }

}
