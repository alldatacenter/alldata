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
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.entity.XXPortalUser;
import org.apache.ranger.service.XPortalUserService;
import org.apache.ranger.biz.XUserMgr;
import org.apache.ranger.common.RangerConstants;
import org.apache.ranger.util.CLIUtil;
import org.apache.ranger.view.VXPortalUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class PatchPermissionModel_J10003 extends BaseLoader {
	private static final Logger logger = LoggerFactory
			.getLogger(PatchPermissionModel_J10003.class);

	@Autowired
	XUserMgr xUserMgr;

	@Autowired
	XPortalUserService xPortalUserService;

	@Autowired
	RangerDaoManager daoManager;

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
			PatchPermissionModel_J10003 loader = (PatchPermissionModel_J10003) CLIUtil
					.getBean(PatchPermissionModel_J10003.class);

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
	public void execLoad() {
		logger.info("==> PermissionPatch.execLoad()");
		assignPermissionToExistingUsers();
		logger.info("<== PermissionPatch.execLoad()");
	}

	public void assignPermissionToExistingUsers() {
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
					logger.info("Permissions assigned to "+countUserPermissionUpdated + " of "+loginIdList.size());
				}else if(userCount.compareTo(Long.valueOf(patchModeMaxLimit))<0 || grantAllUsers){
					xXPortalUsers=daoManager.getXXPortalUser().findAllXPortalUser();
					if(!CollectionUtils.isEmpty(xXPortalUsers)){
						countUserPermissionUpdated=assignPermissions(xXPortalUsers);
						logger.info("Permissions assigned to "+countUserPermissionUpdated + " of "+xXPortalUsers.size());
					}
				}else{
					//if total no. of users are more than 500 then process ADMIN and KEY_ADMIN users only to avoid timeout
					xXPortalUsers=daoManager.getXXPortalUser().findByRole(RangerConstants.ROLE_SYS_ADMIN);
					if(!CollectionUtils.isEmpty(xXPortalUsers)){
						countUserPermissionUpdated=assignPermissions(xXPortalUsers);
						logger.info("Permissions assigned to users having role:"+RangerConstants.ROLE_SYS_ADMIN+". Processed:"+countUserPermissionUpdated + " of total "+xXPortalUsers.size());
					}
					xXPortalUsers=daoManager.getXXPortalUser().findByRole(RangerConstants.ROLE_KEY_ADMIN);
					if(!CollectionUtils.isEmpty(xXPortalUsers)){
						countUserPermissionUpdated=assignPermissions(xXPortalUsers);
						logger.info("Permissions assigned to users having role:"+RangerConstants.ROLE_KEY_ADMIN+". Processed:"+countUserPermissionUpdated + " of total "+xXPortalUsers.size());
					}
					logger.info("Please execute this patch separately with argument 'ALL' to assign permission to remaining users ");
	                System.out.println("Please execute this patch separately with argument 'ALL' to assign module permissions to remaining users!!");
				}
			}
		}catch(Exception ex){
		}
	}

	@Override
	public void printStats() {
	}

	private int assignPermissions(List<XXPortalUser> xXPortalUsers){
		int countUserPermissionUpdated = 0;
		if(!CollectionUtils.isEmpty(xXPortalUsers)){
			for (XXPortalUser xPortalUser : xXPortalUsers) {
				try{
					if(xPortalUser!=null){
						VXPortalUser vPortalUser = xPortalUserService.populateViewBean(xPortalUser);
						if(vPortalUser!=null){
							vPortalUser.setUserRoleList(daoManager.getXXPortalUserRole().findXPortalUserRolebyXPortalUserId(vPortalUser.getId()));
							xUserMgr.assignPermissionToUser(vPortalUser, false);
							countUserPermissionUpdated += 1;
							logger.info("Permissions assigned/updated on base of User's Role, UserId [" + xPortalUser.getId() + "]");
						}
					}
				}catch(Exception ex){
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
