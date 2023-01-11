/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.tagsync.source.atlas;

import java.util.Map;
import java.util.HashMap;

import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.model.RangerServiceResource;
import org.apache.ranger.tagsync.source.atlasrest.RangerAtlasEntity;

public class AtlasHiveResourceMapper extends AtlasResourceMapper {
	public static final String ENTITY_TYPE_HIVE_DB     = "hive_db";
	public static final String ENTITY_TYPE_HIVE_TABLE  = "hive_table";
	public static final String ENTITY_TYPE_HIVE_COLUMN = "hive_column";

	public static final String RANGER_TYPE_HIVE_DB     = "database";
	public static final String RANGER_TYPE_HIVE_TABLE  = "table";
	public static final String RANGER_TYPE_HIVE_COLUMN = "column";

	public static final String[] SUPPORTED_ENTITY_TYPES = { ENTITY_TYPE_HIVE_DB, ENTITY_TYPE_HIVE_TABLE, ENTITY_TYPE_HIVE_COLUMN };

	public AtlasHiveResourceMapper() {
		super("hive", SUPPORTED_ENTITY_TYPES);
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
			throwExceptionWithMessage("cluster-name not found in attribute '" +  ENTITY_ATTRIBUTE_QUALIFIED_NAME + "': " + qualifiedName);
		}

		String   entityType  = entity.getTypeName();
		String   entityGuid  = entity.getGuid();
		String   serviceName = getRangerServiceName(clusterName);
		String[] resources   = resourceStr.split(QUALIFIED_NAME_DELIMITER);
		String   dbName      = resources.length > 0 ? resources[0] : null;
		String   tblName     = resources.length > 1 ? resources[1] : null;
		String   colName     = resources.length > 2 ? resources[2] : null;

		Map<String, RangerPolicyResource> elements = new HashMap<String, RangerPolicyResource>();

		if (StringUtils.equals(entityType, ENTITY_TYPE_HIVE_DB)) {
			if (StringUtils.isNotEmpty(dbName)) {
				elements.put(RANGER_TYPE_HIVE_DB, new RangerPolicyResource(dbName));
			}
		} else if (StringUtils.equals(entityType, ENTITY_TYPE_HIVE_TABLE)) {
			if (StringUtils.isNotEmpty(dbName) && StringUtils.isNotEmpty(tblName)) {
				elements.put(RANGER_TYPE_HIVE_DB, new RangerPolicyResource(dbName));
				elements.put(RANGER_TYPE_HIVE_TABLE, new RangerPolicyResource(tblName));
			}
		} else if (StringUtils.equals(entityType, ENTITY_TYPE_HIVE_COLUMN)) {
			if (StringUtils.isNotEmpty(dbName) && StringUtils.isNotEmpty(tblName) && StringUtils.isNotEmpty(colName)) {
				elements.put(RANGER_TYPE_HIVE_DB, new RangerPolicyResource(dbName));
				elements.put(RANGER_TYPE_HIVE_TABLE, new RangerPolicyResource(tblName));
				elements.put(RANGER_TYPE_HIVE_COLUMN, new RangerPolicyResource(colName));
			}
		} else {
			throwExceptionWithMessage("unrecognized entity-type: " + entityType);
		}

		if(elements.isEmpty()) {
			throwExceptionWithMessage("invalid qualifiedName for entity-type '" + entityType + "': " + qualifiedName);
		}

		RangerServiceResource ret = new RangerServiceResource(entityGuid, serviceName, elements);

		return ret;
	}
}
