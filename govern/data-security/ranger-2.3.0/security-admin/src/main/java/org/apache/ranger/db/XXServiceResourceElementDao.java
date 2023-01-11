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

import org.apache.ranger.common.db.BaseDao;
import org.apache.ranger.entity.XXServiceResourceElement;
import org.springframework.stereotype.Service;

@Service
public class XXServiceResourceElementDao extends BaseDao<XXServiceResourceElement> {

	public XXServiceResourceElementDao(RangerDaoManagerBase daoManager) {
		super(daoManager);
	}

	public List<XXServiceResourceElement> findByResourceId(Long resourceId) {
		if (resourceId == null) {
			return new ArrayList<XXServiceResourceElement>();
		}
		try {
			return getEntityManager().createNamedQuery("XXServiceResourceElement.findByResourceId", tClass)
					.setParameter("resourceId", resourceId)
					.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<XXServiceResourceElement>();
		}
	}

	public List<XXServiceResourceElement> findByServiceId(Long serviceId) {
		if (serviceId == null) {
			return new ArrayList<XXServiceResourceElement>();
		}
		try {
			return getEntityManager().createNamedQuery("XXServiceResourceElement.findByServiceId", tClass)
					.setParameter("serviceId", serviceId)
					.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<XXServiceResourceElement>();
		}
	}

	public List<XXServiceResourceElement> findTaggedResourcesInServiceId(Long serviceId) {
		if (serviceId == null) {
			return new ArrayList<XXServiceResourceElement>();
		}
		try {
			return getEntityManager().createNamedQuery("XXServiceResourceElement.findTaggedResourcesInServiceId", tClass)
					.setParameter("serviceId", serviceId)
					.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<XXServiceResourceElement>();
		}
	}

}
