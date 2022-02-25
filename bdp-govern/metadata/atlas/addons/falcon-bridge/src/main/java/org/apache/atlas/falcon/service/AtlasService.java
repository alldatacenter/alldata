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

package org.apache.atlas.falcon.service;

import org.apache.atlas.falcon.Util.EventUtil;
import org.apache.atlas.falcon.event.FalconEvent;
import org.apache.atlas.falcon.hook.FalconHook;
import org.apache.atlas.falcon.publisher.FalconEventPublisher;
import org.apache.falcon.FalconException;
import org.apache.falcon.entity.v0.Entity;
import org.apache.falcon.entity.v0.EntityType;
import org.apache.falcon.service.ConfigurationChangeListener;
import org.apache.falcon.service.FalconService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Atlas service to publish Falcon events
 */
public class AtlasService implements FalconService, ConfigurationChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(AtlasService.class);
    private FalconEventPublisher publisher;

    /**
     * Constant for the service name.
     */
    public static final String SERVICE_NAME = AtlasService.class.getSimpleName();

    @Override
    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public void init() throws FalconException {
        publisher = new FalconHook();
    }

    @Override
    public void destroy() throws FalconException {
    }

    @Override
    public void onAdd(Entity entity) throws FalconException {
        try {
            EntityType entityType = entity.getEntityType();
            switch (entityType) {
            case CLUSTER:
                addEntity(entity, FalconEvent.OPERATION.ADD_CLUSTER);
                break;

            case PROCESS:
                addEntity(entity, FalconEvent.OPERATION.ADD_PROCESS);
                break;

            case FEED:
                addEntity(entity, FalconEvent.OPERATION.ADD_FEED);
                break;

            default:
                LOG.debug("Entity type not processed {}", entityType);
            }
        } catch(Throwable t) {
            LOG.warn("Error handling entity {}", entity, t);
        }
    }

    @Override
    public void onRemove(Entity entity) throws FalconException {
    }

    @Override
    public void onChange(Entity oldEntity, Entity newEntity) throws FalconException {
        /**
         * Skipping update for now - update uses full update currently and this might result in all attributes wiped for hive entities
        EntityType entityType = newEntity.getEntityType();
        switch (entityType) {
        case CLUSTER:
            addEntity(newEntity, FalconEvent.OPERATION.UPDATE_CLUSTER);
            break;

        case PROCESS:
            addEntity(newEntity, FalconEvent.OPERATION.UPDATE_PROCESS);
            break;

        case FEED:
            FalconEvent.OPERATION operation = isReplicationFeed((Feed) newEntity) ?
                    FalconEvent.OPERATION.UPDATE_REPLICATION_FEED :
                    FalconEvent.OPERATION.UPDATE_FEED;
            addEntity(newEntity, operation);
            break;

        default:
            LOG.debug("Entity type not processed {}", entityType);
        }
         **/
    }

    @Override
    public void onReload(Entity entity) throws FalconException {
        //Since there is no import script that can import existing falcon entities to atlas, adding on falcon service start
        onAdd(entity);
    }

    private void addEntity(Entity entity, FalconEvent.OPERATION operation) throws FalconException {
        LOG.info("Adding {} entity to Atlas: {}", entity.getEntityType().name(), entity.getName());

        try {
            FalconEvent event =
                    new FalconEvent(EventUtil.getUser(), operation, entity);
            FalconEventPublisher.Data data = new FalconEventPublisher.Data(event);
            publisher.publish(data);
        } catch (Exception ex) {
            throw new FalconException("Unable to publish data to publisher " + ex.getMessage(), ex);
        }
    }
}
