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

package org.apache.inlong.tubemq.manager.service.broker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.tubemq.manager.entry.BrokerEntry;
import org.apache.inlong.tubemq.manager.repository.BrokerRepository;
import org.apache.inlong.tubemq.manager.service.BrokerServiceImpl;
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
public class TestBrokerService {

    @MockBean
    private BrokerRepository brokerRepository;

    @Autowired
    @InjectMocks
    private BrokerServiceImpl brokerService;

    @Test
    public void testBrokerService() {
        BrokerEntry entry1 = new BrokerEntry();
        entry1.setBrokerId(1L);
        BrokerEntry entry2 = new BrokerEntry();
        entry2.setBrokerId(2L);
        List<BrokerEntry> brokers = new ArrayList<>();
        brokers.add(entry1);
        brokers.add(entry2);
        List<Long> brokerIdList = new ArrayList<>();
        brokerIdList.add(1L);
        brokerIdList.add(2L);
        doReturn(brokers).when(brokerRepository)
                .findBrokerEntryByBrokerIdInAndClusterIdEquals(brokerIdList, 1L);
        assertThat(brokerService.checkIfBrokersAllExsit(brokerIdList, 1));
    }
}
