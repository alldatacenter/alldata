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
package org.apache.drill.exec.planner.index;


import org.apache.calcite.rex.RexNode;
import org.apache.drill.exec.physical.base.IndexGroupScan;
import org.apache.calcite.rel.RelNode;

import java.util.Set;

public class DrillIndexCollection extends AbstractIndexCollection {
  private final RelNode scan;  // physical scan rel corresponding to the primary table

  public DrillIndexCollection(RelNode scanRel,
                               Set<DrillIndexDescriptor> indexes) {
    this.scan = scanRel;
    for (IndexDescriptor index : indexes) {
      super.addIndex(index);
    }
  }

  private IndexDescriptor getIndexDescriptor() {

    //XXX need a policy to pick the indexDesc to use instead of picking the first one.
    return this.indexes.iterator().next();
  }

  @Override
  public boolean supportsIndexSelection() {
    return true;
  }

  @Override
  public boolean supportsRowCountStats() {
    return true;
  }

  @Override
  public boolean supportsFullTextSearch() {
    return true;
  }

  @Override
  public double getRows(RexNode indexCondition) {

    return getIndexDescriptor().getRows(scan, indexCondition);
  }

  @Override
  public IndexGroupScan getGroupScan() {
    return getIndexDescriptor().getIndexGroupScan();
  }

  @Override
  public IndexCollectionType getIndexCollectionType() {
    return IndexCollection.IndexCollectionType.EXTERNAL_SECONDARY_INDEX_COLLECTION;
  }

}
