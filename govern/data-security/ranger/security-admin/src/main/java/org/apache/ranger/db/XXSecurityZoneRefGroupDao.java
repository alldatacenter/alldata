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
import org.apache.ranger.entity.XXSecurityZoneRefGroup;

public class XXSecurityZoneRefGroupDao extends BaseDao<XXSecurityZoneRefGroup>{

	public XXSecurityZoneRefGroupDao(RangerDaoManagerBase daoManager) {
		super(daoManager);
	}

	public List<XXSecurityZoneRefGroup> findByZoneId(Long zoneId) {
        if (zoneId == null) {
            return null;
        }
        try {
        	List<XXSecurityZoneRefGroup> xxZoneRefService = getEntityManager()
                    .createNamedQuery("XXSecurityZoneRefGroup.findByZoneId", tClass)
                    .setParameter("zoneId", zoneId)
                    .getResultList();
            return xxZoneRefService;
        } catch (NoResultException e) {
            return null;
        }
    }

	public List<XXSecurityZoneRefGroup> findByGroupId(Long groupId) {
		if (groupId == null) {
			return Collections.emptyList();
		}
		try {
			return getEntityManager().createNamedQuery("XXSecurityZoneRefGroup.findByGroupId", tClass)
					.setParameter("groupId", groupId).getResultList();
		} catch (NoResultException e) {
			return Collections.emptyList();
		}
	}

	public List<XXSecurityZoneRefGroup> findAdminGroupByZoneId(Long zoneId) {
        if (zoneId == null) {
            return null;
        }
        try {
        	List<XXSecurityZoneRefGroup> xxZoneRefGroup = getEntityManager()
                    .createNamedQuery("XXSecurityZoneRefGroup.findGroupTypeByZoneId", tClass)
                    .setParameter("zoneId", zoneId)
                    .setParameter("groupType", "1")
                    .getResultList();
            return xxZoneRefGroup;
        } catch (NoResultException e) {
            return null;
        }
    }

	public List<XXSecurityZoneRefGroup> findAuditGroupByZoneId(Long zoneId) {
        if (zoneId == null) {
            return null;
        }
        try {
        	List<XXSecurityZoneRefGroup> xxZoneRefGroup = getEntityManager()
                    .createNamedQuery("XXSecurityZoneRefGroup.findGroupTypeByZoneId", tClass)
                    .setParameter("zoneId", zoneId)
                    .setParameter("groupType", "0")
                    .getResultList();
            return xxZoneRefGroup;
        } catch (NoResultException e) {
            return null;
        }
    }
}
