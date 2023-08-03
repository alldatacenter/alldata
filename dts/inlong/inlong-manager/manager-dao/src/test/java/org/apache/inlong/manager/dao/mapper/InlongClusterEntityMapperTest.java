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
import org.apache.inlong.manager.dao.entity.InlongClusterEntity;
import org.apache.inlong.manager.pojo.cluster.ClusterPageRequest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Inlong cluster entity mapper test
 */
public class InlongClusterEntityMapperTest extends DaoBaseTest {

    @Autowired
    private InlongClusterEntityMapper entityMapper;

    @Test
    public void deleteByPrimaryKey() {
        InlongClusterEntity entity = genEntity();
        entityMapper.insert(entity);
        Assertions.assertEquals(1, entityMapper.deleteByPrimaryKey(entity.getId()));
        Assertions.assertNull(entityMapper.selectByNameAndType(entity.getName(), entity.getType()));
    }

    @Test
    public void deleteByPrimaryKeyWithOtherTenant() {
        InlongClusterEntity entity = genEntity();
        entityMapper.insert(entity);
        setOtherTenant(ANOTHER_TENANT);
        Assertions.assertEquals(0, entityMapper.deleteByPrimaryKey(entity.getId()));
        Assertions.assertNull(entityMapper.selectByNameAndType(entity.getName(), entity.getType()));
        setOtherTenant(PUBLIC_TENANT);
        Assertions.assertNotNull(entityMapper.selectByNameAndType(entity.getName(), entity.getType()));
    }

    @Test
    public void selectByCondition() {
        InlongClusterEntity entity = genEntity();
        entityMapper.insert(entity);

        ClusterPageRequest request = new ClusterPageRequest();
        request.setClusterTag("testTag");

        Assertions.assertEquals(1, entityMapper.selectByCondition(request).size());
        setOtherTenant(ANOTHER_TENANT);
        Assertions.assertEquals(0, entityMapper.selectByCondition(request).size());
    }

    @Test
    public void updateById() {
        InlongClusterEntity entity = genEntity();
        entityMapper.insert(entity);

        entity = entityMapper.selectById(entity.getId());
        String newType = "newType";
        entity.setType(newType);

        // the tenant will be modified in mybatis interceptor, set tenant here does not work
        entity.setTenant(ANOTHER_TENANT);
        Assertions.assertEquals(1, entityMapper.updateById(entity));
        entity = entityMapper.selectById(entity.getId());
        Assertions.assertEquals(newType, entity.getType());

        // should be public
        Assertions.assertEquals(PUBLIC_TENANT, entity.getTenant());

        setOtherTenant(ANOTHER_TENANT);
        Assertions.assertEquals(0, entityMapper.updateById(entity));
    }

    private InlongClusterEntity genEntity() {
        InlongClusterEntity entity = new InlongClusterEntity();
        entity.setClusterTags("testTag");
        entity.setCreator(ADMIN);
        entity.setModifier(ADMIN);
        entity.setInCharges(ADMIN);
        entity.setDisplayName("testCluster");
        entity.setName("testCluster");
        return entity;
    }

}