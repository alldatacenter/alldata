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

package org.apache.ranger.db;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.NoResultException;

import org.apache.commons.lang.StringUtils;
import org.apache.ranger.authorization.utils.StringUtil;
import org.apache.ranger.common.db.BaseDao;
import org.apache.ranger.entity.XXTag;
import org.springframework.stereotype.Service;

@Service
public class XXTagDao extends BaseDao<XXTag> {

	public XXTagDao(RangerDaoManagerBase daoManager) {
		super(daoManager);
	}

	public List<XXTag> findByResourceId(Long resourceId) {
		if (resourceId == null) {
			return new ArrayList<XXTag>();
		}
		try {
			return getEntityManager().createNamedQuery("XXTag.findByResourceId", tClass)
					.setParameter("resourceId", resourceId).getResultList();
		} catch (NoResultException e) {
			return new ArrayList<XXTag>();
		}
	}

	@SuppressWarnings("unchecked")
	public List<String> findTagTypesByServiceId(Long serviceId) {
		if (serviceId == null) {
			return new ArrayList<String>();
		}
		try {
			return getEntityManager().createNamedQuery("XXTag.findTagTypesByServiceId")
					.setParameter("serviceId", serviceId).getResultList();
		} catch (NoResultException e) {
			return new ArrayList<String>();
		}
	}

	public XXTag findByGuid(String guid) {
		if (StringUtil.isEmpty(guid)) {
			return null;
		}
		try {
			return getEntityManager().createNamedQuery("XXTag.findByGuid", tClass)
					.setParameter("guid", guid).getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	public List<XXTag> findByName(String name) {
		if (StringUtils.isEmpty(name)) {
			return new ArrayList<XXTag>();
		}

		try {
			return getEntityManager().createNamedQuery("XXTag.findByName", tClass)
					.setParameter("name", name).getResultList();
		} catch (NoResultException e) {
			return new ArrayList<XXTag>();
		}
	}

	public List<XXTag> findForResourceId(Long resourceId) {
		if (resourceId == null) {
			return new ArrayList<XXTag>();
		}

		try {
			return getEntityManager().createNamedQuery("XXTag.findByResourceId", tClass)
					.setParameter("resourceId", resourceId).getResultList();
		} catch (NoResultException e) {
			return new ArrayList<XXTag>();
		}
	}

	public List<XXTag> findForResourceGuid(String resourceGuid) {
		if (StringUtils.isEmpty(resourceGuid)) {
			return new ArrayList<XXTag>();
		}

		try {
			return getEntityManager().createNamedQuery("XXTag.findByResourceGuid", tClass)
					.setParameter("resourceGuid", resourceGuid).getResultList();
		} catch (NoResultException e) {
			return new ArrayList<XXTag>();
		}
	}

	public List<XXTag> findByServiceId(Long serviceId) {
		if (serviceId == null) {
			return new ArrayList<XXTag>();
		}

		try {
			return getEntityManager().createNamedQuery("XXTag.findByServiceId", tClass)
					.setParameter("serviceId", serviceId).getResultList();
		} catch (NoResultException e) {
			return new ArrayList<XXTag>();
		}
	}

	public List<XXTag> findByServiceIdAndOwner(Long serviceId, Short owner) {
		if (serviceId == null) {
			return new ArrayList<XXTag>();
		}

		try {
			return getEntityManager().createNamedQuery("XXTag.findByServiceIdAndOwner", tClass)
					.setParameter("serviceId", serviceId)
					.setParameter("owner", owner)
					.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<XXTag>();
		}
	}
}
