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
import org.apache.ranger.entity.XXPolicyItem;
import org.springframework.stereotype.Service;

/**
 */

@Service
public class XXPolicyItemDao extends BaseDao<XXPolicyItem> {
	/**
	 * Default Constructor
	 */
	public XXPolicyItemDao(RangerDaoManagerBase daoManager) {
		super(daoManager);
	}

	public List<XXPolicyItem> findByPolicyId(Long policyId) {
		if (policyId == null) {
			return new ArrayList<XXPolicyItem>();
		}
		try {
			List<XXPolicyItem> returnList = getEntityManager()
					.createNamedQuery("XXPolicyItem.findByPolicyId", tClass)
					.setParameter("policyId", policyId).getResultList();
			if (returnList == null) {
				return new ArrayList<XXPolicyItem>();
			}
			return returnList;
		} catch (NoResultException e) {
			return new ArrayList<XXPolicyItem>();
		}
	}

	public List<XXPolicyItem> findByServiceId(Long serviceId) {
		if (serviceId == null) {
			return new ArrayList<XXPolicyItem>();
		}
		try {
			List<XXPolicyItem> returnList = getEntityManager()
					.createNamedQuery("XXPolicyItem.findByServiceId", tClass)
					.setParameter("serviceId", serviceId).getResultList();
			if (returnList == null) {
				return new ArrayList<XXPolicyItem>();
			}
			return returnList;
		} catch (NoResultException e) {
			return new ArrayList<XXPolicyItem>();
		}
	}

	public void deleteByPolicyId(Long policyId) {
		if(policyId == null) {
			return;
		}
		getEntityManager()
			.createNamedQuery("XXPolicyItem.deleteByPolicyId", tClass)
			.setParameter("policyId", policyId).executeUpdate();
	}
}