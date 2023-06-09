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
package org.apache.drill.exec.vector;

import java.util.Iterator;

/**
 * Allows caller to provide input in a bulk manner while abstracting the underlying data structure
 * to provide performance optimizations opportunities.
 */
public interface VarLenBulkInput<T extends VarLenBulkEntry> extends Iterator<T> {
  /**
   * @return start index of this bulk input (relative to this VL container)
   */
  int getStartIndex();

  /**
   * Indicates we're done processing (processor might stop processing when memory buffers
   * are depleted); this allows caller to re-submit any unprocessed data.
   *
   */
  void done();

  /**
   * Enables caller (such as wrapper vector objects) to include more processing logic as the data is being
   * streamed.
   */
  public interface BulkInputCallback<T extends VarLenBulkEntry> {
    /**
     * Invoked when a new bulk entry is read (entry is immutable)
     * @param entry bulk entry
     */
    void onNewBulkEntry(final T entry);

    /**
     * Indicates the bulk input is done
     */
    void onEndBulkInput();
  }
}
