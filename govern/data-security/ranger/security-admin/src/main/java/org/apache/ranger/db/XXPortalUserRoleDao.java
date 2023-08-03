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
import java.util.List;

import javax.persistence.NoResultException;

import org.apache.ranger.common.db.BaseDao;
import org.apache.ranger.entity.XXPortalUserRole;
import org.springframework.stereotype.Service;

@Service
public class XXPortalUserRoleDao extends BaseDao<XXPortalUserRole> {

	public XXPortalUserRoleDao(RangerDaoManagerBase daoManager) {
		super(daoManager);
	}

	@SuppressWarnings("unchecked")
	public List<XXPortalUserRole> findByUserId(Long userId) {
		if (userId == null) {
			return new ArrayList<XXPortalUserRole>();
		}
		return getEntityManager().createNamedQuery("XXPortalUserRole.findByUserId")
				.setParameter("userId", userId).getResultList();
	}
	
	public XXPortalUserRole findByRoleUserId(Long userId, String role) {
		if(userId == null || role == null || role.isEmpty()){
			return null;
		}
		try{
			return (XXPortalUserRole)getEntityManager().createNamedQuery("XXPortalUserRole.findByRoleUserId")
					.setParameter("userId", userId)
					.setParameter("userRole", role).getSingleResult();
		} catch(NoResultException e){
			//doNothing;
		}
		return null;
	}
	@SuppressWarnings("unchecked")
	public List<String> findXPortalUserRolebyXPortalUserId(Long userId) {
		if (userId == null) {
			return new ArrayList<String>();
		}
		try {
			List<String> returnList = getEntityManager()
					.createNamedQuery("XXPortalUserRole.findXPortalUserRolebyXPortalUserId")
					.setParameter("userId", userId).getResultList();
			return returnList;
		} catch (NoResultException e) {
			return new ArrayList<String>();
		}
	}
}
