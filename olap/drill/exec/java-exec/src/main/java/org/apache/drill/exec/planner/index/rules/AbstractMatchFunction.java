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
package org.apache.drill.exec.planner.index.rules;

import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.DbGroupScan;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.planner.logical.DrillScanRel;

public abstract class AbstractMatchFunction<T> implements MatchFunction<T> {
  public boolean checkScan(DrillScanRel scanRel) {
    GroupScan groupScan = scanRel.getGroupScan();
    if (groupScan instanceof DbGroupScan) {
      DbGroupScan dbscan = ((DbGroupScan) groupScan);
      // if we already applied index convert rule, and this scan is indexScan or restricted scan already,
      // no more trying index convert rule
      return dbscan.supportsSecondaryIndex() && (!dbscan.isIndexScan()) && (!dbscan.isRestrictedScan());
    }
    return false;
  }

  public boolean checkScan(GroupScan groupScan) {
    if (groupScan instanceof DbGroupScan) {
      DbGroupScan dbscan = ((DbGroupScan) groupScan);
      // if we already applied index convert rule, and this scan is indexScan or restricted scan already,
      // no more trying index convert rule
      return dbscan.supportsSecondaryIndex() &&
             !dbscan.isRestrictedScan() &&
              (!dbscan.isFilterPushedDown() || dbscan.isIndexScan()) &&
             !containsStar(dbscan);
    }
    return false;
  }

  public static boolean containsStar(DbGroupScan dbscan) {
    for (SchemaPath column : dbscan.getColumns()) {
      if (column.getRootSegment().getPath().startsWith("*")) {
        return true;
      }
    }
    return false;
  }
}