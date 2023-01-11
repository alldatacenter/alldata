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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.common.MessageEnums;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.common.RangerConstants;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.db.XXSecurityZoneRefGroupDao;
import org.apache.ranger.db.XXSecurityZoneRefResourceDao;
import org.apache.ranger.db.XXSecurityZoneRefServiceDao;
import org.apache.ranger.db.XXSecurityZoneRefTagServiceDao;
import org.apache.ranger.db.XXSecurityZoneRefUserDao;
import org.apache.ranger.entity.XXGroup;
import org.apache.ranger.entity.XXResourceDef;
import org.apache.ranger.entity.XXSecurityZoneRefGroup;
import org.apache.ranger.entity.XXSecurityZoneRefResource;
import org.apache.ranger.entity.XXSecurityZoneRefService;
import org.apache.ranger.entity.XXSecurityZoneRefTagService;
import org.apache.ranger.entity.XXSecurityZoneRefUser;
import org.apache.ranger.entity.XXService;
import org.apache.ranger.entity.XXServiceDef;
import org.apache.ranger.entity.XXUser;
import org.apache.ranger.plugin.model.RangerSecurityZone;
import org.apache.ranger.plugin.model.RangerSecurityZone.RangerSecurityZoneService;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.service.RangerAuditFields;
import org.apache.ranger.service.RangerServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SecurityZoneRefUpdater {

	@Autowired
	RangerDaoManager daoMgr;

	@Autowired
	RangerAuditFields<?> rangerAuditFields;

	@Autowired
	RangerServiceService svcService;

	@Autowired
	RESTErrorUtil restErrorUtil;

	public void createNewZoneMappingForRefTable(RangerSecurityZone rangerSecurityZone) throws Exception {

		if(rangerSecurityZone == null) {
			return;
		}

		cleanupRefTables(rangerSecurityZone);

		final Long zoneId = rangerSecurityZone == null ? null : rangerSecurityZone.getId();
		final Map<String, RangerSecurityZoneService> zoneServices = rangerSecurityZone.getServices();

		final Set<String> adminUsers = new HashSet<>();
		final Set<String> adminUserGroups = new HashSet<>();
		final Set<String> auditUsers = new HashSet<>();
		final Set<String> auditUserGroups = new HashSet<>();
                final Set<String> tagServices = new HashSet<>();
		XXServiceDef xServiceDef = new XXServiceDef();

		adminUsers.addAll(rangerSecurityZone.getAdminUsers());
		adminUserGroups.addAll(rangerSecurityZone.getAdminUserGroups());
		auditUsers.addAll(rangerSecurityZone.getAuditUsers());
		auditUserGroups.addAll(rangerSecurityZone.getAuditUserGroups());
                tagServices.addAll(rangerSecurityZone.getTagServices());
		for(Map.Entry<String, RangerSecurityZoneService> service : zoneServices.entrySet()) {
			String serviceName = service.getKey();

			if (StringUtils.isBlank(serviceName)) {
				continue;
			}

			XXService xService = daoMgr.getXXService().findByName(serviceName);
			RangerService rService = svcService.getPopulatedViewObject(xService);
			xServiceDef = daoMgr.getXXServiceDef().findByName(rService.getType());

			XXSecurityZoneRefService xZoneService = rangerAuditFields.populateAuditFieldsForCreate(new XXSecurityZoneRefService());

			xZoneService.setZoneId(zoneId);
			xZoneService.setServiceId(xService.getId());
			xZoneService.setServiceName(serviceName);

			daoMgr.getXXSecurityZoneRefService().create(xZoneService);

			for(Map<String, List<String>> resourceMap:service.getValue().getResources()){//add all resourcedefs in pre defined set
				for(Map.Entry<String, List<String>> resource : resourceMap.entrySet()) {
					String resourceName = resource.getKey();
					if (StringUtils.isBlank(resourceName)) {
						continue;
					}

					XXResourceDef xResourceDef = daoMgr.getXXResourceDef().findByNameAndServiceDefId(resourceName, xServiceDef.getId());

					XXSecurityZoneRefResource xZoneResource = rangerAuditFields.populateAuditFieldsForCreate(new XXSecurityZoneRefResource());

					xZoneResource.setZoneId(zoneId);
					xZoneResource.setResourceDefId(xResourceDef.getId());
					xZoneResource.setResourceName(resourceName);

					daoMgr.getXXSecurityZoneRefResource().create(xZoneResource);
				}
			}
		}

                if(CollectionUtils.isNotEmpty(tagServices)) {
                        for(String tagService : tagServices) {

                                if (StringUtils.isBlank(tagService)) {
                                        continue;
                                }

                                XXService xService = daoMgr.getXXService().findByName(tagService);
                                if (xService == null || xService.getType() != RangerConstants.TAG_SERVICE_TYPE) {
                                        throw restErrorUtil.createRESTException("Tag Service named: " + tagService + " does not exist ",
                                                        MessageEnums.INVALID_INPUT_DATA);
                                }

                                XXSecurityZoneRefTagService xZoneTagService = rangerAuditFields.populateAuditFieldsForCreate(new XXSecurityZoneRefTagService());

                                xZoneTagService.setZoneId(zoneId);
                                xZoneTagService.setTagServiceId(xService.getId());
                                xZoneTagService.setTagServiceName(xService.getName());

                                daoMgr.getXXSecurityZoneRefTagService().create(xZoneTagService);
                        }
                }

		if(CollectionUtils.isNotEmpty(adminUsers)) {
			for(String adminUser : adminUsers) {

				if (StringUtils.isBlank(adminUser)) {
					continue;
				}

				XXUser xUser = daoMgr.getXXUser().findByUserName(adminUser);

				if (xUser == null) {
					throw restErrorUtil.createRESTException("user with name: " + adminUser + " does not exist ",
							MessageEnums.INVALID_INPUT_DATA);
				}

				XXSecurityZoneRefUser xZoneAdminUser = rangerAuditFields.populateAuditFieldsForCreate(new XXSecurityZoneRefUser());

				xZoneAdminUser.setZoneId(zoneId);
				xZoneAdminUser.setUserId(xUser.getId());
				xZoneAdminUser.setUserName(adminUser);
				xZoneAdminUser.setUserType(1);

				daoMgr.getXXSecurityZoneRefUser().create(xZoneAdminUser);
			}
		}

		if(CollectionUtils.isNotEmpty(adminUserGroups)) {
			for(String adminUserGroup : adminUserGroups) {

				if (StringUtils.isBlank(adminUserGroup)) {
					continue;
				}

				XXGroup xGroup = daoMgr.getXXGroup().findByGroupName(adminUserGroup);

				if (xGroup == null) {
					throw restErrorUtil.createRESTException("group with name: " + adminUserGroup + " does not exist ",
							MessageEnums.INVALID_INPUT_DATA);
				}

				XXSecurityZoneRefGroup xZoneAdminGroup = rangerAuditFields.populateAuditFieldsForCreate(new XXSecurityZoneRefGroup());

				xZoneAdminGroup.setZoneId(zoneId);
				xZoneAdminGroup.setGroupId(xGroup.getId());
				xZoneAdminGroup.setGroupName(adminUserGroup);
				xZoneAdminGroup.setGroupType(1);

				daoMgr.getXXSecurityZoneRefGroup().create(xZoneAdminGroup);
			}
		}

		if(CollectionUtils.isNotEmpty(auditUsers)) {
			for(String auditUser : auditUsers) {

				if (StringUtils.isBlank(auditUser)) {
					continue;
				}

				XXUser xUser = daoMgr.getXXUser().findByUserName(auditUser);

				if (xUser == null) {
					throw restErrorUtil.createRESTException("user with name: " + auditUser + " does not exist ",
							MessageEnums.INVALID_INPUT_DATA);
				}

				XXSecurityZoneRefUser xZoneAuditUser = rangerAuditFields.populateAuditFieldsForCreate(new XXSecurityZoneRefUser());

				xZoneAuditUser.setZoneId(zoneId);
				xZoneAuditUser.setUserId(xUser.getId());
				xZoneAuditUser.setUserName(auditUser);
				xZoneAuditUser.setUserType(0);

				daoMgr.getXXSecurityZoneRefUser().create(xZoneAuditUser);
			}
		}

		if(CollectionUtils.isNotEmpty(auditUserGroups)) {
			for(String auditUserGroup : auditUserGroups) {
				if (StringUtils.isBlank(auditUserGroup)) {
					continue;
				}

				XXGroup xGroup = daoMgr.getXXGroup().findByGroupName(auditUserGroup);

				if (xGroup == null) {
					throw restErrorUtil.createRESTException("group with name: " + auditUserGroup + " does not exist ",
							MessageEnums.INVALID_INPUT_DATA);
				}

				XXSecurityZoneRefGroup xZoneAuditGroup = rangerAuditFields.populateAuditFieldsForCreate(new XXSecurityZoneRefGroup());

				xZoneAuditGroup.setZoneId(zoneId);
				xZoneAuditGroup.setGroupId(xGroup.getId());
				xZoneAuditGroup.setGroupName(auditUserGroup);
				xZoneAuditGroup.setGroupType(0);

				daoMgr.getXXSecurityZoneRefGroup().create(xZoneAuditGroup);
			}
		}
	}


	public Boolean cleanupRefTables(RangerSecurityZone rangerSecurityZone) {
		final Long zoneId = rangerSecurityZone == null ? null : rangerSecurityZone.getId();

		if (zoneId == null) {
			return false;
		}

		XXSecurityZoneRefServiceDao     xZoneServiceDao      = daoMgr.getXXSecurityZoneRefService();
                XXSecurityZoneRefTagServiceDao     xZoneTagServiceDao      = daoMgr.getXXSecurityZoneRefTagService();
		XXSecurityZoneRefResourceDao        xZoneResourceDao    = daoMgr.getXXSecurityZoneRefResource();
		XXSecurityZoneRefUserDao         xZoneUserDao     = daoMgr.getXXSecurityZoneRefUser();
		XXSecurityZoneRefGroupDao   xZoneGroupDao   = daoMgr.getXXSecurityZoneRefGroup();

		for (XXSecurityZoneRefService service : xZoneServiceDao.findByZoneId(zoneId)) {
			xZoneServiceDao.remove(service);
		}

                for (XXSecurityZoneRefTagService service : xZoneTagServiceDao.findByZoneId(zoneId)) {
                        xZoneTagServiceDao.remove(service);
                }

		for(XXSecurityZoneRefResource resource : xZoneResourceDao.findByZoneId(zoneId)) {
			xZoneResourceDao.remove(resource);
		}

		for(XXSecurityZoneRefUser user : xZoneUserDao.findByZoneId(zoneId)) {
			xZoneUserDao.remove(user);
		}

		for(XXSecurityZoneRefGroup group : xZoneGroupDao.findByZoneId(zoneId)) {
			xZoneGroupDao.remove(group);
		}

		return true;
	}
}
