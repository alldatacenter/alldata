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

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.persistence.Query;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.authorization.hadoop.config.RangerAdminConfig;
import org.apache.ranger.common.AppConstants;
import org.apache.ranger.common.ContextUtil;
import org.apache.ranger.common.DateUtil;
import org.apache.ranger.common.GUIDUtil;
import org.apache.ranger.common.MessageEnums;
import org.apache.ranger.common.PropertiesUtil;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.common.RangerCommonEnums;
import org.apache.ranger.common.RangerConfigUtil;
import org.apache.ranger.common.RangerConstants;
import org.apache.ranger.common.SearchCriteria;
import org.apache.ranger.common.SearchUtil;
import org.apache.ranger.common.StringUtil;
import org.apache.ranger.common.UserSessionBase;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.entity.XXGroupPermission;
import org.apache.ranger.entity.XXPortalUser;
import org.apache.ranger.entity.XXPortalUserRole;
import org.apache.ranger.entity.XXTrxLog;
import org.apache.ranger.entity.XXUser;
import org.apache.ranger.entity.XXUserPermission;
import org.apache.ranger.service.XGroupPermissionService;
import org.apache.ranger.service.XPortalUserService;
import org.apache.ranger.service.XUserPermissionService;
import org.apache.ranger.util.Pbkdf2PasswordEncoderCust;
import org.apache.ranger.view.VXGroupPermission;
import org.apache.ranger.view.VXPasswordChange;
import org.apache.ranger.view.VXPortalUser;
import org.apache.ranger.view.VXPortalUserList;
import org.apache.ranger.view.VXResponse;
import org.apache.ranger.view.VXString;
import org.apache.ranger.view.VXUserPermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class UserMgr {

	private static final Logger logger = LoggerFactory.getLogger(UserMgr.class);
	@Autowired
	RangerDaoManager daoManager;

	@Autowired
	RESTErrorUtil restErrorUtil;

	@Autowired
	StringUtil stringUtil;

	@Autowired
	SearchUtil searchUtil;

	@Autowired
        RangerBizUtil rangerBizUtil;

	@Autowired
	SessionMgr sessionMgr;

	@Autowired
	DateUtil dateUtil;

	@Autowired
	RangerConfigUtil configUtil;

	@Autowired
	XPortalUserService xPortalUserService;

	@Autowired
	XUserPermissionService xUserPermissionService;

	@Autowired
	XGroupPermissionService xGroupPermissionService;
	
	@Autowired
	XUserMgr xUserMgr;

	@Autowired
	GUIDUtil guidUtil;

	private final boolean isFipsEnabled;
	private static final int DEFAULT_PASSWORD_HISTORY_COUNT = 4;
	private int passwordHistoryCount = PropertiesUtil.getIntProperty("ranger.password.history.count", DEFAULT_PASSWORD_HISTORY_COUNT);
	
	String publicRoles[] = new String[] { RangerConstants.ROLE_USER,
			RangerConstants.ROLE_OTHER };

	private static final List<String> DEFAULT_ROLE_LIST = new ArrayList<String>(
			1);

	private static final List<String> VALID_ROLE_LIST = new ArrayList<String>(2);

	static {
		DEFAULT_ROLE_LIST.add(RangerConstants.ROLE_USER);
		VALID_ROLE_LIST.add(RangerConstants.ROLE_SYS_ADMIN);
		VALID_ROLE_LIST.add(RangerConstants.ROLE_USER);
		VALID_ROLE_LIST.add(RangerConstants.ROLE_KEY_ADMIN);
        VALID_ROLE_LIST.add(RangerConstants.ROLE_ADMIN_AUDITOR);
        VALID_ROLE_LIST.add(RangerConstants.ROLE_KEY_ADMIN_AUDITOR);
	}

	public UserMgr() {
		if (logger.isDebugEnabled()) {
			logger.debug("UserMgr()");
		}
		this.isFipsEnabled = RangerAdminConfig.getInstance().isFipsEnabled();
		if (passwordHistoryCount < 0) {
			passwordHistoryCount = 0;
		}
	}

	public XXPortalUser createUser(VXPortalUser userProfile, int userStatus,
			Collection<String> userRoleList) {
		XXPortalUser user = mapVXPortalUserToXXPortalUser(userProfile);
		checkAdminAccess();
                rangerBizUtil.blockAuditorRoleUser();
                List<String> userRolesList = new ArrayList<String>(userRoleList);
        xUserMgr.checkAccessRoles(userRolesList);
		user = createUser(user, userStatus, userRoleList);

		return user;
	}

	public XXPortalUser createUser(XXPortalUser user, int userStatus,
			Collection<String> userRoleList) {
		user.setStatus(userStatus);
		String saltEncodedpasswd = encrypt(user.getLoginId(),
				user.getPassword());
		user.setPassword(saltEncodedpasswd);
		user.setPasswordUpdatedTime(DateUtil.getUTCDate());
		daoManager.getXXPortalUser().create(user);
		XXPortalUser xXPortalUser = daoManager.getXXPortalUser().findByLoginId(user.getLoginId());
		// Create the XXPortalUserRole entries for this user
		if (xXPortalUser != null && xXPortalUser.getId() != null) {
			if (CollectionUtils.isNotEmpty(userRoleList)) {
				for (String userRole : userRoleList) {
					addUserRole(xXPortalUser.getId(), userRole);
				}
			}
		} else {
			logger.error("XXPortalUser user creation failed for user=" + user.getLoginId());
		}

		return xXPortalUser;
	}

	public XXPortalUser createUser(VXPortalUser userProfile, int userStatus) {
		ArrayList<String> roleList = new ArrayList<String>();
		Collection<String> reqRoleList = userProfile.getUserRoleList();
		if (reqRoleList != null && reqRoleList.size() > 0) {
            for (String role : reqRoleList) {
                if (role != null) {
                    roleList.add(role);
                } else {
                    roleList.add(RangerConstants.ROLE_USER);
                }
            }
		} else {
			roleList.add(RangerConstants.ROLE_USER);
		}

		return createUser(userProfile, userStatus, roleList);
	}

	/**
	 * @param userProfile
	 * @return
	 */
	public XXPortalUser updateUser(VXPortalUser userProfile) {
		XXPortalUser gjUser = daoManager.getXXPortalUser().getById(
				userProfile.getId());

		if (gjUser == null) {
			logger.error("updateUser(). User not found. userProfile="
					+ userProfile);
			return null;
		}

		checkAccess(gjUser);
                rangerBizUtil.blockAuditorRoleUser();
		boolean updateUser = false;
		// Selectively update fields

		// status
		if (userProfile.getStatus() != gjUser.getStatus()) {
			updateUser = true;
		}

		// Allowing email address update even when its set to empty.
		// emailAddress
		String emailAddress = userProfile.getEmailAddress();
		if (stringUtil.isEmpty(emailAddress)) {
			userProfile.setEmailAddress(null);
			updateUser = true;
		} else {
			if (stringUtil.validateEmail(emailAddress)) {
				XXPortalUser checkUser = daoManager.getXXPortalUser()
						.findByEmailAddress(emailAddress);
				if (checkUser != null) {
					String loginId = userProfile.getLoginId();
					if (loginId == null) {
						throw restErrorUtil.createRESTException(
								"Invalid user, please provide valid "
										+ "username.",
								MessageEnums.INVALID_INPUT_DATA);
					} else if (!loginId.equals(checkUser.getLoginId())) {
						throw restErrorUtil
								.createRESTException(
										"The email address "
												+ "you've provided already exists in system.",
										MessageEnums.INVALID_INPUT_DATA);
					} else {
						userProfile.setEmailAddress(emailAddress);
						updateUser = true;
					}
				} else {
					userProfile.setEmailAddress(emailAddress);
					updateUser = true;
				}
			} else {
				throw restErrorUtil.createRESTException(
						"Please provide valid email address.",
						MessageEnums.INVALID_INPUT_DATA);
			}
		}

		// loginId
		// if (!stringUtil.isEmpty(userProfile.getLoginId())
		// && !userProfile.getLoginId().equals(gjUser.getLoginId())) {
		// gjUser.setLoginId(userProfile.getLoginId());
		// updateUser = true;
		// }

		// firstName
		if("null".equalsIgnoreCase(userProfile.getFirstName())){
			userProfile.setFirstName("");
		}
		if (!stringUtil.isEmpty(userProfile.getFirstName())
				&& !userProfile.getFirstName().equals(gjUser.getFirstName())) {
			userProfile.setFirstName(stringUtil.toCamelCaseAllWords(userProfile
					.getFirstName()));
			updateUser = true;
		}

		if("null".equalsIgnoreCase(userProfile.getLastName())){
			userProfile.setLastName("");
		}
		if (!stringUtil.isEmpty(userProfile.getLastName())
				&& !userProfile.getLastName().equals(gjUser.getLastName())) {
			userProfile.setLastName(stringUtil.toCamelCaseAllWords(userProfile
					.getLastName()));
			updateUser = true;
		}

		// publicScreenName
		if (userProfile.getFirstName() != null
				&& userProfile.getLastName() != null
				&& !userProfile.getFirstName().trim().isEmpty()
				&& !userProfile.getLastName().trim().isEmpty()) {
			userProfile.setPublicScreenName(userProfile.getFirstName() + " "
					+ userProfile.getLastName());
			updateUser = true;
		} else {
			userProfile.setPublicScreenName(gjUser.getLoginId());
			updateUser = true;
		}

		// notes
		/*
		 * if (!stringUtil.isEmpty(userProfile.getNotes()) &&
		 * !userProfile.getNotes().equalsIgnoreCase(gjUser.getNotes())) {
		 * updateUser = true; }
		 */

		// userRoleList
		updateRoles(userProfile.getId(), userProfile.getUserRoleList());

		if (updateUser) {

			List<XXTrxLog> trxLogList = xPortalUserService.getTransactionLog(
					userProfile, gjUser, "update");

			userProfile.setPassword(gjUser.getPassword());
			xPortalUserService.updateResource(userProfile);
			sessionMgr.resetUserSessionForProfiles(ContextUtil
					.getCurrentUserSession());

                        rangerBizUtil.createTrxLog(trxLogList);
		}

		return gjUser;
	}

	public boolean updateRoles(Long userId, Collection<String> rolesList) {
		boolean rolesUpdated = false;
		if (rolesList == null || rolesList.size() == 0) {
			return false;
		}
		List<String> stringRolesList = new ArrayList<String>();
		for (String userRole : rolesList) {
			if(!VALID_ROLE_LIST.contains(userRole.toUpperCase())){
				throw restErrorUtil.createRESTException("Invalid user role, please provide valid user role.",MessageEnums.INVALID_INPUT_DATA);
			}
			stringRolesList.add(userRole);
		}
		xUserMgr.checkAccessRoles(stringRolesList);
                rangerBizUtil.blockAuditorRoleUser();
		// Let's first delete old roles
		List<XXPortalUserRole> gjUserRoles = daoManager.getXXPortalUserRole()
				.findByUserId(userId);

		for (XXPortalUserRole gjUserRole : gjUserRoles) {
			boolean found = false;
			for (String userRole : rolesList) {
				if (gjUserRole.getUserRole().equalsIgnoreCase(userRole)) {
					found = true;
					break;
				}
			}
			if (!found) {
				if (deleteUserRole(userId, gjUserRole)) {
					rolesUpdated = true;
				}
			}
		}

		// Let's add new roles
		for (String userRole : rolesList) {
			boolean found = false;
			for (XXPortalUserRole gjUserRole : gjUserRoles) {
				if (gjUserRole.getUserRole().equalsIgnoreCase(userRole)) {
					found = true;
					break;
				}
			}
			if (!found) {
				if (addUserRole(userId, userRole) != null) {
					rolesUpdated = true;
				}
			}
		}
		return rolesUpdated;
	}

	/**
	 * @param userId
	 * @param vStringRolesList
	 */
	public void setUserRoles(Long userId, List<VXString> vStringRolesList) {
		List<String> stringRolesList = new ArrayList<String>();
		for (VXString vXString : vStringRolesList) {
			stringRolesList.add(vXString.getValue());
		}
		xUserMgr.checkAccessRoles(stringRolesList);
                rangerBizUtil.blockAuditorRoleUser();
		VXPortalUser oldUserProfile=getUserProfile(userId);
		xUserMgr.updateUserRolesPermissions(oldUserProfile, stringRolesList);
	}

	/**
	 * @param pwdChange
         * @return
         */
        public VXResponse changePassword(VXPasswordChange pwdChange) {

                VXResponse ret = new VXResponse();

                // First let's get the XXPortalUser for the current logged in user
		String currentUserLoginId = ContextUtil.getCurrentUserLoginId();
		XXPortalUser gjUserCurrent = daoManager.getXXPortalUser().findByLoginId(currentUserLoginId);
		checkAccessForUpdate(gjUserCurrent);

		// Get the user of whom we want to change the password
		XXPortalUser gjUser = daoManager.getXXPortalUser().findByLoginId(pwdChange.getLoginId());
		if (gjUser == null) {
			logger.warn("SECURITY:changePassword(). User not found. LoginId="+ pwdChange.getLoginId());
			throw restErrorUtil.createRESTException("serverMsg.userMgrInvalidUser",MessageEnums.DATA_NOT_FOUND, null, null,pwdChange.getLoginId());
		}
        if (gjUser.getUserSource() == RangerCommonEnums.USER_EXTERNAL) {
            logger.info("SECURITY:changePassword().Ranger External Users cannot change password. LoginId=" + pwdChange.getLoginId());
            VXResponse vXResponse = new VXResponse();
            vXResponse.setStatusCode(HttpServletResponse.SC_FORBIDDEN);
            vXResponse.setMsgDesc("SECURITY:changePassword().Ranger External Users cannot change password. LoginId=" + pwdChange.getLoginId());
            throw restErrorUtil.generateRESTException(vXResponse);
        }
        
        String currentPassword = gjUser.getPassword();
		//check current password and provided old password is same or not
		if (this.isFipsEnabled) {
			if (!isPasswordValid(pwdChange.getLoginId(), currentPassword, pwdChange.getOldPassword())) {
				logger.info("changePassword(). Invalid old password. LoginId="+ pwdChange.getLoginId());
				throw restErrorUtil.createRESTException("serverMsg.userMgrOldPassword",MessageEnums.INVALID_INPUT_DATA, null, null,pwdChange.getLoginId());
				}
			} else {
				String encryptedOldPwd = encrypt(pwdChange.getLoginId(),pwdChange.getOldPassword());
				if (!stringUtil.equals(encryptedOldPwd, gjUser.getPassword())) {
					logger.info("changePassword(). Invalid old password. LoginId="+ pwdChange.getLoginId());
					throw restErrorUtil.createRESTException("serverMsg.userMgrOldPassword",MessageEnums.INVALID_INPUT_DATA, null, null,pwdChange.getLoginId());
				}
			}
		//validate new password
		if (!stringUtil.validatePassword(pwdChange.getUpdPassword(),new String[] { gjUser.getFirstName(),gjUser.getLastName(), gjUser.getLoginId()})) {
			logger.warn("SECURITY:changePassword(). Invalid new password. LoginId="+ pwdChange.getLoginId());
			throw restErrorUtil.createRESTException("serverMsg.userMgrNewPassword",MessageEnums.INVALID_PASSWORD, null, null,pwdChange.getLoginId());
		}

		String encryptedNewPwd = encrypt(pwdChange.getLoginId(),pwdChange.getUpdPassword());
		String oldPasswordStr = gjUser.getOldPasswords();
		List<String> oldPasswords;

		if (StringUtils.isNotEmpty(oldPasswordStr)) {
			oldPasswords = new ArrayList<>(Arrays.asList(oldPasswordStr.split(",")));
		} else {
			oldPasswords = new ArrayList<>();
		}
		oldPasswords.add(gjUser.getPassword());
		while (oldPasswords.size() > this.passwordHistoryCount) {
			oldPasswords.remove(0);
		}
		boolean isNewPasswordDifferent = oldPasswords.isEmpty();
		for (String oldPassword : oldPasswords) {
			if (this.isFipsEnabled) {
				isNewPasswordDifferent = isNewPasswordDifferent(pwdChange.getLoginId(), oldPassword, encryptedNewPwd);
			} else {
				isNewPasswordDifferent = !encryptedNewPwd.equals(oldPassword);
			}
			if (!isNewPasswordDifferent){
				break;
			}
		}
			if (isNewPasswordDifferent) {
				List<XXTrxLog> trxLogList = new ArrayList<XXTrxLog>();
				XXTrxLog xTrxLog = new XXTrxLog();
				xTrxLog.setAttributeName("Password");
				xTrxLog.setPreviousValue(currentPassword);
				xTrxLog.setNewValue(encryptedNewPwd);
				xTrxLog.setAction("password change");
				xTrxLog.setObjectClassType(AppConstants.CLASS_TYPE_PASSWORD_CHANGE);
				xTrxLog.setObjectId(pwdChange.getId());
				xTrxLog.setObjectName(pwdChange.getLoginId());
				trxLogList.add(xTrxLog);
	                        rangerBizUtil.createTrxLog(trxLogList);
				gjUser.setPassword(encryptedNewPwd);
				updateOldPasswords(gjUser, oldPasswords);
				gjUser = daoManager.getXXPortalUser().update(gjUser);
				ret.setMsgDesc("Password successfully updated");
				ret.setStatusCode(VXResponse.STATUS_SUCCESS);
			} else {
				logger.error("SECURITY:changePassword(). Password update failed. LoginId="+ pwdChange.getLoginId());
				ret.setMsgDesc("Password update failed");
				ret.setStatusCode(VXResponse.STATUS_ERROR);
				throw restErrorUtil.createRESTException("serverMsg.userMgrOldPassword",MessageEnums.INVALID_INPUT_DATA, gjUser.getId(),"password", gjUser.toString());
		}
		return ret;
	}

	private void updateOldPasswords(XXPortalUser gjUser, List<String> oldPasswords) {
		String oldPasswordStr = CollectionUtils.isNotEmpty(oldPasswords) ? StringUtils.join(oldPasswords, ",") : null;
		gjUser.setOldPasswords(oldPasswordStr);
		gjUser.setPasswordUpdatedTime(DateUtil.getUTCDate());
	}

	/**
	 * @param gjUser
	 * @param changeEmail
	 * @return
	 */
	public VXPortalUser changeEmailAddress(XXPortalUser gjUser,
			VXPasswordChange changeEmail) {
		checkAccessForUpdate(gjUser);
                rangerBizUtil.blockAuditorRoleUser();
		if (StringUtils.isEmpty(changeEmail.getEmailAddress())) {
			changeEmail.setEmailAddress(null);
		}

		if (!StringUtils.isEmpty(changeEmail.getEmailAddress()) && !stringUtil.validateEmail(changeEmail.getEmailAddress())) {
			logger.info("Invalid email address." + changeEmail);
			throw restErrorUtil.createRESTException(
					"serverMsg.userMgrInvalidEmail",
					MessageEnums.INVALID_INPUT_DATA, changeEmail.getId(),
					"emailAddress", changeEmail.toString());

		}
		
		if (this.isFipsEnabled) {
			if (!isPasswordValid(changeEmail.getLoginId(), gjUser.getPassword(), changeEmail.getOldPassword())) {
				logger.info("changeEmailAddress(). Invalid  password. changeEmail="
								+ changeEmail);
								throw restErrorUtil.createRESTException(
											"serverMsg.userMgrWrongPassword",
												MessageEnums.OPER_NO_PERMISSION, null, null, ""
														+ changeEmail);
					}
		} else {
			String encryptedOldPwd = encrypt(gjUser.getLoginId(), changeEmail.getOldPassword());
			if (!stringUtil.equals(encryptedOldPwd, gjUser.getPassword())) {
				encryptedOldPwd = encryptWithOlderAlgo(gjUser.getLoginId(), changeEmail.getOldPassword());
				if (!stringUtil.equals(encryptedOldPwd, gjUser.getPassword())) {
					logger.info("changeEmailAddress(). Invalid  password. changeEmail=" + changeEmail);
					throw restErrorUtil.createRESTException("serverMsg.userMgrWrongPassword",
							MessageEnums.OPER_NO_PERMISSION, null, null, "" + changeEmail);
				}
			}
		}

		// Normalize email. Make it lower case
		gjUser.setEmailAddress(stringUtil.normalizeEmail(changeEmail
				.getEmailAddress()));

		String saltEncodedpasswd = encrypt(gjUser.getLoginId(),
				changeEmail.getOldPassword());
        if (gjUser.getUserSource() == RangerCommonEnums.USER_APP) {
		gjUser.setPassword(saltEncodedpasswd);
       }
        else if (gjUser.getUserSource() == RangerCommonEnums.USER_EXTERNAL) {
                gjUser.setPassword(gjUser.getPassword());
        }
		daoManager.getXXPortalUser().update(gjUser);
		return mapXXPortalUserVXPortalUser(gjUser);
	}

	/**
	 * @param gjUser
	 */
	public VXPortalUser deactivateUser(XXPortalUser gjUser) {
		checkAdminAccess();
                rangerBizUtil.blockAuditorRoleUser();
		if (gjUser != null
				&& gjUser.getStatus() != RangerConstants.ACT_STATUS_DEACTIVATED) {
			logger.info("Marking user " + gjUser.getLoginId() + " as deleted");
			gjUser.setStatus(RangerConstants.ACT_STATUS_DEACTIVATED);
			gjUser = daoManager.getXXPortalUser().update(gjUser);
			return mapXXPortalUserVXPortalUser(gjUser);
		}
		return null;
	}

	public VXPortalUser getUserProfile(Long id) {
		XXPortalUser user = daoManager.getXXPortalUser().getById(id);
		if (user != null) {
			checkAccess(user);
			return mapXXPortalUserVXPortalUser(user);
		} else {
			if (logger.isDebugEnabled()) {
				logger.debug("User not found. userId=" + id);
			}
			return null;
		}
	}

	public VXPortalUser getUserProfileByLoginId() {
		String loginId = ContextUtil.getCurrentUserLoginId();
		return getUserProfileByLoginId(loginId);
	}

	public VXPortalUser getUserProfileByLoginId(String loginId) {
		XXPortalUser user = daoManager.getXXPortalUser().findByLoginId(loginId);
		if (user != null) {
			return mapXXPortalUserVXPortalUser(user);
		} else {
			if (logger.isDebugEnabled()) {
				logger.debug("User not found. loginId=" + loginId);
			}
			return null;
		}
	}

	public XXPortalUser mapVXPortalUserToXXPortalUser(VXPortalUser userProfile) {
		XXPortalUser gjUser = new XXPortalUser();
		gjUser.setEmailAddress(userProfile.getEmailAddress());
		if("null".equalsIgnoreCase(userProfile.getFirstName())){
			userProfile.setFirstName("");
		}
		gjUser.setFirstName(userProfile.getFirstName());
		if("null".equalsIgnoreCase(userProfile.getLastName())){
			userProfile.setLastName("");
		}
		gjUser.setLastName(userProfile.getLastName());
		if (userProfile.getLoginId() == null
				|| userProfile.getLoginId().trim().isEmpty()
				|| "null".equalsIgnoreCase(userProfile.getLoginId())) {
			throw restErrorUtil.createRESTException(
					"LoginId should not be null or blank, It is",
					MessageEnums.INVALID_INPUT_DATA);
		}
		gjUser.setLoginId(userProfile.getLoginId());
		gjUser.setPassword(userProfile.getPassword());
		gjUser.setUserSource(userProfile.getUserSource());
		gjUser.setPublicScreenName(userProfile.getPublicScreenName());
		gjUser.setOtherAttributes(userProfile.getOtherAttributes());
		gjUser.setSyncSource(userProfile.getSyncSource());
		gjUser.setStatus(userProfile.getStatus());
		if (userProfile.getFirstName() != null
				&& userProfile.getLastName() != null
				&& !userProfile.getFirstName().trim().isEmpty()
				&& !userProfile.getLastName().trim().isEmpty()) {
			gjUser.setPublicScreenName(userProfile.getFirstName() + " "
					+ userProfile.getLastName());
		} else {
			gjUser.setPublicScreenName(userProfile.getLoginId());
		}
		return gjUser;
	}

	/**
	 * @param user
	 * @return
	 */
	public VXPortalUser mapXXPortalUserToVXPortalUser(XXPortalUser user,
			Collection<String> userRoleList) {
		if (user == null) {
			return null;
		}
		UserSessionBase sess = ContextUtil.getCurrentUserSession();
		if (sess == null) {
			return null;
		}

		VXPortalUser userProfile = new VXPortalUser();
		gjUserToUserProfile(user, userProfile);
		if (sess.isUserAdmin() || sess.isKeyAdmin()
				|| sess.getXXPortalUser().getId().equals(user.getId())) {
			if (userRoleList == null) {
				userRoleList = new ArrayList<String>();
				List<XXPortalUserRole> gjUserRoleList = daoManager
						.getXXPortalUserRole().findByParentId(user.getId());

				for (XXPortalUserRole userRole : gjUserRoleList) {
					userRoleList.add(userRole.getUserRole());
				}
			}

			userProfile.setUserRoleList(userRoleList);
		}
		userProfile.setUserSource(user.getUserSource());
		return userProfile;
	}

	protected void gjUserToUserProfile(XXPortalUser user, VXPortalUser userProfile) {
		UserSessionBase sess = ContextUtil.getCurrentUserSession();
		if (sess == null) {
			return;
		}

		// Admin
		if (sess.isUserAdmin() || sess.isKeyAdmin()
				|| sess.getXXPortalUser().getId().equals(user.getId())) {
			userProfile.setLoginId(user.getLoginId());
			userProfile.setStatus(user.getStatus());
			userProfile.setUserRoleList(new ArrayList<String>());

			String emailAddress = user.getEmailAddress();

			if (emailAddress != null && stringUtil.validateEmail(emailAddress)) {
				userProfile.setEmailAddress(user.getEmailAddress());
			}

			userProfile.setUserSource(sess.getAuthProvider());

			List<XXPortalUserRole> gjUserRoleList = daoManager
					.getXXPortalUserRole().findByParentId(user.getId());

			for (XXPortalUserRole gjUserRole : gjUserRoleList) {
				userProfile.getUserRoleList().add(gjUserRole.getUserRole());
			}

			userProfile.setId(user.getId());
			List<XXUserPermission> xUserPermissions = daoManager
					.getXXUserPermission().findByUserPermissionIdAndIsAllowed(
							userProfile.getId());
			List<XXGroupPermission> xxGroupPermissions = daoManager
					.getXXGroupPermission().findbyVXPortalUserId(
							userProfile.getId());

			List<VXGroupPermission> groupPermissions = new ArrayList<VXGroupPermission>();
			List<VXUserPermission> vxUserPermissions = new ArrayList<VXUserPermission>();
			for (XXGroupPermission xxGroupPermission : xxGroupPermissions) {
				VXGroupPermission groupPermission = xGroupPermissionService
						.populateViewBean(xxGroupPermission);
				groupPermission.setModuleName(daoManager.getXXModuleDef()
						.findByModuleId(groupPermission.getModuleId())
						.getModule());
				groupPermissions.add(groupPermission);
			}
			for (XXUserPermission xUserPermission : xUserPermissions) {
				VXUserPermission vXUserPermission = xUserPermissionService
						.populateViewBean(xUserPermission);
				vXUserPermission.setModuleName(daoManager.getXXModuleDef()
						.findByModuleId(vXUserPermission.getModuleId())
						.getModule());
				vxUserPermissions.add(vXUserPermission);
			}
			userProfile.setGroupPermissions(groupPermissions);
			userProfile.setUserPermList(vxUserPermissions);
			userProfile.setFirstName(user.getFirstName());
			userProfile.setLastName(user.getLastName());
			userProfile.setPublicScreenName(user.getPublicScreenName());
		}

	}

	/**
	 * Translates XXPortalUser to VUserProfile. This method should be called in
	 * the same transaction in which the XXPortalUser was retrieved from the
	 * database
	 *
	 * @param user
	 * @return
	 */
	public VXPortalUser mapXXPortalUserVXPortalUser(XXPortalUser user) {
		return mapXXPortalUserToVXPortalUser(user, null);
	}

	/**
	 * @param emailId
	 * @return
	 */
	public XXPortalUser findByEmailAddress(String emailId) {
		return daoManager.getXXPortalUser().findByEmailAddress(emailId);
	}

	public XXPortalUser findByLoginId(String loginId) {
		return daoManager.getXXPortalUser().findByLoginId(loginId);
	}

	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public Collection<String> getRolesForUser(XXPortalUser user) {
		Collection<String> roleList = new ArrayList<String>();

		Collection<XXPortalUserRole> roleCollection = daoManager
				.getXXPortalUserRole().findByUserId(user.getId());
		for (XXPortalUserRole role : roleCollection) {
			roleList.add(role.getUserRole());
		}
		return roleList;
	}

	/**
	 * @param searchCriteria
	 * @return
	 */
	public VXPortalUserList searchUsers(SearchCriteria searchCriteria) {

		VXPortalUserList returnList = new VXPortalUserList();
		ArrayList<VXPortalUser> objectList = new ArrayList<VXPortalUser>();
		String queryStr = "SELECT u FROM  XXPortalUser u ";
		String countQueryStr = "SELECT COUNT(u) FROM XXPortalUser u ";

		// Get total count first
		Query query = createUserSearchQuery(countQueryStr, null, searchCriteria);
		Long count = (Long) query.getSingleResult();
		int resultSize = count!=null ? count.intValue() :0;
		if (resultSize == 0) {
			return returnList;
		}

		// Get actual data

		// Add sort by
		String sortBy = searchCriteria.getSortBy();
		String querySortBy = "u.loginId";
		if (sortBy != null && !sortBy.trim().isEmpty()) {
			sortBy = sortBy.trim();
			if (sortBy.equalsIgnoreCase("userId")) {
				querySortBy = "u.id";
			} else if (sortBy.equalsIgnoreCase("loginId")) {
				querySortBy = "ua.loginId";
			} else if (sortBy.equalsIgnoreCase("emailAddress")) {
				querySortBy = "u.emailAddress";
			} else if (sortBy.equalsIgnoreCase("firstName")) {
				querySortBy = "u.firstName";
			} else if (sortBy.equalsIgnoreCase("lastName")) {
				querySortBy = "u.lastName";
			} else {
				sortBy = "loginId";
				logger.error("Invalid sortBy provided. sortBy=" + sortBy);
			}
		} else {
			sortBy = "loginId";
		}

		// Default sort field
		String sortClause = " order by " + querySortBy + " ";

		// Add sort type
		String sortType = searchCriteria.getSortType();
		String querySortType = "asc";
		if (sortType != null) {
			if (sortType.equalsIgnoreCase("asc")
					|| sortType.equalsIgnoreCase("desc")) {
				querySortType = sortType;
			} else {
				logger.error("Invalid sortType. sortType=" + sortType);
			}
		}
		sortClause += querySortType;

		query = createUserSearchQuery(queryStr, sortClause, searchCriteria);

		// Set start index
		query.setFirstResult(searchCriteria.getStartIndex());

		searchUtil.updateQueryPageSize(query, searchCriteria);

		@SuppressWarnings("rawtypes")
		List resultList = query.getResultList();
		// Iterate over the result list and create the return list
		for (Object object : resultList) {
			XXPortalUser gjUser = (XXPortalUser) object;
			VXPortalUser userProfile = new VXPortalUser();
			gjUserToUserProfile(gjUser, userProfile);
			objectList.add(userProfile);
		}

		returnList.setResultSize(resultSize);
		returnList.setPageSize(query.getMaxResults());
		returnList.setSortBy(sortBy);
		returnList.setSortType(querySortType);
		returnList.setStartIndex(query.getFirstResult());
		returnList.setTotalCount(count.longValue());
		returnList.setVXPortalUsers(objectList);
		return returnList;
	}

	/**
	 * @param queryStr
	 * @param sortClause
	 * @param searchCriteria
	 * @return
	 */
	protected Query createUserSearchQuery(String queryStr, String sortClause,
			SearchCriteria searchCriteria) {
		HashMap<String, Object> paramList = searchCriteria.getParamList();

		String whereClause = "WHERE 1 = 1 ";

		// roles
		@SuppressWarnings("unchecked")
		List<String> roleList = (List<String>) paramList.get("roleList");
		if (roleList != null && roleList.size() > 0) {
			whereClause = ", XXPortalUserRole ur WHERE u.id = ur.userId";
			if (roleList.size() == 1) {
				// For only one role, let's do an equal to
				whereClause += " and ur.userRole = :role";
			} else {
				whereClause += " and ur.userRole in (:roleList)";
			}
		}

		// userId
		Long userId = (Long) paramList.get("userId");
		if (userId != null) {
			whereClause += " and u.id = :userId ";
		}

		// loginId
		String loginId = (String) paramList.get("loginId");
		if (loginId != null) {
			whereClause += " and LOWER(u.loginId) = :loginId ";
		}

		// emailAddress
		String emailAddress = (String) paramList.get("emailAddress");
		if (emailAddress != null) {
			whereClause += " and LOWER(u.emailAddress) = :emailAddress ";
		}

		// firstName
		String firstName = (String) paramList.get("firstName");
		if (firstName != null) {
			whereClause += " and LOWER(u.firstName) = :firstName ";
		}

		// lastName
		String lastName = (String) paramList.get("lastName");
		if (lastName != null) {
			whereClause += " and LOWER(u.lastName) = :lastName ";
		}

		// status
		Integer status = null;
		@SuppressWarnings("unchecked")
		List<Integer> statusList = (List<Integer>) paramList.get("statusList");
		if (statusList != null && statusList.size() == 1) {
			// use == condition
			whereClause += " and u.status = :status";
			status = statusList.get(0);
		} else if (statusList != null && statusList.size() > 1) {
			// use in operator
			whereClause += " and u.status in (:statusList) ";
		}

		// publicScreenName
		String publicScreenName = (String) paramList.get("publicScreenName");
		if (publicScreenName != null) {
			whereClause += " and LOWER(u.publicScreenName) = :publicScreenName ";
		}

		// familyScreenName
		String familyScreenName = (String) paramList.get("familyScreenName");
		if (familyScreenName != null) {
			whereClause += " and LOWER(u.familyScreenName) = :familyScreenName ";
		}

		if (sortClause != null) {
			whereClause += sortClause;
		}

		Query query = daoManager.getEntityManager().createQuery(
				queryStr + whereClause);

		if (roleList != null && roleList.size() > 0) {
			if (roleList.size() == 1) {
				query.setParameter("role", roleList.get(0));
			} else {
				query.setParameter("roleList", roleList);
			}
		}

		if (status != null) {
			query.setParameter("status", status);
		}
		if (statusList != null && statusList.size() > 1) {
			query.setParameter("statusList", statusList);
		}
		if (emailAddress != null) {
			query.setParameter("emailAddress", emailAddress.toLowerCase());
		}

		// userId
		if (userId != null) {
			query.setParameter("userId", userId);
		}
		// firstName
		if (firstName != null) {
			query.setParameter("firstName", firstName.toLowerCase());
		}
		// lastName
		if (lastName != null) {
			query.setParameter("lastName", lastName.toLowerCase());
		}

		// loginId
		if (loginId != null) {
			query.setParameter("loginId", loginId.toLowerCase());
		}

		// publicScreenName
		if (publicScreenName != null) {
			query.setParameter("publicScreenName",
					publicScreenName.toLowerCase());
		}

		// familyScreenName
		if (familyScreenName != null) {
			query.setParameter("familyScreenName",
					familyScreenName.toLowerCase());
		}

		return query;
	}

	public boolean deleteUserRole(Long userId, String userRole) {
		List<XXPortalUserRole> roleList = daoManager.getXXPortalUserRole()
				.findByUserId(userId);
		for (XXPortalUserRole gjUserRole : roleList) {
			if (gjUserRole.getUserRole().equalsIgnoreCase(userRole)) {
				return deleteUserRole(userId, gjUserRole);
			}
		}
		return false;
	}

	public boolean deleteUserRole(Long userId, XXPortalUserRole gjUserRole) {
		/*
		 * if (RangerConstants.ROLE_USER.equals(gjUserRole.getUserRole())) {
		 * return false; }
		 */
		boolean publicRole = false;
		for (String publicRoleStr : publicRoles) {
			if (publicRoleStr.equalsIgnoreCase(gjUserRole.getUserRole())) {
				publicRole = true;
				break;
			}
		}
		if (!publicRole) {
			UserSessionBase sess = ContextUtil.getCurrentUserSession();
			if (sess == null || (!sess.isUserAdmin() && !sess.isKeyAdmin())) {
				return false;
			}
		}

		daoManager.getXXPortalUserRole().remove(gjUserRole.getId());
		return true;
	}

	public XXPortalUserRole addUserRole(Long userId, String userRole) {
		List<XXPortalUserRole> roleList = daoManager.getXXPortalUserRole()
				.findByUserId(userId);
		boolean publicRole = false;
		for (String publicRoleStr : publicRoles) {
			if (publicRoleStr.equalsIgnoreCase(userRole)) {
				publicRole = true;
				break;
			}
		}
		if (!publicRole) {
			UserSessionBase sess = ContextUtil.getCurrentUserSession();
			if (sess == null) {
				return null;
			}
			// Admin
			if (!sess.isUserAdmin() && !sess.isKeyAdmin()) {
				logger.error(
						"SECURITY WARNING: User trying to add non public role. userId="
								+ userId + ", role=" + userRole + ", session="
								+ sess.toString(), new Throwable());
				return null;
			}
                        rangerBizUtil.blockAuditorRoleUser();
		}

		for (XXPortalUserRole gjUserRole : roleList) {
			if (userRole.equalsIgnoreCase(gjUserRole.getUserRole())) {
				return gjUserRole;
			}
		}
		XXPortalUserRole userRoleObj = new XXPortalUserRole();
                if(!VALID_ROLE_LIST.contains(userRole.toUpperCase())){
                        throw restErrorUtil.createRESTException("Invalid user role, please provide valid user role.",MessageEnums.INVALID_INPUT_DATA);
                }
		userRoleObj.setUserRole(userRole.toUpperCase());
		userRoleObj.setUserId(userId);
		userRoleObj.setStatus(RangerConstants.STATUS_ENABLED);
		daoManager.getXXPortalUserRole().create(userRoleObj);

		// If role is not OTHER, then remove OTHER
		if (!RangerConstants.ROLE_OTHER.equalsIgnoreCase(userRole)) {
			deleteUserRole(userId, RangerConstants.ROLE_OTHER);
		}

		sessionMgr.resetUserSessionForProfiles(ContextUtil
				.getCurrentUserSession());
		return null;
	}

	public void checkAccess(Long userId) {
		XXPortalUser gjUser = daoManager.getXXPortalUser().getById(userId);
		if (gjUser == null) {
			throw restErrorUtil
					.create403RESTException("serverMsg.userMgrWrongUser: "
							+ userId);
		}

		checkAccess(gjUser);
	}

	/**
	 * @param gjUser
	 * @return
	 */
	public void checkAccess(XXPortalUser gjUser) {
		if (gjUser == null) {
			throw restErrorUtil
					.create403RESTException("serverMsg.userMgrWrongUser");
		}
		UserSessionBase sess = ContextUtil.getCurrentUserSession();
		if (sess != null) {

			// Admin
			if (sess.isUserAdmin() || sess.isKeyAdmin()) {
				return;
			}

			// Self
			if (sess.getXXPortalUser().getId().equals(gjUser.getId())) {
				return;
			}

		}
		throw restErrorUtil.create403RESTException("User "
				+ " access denied. loggedInUser="
				+ (sess != null ? sess.getXXPortalUser().getId()
						: "Not Logged In") + ", accessing user="
				+ gjUser.getId());

	}

	public void checkAccessForUpdate(XXPortalUser gjUser) {
		if (gjUser == null) {
			throw restErrorUtil
					.create403RESTException("serverMsg.userMgrWrongUser");
		}
		UserSessionBase sess = ContextUtil.getCurrentUserSession();
		if (sess != null) {

			// Admin
			if (sess.isUserAdmin()) {
				return;
			}

			// Self
			if (sess.getXXPortalUser().getId().equals(gjUser.getId())) {
				return;
			}

		}
		VXResponse vXResponse = new VXResponse();
		vXResponse.setStatusCode(HttpServletResponse.SC_FORBIDDEN);
		vXResponse.setMsgDesc("User "
				+ " access denied. loggedInUser="
				+ (sess != null ? sess.getXXPortalUser().getId()
						: "Not Logged In") + ", accessing user="
				+ gjUser.getId());
		throw restErrorUtil.generateRESTException(vXResponse);

	}

	public String encrypt(String loginId, String password) {
		String saltEncodedpasswd = "";
		if (this.isFipsEnabled) {
			try {
				Pbkdf2PasswordEncoderCust pbkdf2Encoder = new Pbkdf2PasswordEncoderCust(loginId);
				pbkdf2Encoder.setEncodeHashAsBase64(true);
				if (password != null) {
					saltEncodedpasswd = pbkdf2Encoder.encode(password);
				}
			} catch (Throwable t) {
					logger.error("Password doesn't meet requirements");
					throw restErrorUtil.createRESTException("Invalid password",
							MessageEnums.INVALID_PASSWORD, null, null, ""
									+ loginId);
			}
		} else {
			String sha256PasswordUpdateDisable = PropertiesUtil.getProperty("ranger.sha256Password.update.disable", "false");

			if ("false".equalsIgnoreCase(sha256PasswordUpdateDisable)) {
				saltEncodedpasswd = encodeString(password, loginId, "SHA-256");
			} else {
				saltEncodedpasswd = encodeString(password, loginId, "MD5");
			}
		}
		
		return saltEncodedpasswd;
	}

	public String encryptWithOlderAlgo(String loginId, String password) {
		String saltEncodedpasswd = "";

		saltEncodedpasswd = encodeString(password, loginId, "MD5");

		return saltEncodedpasswd;
	}

	public VXPortalUser createUser(VXPortalUser userProfile) {
		checkAdminAccess();
                rangerBizUtil.blockAuditorRoleUser();
		XXPortalUser xXPortalUser = this.createUser(userProfile,
				RangerCommonEnums.STATUS_ENABLED);
		return mapXXPortalUserVXPortalUser(xXPortalUser);
	}

	public VXPortalUser createDefaultAccountUser(VXPortalUser userProfile) {
		if (userProfile.getPassword() == null
				|| userProfile.getPassword().trim().isEmpty()) {
			userProfile.setUserSource(RangerCommonEnums.USER_EXTERNAL);
		}
		// access control
		checkAdminAccess();
                rangerBizUtil.blockAuditorRoleUser();
		logger.info("create:" + userProfile.getLoginId());
		XXPortalUser xXPortalUser = null;
                Collection<String> existingRoleList = null;
                Collection<String> reqRoleList = null;
		String loginId = userProfile.getLoginId();
		String emailAddress = userProfile.getEmailAddress();

                if (loginId != null && !loginId.isEmpty()) {
			xXPortalUser = this.findByLoginId(loginId);
			if (xXPortalUser == null) {
				if (emailAddress != null && !emailAddress.trim().isEmpty()) {
					xXPortalUser = this.findByEmailAddress(emailAddress);
					if (xXPortalUser == null) {
                                            xXPortalUser = this.createUser(userProfile,
								RangerCommonEnums.STATUS_ENABLED);
					} else {
						throw restErrorUtil
								.createRESTException(
										"The email address "
												+ emailAddress
												+ " you've provided already exists. Please try again with different "
												+ "email address.",
										MessageEnums.OPER_NOT_ALLOWED_FOR_STATE);
					}
				} else {
                        userProfile.setEmailAddress(null);
                        xXPortalUser = this.createUser(userProfile,
                                RangerCommonEnums.STATUS_ENABLED);
				}
			} else { //NOPMD
				/*
				 * throw restErrorUtil .createRESTException( "The login id " +
				 * loginId +
				 * " you've provided already exists. Please try again with different "
				 * + "login id.", MessageEnums.OPER_NOT_ALLOWED_FOR_STATE);
				 */
			}
        }

        VXPortalUser userProfileRes = null;
        if (xXPortalUser != null) {
            userProfileRes = mapXXPortalUserToVXPortalUserForDefaultAccount(xXPortalUser);
            if (userProfile.getUserRoleList() != null
                    && userProfile.getUserRoleList().size() > 0
                    && ((List<String>) userProfile.getUserRoleList()).get(0) != null) {
                reqRoleList = userProfile.getUserRoleList();
                existingRoleList = this.getRolesByLoginId(loginId);
                XXPortalUser xxPortalUser = daoManager.getXXPortalUser()
                        .findByLoginId(userProfile.getLoginId());
                if (xxPortalUser != null
                        && xxPortalUser.getUserSource() == RangerCommonEnums.USER_EXTERNAL) {
                    userProfileRes = updateRoleForExternalUsers(reqRoleList,
                            existingRoleList, userProfileRes);
                }
            }
        }
        return userProfileRes;
        }

    protected VXPortalUser updateRoleForExternalUsers(
            Collection<String> reqRoleList,
            Collection<String> existingRoleList, VXPortalUser userProfileRes) {
        UserSessionBase session = ContextUtil.getCurrentUserSession();
        if (session != null && session.getXXPortalUser() != null && session.getXXPortalUser().getLoginId() != null &&  "rangerusersync".equals(session.getXXPortalUser().getLoginId())
                && reqRoleList != null && !reqRoleList.isEmpty()
                && existingRoleList != null && !existingRoleList.isEmpty()) {
            if (!reqRoleList.equals(existingRoleList)) {
                userProfileRes.setUserRoleList(reqRoleList);
                userProfileRes.setUserSource(RangerCommonEnums.USER_EXTERNAL);
                List<XXUserPermission> xuserPermissionList = daoManager
                        .getXXUserPermission().findByUserPermissionId(
                                userProfileRes.getId());
                if (xuserPermissionList != null
                        && xuserPermissionList.size() > 0) {
                    for (XXUserPermission xXUserPermission : xuserPermissionList) {
                        if (xXUserPermission != null) {
                            try {
                                xUserPermissionService
                                        .deleteResource(xXUserPermission
                                                .getId());
                            } catch (Exception e) {
                                logger.error(e.getMessage());
                            }
                        }

                    }
                }
                updateUser(userProfileRes);
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Permission"
                        + " denied. LoggedInUser="
                        + (session != null ? session.getXXPortalUser().getId()
                                : "")
                        + " isn't permitted to perform the action.");
            }
        }
        return userProfileRes;
    }

        protected VXPortalUser mapXXPortalUserToVXPortalUserForDefaultAccount(
                        XXPortalUser user) {

		VXPortalUser userProfile = new VXPortalUser();

		userProfile.setLoginId(user.getLoginId());
		userProfile.setEmailAddress(user.getEmailAddress());
		userProfile.setStatus(user.getStatus());
		userProfile.setUserRoleList(new ArrayList<String>());
		userProfile.setId(user.getId());
		userProfile.setFirstName(user.getFirstName());
		userProfile.setLastName(user.getLastName());
		userProfile.setPublicScreenName(user.getPublicScreenName());
		userProfile.setOtherAttributes(user.getOtherAttributes());
		userProfile.setSyncSource(user.getSyncSource());
		List<XXPortalUserRole> gjUserRoleList = daoManager
				.getXXPortalUserRole().findByParentId(user.getId());

		for (XXPortalUserRole gjUserRole : gjUserRoleList) {
			userProfile.getUserRoleList().add(gjUserRole.getUserRole());
		}
		
		return userProfile;
	}

	public boolean isUserInRole(Long userId, String role) {
		XXPortalUserRole xXPortalUserRole = daoManager.getXXPortalUserRole()
				.findByRoleUserId(userId, role);
		if (xXPortalUserRole != null) {
			String userRole = xXPortalUserRole.getUserRole();
			if (userRole.equalsIgnoreCase(role)) {
				return true;
			}
		}
		return false;
	}

	public XXPortalUser updateUserWithPass(VXPortalUser userProfile) {
		String updatedPassword = userProfile.getPassword();
        XXPortalUser xXPortalUser = this.updateUser(userProfile);

		if (xXPortalUser == null) {
			return null;
		}

		if (updatedPassword != null && !updatedPassword.isEmpty()) {
			if (!stringUtil.validatePassword(updatedPassword, new String[] {
					xXPortalUser.getFirstName(), xXPortalUser.getLastName(),
					xXPortalUser.getLoginId() })) {
				logger.warn("SECURITY:changePassword(). Invalid new password. userId="
						+ xXPortalUser.getId());

				throw restErrorUtil.createRESTException(
						"serverMsg.userMgrNewPassword",
						MessageEnums.INVALID_PASSWORD, null, null, ""
								+ xXPortalUser.getId());
			}

			String encryptedNewPwd = encrypt(xXPortalUser.getLoginId(),
					updatedPassword);
            if (xXPortalUser.getUserSource() != RangerCommonEnums.USER_EXTERNAL) {
				String oldPasswordsStr = xXPortalUser.getOldPasswords();
				List<String> oldPasswords;
				if (StringUtils.isNotEmpty(oldPasswordsStr)) {
					oldPasswords = new ArrayList<>(Arrays.asList(oldPasswordsStr.split(",")));
				} else {
					oldPasswords = new ArrayList<>();
				}
				oldPasswords.add(encryptedNewPwd);
				updateOldPasswords(xXPortalUser, oldPasswords);
		xXPortalUser.setPassword(encryptedNewPwd);
             }
             xXPortalUser = daoManager.getXXPortalUser().update(xXPortalUser);
		}
		return xXPortalUser;
	}
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
        public XXPortalUser updatePasswordInSHA256(String userName,String userPassword,boolean logAudits) {
		if (userName == null || userPassword == null
				|| userName.trim().isEmpty() || userPassword.trim().isEmpty()){
				return null;
		}

		XXPortalUser xXPortalUser = this.findByLoginId(userName);

		if (xXPortalUser == null) {
			return null;
		}
                String dbOldPwd =xXPortalUser.getPassword();
		String encryptedNewPwd = encrypt(xXPortalUser.getLoginId(),userPassword);
       if (xXPortalUser.getUserSource() != RangerCommonEnums.USER_EXTERNAL) {
                xXPortalUser.setPassword(encryptedNewPwd);
       }

		xXPortalUser = daoManager.getXXPortalUser().update(xXPortalUser);
                if(xXPortalUser!=null && logAudits){
                        String dbNewPwd=xXPortalUser.getPassword();
                        if (!dbOldPwd.equals(dbNewPwd)) {
                                List<XXTrxLog> trxLogList = new ArrayList<XXTrxLog>();
                                XXTrxLog xTrxLog = new XXTrxLog();
                                xTrxLog.setAttributeName("Password");
                                xTrxLog.setPreviousValue(dbOldPwd);
                                xTrxLog.setNewValue(dbNewPwd);
                                xTrxLog.setAction("password change");
                                xTrxLog.setObjectClassType(AppConstants.CLASS_TYPE_PASSWORD_CHANGE);
                                xTrxLog.setObjectId(xXPortalUser.getId());
                                xTrxLog.setObjectName(xXPortalUser.getLoginId());
                                xTrxLog.setAddedByUserId(xXPortalUser.getId());
                                xTrxLog.setUpdatedByUserId(xXPortalUser.getId());
                                trxLogList.add(xTrxLog);
                                rangerBizUtil.createTrxLog(trxLogList);
                        }
                }

		return xXPortalUser;
	}

	public void checkAdminAccess() {
		UserSessionBase sess = ContextUtil.getCurrentUserSession();
		if (sess != null && sess.isUserAdmin()) {
			return;
		}
		throw restErrorUtil.create403RESTException("Operation not allowed." + " loggedInUser=" + (sess != null ? sess.getXXPortalUser().getId() : ". Not Logged In."));
	}

	public Collection<String> getRolesByLoginId(String loginId) {
		if (loginId == null || loginId.trim().isEmpty()){
			return DEFAULT_ROLE_LIST;
		}
		XXPortalUser xXPortalUser=daoManager.getXXPortalUser().findByLoginId(loginId);
		if(xXPortalUser==null){
			return DEFAULT_ROLE_LIST;
        }
		Collection<XXPortalUserRole> xXPortalUserRoles = daoManager
                        .getXXPortalUserRole().findByUserId(xXPortalUser.getId());
		if(xXPortalUserRoles==null){
			return DEFAULT_ROLE_LIST;
		}
		Collection<String> roleList = new ArrayList<String>();
		for (XXPortalUserRole role : xXPortalUserRoles) {
			if(role!=null && VALID_ROLE_LIST.contains(role.getUserRole())){
				if(!roleList.contains(role.getUserRole())){
					roleList.add(role.getUserRole());
				}
			}
        }
		if(roleList==null || roleList.size()==0){
			return DEFAULT_ROLE_LIST;
		}
		return roleList;
	}

        @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
        public XXPortalUser updateOldUserName(String userLoginId,String newUserName, String currentPassword) {
                if (userLoginId == null || newUserName == null
                                || userLoginId.trim().isEmpty() || newUserName.trim().isEmpty()){
                        return null;
                }

                XXPortalUser xXPortalUser = this.findByLoginId(userLoginId);
        XXUser xXUser = daoManager.getXXUser().findByUserName(userLoginId);
                if (xXPortalUser == null || xXUser == null) {
                        return null;
                }
                xXUser.setName(newUserName);
                daoManager.getXXUser().update(xXUser);

                xXPortalUser.setLoginId(newUserName);
                // The old password needs to be encrypted by the new user name
                String updatedPwd = encrypt(newUserName,currentPassword);
                if (xXPortalUser.getUserSource() == RangerCommonEnums.USER_APP) {
                        xXPortalUser.setPassword(updatedPwd);
                }
                else  if (xXPortalUser.getUserSource() == RangerCommonEnums.USER_EXTERNAL) {
                    xXPortalUser.setPassword(xXPortalUser.getPassword());
                }
                xXPortalUser = daoManager.getXXPortalUser().update(xXPortalUser);
                List<XXTrxLog> trxLogList = new ArrayList<XXTrxLog>();
                XXTrxLog xTrxLog = new XXTrxLog();
                xTrxLog.setAttributeName("User Name");
                xTrxLog.setPreviousValue(userLoginId);
                xTrxLog.setNewValue(newUserName);
                xTrxLog.setAction("update");
                xTrxLog.setObjectClassType(AppConstants.CLASS_TYPE_USER_PROFILE);
                xTrxLog.setObjectId(xXPortalUser.getId());
                xTrxLog.setObjectName(xXPortalUser.getLoginId());
                xTrxLog.setAddedByUserId(xXPortalUser.getId());
                xTrxLog.setUpdatedByUserId(xXPortalUser.getId());
                trxLogList.add(xTrxLog);
                rangerBizUtil.createTrxLog(trxLogList);
                return xXPortalUser;
        }
        public boolean isPasswordValid(String loginId, String encodedPassword, String password) {
        			boolean isPasswordValid = false;
        			try {
        				Pbkdf2PasswordEncoderCust pbkdf2Encoder = new Pbkdf2PasswordEncoderCust(loginId);
        				pbkdf2Encoder.setEncodeHashAsBase64(true);
        				
        				if (pbkdf2Encoder.matches(password, encodedPassword)) {
        					isPasswordValid = true;
        				}
        			} catch (Throwable t) {
        				logger.error("Unable to validate old password ", t);
        			}
        	
        			return isPasswordValid;
        		}
        
        public boolean isNewPasswordDifferent(String loginId, String currentPassword, String newPassword) {
        			boolean isNewPasswordDifferent = true;
        			String saltEncodedpasswd = "";
        			try {
        				Pbkdf2PasswordEncoderCust pbkdf2Encoder = new Pbkdf2PasswordEncoderCust(loginId);
        				pbkdf2Encoder.setEncodeHashAsBase64(true);
        				if (currentPassword != null) {
        					saltEncodedpasswd = pbkdf2Encoder.encode(currentPassword);
        			}
        				if (pbkdf2Encoder.matches(newPassword, saltEncodedpasswd)) {
        					isNewPasswordDifferent = false;
        				}
        			} catch (Throwable t) {
        				logger.error("Unable to validate old and new passwords ", t);
        			}
        	
        			return isNewPasswordDifferent;
        	}

	private String mergeTextAndSalt(String text, Object salt, boolean strict) {
		if (text == null) {
			text = "";
		}

		if ((strict) && (salt != null) && ((salt.toString().lastIndexOf("{") != -1) || (salt.toString().lastIndexOf("}") != -1))) {
			throw new IllegalArgumentException("Cannot use { or } in salt.toString()");
		}

		if ((salt == null) || ("".equals(salt))) {
			return text;
		}
		return text + "{" + salt.toString() + "}";
	}

	private String encodeString(String text, String salt, String algorithm) {
		String mergedString = mergeTextAndSalt(text, salt, false);
		try {
			MessageDigest digest = MessageDigest.getInstance(algorithm);
			return new String(Hex.encode(digest.digest(mergedString.getBytes("UTF-8"))));
		} catch (UnsupportedEncodingException e) {
			throw restErrorUtil.createRESTException("UTF-8 not supported");
		} catch (NoSuchAlgorithmException e) {
			throw restErrorUtil.createRESTException("algorithm `" + algorithm + "' not supported");
		}
	}
}
