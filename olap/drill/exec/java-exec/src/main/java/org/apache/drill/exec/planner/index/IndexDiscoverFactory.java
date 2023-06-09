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

import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.planner.common.DrillScanRelBase;
import org.apache.drill.exec.planner.physical.ScanPrel;
import org.apache.calcite.rel.RelNode;

import java.lang.reflect.Constructor;

/**
 * With this factory, we allow user to load a different indexDiscover class to obtain index information
 */
public class IndexDiscoverFactory {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(IndexDiscoverFactory.class);
  static final String INDEX_DISCOVER_CLASS_KEY = "index.discoverClass";
  static final String INDEX_DISCOVER_CONFIG_KEY = "index.meta";

  public static IndexDiscover getIndexDiscover(StoragePluginConfig config,
                                               GroupScan inScan, RelNode scanRel,
                                               Class<? extends IndexDiscover> targetIndexDiscoverClass) {
    Class discoverClass = targetIndexDiscoverClass;

    try {
      if (config != null ) {
        String discoverClassName = config.getValue(INDEX_DISCOVER_CLASS_KEY);
        if(discoverClassName!= null && discoverClassName != "") {
          discoverClass = Class.forName(discoverClassName);
        }
      }
    } catch(Exception e) {
      logger.error("Could not find configured IndexDiscover class {}", e);
    }
    Constructor<? extends IndexDiscoverBase> constructor;
    try {
      constructor = getConstructor(discoverClass,
          scanRel);
      IndexDiscoverBase idxDiscover = constructor.newInstance(inScan, scanRel);
      if((targetIndexDiscoverClass != null) && (discoverClass != targetIndexDiscoverClass)) {

        //idxDiscover.setOriginalDiscover(targetIndexDiscoverClass);
      }
      return idxDiscover;
    } catch(Exception e) {
      logger.error("Could not construct {}", discoverClass.getName(), e);
    }
    return null;
  }

  private static Constructor<? extends IndexDiscoverBase> getConstructor(Class discoverClass,RelNode scanRel) throws Exception {
    if (scanRel instanceof DrillScanRelBase) {
      return discoverClass.getConstructor(GroupScan.class, DrillScanRelBase.class);
    } else {
      return discoverClass.getConstructor(GroupScan.class, ScanPrel.class);
    }
  }

}
