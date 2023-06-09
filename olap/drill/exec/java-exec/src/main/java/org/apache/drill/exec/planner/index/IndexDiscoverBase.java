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

import org.apache.drill.exec.physical.base.AbstractDbGroupScan;
import org.apache.drill.exec.planner.common.DrillScanRelBase;
import org.apache.drill.exec.planner.logical.DrillTable;
import org.apache.drill.exec.planner.physical.ScanPrel;
import org.apache.calcite.rel.RelNode;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * IndexDiscoverBase is the layer to read index configurations of tables on storage plugins,
 * then based on the properties it collected, get the StoragePlugin from StoragePluginRegistry,
 * together with indexes information, build an IndexCollection
 */
public abstract class IndexDiscoverBase implements IndexDiscover {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(IndexDiscoverBase.class);

  private AbstractDbGroupScan scan; // group scan corresponding to the primary table
  private RelNode scanRel;   // physical scan rel corresponding to the primary table

  public IndexDiscoverBase(AbstractDbGroupScan inScan, DrillScanRelBase inScanPrel) {
    scan = inScan;
    scanRel = inScanPrel;
  }

  public IndexDiscoverBase(AbstractDbGroupScan inScan, ScanPrel inScanPrel) {
    scan = inScan;
    scanRel = inScanPrel;
  }

  public AbstractDbGroupScan getOriginalScan() {
    return scan;
  }

  public RelNode getOriginalScanRel() {
    return scanRel;
  }

  public IndexCollection getTableIndex(String tableName, String storageName, Collection<DrillIndexDefinition>  indexDefs ) {
    Set<DrillIndexDescriptor> idxSet = new HashSet<>();
    for (DrillIndexDefinition def : indexDefs) {
      DrillIndexDescriptor indexDescriptor = new DrillIndexDescriptor(def);
      materializeIndex(storageName, indexDescriptor);
    }
    return new DrillIndexCollection(getOriginalScanRel(), idxSet);
  }

  public void materializeIndex(String storageName, DrillIndexDescriptor index) {
    index.setStorageName(storageName);
    index.setDrillTable(buildDrillTable(index));
  }

  /**
   * When there is storageName in IndexDescriptor, get a DrillTable instance based on the
   * StorageName and other informaiton in idxDesc that helps identifying the table.
   * @param idxDesc
   * @return
   */
  public DrillTable getExternalDrillTable(IndexDescriptor idxDesc) {
    //XX: get table object for this index, index storage plugin should provide interface to get the DrillTable object
    return null;
  }

  /**
   * Abstract function getDrillTable will be implemented the IndexDiscover within storage plugin(e.g. HBase, MaprDB)
   * since the implementations of AbstractStoragePlugin, IndexDescriptor and DrillTable in that storage plugin may have
   * the implement details.
   * @param idxDesc

   * @return
   */
  public DrillTable buildDrillTable(IndexDescriptor idxDesc) {
    if(idxDesc.getIndexType() == IndexDescriptor.IndexType.EXTERNAL_SECONDARY_INDEX) {
      return getExternalDrillTable(idxDesc);
    }
    else {
      return getNativeDrillTable(idxDesc);
    }
  }

  /**
   * When it is native index(index provided by native storage plugin),
   * the actual IndexDiscover should provide the implementation to get the DrillTable object of index,
   * Otherwise, we call IndexDiscoverable interface exposed from external storage plugin's SchemaFactory
   * to get the desired DrillTable.
   * @param idxDesc
   * @return
   */
  public abstract DrillTable getNativeDrillTable(IndexDescriptor idxDesc);

}
