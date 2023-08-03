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

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.common.db.BaseDao;
import org.apache.ranger.entity.XXRole;
import org.springframework.stereotype.Service;

import javax.persistence.NoResultException;
import java.util.ArrayList;
import java.util.List;

@Service
public class XXRoleDao extends BaseDao<XXRole> {
    /**
     * Default Constructor
     */
    public XXRoleDao(RangerDaoManagerBase daoManager) {
        super(daoManager);
    }
    public XXRole findByRoleId(Long roleId) {
        if (roleId == null) {
            return null;
        }
        try {
            XXRole xxRole = getEntityManager()
                    .createNamedQuery("XXRole.findByRoleId", tClass)
                    .setParameter("roleId", roleId)
                    .getSingleResult();
            return xxRole;
        } catch (NoResultException e) {
            return null;
        }
    }
    public XXRole findByRoleName(String roleName) {
        if (StringUtils.isBlank(roleName)) {
            return null;
        }
        try {
            XXRole xxRole = getEntityManager()
                    .createNamedQuery("XXRole.findByRoleName", tClass)
                    .setParameter("roleName", roleName)
                    .getSingleResult();
            return xxRole;
        } catch (NoResultException e) {
            return null;
        }
    }
    public List<XXRole> findByServiceId(Long serviceId) {
        List<XXRole> ret;
        try {
            ret = getEntityManager()
                    .createNamedQuery("XXRole.findByServiceId", tClass)
                    .setParameter("serviceId", serviceId)
                    .getResultList();
        } catch (NoResultException e) {
            ret = ListUtils.EMPTY_LIST;
        }
        return ret;
    }

    public List<String> findRoleNamesByServiceId(Long serviceId) {
        List<String> ret;
        try {
            ret = getEntityManager()
                    .createNamedQuery("XXRole.findRoleNamesByServiceId", String.class)
                    .setParameter("serviceId", serviceId)
                    .getResultList();
        } catch (NoResultException e) {
            ret = ListUtils.EMPTY_LIST;
        }
        return ret;
    }

    public List<String> getAllNames() {
        try {
            return getEntityManager().createNamedQuery("XXRole.getAllNames", String.class).getResultList();
        } catch (NoResultException e) {
            return new ArrayList<String>();
        }
    }

    @SuppressWarnings("unchecked")
	public List<XXRole> findByUserId(Long UserId) {
		if (UserId == null) {
			return null;
		}
		List<XXRole> ret;
		try {
			ret = getEntityManager().createNamedQuery("XXRole.findByUserId", tClass).setParameter("userId", UserId)
					.getResultList();
		} catch (NoResultException e) {
			ret = ListUtils.EMPTY_LIST;
		}
		return ret;
	}

    @SuppressWarnings("unchecked")
    public List<XXRole> findByGroupId(Long groupId) {
        if (groupId == null) {
            return null;
        }
        List<XXRole> ret;
        try {
            ret = getEntityManager().createNamedQuery("XXRole.findByGroupId", tClass).setParameter("groupId", groupId)
                    .getResultList();
        } catch (NoResultException e) {
            ret = ListUtils.EMPTY_LIST;
        }
        return ret;
    }
}

