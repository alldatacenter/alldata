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
import java.util.Collections;
import java.util.List;

import javax.persistence.NoResultException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.ranger.biz.RangerPolicyRetriever;
import org.apache.ranger.common.db.BaseDao;
import org.apache.ranger.entity.XXPolicyRefCondition;
import org.springframework.stereotype.Service;

@Service
public class XXPolicyRefConditionDao extends BaseDao<XXPolicyRefCondition>  {

	public XXPolicyRefConditionDao(RangerDaoManagerBase daoManager) {
		super(daoManager);
	}

	public List<XXPolicyRefCondition> findByPolicyId(Long polId) {
		if(polId == null) {
			return Collections.EMPTY_LIST;
		}
		try {
			return getEntityManager()
					.createNamedQuery("XXPolicyRefCondition.findByPolicyId", tClass)
					.setParameter("policyId", polId).getResultList();
		} catch (NoResultException e) {
			return Collections.EMPTY_LIST;
		}
	}
	public List<XXPolicyRefCondition> findByConditionName(String conditionName) {
		if (conditionName == null) {
			return Collections.EMPTY_LIST;
		}
		try {
			return getEntityManager().createNamedQuery("XXPolicyRefCondition.findByConditionName", tClass)
					.setParameter("conditionName", conditionName).getResultList();
		} catch (NoResultException e) {
			return Collections.EMPTY_LIST;
		}
	}

	public List<XXPolicyRefCondition> findByConditionDefId(Long conditionDefId) {
		if (conditionDefId == null) {
			return Collections.EMPTY_LIST;
		}
		try {
			return getEntityManager().createNamedQuery("XXPolicyRefCondition.findByConditionDefId", tClass)
					.setParameter("conditionDefId", conditionDefId)
					.getResultList();
		} catch (NoResultException e) {
			return Collections.EMPTY_LIST;
		}
	}

	@SuppressWarnings("unchecked")
    public List<RangerPolicyRetriever.PolicyTextNameMap> findUpdatedConditionNamesByPolicy(Long policyId) {
        List<RangerPolicyRetriever.PolicyTextNameMap> ret = new ArrayList<>();
        if (policyId != null) {
            List<Object[]> rows = (List<Object[]>) getEntityManager()
                    .createNamedQuery("XXPolicyRefCondition.findUpdatedConditionNamesByPolicy")
                    .setParameter("policy", policyId)
                    .getResultList();
            if (rows != null) {
                for (Object[] row : rows) {
                    ret.add(new RangerPolicyRetriever.PolicyTextNameMap((Long)row[0], (String)row[1], (String)row[2]));
                }
            }
        }
        return ret;
    }

	@SuppressWarnings("unchecked")
	public List<RangerPolicyRetriever.PolicyTextNameMap> findUpdatedConditionNamesByService(Long serviceId) {
        List<RangerPolicyRetriever.PolicyTextNameMap> ret = new ArrayList<>();
        if (serviceId != null) {
            List<Object[]> rows = (List<Object[]>) getEntityManager()
                    .createNamedQuery("XXPolicyRefCondition.findUpdatedConditionNamesByService")
                    .setParameter("service", serviceId)
                    .getResultList();
            if (rows != null) {
                for (Object[] row : rows) {
                    ret.add(new RangerPolicyRetriever.PolicyTextNameMap((Long)row[0], (String)row[1], (String)row[2]));
                }
            }
        }
        return ret;
    }

	public void deleteByPolicyId(Long policyId) {
		if(policyId == null) {
			return;
		}

		// First select ids according to policyId, then delete records according to ids
		// The purpose of dividing the delete sql into these two steps is to avoid deadlocks at rr isolation level
		List<Long> ids = getEntityManager()
				.createNamedQuery("XXPolicyRefCondition.findIdsByPolicyId", Long.class)
				.setParameter("policyId", policyId)
				.getResultList();

		if (CollectionUtils.isEmpty(ids)) {
			return;
		}

		batchDeleteByIds("XXPolicyRefCondition.deleteByIds", ids, "ids");
	}
}
