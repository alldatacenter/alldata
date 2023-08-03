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

import org.apache.commons.collections.CollectionUtils;
import org.apache.ranger.biz.ServiceDBStore;
import org.apache.ranger.common.db.BaseDao;
import org.apache.ranger.entity.XXServiceVersionInfo;
import org.apache.ranger.plugin.util.ServiceTags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 */
@Service
public class XXServiceVersionInfoDao extends BaseDao<XXServiceVersionInfo> {

	private static final Logger LOG = LoggerFactory.getLogger(XXServiceVersionInfoDao.class);

	/**
	 * Default Constructor
	 */
	public XXServiceVersionInfoDao(RangerDaoManagerBase daoManager) {
		super(daoManager);
	}

	public XXServiceVersionInfo findByServiceName(String serviceName) {
		if (serviceName == null) {
			return null;
		}
		try {
			return getEntityManager()
					.createNamedQuery("XXServiceVersionInfo.findByServiceName", tClass)
					.setParameter("serviceName", serviceName).getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	public XXServiceVersionInfo findByServiceId(Long serviceId) {
		if (serviceId == null) {
			return null;
		}
		try {
			return getEntityManager().createNamedQuery("XXServiceVersionInfo.findByServiceId", tClass)
					.setParameter("serviceId", serviceId).getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public List<Object[]> getAllWithServiceNames(){
		return getEntityManager()
				.createNamedQuery("XXServiceVersionInfo.getAllWithServiceNames")
				.getResultList();
	}

	public void updateServiceVersionInfoForTagResourceMapCreate(Long resourceId, Long tagId) {
		if (resourceId == null || tagId == null) {
			LOG.warn("Unexpected null value for resourceId and/or tagId");
			return;
		}

		try {
			List<XXServiceVersionInfo> serviceVersionInfos = getEntityManager().createNamedQuery("XXServiceVersionInfo.findByServiceResourceId", tClass).setParameter("resourceId", resourceId).getResultList();

			updateTagVersionAndTagUpdateTime(serviceVersionInfos, resourceId, tagId);
		} catch (NoResultException e) {
		}
	}

	public void updateServiceVersionInfoForTagResourceMapDelete(Long resourceId, Long tagId) {
		if (resourceId == null || tagId == null) {
			LOG.warn("Unexpected null value for resourceId and/or tagId");
			return;
		}

		try {
			List<XXServiceVersionInfo> serviceVersionInfos = getEntityManager().createNamedQuery("XXServiceVersionInfo.findByServiceResourceId", tClass).setParameter("resourceId", resourceId).getResultList();

			updateTagVersionAndTagUpdateTime(serviceVersionInfos, resourceId, tagId);
		} catch (NoResultException e) {
		}
	}
	public void updateServiceVersionInfoForServiceResourceUpdate(Long resourceId) {
		if (resourceId == null) {
			LOG.warn("Unexpected null value for resourceId");
			return;
		}

		Long tagId = null;

		try {
			List<XXServiceVersionInfo> serviceVersionInfos = getEntityManager().createNamedQuery("XXServiceVersionInfo.findByServiceResourceId", tClass).setParameter("resourceId", resourceId).getResultList();

			updateTagVersionAndTagUpdateTime(serviceVersionInfos, resourceId, tagId);
		} catch (NoResultException e) {
		}
	}

	public void updateServiceVersionInfoForTagUpdate(Long tagId) {
		if (tagId == null) {
			LOG.warn("Unexpected null value for tagId");
			return;
		}

		Long resourceId = null;
		try {
			List<XXServiceVersionInfo> serviceVersionInfos = getEntityManager().createNamedQuery("XXServiceVersionInfo.findByTagId", tClass).setParameter("tagId", tagId).getResultList();

			updateTagVersionAndTagUpdateTime(serviceVersionInfos, resourceId, tagId);
		} catch (NoResultException e) {
		}
	}

	private void updateTagVersionAndTagUpdateTime(List<XXServiceVersionInfo> serviceVersionInfos, Long resourceId, Long tagId) {

		if (resourceId != null || tagId != null) {
			if (CollectionUtils.isNotEmpty(serviceVersionInfos)) {

				final ServiceDBStore.VERSION_TYPE versionType = ServiceDBStore.VERSION_TYPE.TAG_VERSION;
				final ServiceTags.TagsChangeType tagChangeType;

				if (tagId == null) {
					tagChangeType = ServiceTags.TagsChangeType.SERVICE_RESOURCE_UPDATE;
				} else if (resourceId == null) {
					tagChangeType = ServiceTags.TagsChangeType.TAG_UPDATE;
				} else {
					tagChangeType = ServiceTags.TagsChangeType.TAG_RESOURCE_MAP_UPDATE;
				}

				for (XXServiceVersionInfo serviceVersionInfo : serviceVersionInfos) {

					final Long serviceId = serviceVersionInfo.getServiceId();
					final Runnable serviceVersionUpdater = new ServiceDBStore.ServiceVersionUpdater(daoManager, serviceId, versionType, tagChangeType, resourceId, tagId);

					daoManager.getRangerTransactionSynchronizationAdapter().executeOnTransactionCommit(serviceVersionUpdater);
				}
			}
		} else {
			LOG.warn("Both resourceId and tagId are null! Should not have come here!");
		}

	}
}
