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

package org.apache.atlas.notification.hook;

import org.apache.atlas.model.notification.MessageSource;
import org.apache.atlas.model.notification.HookNotification;
import org.apache.atlas.notification.entity.EntityNotificationTest;
import org.apache.atlas.v1.model.instance.Referenceable;
import org.apache.atlas.v1.model.instance.Struct;
import org.apache.atlas.notification.AbstractNotification;
import org.apache.atlas.v1.model.notification.HookNotificationV1.EntityUpdateRequest;
import org.apache.atlas.type.AtlasType;
import org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * HookMessageDeserializer tests.
 */
public class HookNotificationDeserializerTest {
    private HookMessageDeserializer deserializer = new HookMessageDeserializer();
    MessageSource source = new MessageSource(this.getClass().getSimpleName());

    @Test
    public void testDeserialize() throws Exception {
        Referenceable       entity      = generateEntityWithTrait();
        EntityUpdateRequest message     = new EntityUpdateRequest("user1", entity);
        List<String>        jsonMsgList = new ArrayList<>();

        AbstractNotification.createNotificationMessages(message, jsonMsgList, source);

        HookNotification deserializedMessage = deserialize(jsonMsgList);

        assertEqualMessage(deserializedMessage, message);
    }

    // validate deserialization of legacy message, which doesn't use MessageVersion
    @Test
    public void testDeserializeLegacyMessage() throws Exception {
        Referenceable       entity              = generateEntityWithTrait();
        EntityUpdateRequest message             = new EntityUpdateRequest("user1", entity);
        String              jsonMsg             = AtlasType.toV1Json(message);
        HookNotification    deserializedMessage = deserialize(Collections.singletonList(jsonMsg));

        assertEqualMessage(deserializedMessage, message);
    }

    @Test
    public void testDeserializeCompressedMessage() throws Exception {
        Referenceable       entity     = generateLargeEntityWithTrait();
        EntityUpdateRequest message    = new EntityUpdateRequest("user1", entity);
        List<String>       jsonMsgList = new ArrayList<>();

        AbstractNotification.createNotificationMessages(message, jsonMsgList, source);

        assertTrue(jsonMsgList.size() == 1);

        String compressedMsg   = jsonMsgList.get(0);
        String uncompressedMsg = AtlasType.toV1Json(message);

        assertTrue(compressedMsg.length() < uncompressedMsg.length(), "Compressed message (" + compressedMsg.length() + ") should be shorter than uncompressed message (" + uncompressedMsg.length() + ")");

        HookNotification deserializedMessage = deserialize(jsonMsgList);

        assertEqualMessage(deserializedMessage, message);
    }

    @Test
    public void testDeserializeSplitMessage() throws Exception {
        Referenceable       entity      = generateVeryLargeEntityWithTrait();
        EntityUpdateRequest message     = new EntityUpdateRequest("user1", entity);
        List<String>        jsonMsgList = new ArrayList<>();

        AbstractNotification.createNotificationMessages(message, jsonMsgList, source);

        assertTrue(jsonMsgList.size() > 1);

        HookNotification deserializedMessage = deserialize(jsonMsgList);

        assertEqualMessage(deserializedMessage, message);
    }

    private Referenceable generateEntityWithTrait() {
        Referenceable ret = EntityNotificationTest.getEntity("id", new Struct("MyTrait", Collections.<String, Object>emptyMap()));

        return ret;
    }

    private HookNotification deserialize(List<String> jsonMsgList) {
        HookNotification deserializedMessage = null;

        for (String jsonMsg : jsonMsgList) {
            deserializedMessage = deserializer.deserialize(jsonMsg);

            if (deserializedMessage != null) {
                break;
            }
        }

        return deserializedMessage;
    }

    private void assertEqualMessage(HookNotification deserializedMessage, EntityUpdateRequest message) throws Exception {
        assertNotNull(deserializedMessage);
        assertEquals(deserializedMessage.getType(), message.getType());
        assertEquals(deserializedMessage.getUser(), message.getUser());

        assertTrue(deserializedMessage instanceof EntityUpdateRequest);

        EntityUpdateRequest deserializedEntityUpdateRequest = (EntityUpdateRequest) deserializedMessage;
        Referenceable       deserializedEntity              = deserializedEntityUpdateRequest.getEntities().get(0);
        Referenceable       entity                          = message.getEntities().get(0);
        String              traitName                       = entity.getTraitNames().get(0);

        assertEquals(deserializedEntity.getId(), entity.getId());
        assertEquals(deserializedEntity.getTypeName(), entity.getTypeName());
        assertEquals(deserializedEntity.getTraits(), entity.getTraits());
        assertEquals(deserializedEntity.getTrait(traitName).hashCode(), entity.getTrait(traitName).hashCode());

    }

    private Referenceable generateLargeEntityWithTrait() {
        Referenceable ret = EntityNotificationTest.getEntity("id", new Struct("MyTrait", Collections.<String, Object>emptyMap()));

        // add 100 attributes, each with value of size 10k
        // Json Size=1,027,984; GZipped Size=16,387 ==> will compress, but not split
        String attrValue = RandomStringUtils.randomAlphanumeric(10 * 1024); // use the same value for all attributes - to aid better compression
        for (int i = 0; i < 100; i++) {
            ret.set("attr_" + i, attrValue);
        }

        return ret;
    }

    private Referenceable generateVeryLargeEntityWithTrait() {
        Referenceable ret = EntityNotificationTest.getEntity("id", new Struct("MyTrait", Collections.<String, Object>emptyMap()));

        // add 300 attributes, each with value of size 10k
        // Json Size=3,082,384; GZipped Size=2,313,357 ==> will compress & split
        for (int i = 0; i < 300; i++) {
            ret.set("attr_" + i, RandomStringUtils.randomAlphanumeric(10 * 1024));
        }

        return ret;
    }
}
