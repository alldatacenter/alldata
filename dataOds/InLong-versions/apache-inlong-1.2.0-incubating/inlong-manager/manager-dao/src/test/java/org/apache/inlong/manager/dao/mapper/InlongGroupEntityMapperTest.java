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

import org.apache.inlong.manager.dao.DaoBaseTest;
import org.apache.inlong.manager.dao.entity.InlongGroupEntity;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

/**
 * Inlong group entity mapper test for {@link InlongGroupEntityMapper}
 */
public class InlongGroupEntityMapperTest extends DaoBaseTest {

    @Autowired
    private InlongGroupEntityMapper groupEntityMapper;

    @Test
    public void deleteByPrimaryKey() {
        InlongGroupEntity entity = createHeartbeatEntity();
        groupEntityMapper.insert(entity);
        groupEntityMapper.deleteByPrimaryKey(entity.getId());
        Assert.assertNull(groupEntityMapper.selectByGroupId(entity.getInlongGroupId()));
    }

    @Test
    public void selectByPrimaryKey() {
        InlongGroupEntity entity = createHeartbeatEntity();
        groupEntityMapper.insert(entity);
        Assert.assertEquals(entity, groupEntityMapper.selectByPrimaryKey(entity.getId()));
    }

    private InlongGroupEntity createHeartbeatEntity() {
        InlongGroupEntity entity = new InlongGroupEntity();
        entity.setInlongGroupId("test_group");
        entity.setMqResource("test_group");
        entity.setInCharges("admin");
        entity.setCreator("admin");
        entity.setCreateTime(new Date());
        return entity;
    }

}