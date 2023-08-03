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
import org.apache.ranger.entity.XXDataMaskTypeDef;
import org.springframework.stereotype.Service;

@Service
public class XXDataMaskTypeDefDao extends BaseDao<XXDataMaskTypeDef> {

	public XXDataMaskTypeDefDao(RangerDaoManagerBase daoManager) {
		super(daoManager);
	}

	public List<XXDataMaskTypeDef> findByServiceDefId(Long serviceDefId) {
		if (serviceDefId == null) {
			return new ArrayList<XXDataMaskTypeDef>();
		}
		try {
			List<XXDataMaskTypeDef> retList = getEntityManager()
					.createNamedQuery("XXDataMaskTypeDef.findByServiceDefId", tClass)
					.setParameter("serviceDefId", serviceDefId).getResultList();
			return retList;
		} catch (NoResultException e) {
			return new ArrayList<XXDataMaskTypeDef>();
		}
	}

	public XXDataMaskTypeDef findByNameAndServiceId(String name, Long serviceId) {
		if(name == null || serviceId == null) {
			return null;
		}
		try {
			return getEntityManager()
					.createNamedQuery("XXDataMaskTypeDef.findByNameAndServiceId", tClass)
					.setParameter("name", name).setParameter("serviceId", serviceId)
					.getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}
}
