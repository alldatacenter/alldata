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

import org.apache.commons.collections.CollectionUtils;
import org.apache.ranger.common.db.BaseDao;
import org.apache.ranger.entity.XXRoleRefUser;
import org.springframework.stereotype.Service;

@Service
public class XXRoleRefUserDao extends BaseDao<XXRoleRefUser>{

    public XXRoleRefUserDao(RangerDaoManagerBase daoManager)  {
        super(daoManager);
    }

    public List<XXRoleRefUser> findByRoleId(Long roleId) {
        if(roleId == null) {
            return Collections.EMPTY_LIST;
        }
        try {
            return getEntityManager()
                    .createNamedQuery("XXRoleRefUser.findByRoleId", tClass)
                    .setParameter("roleId", roleId).getResultList();
        } catch (NoResultException e) {
            return Collections.EMPTY_LIST;
        }
    }

    public List<Long> findIdsByRoleId(Long roleId) {
        List<Long> ret = Collections.EMPTY_LIST;

        if (roleId != null) {
            try {
                ret = getEntityManager()
                        .createNamedQuery("XXRoleRefUser.findIdsByRoleId", Long.class)
                        .setParameter("roleId", roleId).getResultList();
            } catch (NoResultException e) {
                ret = Collections.EMPTY_LIST;
            }
        }

        return ret;
    }

    public List<XXRoleRefUser> findByUserId(Long userId) {
        if(userId == null) {
            return Collections.EMPTY_LIST;
        }
        try {
            return getEntityManager()
                    .createNamedQuery("XXRoleRefUser.findByUserId", tClass)
                    .setParameter("userId", userId).getResultList();
        } catch (NoResultException e) {
            return Collections.EMPTY_LIST;
        }
    }

    public List<XXRoleRefUser> findByUserName(String userName) {
        if (userName == null) {
            return Collections.EMPTY_LIST;
        }
        try {
            return getEntityManager().createNamedQuery("XXRoleRefUser.findByUserName", tClass)
                    .setParameter("userName", userName).getResultList();
        } catch (NoResultException e) {
            return Collections.EMPTY_LIST;
        }
    }

    public void deleteRoleRefUserByIds(List<Long> ids) {
        if (CollectionUtils.isNotEmpty(ids)) {
            batchDeleteByIds("XXRoleRefUser.deleteRoleRefUserByIds", ids, "ids");
        }
    }
}