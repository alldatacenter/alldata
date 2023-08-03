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
import org.apache.ranger.entity.XXEnumElementDef;
import org.springframework.stereotype.Service;

@Service
public class XXEnumElementDefDao extends BaseDao<XXEnumElementDef> {

	public XXEnumElementDefDao(RangerDaoManagerBase daoManager) {
		super(daoManager);
	}

	public List<XXEnumElementDef> findByEnumDefId(Long enumDefId) {
		if(enumDefId == null) {
			return new ArrayList<XXEnumElementDef>();
		}
		try {
			List<XXEnumElementDef> returnList = getEntityManager()
					.createNamedQuery("XXEnumElementDef.findByEnumDefId", tClass)
					.setParameter("enumDefId", enumDefId).getResultList();
			return returnList;
		} catch (NoResultException e) {
			return new ArrayList<XXEnumElementDef>();
		}
	}

}
