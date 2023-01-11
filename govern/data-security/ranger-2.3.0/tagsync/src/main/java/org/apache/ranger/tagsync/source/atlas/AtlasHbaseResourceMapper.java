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

public class AtlasHbaseResourceMapper extends AtlasResourceMapper {
	public static final String ENTITY_TYPE_HBASE_NAMESPACE      = "hbase_namespace";
	public static final String ENTITY_TYPE_HBASE_TABLE          = "hbase_table";
	public static final String ENTITY_TYPE_HBASE_COLUMN_FAMILY  = "hbase_column_family";
	public static final String ENTITY_TYPE_HBASE_COLUMN         = "hbase_column";

	public static final String RANGER_TYPE_HBASE_TABLE          = "table";
	public static final String RANGER_TYPE_HBASE_COLUMN_FAMILY  = "column-family";
	public static final String RANGER_TYPE_HBASE_COLUMN         = "column";

	public static final String RANGER_NAMESPACE_TABLE_DELIMITER = ":";

	public static final String[] SUPPORTED_ENTITY_TYPES = { ENTITY_TYPE_HBASE_TABLE, ENTITY_TYPE_HBASE_COLUMN_FAMILY, ENTITY_TYPE_HBASE_COLUMN };

	public AtlasHbaseResourceMapper() {
		super("hbase", SUPPORTED_ENTITY_TYPES);
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

		String entityType  = entity.getTypeName();
		String entityGuid  = entity.getGuid();
		String serviceName = getRangerServiceName(clusterName);

		Map<String, RangerPolicyResource> elements = new HashMap<String, RangerPolicyResource>();

		if (StringUtils.equals(entityType, ENTITY_TYPE_HBASE_NAMESPACE)) {
			if (StringUtils.isNotEmpty(resourceStr)) {
				String namespaceName = StringUtils.strip(resourceStr);
				if (StringUtils.isNotEmpty(namespaceName)) {
					elements.put(RANGER_TYPE_HBASE_TABLE, new RangerPolicyResource(namespaceName + RANGER_NAMESPACE_TABLE_DELIMITER + "*"));
				}
			}
		} else if (StringUtils.equals(entityType, ENTITY_TYPE_HBASE_TABLE)) {
			if (StringUtils.isNotEmpty(resourceStr)) {
				elements.put(RANGER_TYPE_HBASE_TABLE, new RangerPolicyResource(resourceStr));
			}
		} else if (StringUtils.equals(entityType, ENTITY_TYPE_HBASE_COLUMN_FAMILY)) {
			String[] resources  = resourceStr.split(QUALIFIED_NAME_DELIMITER);
			String   tblName    = null;
			String   familyName = null;

			if (resources.length == 2) {
				tblName    = resources[0];
				familyName = resources[1];
			} else if (resources.length > 2) {
				StringBuilder tblNameBuf = new StringBuilder(resources[0]);

				for (int i = 1; i < resources.length - 1; i++) {
					tblNameBuf.append(QUALIFIED_NAME_DELIMITER_CHAR).append(resources[i]);
				}

				tblName = tblNameBuf.toString();
				familyName = resources[resources.length - 1];
			}

			if (StringUtils.isNotEmpty(tblName) && StringUtils.isNotEmpty(familyName)) {
				elements.put(RANGER_TYPE_HBASE_TABLE, new RangerPolicyResource(tblName));
				elements.put(RANGER_TYPE_HBASE_COLUMN_FAMILY, new RangerPolicyResource(familyName));
			}
		} else if (StringUtils.equals(entityType, ENTITY_TYPE_HBASE_COLUMN)) {
			String[] resources  = resourceStr.split(QUALIFIED_NAME_DELIMITER);
			String   tblName    = null;
			String   familyName = null;
			String   colName    = null;

			if (resources.length == 3) {
				tblName    = resources[0];
				familyName = resources[1];
				colName    = resources[2];
			} else if (resources.length > 3) {
				StringBuilder tblNameBuf = new StringBuilder(resources[0]);

				for (int i = 1; i < resources.length - 2; i++) {
					tblNameBuf.append(QUALIFIED_NAME_DELIMITER_CHAR).append(resources[i]);
				}

				tblName    = tblNameBuf.toString();
				familyName = resources[resources.length - 2];
				colName    = resources[resources.length - 1];
			}

			if (StringUtils.isNotEmpty(tblName) && StringUtils.isNotEmpty(familyName) && StringUtils.isNotEmpty(colName)) {
				elements.put(RANGER_TYPE_HBASE_TABLE, new RangerPolicyResource(tblName));
				elements.put(RANGER_TYPE_HBASE_COLUMN_FAMILY, new RangerPolicyResource(familyName));
				elements.put(RANGER_TYPE_HBASE_COLUMN, new RangerPolicyResource(colName));
			}
		} else {
			throwExceptionWithMessage("unrecognized entity-type: " + entityType);
		}

		if(elements.isEmpty()) {
			throwExceptionWithMessage("invalid qualifiedName for entity-type '" + entityType + "': " + qualifiedName);
		}

		return new RangerServiceResource(entityGuid, serviceName, elements);

	}
}
