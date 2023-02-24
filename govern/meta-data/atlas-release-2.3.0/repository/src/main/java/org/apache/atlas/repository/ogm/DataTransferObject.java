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
package org.apache.atlas.repository.ogm;

import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.AtlasBaseModelObject;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;
import org.apache.atlas.type.AtlasEntityType;

import java.util.Map;


public interface DataTransferObject<T extends AtlasBaseModelObject> {
    Class getObjectType();

    AtlasEntityType getEntityType();

    T from(AtlasEntity entity);

    T from(AtlasEntityWithExtInfo entityWithExtInfo);

    AtlasEntity toEntity(T obj) throws AtlasBaseException;

    AtlasEntityWithExtInfo toEntityWithExtInfo(T obj) throws AtlasBaseException;

    Map<String, Object> getUniqueAttributes(T obj);
}
