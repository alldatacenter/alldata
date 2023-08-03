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

 package org.apache.ranger.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.ranger.common.ContextUtil;
import org.apache.ranger.common.MessageEnums;
import org.apache.ranger.common.RangerConfigUtil;
import org.apache.ranger.common.RangerConstants;
import org.apache.ranger.common.StringUtil;
import org.apache.ranger.common.UserSessionBase;
import org.apache.ranger.entity.XXPortalUser;
import org.apache.ranger.entity.XXPortalUserRole;
import org.apache.ranger.view.VXMessage;
import org.apache.ranger.view.VXPortalUser;
import org.apache.ranger.view.VXResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope("singleton")
public class UserService extends UserServiceBase<XXPortalUser, VXPortalUser> {
	private static final Logger logger = LoggerFactory.getLogger(UserService.class);

	public static final String NAME = "User";

	@Autowired
	RangerConfigUtil configUtil;

	@Autowired
	XUserPermissionService xUserPermissionService;

	private static UserService instance = null;

	public UserService() {
		super();
		instance = this;
	}

	public static UserService getInstance() {
		if (instance == null) {
			logger.error("Instance is null", new Throwable());
		}
		return instance;
	}

	@Override
	protected void validateForCreate(VXPortalUser userProfile) {
		List<VXMessage> messageList = new ArrayList<VXMessage>();
		if (stringUtil.isEmpty(userProfile.getEmailAddress())) {
			logger.info("Empty Email Address." + userProfile);
			messageList.add(MessageEnums.NO_INPUT_DATA.getMessage(null,
					"emailAddress"));
		}

		if (stringUtil.isEmpty(userProfile.getFirstName())) {
			logger.info("Empty firstName." + userProfile);
			messageList.add(MessageEnums.NO_INPUT_DATA.getMessage(null,
					"firstName"));
		}
		if (stringUtil.isEmpty(userProfile.getLastName())) {
			logger.info("Empty lastName." + userProfile);
			messageList.add(MessageEnums.NO_INPUT_DATA.getMessage(null,
					"lastName"));
		}
		// firstName
		if (!stringUtil.isValidName(userProfile.getFirstName())) {
			logger.info("Invalid first name." + userProfile);
			messageList.add(MessageEnums.INVALID_INPUT_DATA.getMessage(null,
					"firstName"));
		}
		userProfile.setFirstName(stringUtil.toCamelCaseAllWords(userProfile
				.getFirstName()));

		// lastName
		if (!stringUtil.isValidName(userProfile.getLastName())) {
			logger.info("Invalid last name." + userProfile);
			messageList.add(MessageEnums.INVALID_INPUT_DATA.getMessage(null,
					"lastName"));
		}
		userProfile.setLastName(stringUtil.toCamelCaseAllWords(userProfile
				.getLastName()));

		if (!stringUtil.validateEmail(userProfile.getEmailAddress())) {
			logger.info("Invalid email address." + userProfile);
			messageList.add(MessageEnums.INVALID_INPUT_DATA.getMessage(null,
					"emailAddress"));

		}

		// Normalize email. Make it lower case
		userProfile.setEmailAddress(stringUtil.normalizeEmail(userProfile
				.getEmailAddress()));

		// loginId
		userProfile.setLoginId(userProfile.getEmailAddress());

		// password
		if (!stringUtil.validatePassword(
				userProfile.getPassword(),
				new String[] { userProfile.getFirstName(),
						userProfile.getLastName() })) {
			logger.info("Invalid password." + userProfile);
			messageList.add(MessageEnums.INVALID_INPUT_DATA.getMessage(null,
					"password"));
		}

		// firstName
		if (!stringUtil.validateString(StringUtil.VALIDATION_NAME,
				userProfile.getFirstName())) {
			logger.info("Invalid first name." + userProfile);
			messageList.add(MessageEnums.INVALID_INPUT_DATA.getMessage(null,
					"firstName"));
		}

		// lastName
		if (!stringUtil.validateString(StringUtil.VALIDATION_NAME,
				userProfile.getLastName())) {
			logger.info("Invalid last name." + userProfile);
			messageList.add(MessageEnums.INVALID_INPUT_DATA.getMessage(null,
					"lastName"));
		}

		// create the public screen name
		userProfile.setPublicScreenName(userProfile.getFirstName() + " "
				+ userProfile.getLastName());

		if (!messageList.isEmpty()) {
			VXResponse gjResponse = new VXResponse();
			gjResponse.setStatusCode(VXResponse.STATUS_ERROR);
			gjResponse.setMsgDesc("Validation failure");
			gjResponse.setMessageList(messageList);
			logger.info("Validation Error in createUser() userProfile="
					+ userProfile + ", error=" + gjResponse);
			throw restErrorUtil.createRESTException(gjResponse);
		}
	}

	@Override
	protected void validateForUpdate(VXPortalUser userProfile, XXPortalUser xXPortalUser) {
		List<VXMessage> messageList = new ArrayList<VXMessage>();

		if (userProfile.getEmailAddress() != null
				&& !userProfile.getEmailAddress().equalsIgnoreCase(
						xXPortalUser.getEmailAddress())) {
			throw restErrorUtil.createRESTException("serverMsg.userEmail",
					MessageEnums.DATA_NOT_UPDATABLE, null, "emailAddress",
					userProfile.getEmailAddress());
		}

		// Login Id can't be changed
		if (userProfile.getLoginId() != null
				&& !xXPortalUser.getLoginId().equalsIgnoreCase(
						userProfile.getLoginId())) {
			throw restErrorUtil.createRESTException("serverMsg.userUserName",
					MessageEnums.DATA_NOT_UPDATABLE, null, "loginId",
					userProfile.getLoginId());
		}
		// }

		userProfile.setFirstName(restErrorUtil.validateStringForUpdate(
				userProfile.getFirstName(), xXPortalUser.getFirstName(),
				StringUtil.VALIDATION_NAME, "serverMsg.userFirstName",
				MessageEnums.INVALID_INPUT_DATA, null, "firstName"));

		userProfile.setFirstName(restErrorUtil.validateStringForUpdate(
				userProfile.getFirstName(), xXPortalUser.getFirstName(),
				StringUtil.VALIDATION_NAME, "serverMsg.userFirstName",
				MessageEnums.INVALID_INPUT_DATA, null, "firstName"));

		userProfile.setLastName(restErrorUtil.validateStringForUpdate(
				userProfile.getLastName(), xXPortalUser.getLastName(),
				StringUtil.VALIDATION_NAME, "serverMsg.userLastName",
				MessageEnums.INVALID_INPUT_DATA, null, "lastName"));

		// firstName
		if (!stringUtil.isValidName(userProfile.getFirstName())) {
			logger.info("Invalid first name." + userProfile);
			messageList.add(MessageEnums.INVALID_INPUT_DATA.getMessage(null,
					"firstName"));
		}

		// lastName
		if (!stringUtil.isValidName(userProfile.getLastName())) {
			logger.info("Invalid last name." + userProfile);
			messageList.add(MessageEnums.INVALID_INPUT_DATA.getMessage(null,
					"lastName"));
		}

		userProfile.setNotes(restErrorUtil.validateStringForUpdate(
				userProfile.getNotes(), xXPortalUser.getNotes(),
				StringUtil.VALIDATION_NAME, "serverMsg.userNotes",
				MessageEnums.INVALID_INPUT_DATA, null, "notes"));

		// validate status
		restErrorUtil.validateMinMax(userProfile.getStatus(), 0,
				RangerConstants.ActivationStatus_MAX, "Invalid status", null,
				"status");

		// validate user roles
		if (userProfile.getUserRoleList() != null) {
			// First let's normalize it
			splitUserRoleList(userProfile.getUserRoleList());
			for (String userRole : userProfile.getUserRoleList()) {
				restErrorUtil.validateStringList(userRole,
						configUtil.getRoles(), "serverMsg.userRole", null,
						"userRoleList");
			}

		}

		// TODO: Need to see whether user can set user as internal

		if (!messageList.isEmpty()) {
			VXResponse gjResponse = new VXResponse();
			gjResponse.setStatusCode(VXResponse.STATUS_ERROR);
			gjResponse.setMsgDesc("Validation failure");
			gjResponse.setMessageList(messageList);
			logger.info("Validation Error in updateUser() userProfile="
					+ userProfile + ", error=" + gjResponse);
			throw restErrorUtil.createRESTException(gjResponse);
		}
	}

	void splitUserRoleList(Collection<String> collection) {
		Collection<String> newCollection = new ArrayList<String>();
		for (String role : collection) {
			String roles[] = role.split(",");
            newCollection.addAll(Arrays.asList(roles));
		}
		collection.clear();
		collection.addAll(newCollection);
	}

	@Override
	protected XXPortalUser mapViewToEntityBean(VXPortalUser userProfile, XXPortalUser mObj,
			int OPERATION_CONTEXT) {
		mObj.setEmailAddress(userProfile.getEmailAddress());
		mObj.setFirstName(userProfile.getFirstName());
		mObj.setLastName(userProfile.getLastName());
		mObj.setLoginId(userProfile.getLoginId());
		mObj.setPassword(userProfile.getPassword());
		mObj.setPublicScreenName(bizUtil.generatePublicName(userProfile, null));
		mObj.setUserSource(userProfile.getUserSource());
		return mObj;

	}

	@Override
	protected VXPortalUser mapEntityToViewBean(VXPortalUser userProfile,
			XXPortalUser user) {
		userProfile.setId(user.getId());
		userProfile.setLoginId(user.getLoginId());
		userProfile.setFirstName(user.getFirstName());
		userProfile.setLastName(user.getLastName());
		userProfile.setPublicScreenName(user.getPublicScreenName());
		userProfile.setStatus(user.getStatus());
		userProfile.setUserRoleList(new ArrayList<String>());
		String emailAddress = user.getEmailAddress();
		if (emailAddress != null && stringUtil.validateEmail(emailAddress)) {
			userProfile.setEmailAddress(user.getEmailAddress());
		}

		UserSessionBase sess = ContextUtil.getCurrentUserSession();
		if (sess != null) {
			userProfile.setUserSource(sess.getAuthProvider());
		}

		List<XXPortalUserRole> gjUserRoleList = daoManager.getXXPortalUserRole().findByParentId(
				user.getId());

		for (XXPortalUserRole gjUserRole : gjUserRoleList) {
			userProfile.getUserRoleList().add(gjUserRole.getUserRole());
		}
		return userProfile;
	}

	// TODO: Need to remove this ASAP
	public void gjUserToUserProfile(XXPortalUser user, VXPortalUser userProfile) {
		userProfile.setId(user.getId());
		userProfile.setLoginId(user.getLoginId());
		userProfile.setFirstName(user.getFirstName());
		userProfile.setLastName(user.getLastName());
		userProfile.setPublicScreenName(user.getPublicScreenName());
		userProfile.setStatus(user.getStatus());
		userProfile.setUserRoleList(new ArrayList<String>());
		UserSessionBase sess = ContextUtil.getCurrentUserSession();

		String emailAddress = user.getEmailAddress();
		if (emailAddress != null && stringUtil.validateEmail(emailAddress)) {
			userProfile.setEmailAddress(user.getEmailAddress());
		}

		if (sess != null) {
			userProfile.setUserSource(sess.getAuthProvider());
		}

		List<XXPortalUserRole> gjUserRoleList = daoManager.getXXPortalUserRole().findByParentId(
				user.getId());

		for (XXPortalUserRole gjUserRole : gjUserRoleList) {
			userProfile.getUserRoleList().add(gjUserRole.getUserRole());
		}
	}

}
