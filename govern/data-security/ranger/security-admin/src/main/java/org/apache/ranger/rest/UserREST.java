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

 package org.apache.ranger.rest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import org.apache.ranger.biz.UserMgr;
import org.apache.ranger.biz.XUserMgr;
import org.apache.ranger.common.MessageEnums;
import org.apache.ranger.common.PropertiesUtil;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.common.RangerConfigUtil;
import org.apache.ranger.common.RangerConstants;
import org.apache.ranger.common.SearchCriteria;
import org.apache.ranger.common.SearchUtil;
import org.apache.ranger.common.SortField;
import org.apache.ranger.common.StringUtil;
import org.apache.ranger.common.annotation.RangerAnnotationClassName;
import org.apache.ranger.common.annotation.RangerAnnotationJSMgrName;
import org.apache.ranger.common.annotation.RangerAnnotationRestAPI;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.entity.XXPortalUser;
import org.apache.ranger.security.context.RangerAPIList;
import org.apache.ranger.util.RangerRestUtil;
import org.apache.ranger.view.VXPasswordChange;
import org.apache.ranger.view.VXPortalUser;
import org.apache.ranger.view.VXPortalUserList;
import org.apache.ranger.view.VXResponse;
import org.apache.ranger.view.VXStringList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


@Path("users")
@Component
@Scope("request")
@RangerAnnotationJSMgrName("UserMgr")
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class UserREST {
	private static final Logger logger = LoggerFactory.getLogger(UserREST.class);

	@Autowired
	StringUtil stringUtil;

	@Autowired
	RangerDaoManager daoManager;

	@Autowired
	RangerConfigUtil configUtil;

	@Autowired
	RESTErrorUtil restErrorUtil;

	@Autowired
	SearchUtil searchUtil;

	@Autowired
	UserMgr userManager;

	@Autowired
	RangerRestUtil msRestUtil;
	
	@Autowired
	XUserMgr xUserMgr;

	private final static List<SortField> SORT_FIELDS = Arrays.asList(
			new SortField("requestDate", "requestDate"),
			new SortField("approvedDate", "approvedDate"),
			new SortField("activationDate", "activationDate"),
			new SortField("emailAddress", "emailAddress"),
			new SortField("firstName", "firstName"),
			new SortField("lastName", "lastName")
		);
	/**
	 * Implements the traditional search functionalities for UserProfile
	 *
	 * @param request
	 * @return
	 */
	@GET
	@Produces({ "application/json" })
	@PreAuthorize("@rangerPreAuthSecurityHandler.isAPIAccessible(\"" + RangerAPIList.SEARCH_USERS + "\")")
	public VXPortalUserList searchUsers(@Context HttpServletRequest request) {
		SearchCriteria searchCriteria = searchUtil.extractCommonCriterias(
				request, SORT_FIELDS);

		// userId
		searchUtil.extractLong(request, searchCriteria, "userId", "User Id");

		// loginId
		searchUtil.extractString(request, searchCriteria, "loginId",
				"Login Id", null);

		// emailAddress
		searchUtil.extractString(request, searchCriteria, "emailAddress",
				"Email Address", null);

		// firstName
		searchUtil.extractString(request, searchCriteria, "firstName",
				"First Name", StringUtil.VALIDATION_NAME);

		// lastName
		searchUtil.extractString(request, searchCriteria, "lastName",
				"Last Name", StringUtil.VALIDATION_NAME);

		// status
		searchUtil.extractEnum(request, searchCriteria, "status", "Status",
				"statusList", RangerConstants.ActivationStatus_MAX);

		// publicScreenName
		searchUtil.extractString(request, searchCriteria, "publicScreenName",
				"Public Screen Name", StringUtil.VALIDATION_NAME);
		// roles
		searchUtil.extractStringList(request, searchCriteria, "role", "Role",
				"roleList", configUtil.getRoles(), StringUtil.VALIDATION_NAME);

		return userManager.searchUsers(searchCriteria);
	}

	/**
	 * Return the VUserProfile for the given userId
	 *
	 * @param userId
	 * @return
	 */
	@GET
	@Path("{userId}")
	@Produces({ "application/json" })
	@PreAuthorize("@rangerPreAuthSecurityHandler.isAPIAccessible(\"" + RangerAPIList.GET_USER_PROFILE_FOR_USER + "\")")
	public VXPortalUser getUserProfileForUser(@PathParam("userId") Long userId) {
		try {
			VXPortalUser userProfile = userManager.getUserProfile(userId);
			if (userProfile != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("getUserProfile() Found User userId=" + userId);
				}
			} else {
				logger.debug("getUserProfile() Not found userId=" + userId);
			}
			return userProfile;
		} catch (Throwable t) {
			logger.error("getUserProfile() no user session. error="
					+ t.toString());
		}
		return null;
	}

	@POST
	@Consumes({ "application/json" })
	@Produces({ "application/json" })
	@PreAuthorize("@rangerPreAuthSecurityHandler.isAPIAccessible(\"" + RangerAPIList.CREATE + "\")")
	public VXPortalUser create(VXPortalUser userProfile,
			@Context HttpServletRequest servletRequest) {
		logger.info("create:" + userProfile.getEmailAddress());

		return userManager.createUser(userProfile);
	}
	
	// API to add user with default account
	@POST
	@Path("/default")
	@Consumes({ "application/json" })
	@Produces({ "application/json" })
	@PreAuthorize("@rangerPreAuthSecurityHandler.isAPIAccessible(\"" + RangerAPIList.CREATE_DEFAULT_ACCOUNT_USER + "\")")
	public VXPortalUser createDefaultAccountUser(VXPortalUser userProfile,
			@Context HttpServletRequest servletRequest) {
		VXPortalUser vxPortalUser;
		vxPortalUser=userManager.createDefaultAccountUser(userProfile);
		if(vxPortalUser!=null)
		{
			xUserMgr.assignPermissionToUser(vxPortalUser, true);
		}
		 return vxPortalUser;
	}


	@PUT
	@Consumes({ "application/json" })
	@Produces({ "application/json" })
	@RangerAnnotationRestAPI(updates_classes = "VUserProfile")
	@PreAuthorize("@rangerPreAuthSecurityHandler.isAPIAccessible(\"" + RangerAPIList.UPDATE + "\")")
	public VXPortalUser update(VXPortalUser userProfile,
			@Context HttpServletRequest servletRequest) {
		logger.info("update:" + userProfile.getEmailAddress());
		XXPortalUser gjUser = daoManager.getXXPortalUser().getById(userProfile.getId());
		userManager.checkAccess(gjUser);
		if (gjUser != null) {
			msRestUtil.validateVUserProfileForUpdate(gjUser, userProfile);
			gjUser = userManager.updateUser(userProfile);
			return userManager.mapXXPortalUserVXPortalUser(gjUser);
		} else {
			logger.info("update(): Invalid userId provided: userId="
					+ userProfile.getId());
			throw restErrorUtil.createRESTException("serverMsg.userRestUser",
					MessageEnums.DATA_NOT_FOUND, null, null,
					userProfile.toString());
		}
	}

	@PUT
	@Path("/{userId}/roles")
	@Consumes({ "application/json" })
	@Produces({ "application/json" })
	@PreAuthorize("@rangerPreAuthSecurityHandler.isAPIAccessible(\"" + RangerAPIList.SET_USER_ROLES + "\")")
	public VXResponse setUserRoles(@PathParam("userId") Long userId,
			VXStringList roleList) {
		userManager.checkAccess(userId);
		userManager.setUserRoles(userId, roleList.getVXStrings());
		VXResponse response = new VXResponse();
		response.setStatusCode(VXResponse.STATUS_SUCCESS);
		return response;
	}

	/**
	 * Deactivate the user
	 *
	 * @param userId
	 * @return
	 */
	@POST
	@Path("{userId}/deactivate")
	@Consumes({ "application/json" })
	@Produces({ "application/json" })
	@PreAuthorize("@rangerPreAuthSecurityHandler.isAPIAccessible(\"" + RangerAPIList.DEACTIVATE_USER + "\")")
	@RangerAnnotationClassName(class_name = VXPortalUser.class)
	public VXPortalUser deactivateUser(@PathParam("userId") Long userId) {
		XXPortalUser gjUser = daoManager.getXXPortalUser().getById(userId);
		if (gjUser == null) {
			logger.info("update(): Invalid userId provided: userId=" + userId);
			throw restErrorUtil.createRESTException("serverMsg.userRestUser",
					MessageEnums.DATA_NOT_FOUND, null, null, "" + userId);
		}
		return userManager.deactivateUser(gjUser);
	}

	/**
	 * This method returns the VUserProfile for the current session
	 *
	 * @param request
	 * @return
	 */
	@GET
	@Path("/profile")
	@Produces({ "application/json" })
	public VXPortalUser getUserProfile(@Context HttpServletRequest request) {
		try {
			logger.debug("getUserProfile(). httpSessionId="
					+ request.getSession().getId());
			Map<String, String> configProperties  = new HashMap<>();
			Long inactivityTimeout = PropertiesUtil.getLongProperty("ranger.service.inactivity.timeout", 15*60);
			configProperties.put("inactivityTimeout", Long.toString(inactivityTimeout));
			VXPortalUser userProfile = userManager.getUserProfileByLoginId();
			userProfile.setConfigProperties(configProperties);
			return userProfile;
		} catch (Throwable t) {
			logger.error(
					"getUserProfile() no user session. error=" + t.toString(),
					t);
		}
		return null;
	}

	/**	
	 * @param userId
	 * @param changePassword
	 * @return
	 */
	@POST
	@Path("{userId}/passwordchange")
	@Consumes({ "application/json" })
	@Produces({ "application/json" })
	public VXResponse changePassword(@PathParam("userId") Long userId,
			VXPasswordChange changePassword) {
		if(changePassword==null || stringUtil.isEmpty(changePassword.getLoginId())) {
			logger.warn("SECURITY:changePassword(): Invalid loginId provided. loginId was empty or null");
			throw restErrorUtil.createRESTException("serverMsg.userRestUser", MessageEnums.DATA_NOT_FOUND, null, null, "");
		} else if (changePassword.getId() == null) {
			changePassword.setId(userId);
		} else if (!changePassword.getId().equals(userId) ) {
			logger.warn("SECURITY:changePassword(): userId mismatch");
			throw restErrorUtil.createRESTException("serverMsg.userRestUser",MessageEnums.DATA_NOT_FOUND, null, null,"");
		}

		XXPortalUser gjUser = daoManager.getXXPortalUser().findByLoginId(changePassword.getLoginId());
		if (gjUser == null) {
			logger.warn("SECURITY:changePassword(): Invalid loginId provided: loginId="+ changePassword.getLoginId());
			throw restErrorUtil.createRESTException("serverMsg.userRestUser",MessageEnums.DATA_NOT_FOUND, null, null, changePassword.getLoginId());
		}

		userManager.checkAccessForUpdate(gjUser);
		changePassword.setId(gjUser.getId());
 		VXResponse ret = userManager.changePassword(changePassword);
		return ret;
	}

	/**	
	 *
	 * @param userId
	 * @param changeEmail
	 * @return
	 */
	@POST
	@Path("{userId}/emailchange")
	@Consumes({ "application/json" })
	@Produces({ "application/json" })
	public VXPortalUser changeEmailAddress(@PathParam("userId") Long userId,
			VXPasswordChange changeEmail) {
		if(changeEmail==null || stringUtil.isEmpty(changeEmail.getLoginId())) {
			logger.warn("SECURITY:changeEmail(): Invalid loginId provided. loginId was empty or null");
			throw restErrorUtil.createRESTException("serverMsg.userRestUser", MessageEnums.DATA_NOT_FOUND, null, null, "");
		} else if (changeEmail.getId() == null) {
			changeEmail.setId(userId);
		} else if (!changeEmail.getId().equals(userId) ) {
			logger.warn("SECURITY:changeEmail(): userId mismatch");
			throw restErrorUtil.createRESTException("serverMsg.userRestUser",MessageEnums.DATA_NOT_FOUND, null, null,"");
		}

		logger.info("changeEmail:" + changeEmail.getLoginId());
		XXPortalUser gjUser = daoManager.getXXPortalUser().findByLoginId(changeEmail.getLoginId());
		if (gjUser == null) {
			logger.warn("SECURITY:changeEmail(): Invalid loginId provided: loginId="+ changeEmail.getLoginId());
			throw restErrorUtil.createRESTException("serverMsg.userRestUser",MessageEnums.DATA_NOT_FOUND, null, null, changeEmail.getLoginId());
		}

		userManager.checkAccessForUpdate(gjUser);
		changeEmail.setId(gjUser.getId());
		VXPortalUser ret = userManager.changeEmailAddress(gjUser, changeEmail);
		return ret;
	}

}
