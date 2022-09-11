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

package org.apache.inlong.manager.service.mq.util;

import com.google.common.collect.Lists;
import org.apache.inlong.manager.common.pojo.group.InlongGroupExtInfo;
import org.apache.inlong.manager.common.pojo.group.pulsar.InlongPulsarInfo;
import org.apache.inlong.manager.common.settings.InlongGroupSettings;
import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.admin.internal.PulsarAdminImpl;
import org.apache.pulsar.client.api.Authentication;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.impl.auth.AuthenticationDisabled;
import org.junit.Assert;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * Test class for pulsar utils.
 */
public class PulsarUtilsTest {

    // There will be concurrency problems in the overall operation,This method temporarily fails the test
    // @Test
    public void testGetPulsarAdmin() {
        InlongGroupExtInfo groupExtInfo1 = new InlongGroupExtInfo();
        groupExtInfo1.setId(1);
        groupExtInfo1.setInlongGroupId("group1");
        groupExtInfo1.setKeyName(InlongGroupSettings.PULSAR_ADMIN_URL);
        groupExtInfo1.setKeyValue("http://127.0.0.1:8080");

        InlongGroupExtInfo groupExtInfo2 = new InlongGroupExtInfo();
        groupExtInfo2.setId(2);
        groupExtInfo2.setInlongGroupId("group1");
        groupExtInfo2.setKeyName(InlongGroupSettings.PULSAR_AUTHENTICATION);
        groupExtInfo2.setKeyValue("QWEASDZXC");
        ArrayList<InlongGroupExtInfo> groupExtInfoList = Lists.newArrayList(groupExtInfo1, groupExtInfo2);
        InlongPulsarInfo groupInfo = new InlongPulsarInfo();
        groupInfo.setExtList(groupExtInfoList);

        final String defaultServiceUrl = "http://127.0.0.1:10080";
        try {
            PulsarAdmin admin = PulsarUtils.getPulsarAdmin(defaultServiceUrl);
            Assert.assertEquals("http://127.0.0.1:8080", admin.getServiceUrl());
            Field auth = ReflectionUtils.findField(PulsarAdminImpl.class, "auth");
            assert auth != null;
            auth.setAccessible(true);
            Authentication authentication = (Authentication) auth.get(admin);
            Assert.assertNotNull(authentication);

            InlongGroupExtInfo groupExtInfo3 = new InlongGroupExtInfo();
            groupExtInfo3.setId(3);
            groupExtInfo3.setInlongGroupId("group1");
            groupExtInfo3.setKeyName(InlongGroupSettings.PULSAR_AUTHENTICATION_TYPE);
            groupExtInfo3.setKeyValue("token1");
            groupExtInfoList.add(groupExtInfo3);
            try {
                admin = PulsarUtils.getPulsarAdmin(defaultServiceUrl);
            } catch (Exception e) {
                if (e instanceof IllegalArgumentException) {
                    Assert.assertTrue(e.getMessage().contains("illegal authentication type"));
                }
            }

            groupExtInfoList = new ArrayList<>();
            groupInfo.setExtList(groupExtInfoList);
            admin = PulsarUtils.getPulsarAdmin(defaultServiceUrl);
            Assert.assertEquals("http://127.0.0.1:10080", admin.getServiceUrl());
            auth = ReflectionUtils.findField(PulsarAdminImpl.class, "auth");
            assert auth != null;
            auth.setAccessible(true);
            authentication = (Authentication) auth.get(admin);
            Assert.assertTrue(authentication instanceof AuthenticationDisabled);
        } catch (PulsarClientException | IllegalAccessException e) {
            Assert.fail();
        }
    }

}
