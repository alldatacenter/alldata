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
package org.apache.atlas.repository.converters;


import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.TypeCategory;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntitiesWithExtInfo;
import org.apache.atlas.type.AtlasType;


public interface AtlasFormatConverter {
    boolean isValidValueV1(Object v1Ob, AtlasType typej);

    Object fromV1ToV2(Object v1Obj, AtlasType type, ConverterContext context) throws AtlasBaseException;

    Object fromV2ToV1(Object v2Obj, AtlasType type, ConverterContext context) throws AtlasBaseException;

    TypeCategory getTypeCategory();

    class ConverterContext {

        private AtlasEntitiesWithExtInfo entities = null;

        public void addEntity(AtlasEntity entity) {
            if (entities == null) {
                entities = new AtlasEntitiesWithExtInfo();
            }
            entities.addEntity(entity);
        }

        public void addReferredEntity(AtlasEntity entity) {
            if (entities == null) {
                entities = new AtlasEntitiesWithExtInfo();
            }
            entities.addReferredEntity(entity);
        }

        public AtlasEntity getById(String guid) {
            if( entities != null) {
                return entities.getEntity(guid);
            }

            return null;
        }

        public boolean entityExists(String guid) { return entities != null && entities.hasEntity(guid); }

        public AtlasEntitiesWithExtInfo getEntities() {
            if (entities != null) {
                entities.compact();
            }

            return entities;
        }
    }
}
