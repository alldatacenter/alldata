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

package org.apache.inlong.manager.service.queue;

import org.apache.inlong.manager.service.resource.queue.pulsar.PulsarUtils;
import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.admin.internal.PulsarAdminImpl;
import org.apache.pulsar.client.api.Authentication;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.impl.auth.AuthenticationDisabled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;

/**
 * Test class for Pulsar utils.
 */
public class PulsarUtilsTest {

    @Test
    public void testGetPulsarAdmin() {
        final String defaultServiceUrl = "http://127.0.0.1:10080";
        try {
            PulsarAdmin admin = PulsarUtils.getPulsarAdmin(defaultServiceUrl);
            Assertions.assertEquals(defaultServiceUrl, admin.getServiceUrl());
            Field auth = ReflectionUtils.findField(PulsarAdminImpl.class, "auth");
            assert auth != null;
            auth.setAccessible(true);
            Authentication authentication = (Authentication) auth.get(admin);
            Assertions.assertNotNull(authentication);
            Assertions.assertTrue(authentication instanceof AuthenticationDisabled);
        } catch (PulsarClientException | IllegalAccessException e) {
            Assertions.fail();
        }
    }

}
