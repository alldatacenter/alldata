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

import java.util.Collections;
import java.util.List;

import javax.persistence.NoResultException;

import org.apache.ranger.common.db.BaseDao;
import org.apache.ranger.entity.XXSecurityZoneRefResource;

public class XXSecurityZoneRefResourceDao extends BaseDao<XXSecurityZoneRefResource>{

	public XXSecurityZoneRefResourceDao(RangerDaoManagerBase daoManager) {
		super(daoManager);
	}

	public List<XXSecurityZoneRefResource> findByZoneId(Long zoneId) {
        if (zoneId == null) {
            return null;
        }
        try {
        	List<XXSecurityZoneRefResource> xxZoneRefResource = getEntityManager()
                    .createNamedQuery("XXSecurityZoneRefResource.findByZoneId", tClass)
                    .setParameter("zoneId", zoneId)
                    .getResultList();
            return xxZoneRefResource;
        } catch (NoResultException e) {
            return null;
        }
    }

	public List<XXSecurityZoneRefResource> findByResourceDefId(Long resourceDefId) {
		if (resourceDefId == null) {
			return Collections.emptyList();
		}
		try {
			return getEntityManager().createNamedQuery("XXSecurityZoneRefResource.findByResourceDefId", tClass)
					.setParameter("resourceDefId", resourceDefId).getResultList();
		} catch (NoResultException e) {
			return Collections.emptyList();
		}
	}
}
