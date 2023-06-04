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

import static org.apache.inlong.tubemq.manager.service.TubeConst.DELETE_FAIL;

import java.util.Date;
import java.util.List;
import javax.transaction.Transactional;

import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.tubemq.manager.controller.TubeMQResult;
import org.apache.inlong.tubemq.manager.controller.cluster.dto.ClusterDto;
import org.apache.inlong.tubemq.manager.controller.cluster.request.AddClusterReq;
import org.apache.inlong.tubemq.manager.entry.ClusterEntry;
import org.apache.inlong.tubemq.manager.entry.MasterEntry;
import org.apache.inlong.tubemq.manager.repository.ClusterRepository;
import org.apache.inlong.tubemq.manager.service.interfaces.ClusterService;
import org.apache.inlong.tubemq.manager.service.interfaces.MasterService;
import org.apache.inlong.tubemq.manager.service.interfaces.NodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ClusterServiceImpl implements ClusterService {

    @Autowired
    ClusterRepository clusterRepository;

    @Autowired
    NodeService nodeService;

    @Autowired
    MasterService masterService;

    @Override
    @Transactional(rollbackOn = Exception.class)
    public void addClusterAndMasterNode(AddClusterReq req) {
        ClusterEntry entry = new ClusterEntry();
        if (req.getId() != null) {
            entry.setClusterId(req.getId());
        }
        entry.setCreateTime(new Date());
        entry.setCreateUser(req.getCreateUser());
        entry.setClusterName(req.getClusterName());
        entry.setReloadBrokerSize(req.getReloadBrokerSize());
        ClusterEntry retEntry = clusterRepository.saveAndFlush(entry);
        // add master node
        addMasterNode(req, retEntry);
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public void deleteCluster(Long clusterId) {
        masterService.deleteMaster(clusterId);
        Integer successCode = clusterRepository.deleteByClusterId(clusterId);
        if (successCode.equals(DELETE_FAIL)) {
            throw new RuntimeException("no such cluster with clusterId = " + clusterId);
        }
    }

    @Override
    public ClusterEntry getOneCluster(long clusterId) {
        return clusterRepository
                .findClusterEntryByClusterId(clusterId);
    }

    @Override
    public ClusterEntry getOneCluster(String clusterName) {
        return clusterRepository
                .findClusterEntryByClusterName(clusterName);
    }

    @Override
    public List<ClusterEntry> getAllClusters() {
        return clusterRepository.findAll();
    }

    @Override
    public TubeMQResult modifyCluster(ClusterDto clusterDto) {
        try {
            ClusterEntry cluster = clusterRepository
                    .findClusterEntryByClusterId(clusterDto.getClusterId());
            cluster.setClusterName(clusterDto.getClusterName());
            cluster.setReloadBrokerSize(clusterDto.getReloadBrokerSize());
            clusterRepository.save(cluster);
        } catch (Exception e) {
            return TubeMQResult.errorResult(e.getMessage());
        }
        return TubeMQResult.successResult();
    }

    @Transactional(rollbackOn = Exception.class)
    public void addMasterNode(AddClusterReq req, ClusterEntry clusterEntry) {
        if (clusterEntry == null) {
            return;
        }
        for (MasterEntry masterEntry : req.getMasterEntries()) {
            masterEntry.setPort(masterEntry.getPort());
            masterEntry.setClusterId(req.getId());
            masterEntry.setWebPort(masterEntry.getWebPort());
            masterEntry.setIp(masterEntry.getIp());
            masterEntry.setToken(req.getToken());
            nodeService.addNode(masterEntry);
        }
    }

}
