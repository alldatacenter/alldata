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
package org.apache.drill.exec.physical.resultSet.model.single;

import org.apache.drill.exec.physical.resultSet.model.ReaderIndex;
import org.apache.drill.exec.record.VectorContainer;

/**
 * Reader index that points directly to each row in the row set.
 * This index starts with pointing to the -1st row, so that the
 * reader can require a <tt>next()</tt> for every row, including
 * the first. (This is the JDBC <tt>RecordSet</tt> convention.)
 */

public class DirectRowIndex extends ReaderIndex {

  public DirectRowIndex(VectorContainer container) {
    super(container::getRecordCount);
  }

  @Override
  public int offset() { return position; }

  @Override
  public int hyperVectorIndex() { return 0; }
}
