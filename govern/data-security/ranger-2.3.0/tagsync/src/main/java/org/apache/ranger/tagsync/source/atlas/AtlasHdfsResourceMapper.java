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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.fs.Path;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.model.RangerServiceResource;
import org.apache.ranger.tagsync.source.atlasrest.RangerAtlasEntity;

public class AtlasHdfsResourceMapper extends AtlasResourceMapper {
	public static final String ENTITY_TYPE_HDFS_PATH = "hdfs_path";
	public static final String RANGER_TYPE_HDFS_PATH = "path";
	public static final String TAGSYNC_ATLAS_NAME_SERVICE_IDENTIFIER = ".nameservice.";
	public static final String ENTITY_TYPE_HDFS_CLUSTER_AND_NAME_SERVICE_SEPARATOR = "_";

	public static final String ENTITY_ATTRIBUTE_PATH           = "path";
	public static final String ENTITY_ATTRIBUTE_CLUSTER_NAME   = "clusterName";
	public static final String ENTITY_ATTRIBUTE_NAME_SERVICE_ID   = "nameServiceId";

	public static final String[] SUPPORTED_ENTITY_TYPES = { ENTITY_TYPE_HDFS_PATH };

	public AtlasHdfsResourceMapper() {
		super("hdfs", SUPPORTED_ENTITY_TYPES);
	}

	/*
		If cl2_hdfs is the service-name for HDFS in 'cl2' cluster, with no specific nameService (in a non-Federated) setup
		# ranger.ragsync.atlas.hdfs.instance.cl2.ranger.service=cl2_hdfs
	 */
	@Override
	public String getRangerServiceName(String clusterName) {
		String ret = getCustomRangerServiceName(clusterName);

		if (StringUtils.isBlank(ret)) {
			ret = clusterName + TAGSYNC_DEFAULT_CLUSTERNAME_AND_COMPONENTNAME_SEPARATOR + "hadoop";
		}

		return ret;
	}

	/*
		If c11_hdfs_ns1 is the service-name for HDFS in 'cl1' cluster, that services nameService 'ns1' in a Federated setup
		# ranger.tagsync.atlas.hdfs.instance.cl1.nameservice.ns1.ranger.service=cl1_hdfs_ns1
	*/

	private String getCustomRangerServiceNameForClusterAndNameService(String clusterName, String nameServiceId) {
		String ret = null;
		if(properties != null) {
			String propName = TAGSYNC_SERVICENAME_MAPPER_PROP_PREFIX + componentName
					+ TAGSYNC_ATLAS_CLUSTER_IDENTIFIER + clusterName
					+ TAGSYNC_ATLAS_NAME_SERVICE_IDENTIFIER + nameServiceId
					+ TAGSYNC_SERVICENAME_MAPPER_PROP_SUFFIX;

			ret = properties.getProperty(propName);
		}
		if (ret == null) {
			ret = getRangerServiceName(clusterName) + ENTITY_TYPE_HDFS_CLUSTER_AND_NAME_SERVICE_SEPARATOR + nameServiceId;
		}
		return ret;
	}

	@Override
	public RangerServiceResource buildResource(final RangerAtlasEntity entity) throws Exception {
		String qualifiedName = (String)entity.getAttributes().get(AtlasResourceMapper.ENTITY_ATTRIBUTE_QUALIFIED_NAME);
		String nameServiceId = (String)entity.getAttributes().get(ENTITY_ATTRIBUTE_NAME_SERVICE_ID);

		String path          = null;
		String clusterName   = null;

		if (StringUtils.isNotEmpty(qualifiedName)) {
			path = getResourceNameFromQualifiedName(qualifiedName);
			clusterName = getClusterNameFromQualifiedName(qualifiedName);
		}

		if (StringUtils.isEmpty(path)) {
			path = (String) entity.getAttributes().get(ENTITY_ATTRIBUTE_PATH);
		}
		if (StringUtils.isEmpty(path)) {
			throwExceptionWithMessage("path not found in attribute '" + ENTITY_ATTRIBUTE_QUALIFIED_NAME + "' or '" + ENTITY_ATTRIBUTE_PATH +  "'");
		}

		if (StringUtils.isEmpty(clusterName)) {
			clusterName = (String) entity.getAttributes().get(ENTITY_ATTRIBUTE_CLUSTER_NAME);
		}
		if (StringUtils.isEmpty(clusterName)) {
			clusterName = defaultClusterName;
		}
		if (StringUtils.isEmpty(clusterName)) {
			throwExceptionWithMessage("clusterName not found in attribute '" + ENTITY_ATTRIBUTE_QUALIFIED_NAME + "' or '" + ENTITY_ATTRIBUTE_CLUSTER_NAME +  "'");
		}

		String  entityGuid  = entity.getGuid();
		String  serviceName = StringUtils.isNotBlank(nameServiceId) ? getCustomRangerServiceNameForClusterAndNameService(clusterName, nameServiceId) : getRangerServiceName(clusterName);
		Boolean isExcludes  = Boolean.FALSE;
		Boolean isRecursive = Boolean.TRUE;

		Path pathObj = new Path(path);

		Map<String, RangerPolicyResource> elements = new HashMap<String, RangerPolicy.RangerPolicyResource>();
		elements.put(RANGER_TYPE_HDFS_PATH, new RangerPolicyResource(pathObj.toUri().getPath(), isExcludes, isRecursive));

		RangerServiceResource ret = new RangerServiceResource(entityGuid, serviceName, elements);

		return ret;
	}
}
