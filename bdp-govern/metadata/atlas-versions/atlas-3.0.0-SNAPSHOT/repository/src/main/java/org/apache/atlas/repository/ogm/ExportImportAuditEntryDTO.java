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
import org.apache.atlas.model.impexp.ExportImportAuditEntry;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import javax.inject.Inject;
import java.util.Map;
import java.util.Set;

@Component
public class ExportImportAuditEntryDTO extends AbstractDataTransferObject<ExportImportAuditEntry> {

    public static final String PROPERTY_USER_NAME              = "userName";
    public static final String PROPERTY_OPERATION              = "operation";
    public static final String PROPERTY_OPERATION_PARAMS       = "operationParams";
    public static final String PROPERTY_START_TIME             = "operationStartTime";
    public static final String PROPERTY_END_TIME               = "operationEndTime";
    public static final String PROPERTY_RESULT_SUMMARY         = "resultSummary";
    public static final String PROPERTY_SOURCE_SERVER_NAME     = "sourceServerName";
    public static final String PROPERTY_TARGET_SERVER_NAME     = "targetServerName";

    private static final Set<String> ATTRIBUTE_NAMES = new HashSet<>(Arrays.asList(PROPERTY_USER_NAME,
            PROPERTY_OPERATION, PROPERTY_OPERATION_PARAMS,
            PROPERTY_START_TIME, PROPERTY_END_TIME,
            PROPERTY_RESULT_SUMMARY,
            PROPERTY_SOURCE_SERVER_NAME, PROPERTY_TARGET_SERVER_NAME));

    @Inject
    public ExportImportAuditEntryDTO(AtlasTypeRegistry typeRegistry) {
        super(typeRegistry, ExportImportAuditEntry.class,
                Constants.INTERNAL_PROPERTY_KEY_PREFIX + ExportImportAuditEntry.class.getSimpleName());
    }

    public static Set<String> getAttributes() {
        return ATTRIBUTE_NAMES;
    }

    public static ExportImportAuditEntry from(String guid, Map<String,Object> attributes) {
        ExportImportAuditEntry entry = new ExportImportAuditEntry();

        entry.setGuid(guid);
        entry.setUserName((String) attributes.get(PROPERTY_USER_NAME));
        entry.setOperation((String) attributes.get(PROPERTY_OPERATION));
        entry.setOperationParams((String) attributes.get(PROPERTY_OPERATION_PARAMS));
        entry.setStartTime((long) attributes.get(PROPERTY_START_TIME));
        entry.setEndTime((long) attributes.get(PROPERTY_END_TIME));
        entry.setSourceServerName((String) attributes.get(PROPERTY_SOURCE_SERVER_NAME));
        entry.setTargetServerName((String) attributes.get(PROPERTY_TARGET_SERVER_NAME));
        entry.setResultSummary((String) attributes.get(PROPERTY_RESULT_SUMMARY));

        return entry;
    }

    @Override
    public ExportImportAuditEntry from(AtlasEntity entity) {
        return from(entity.getGuid(), entity.getAttributes());
    }

    @Override
    public ExportImportAuditEntry from(AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo) {
        return from(entityWithExtInfo.getEntity());
    }

    @Override
    public AtlasEntity toEntity(ExportImportAuditEntry obj) {
        AtlasEntity entity = getDefaultAtlasEntity(obj);

        entity.setAttribute(PROPERTY_USER_NAME, obj.getUserName());
        entity.setAttribute(PROPERTY_OPERATION, obj.getOperation());
        entity.setAttribute(PROPERTY_OPERATION_PARAMS, obj.getOperationParams());
        entity.setAttribute(PROPERTY_START_TIME, obj.getStartTime());
        entity.setAttribute(PROPERTY_END_TIME, obj.getEndTime());
        entity.setAttribute(PROPERTY_SOURCE_SERVER_NAME, obj.getSourceServerName());
        entity.setAttribute(PROPERTY_TARGET_SERVER_NAME, obj.getTargetServerName());
        entity.setAttribute(PROPERTY_RESULT_SUMMARY, obj.getResultSummary());

        return entity;
    }

    @Override
    public AtlasEntity.AtlasEntityWithExtInfo toEntityWithExtInfo(ExportImportAuditEntry obj) throws AtlasBaseException {
        return new AtlasEntity.AtlasEntityWithExtInfo(toEntity(obj));
    }

    @Override
    public Map<String, Object> getUniqueAttributes(final ExportImportAuditEntry obj) {
        return null;
    }
}
