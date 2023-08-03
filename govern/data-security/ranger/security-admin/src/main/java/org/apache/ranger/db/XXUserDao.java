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

import javax.persistence.NoResultException;

import org.apache.ranger.common.db.BaseDao;
import org.apache.ranger.entity.XXUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

@Service
public class XXUserDao extends BaseDao<XXUser> {
	private static final Logger logger = LoggerFactory.getLogger(XXResourceDao.class);

	public XXUserDao(RangerDaoManagerBase daoManager) {
		super(daoManager);
	}

	public XXUser findByUserName(String name) {
		if (daoManager.getStringUtil().isEmpty(name)) {
			logger.debug("name is empty");
			return null;
		}
		try {
			return getEntityManager()
					.createNamedQuery("XXUser.findByUserName", XXUser.class)
					.setParameter("name", name.trim())
					.getSingleResult();
		} catch (NoResultException e) {
			// ignore
		}
		return null;
	}

	public XXUser findByPortalUserId(Long portalUserId) {
		if (portalUserId == null) {
			return null;
		}
		try {
			return getEntityManager().createNamedQuery("XXUser.findByPortalUserId", tClass)
					.setParameter("portalUserId", portalUserId).getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	public Map<String, Set<String>> findGroupsByUserIds() {
		Map<String, Set<String>> userGroups = new HashMap<>();

		try {
			List<Object[]> rows = (List<Object[]>) getEntityManager()
					.createNamedQuery("XXUser.findGroupsByUserIds")
					.getResultList();
			if (rows != null) {
				for (Object[] row : rows) {
					if (userGroups.containsKey((String)row[0])) {
						userGroups.get((String)row[0]).add((String)row[1]);
					} else {
						Set<String> groups = new HashSet<>();
						groups.add((String)row[1]);
						userGroups.put((String)row[0], groups);
					}
				}
			}
		} catch (NoResultException e) {
			//Ignore
		}
		return userGroups;
	}

	public Map<String, Long> getAllUserIds() {
		Map<String, Long> users = new HashMap<>();
		try {
			List<Object[]> rows = (List<Object[]>) getEntityManager().createNamedQuery("XXUser.getAllUserIds").getResultList();
			if (rows != null) {
				for (Object[] row : rows) {
					users.put((String)row[0], (Long)row[1]);
				}
			}
		} catch (NoResultException e) {
			if (logger.isDebugEnabled()) {
				logger.debug(e.getMessage());
			}
		}
		return users;
	}

	public List<Object[]> getAllUserIdNames() {
		List<Object[]> users = new ArrayList<Object[]>();
		try {
			users = (List<Object[]>) getEntityManager().createNamedQuery("XXUser.getAllUserIdNames").getResultList();
		} catch (NoResultException e) {
			if (logger.isDebugEnabled()) {
				logger.debug(e.getMessage());
			}
		}
		return users;
	}

}
