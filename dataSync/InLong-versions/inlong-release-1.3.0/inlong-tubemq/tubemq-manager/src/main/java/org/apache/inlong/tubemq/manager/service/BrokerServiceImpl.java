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

import static org.apache.inlong.tubemq.manager.service.TubeConst.DEFAULT_REGION;
import static org.apache.inlong.tubemq.manager.service.TubeConst.SCHEMA;
import static org.apache.inlong.tubemq.manager.service.TubeConst.TUBE_REQUEST_PATH;

import com.google.gson.Gson;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.inlong.tubemq.manager.controller.TubeMQResult;
import org.apache.inlong.tubemq.manager.controller.group.request.DeleteOffsetReq;
import org.apache.inlong.tubemq.manager.controller.group.request.QueryOffsetReq;
import org.apache.inlong.tubemq.manager.controller.group.result.OffsetQueryRes;
import org.apache.inlong.tubemq.manager.controller.node.request.CloneOffsetReq;
import org.apache.inlong.tubemq.manager.entry.BrokerEntry;
import org.apache.inlong.tubemq.manager.repository.BrokerRepository;
import org.apache.inlong.tubemq.manager.service.interfaces.BrokerService;
import org.apache.inlong.tubemq.manager.service.interfaces.MasterService;
import org.apache.inlong.tubemq.manager.utils.ConvertUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BrokerServiceImpl implements BrokerService {

    @Autowired
    BrokerRepository brokerRepository;

    @Autowired
    MasterService masterService;

    private final Gson gson = new Gson();

    @Override
    public void resetBrokerRegions(long regionId, long clusterId) {
        List<BrokerEntry> brokerEntries = brokerRepository
                .findBrokerEntriesByRegionIdEqualsAndClusterIdEquals(regionId, clusterId);
        for (BrokerEntry brokerEntry : brokerEntries) {
            brokerEntry.setRegionId(DEFAULT_REGION);
            brokerRepository.save(brokerEntry);
        }
    }

    @Override
    public void updateBrokersRegion(List<Long> brokerIdList, Long regionId, Long clusterId) {
        List<BrokerEntry> brokerEntries = brokerRepository
                .findBrokerEntryByBrokerIdInAndClusterIdEquals(brokerIdList, clusterId);
        for (BrokerEntry brokerEntry : brokerEntries) {
            brokerEntry.setRegionId(regionId);
            brokerRepository.save(brokerEntry);
        }
    }

    @Override
    public boolean checkIfBrokersAllExsit(List<Long> brokerIdList, long clusterId) {
        List<BrokerEntry> brokerEntries = brokerRepository
                .findBrokerEntryByBrokerIdInAndClusterIdEquals(brokerIdList, clusterId);
        List<Long> regionBrokerIdList = brokerEntries.stream().map(BrokerEntry::getBrokerId).collect(
                Collectors.toList());
        return regionBrokerIdList.containsAll(brokerIdList);
    }

    @Override
    public List<Long> getBrokerIdListInRegion(long regionId, long clusterId) {
        List<BrokerEntry> brokerEntries = brokerRepository
                .findBrokerEntriesByRegionIdEqualsAndClusterIdEquals(regionId, clusterId);
        List<Long> regionBrokerIdList = brokerEntries.stream().map(BrokerEntry::getBrokerId).collect(
                Collectors.toList());
        return regionBrokerIdList;
    }

    @Override
    public TubeMQResult cloneOffset(String brokerIp, int brokerWebPort, CloneOffsetReq req) {
        return requestMaster(brokerIp, brokerWebPort, req);
    }

    @Override
    public TubeMQResult deleteOffset(String brokerIp, int brokerWebPort, DeleteOffsetReq req) {
        return requestMaster(brokerIp, brokerWebPort, req);
    }

    public TubeMQResult requestMaster(String brokerIp, int brokerWebPort, Object req) {
        String url = SCHEMA + brokerIp + ":" + brokerWebPort
                + "/" + TUBE_REQUEST_PATH + "?" + ConvertUtils
                .convertReqToQueryStr(req);
        return masterService.requestMaster(url);
    }

    @Override
    public OffsetQueryRes queryOffset(String brokerIp, int brokerWebPort, QueryOffsetReq req) {
        String url = SCHEMA + brokerIp + ":" + brokerWebPort
                + "/" + TUBE_REQUEST_PATH + "?" + ConvertUtils
                .convertReqToQueryStr(req);
        return gson.fromJson(masterService.queryMaster(url), OffsetQueryRes.class);
    }
}
