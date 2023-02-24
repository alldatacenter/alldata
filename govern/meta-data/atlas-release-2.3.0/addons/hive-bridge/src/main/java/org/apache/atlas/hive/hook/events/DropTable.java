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
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.model.notification.HookNotification;
import org.apache.atlas.model.notification.HookNotification.EntityDeleteRequestV2;
import org.apache.commons.collections.CollectionUtils;
import org.apache.hadoop.hive.metastore.events.DropTableEvent;
import org.apache.hadoop.hive.ql.hooks.Entity;
import org.apache.hadoop.hive.ql.metadata.Table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DropTable extends BaseHiveEvent {
    public DropTable(AtlasHiveHookContext context) {
        super(context);
    }

    @Override
    public List<HookNotification> getNotificationMessages() {
        List<HookNotification> ret      = null;
        List<AtlasObjectId>    entities = context.isMetastoreHook() ? getHiveMetastoreEntities() : getHiveEntities();

        if (CollectionUtils.isNotEmpty(entities)) {
            ret = new ArrayList<>(entities.size());

            for (AtlasObjectId entity : entities) {
                ret.add(new EntityDeleteRequestV2(getUserName(), Collections.singletonList(entity)));
            }
        }

        return ret;
    }

    public List<AtlasObjectId> getHiveMetastoreEntities() {
        List<AtlasObjectId> ret      = new ArrayList<>();
        DropTableEvent      tblEvent = (DropTableEvent) context.getMetastoreEvent();
        Table               table    = new Table(tblEvent.getTable());
        String              tblQName = getQualifiedName(table);
        AtlasObjectId       tblId    = new AtlasObjectId(HIVE_TYPE_TABLE, ATTRIBUTE_QUALIFIED_NAME, tblQName);

        context.removeFromKnownTable(tblQName);

        ret.add(tblId);

        return ret;
    }

    public List<AtlasObjectId> getHiveEntities() {
        List<AtlasObjectId> ret = new ArrayList<>();

        for (Entity entity : getOutputs()) {
            if (entity.getType() == Entity.Type.TABLE) {
                String        tblQName = getQualifiedName(entity.getTable());
                AtlasObjectId tblId    = new AtlasObjectId(HIVE_TYPE_TABLE, ATTRIBUTE_QUALIFIED_NAME, tblQName);

                context.removeFromKnownTable(tblQName);

                ret.add(tblId);
            }
        }

        return ret;
    }
}