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

package org.apache.inlong.tubemq.manager.service.region;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.tubemq.manager.controller.TubeMQResult;
import org.apache.inlong.tubemq.manager.entry.BrokerEntry;
import org.apache.inlong.tubemq.manager.entry.RegionEntry;
import org.apache.inlong.tubemq.manager.repository.BrokerRepository;
import org.apache.inlong.tubemq.manager.repository.RegionRepository;
import org.apache.inlong.tubemq.manager.service.RegionServiceImpl;
import org.apache.inlong.tubemq.manager.service.TubeConst;
import org.apache.inlong.tubemq.manager.service.TubeMQErrorConst;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@RunWith(SpringJUnit4ClassRunner.class)
public class TestRegionService {

    @MockBean
    private RegionRepository regionRepository;

    @MockBean
    private BrokerRepository brokerRepository;

    @Autowired
    @InjectMocks
    private RegionServiceImpl regionService;

    @Test
    public void testNoResource() {
        RegionEntry regionEntry = new RegionEntry();
        regionEntry.setRegionId(1L);
        regionEntry.setClusterId(1L);
        List<Long> brokerIdList = new ArrayList<>();
        brokerIdList.add(1L);
        brokerIdList.add(2L);
        TubeMQResult result = regionService.createNewRegion(regionEntry, brokerIdList);
        assertThat(result.getErrMsg().equals(TubeMQErrorConst.RESOURCE_NOT_EXIST));
    }

    @Test
    public void testCreateNewRegion() {
        RegionEntry regionEntry = new RegionEntry();
        regionEntry.setRegionId(1L);
        regionEntry.setClusterId(1L);
        List<Long> brokerIdList = new ArrayList<>();
        brokerIdList.add(1L);
        RegionEntry regionEntry1 = new RegionEntry();
        regionEntry1.setRegionId(1L);
        List<RegionEntry> regionEntries = new ArrayList<>();
        regionEntries.add(regionEntry1);

        BrokerEntry brokerEntry1 = new BrokerEntry();
        brokerEntry1.setBrokerId(1L);
        List<BrokerEntry> brokerEntries = new ArrayList<>();
        brokerEntries.add(brokerEntry1);
        doReturn(regionEntries).when(regionRepository)
                .findRegionEntriesByClusterIdEquals(any(Integer.class));
        doReturn(brokerEntries).when(brokerRepository)
                .findBrokerEntryByBrokerIdInAndClusterIdEquals(any(List.class), any(Long.class));
        TubeMQResult result = regionService.createNewRegion(regionEntry, brokerIdList);
        assertThat(result.getErrCode() == TubeConst.SUCCESS_CODE);
    }

}
