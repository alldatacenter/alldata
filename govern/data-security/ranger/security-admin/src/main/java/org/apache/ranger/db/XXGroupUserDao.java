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


import java.util.*;

import javax.persistence.NoResultException;

import org.apache.commons.lang.StringUtils;
import org.apache.ranger.common.db.BaseDao;
import org.apache.ranger.entity.XXGroupUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class XXGroupUserDao extends BaseDao<XXGroupUser> {
	private static final Logger logger = LoggerFactory.getLogger(XXGroupUserDao.class);

	public XXGroupUserDao(RangerDaoManagerBase daoManager) {
		super(daoManager);
	}

	public void deleteByGroupIdAndUserId(Long groupId, Long userId) {
		getEntityManager()
				.createNamedQuery("XXGroupUser.deleteByGroupIdAndUserId")
				.setParameter("userId", userId)
				.setParameter("parentGroupId", groupId).executeUpdate();

	}

	public List<XXGroupUser> findByUserId(Long userId) {
		if (userId != null) {
			try {
				return getEntityManager()
						.createNamedQuery("XXGroupUser.findByUserId", XXGroupUser.class)
						.setParameter("userId", userId)
						.getResultList();
			} catch (NoResultException e) {
				logger.debug(e.getMessage());
			}
		} else {
			logger.debug("ResourceId not provided.");
			return new ArrayList<XXGroupUser>();
		}
		return null;
	}

	/**
	 * @param xUserId
	 *            -- Id of X_USER table
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Long> findGroupIdListByUserId(Long xUserId) {
		if (xUserId != null) {
			try {
				return getEntityManager().createNamedQuery("XXGroupUser.findGroupIdListByUserId").setParameter("xUserId", xUserId).getResultList();
			} catch (NoResultException e) {
				logger.debug(e.getMessage());
			}
		} else {
			logger.debug("UserId not provided.");
			return new ArrayList<Long>();
		}
		return null;
	}

	public Set<String> findGroupNamesByUserName(String userName) {
		List<String> groupList = null;

		if (userName != null) {
			try {
				groupList = getEntityManager().createNamedQuery("XXGroupUser.findGroupNamesByUserName", String.class).setParameter("userName", userName).getResultList();
			} catch (NoResultException e) {
				logger.debug(e.getMessage());
			}
		} else {
			logger.debug("UserId not provided.");
		}

		if(groupList != null) {
			return new HashSet<String>(groupList);
		}

		return new HashSet<String>();
	}

	public List<XXGroupUser> findByGroupId(Long groupId) {
		if (groupId == null) {
			return new ArrayList<XXGroupUser>();
		}
		try {
			return getEntityManager().createNamedQuery("XXGroupUser.findByGroupId", tClass).setParameter("groupId", groupId).getResultList();
		} catch (NoResultException e) {
			return new ArrayList<XXGroupUser>();
		}
	}

	public XXGroupUser findByGroupNameAndUserId(String groupName, Long userId) {
		if (StringUtils.isNotBlank(groupName) && userId != null) {
			try {
				return getEntityManager()
						.createNamedQuery("XXGroupUser.findByGroupNameAndUserId", XXGroupUser.class)
						.setParameter("userId", userId)
						.setParameter("groupName", groupName)
						.getSingleResult();
			} catch (NoResultException e) {
				logger.debug(e.getMessage());
			}
		} else {
			logger.debug("userId and/or groupId not provided.");
			return new XXGroupUser();
		}
		return null;
	}

	public Map<String, Set<String>> findUsersByGroupIds() {
		Map<String, Set<String>> groupUsers = new HashMap<>();

		try {
			List<Object[]> rows = (List<Object[]>) getEntityManager()
					.createNamedQuery("XXGroupUser.findUsersByGroupIds")
					.getResultList();
			if (rows != null) {
				for (Object[] row : rows) {
					if (groupUsers.containsKey((String)row[0])) {
						groupUsers.get((String)row[0]).add((String)row[1]);
					} else {
						Set<String> users = new HashSet<>();
						users.add((String)row[1]);
						groupUsers.put((String)row[0], users);
					}
				}
			}
		} catch (NoResultException e) {
			//Ignore
		}
		return groupUsers;
	}

	public Map<String, XXGroupUser> findUsersByGroupName(String groupName) {
		Map<String, XXGroupUser> users = new HashMap<>();

		if (StringUtils.isNotBlank(groupName)) {
			try {
				List<Object[]> rows = (List<Object[]>) getEntityManager()
						.createNamedQuery("XXGroupUser.findUsersByGroupName")
						.setParameter("groupName", groupName)
						.getResultList();
				if (rows != null) {
					for (Object[] row : rows) {
							users.put((String)row[0], (XXGroupUser)row[1]);
					}
				}
			} catch (NoResultException e) {
				if (logger.isDebugEnabled()) {
					logger.debug(e.getMessage());
				}
			}
		}

		return users;
	}
}
