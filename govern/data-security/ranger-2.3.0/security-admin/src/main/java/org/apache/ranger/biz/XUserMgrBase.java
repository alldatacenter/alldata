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
import org.apache.ranger.service.XAuditMapService;
import org.apache.ranger.service.XGroupGroupService;
import org.apache.ranger.service.XGroupPermissionService;
import org.apache.ranger.service.XGroupService;
import org.apache.ranger.service.XGroupUserService;
import org.apache.ranger.service.XModuleDefService;
import org.apache.ranger.service.XPermMapService;
import org.apache.ranger.service.XUserPermissionService;
import org.apache.ranger.service.XUserService;
import org.apache.ranger.view.VXAuditMap;
import org.apache.ranger.view.VXAuditMapList;
import org.apache.ranger.view.VXGroup;
import org.apache.ranger.view.VXGroupGroup;
import org.apache.ranger.view.VXGroupGroupList;
import org.apache.ranger.view.VXGroupList;
import org.apache.ranger.view.VXGroupPermissionList;
import org.apache.ranger.view.VXGroupUser;
import org.apache.ranger.view.VXGroupUserList;
import org.apache.ranger.view.VXLong;
import org.apache.ranger.view.VXModuleDefList;
import org.apache.ranger.view.VXPermMap;
import org.apache.ranger.view.VXPermMapList;
import org.apache.ranger.view.VXUser;
import org.apache.ranger.view.VXModulePermissionList;
import org.apache.ranger.view.VXUserList;
import org.apache.ranger.view.VXUserPermissionList;
import org.springframework.beans.factory.annotation.Autowired;
public class XUserMgrBase {

	@Autowired
	RESTErrorUtil restErrorUtil;

	@Autowired
	XGroupService xGroupService;

	@Autowired
	XUserService xUserService;

	@Autowired
	XGroupUserService xGroupUserService;

	@Autowired
	XGroupGroupService xGroupGroupService;

	@Autowired
	XPermMapService xPermMapService;

	@ Autowired
	XModuleDefService xModuleDefService;

	@ Autowired
	XUserPermissionService xUserPermissionService;

	@ Autowired
	XGroupPermissionService xGroupPermissionService;

	@Autowired
	XAuditMapService xAuditMapService;
	public VXGroup getXGroup(Long id){
		return (VXGroup)xGroupService.readResource(id);
	}

	public VXGroup createXGroup(VXGroup vXGroup){
		vXGroup =  (VXGroup)xGroupService.createResource(vXGroup);
		return vXGroup;
	}

	public VXGroup updateXGroup(VXGroup vXGroup) {
		vXGroup =  (VXGroup)xGroupService.updateResource(vXGroup);
		return vXGroup;
	}

	public void deleteXGroup(Long id, boolean force) {
		 if (force) {
			 xGroupService.deleteResource(id);
		 } else {
			 throw restErrorUtil.createRESTException(
				"serverMsg.modelMgrBaseDeleteModel",
				MessageEnums.OPER_NOT_ALLOWED_FOR_ENTITY);
		 }
	}

	public VXGroupList searchXGroups(SearchCriteria searchCriteria) {
		return xGroupService.searchXGroups(searchCriteria);
	}

	public VXLong getXGroupSearchCount(SearchCriteria searchCriteria) {
		return xGroupService.getSearchCount(searchCriteria,
				xGroupService.searchFields);
	}

	public VXUser getXUser(Long id){
		return (VXUser)xUserService.readResource(id);
	}

	public VXUser createXUser(VXUser vXUser){
		vXUser =  (VXUser)xUserService.createResource(vXUser);
		return vXUser;
	}

	public VXUser updateXUser(VXUser vXUser) {
		vXUser =  (VXUser)xUserService.updateResource(vXUser);
		return vXUser;
	}

	public void deleteXUser(Long id, boolean force) {
		 if (force) {
			 xUserService.deleteResource(id);
		 } else {
			 throw restErrorUtil.createRESTException(
				"serverMsg.modelMgrBaseDeleteModel",
				MessageEnums.OPER_NOT_ALLOWED_FOR_ENTITY);
		 }
	}

	public VXUserList searchXUsers(SearchCriteria searchCriteria) {
		return xUserService.searchXUsers(searchCriteria);
	}

	public VXLong getXUserSearchCount(SearchCriteria searchCriteria) {
		return xUserService.getSearchCount(searchCriteria,
				xUserService.searchFields);
	}

	public VXGroupUser getXGroupUser(Long id){
		return (VXGroupUser)xGroupUserService.readResource(id);
	}

	public VXGroupUser createXGroupUser(VXGroupUser vXGroupUser){
		vXGroupUser =  (VXGroupUser)xGroupUserService.createResource(vXGroupUser);
		return vXGroupUser;
	}

	public VXGroupUser updateXGroupUser(VXGroupUser vXGroupUser) {
		vXGroupUser =  (VXGroupUser)xGroupUserService.updateResource(vXGroupUser);
		return vXGroupUser;
	}

	public void deleteXGroupUser(Long id, boolean force) {
		 if (force) {
			 xGroupUserService.deleteResource(id);
		 } else {
			 throw restErrorUtil.createRESTException(
				"serverMsg.modelMgrBaseDeleteModel",
				MessageEnums.OPER_NOT_ALLOWED_FOR_ENTITY);
		 }
	}

	public VXGroupUserList searchXGroupUsers(SearchCriteria searchCriteria) {
		return xGroupUserService.searchXGroupUsers(searchCriteria);
	}

	public VXLong getXGroupUserSearchCount(SearchCriteria searchCriteria) {
		return xGroupUserService.getSearchCount(searchCriteria,
				xGroupUserService.searchFields);
	}

	public VXGroupGroup getXGroupGroup(Long id){
		return (VXGroupGroup)xGroupGroupService.readResource(id);
	}

	public VXGroupGroup createXGroupGroup(VXGroupGroup vXGroupGroup){
		vXGroupGroup =  (VXGroupGroup)xGroupGroupService.createResource(vXGroupGroup);
		return vXGroupGroup;
	}

	public VXGroupGroup updateXGroupGroup(VXGroupGroup vXGroupGroup) {
		vXGroupGroup =  (VXGroupGroup)xGroupGroupService.updateResource(vXGroupGroup);
		return vXGroupGroup;
	}

	public void deleteXGroupGroup(Long id, boolean force) {
		 if (force) {
			 xGroupGroupService.deleteResource(id);
		 } else {
			 throw restErrorUtil.createRESTException(
				"serverMsg.modelMgrBaseDeleteModel",
				MessageEnums.OPER_NOT_ALLOWED_FOR_ENTITY);
		 }
	}

	public VXGroupGroupList searchXGroupGroups(SearchCriteria searchCriteria) {
		return xGroupGroupService.searchXGroupGroups(searchCriteria);
	}

	public VXLong getXGroupGroupSearchCount(SearchCriteria searchCriteria) {
		return xGroupGroupService.getSearchCount(searchCriteria,
				xGroupGroupService.searchFields);
	}

	public VXPermMap getXPermMap(Long id){
		return (VXPermMap)xPermMapService.readResource(id);
	}

	public VXPermMap createXPermMap(VXPermMap vXPermMap){
		vXPermMap =  (VXPermMap)xPermMapService.createResource(vXPermMap);
		return vXPermMap;
	}

	public VXPermMap updateXPermMap(VXPermMap vXPermMap) {
		vXPermMap =  (VXPermMap)xPermMapService.updateResource(vXPermMap);
		return vXPermMap;
	}

	public void deleteXPermMap(Long id, boolean force) {
		 if (force) {
			 xPermMapService.deleteResource(id);
		 } else {
			 throw restErrorUtil.createRESTException(
				"serverMsg.modelMgrBaseDeleteModel",
				MessageEnums.OPER_NOT_ALLOWED_FOR_ENTITY);
		 }
	}

	public VXPermMapList searchXPermMaps(SearchCriteria searchCriteria) {
		return xPermMapService.searchXPermMaps(searchCriteria);
	}

	public VXLong getXPermMapSearchCount(SearchCriteria searchCriteria) {
		return xPermMapService.getSearchCount(searchCriteria,
				xPermMapService.searchFields);
	}

	public VXAuditMap getXAuditMap(Long id){
		return (VXAuditMap)xAuditMapService.readResource(id);
	}

	public VXAuditMap createXAuditMap(VXAuditMap vXAuditMap){
		vXAuditMap =  (VXAuditMap)xAuditMapService.createResource(vXAuditMap);
		return vXAuditMap;
	}

	public VXAuditMap updateXAuditMap(VXAuditMap vXAuditMap) {
		vXAuditMap =  (VXAuditMap)xAuditMapService.updateResource(vXAuditMap);
		return vXAuditMap;
	}

	public void deleteXAuditMap(Long id, boolean force) {
		 if (force) {
			 xAuditMapService.deleteResource(id);
		 } else {
			 throw restErrorUtil.createRESTException(
				"serverMsg.modelMgrBaseDeleteModel",
				MessageEnums.OPER_NOT_ALLOWED_FOR_ENTITY);
		 }
	}

	public VXAuditMapList searchXAuditMaps(SearchCriteria searchCriteria) {
		return xAuditMapService.searchXAuditMaps(searchCriteria);
	}

	public VXLong getXAuditMapSearchCount(SearchCriteria searchCriteria) {
		return xAuditMapService.getSearchCount(searchCriteria,
				xAuditMapService.searchFields);
	}

	public VXModuleDefList searchXModuleDef(SearchCriteria searchCriteria) {
		return xModuleDefService.searchModuleDef(searchCriteria);
	}
	public VXModulePermissionList searchXModuleDefList(SearchCriteria searchCriteria) {
		return xModuleDefService.searchModuleDefList(searchCriteria);
	}

	public VXUserPermissionList searchXUserPermission(SearchCriteria searchCriteria) {
		return xUserPermissionService.searchXUserPermission(searchCriteria);
	}

	public VXGroupPermissionList searchXGroupPermission(SearchCriteria searchCriteria) {
		return xGroupPermissionService.searchXGroupPermission(searchCriteria);
	}

	public VXLong getXModuleDefSearchCount(SearchCriteria searchCriteria) {
		return xModuleDefService.getSearchCount(searchCriteria,
				xModuleDefService.searchFields);
	}

	public VXLong getXUserPermissionSearchCount(SearchCriteria searchCriteria) {
		return xUserPermissionService.getSearchCount(searchCriteria,
				xUserPermissionService.searchFields);
	}

	public VXLong getXGroupPermissionSearchCount(SearchCriteria searchCriteria){
		return xGroupPermissionService.getSearchCount(searchCriteria,
				xGroupPermissionService.searchFields);
	}
}
