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
import org.apache.inlong.manager.dao.entity.DataNodeEntity;
import org.apache.inlong.manager.pojo.node.DataNodePageRequest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

/**
 * Data node mapper test
 */
public class DataNodeEntityMapperTest extends DaoBaseTest {

    @Autowired
    private DataNodeEntityMapper dataNodeEntityMapper;

    @Test
    public void deleteByPrimaryKey() {
        DataNodeEntity entity = genEntity();
        dataNodeEntityMapper.insert(entity);
        Assertions.assertEquals(1, dataNodeEntityMapper.deleteById(entity.getId()));
        Assertions.assertNull(dataNodeEntityMapper.selectByUniqueKey(entity.getName(), entity.getType()));
    }

    @Test
    public void deleteByPrimaryKeyWithOtherTenant() {
        DataNodeEntity entity = genEntity();
        dataNodeEntityMapper.insert(entity);
        setOtherTenant(ANOTHER_TENANT);
        Assertions.assertEquals(0, dataNodeEntityMapper.deleteById(entity.getId()));
        Assertions.assertNull(dataNodeEntityMapper.selectByUniqueKey(entity.getName(), entity.getType()));
        setOtherTenant(PUBLIC_TENANT);
        Assertions.assertNotNull(dataNodeEntityMapper.selectByUniqueKey(entity.getName(), entity.getType()));
    }

    @Test
    public void selectByCondition() {
        DataNodeEntity entity = genEntity();
        dataNodeEntityMapper.insert(entity);
        DataNodePageRequest request = new DataNodePageRequest();
        request.setType("test type");
        Assertions.assertEquals(1, dataNodeEntityMapper.selectByCondition(request).size());
        setOtherTenant(ANOTHER_TENANT);
        Assertions.assertEquals(0, dataNodeEntityMapper.selectByCondition(request).size());
    }

    @Test
    public void updateById() {
        DataNodeEntity entity = genEntity();
        dataNodeEntityMapper.insert(entity);
        entity = dataNodeEntityMapper.selectById(entity.getId());
        String newType = "newType";
        entity.setType(newType);
        Assertions.assertEquals(1, dataNodeEntityMapper.updateById(entity));
        entity = dataNodeEntityMapper.selectById(entity.getId());
        Assertions.assertEquals(newType, entity.getType());

        setOtherTenant(ANOTHER_TENANT);
        Assertions.assertEquals(0, dataNodeEntityMapper.updateById(entity));
    }

    private DataNodeEntity genEntity() {
        DataNodeEntity entity = new DataNodeEntity();
        entity.setModifier(ADMIN);
        entity.setCreator(ADMIN);
        entity.setDescription("test datanode entity");
        entity.setDisplayName("test datanode");
        entity.setName("test datanode");
        entity.setInCharges(ADMIN);
        entity.setType("test type");
        Date now = new Date();
        entity.setCreateTime(now);
        entity.setModifyTime(now);
        entity.setVersion(InlongConstants.INITIAL_VERSION);
        return entity;
    }
}
