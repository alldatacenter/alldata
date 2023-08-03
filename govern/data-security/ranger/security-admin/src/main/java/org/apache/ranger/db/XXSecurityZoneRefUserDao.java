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
import org.apache.ranger.entity.XXSecurityZoneRefUser;

public class XXSecurityZoneRefUserDao extends BaseDao<XXSecurityZoneRefUser>{

	public XXSecurityZoneRefUserDao(RangerDaoManagerBase daoManager) {
		super(daoManager);
	}

	public List<XXSecurityZoneRefUser> findByZoneId(Long zoneId) {
        if (zoneId == null) {
            return null;
        }
        try {
        	List<XXSecurityZoneRefUser> xxZoneRefService = getEntityManager()
                    .createNamedQuery("XXSecurityZoneRefUser.findByZoneId", tClass)
                    .setParameter("zoneId", zoneId)
                    .getResultList();
            return xxZoneRefService;
        } catch (NoResultException e) {
            return null;
        }
    }

	public List<XXSecurityZoneRefUser> findAdminUsersByZoneId(Long zoneId) {
        if (zoneId == null) {
            return null;
        }
        try {
        	List<XXSecurityZoneRefUser> xxZoneRefService = getEntityManager()
                    .createNamedQuery("XXSecurityZoneRefUser.findUserTypeByZoneId", tClass)
                    .setParameter("zoneId", zoneId)
                    .setParameter("userType", "1")
                    .getResultList();
            return xxZoneRefService;
        } catch (NoResultException e) {
            return null;
        }
    }

	public List<XXSecurityZoneRefUser> findAuditUsersByZoneId(Long zoneId) {
        if (zoneId == null) {
            return null;
        }
        try {
        	List<XXSecurityZoneRefUser> xxZoneRefService = getEntityManager()
                    .createNamedQuery("XXSecurityZoneRefUser.findUserTypeByZoneId", tClass)
                    .setParameter("zoneId", zoneId)
                    .setParameter("userType", "0")
                    .getResultList();
            return xxZoneRefService;
        } catch (NoResultException e) {
            return null;
        }
    }

	public List<XXSecurityZoneRefUser> findByUserId(Long userId) {
		if (userId == null) {
			return Collections.emptyList();
		}
		try {
			return getEntityManager().createNamedQuery("XXSecurityZoneRefUser.findByUserId", tClass)
					.setParameter("userId", userId).getResultList();
		} catch (NoResultException e) {
			return Collections.emptyList();
		}
	}
}
