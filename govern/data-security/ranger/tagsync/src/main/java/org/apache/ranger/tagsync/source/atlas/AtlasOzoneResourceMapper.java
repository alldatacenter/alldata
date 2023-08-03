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

import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.model.RangerServiceResource;
import org.apache.ranger.tagsync.source.atlasrest.RangerAtlasEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AtlasOzoneResourceMapper extends AtlasResourceMapper {
	private static final Logger LOG = LoggerFactory.getLogger(AtlasOzoneResourceMapper.class);

	public static final String ENTITY_TYPE_OZONE_VOLUME  = "ozone_volume";
	public static final String ENTITY_TYPE_OZONE_BUCKET  = "ozone_bucket";
	public static final String ENTITY_TYPE_OZONE_KEY 	 = "ozone_key";

	public static final String RANGER_TYPE_OZONE_VOLUME  = "volume";
	public static final String RANGER_TYPE_OZONE_BUCKET  = "bucket";
	public static final String RANGER_TYPE_OZONE_KEY 	 = "key";

	public static final String[] SUPPORTED_ENTITY_TYPES = { ENTITY_TYPE_OZONE_VOLUME, ENTITY_TYPE_OZONE_BUCKET, ENTITY_TYPE_OZONE_KEY };

	private static final String SEP_PROTOCOL               = "://";
	private static final String SEP_RELATIVE_PATH          = "/";
	private static final int    IDX_VOLUME       		   = 0;
	private static final int    IDX_BUCKET     			   = 1;
	private static final int    IDX_KEY 				   = 2;
	private static final int    IDX_CLUSTER_NAME           = 3;
	private static final int    RESOURCE_COUNT             = 4;

	public AtlasOzoneResourceMapper() {
		super("ozone", SUPPORTED_ENTITY_TYPES);
	}

	@Override
	public RangerServiceResource buildResource(final RangerAtlasEntity entity) throws Exception {
		String qualifiedName = (String)entity.getAttributes().get(AtlasResourceMapper.ENTITY_ATTRIBUTE_QUALIFIED_NAME);
		if (StringUtils.isEmpty(qualifiedName)) {
			throw new Exception("attribute '" +  ENTITY_ATTRIBUTE_QUALIFIED_NAME + "' not found in entity");
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("ENTITY_ATTRIBUTE_QUALIFIED_NAME = " + qualifiedName);
		}

		String   entityType  = entity.getTypeName();
		String   entityGuid  = entity.getGuid();

		String[] resources   = parseQualifiedName(qualifiedName, entityType);
		String   volName     = resources[IDX_VOLUME];
		String   bktName     = resources[IDX_BUCKET];
		String   keyName     = resources[IDX_KEY];
		String   clusterName = resources[IDX_CLUSTER_NAME];

		if (LOG.isDebugEnabled()) {
			LOG.debug("Ozone resources for entityType " + entityType + " are " + Arrays.toString(resources));
		}

		if (StringUtils.isEmpty(clusterName)) {
			throwExceptionWithMessage("cluster-name not found in attribute '" +  ENTITY_ATTRIBUTE_QUALIFIED_NAME + "': " + qualifiedName);
		}
		String   serviceName = getRangerServiceName(clusterName);

		Map<String, RangerPolicyResource> elements = new HashMap<String, RangerPolicyResource>();

		if (StringUtils.equals(entityType, ENTITY_TYPE_OZONE_VOLUME)) {
			if (StringUtils.isNotEmpty(volName)) {
				elements.put(RANGER_TYPE_OZONE_VOLUME, new RangerPolicyResource(volName));
			} else {
				throwExceptionWithMessage("volume-name not found in attribute '" +  ENTITY_ATTRIBUTE_QUALIFIED_NAME + "': " + qualifiedName);
			}
		} else if (StringUtils.equals(entityType, ENTITY_TYPE_OZONE_BUCKET)) {
			if (StringUtils.isNotEmpty(volName) && StringUtils.isNotEmpty(bktName)) {
				elements.put(RANGER_TYPE_OZONE_VOLUME, new RangerPolicyResource(volName));
				elements.put(RANGER_TYPE_OZONE_BUCKET, new RangerPolicyResource(bktName));
			} else {
				if (StringUtils.isEmpty(volName)) {
					throwExceptionWithMessage("volume-name not found in attribute '" +  ENTITY_ATTRIBUTE_QUALIFIED_NAME + "': " + qualifiedName);
				} else if (StringUtils.isEmpty(bktName)) {
					throwExceptionWithMessage("bucket-name not found in attribute '" +  ENTITY_ATTRIBUTE_QUALIFIED_NAME + "': " + qualifiedName);
				}
			}
		} else if (StringUtils.equals(entityType, ENTITY_TYPE_OZONE_KEY)) {
			if (StringUtils.isNotEmpty(volName) && StringUtils.isNotEmpty(bktName) && StringUtils.isNotEmpty(keyName)) {
				elements.put(RANGER_TYPE_OZONE_VOLUME, new RangerPolicyResource(volName));
				elements.put(RANGER_TYPE_OZONE_BUCKET, new RangerPolicyResource(bktName));
				elements.put(RANGER_TYPE_OZONE_KEY, new RangerPolicyResource(keyName));
			} else {
				if (StringUtils.isEmpty(volName)) {
					throwExceptionWithMessage("volume-name not found in attribute '" +  ENTITY_ATTRIBUTE_QUALIFIED_NAME + "': " + qualifiedName);
				} else if (StringUtils.isEmpty(bktName)) {
					throwExceptionWithMessage("bucket-name not found in attribute '" +  ENTITY_ATTRIBUTE_QUALIFIED_NAME + "': " + qualifiedName);
				} else if (StringUtils.isEmpty(keyName)) {
					throwExceptionWithMessage("key-name not found in attribute '" +  ENTITY_ATTRIBUTE_QUALIFIED_NAME + "': " + qualifiedName);
				}
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

	/* qualifiedName can be of format, depending upon the entity-type:
	 * o3fs://<volume name>@cm (ozone_key)
	 * o3fs://<volume name>.<bucket name>@<clusterName> (ozone_bucket)
	 * o3fs://<bucket name>.<volume name>.<ozone service id>/<key path>@<clusterName> (ozone_key)
	 */
	private String[] parseQualifiedName(String qualifiedName, String entityType) {
		String[] ret = new String[RESOURCE_COUNT];

		if(StringUtils.isNotBlank(qualifiedName)) {
			int idxClusterNameSep = qualifiedName.lastIndexOf(CLUSTER_DELIMITER);

			if (idxClusterNameSep != -1) {
				ret[IDX_CLUSTER_NAME] = qualifiedName.substring(idxClusterNameSep + CLUSTER_DELIMITER.length());

				int idxProtocolSep = qualifiedName.indexOf(SEP_PROTOCOL);

				if (idxProtocolSep != -1) {
					int idxResourceStart       = idxProtocolSep + SEP_PROTOCOL.length();
					if (StringUtils.equals(entityType, ENTITY_TYPE_OZONE_VOLUME)) {
						ret[IDX_VOLUME] = qualifiedName.substring(idxResourceStart, idxClusterNameSep);
					} else if (StringUtils.equals(entityType, ENTITY_TYPE_OZONE_BUCKET)) {
						String[] resources = qualifiedName.substring(idxResourceStart, idxClusterNameSep).split(QUALIFIED_NAME_DELIMITER);
						ret[IDX_VOLUME]      = resources.length > 0 ? resources[0] : null;
						ret[IDX_BUCKET]     = resources.length > 1 ? resources[1] : null;
					} else if (StringUtils.equals(entityType, ENTITY_TYPE_OZONE_KEY)) {
						String[] resources = qualifiedName.substring(idxResourceStart, idxClusterNameSep).split(QUALIFIED_NAME_DELIMITER);
						ret[IDX_BUCKET]      = resources.length > 0 ? resources[0] : null;
						ret[IDX_VOLUME]     = resources.length > 1 ? resources[1] : null;
						int idxRelativePath = qualifiedName.indexOf(SEP_RELATIVE_PATH, idxResourceStart);
						if (idxRelativePath != -1) {
							ret[IDX_KEY] = qualifiedName.substring(idxRelativePath, idxClusterNameSep);
						}
					}
				}
			}
		}

		return ret;
	}
}
