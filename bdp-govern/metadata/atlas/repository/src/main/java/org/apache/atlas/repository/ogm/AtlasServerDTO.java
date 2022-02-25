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

import org.apache.atlas.model.impexp.AtlasServer;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AtlasServerDTO extends AbstractDataTransferObject<AtlasServer> {
    private final String PROPERTY_NAME = "name";
    private final String PROPERTY_DISPLAY_NAME = "displayName";
    private final String PROPERTY_FULL_NAME = "fullName";
    private final String PROPERTY_ADDITIONAL_INFO = "additionalInfo";
    private final String PROPERTY_URLS = "urls";

    @Inject
    public AtlasServerDTO(AtlasTypeRegistry typeRegistry) {
        super(typeRegistry, AtlasServer.class, AtlasServer.class.getSimpleName());
    }

    public AtlasServer from(AtlasEntity entity) {
        AtlasServer cluster = new AtlasServer();

        setGuid(cluster, entity);
        cluster.setName((String) entity.getAttribute(PROPERTY_NAME));
        cluster.setFullName((String) entity.getAttribute(PROPERTY_FULL_NAME));
        cluster.setDisplayName((String) entity.getAttribute(PROPERTY_DISPLAY_NAME));
        cluster.setAdditionalInfo((Map<String,String>) entity.getAttribute(PROPERTY_ADDITIONAL_INFO));
        cluster.setUrls((List<String>) entity.getAttribute(PROPERTY_URLS));

        return cluster;
    }

    public AtlasServer from(AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo) {
        return from(entityWithExtInfo.getEntity());
    }

    @Override
    public AtlasEntity toEntity(AtlasServer obj) {
        AtlasEntity entity = getDefaultAtlasEntity(obj);

        entity.setAttribute(PROPERTY_NAME, obj.getName());
        entity.setAttribute(PROPERTY_DISPLAY_NAME, obj.getDisplayName());
        entity.setAttribute(PROPERTY_FULL_NAME, obj.getFullName());
        entity.setAttribute(PROPERTY_ADDITIONAL_INFO, obj.getAdditionalInfo());
        entity.setAttribute(PROPERTY_URLS, obj.getUrls());

        return entity;
    }

    @Override
    public AtlasEntity.AtlasEntityWithExtInfo toEntityWithExtInfo(AtlasServer obj) {
        return new AtlasEntity.AtlasEntityWithExtInfo(toEntity(obj));
    }

    @Override
    public Map<String, Object> getUniqueAttributes(final AtlasServer obj) {
        return new HashMap<String, Object>() {{
            put(PROPERTY_FULL_NAME, obj.getFullName());
        }};
    }
}
