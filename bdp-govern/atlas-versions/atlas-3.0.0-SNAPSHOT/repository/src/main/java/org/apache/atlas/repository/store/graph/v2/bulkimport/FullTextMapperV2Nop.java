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
package org.apache.atlas.repository.store.graph.v2.bulkimport;

import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.instance.AtlasClassification;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.repository.graph.IFullTextMapper;

import java.util.List;

public class FullTextMapperV2Nop implements IFullTextMapper {
    @Override
    public String getIndexTextForClassifications(String guid, List<AtlasClassification> classifications) throws AtlasBaseException {
        return null;
    }

    @Override
    public String getIndexTextForEntity(String guid) throws AtlasBaseException {
        return null;
    }

    @Override
    public String getClassificationTextForEntity(AtlasEntity entity) throws AtlasBaseException {
        return null;
    }

    @Override
    public AtlasEntity getAndCacheEntity(String guid) throws AtlasBaseException {
        return null;
    }

    @Override
    public AtlasEntity getAndCacheEntity(String guid, boolean includeReferences) throws AtlasBaseException {
        return null;
    }

    @Override
    public AtlasEntity.AtlasEntityWithExtInfo getAndCacheEntityWithExtInfo(String guid) throws AtlasBaseException {
        return null;
    }
}
