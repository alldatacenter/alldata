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

package org.apache.ranger.patch;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.ranger.biz.XUserMgr;
import org.apache.ranger.common.RangerConstants;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.entity.XXPortalUser;
import org.apache.ranger.service.XPortalUserService;
import org.apache.ranger.util.CLIUtil;
import org.apache.ranger.view.VXPortalUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class PatchAssignSecurityZonePersmissionToAdmin_J10026 extends BaseLoader {

	private static final Logger logger = LoggerFactory.getLogger(PatchAssignSecurityZonePersmissionToAdmin_J10026.class);

	@Autowired
	RangerDaoManager daoManager;

	@Autowired
	XUserMgr xUserMgr;

	@Autowired
	XPortalUserService xPortalUserService;

	private static boolean grantAllUsers=false;
	private static String usersListFileName=null;
	private final static Charset ENCODING = StandardCharsets.UTF_8;
	public static void main(String[] args) {
		logger.info("main()");
		try {
			if(args!=null && args.length>0){
				if(StringUtils.equalsIgnoreCase("ALL", args[0])){
					grantAllUsers=true;
				}else if(!StringUtils.isEmpty(args[0])){
					usersListFileName=args[0];
				}
			}
			PatchAssignSecurityZonePersmissionToAdmin_J10026 loader = (PatchAssignSecurityZonePersmissionToAdmin_J10026) CLIUtil
					.getBean(PatchAssignSecurityZonePersmissionToAdmin_J10026.class);

			loader.init();
			while (loader.isMoreToProcess()) {
				loader.load();
			}
			logger.info("Load complete. Exiting!!!");
			System.exit(0);
		} catch (Exception e) {
			logger.error("Error loading", e);
			System.exit(1);
		}
	}


	@Override
	public void init() throws Exception {
		// Do Nothing
	}

	@Override
	public void printStats() {
		// Do Nothing
	}

	@Override
	public void execLoad() {
		logger.info("==> PatchAssignSecurityZonePersmissionToAdmin_J10026.execLoad() started");
		assignSecurityZonePermissionToExistingAdminUsers();
		logger.info("<== PatchAssignSecurityZonePersmissionToAdmin_J10026.execLoad() completed");

	}

	private void assignSecurityZonePermissionToExistingAdminUsers() {
		int countUserPermissionUpdated = 0;
		Long userCount=daoManager.getXXPortalUser().getAllCount();
		List<XXPortalUser> xXPortalUsers=null;
		Long patchModeMaxLimit=Long.valueOf(500L);
		try{
			if (userCount!=null && userCount>0){
				List<String> loginIdList=readUserNamesFromFile(usersListFileName);
				if(!CollectionUtils.isEmpty(loginIdList)){
					xXPortalUsers=new ArrayList<XXPortalUser>();
					XXPortalUser xXPortalUser=null;
					for(String loginId:loginIdList){
						try{
							xXPortalUser=daoManager.getXXPortalUser().findByLoginId(loginId);
							if(xXPortalUser!=null){
								xXPortalUsers.add(xXPortalUser);
							}else{
								logger.info("User "+loginId+" doesn't exist!");
							}
						}catch(Exception ex){
						}
					}
					countUserPermissionUpdated=assignPermissions(xXPortalUsers);
					logger.info("Security Zone Permissions assigned to "+countUserPermissionUpdated + " of total "+loginIdList.size());
				} else {
					xXPortalUsers=daoManager.getXXPortalUser().findByRole(RangerConstants.ROLE_SYS_ADMIN);
					if(!CollectionUtils.isEmpty(xXPortalUsers)){
						countUserPermissionUpdated=assignPermissions(xXPortalUsers);
						logger.info("Security Zone Permissions assigned to users having role:"+RangerConstants.ROLE_SYS_ADMIN+". Processed:"+countUserPermissionUpdated + " of total "+xXPortalUsers.size());
					}
					xXPortalUsers=daoManager.getXXPortalUser().findByRole(RangerConstants.ROLE_ADMIN_AUDITOR);
					if(!CollectionUtils.isEmpty(xXPortalUsers)){
						countUserPermissionUpdated=assignPermissions(xXPortalUsers);
						logger.info("Security Zone Permissions assigned to users having role:"+RangerConstants.ROLE_ADMIN_AUDITOR+". Processed:"+countUserPermissionUpdated + " of total "+xXPortalUsers.size());
					}
					//if total no. of users are more than 500 then process ADMIN and KEY_ADMIN users only to avoid timeout
					if(userCount.compareTo(Long.valueOf(patchModeMaxLimit))<0 || grantAllUsers){
						xXPortalUsers=daoManager.getXXPortalUser().findByRole(RangerConstants.ROLE_USER);
						if(!CollectionUtils.isEmpty(xXPortalUsers)){
							countUserPermissionUpdated=assignPermissions(xXPortalUsers);
							logger.info("Security Zone Permissions assigned to "+countUserPermissionUpdated + " of total "+xXPortalUsers.size());
						}
						logger.info("Please execute this patch separately with argument 'ALL' to assign permission to remaining users ");
						System.out.println("Please execute this patch separately with argument 'ALL' to assign module permissions to remaining users!!");
					}
				}
			}
		}catch(Exception ex){
		}
	}

	private int assignPermissions(List<XXPortalUser> xXPortalUsers) {
		HashMap<String, Long> moduleNameId = xUserMgr.getAllModuleNameAndIdMap();
		int countUserPermissionUpdated = 0;
		if (!CollectionUtils.isEmpty(xXPortalUsers)) {
			for (XXPortalUser xPortalUser : xXPortalUsers) {
				try {
					if (xPortalUser != null) {
						VXPortalUser vPortalUser = xPortalUserService.populateViewBean(xPortalUser);
						if (vPortalUser != null) {
							vPortalUser.setUserRoleList(daoManager.getXXPortalUserRole()
									.findXPortalUserRolebyXPortalUserId(vPortalUser.getId()));
							xUserMgr.createOrUpdateUserPermisson(vPortalUser,
									moduleNameId.get(RangerConstants.MODULE_SECURITY_ZONE), false);
							countUserPermissionUpdated += 1;
							logger.info("Security Zone Permission assigned/updated to Admin Role, UserId ["
									+ xPortalUser.getId() + "]");
						}
					}
				} catch (Exception ex) {
					logger.error("Error while assigning security zone permission for admin users", ex);
					System.exit(1);
				}
			}
		}
		return countUserPermissionUpdated;
	}

	private List<String> readUserNamesFromFile(String aFileName) throws IOException {
		List<String> userNames=new ArrayList<String>();
		if(!StringUtils.isEmpty(aFileName)){
			Path path = Paths.get(aFileName);
			if (Files.exists(path) && Files.isRegularFile(path)) {
				List<String> fileContents=Files.readAllLines(path, ENCODING);
				if(fileContents!=null && !fileContents.isEmpty()){
					for(String line:fileContents){
						if(!StringUtils.isEmpty(line) && !userNames.contains(line)){
							try{
								userNames.add(line.trim());
							}catch(Exception ex){
							}
						}
					}
				}
			}
		}
	   return userNames;
	}
}
