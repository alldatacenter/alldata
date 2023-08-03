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

import java.util.List;

import javax.persistence.NoResultException;

import org.apache.ranger.common.DateUtil;
import org.apache.ranger.common.db.BaseDao;
import org.apache.ranger.entity.XXPluginInfo;
import org.springframework.stereotype.Service;

/**
 */
@Service
public class XXPluginInfoDao extends BaseDao<XXPluginInfo> {
	/**
	 * Default Constructor
	 */
	public XXPluginInfoDao(RangerDaoManagerBase daoManager) {
		super(daoManager);
	}

	@Override
	public XXPluginInfo create(XXPluginInfo obj) {
		obj.setCreateTime(DateUtil.getUTCDate());
		obj.setUpdateTime(DateUtil.getUTCDate());
		return super.create(obj);
	}

	@Override
	public XXPluginInfo update(XXPluginInfo obj) {
		obj.setUpdateTime(DateUtil.getUTCDate());
		return super.update(obj);
	}
	public XXPluginInfo find(String serviceName, String hostName, String appType) {
		if (serviceName == null || hostName == null || appType == null) {
			return null;
		}
		try {
			return getEntityManager()
					.createNamedQuery("XXPluginInfo.find", tClass)
					.setParameter("serviceName", serviceName)
					.setParameter("appType", appType)
					.setParameter("hostName", hostName)
					.getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}
	public List<XXPluginInfo> findByServiceName(String serviceName) {
		if (serviceName == null) {
			return null;
		}
		try {
			return getEntityManager()
					.createNamedQuery("XXPluginInfo.findByServiceName", tClass)
					.setParameter("serviceName", serviceName).getResultList();
		} catch (NoResultException e) {
			return null;
		}
	}

	public List<XXPluginInfo> findByServiceId(Long serviceId) {
		if (serviceId == null) {
			return null;
		}
		try {
			return getEntityManager()
					.createNamedQuery("XXPluginInfo.findByServiceId", tClass)
					.setParameter("serviceId", serviceId).getResultList();
		} catch (NoResultException e) {
			return null;
		}
	}

	public List<XXPluginInfo> findByServiceAndHostName(String serviceName, String hostName) {
		if (serviceName == null || hostName == null) {
			return null;
		}
		try {
			return getEntityManager()
					.createNamedQuery("XXPluginInfo.findByServiceAndHostName", tClass)
					.setParameter("serviceName", serviceName)
					.setParameter("hostName", hostName)
					.getResultList();
		} catch (NoResultException e) {
			return null;
		}
	}

}
