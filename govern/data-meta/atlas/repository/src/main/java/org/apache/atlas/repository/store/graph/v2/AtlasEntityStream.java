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
package org.apache.atlas.repository.store.graph.v2;

import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntitiesWithExtInfo;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;

import java.util.Iterator;
import java.util.List;

public class AtlasEntityStream implements EntityStream {
    protected final AtlasEntitiesWithExtInfo entitiesWithExtInfo;
    protected final EntityStream             entityStream;
    private         Iterator<AtlasEntity>    iterator;


    public AtlasEntityStream(AtlasEntity entity) {
        this(new AtlasEntitiesWithExtInfo(entity), null);
    }

    public AtlasEntityStream(List<AtlasEntity> entities) {
        this(new AtlasEntitiesWithExtInfo(entities), null);
    }

    public AtlasEntityStream(AtlasEntityWithExtInfo entityWithExtInfo) {
        this(new AtlasEntitiesWithExtInfo(entityWithExtInfo), null);
    }

    public AtlasEntityStream(AtlasEntitiesWithExtInfo entitiesWithExtInfo) {
        this(entitiesWithExtInfo, null);
    }

    public AtlasEntityStream(AtlasEntity entity, EntityStream entityStream) {
        this(new AtlasEntitiesWithExtInfo(entity), entityStream);
    }

    public AtlasEntityStream(AtlasEntityWithExtInfo entityWithExtInfo, EntityStream entityStream) {
        this(new AtlasEntitiesWithExtInfo(entityWithExtInfo), entityStream);
    }

    public AtlasEntityStream(AtlasEntitiesWithExtInfo entitiesWithExtInfo, EntityStream entityStream) {
        this.entitiesWithExtInfo = entitiesWithExtInfo;
        this.iterator            = this.entitiesWithExtInfo.getEntities().iterator();
        this.entityStream        = entityStream;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public AtlasEntity next() {
        return iterator.hasNext() ? iterator.next() : null;
    }

    @Override
    public void reset() {
        this.iterator = entitiesWithExtInfo.getEntities().iterator();
    }

    @Override
    public AtlasEntity getByGuid(String guid) {
        return entityStream != null ?  entityStream.getByGuid(guid) : entitiesWithExtInfo.getEntity(guid);
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("AtlasEntityStream{");

        sb.append("entitiesWithExtInfo=").append(entitiesWithExtInfo);
        sb.append(", iterator=").append(iterator);
        sb.append('}');

        return sb.toString();
    }
}
