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

package org.apache.inlong.manager.web;

import org.apache.inlong.manager.common.pojo.group.InlongGroupRequest;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.dao.entity.InlongGroupEntity;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Tool testing
 */
public class UtilsTest {

    @Test
    public void testCopyProperties() {
        InlongGroupEntity entity = new InlongGroupEntity();
        entity.setInlongGroupId("test");
        entity.setStatus(1);
        entity.setCreator("user1");
        entity.setModifier("");
        entity.setCreateTime(new Date());
        entity.setModifyTime(null);

        InlongGroupRequest request = new InlongGroupRequest();
        request.setInlongGroupId("info");

        BeanUtils.copyProperties(request, entity);
        Assert.assertEquals(1, (int) entity.getStatus());
        Assert.assertNotNull(entity.getCreateTime());
        Assert.assertNull(entity.getModifyTime());
    }

    @Test
    public void testCopyPropertiesIgnoreNull() {
        InlongGroupEntity entity = new InlongGroupEntity();
        entity.setInlongGroupId("test");
        entity.setCreator("user1");
        entity.setModifier("");
        entity.setStatus(1);
        entity.setCreateTime(new Date());
        entity.setModifyTime(null);

        InlongGroupRequest request = new InlongGroupRequest();
        request.setInlongGroupId("info");

        CommonBeanUtils.copyProperties(request, entity, true);
        Assert.assertEquals(1, (int) entity.getStatus());
        Assert.assertNotNull(entity.getCreateTime());
        Assert.assertNull(entity.getModifyTime());
    }

    @Test
    public void testForeach() {
        List<String> list = new ArrayList<>(Arrays.asList("one", "two", "three"));
        Optional<String> optional = list.stream().filter(e -> e.contains("o")).findFirst();

        Assert.assertTrue(optional.isPresent());
        Assert.assertEquals("one", optional.get());
    }

}
