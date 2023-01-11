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

import org.apache.ranger.common.db.BaseDao;
import org.apache.ranger.entity.XXService;
import org.springframework.stereotype.Service;

/**
 */

@Service
public class XXServiceDao extends BaseDao<XXService> {
	/**
	 * Default Constructor
	 */
	public XXServiceDao(RangerDaoManagerBase daoManager) {
		super(daoManager);
	}

	public XXService findByName(String name) {
		if (name == null) {
			return null;
		}
		try {
			return getEntityManager()
					.createNamedQuery("XXService.findByName", tClass)
					.setParameter("name", name).getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	public XXService findByDisplayName(String displayName) {
		if (displayName == null) {
			return null;
		}
		try {
			return getEntityManager()
					.createNamedQuery("XXService.findByDisplayName", tClass)
					.setParameter("displayName", displayName).getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	public Long getMaxIdOfXXService() {
		try {
			return (Long) getEntityManager().createNamedQuery("XXService.getMaxIdOfXXService").getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	public List<XXService> findByServiceDefId(Long serviceDefId) {
		if (serviceDefId == null) {
			return new ArrayList<XXService>();
		}
		try {
			return getEntityManager().createNamedQuery("XXService.findByServiceDefId", tClass)
					.setParameter("serviceDefId", serviceDefId).getResultList();
		} catch (NoResultException e) {
			return new ArrayList<XXService>();
		}
	}

	public List<XXService> findByTagServiceId(Long tagServiceId) {
		if (tagServiceId == null) {
			return new ArrayList<XXService>();
		}
		try {
			return getEntityManager().createNamedQuery("XXService.findByTagServiceId", tClass)
					.setParameter("tagServiceId", tagServiceId).getResultList();
		} catch (NoResultException e) {
			return new ArrayList<XXService>();
		}
	}

	public XXService findAssociatedTagService(String serviceName) {
		try {
			return getEntityManager().createNamedQuery("XXService.findAssociatedTagService", tClass)
					.setParameter("serviceName", serviceName).getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	public List<XXService> getAllServicesWithTagService() {
		try {
			return getEntityManager().createNamedQuery("XXService.getAllServicesWithTagService", tClass)
					.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<XXService>();
		}
	}

	public void updateSequence() {
		Long maxId = getMaxIdOfXXService();

		if(maxId == null) {
			return;
		}

		updateSequence("X_SERVICE_SEQ", maxId + 1);
	}

	public List<Long> getAllServiceIds() {
		try {
			return getEntityManager().createNamedQuery("XXService.getAllServiceIds", Long.class)
					.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<>();
		}
	}

}
