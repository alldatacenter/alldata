/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.falcon.hook;

import org.apache.atlas.falcon.bridge.FalconBridge;
import org.apache.atlas.falcon.event.FalconEvent;
import org.apache.atlas.falcon.publisher.FalconEventPublisher;
import org.apache.atlas.hook.AtlasHook;
import org.apache.atlas.model.notification.HookNotification;
import org.apache.atlas.v1.model.instance.Referenceable;
import org.apache.atlas.v1.model.notification.HookNotificationV1.EntityCreateRequest;
import org.apache.falcon.FalconException;
import org.apache.falcon.entity.store.ConfigurationStore;
import org.apache.falcon.entity.v0.feed.Feed;
import org.apache.falcon.entity.v0.process.Process;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import static org.apache.atlas.repository.Constants.FALCON_SOURCE;

/**
 * Falcon hook sends lineage information to the Atlas Service.
 */
public class FalconHook extends AtlasHook implements FalconEventPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(FalconHook.class);

    private static ConfigurationStore STORE;

    @Override
    public String getMessageSource() {
        return FALCON_SOURCE;
    }

    private enum Operation {
        ADD,
        UPDATE
    }

    static {
        try {
            STORE = ConfigurationStore.get();
        } catch (Exception e) {
            LOG.error("Caught exception initializing the falcon hook.", e);
        }

        LOG.info("Created Atlas Hook for Falcon");
    }

    @Override
    public void publish(final Data data) {
        final FalconEvent event = data.getEvent();
        try {
            fireAndForget(event);
        } catch (Throwable t) {
            LOG.warn("Error in processing data {}", data, t);
        }
    }

    private void fireAndForget(FalconEvent event) throws FalconException, URISyntaxException {
        LOG.info("Entered Atlas hook for Falcon hook operation {}", event.getOperation());
        List<HookNotification> messages = new ArrayList<>();

        Operation op = getOperation(event.getOperation());
        String user = getUser(event.getUser());
        LOG.info("fireAndForget user:{}", user);
        switch (op) {
        case ADD:
            messages.add(new EntityCreateRequest(user, createEntities(event, user)));
            break;

        }
        notifyEntities(messages, null);
    }

    private List<Referenceable> createEntities(FalconEvent event, String user) throws FalconException, URISyntaxException {
        List<Referenceable> entities = new ArrayList<>();

        switch (event.getOperation()) {
        case ADD_CLUSTER:
            entities.add(FalconBridge
                    .createClusterEntity((org.apache.falcon.entity.v0.cluster.Cluster) event.getEntity()));
            break;

        case ADD_PROCESS:
            entities.addAll(FalconBridge.createProcessEntity((Process) event.getEntity(), STORE));
            break;

        case ADD_FEED:
            entities.addAll(FalconBridge.createFeedCreationEntity((Feed) event.getEntity(), STORE));
            break;

        case UPDATE_CLUSTER:
        case UPDATE_FEED:
        case UPDATE_PROCESS:
        default:
            LOG.info("Falcon operation {} is not valid or supported", event.getOperation());
        }

        return entities;
    }

    private static Operation getOperation(final FalconEvent.OPERATION op) throws FalconException {
        switch (op) {
        case ADD_CLUSTER:
        case ADD_FEED:
        case ADD_PROCESS:
            return Operation.ADD;

        case UPDATE_CLUSTER:
        case UPDATE_FEED:
        case UPDATE_PROCESS:
            return Operation.UPDATE;

        default:
            throw new FalconException("Falcon operation " + op + " is not valid or supported");
        }
    }
}

