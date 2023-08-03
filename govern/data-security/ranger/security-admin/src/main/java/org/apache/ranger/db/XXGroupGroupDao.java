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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.NoResultException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.ranger.common.db.BaseDao;
import org.apache.ranger.entity.XXGroupGroup;
import org.springframework.stereotype.Service;

@Service
public class XXGroupGroupDao extends BaseDao<XXGroupGroup> {

	private static final Logger logger = LoggerFactory.getLogger(XXGroupGroupDao.class);

	public XXGroupGroupDao( RangerDaoManagerBase daoManager ) {
		super(daoManager);
    }
    public List<XXGroupGroup> findByGroupId(Long groupId) {
		if (groupId == null) {
			return new ArrayList<XXGroupGroup>();
		}
		try {
			return getEntityManager().createNamedQuery("XXGroupGroup.findByGroupId", tClass)
					.setParameter("groupId", groupId)
					.setParameter("parentGroupId", groupId)
					.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<XXGroupGroup>();
		}
	}

	public Set<String> findGroupNamesByGroupName(String groupName) {
		List<String> groupList = null;

		if (groupName != null) {
			try {
				groupList = getEntityManager().createNamedQuery("XXGroupGroup.findGroupNamesByGroupName", String.class).setParameter("groupName", groupName).getResultList();
			} catch (NoResultException e) {
				logger.debug(e.getMessage());
			}
		} else {
			logger.debug("GroupName not provided...");
		}

		if(groupList != null) {
			return new HashSet<String>(groupList);
		}

		return new HashSet<String>();
	}
}

