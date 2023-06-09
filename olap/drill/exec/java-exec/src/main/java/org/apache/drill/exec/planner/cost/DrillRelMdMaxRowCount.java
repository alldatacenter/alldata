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
package org.apache.drill.exec.planner.cost;

import org.apache.calcite.rel.core.Aggregate;
import org.apache.calcite.rel.metadata.ReflectiveRelMetadataProvider;
import org.apache.calcite.rel.metadata.RelMdMaxRowCount;
import org.apache.calcite.rel.metadata.RelMetadataProvider;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.util.BuiltInMethod;

public class DrillRelMdMaxRowCount extends RelMdMaxRowCount {

  private static final DrillRelMdMaxRowCount INSTANCE = new DrillRelMdMaxRowCount();

  public static final RelMetadataProvider SOURCE =
      ReflectiveRelMetadataProvider.reflectiveSource(BuiltInMethod.MAX_ROW_COUNT.method, INSTANCE);

  // The method is overriden because of changes done in CALCITE-2991 and
  // TODO: should be discarded when CALCITE-1048 is fixed.
  @Override
  public Double getMaxRowCount(Aggregate rel, RelMetadataQuery mq) {
    if (rel.getGroupSet().isEmpty()) {
      // Aggregate with no GROUP BY always returns 1 row (even on empty table).
      return 1D;
    }
    final Double rowCount = mq.getMaxRowCount(rel.getInput());
    if (rowCount == null) {
      return null;
    }
    return rowCount * rel.getGroupSets().size();
  }
}
