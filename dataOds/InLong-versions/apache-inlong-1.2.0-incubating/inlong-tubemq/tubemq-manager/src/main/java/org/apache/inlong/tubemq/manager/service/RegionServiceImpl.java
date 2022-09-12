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

package org.apache.inlong.tubemq.manager.service;

import java.util.List;
import javax.transaction.Transactional;

import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.tubemq.manager.controller.TubeMQResult;
import org.apache.inlong.tubemq.manager.entry.RegionEntry;
import org.apache.inlong.tubemq.manager.repository.BrokerRepository;
import org.apache.inlong.tubemq.manager.repository.MasterRepository;
import org.apache.inlong.tubemq.manager.repository.RegionRepository;
import org.apache.inlong.tubemq.manager.service.interfaces.BrokerService;
import org.apache.inlong.tubemq.manager.service.interfaces.RegionService;
import org.apache.inlong.tubemq.manager.utils.ValidateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RegionServiceImpl implements RegionService {

    public static final String DUPLICATE_ENTRY = "duplicate entry";
    @Autowired
    RegionRepository regionRepository;

    @Autowired
    BrokerRepository brokerRepository;

    @Autowired
    MasterRepository masterRepository;

    @Autowired
    BrokerService brokerService;

    @Override
    public TubeMQResult createNewRegion(RegionEntry regionEntry,
                                        List<Long> brokerIdList) {
        try {
            Long clusterId = regionEntry.getClusterId();
            if (!brokerService.checkIfBrokersAllExsit(brokerIdList, clusterId)) {
                return TubeMQResult.errorResult(TubeMQErrorConst.RESOURCE_NOT_EXIST);
            }
            if (existBrokerIdAlreadyInRegion(clusterId, brokerIdList, null)) {
                return TubeMQResult.errorResult(TubeMQErrorConst.BROKER_IN_OTHER_REGION);
            }
            regionRepository.save(regionEntry);
            brokerService.updateBrokersRegion(brokerIdList, regionEntry.getRegionId(), clusterId);
        } catch (DataIntegrityViolationException e) {
            log.error("duplicate entry, newRegionDO:{}.", regionEntry, e);
            return TubeMQResult.errorResult(DUPLICATE_ENTRY);
        } catch (Exception e) {
            log.error("create region failed, newRegionDO:{}.", regionEntry, e);
            return TubeMQResult.errorResult(TubeMQErrorConst.MYSQL_ERROR);
        }
        return TubeMQResult.successResult();
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public TubeMQResult deleteRegion(long regionId, long clusterId) {
        try {
            regionRepository.deleteRegion(regionId, clusterId);
            brokerService.resetBrokerRegions(regionId, clusterId);
        } catch (Exception e) {
            log.error("delete region failed, regionId:{}.", regionId, e);
            throw new RuntimeException(TubeMQErrorConst.MYSQL_ERROR);
        }
        return TubeMQResult.successResult();
    }

    private RegionEntry getRegionEntry(long clusterId, long regionId) {
        List<RegionEntry> regionEntries = regionRepository
                .findRegionEntriesByClusterIdEqualsAndRegionIdEquals(clusterId, regionId);
        if (regionEntries.isEmpty()) {
            return null;
        }
        if (regionEntries.size() > 1) {
            throw new RuntimeException(DUPLICATE_ENTRY);
        }
        return regionEntries.get(0);
    }

    @Override
    public TubeMQResult updateRegion(RegionEntry newRegionEntry, List<Long> brokerIdList, long clusterId) {
        if (ValidateUtils.isNull(newRegionEntry) || ValidateUtils.isNull(newRegionEntry.getRegionId())) {
            return TubeMQResult.errorResult(TubeMQErrorConst.PARAM_ILLEGAL);
        }
        try {
            RegionEntry oldRegionDO = getRegionEntry(clusterId, newRegionEntry.getRegionId());
            if (ValidateUtils.isNull(oldRegionDO)) {
                return TubeMQResult.errorResult(TubeMQErrorConst.RESOURCE_NOT_EXIST);
            }
            // set id for update operation
            newRegionEntry.setId(oldRegionDO.getId());
            List<Long> oldBrokerList = brokerService.getBrokerIdListInRegion(oldRegionDO.getRegionId(), clusterId);
            if (oldBrokerList.equals(brokerIdList)) {
                // no change in broker list update directly
                regionRepository.save(newRegionEntry);
                return TubeMQResult.successResult();
            }
            if (existBrokerIdAlreadyInRegion(newRegionEntry.getClusterId(), brokerIdList,
                    newRegionEntry.getRegionId())) {
                return TubeMQResult.errorResult(TubeMQErrorConst.BROKER_IN_OTHER_REGION);
            }
            handleUpdateRepo(newRegionEntry, brokerIdList, clusterId);
        } catch (Exception e) {
            log.error("update region failed, newRegionDO:{}", newRegionEntry, e);
            return TubeMQResult.errorResult(TubeMQErrorConst.MYSQL_ERROR);
        }
        return TubeMQResult.successResult();
    }

    @Transactional(rollbackOn = Exception.class)
    public void handleUpdateRepo(RegionEntry newRegionEntry, List<Long> brokerIdList, long clusterId) {
        regionRepository.save(newRegionEntry);
        // reset brokers to default region
        brokerService.resetBrokerRegions(newRegionEntry.getRegionId(), clusterId);
        // update brokers to new region
        brokerService.updateBrokersRegion(brokerIdList, newRegionEntry.getRegionId(),
                newRegionEntry.getClusterId());
    }

    @Override
    public List<RegionEntry> queryRegion(Long regionId, Long clusterId) {
        if (ValidateUtils.isNull(regionId)) {
            return regionRepository.findRegionEntriesByClusterIdEquals(clusterId);
        }
        return regionRepository
                .findRegionEntriesByClusterIdEqualsAndRegionIdEquals(clusterId, regionId);
    }

    private boolean existBrokerIdAlreadyInRegion(Long clusterId, List<Long> newBrokerIdList, Long regionId) {
        if (ValidateUtils.isNull(clusterId) || ValidateUtils.isEmptyList(newBrokerIdList)) {
            return true;
        }
        List<RegionEntry> regionEntries = regionRepository.findRegionEntriesByClusterIdEquals(clusterId);
        if (ValidateUtils.isEmptyList(regionEntries)) {
            return false;
        }
        for (RegionEntry regionEntry : regionEntries) {
            if (regionEntry.getRegionId().equals(regionId)) {
                continue;
            }
            List<Long> regionBrokerIdList = brokerService.getBrokerIdListInRegion(regionEntry.getRegionId(), clusterId);
            if (ValidateUtils.isEmptyList(regionBrokerIdList)) {
                continue;
            }
            if (regionBrokerIdList.stream().anyMatch(newBrokerIdList::contains)) {
                return true;
            }
        }
        return false;
    }

}
