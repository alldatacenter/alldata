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
import org.apache.atlas.model.notification.HookNotification.EntityUpdateRequestV2;
import org.apache.commons.collections.CollectionUtils;
import org.apache.hadoop.hive.metastore.api.Database;
import org.apache.hadoop.hive.metastore.events.AlterDatabaseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class AlterDatabase extends CreateDatabase {
    private static final Logger LOG = LoggerFactory.getLogger(AlterDatabase.class);

    public AlterDatabase(AtlasHiveHookContext context) {
        super(context);
    }

    @Override
    public List<HookNotification> getNotificationMessages() throws Exception {
        List<HookNotification>   ret      = null;
        AtlasEntitiesWithExtInfo entities = context.isMetastoreHook() ? getHiveMetastoreEntities() : getHiveEntities();

        if (entities != null && CollectionUtils.isNotEmpty(entities.getEntities())) {
            ret = Collections.singletonList(new EntityUpdateRequestV2(getUserName(), entities));
        }

        return ret;
    }

    public AtlasEntitiesWithExtInfo getHiveMetastoreEntities() throws Exception {
        AtlasEntitiesWithExtInfo ret     = new AtlasEntitiesWithExtInfo();
        AlterDatabaseEvent       dbEvent = (AlterDatabaseEvent) context.getMetastoreEvent();
        Database                 oldDb   = dbEvent.getOldDatabase();
        Database                 newDb   = dbEvent.getNewDatabase();

        if (newDb != null) {
            AtlasEntity dbEntity = toDbEntity(newDb);

            ret.addEntity(dbEntity);

            addLocationEntities(dbEntity, ret);
        } else {
            LOG.error("AlterDatabase.getEntities(): failed to retrieve db");
        }

        addProcessedEntities(ret);

        return ret;
    }

    public AtlasEntitiesWithExtInfo getHiveEntities() throws Exception {
        return super.getHiveEntities();
    }
}