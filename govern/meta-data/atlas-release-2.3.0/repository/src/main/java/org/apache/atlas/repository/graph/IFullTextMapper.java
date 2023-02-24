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
package org.apache.atlas.repository.graph;

import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.instance.AtlasClassification;
import org.apache.atlas.model.instance.AtlasEntity;

import java.util.List;

public interface IFullTextMapper {
    /**
     * Map newly associated/defined classifications for the entity with given GUID
     * @param guid Entity guid
     * @param classifications new classifications added to the entity
     * @return Full text string ONLY for the added classifications
     * @throws AtlasBaseException
     */
    String getIndexTextForClassifications(String guid, List<AtlasClassification> classifications) throws AtlasBaseException;

    String getIndexTextForEntity(String guid) throws AtlasBaseException;

    String getClassificationTextForEntity(AtlasEntity entity) throws AtlasBaseException;

    AtlasEntity getAndCacheEntity(String guid) throws AtlasBaseException;

    AtlasEntity  getAndCacheEntity(String guid, boolean includeReferences) throws AtlasBaseException;

    AtlasEntity.AtlasEntityWithExtInfo getAndCacheEntityWithExtInfo(String guid) throws AtlasBaseException;
}
