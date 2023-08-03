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

import org.apache.ranger.common.DateUtil;
import org.apache.ranger.common.db.BaseDao;
import org.apache.ranger.entity.XXUgsyncAuditInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.persistence.NoResultException;
import java.util.List;

/**
 */

@Service
public class XXUgsyncAuditInfoDao extends BaseDao<XXUgsyncAuditInfo> {
	protected static final Logger logger = LoggerFactory
			.getLogger(XXUgsyncAuditInfoDao.class);
	/**
	 * Default Constructor
	 */
	public XXUgsyncAuditInfoDao(RangerDaoManagerBase daoManager) {
		super(daoManager);
	}

	@Override
	public XXUgsyncAuditInfo create(XXUgsyncAuditInfo obj) {
		obj.setEventTime(DateUtil.getUTCDate());
		return super.create(obj);
	}

	public XXUgsyncAuditInfo findBySessionId(String sessionId) {
		if (sessionId == null) {
			return null;
		}
		try {
			return getEntityManager()
					.createNamedQuery("XXUgsyncAuditInfo.findBySessionId", tClass)
					.setParameter("sessionId", sessionId)
					.getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}
	public List<XXUgsyncAuditInfo> findBySyncSource(String syncSource) {
		if (syncSource == null) {
			return null;
		}
		try {
			return getEntityManager()
					.createNamedQuery("XXUgsyncAuditInfo.findBySyncSource", tClass)
					.setParameter("syncSource", syncSource).getResultList();
		} catch (NoResultException e) {
			return null;
		}
	}
}
