/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.manager.service.interfaces;

import java.util.List;

import org.apache.inlong.tubemq.manager.controller.TubeMQResult;
import org.apache.inlong.tubemq.manager.entry.RegionEntry;

public interface RegionService {

    /**
     * create new region with brokers
     *
     * @param regionEntry
     * @param brokerIdList
     * @return
     */
    TubeMQResult createNewRegion(RegionEntry regionEntry,
                                 List<Long> brokerIdList);

    /**
     * delete region and set the brokers in the region to be default region
     *
     * @param regionId
     * @param clusterId
     * @return
     */
    TubeMQResult deleteRegion(long regionId, long clusterId);

    /**
     * update region to contain new brokers
     * need to check if other region contains the same brokers
     *
     * @param newRegionEntry
     * @param brokerIdList
     * @param clusterId
     * @return
     */
    TubeMQResult updateRegion(RegionEntry newRegionEntry, List<Long> brokerIdList, long clusterId);

    /**
     * query region inside a cluster
     * if no regionId is passed return all regions inside the cluster
     *
     * @param regionId
     * @param clusterId
     * @return
     */
    List<RegionEntry> queryRegion(Long regionId, Long clusterId);
}
