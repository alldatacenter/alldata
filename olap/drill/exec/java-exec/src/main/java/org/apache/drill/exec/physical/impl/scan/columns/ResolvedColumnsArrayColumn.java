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
package org.apache.drill.exec.physical.impl.scan.columns;

import org.apache.drill.exec.physical.impl.scan.project.ResolvedTableColumn;
import org.apache.drill.exec.physical.impl.scan.project.VectorSource;
import org.apache.drill.exec.physical.resultSet.project.RequestedColumn;
import org.apache.drill.exec.record.MaterializedField;

public class ResolvedColumnsArrayColumn extends ResolvedTableColumn {

  private final RequestedColumn inCol;

  public ResolvedColumnsArrayColumn(UnresolvedColumnsArrayColumn unresolved,
      MaterializedField schema,
      VectorSource source, int sourceIndex) {
    super(unresolved.name(), schema, source, sourceIndex);
    inCol = unresolved.element();
  }

  public boolean[] selectedIndexes() { return inCol.indexes(); }
}
