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

package org.apache.inlong.manager.dao.mapper;

import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.dao.DaoBaseTest;
import org.apache.inlong.manager.dao.entity.InlongGroupEntity;
import org.apache.inlong.manager.pojo.group.InlongGroupPageRequest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Inlong group entity mapper test for {@link InlongGroupEntityMapper}
 */
public class InlongGroupEntityMapperTest extends DaoBaseTest {

    @Autowired
    private InlongGroupEntityMapper groupEntityMapper;

    @Test
    public void deleteByPrimaryKey() {
        InlongGroupEntity entity = createGroupEntity();
        groupEntityMapper.insert(entity);
        int delete = groupEntityMapper.deleteByPrimaryKey(entity.getId());
        Assertions.assertEquals(1, delete);
        Assertions.assertNull(groupEntityMapper.selectByGroupId(entity.getInlongGroupId()));
    }

    @Test
    public void deleteByPrimaryKeyWithOtherTenant() {
        InlongGroupEntity entity = createGroupEntity();
        groupEntityMapper.insert(entity);
        setOtherTenant(ANOTHER_TENANT);
        int delete = groupEntityMapper.deleteByPrimaryKey(entity.getId());
        setOtherTenant(PUBLIC_TENANT);
        InlongGroupEntity select = groupEntityMapper.selectByGroupId(entity.getInlongGroupId());
        Assertions.assertEquals(0, delete);
        Assertions.assertNotNull(select);
    }

    @Test
    public void selectByPrimaryKey() {
        InlongGroupEntity entity = createGroupEntity();
        groupEntityMapper.insert(entity);
        InlongGroupEntity groupEntity = groupEntityMapper.selectByPrimaryKey(entity.getId());
        Assertions.assertEquals(entity.getInlongGroupId(), groupEntity.getInlongGroupId());
    }

    @Test
    public void selectByPrimaryKeyWithOtherTenant() {
        InlongGroupEntity entity = createGroupEntity();
        groupEntityMapper.insert(entity);
        setOtherTenant(ANOTHER_TENANT);
        InlongGroupEntity groupEntity = groupEntityMapper.selectByPrimaryKey(entity.getId());
        Assertions.assertNull(groupEntity);
    }

    @Test
    public void selectByCondition() {
        InlongGroupEntity entity = createGroupEntity();
        groupEntityMapper.insert(entity);
        InlongGroupPageRequest request = new InlongGroupPageRequest();
        List<String> groups = new LinkedList<>();
        request.setGroupIdList(groups);
        groups.add(entity.getInlongGroupId());
        List<InlongGroupEntity> entities = groupEntityMapper.selectByCondition(request);
        Assertions.assertEquals(1, entities.size());
    }

    @Test
    public void selectByConditionWithOtherTenant() {
        InlongGroupEntity entity = createGroupEntity();
        groupEntityMapper.insert(entity);
        setOtherTenant(ANOTHER_TENANT);
        InlongGroupPageRequest request = new InlongGroupPageRequest();
        List<String> groups = new LinkedList<>();
        request.setGroupIdList(groups);
        groups.add(entity.getInlongGroupId());
        List<InlongGroupEntity> entities = groupEntityMapper.selectByCondition(request);
        Assertions.assertEquals(0, entities.size());
    }

    private InlongGroupEntity createGroupEntity() {
        InlongGroupEntity entity = new InlongGroupEntity();
        entity.setInlongGroupId("test_group");
        entity.setMqResource("test_group");
        entity.setInCharges("admin");
        entity.setCreator("admin");
        entity.setModifier("admin");
        Date now = new Date();
        entity.setCreateTime(now);
        entity.setModifyTime(now);
        entity.setVersion(InlongConstants.INITIAL_VERSION);
        return entity;
    }

}