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

 package org.apache.ranger.biz;

import org.apache.ranger.common.MessageEnums;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.common.SearchCriteria;
import org.apache.ranger.service.XPortalUserService;
import org.apache.ranger.view.VXLong;
import org.apache.ranger.view.VXPortalUser;
import org.apache.ranger.view.VXPortalUserList;
import org.springframework.beans.factory.annotation.Autowired;
public class UserMgrBase {

	@Autowired
	RESTErrorUtil restErrorUtil;

	@Autowired
	XPortalUserService xPortalUserService;
	public VXPortalUser getXPortalUser(Long id){
		return (VXPortalUser)xPortalUserService.readResource(id);
	}

	public VXPortalUser createXPortalUser(VXPortalUser vXPortalUser){
		vXPortalUser =  (VXPortalUser)xPortalUserService.createResource(vXPortalUser);
		return vXPortalUser;
	}

	public VXPortalUser updateXPortalUser(VXPortalUser vXPortalUser) {
		vXPortalUser =  (VXPortalUser)xPortalUserService.updateResource(vXPortalUser);
		return vXPortalUser;
	}

	public void deleteXPortalUser(Long id, boolean force) {
		 if (force) {
			 xPortalUserService.deleteResource(id);
		 } else {
			 throw restErrorUtil.createRESTException(
				"serverMsg.modelMgrBaseDeleteModel",
				MessageEnums.OPER_NOT_ALLOWED_FOR_ENTITY);
		 }
	}

	public VXPortalUserList searchXPortalUsers(SearchCriteria searchCriteria) {
		return xPortalUserService.searchXPortalUsers(searchCriteria);
	}

	public VXLong getXPortalUserSearchCount(SearchCriteria searchCriteria) {
		return xPortalUserService.getSearchCount(searchCriteria,
				xPortalUserService.searchFields);
	}

}
