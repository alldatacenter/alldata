/*<?xml version="1.0" encoding="UTF-8"?>
<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->*/

package org.apache.ranger.patch.cliutil;

import org.apache.ranger.biz.UserMgr;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.entity.XXPortalUser;
import org.apache.ranger.patch.BaseLoader;
import org.apache.ranger.util.CLIUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ChangeUserNameUtil extends BaseLoader {
    private static final Logger logger = LoggerFactory.getLogger(ChangeUserNameUtil.class);

    @Autowired
    RangerDaoManager daoMgr;

    @Autowired
    UserMgr userMgr;

    public static String userLoginId;
    public static String currentPassword;
    public static String newUserName;

    public static void main(String[] args) {
        logger.info("main()");
        try {
            ChangeUserNameUtil loader = (ChangeUserNameUtil) CLIUtil.getBean(ChangeUserNameUtil.class);
            loader.init();
            if (args.length == 3) {
                userLoginId = args[0];
                currentPassword = args[1];
                newUserName = args[2];
                if(StringUtils.isEmpty(userLoginId)){
                    System.out.println("Invalid login ID. Exiting!!!");
                    logger.info("Invalid login ID. Exiting!!!");
                    System.exit(1);
                }
                if(StringUtils.isEmpty(currentPassword)){
                    System.out.println("Invalid current password. Exiting!!!");
                    logger.info("Invalid current password. Exiting!!!");
                    System.exit(1);
                }
                if(StringUtils.isEmpty(newUserName)){
                    System.out.println("Invalid new user name. Exiting!!!");
                    logger.info("Invalid new user name. Exiting!!!");
                    System.exit(1);
                }
                while (loader.isMoreToProcess()) {
                    loader.load();
                }
                logger.info("Load complete. Exiting!!!");
                System.exit(0);
            }else{
                System.out.println("ChangeUserNameUtil: Incorrect Arguments \n Usage: \n <loginId> <current-password> <new-username>");
                logger.error("ChangeUserNameUtil: Incorrect Arguments \n Usage: \n <loginId> <current-password> <new-username>");
                System.exit(1);
            }
        }
        catch (Exception e) {
            logger.error("Error loading", e);
            System.exit(1);
        }
    }

    @Override
    public void init() throws Exception {
    }

    @Override
    public void printStats() {
    }

    @Override
    public void execLoad() {
        logger.info("==> ChangeUserNameUtil.execLoad()");
        updateUserName();
        logger.info("<== ChangeUserNameUtil.execLoad()");
    }

    public void updateUserName() {
        // if newUserName already exists
        if (daoMgr.getXXPortalUser().findByLoginId(newUserName) != null) {
            System.out.println("New user name already exist in DB!");
            logger.error("New user name already exist in DB");
            System.exit(1);
        }
        XXPortalUser xPortalUser=daoMgr.getXXPortalUser().findByLoginId(userLoginId);
        if (xPortalUser!=null){
            String dbPassword=xPortalUser.getPassword();
            String currentEncryptedPassword=null;
            try {
                currentEncryptedPassword=userMgr.encrypt(userLoginId, currentPassword);
                if (currentEncryptedPassword.equals(dbPassword)){
                    userMgr.updateOldUserName(userLoginId, newUserName, currentPassword);
                    logger.info("User Name '"+userLoginId+"' updated to '"+newUserName+"' sucessfully.");
                }
                else{
                    System.out.println("Invalid user password");
                    logger.error("Invalid user password");
                    System.exit(1);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else{
            System.out.println("User does not exist in DB!!");
            logger.error("User does not exist in DB");
            System.exit(1);
        }
    }
}
