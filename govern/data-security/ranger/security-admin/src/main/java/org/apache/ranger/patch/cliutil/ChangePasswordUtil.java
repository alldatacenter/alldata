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
import org.apache.ranger.common.MessageEnums;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.common.StringUtil;
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
public class ChangePasswordUtil extends BaseLoader {
	private static final Logger logger = LoggerFactory.getLogger(ChangePasswordUtil.class);

	@Autowired
	RangerDaoManager daoMgr;

	@Autowired
	UserMgr userMgr;

	@Autowired
	RESTErrorUtil restErrorUtil;

	public static String userLoginId;
	public static String currentPassword;
	public static String newPassword;
	public static boolean defaultPwdChangeRequest = false;
	public static String[] userPwdArgs;

	public static void main(String[] args) {
		logger.info("main()");
		try {
			ChangePasswordUtil loader = (ChangePasswordUtil) CLIUtil.getBean(ChangePasswordUtil.class);
			loader.init();
			userPwdArgs=args;
			if (args.length > 4) {
				if ("-default".equalsIgnoreCase(args[args.length-1])) {
					defaultPwdChangeRequest = true;
				}
				while (loader.isMoreToProcess()) {
					loader.load();
				}
				logger.info("Load complete. Exiting!!!");
				System.exit(0);
			} else if (args.length == 3 || args.length == 4) {
				userLoginId = args[0];
				currentPassword = args[1];
				newPassword = args[2];
				if (args.length == 4) {
					if ("-default".equalsIgnoreCase(args[3])) {
						defaultPwdChangeRequest = true;
					}
				}
				if (StringUtils.isEmpty(userLoginId)) {
					System.out.println("Invalid login ID. Exiting!!!");
					logger.info("Invalid login ID. Exiting!!!");
					System.exit(1);
				}
				if (StringUtils.isEmpty(currentPassword)) {
					System.out.println("Invalid current password. Exiting!!!");
					logger.info("Invalid current password. Exiting!!!");
					System.exit(1);
				}
				if (StringUtils.isEmpty(newPassword)) {
					System.out.println("Invalid new password. Exiting!!!");
					logger.info("Invalid new password. Exiting!!!");
					System.exit(1);
				}
				while (loader.isMoreToProcess()) {
					loader.load();
				}
				logger.info("Load complete. Exiting!!!");
				System.exit(0);
			} else {
				System.out.println(
						"ChangePasswordUtil: Incorrect Arguments \n Usage: \n <loginId> <current-password> <new-password>");
				logger.error(
						"ChangePasswordUtil: Incorrect Arguments \n Usage: \n <loginId> <current-password> <new-password>");
				System.exit(1);
			}
		} catch (Exception e) {
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
		logger.info("==> ChangePasswordUtil.execLoad()");
		if(userPwdArgs.length>4) {
			updateMultiplePasswords();
		}else {
			updateAdminPassword();
		}
		logger.info("<== ChangePasswordUtil.execLoad()");
	}

	public void updateAdminPassword() {
		XXPortalUser xPortalUser = daoMgr.getXXPortalUser().findByLoginId(userLoginId);
		if (xPortalUser != null) {
			String dbPassword = xPortalUser.getPassword();
			String currentEncryptedPassword = null;
			String md5EncryptedPassword = null;
			try {
				if (config.isFipsEnabled()) {
					if (defaultPwdChangeRequest) {
						md5EncryptedPassword = userMgr.encryptWithOlderAlgo(userLoginId, currentPassword);
						if (md5EncryptedPassword.equals(dbPassword)) {
							validatePassword(newPassword);
							userMgr.updatePasswordInSHA256(userLoginId, newPassword, true);
							logger.info("User '" + userLoginId + "' Password updated sucessfully.");
						} else {
							System.out.println(
									"Skipping default password change request as provided password doesn't match with existing password.");
							logger.error(
									"Skipping default password change request as provided password doesn't match with existing password.");
							System.exit(2);
						}
					} else if (userMgr.isPasswordValid(userLoginId, dbPassword, currentPassword)) {
						validatePassword(newPassword);
						userMgr.updatePasswordInSHA256(userLoginId, newPassword, true);
						logger.info("User '" + userLoginId + "' Password updated sucessfully.");
					}
				} else {
					currentEncryptedPassword = userMgr.encrypt(userLoginId, currentPassword);
					if (currentEncryptedPassword.equals(dbPassword)) {
						validatePassword(newPassword);
						userMgr.updatePasswordInSHA256(userLoginId, newPassword, true);
						logger.info("User '" + userLoginId + "' Password updated sucessfully.");
					} else if (!currentEncryptedPassword.equals(dbPassword) && defaultPwdChangeRequest) {
						logger.info("current encryped password is not equal to dbpassword , trying with md5 now");
						md5EncryptedPassword = userMgr.encryptWithOlderAlgo(userLoginId, currentPassword);
						if (md5EncryptedPassword.equals(dbPassword)) {
							validatePassword(newPassword);
							userMgr.updatePasswordInSHA256(userLoginId, newPassword, true);
							logger.info("User '" + userLoginId + "' Password updated sucessfully.");
						} else {
							System.out.println(
									"Skipping default password change request as provided password doesn't match with existing password.");
							logger.error(
									"Skipping default password change request as provided password doesn't match with existing password.");
							System.exit(2);
						}
					} else {
						System.out.println("Invalid user password");
						logger.error("Invalid user password");
						System.exit(1);
					}
				}
			} catch (Exception e) {
				logger.error("Update Admin Password failure. Detail:  \n", e);
				System.exit(1);
			}
		} else {
			System.out.println("User does not exist in DB!!");
			logger.error("User does not exist in DB");
			System.exit(1);
		}
	}

	public void updateMultiplePasswords() {
		for (int i=0; i<userPwdArgs.length ; i+=3) {
			if ("-default".equalsIgnoreCase(userPwdArgs[i])) {
				continue;
			}
			String userLoginIdTemp=userPwdArgs[i];
			String currentPasswordTemp=userPwdArgs[i+1];
			String newPasswordTemp=userPwdArgs[i+2];
			if (StringUtils.isEmpty(userLoginIdTemp)) {
				System.out.println("Invalid login ID. Exiting!!!");
				logger.info("Invalid login ID. Exiting!!!");
				System.exit(1);
			}
			if (StringUtils.isEmpty(currentPasswordTemp)) {
				System.out.println("Invalid current password. Exiting!!!");
				logger.info("Invalid current password. Exiting!!!");
				System.exit(1);
			}
			if (StringUtils.isEmpty(newPasswordTemp)) {
				System.out.println("Invalid new password. Exiting!!!");
				logger.info("Invalid new password. Exiting!!!");
				System.exit(1);
			}
			XXPortalUser xPortalUser = daoMgr.getXXPortalUser().findByLoginId(userLoginIdTemp);
			if (xPortalUser != null) {
				String dbPassword = xPortalUser.getPassword();
				String currentEncryptedPassword = null;
				String md5EncryptedPassword = null;
				try {
					currentEncryptedPassword = userMgr.encrypt(userLoginIdTemp, currentPasswordTemp);
					if (currentEncryptedPassword.equals(dbPassword)) {
						validatePassword(newPasswordTemp);
						userMgr.updatePasswordInSHA256(userLoginIdTemp, newPasswordTemp, true);
						logger.info("User '" + userLoginIdTemp + "' Password updated sucessfully.");
					} else if (!currentEncryptedPassword.equals(dbPassword) && defaultPwdChangeRequest) {
						logger.info("current encryped password is not equal to dbpassword , trying with md5 now");
						md5EncryptedPassword = userMgr.encryptWithOlderAlgo(userLoginIdTemp, currentPasswordTemp);
						if (md5EncryptedPassword.equals(dbPassword)) {
							validatePassword(newPasswordTemp);
							userMgr.updatePasswordInSHA256(userLoginIdTemp, newPasswordTemp, true);
							logger.info("User '" + userLoginIdTemp + "' Password updated sucessfully.");
						} else {
							System.out.println(
									"Skipping default password change request as provided password doesn't match with existing password.");
							logger.error(
									"Skipping default password change request as provided password doesn't match with existing password.");
							System.exit(2);
						}
					} else {
						System.out.println("Invalid user password");
						logger.error("Invalid user password");
						System.exit(1);
						break;
					}
				} catch (Exception e) {
					logger.error("Update Admin Password failure. Detail:  \n", e);
					System.exit(1);
					break;
				}
			} else {
				System.out.println("User does not exist in DB!!");
				logger.error("User does not exist in DB");
				System.exit(1);
				break;
			}
		}
	}

	private void validatePassword(String newPassword) {
		boolean checkPassword = false;
		if (newPassword != null) {
			checkPassword = newPassword.trim().matches(StringUtil.VALIDATION_CRED);
			if (!checkPassword) {
				String msg = "Password should be minimum 8 characters, at least one uppercase letter, one lowercase letter and one numeric.";
				logger.error(msg);
				System.out.println(msg);
				throw restErrorUtil.createRESTException("serverMsg.changePasswordValidatePassword",
						MessageEnums.INVALID_PASSWORD, null, msg, null);
			}
		} else {
			logger.error("validatePassword(). Password cannot be blank/null.");
			System.out.println("validatePassword(). Password cannot be blank/null.");
			throw restErrorUtil.createRESTException("serverMsg.changePasswordValidatePassword",
					MessageEnums.INVALID_PASSWORD, null, "Password cannot be blank/null", null);
		}
	}

}
