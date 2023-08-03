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

import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.model.RangerServiceResource;
import org.apache.ranger.tagsync.source.atlasrest.RangerAtlasEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AtlasResourceMapper {
	private static final Logger LOG = LoggerFactory.getLogger(AtlasResourceMapper.class);

	public static final String TAGSYNC_DEFAULT_CLUSTER_NAME = "ranger.tagsync.atlas.default.cluster.name";
	public static final String ENTITY_ATTRIBUTE_QUALIFIED_NAME = "qualifiedName";
	public static final String QUALIFIED_NAME_DELIMITER        = "\\.";
	public static final Character QUALIFIED_NAME_DELIMITER_CHAR    = '.';

	public static final String TAGSYNC_SERVICENAME_MAPPER_PROP_PREFIX                  = "ranger.tagsync.atlas.";
	public static final String TAGSYNC_SERVICENAME_MAPPER_PROP_SUFFIX                  = ".ranger.service";
	public static final String TAGSYNC_ATLAS_CLUSTER_IDENTIFIER                        = ".instance.";
	public static final String TAGSYNC_DEFAULT_CLUSTERNAME_AND_COMPONENTNAME_SEPARATOR = "_";
	public static final String CLUSTER_DELIMITER                                       = "@";

	protected final String   componentName;
	protected final String[] supportedEntityTypes;

	protected Properties properties;
	protected String     defaultClusterName;

	public AtlasResourceMapper(String componentName, String[] supportedEntityTypes) {
		this.componentName        = componentName;
		this.supportedEntityTypes = supportedEntityTypes;
	}

	public final String getComponentName() {
		return componentName;
	}

	public final String[] getSupportedEntityTypes() {
		return supportedEntityTypes;
	}

	public String getRangerServiceName(String clusterName) {
		String ret = getCustomRangerServiceName(clusterName);

		if (StringUtils.isBlank(ret)) {
			ret = clusterName + TAGSYNC_DEFAULT_CLUSTERNAME_AND_COMPONENTNAME_SEPARATOR + componentName;
		}
		return ret;
	}

	public void initialize(Properties properties) {
		this.properties         = properties;
		this.defaultClusterName = properties != null ? properties.getProperty(TAGSYNC_DEFAULT_CLUSTER_NAME) : null;
	}

	abstract public RangerServiceResource buildResource(final RangerAtlasEntity entity) throws Exception;

	protected String getCustomRangerServiceName(String atlasInstanceName) {
		if(properties != null) {
			String propName = TAGSYNC_SERVICENAME_MAPPER_PROP_PREFIX + componentName
					+ TAGSYNC_ATLAS_CLUSTER_IDENTIFIER + atlasInstanceName
					+ TAGSYNC_SERVICENAME_MAPPER_PROP_SUFFIX;

			return properties.getProperty(propName);
		} else {
			return null;
		}
	}

	protected  String getResourceNameFromQualifiedName(String qualifiedName) {
		if(StringUtils.isNotBlank(qualifiedName)) {
			int idx = qualifiedName.lastIndexOf(CLUSTER_DELIMITER);

			if(idx != -1) {
				return qualifiedName.substring(0, idx);
			} else {
				return qualifiedName;
			}
		}

		return null;
	}

	protected  String getClusterNameFromQualifiedName(String qualifiedName) {
		if(StringUtils.isNotBlank(qualifiedName)) {
			int idx = qualifiedName.lastIndexOf(CLUSTER_DELIMITER);

			if(idx != -1 && qualifiedName.length() > idx) {
				return qualifiedName.substring(idx + 1);
			}
		}

		return null;
	}

	protected void throwExceptionWithMessage(String msg) throws Exception {
		LOG.error(msg);

		throw new Exception(msg);
	}
}
