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

import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntitiesWithExtInfo;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.model.notification.HookNotification;
import org.apache.atlas.model.notification.HookNotification.EntityCreateRequestV2;
import org.apache.atlas.model.notification.HookNotification.EntityDeleteRequestV2;
import org.apache.atlas.model.notification.HookNotification.EntityUpdateRequestV2;
import org.apache.atlas.model.notification.HookNotification.EntityPartialUpdateRequestV2;
import org.apache.atlas.model.notification.HookNotification.HookNotificationType;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeUtil;
import org.apache.atlas.utils.AtlasJson;
import org.apache.atlas.v1.model.instance.Referenceable;
import org.apache.atlas.v1.model.notification.HookNotificationV1.EntityCreateRequest;

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


public class HookNotificationTest {
    private HookMessageDeserializer deserializer = new HookMessageDeserializer();

    private static final String  ATTR_VALUE_STRING  = "strValue";
    private static final Integer ATTR_VALUE_INTEGER = 10;
    private static final Boolean ATTR_VALUE_BOOLEAN = Boolean.TRUE;
    private static final Date    ATTR_VALUE_DATE    = new Date();

    @Test
    public void testNewMessageSerDe() throws Exception {
        Referenceable entity1 = new Referenceable("sometype");
        Referenceable entity2 = new Referenceable("newtype");

        entity1.set("attr", "value");
        entity1.set("complex", new Referenceable("othertype"));

        String user = "user";

        EntityCreateRequest request           = new EntityCreateRequest(user, entity1, entity2);
        String              notificationJson  = AtlasType.toV1Json(request);
        HookNotification    actualNotification = deserializer.deserialize(notificationJson);

        assertEquals(actualNotification.getType(), HookNotificationType.ENTITY_CREATE);
        assertEquals(actualNotification.getUser(), user);
        assertTrue(actualNotification instanceof EntityCreateRequest);

        EntityCreateRequest createRequest = (EntityCreateRequest) actualNotification;

        assertEquals(createRequest.getEntities().size(), 2);

        Referenceable actualEntity1 = createRequest.getEntities().get(0);

        assertEquals(actualEntity1.getTypeName(), "sometype");
        assertEquals(((Referenceable)actualEntity1.get("complex")).getTypeName(), "othertype");
        assertEquals(createRequest.getEntities().get(1).getTypeName(), "newtype");
    }

    @Test
    public void testBackwardCompatibility() throws Exception {
        //Code to generate the json, use it for hard-coded json used later in this test
        Referenceable entity = new Referenceable("sometype");
        entity.set("attr", "value");

        EntityCreateRequest request                  = new EntityCreateRequest(null, entity);
        String              notificationJsonFromCode = AtlasType.toV1Json(request);

        System.out.println(notificationJsonFromCode);

        //Json without user and assert that the string can be deserialised
        String notificationJson = "{\n"
                + "  \"entities\": [\n"
                + "    {\n"
                + "      \"jsonClass\": \"org.apache.atlas.typesystem.json.InstanceSerialization$_Reference\",\n"
                + "      \"id\": {\n"
                + "        \"jsonClass\": \"org.apache.atlas.typesystem.json.InstanceSerialization$_Id\",\n"
                + "        \"id\": \"-1459493350903186000\",\n"
                + "        \"version\": 0,\n"
                + "        \"typeName\": \"sometype\",\n"
                + "        \"state\": \"ACTIVE\"\n"
                + "      },\n"
                + "      \"typeName\": \"sometype\",\n"
                + "      \"values\": {\n"
                + "        \"attr\": \"value\"\n"
                + "      },\n"
                + "      \"traitNames\": [],\n"
                + "      \"traits\": {}\n"
                + "    }\n"
                + "  ],\n"
                + "  \"type\": \"ENTITY_CREATE\"\n"
                + "}";


        HookNotification actualNotification = deserializer.deserialize(notificationJson);

        assertEquals(actualNotification.getType(), HookNotificationType.ENTITY_CREATE);
        assertEquals(actualNotification.getUser(), HookNotification.UNKNOW_USER);
    }

    @Test
    public void testEntityCreateV2SerDe() throws Exception {
        AtlasEntity entity1 = new AtlasEntity("sometype");
        AtlasEntity entity2 = new AtlasEntity("newtype");
        AtlasEntity entity3 = new AtlasEntity("othertype");

        setAttributes(entity1);
        entity1.setAttribute("complex", new AtlasObjectId(entity3.getGuid(), entity3.getTypeName()));

        AtlasEntitiesWithExtInfo entities = new AtlasEntitiesWithExtInfo();
        entities.addEntity(entity1);
        entities.addEntity(entity2);
        entities.addReferredEntity(entity3);

        String                user               = "user";
        EntityCreateRequestV2 request            = new EntityCreateRequestV2(user, entities);
        String                notificationJson   = AtlasJson.toJson(request);
        HookNotification      actualNotification = deserializer.deserialize(notificationJson);

        assertEquals(actualNotification.getType(), HookNotificationType.ENTITY_CREATE_V2);
        assertEquals(actualNotification.getUser(), user);

        EntityCreateRequestV2 createRequest = (EntityCreateRequestV2) actualNotification;

        assertEquals(createRequest.getEntities().getEntities().size(), 2);

        AtlasEntity   actualEntity1     = createRequest.getEntities().getEntities().get(0);
        AtlasEntity   actualEntity2     = createRequest.getEntities().getEntities().get(1);
        AtlasEntity   actualEntity3     = createRequest.getEntities().getReferredEntity(entity3.getGuid());
        Map           actualComplexAttr = (Map)actualEntity1.getAttribute("complex");

        assertEquals(actualEntity1.getGuid(), entity1.getGuid());
        assertEquals(actualEntity1.getTypeName(), entity1.getTypeName());
        assertAttributes(actualEntity1);
        assertEquals(actualComplexAttr.get(AtlasObjectId.KEY_GUID), entity3.getGuid());
        assertEquals(actualComplexAttr.get(AtlasObjectId.KEY_TYPENAME), entity3.getTypeName());

        assertEquals(actualEntity2.getGuid(), entity2.getGuid());
        assertEquals(actualEntity2.getTypeName(), entity2.getTypeName());

        assertEquals(actualEntity3.getGuid(), entity3.getGuid());
        assertEquals(actualEntity3.getTypeName(), entity3.getTypeName());
    }

    @Test
    public void testEntityUpdateV2SerDe() throws Exception {
        AtlasEntity entity1 = new AtlasEntity("sometype");
        AtlasEntity entity2 = new AtlasEntity("newtype");
        AtlasEntity entity3 = new AtlasEntity("othertype");

        setAttributes(entity1);
        entity1.setAttribute("complex", new AtlasObjectId(entity3.getGuid(), entity3.getTypeName()));

        AtlasEntitiesWithExtInfo entities = new AtlasEntitiesWithExtInfo();
        entities.addEntity(entity1);
        entities.addEntity(entity2);
        entities.addReferredEntity(entity3);

        String                user               = "user";
        EntityUpdateRequestV2 request            = new EntityUpdateRequestV2(user, entities);
        String                notificationJson   = AtlasJson.toJson(request);
        HookNotification      actualNotification = deserializer.deserialize(notificationJson);

        assertEquals(actualNotification.getType(), HookNotificationType.ENTITY_FULL_UPDATE_V2);
        assertEquals(actualNotification.getUser(), user);

        EntityUpdateRequestV2 updateRequest = (EntityUpdateRequestV2) actualNotification;

        assertEquals(updateRequest.getEntities().getEntities().size(), 2);

        AtlasEntity   actualEntity1     = updateRequest.getEntities().getEntities().get(0);
        AtlasEntity   actualEntity2     = updateRequest.getEntities().getEntities().get(1);
        AtlasEntity   actualEntity3     = updateRequest.getEntities().getReferredEntity(entity3.getGuid());
        Map           actualComplexAttr = (Map)actualEntity1.getAttribute("complex");

        assertEquals(actualEntity1.getGuid(), entity1.getGuid());
        assertEquals(actualEntity1.getTypeName(), entity1.getTypeName());
        assertAttributes(actualEntity1);
        assertEquals(actualComplexAttr.get(AtlasObjectId.KEY_GUID), entity3.getGuid());
        assertEquals(actualComplexAttr.get(AtlasObjectId.KEY_TYPENAME), entity3.getTypeName());

        assertEquals(actualEntity2.getGuid(), entity2.getGuid());
        assertEquals(actualEntity2.getTypeName(), entity2.getTypeName());

        assertEquals(actualEntity3.getGuid(), entity3.getGuid());
        assertEquals(actualEntity3.getTypeName(), entity3.getTypeName());
    }

    @Test
    public void testEntityPartialUpdateV2SerDe() throws Exception {
        AtlasEntity entity1 = new AtlasEntity("sometype");
        AtlasEntity entity2 = new AtlasEntity("newtype");
        AtlasEntity entity3 = new AtlasEntity("othertype");

        setAttributes(entity1);
        entity1.setAttribute("complex", new AtlasObjectId(entity3.getGuid(), entity3.getTypeName()));

        AtlasEntityWithExtInfo entity = new AtlasEntityWithExtInfo(entity1);
        entity.addReferredEntity(entity2);
        entity.addReferredEntity(entity3);

        String                       user               = "user";
        EntityPartialUpdateRequestV2 request            = new EntityPartialUpdateRequestV2(user, AtlasTypeUtil.getAtlasObjectId(entity1), entity);
        String                       notificationJson   = AtlasJson.toJson(request);
        HookNotification             actualNotification = deserializer.deserialize(notificationJson);

        assertEquals(actualNotification.getType(), HookNotificationType.ENTITY_PARTIAL_UPDATE_V2);
        assertEquals(actualNotification.getUser(), user);

        EntityPartialUpdateRequestV2 updateRequest = (EntityPartialUpdateRequestV2) actualNotification;

        assertEquals(updateRequest.getEntity().getReferredEntities().size(), 2);

        AtlasEntity   actualEntity1     = updateRequest.getEntity().getEntity();
        AtlasEntity   actualEntity2     = updateRequest.getEntity().getReferredEntity(entity2.getGuid());
        AtlasEntity   actualEntity3     = updateRequest.getEntity().getReferredEntity(entity3.getGuid());
        Map           actualComplexAttr = (Map)actualEntity1.getAttribute("complex");

        assertEquals(actualEntity1.getGuid(), entity1.getGuid());
        assertEquals(actualEntity1.getTypeName(), entity1.getTypeName());
        assertAttributes(actualEntity1);
        assertEquals(actualComplexAttr.get(AtlasObjectId.KEY_GUID), entity3.getGuid());
        assertEquals(actualComplexAttr.get(AtlasObjectId.KEY_TYPENAME), entity3.getTypeName());

        assertEquals(actualEntity2.getGuid(), entity2.getGuid());
        assertEquals(actualEntity2.getTypeName(), entity2.getTypeName());

        assertEquals(actualEntity3.getGuid(), entity3.getGuid());
        assertEquals(actualEntity3.getTypeName(), entity3.getTypeName());
    }

    @Test
    public void testEntityDeleteV2SerDe() throws Exception {
        AtlasEntity entity1 = new AtlasEntity("sometype");
        AtlasEntity entity2 = new AtlasEntity("newtype");
        AtlasEntity entity3 = new AtlasEntity("othertype");

        List<AtlasObjectId> objectsToDelete = new ArrayList<>();
        objectsToDelete.add(new AtlasObjectId(entity1.getGuid(), entity1.getTypeName()));
        objectsToDelete.add(new AtlasObjectId(entity2.getGuid(), entity2.getTypeName()));
        objectsToDelete.add(new AtlasObjectId(entity3.getGuid(), entity3.getTypeName()));

        String                user               = "user";
        EntityDeleteRequestV2 request            = new EntityDeleteRequestV2(user, objectsToDelete);
        String                notificationJson   = AtlasJson.toJson(request);
        HookNotification      actualNotification = deserializer.deserialize(notificationJson);

        assertEquals(actualNotification.getType(), HookNotificationType.ENTITY_DELETE_V2);
        assertEquals(actualNotification.getUser(), user);

        EntityDeleteRequestV2 deleteRequest = (EntityDeleteRequestV2) actualNotification;

        assertEquals(deleteRequest.getEntities().size(), objectsToDelete.size());
        assertEquals(deleteRequest.getEntities(), objectsToDelete);
    }

    private void setAttributes(AtlasEntity entity) {
        entity.setAttribute("attrStr", ATTR_VALUE_STRING);
        entity.setAttribute("attrInt", ATTR_VALUE_INTEGER);
        entity.setAttribute("attrBool", ATTR_VALUE_BOOLEAN);
        entity.setAttribute("attrDate", ATTR_VALUE_DATE);
    }

    private void assertAttributes(AtlasEntity entity) {
        assertEquals(entity.getAttribute("attrStr"), ATTR_VALUE_STRING);
        assertEquals(entity.getAttribute("attrInt"), ATTR_VALUE_INTEGER);
        assertEquals(entity.getAttribute("attrBool"), ATTR_VALUE_BOOLEAN);
        assertEquals(entity.getAttribute("attrDate"), ATTR_VALUE_DATE.getTime());
    }
}
