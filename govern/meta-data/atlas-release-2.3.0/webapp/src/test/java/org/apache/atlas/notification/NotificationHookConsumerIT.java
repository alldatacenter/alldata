/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.notification;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.atlas.EntityAuditEvent;
import org.apache.atlas.model.notification.HookNotification;
import org.apache.atlas.v1.model.instance.Id;
import org.apache.atlas.v1.model.instance.Referenceable;
import org.apache.atlas.v1.model.notification.HookNotificationV1.EntityDeleteRequest;
import org.apache.atlas.v1.model.notification.HookNotificationV1.EntityPartialUpdateRequest;
import org.apache.atlas.v1.model.notification.HookNotificationV1.EntityCreateRequest;
import org.apache.atlas.v1.model.notification.HookNotificationV1.EntityUpdateRequest;
import org.apache.atlas.web.integration.BaseResourceIT;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

import static java.lang.Thread.sleep;
import static org.testng.Assert.assertEquals;

public class NotificationHookConsumerIT extends BaseResourceIT {
    private static final String TEST_USER = "testuser";

    public static final String NAME           = "name";
    public static final String DESCRIPTION    = "description";
    public static final String QUALIFIED_NAME = "qualifiedName";
    public static final String CLUSTER_NAME   = "clusterName";

    @BeforeClass
    public void setUp() throws Exception {
        super.setUp();

        initNotificationService();

        createTypeDefinitionsV1();
    }

    @AfterClass
    public void teardown() throws Exception {
        cleanUpNotificationService();
    }

    private void sendHookMessage(HookNotification message) throws NotificationException, InterruptedException {
        notificationInterface.send(NotificationInterface.NotificationType.HOOK, message);

        sleep(1000);
    }

    @Test
    public void testMessageHandleFailureConsumerContinues() throws Exception {
        //send invalid message - update with invalid type
        sendHookMessage(new EntityPartialUpdateRequest(TEST_USER, randomString(), null, null, new Referenceable(randomString())));

        //send valid message
        final Referenceable entity = new Referenceable(DATABASE_TYPE_BUILTIN);
        final String        dbName = "db" + randomString();

        entity.set(NAME, dbName);
        entity.set(DESCRIPTION, randomString());
        entity.set(QUALIFIED_NAME, dbName);
        entity.set(CLUSTER_NAME, randomString());

        sendHookMessage(new EntityCreateRequest(TEST_USER, entity));

        waitFor(MAX_WAIT_TIME, new Predicate() {
            @Override
            public boolean evaluate() throws Exception {
                ArrayNode results = searchByDSL(String.format("%s where name='%s'", DATABASE_TYPE_BUILTIN, entity.get(NAME)));

                return results.size() == 1;
            }
        });
    }

    @Test
    public void testCreateEntity() throws Exception {
        final Referenceable entity        = new Referenceable(DATABASE_TYPE_BUILTIN);
        final String        dbName        = "db" + randomString();
        final String        clusterName   = randomString();
        final String        qualifiedName = dbName + "@" + clusterName;

        entity.set(NAME, dbName);
        entity.set(DESCRIPTION, randomString());
        entity.set(QUALIFIED_NAME, qualifiedName);
        entity.set(CLUSTER_NAME, clusterName);

        sendHookMessage(new EntityCreateRequest(TEST_USER, entity));

        waitFor(MAX_WAIT_TIME, new Predicate() {
            @Override
            public boolean evaluate() throws Exception {
                ArrayNode results = searchByDSL(String.format("%s where qualifiedName='%s'", DATABASE_TYPE_BUILTIN, entity.get(QUALIFIED_NAME)));

                return results.size() == 1;
            }
        });

        //Assert that user passed in hook message is used in audit
        Referenceable          instance = atlasClientV1.getEntity(DATABASE_TYPE_BUILTIN, QUALIFIED_NAME, (String) entity.get(QUALIFIED_NAME));
        List<EntityAuditEvent> events   = atlasClientV1.getEntityAuditEvents(instance.getId()._getId(), (short) 1);

        assertEquals(events.size(), 1);
        assertEquals(events.get(0).getUser(), TEST_USER);
    }

    @Test
    public void testUpdateEntityPartial() throws Exception {
        final Referenceable entity        = new Referenceable(DATABASE_TYPE_BUILTIN);
        final String        dbName        = "db" + randomString();
        final String        clusterName   = randomString();
        final String        qualifiedName = dbName + "@" + clusterName;

        entity.set(NAME, dbName);
        entity.set(DESCRIPTION, randomString());
        entity.set(QUALIFIED_NAME, qualifiedName);
        entity.set(CLUSTER_NAME, clusterName);

        atlasClientV1.createEntity(entity);

        final Referenceable newEntity = new Referenceable(DATABASE_TYPE_BUILTIN);

        newEntity.set("owner", randomString());

        sendHookMessage(new EntityPartialUpdateRequest(TEST_USER, DATABASE_TYPE_BUILTIN, QUALIFIED_NAME, (String) entity.get(QUALIFIED_NAME), newEntity));

        waitFor(MAX_WAIT_TIME, new Predicate() {
            @Override
            public boolean evaluate() throws Exception {
                Referenceable localEntity = atlasClientV1.getEntity(DATABASE_TYPE_BUILTIN, QUALIFIED_NAME, qualifiedName);

                return (localEntity.get("owner") != null && localEntity.get("owner").equals(newEntity.get("owner")));
            }
        });

        //Its partial update and un-set fields are not updated
        Referenceable actualEntity = atlasClientV1.getEntity(DATABASE_TYPE_BUILTIN, QUALIFIED_NAME, (String) entity.get(QUALIFIED_NAME));

        assertEquals(actualEntity.get(DESCRIPTION), entity.get(DESCRIPTION));
    }

    @Test
    public void testUpdatePartialUpdatingQualifiedName() throws Exception {
        final Referenceable entity        = new Referenceable(DATABASE_TYPE_BUILTIN);
        final String        dbName        = "db" + randomString();
        final String        clusterName   = randomString();
        final String        qualifiedName = dbName + "@" + clusterName;

        entity.set(NAME, dbName);
        entity.set(DESCRIPTION, randomString());
        entity.set(QUALIFIED_NAME, qualifiedName);
        entity.set(CLUSTER_NAME, clusterName);

        atlasClientV1.createEntity(entity);

        final Referenceable newEntity        = new Referenceable(DATABASE_TYPE_BUILTIN);
        final String        newName          = "db" + randomString();
        final String        newQualifiedName = newName + "@" + clusterName;

        newEntity.set(QUALIFIED_NAME, newQualifiedName);

        sendHookMessage(new EntityPartialUpdateRequest(TEST_USER, DATABASE_TYPE_BUILTIN, QUALIFIED_NAME, qualifiedName, newEntity));

        waitFor(MAX_WAIT_TIME, new Predicate() {
            @Override
            public boolean evaluate() throws Exception {
                ArrayNode results = searchByDSL(String.format("%s where qualifiedName='%s'", DATABASE_TYPE_BUILTIN, newQualifiedName));

                return results.size() == 1;
            }
        });

        //no entity with the old qualified name
        ArrayNode results = searchByDSL(String.format("%s where qualifiedName='%s'", DATABASE_TYPE_BUILTIN, qualifiedName));

        assertEquals(results.size(), 0);
    }

    @Test
    public void testDeleteByQualifiedName() throws Exception {
        final Referenceable entity        = new Referenceable(DATABASE_TYPE_BUILTIN);
        final String        dbName        = "db" + randomString();
        final String        clusterName   = randomString();
        final String        qualifiedName = dbName + "@" + clusterName;

        entity.set(NAME, dbName);
        entity.set(DESCRIPTION, randomString());
        entity.set(QUALIFIED_NAME, qualifiedName);
        entity.set(CLUSTER_NAME, clusterName);

        final String dbId = atlasClientV1.createEntity(entity).get(0);

        sendHookMessage(new EntityDeleteRequest(TEST_USER, DATABASE_TYPE_BUILTIN, QUALIFIED_NAME, qualifiedName));

        waitFor(MAX_WAIT_TIME, new Predicate() {
            @Override
            public boolean evaluate() throws Exception {
                Referenceable getEntity = atlasClientV1.getEntity(dbId);

                return getEntity.getId().getState() == Id.EntityState.DELETED;
            }
        });
    }

    @Test
    public void testUpdateEntityFullUpdate() throws Exception {
        final Referenceable entity        = new Referenceable(DATABASE_TYPE_BUILTIN);
        final String        dbName        = "db" + randomString();
        final String        clusterName   = randomString();
        final String        qualifiedName = dbName + "@" + clusterName;

        entity.set(NAME, dbName);
        entity.set(DESCRIPTION, randomString());
        entity.set(QUALIFIED_NAME, qualifiedName);
        entity.set(CLUSTER_NAME, clusterName);

        atlasClientV1.createEntity(entity);

        final Referenceable newEntity = new Referenceable(DATABASE_TYPE_BUILTIN);

        newEntity.set(NAME, dbName);
        newEntity.set(DESCRIPTION, randomString());
        newEntity.set("owner", randomString());
        newEntity.set(QUALIFIED_NAME, qualifiedName);
        newEntity.set(CLUSTER_NAME, clusterName);

        //updating unique attribute
        sendHookMessage(new EntityUpdateRequest(TEST_USER, newEntity));

        waitFor(MAX_WAIT_TIME, new Predicate() {
            @Override
            public boolean evaluate() throws Exception {
                ArrayNode results = searchByDSL(String.format("%s where qualifiedName='%s'", DATABASE_TYPE_BUILTIN, newEntity.get(QUALIFIED_NAME)));

                return results.size() == 1;
            }
        });

        Referenceable actualEntity = atlasClientV1.getEntity(DATABASE_TYPE_BUILTIN, QUALIFIED_NAME, qualifiedName);

        assertEquals(actualEntity.get(DESCRIPTION), newEntity.get(DESCRIPTION));
        assertEquals(actualEntity.get("owner"), newEntity.get("owner"));
    }
}
