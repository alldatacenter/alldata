/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ranger.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.NoResultException;

import org.apache.ranger.common.db.BaseDao;
import org.apache.ranger.entity.XXServiceConfigMap;
import org.apache.ranger.services.tag.RangerServiceTag;
import org.springframework.stereotype.Service;

@Service
public class XXServiceConfigMapDao extends BaseDao<XXServiceConfigMap> {

	private static final String SERVICE_CLUSTER_NAME_CONF_KEY = "cluster.name";

	public XXServiceConfigMapDao(RangerDaoManagerBase daoManager) {
		super(daoManager);
	}

	public List<XXServiceConfigMap> findByServiceId(Long serviceId) {
		if (serviceId == null) {
			return new ArrayList<XXServiceConfigMap>();
		}
		try {
			return getEntityManager()
					.createNamedQuery("XXServiceConfigMap.findByServiceId", tClass)
					.setParameter("serviceId", serviceId)
					.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<XXServiceConfigMap>();
		}
	}

	public XXServiceConfigMap findByServiceAndConfigKey(Long serviceId,
			String configKey) {
		if(serviceId == null || configKey == null) {
			return null;
		}
		try {
			return getEntityManager()
					.createNamedQuery("XXServiceConfigMap.findByServiceAndConfigKey", tClass)
					.setParameter("serviceId", serviceId)
					.setParameter("configKey", configKey).getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	public XXServiceConfigMap findByServiceNameAndConfigKey(String serviceName, String configKey) {
		if(serviceName == null || configKey == null) {
			return null;
		}
		try {
			return getEntityManager()
					.createNamedQuery("XXServiceConfigMap.findByServiceNameAndConfigKey", tClass)
					.setParameter("name", serviceName)
					.setParameter("configKey", configKey).getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	/**
	 * Get resource based service IDs, using supplied clusterName
	 * @param clusterName
	 * @return {@link java.util.List List} of service IDs if found, empty {@link java.util.List List} otherwise.
	 */
	public List<Long> findServiceIdsByClusterName(String clusterName) {
		if(clusterName == null) {
			return Collections.emptyList();
		}

		return findServiceIdsByConfigKeyAndConfigValueFilterByServiceType(SERVICE_CLUSTER_NAME_CONF_KEY, clusterName, RangerServiceTag.TAG_RESOURCE_NAME);
	}

	/**
	 * Get service IDs, using supplied configKey and configValue and are not of provided serviceType.
	 * Caller of this method must make sure, {@code configKey} and {@code configValue) passed as method parameter are not null.
	 *
	 * @param configKey
	 * @param configValue
	 * @param serviceType
	 * @return {@link java.util.List list} of service IDs if found, empty {@link java.util.List list} otherwise.
	 */
	private List<Long> findServiceIdsByConfigKeyAndConfigValueFilterByServiceType(String configKey, String configValue, String serviceType) {
		try {
			return getEntityManager()
					.createNamedQuery("XXServiceConfigMap.findServiceIdsByConfigKeyAndConfigValueAndFilterByServiceType", Long.class)
					.setParameter("configKey", configKey)
					.setParameter("configValue", configValue)
					.setParameter("serviceType", serviceType)
					.getResultList();
		}

		catch (NoResultException e) {
			return Collections.emptyList();
		}
	}

	public List<XXServiceConfigMap> findByConfigKey(String configKey) {
		if(configKey == null) {
			return Collections.emptyList();
		}
		try {
			return getEntityManager()
					.createNamedQuery("XXServiceConfigMap.findByConfigKey", tClass)
					.setParameter("configKey", configKey).getResultList();
		} catch (NoResultException e) {
			return Collections.emptyList();
		}
	}
}
