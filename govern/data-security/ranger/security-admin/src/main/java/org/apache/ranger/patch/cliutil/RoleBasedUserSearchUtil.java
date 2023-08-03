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

package org.apache.ranger.patch.cliutil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.ranger.biz.UserMgr;
import org.apache.ranger.biz.XUserMgr;
import org.apache.ranger.common.RangerConstants;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.entity.XXPortalUser;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.patch.BaseLoader;
import org.apache.ranger.service.XUserService;
import org.apache.ranger.util.CLIUtil;
import org.apache.ranger.view.VXUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RoleBasedUserSearchUtil extends BaseLoader {

        private static final Logger logger = LoggerFactory.getLogger(RoleBasedUserSearchUtil.class);
        @Autowired
        XUserService xUserService;

        @Autowired
        RangerDaoManager daoMgr;

        @Autowired
        UserMgr userMgr;

        @Autowired
        XUserMgr xUserMgr;

        public static Boolean checkRole = true;
        public static String userLoginId = "";
        public static String currentPassword = "";
        public static String userRole = "";

        public static void main(String[] args) {
            logger.info("RoleBaseUserSearchUtil : main()");
            try {
		        RoleBasedUserSearchUtil loader = (RoleBasedUserSearchUtil) CLIUtil.getBean(RoleBasedUserSearchUtil.class);
		        loader.init();
		        if (args.length == 3 || args.length == 2) {
	                userLoginId = args[0];
	                currentPassword = args[1];
	                if (args.length == 3) {
	                    userRole = args[2];
                        if (!StringUtils.isBlank(userRole)) {
	                        userRole = userRole.toUpperCase();
	                        if (!RangerConstants.VALID_USER_ROLE_LIST.contains(userRole)) {
	                            System.out.println("Invalid UserRole. Exiting!!!");
	                            logger.info("Invalid UserRole. Exiting!!!");
	                            System.exit(1);
	                        } else {
	                        	checkRole = false;
	                        }
	                    }
	                }
	                if (StringUtils.isBlank(userLoginId)) {
	                    System.out.println("Invalid login ID. Exiting!!!");
	                    logger.info("Invalid login ID. Exiting!!!");
	                    System.exit(1);
	                }
	                if (StringUtils.isBlank(currentPassword)) {
                        System.out.println("Invalid current password. Exiting!!!");
                        logger.info("Invalid current password. Exiting!!!");
                        System.exit(1);
	                }
	                while (loader.isMoreToProcess()) {
	                	loader.load();
	                }
	                logger.info("Load complete. Exiting!!!");
	                System.exit(0);
		        } else {
		            System.out.println("RoleBaseUserSearchUtil: Incorrect Arguments \n Usage: \n <UserRole> ");
		            logger.error("RoleBaseUserSearchUtil: Incorrect Arguments \n Usage: \n <UserRole> ");
		            System.exit(1);
		        }
            } catch (Exception e) {
	            logger.error("Error loading", e);
	            System.exit(1);
            }
        }

        @Override
        public void init() throws Exception {
        	logger.info("==> RoleBaseUserSearchUtil.init()");
        }

        @Override
        public void printStats() {
        }

        @Override
        public void execLoad() {
	        logger.info("==> RoleBaseUserSearchUtil.execLoad()");
	        validateUserAndFetchUserList();
	        logger.info("<== RoleBaseUserSearchUtil.execLoad()");
        }

        public void getUsersBasedOnRole(List<String> userRoleList) {
        	try {
        		if (!CollectionUtils.isEmpty(userRoleList) && userRoleList != null) {
	                Map<String, String> roleSysAdminMap = new HashMap<String, String>();
                        Map<String, String> roleAdminAuditorMap = new HashMap<String, String>();
	                Map<String, String> roleKeyAdminMap = new HashMap<String, String>();
                        Map<String, String> roleKeyAdminAuditorMap = new HashMap<String, String>();
	                Map<String, String> roleUserMap = new HashMap<String, String>();
	                for (String userRole : userRoleList) {
	                    List<XXPortalUser> listXXPortalUser = daoMgr.getXXPortalUser().findByRole(userRole);
	                    if (listXXPortalUser != null && !CollectionUtils.isEmpty(listXXPortalUser)) {
	                        if (userRole.equalsIgnoreCase(RangerConstants.ROLE_SYS_ADMIN)) {
	                            for (XXPortalUser xXPortalUser : listXXPortalUser) {
	                                    roleSysAdminMap.put(xXPortalUser.getLoginId(),userRole);
	                            }
                                } else  if (userRole.equalsIgnoreCase(RangerConstants.ROLE_ADMIN_AUDITOR)) {
                                for (XXPortalUser xXPortalUser : listXXPortalUser) {
                                    roleAdminAuditorMap.put(xXPortalUser.getLoginId(),userRole);
                                }
	                        } else if (userRole.equalsIgnoreCase(RangerConstants.ROLE_KEY_ADMIN)) {
                                for (XXPortalUser xXPortalUser : listXXPortalUser) {
                                        roleKeyAdminMap.put(xXPortalUser.getLoginId(),userRole);
                                }
                                } else if (userRole.equalsIgnoreCase(RangerConstants.ROLE_KEY_ADMIN_AUDITOR)) {
                                for (XXPortalUser xXPortalUser : listXXPortalUser) {
                                        roleKeyAdminAuditorMap.put(xXPortalUser.getLoginId(),userRole);
                                }
	                        } else if (userRole.equalsIgnoreCase(RangerConstants.ROLE_USER)) {
                                for (XXPortalUser xXPortalUser : listXXPortalUser) {
                                        roleUserMap.put(xXPortalUser.getLoginId(),userRole);
                                }
	                        }
	                    }
	                }
                    if (MapUtils.isEmpty(roleSysAdminMap) && MapUtils.isEmpty(roleKeyAdminMap) && MapUtils.isEmpty(roleUserMap) && MapUtils.isEmpty(roleAdminAuditorMap) && MapUtils.isEmpty(roleKeyAdminAuditorMap)) {
                        System.out.println("users with given user role are not there");
                        logger.error("users with given user role are not there");
                        System.exit(1);
                    } else {
	                    if (!MapUtils.isEmpty(roleSysAdminMap)) {
	                    	for(Entry<String, String> entry : roleSysAdminMap.entrySet()){
	                    		System.out.println(entry.getValue() + " : " + entry.getKey());
	                        }
	                    }
                        if (!MapUtils.isEmpty(roleKeyAdminMap)) {
	                    	for(Entry<String, String> entry : roleKeyAdminMap.entrySet()){
	                          System.out.println(entry.getValue() + " : " + entry.getKey());
	                        }
                        }
                        if (!MapUtils.isEmpty(roleUserMap)) {
                        	for(Entry<String, String> entry : roleUserMap.entrySet()){
                              System.out.println(entry.getValue() + " : " + entry.getKey());
                            }
                        }
                        if (!MapUtils.isEmpty(roleAdminAuditorMap)) {
                                for(Entry<String, String> entry : roleAdminAuditorMap.entrySet()){
                                        System.out.println(entry.getValue() + " : " + entry.getKey());
                                }
                        }
                        if (!MapUtils.isEmpty(roleKeyAdminAuditorMap)) {
                                for(Entry<String, String> entry : roleKeyAdminAuditorMap.entrySet()){
                                        System.out.println(entry.getValue() + " : " + entry.getKey());
                                }
                        }
                        if (userRoleList.contains(RangerConstants.ROLE_SYS_ADMIN)) {
                        	System.out.println("ROLE_SYS_ADMIN Total Count : " + roleSysAdminMap.size());
                        }
                        if (userRoleList.contains(RangerConstants.ROLE_KEY_ADMIN)) {
                        	System.out.println("ROLE_KEY_ADMIN Total Count : " + roleKeyAdminMap.size());
                        }
                        if (userRoleList.contains(RangerConstants.ROLE_USER)) {
                        	System.out.println("ROLE_USER Total Count : " + roleUserMap.size());
                        }
                        if (userRoleList.contains(RangerConstants.ROLE_ADMIN_AUDITOR)) {
                                System.out.println("ROLE_ADMIN_AUDITOR Total Count : " + roleAdminAuditorMap.size());
                        }
                        if (userRoleList.contains(RangerConstants.ROLE_KEY_ADMIN_AUDITOR)) {
                                System.out.println("ROLE_KEY_ADMIN_AUDITOR Total Count : " + roleKeyAdminAuditorMap.size());
                        }

                        int total = roleSysAdminMap.size() + roleKeyAdminMap.size() + roleUserMap.size() + roleAdminAuditorMap.size() + roleKeyAdminAuditorMap.size();
                        System.out.println("Total Count : " + total);
                    }
        		}
	
	        } catch (Exception e) {
	                logger.error("Error getting User's List with the mentioned role: "+ e.getMessage());
	        }
        }

        public void validateUserAndFetchUserList() {
            userLoginId = userLoginId.toLowerCase();
            XXPortalUser xxPortalUser = daoMgr.getXXPortalUser().findByLoginId(userLoginId);
            Boolean isUserAuthorized = false;
            if (xxPortalUser != null) {
            	String dbPassword = xxPortalUser.getPassword();
                String currentEncryptedPassword = null;
                try {
                	currentEncryptedPassword = userMgr.encrypt(userLoginId,currentPassword);
                    if (currentEncryptedPassword != null && currentEncryptedPassword.equals(dbPassword)) {
                        VXUser vxUser = xUserService.getXUserByUserName(xxPortalUser.getLoginId());
                        if (vxUser != null) {
                            List<String> existingRole = (List<String>) vxUser.getUserRoleList();
                            List<String> permissionList = daoMgr.getXXModuleDef().findAccessibleModulesByUserId(xxPortalUser.getId(), vxUser.getId());
                            if (permissionList != null && permissionList.contains(RangerConstants.MODULE_USER_GROUPS) && !CollectionUtils.isEmpty(existingRole) &&  !StringUtils.isBlank(existingRole.get(0))) {
                                List<String> userRoleList = new ArrayList<String>();
                                if (existingRole.get(0).equalsIgnoreCase(RangerConstants.ROLE_USER)) {
	                                userRoleList.add(RangerConstants.ROLE_USER);
	                                if (checkRole) {
	                                	getUsersBasedOnRole(userRoleList);
	                                } else if (existingRole.get(0).equalsIgnoreCase(userRole) || userRole.equalsIgnoreCase(RangerConstants.ROLE_USER)) {
	                                	getUsersBasedOnRole(userRoleList);
	                                } else {
	                                	isUserAuthorized = true;
	                                }
                                } else if (existingRole.get(0).equalsIgnoreCase(RangerConstants.ROLE_SYS_ADMIN) || existingRole.get(0).equalsIgnoreCase(RangerConstants.ROLE_ADMIN_AUDITOR)) {
                                	if (checkRole) {
                                                userRoleList.add(RangerConstants.ROLE_SYS_ADMIN);
                                                userRoleList.add(RangerConstants.ROLE_ADMIN_AUDITOR);
	                                    userRoleList.add(RangerConstants.ROLE_USER);
	                                    getUsersBasedOnRole(userRoleList);
                                    } else if (existingRole.get(0).equalsIgnoreCase(userRole) || userRole.equalsIgnoreCase(RangerConstants.ROLE_USER) || userRole.equalsIgnoreCase(RangerConstants.ROLE_ADMIN_AUDITOR) || userRole.equalsIgnoreCase(RangerConstants.ROLE_SYS_ADMIN)) {
                                        userRoleList.add(userRole);
                                        getUsersBasedOnRole(userRoleList);
                                    }else {
                                    	isUserAuthorized = true;
                                    }
                                } else if (existingRole.get(0).equalsIgnoreCase(RangerConstants.ROLE_KEY_ADMIN) || existingRole.get(0).equalsIgnoreCase(RangerConstants.ROLE_KEY_ADMIN_AUDITOR) || userRole.equalsIgnoreCase(RangerConstants.ROLE_USER)) {
                                    if (checkRole) {
                                        userRoleList.add(RangerConstants.ROLE_KEY_ADMIN);
                                            userRoleList.add(RangerConstants.ROLE_KEY_ADMIN_AUDITOR);
	                                    userRoleList.add(RangerConstants.ROLE_USER);
	                                    getUsersBasedOnRole(userRoleList);
                                    } else if (existingRole.get(0).equalsIgnoreCase(userRole) || userRole.equalsIgnoreCase(RangerConstants.ROLE_USER) || userRole.equalsIgnoreCase(RangerConstants.ROLE_KEY_ADMIN) || userRole.equalsIgnoreCase(RangerConstants.ROLE_KEY_ADMIN_AUDITOR)) {
                                        userRoleList.add(userRole);
                                        getUsersBasedOnRole(userRoleList);
                                    } else {
                                    	isUserAuthorized = true;
                                    }
                                }
                                if (isUserAuthorized == true) {
                                    System.out.println("user is not authorized to fetch this list");
                                    logger.error("user is not authorized to fetch this list");
                                    System.exit(1);
                                }
                            } else {
                                System.out.println("user permission denied");
                                logger.error("user permission denied");
                                System.exit(1);
                            }
                        }
                    } else {
                    	System.out.println("Invalid user password");
                        logger.error("Invalid user password");
                        System.exit(1);
                    }
                } catch (Exception e) {
                    logger.error("Getting User's List with the mentioned role failure. Detail:  \n",e);
                    System.exit(1);
                }
            } else {
                System.out.println("User does not exist in DB!!");
                logger.error("User does not exist in DB");
                System.exit(1);
            }
        }
}
