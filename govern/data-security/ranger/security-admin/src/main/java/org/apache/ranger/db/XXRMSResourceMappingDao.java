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
import java.util.List;

import javax.persistence.NoResultException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.ranger.common.db.BaseDao;
import org.apache.ranger.plugin.model.RangerServiceResource;
import org.apache.ranger.entity.XXRMSResourceMapping;
import org.apache.ranger.entity.XXRMSServiceResource;
import org.springframework.stereotype.Service;

/**
 */
@Service
public class XXRMSResourceMappingDao extends BaseDao<XXRMSResourceMapping> {

	//private static final Log LOG = LogFactory.getLog(XXRMSResourceMappingDao.class);

	public XXRMSResourceMappingDao(RangerDaoManagerBase daoManager) {
		super(daoManager);
	}

	@SuppressWarnings("unchecked")
	public List<Object[]> getResourceMappings() {
		return getEntityManager().createNamedQuery("XXRMSResourceMapping.getResourceMapping").getResultList();
	}

	public void deleteByHlResourceId(Long resourceId) {
		getEntityManager()
			.createNamedQuery("XXRMSResourceMapping.deleteByHlResourceId")
			.setParameter("resourceId", resourceId)
			.executeUpdate();
	}

	public void deleteByLlResourceId(Long resourceId) {
		getEntityManager()
				.createNamedQuery("XXRMSResourceMapping.deleteByLlResourceId")
				.setParameter("resourceId", resourceId)
				.executeUpdate();
	}

	public void deleteByHlAndLlResourceId(Long hlResourceId, Long llResourceId) {
		getEntityManager()
				.createNamedQuery("XXRMSResourceMapping.deleteByHlAndLlResourceId")
				.setParameter("hlResourceId", hlResourceId)
				.setParameter("llResourceId", llResourceId)
				.executeUpdate();
	}

	public XXRMSResourceMapping findByHlAndLlResourceId(Long hlResourceId, Long llResourceId) {
		try {
			return getEntityManager()
					.createNamedQuery("XXRMSResourceMapping.findByHlAndLlResourceId", XXRMSResourceMapping.class)
					.setParameter("hlResourceId", hlResourceId)
					.setParameter("llResourceId", llResourceId)
					.getSingleResult();
		} catch (NoResultException e) {
		}
		return null;
	}

	public List<Long> findByHlResource(RangerServiceResource hlResource) {
		return findByHlResourceId(hlResource.getId());
	}

	public List<Long> findByHlResourceId(Long hlResourceId) {
		return getEntityManager()
				.createNamedQuery("XXRMSResourceMapping.findByHlResourceId", Long.class)
				.setParameter("hlResourceId", hlResourceId)
				.getResultList();
	}

	public List<Long> findByLlResource(RangerServiceResource llResource) {
		return findByLlResourceId(llResource.getId());
	}

	public List<Long> findByLlResourceId(Long llResourceId) {
		return getEntityManager()
				.createNamedQuery("XXRMSResourceMapping.findByLlResourceId", Long.class)
				.setParameter("llResourceId", llResourceId)
				.getResultList();
	}

	public List<RangerServiceResource> getServiceResourcesByLlResourceId(long llResourceId) {
		List<RangerServiceResource> ret = new ArrayList<>();

		List<Object[]> rows = null;
		try {
			rows = getEntityManager()
					.createNamedQuery("XXRMSResourceMapping.getServiceResourcesByLlResourceId", Object[].class)
					.setParameter("llResourceId", llResourceId)
					.getResultList();
		} catch (NoResultException e) {
			// Nothing
		}

		if (CollectionUtils.isNotEmpty(rows)) {
			for (Object[] row : rows) {
				XXRMSServiceResource xxServiceResource = new XXRMSServiceResource();
				xxServiceResource.setId((Long) row[0]);
				xxServiceResource.setGuid((String) row[1]);
				xxServiceResource.setVersion((Long) row[2]);
				xxServiceResource.setIsEnabled((Boolean) row[3]);
				xxServiceResource.setResourceSignature((String) row[4]);
				xxServiceResource.setServiceId((Long) row[5]);
				xxServiceResource.setServiceResourceElements((String) row[6]);
				ret.add(XXRMSServiceResourceDao.populateViewBean(xxServiceResource));
			}
		}
		return ret;
	}

}
