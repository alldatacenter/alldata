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

package org.apache.atlas.notification.entity;

import org.apache.atlas.model.notification.EntityNotification;
import org.apache.atlas.model.notification.MessageSource;
import org.apache.atlas.v1.model.instance.Referenceable;
import org.apache.atlas.v1.model.instance.Struct;
import org.apache.atlas.notification.AbstractNotification;
import org.apache.atlas.v1.model.notification.EntityNotificationV1;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * EntityMessageDeserializer tests.
 */
public class EntityNotificationDeserializerTest {
    private EntityMessageDeserializer deserializer = new EntityMessageDeserializer();
    MessageSource source = new MessageSource(this.getClass().getSimpleName());

    @Test
    public void testDeserialize() throws Exception {
        Referenceable        entity       = EntityNotificationTest.getEntity("id");
        String               traitName    = "MyTrait";
        List<Struct>         traits       = Collections.singletonList(new Struct(traitName, Collections.<String, Object>emptyMap()));
        EntityNotificationV1 notification = new EntityNotificationV1(entity, EntityNotificationV1.OperationType.TRAIT_ADD, traits);
        List<String>         jsonMsgList  = new ArrayList<>();

        AbstractNotification.createNotificationMessages(notification, jsonMsgList, source);

        EntityNotification deserializedNotification = null;

        for (String jsonMsg : jsonMsgList) {
            deserializedNotification =  deserializer.deserialize(jsonMsg);

            if (deserializedNotification != null) {
                break;
            }
        }

        assertTrue(deserializedNotification instanceof EntityNotificationV1);

        EntityNotificationV1 entityNotificationV1 = (EntityNotificationV1)deserializedNotification;

        assertEquals(entityNotificationV1.getOperationType(), notification.getOperationType());
        assertEquals(entityNotificationV1.getEntity().getId(), notification.getEntity().getId());
        assertEquals(entityNotificationV1.getEntity().getTypeName(), notification.getEntity().getTypeName());
        assertEquals(entityNotificationV1.getEntity().getTraits(), notification.getEntity().getTraits());
        assertEquals(entityNotificationV1.getEntity().getTrait(traitName), notification.getEntity().getTrait(traitName));
    }
}
