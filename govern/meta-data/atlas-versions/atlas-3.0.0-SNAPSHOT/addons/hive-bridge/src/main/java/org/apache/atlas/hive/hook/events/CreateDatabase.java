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

package org.apache.atlas.hive.hook.events;

import org.apache.atlas.hive.hook.AtlasHiveHookContext;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntitiesWithExtInfo;
import org.apache.atlas.model.notification.HookNotification;
import org.apache.atlas.model.notification.HookNotification.EntityCreateRequestV2;
import org.apache.commons.collections.CollectionUtils;
import org.apache.hadoop.hive.metastore.api.Database;
import org.apache.hadoop.hive.metastore.events.CreateDatabaseEvent;
import org.apache.hadoop.hive.ql.hooks.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

import static org.apache.atlas.hive.bridge.HiveMetaStoreBridge.getDatabaseName;
import static org.apache.hadoop.hive.ql.hooks.Entity.Type.DATABASE;

public class CreateDatabase extends BaseHiveEvent {
    private static final Logger LOG = LoggerFactory.getLogger(CreateDatabase.class);

    public CreateDatabase(AtlasHiveHookContext context) {
        super(context);
    }

    @Override
    public List<HookNotification> getNotificationMessages() throws Exception {
        List<HookNotification>   ret      = null;
        AtlasEntitiesWithExtInfo entities = context.isMetastoreHook() ? getHiveMetastoreEntities() : getHiveEntities();

        if (entities != null && CollectionUtils.isNotEmpty(entities.getEntities())) {
            ret = Collections.singletonList(new EntityCreateRequestV2(getUserName(), entities));
        }

        return ret;
    }

    public AtlasEntitiesWithExtInfo getHiveMetastoreEntities() throws Exception {
        AtlasEntitiesWithExtInfo ret     = new AtlasEntitiesWithExtInfo();
        CreateDatabaseEvent      dbEvent = (CreateDatabaseEvent) context.getMetastoreEvent();
        Database                 db      = dbEvent.getDatabase();

        if (db != null) {
            db = context.getMetastoreHandler().get_database(db.getName());
        }

        if (db != null) {
            AtlasEntity dbEntity = toDbEntity(db);

            ret.addEntity(dbEntity);

            addLocationEntities(dbEntity, ret);
        } else {
            LOG.error("CreateDatabase.getEntities(): failed to retrieve db");
        }

        addProcessedEntities(ret);

        return ret;
    }

    public AtlasEntitiesWithExtInfo getHiveEntities() throws Exception {
        AtlasEntitiesWithExtInfo ret = new AtlasEntitiesWithExtInfo();

        for (Entity entity : getOutputs()) {
            if (entity.getType() == DATABASE) {
                Database db = entity.getDatabase();

                if (db != null) {
                    db = getHive().getDatabase(getDatabaseName(db));
                }

                if (db != null) {
                    AtlasEntity dbEntity    = toDbEntity(db);
                    AtlasEntity dbDDLEntity = createHiveDDLEntity(dbEntity);

                    ret.addEntity(dbEntity);

                    if (dbDDLEntity != null) {
                        ret.addEntity(dbDDLEntity);
                    }

                    addLocationEntities(dbEntity, ret);
                } else {
                    LOG.error("CreateDatabase.getEntities(): failed to retrieve db");
                }
            }
        }

        addProcessedEntities(ret);

        return ret;
    }

    public void addLocationEntities(AtlasEntity dbEntity, AtlasEntitiesWithExtInfo ret) {
        AtlasEntity dbLocationEntity = createHiveLocationEntity(dbEntity, ret);

        if (dbLocationEntity != null) {
            ret.addEntity(dbLocationEntity);
        }
    }
}