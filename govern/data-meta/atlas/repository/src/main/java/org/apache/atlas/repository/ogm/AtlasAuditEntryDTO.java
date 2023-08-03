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

package org.apache.atlas.repository.ogm;

import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.audit.AtlasAuditEntry;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Date;
import java.util.Arrays;

@Component
public class AtlasAuditEntryDTO extends AbstractDataTransferObject<AtlasAuditEntry> {

    public static final String ATTRIBUTE_USER_NAME    = "userName";
    public static final String ATTRIBUTE_OPERATION    = "operation";
    public static final String ATTRIBUTE_PARAMS       = "params";
    public static final String ATTRIBUTE_START_TIME   = "startTime";
    public static final String ATTRIBUTE_END_TIME     = "endTime";
    public static final String ATTRIBUTE_CLIENT_ID    = "clientId";
    public static final String ATTRIBUTE_RESULT       = "result";
    public static final String ATTRIBUTE_RESULT_COUNT = "resultCount";

    private static final Set<String> ATTRIBUTE_NAMES = new HashSet<>(Arrays.asList(ATTRIBUTE_USER_NAME,
            ATTRIBUTE_OPERATION, ATTRIBUTE_PARAMS,
            ATTRIBUTE_START_TIME, ATTRIBUTE_END_TIME,
            ATTRIBUTE_CLIENT_ID, ATTRIBUTE_RESULT, ATTRIBUTE_RESULT_COUNT));

    @Inject
    public AtlasAuditEntryDTO(AtlasTypeRegistry typeRegistry) {
        super(typeRegistry, AtlasAuditEntry.class,
                Constants.INTERNAL_PROPERTY_KEY_PREFIX + AtlasAuditEntry.class.getSimpleName());
    }

    public static Set<String> getAttributes() {
        return ATTRIBUTE_NAMES;
    }

    public static AtlasAuditEntry from(String guid, Map<String,Object> attributes) {
        AtlasAuditEntry entry = new AtlasAuditEntry();

        entry.setGuid(guid);
        entry.setUserName((String) attributes.get(ATTRIBUTE_USER_NAME));
        entry.setOperation(AtlasAuditEntry.AuditOperation.valueOf((String)attributes.get(ATTRIBUTE_OPERATION)));
        entry.setParams((String) attributes.get(ATTRIBUTE_PARAMS));
        entry.setStartTime((Date) attributes.get(ATTRIBUTE_START_TIME));
        entry.setEndTime((Date) attributes.get(ATTRIBUTE_END_TIME));
        entry.setClientId((String) attributes.get(ATTRIBUTE_CLIENT_ID));
        entry.setResult((String) attributes.get(ATTRIBUTE_RESULT));
        entry.setResultCount((long) attributes.get(ATTRIBUTE_RESULT_COUNT));

        return entry;
    }

    @Override
    public AtlasAuditEntry from(AtlasEntity entity) {
        return from(entity.getGuid(), entity.getAttributes());
    }

    @Override
    public AtlasAuditEntry from(AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo) {
        return from(entityWithExtInfo.getEntity());
    }

    @Override
    public AtlasEntity toEntity(AtlasAuditEntry obj) {
        AtlasEntity entity = getDefaultAtlasEntity(obj);

        entity.setAttribute(ATTRIBUTE_USER_NAME, obj.getUserName());
        entity.setAttribute(ATTRIBUTE_OPERATION, obj.getOperation());
        entity.setAttribute(ATTRIBUTE_PARAMS, obj.getParams());
        entity.setAttribute(ATTRIBUTE_START_TIME, obj.getStartTime());
        entity.setAttribute(ATTRIBUTE_END_TIME, obj.getEndTime());
        entity.setAttribute(ATTRIBUTE_CLIENT_ID, obj.getClientId());
        entity.setAttribute(ATTRIBUTE_RESULT, obj.getResult());
        entity.setAttribute(ATTRIBUTE_RESULT_COUNT, obj.getResultCount());

        return entity;
    }

    @Override
    public AtlasEntity.AtlasEntityWithExtInfo toEntityWithExtInfo(AtlasAuditEntry obj) throws AtlasBaseException {
        return new AtlasEntity.AtlasEntityWithExtInfo(toEntity(obj));
    }

    @Override
    public Map<String, Object> getUniqueAttributes(final AtlasAuditEntry obj) {
        return null;
    }
}
