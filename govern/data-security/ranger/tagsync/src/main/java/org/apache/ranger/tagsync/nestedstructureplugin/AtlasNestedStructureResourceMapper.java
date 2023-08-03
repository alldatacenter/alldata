/**
* Copyright 2022 Comcast Cable Communications Management, LLC
*
* Licensed under the Apache License, Version 2.0 (the ""License"");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an ""AS IS"" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or   implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
* SPDX-License-Identifier: Apache-2.0
*/
package org.apache.ranger.tagsync.nestedstructureplugin;

import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.model.RangerServiceResource;
import org.apache.ranger.tagsync.source.atlas.AtlasResourceMapper;
import org.apache.ranger.tagsync.source.atlasrest.RangerAtlasEntity;

import java.util.HashMap;
import java.util.Map;

/** Syncing tag-metadata (resource) relationships from Apache Atlas to Ranger
 **/

public class AtlasNestedStructureResourceMapper extends AtlasResourceMapper {
    public static final String RANGER_SERVICETYPE                 = "nestedstructure";
    public static final String ENTITY_TYPE_NESTEDSTRUCTURE_SCHEMA = "json_object";
    public static final String ENTITY_TYPE_NESTEDSTRUCTURE_FIELD  = "json_field";
    public static final String RANGER_TYPE_NESTEDSTRUCTURE_SCHEMA = "schema";
    public static final String RANGER_TYPE_NESTEDSTRUCTURE_FIELD  = "field";
    public static final String QUALIFIED_NAME_DELIMITER           = "#";

    public static final String[] SUPPORTED_ENTITY_TYPES = { ENTITY_TYPE_NESTEDSTRUCTURE_SCHEMA, ENTITY_TYPE_NESTEDSTRUCTURE_FIELD };

    public AtlasNestedStructureResourceMapper() {
        super(RANGER_SERVICETYPE, SUPPORTED_ENTITY_TYPES);
    }

    @Override
    public RangerServiceResource buildResource(final RangerAtlasEntity entity) throws Exception {
        String qualifiedName = (String)entity.getAttributes().get(AtlasResourceMapper.ENTITY_ATTRIBUTE_QUALIFIED_NAME);

        if (StringUtils.isEmpty(qualifiedName)) {
            throw new Exception("attribute '" +  ENTITY_ATTRIBUTE_QUALIFIED_NAME + "' not found in entity");
        }

        String resourceStr = getResourceNameFromQualifiedName(qualifiedName);

        if (StringUtils.isEmpty(resourceStr)) {
            throwExceptionWithMessage("resource not found in attribute '" +  ENTITY_ATTRIBUTE_QUALIFIED_NAME + "': " + qualifiedName);
        }

        String clusterName = getClusterNameFromQualifiedName(qualifiedName);
        if (StringUtils.isEmpty(clusterName)) {
            clusterName = defaultClusterName;
        }

        String   entityType  = entity.getTypeName();
        String   entityGuid  = entity.getGuid();
        String   serviceName = getRangerServiceName(clusterName);
        String[] resources   = resourceStr.split(QUALIFIED_NAME_DELIMITER);
        String   schemaName  = resources.length > 0 ? resources[0] : null;
        String   fieldName   = resources.length > 1 ? resources[1] : null;

        Map<String, RangerPolicyResource> elements = new HashMap<>();

        if (StringUtils.equals(entityType, ENTITY_TYPE_NESTEDSTRUCTURE_SCHEMA)) {
            if (StringUtils.isNotEmpty(schemaName)) {
                elements.put(RANGER_TYPE_NESTEDSTRUCTURE_SCHEMA , new RangerPolicyResource(schemaName));
            }
        } else if (StringUtils.equals(entityType, ENTITY_TYPE_NESTEDSTRUCTURE_FIELD)) {
            if (StringUtils.isNotEmpty(schemaName) && StringUtils.isNotEmpty(fieldName)) {
                elements.put(RANGER_TYPE_NESTEDSTRUCTURE_SCHEMA, new RangerPolicyResource(schemaName));
                elements.put(RANGER_TYPE_NESTEDSTRUCTURE_FIELD, new RangerPolicyResource(fieldName));
            }
        } else {
            throwExceptionWithMessage("unrecognized entity-type: " + entityType);
        }

        if (elements.isEmpty()) {
            throwExceptionWithMessage("invalid qualifiedName for entity-type '" + entityType + "': " + qualifiedName);
        }

        RangerServiceResource ret = new RangerServiceResource(entityGuid, serviceName, elements);

        return ret;
    }
}
