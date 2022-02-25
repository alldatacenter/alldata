/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.notification;

import org.apache.atlas.model.notification.AtlasNotificationMessage;
import org.apache.atlas.model.notification.MessageSource;
import org.apache.atlas.model.notification.MessageVersion;
import org.apache.atlas.notification.entity.EntityNotificationTest;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.v1.model.instance.Referenceable;
import org.apache.atlas.v1.model.instance.Struct;
import org.apache.atlas.v1.model.notification.HookNotificationV1;
import org.testng.annotations.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Collections;

import static org.testng.Assert.*;

/**
 * AtlasNotificationMessage tests.
 */
public class AtlasNotificationMessageTest {
    
    @Test
    public void testGetVersion() throws Exception {
        MessageVersion version = new MessageVersion("1.0.0");
        AtlasNotificationMessage<String> atlasNotificationMessage = new AtlasNotificationMessage<>(version, "a");
        assertEquals(atlasNotificationMessage.getVersion(), version);
    }

    @Test
    public void testGetMessage() throws Exception {
        String message = "a";
        MessageVersion version = new MessageVersion("1.0.0");
        AtlasNotificationMessage<String> atlasNotificationMessage = new AtlasNotificationMessage<>(version, message);
        assertEquals(atlasNotificationMessage.getMessage(), message);
    }

    @Test
    public void testCompareVersion() throws Exception {
        MessageVersion version1 = new MessageVersion("1.0.0");
        MessageVersion version2 = new MessageVersion("2.0.0");
        MessageVersion version3 = new MessageVersion("0.5.0");

        AtlasNotificationMessage<String> atlasNotificationMessage = new AtlasNotificationMessage<>(version1, "a");

        assertTrue(atlasNotificationMessage.compareVersion(version1) == 0);
        assertTrue(atlasNotificationMessage.compareVersion(version2) < 0);
        assertTrue(atlasNotificationMessage.compareVersion(version3) > 0);
    }

    @Test
    public void testMessageSource() throws Exception {
        Referenceable entity                           = generateEntityWithTrait();
        HookNotificationV1.EntityUpdateRequest message = new HookNotificationV1.EntityUpdateRequest("user1", entity);
        MessageSource source                           = new MessageSource(this.getClass().getSimpleName());
        List<String> jsonList                          = new LinkedList<>();

        AbstractNotification.createNotificationMessages(message, jsonList, source);
        for(Object json :  jsonList) {
            AtlasNotificationMessage atlasNotificationMessage = AtlasType.fromV1Json((String) json, AtlasNotificationMessage.class);
            assertEquals("\"" + source.getSource() + "\"" ,AtlasType.toV1Json(atlasNotificationMessage.getSource().getSource()));
        }
    }

    private Referenceable generateEntityWithTrait() {
        Referenceable ret = EntityNotificationTest.getEntity("id", new Struct("MyTrait", Collections.<String, Object>emptyMap()));

        return ret;
    }

}
