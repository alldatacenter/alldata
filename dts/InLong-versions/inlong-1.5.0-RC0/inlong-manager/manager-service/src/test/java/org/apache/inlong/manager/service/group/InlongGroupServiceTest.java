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

package org.apache.inlong.manager.service.group;

import org.apache.inlong.common.constant.MQType;
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.dao.entity.InlongGroupExtEntity;
import org.apache.inlong.manager.dao.mapper.InlongGroupExtEntityMapper;
import org.apache.inlong.manager.pojo.group.InlongGroupExtInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.group.pulsar.InlongPulsarInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

import java.util.Arrays;
import java.util.List;

/**
 * Test for {@link InlongGroupService}
 */
@TestComponent
public class InlongGroupServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(InlongGroupServiceTest.class);

    private final String globalGroupId = "group1";
    private final String globalOperator = "admin";
    @Autowired
    public InlongGroupService groupService;
    @Autowired
    InlongGroupExtEntityMapper groupExtMapper;

    /**
     * Save one inlong group with Pulsar info
     */
    public String saveGroup(String inlongGroupId, String operator) {
        InlongGroupInfo groupInfo;
        try {
            groupInfo = groupService.get(inlongGroupId);
            if (groupInfo != null) {
                return groupInfo.getInlongGroupId();
            }
        } catch (Exception e) {
            // ignore
        }

        InlongPulsarInfo pulsarInfo = new InlongPulsarInfo();
        pulsarInfo.setInlongGroupId(inlongGroupId);
        pulsarInfo.setMqType(MQType.PULSAR);
        pulsarInfo.setCreator(operator);
        pulsarInfo.setInCharges(operator);
        pulsarInfo.setStatus(GroupStatus.CONFIG_SUCCESSFUL.getCode());

        pulsarInfo.setEnsemble(3);
        pulsarInfo.setWriteQuorum(3);
        pulsarInfo.setAckQuorum(2);

        return groupService.save(pulsarInfo.genRequest(), operator);
    }

    /**
     * Delete one inlong group
     */
    public Boolean deleteGroup(String groupId, String operator) {
        return groupService.delete(groupId, operator);
    }

    // @TestComponent runs as a whole without injecting objects
    // @Test
    public void testSaveAndUpdateExt() {
        // check insert
        InlongGroupExtInfo groupExtInfo1 = new InlongGroupExtInfo();
        groupExtInfo1.setId(1);
        groupExtInfo1.setInlongGroupId(globalGroupId);
        groupExtInfo1.setKeyName("pulsar_url");
        groupExtInfo1.setKeyValue("http://127.0.0.1:8080");

        InlongGroupExtInfo groupExtInfo2 = new InlongGroupExtInfo();
        groupExtInfo2.setId(2);
        groupExtInfo2.setInlongGroupId(globalGroupId);
        groupExtInfo2.setKeyName("pulsar_secret");
        groupExtInfo2.setKeyValue("QWEASDZXC");

        List<InlongGroupExtInfo> groupExtInfoList = Arrays.asList(groupExtInfo1, groupExtInfo2);
        groupService.saveOrUpdateExt(globalGroupId, groupExtInfoList);

        List<InlongGroupExtEntity> extEntityList = groupExtMapper.selectByGroupId(globalGroupId);
        Assertions.assertEquals(2, extEntityList.size());
        Assertions.assertEquals("pulsar_url", extEntityList.get(0).getKeyName());
        Assertions.assertEquals("http://127.0.0.1:8080", extEntityList.get(0).getKeyValue());

        // check update
        groupExtInfo1.setKeyValue("http://127.0.0.1:8081");
        groupService.saveOrUpdateExt(globalGroupId, groupExtInfoList);
        extEntityList = groupExtMapper.selectByGroupId(globalGroupId);
        Assertions.assertEquals(2, extEntityList.size());
        Assertions.assertEquals("http://127.0.0.1:8081", extEntityList.get(0).getKeyValue());

        groupExtInfo2.setKeyValue("qweasdzxc");
        groupService.saveOrUpdateExt(globalGroupId, groupExtInfoList);
        extEntityList = groupExtMapper.selectByGroupId(globalGroupId);
        Assertions.assertEquals(2, extEntityList.size());
        Assertions.assertEquals("qweasdzxc", extEntityList.get(1).getKeyValue());
    }

    @Test
    public void test() {
        LOGGER.info("If you don't add test, UnusedImports: Unused import: org.junit.Test.");
    }
}
