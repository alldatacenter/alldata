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

import java.util.Date;
import java.util.List;

import javax.persistence.NoResultException;

import org.apache.ranger.common.DateUtil;
import org.apache.ranger.common.db.BaseDao;
import org.apache.ranger.entity.XXAuthSession;
import org.springframework.stereotype.Service;

@Service
public class XXAuthSessionDao extends BaseDao<XXAuthSession> {

    public XXAuthSessionDao( RangerDaoManagerBase daoManager ) {
		super(daoManager);
    }

    @SuppressWarnings("unchecked")
	public List<Object[]> getUserLoggedIn(){
    	return getEntityManager()
    			.createNamedQuery("XXAuthSession.getUserLoggedIn")
    			.getResultList();
    }
	
	public XXAuthSession getAuthSessionBySessionId(String sessionId){
		try{
	    	return (XXAuthSession) getEntityManager()
	    			.createNamedQuery("XXAuthSession.getAuthSessionBySessionId")
	    			.setParameter("sessionId", sessionId)
	    			.getSingleResult();
		} catch(NoResultException ignoreNoResultFound) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public List<XXAuthSession> getAuthSessionByUserId(Long userId){
		try{
			return getEntityManager()
					.createNamedQuery("XXAuthSession.getAuthSessionByUserId")
					.setParameter("userId", userId)
					.getResultList();
		} catch(NoResultException ignoreNoResultFound) {
			return null;
		}
	}

	public long getRecentAuthFailureCountByLoginId(String loginId, int timeRangezSecond){
		Date authWindowStartTime = new Date(DateUtil.getUTCDate().getTime() - timeRangezSecond * 1000);

		return getEntityManager()
				.createNamedQuery("XXAuthSession.getRecentAuthFailureCountByLoginId", Long.class)
				.setParameter("loginId", loginId)
				.setParameter("authWindowStartTime", authWindowStartTime)
				.getSingleResult();
	}

}

