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
import org.apache.ranger.entity.XXContextEnricherDef;
import org.springframework.stereotype.Service;

@Service
public class XXContextEnricherDefDao extends BaseDao<XXContextEnricherDef> {

	public XXContextEnricherDefDao(RangerDaoManagerBase daoManager) {
		super(daoManager);
	}

	public List<XXContextEnricherDef> findByServiceDefId(Long serviceDefId) {
		if (serviceDefId == null) {
			return new ArrayList<XXContextEnricherDef>();
		}
		try {
			List<XXContextEnricherDef> retList = getEntityManager()
					.createNamedQuery("XXContextEnricherDef.findByServiceDefId", tClass)
					.setParameter("serviceDefId", serviceDefId).getResultList();
			return retList;
		} catch (NoResultException e) {
			return new ArrayList<XXContextEnricherDef>();
		}
	}

	public XXContextEnricherDef findByServiceDefIdAndName(Long serviceDefId, String name) {
		if (serviceDefId == null) {
			return null;
		}
		try {
			XXContextEnricherDef retList = getEntityManager()
					.createNamedQuery("XXContextEnricherDef.findByServiceDefIdAndName", tClass)
					.setParameter("serviceDefId", serviceDefId)
					.setParameter("name", name).getSingleResult();
			return retList;
		} catch (NoResultException e) {
			return null;
		}
	}
}
