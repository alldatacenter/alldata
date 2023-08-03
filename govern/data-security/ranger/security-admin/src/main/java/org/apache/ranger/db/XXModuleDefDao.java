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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.NoResultException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.ranger.common.RangerCommonEnums;
import org.apache.ranger.common.db.BaseDao;
import org.apache.ranger.entity.XXModuleDef;
import org.springframework.stereotype.Service;

@Service
public class XXModuleDefDao extends BaseDao<XXModuleDef>{

	public XXModuleDefDao(RangerDaoManagerBase daoManager) {
		super(daoManager);
	}

	public XXModuleDef findByModuleName(String moduleName){
		if (moduleName == null) {
			return null;
		}
		try {

			return (XXModuleDef) getEntityManager()
					.createNamedQuery("XXModuleDef.findByModuleName")
					.setParameter("moduleName", moduleName)
					.getSingleResult();
		} catch (Exception e) {

		}
		return null;
	}


	public XXModuleDef  findByModuleId(Long id) {
		if(id == null) {
			return new XXModuleDef();
		}
		try {
			List<XXModuleDef> xxModuelDefs=getEntityManager()
					.createNamedQuery("XXModuleDef.findByModuleId", tClass)
					.setParameter("id", id).getResultList();
			return xxModuelDefs.get(0);
		} catch (NoResultException e) {
			return new XXModuleDef();
		}
	}

	@SuppressWarnings("unchecked")
	public List<String>  findModuleURLOfPemittedModules(Long userId) {
		try {

			String query="select";
			query+=" url";
			query+=" FROM";
			query+="   x_modules_master";
			query+=" WHERE";
			query+="  url NOT IN (SELECT ";
			query+="    moduleMaster.url";
			query+=" FROM";
			query+=" x_modules_master moduleMaster,";
			query+=" x_user_module_perm userModulePermission";
			query+=" WHERE";
			query+=" moduleMaster.id = userModulePermission.module_id";
			query+=" AND userModulePermission.user_id = "+userId+")";
			query+=" AND ";
			query+=" id NOT IN (SELECT DISTINCT";
			query+=" gmp.module_id";
			query+=" FROM";
			query+=" x_group_users xgu,";
			query+=" x_user xu,";
			query+=" x_group_module_perm gmp,";
			query+=" x_portal_user xpu";
			query+=" WHERE";
			query+=" xu.user_name = xpu.login_id";
			query+=" AND xu.id = xgu.user_id";
			query+=" AND xgu.p_group_id = gmp.group_id";
			query+=" AND xpu.id = "+userId+")";

			return getEntityManager()
					.createNativeQuery(query)
					.getResultList();

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public List<String> findAccessibleModulesByGroupIdList(List<Long> grpIdList) {
		if (CollectionUtils.isEmpty(grpIdList)) {
			return new ArrayList<String>();
		}
		try {
			return getEntityManager().createNamedQuery("XXModuleDef.findAccessibleModulesByGroupId").setParameter("grpIdList", grpIdList)
					.setParameter("isAllowed", RangerCommonEnums.ACCESS_RESULT_ALLOWED).getResultList();
		} catch (NoResultException e) {
			return new ArrayList<String>();
		}
	}

	/**
	 * @param portalUserId
	 * @param xUserId
	 * @return This function will return all the modules accessible for particular user, considering all the groups as well in which that user belongs
	 */
	@SuppressWarnings("unchecked")
	public List<String> findAccessibleModulesByUserId(Long portalUserId, Long xUserId) {
		if (portalUserId == null || xUserId == null) {
			return new ArrayList<String>();
		}
		try {

			List<String> userPermList = getEntityManager().createNamedQuery("XXModuleDef.findAllAccessibleModulesByUserId").setParameter("portalUserId", portalUserId)
					.setParameter("xUserId", xUserId).setParameter("isAllowed", RangerCommonEnums.ACCESS_RESULT_ALLOWED).getResultList();

			return userPermList;

		} catch (NoResultException e) {
			return new ArrayList<String>();
		}
	}

}
