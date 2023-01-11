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

import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.entity.XXModuleDef;
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

@Component
public class PatchGrantAuditPermissionToKeyRoleUser_J10014 extends BaseLoader {
        private static final Logger logger = LoggerFactory
                        .getLogger(PatchGrantAuditPermissionToKeyRoleUser_J10014.class);

        @Autowired
        XUserMgr xUserMgr;

        @Autowired
        XPortalUserService xPortalUserService;

        @Autowired
        RangerDaoManager daoManager;

        public static void main(String[] args) {
                logger.info("main()");
                try {
                        PatchGrantAuditPermissionToKeyRoleUser_J10014 loader = (PatchGrantAuditPermissionToKeyRoleUser_J10014) CLIUtil
                                        .getBean(PatchGrantAuditPermissionToKeyRoleUser_J10014.class);

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
                logger.info("==>Starting : PatchGrantAuditPermissionToKeyRoleUser.execLoad()");
                assignAuditAndUserGroupPermissionToKeyAdminRoleUser();

                logger.info("<==Completed : PatchGrantAuditPermissionToKeyRoleUser.execLoad()");
        }

        private void assignAuditAndUserGroupPermissionToKeyAdminRoleUser() {
                try {
                        int countUserPermissionUpdated = 0;
                        XXModuleDef xAuditModDef = daoManager.getXXModuleDef()
                                        .findByModuleName(RangerConstants.MODULE_AUDIT);
                        XXModuleDef xUserGrpModDef = daoManager.getXXModuleDef()
                                        .findByModuleName(RangerConstants.MODULE_USER_GROUPS);
                        logger.warn("Audit Module Object : " + xAuditModDef);
                        logger.warn("USer Group Module Object : " + xUserGrpModDef);
                        if (xAuditModDef == null && xUserGrpModDef == null) {
                                logger.warn("Audit Module and User Group module not found");
                                return;
                        }
                        List<XXPortalUser> allKeyAdminUsers = daoManager.getXXPortalUser()
                                        .findByRole(RangerConstants.ROLE_KEY_ADMIN);
                        if (!CollectionUtils.isEmpty(allKeyAdminUsers)) {
                                for (XXPortalUser xPortalUser : allKeyAdminUsers) {
                                        boolean isUserUpdated = false;
                                        VXPortalUser vPortalUser = xPortalUserService
                                                        .populateViewBean(xPortalUser);
                                        if (vPortalUser != null) {
                                                vPortalUser.setUserRoleList(daoManager
                                                                .getXXPortalUserRole()
                                                                .findXPortalUserRolebyXPortalUserId(
                                                                                vPortalUser.getId()));
                                                if (xAuditModDef != null) {
                                                        xUserMgr.createOrUpdateUserPermisson(vPortalUser,
                                                                        xAuditModDef.getId(), true);
                                                        isUserUpdated = true;
                                                        logger.info("Added '" + xAuditModDef.getModule()
                                                                        + "' permission to user '"
                                                                        + xPortalUser.getLoginId() + "'");
                                                }
                                                if (xUserGrpModDef != null) {
                                                        xUserMgr.createOrUpdateUserPermisson(vPortalUser,
                                                                        xUserGrpModDef.getId(), true);
                                                        isUserUpdated = true;
                                                        logger.info("Added '" + xUserGrpModDef.getModule()
                                                                        + "' permission to user '"
                                                                        + xPortalUser.getLoginId() + "'");
                                                }
                                                if (isUserUpdated) {
                                                        countUserPermissionUpdated += 1;
                                                }

                                        }
                                }

                                logger.info(countUserPermissionUpdated
                                                + " permissions were assigned");
                        } else {
                                logger.info("There are no user with Key Admin role");
                        }
                } catch (Exception ex) {
                        logger.error("Error while granting Audit and User group permission ",ex);
                }
        }



        @Override
        public void printStats() {
        }
}
