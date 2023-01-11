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

 package org.apache.ranger.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.ranger.common.MessageEnums;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.common.RangerConfigUtil;
import org.apache.ranger.common.StringUtil;
import org.apache.ranger.entity.XXPortalUser;
import org.apache.ranger.view.VXMessage;
import org.apache.ranger.view.VXPortalUser;
import org.apache.ranger.view.VXResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RangerRestUtil {
	private static final Logger logger = LoggerFactory.getLogger(RangerRestUtil.class);

	@Autowired
	StringUtil stringUtil;

	@Autowired
	RESTErrorUtil restErrorUtil;

	@Autowired
	RangerConfigUtil configUtil;

	void splitUserRoleList(Collection<String> collection) {
		Collection<String> newCollection = new ArrayList<String>();
		for (String role : collection) {
			String[] roles = role.split(",");
            newCollection.addAll(Arrays.asList(roles));
		}
		collection.clear();
		collection.addAll(newCollection);
	}

	/**
	 * This method cleans up the data provided by the user for update
	 *
	 * @param userProfile
	 * @return
	 */
	public void validateVUserProfileForUpdate(XXPortalUser gjUser,
			VXPortalUser userProfile) {

		List<VXMessage> messageList = new ArrayList<VXMessage>();

		// Email Update is allowed.
		// if (userProfile.getEmailAddress() != null
		// && !userProfile.getEmailAddress().equalsIgnoreCase(
		// gjUser.getEmailAddress())) {
		// throw restErrorUtil.createRESTException(
		// "Email address can't be updated",
		// MessageEnums.DATA_NOT_UPDATABLE, null, "emailAddress",
		// userProfile.getEmailAddress());
		// }

		// Login Id can't be changed
		if (userProfile.getLoginId() != null
				&& !gjUser.getLoginId().equalsIgnoreCase(
						userProfile.getLoginId())) {
			throw restErrorUtil.createRESTException(
					"Username can't be updated",
					MessageEnums.DATA_NOT_UPDATABLE, null, "loginId",
					userProfile.getLoginId());
		}
		// }
		userProfile.setFirstName(restErrorUtil.validateStringForUpdate(
				userProfile.getFirstName(), gjUser.getFirstName(),
				StringUtil.VALIDATION_NAME, "Invalid first name",
				MessageEnums.INVALID_INPUT_DATA, null, "firstName"));

		userProfile.setFirstName(restErrorUtil.validateStringForUpdate(
				userProfile.getFirstName(), gjUser.getFirstName(),
				StringUtil.VALIDATION_NAME, "Invalid first name",
				MessageEnums.INVALID_INPUT_DATA, null, "firstName"));

		
		// firstName
		if (!stringUtil.isValidName(userProfile.getFirstName())) {
			logger.info("Invalid first name." + userProfile);
			messageList.add(MessageEnums.INVALID_INPUT_DATA.getMessage(null,
					"firstName"));
		}

		
		// create the public screen name
		userProfile.setPublicScreenName(userProfile.getFirstName() + " "
				+ userProfile.getLastName());

		userProfile.setNotes(restErrorUtil.validateStringForUpdate(
				userProfile.getNotes(), gjUser.getNotes(),
				StringUtil.VALIDATION_NAME, "Invalid notes",
				MessageEnums.INVALID_INPUT_DATA, null, "notes"));

		// validate user roles
		if (userProfile.getUserRoleList() != null) {
			// First let's normalize it
			splitUserRoleList(userProfile.getUserRoleList());
			for (String userRole : userProfile.getUserRoleList()) {
				restErrorUtil.validateStringList(userRole,
						configUtil.getRoles(), "Invalid role", null,
						"userRoleList");
			}

		}
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

}
