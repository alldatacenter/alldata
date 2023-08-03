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

 
// Manages the user session
define(function(require){
	'use strict';

	var VXPortalUser		= require('models/VXPortalUser');

	// Private properties
	var vXPortalUser = null;
	var vSessionSettings = null;

	// Public methods
	var SessionMgr = {};

	/**
	 * Gets the user profile for the given session
	 * 
	 * @returns VXPortalUser
	 */

	SessionMgr.getUserProfile = function() {
		if ( vXPortalUser){
			return vXPortalUser;
		}

		vXPortalUser = new VXPortalUser();
		vXPortalUser.getUserProfile({async : false,cache:false}).done(function(data){
			vXPortalUser.set(data);
		});
		return vXPortalUser;
	};

	SessionMgr.updateUserProfile = function() {
		if (vXPortalUser){
			vXPortalUser.getUserProfile({async : false,cache:false}).done(function(data){
				vXPortalUser.set(data);
			});
		}
	};

	SessionMgr.getLoginId = function() {
		if (vXPortalUser) {
			return vXPortalUser.get('loginId');
		}
	};

	SessionMgr.userInRole = function(role) {
		var vXPortalUser = SessionMgr.getUserProfile();
		var userRoles = vXPortalUser.get('userRoleList');
		if (!userRoles || !role) {
			return false;
		}
		if (userRoles.constructor != Array) {
			userRoles = [ userRoles ];
		}

		return (userRoles.indexOf(role) > -1);
	};

	SessionMgr.getUserRoles = function() {
		var vXPortalUser = SessionMgr.getUserProfile();
		var userRoles = vXPortalUser.get('userRoleList');
		if (!userRoles) {
			return [];
		}
		if (userRoles.constructor != Array) {
			userRoles = [ userRoles ];
		}

		return userRoles;
	};

	SessionMgr.getSetting = function(key) {
		if (!vSessionSettings) {
			var msResponse = GeneralMgr.getSessionSettings();
			if (msResponse.isSuccess()) {
				vSessionSettings = msResponse.response;
			}
		}
		var value = null;
		if (vSessionSettings && key) {
			vSessionSettings.each(function(vNameValue) {
				if (vNameValue.get('name') == key) {
					value = vNameValue.get('value');
				}
			});
		}
		return value;
	};

	SessionMgr.resetSession = function() {
		vXPortalUser = null;
		vSessionSettings = null;
		MSCacheMgr.resetAll();
	};

	/**
	 * Logs out the user and resets all session variables
	 */
	SessionMgr.logout = function(reDirectUser) {
		SessionMgr.resetSession();
		MSCacheMgr.resetAll();
		if (reDirectUser) {
			// This will ask the browser to redirect
			window.location.replace("logout");
		} else {
			// We will do an implicit logout
			$.ajax({
				url : 'logout',
				type : 'GET',
				async : false
			});
		}
	};

	SessionMgr.isSystemAdmin = function(){
		return this.userInRole('ROLE_SYS_ADMIN') ? true : false;
	};
	SessionMgr.isKeyAdmin = function(){
		return this.userInRole('ROLE_KEY_ADMIN') ? true : false;
	};
	SessionMgr.isUser = function(){
		return this.userInRole('ROLE_USER') ? true : false;
	};
    SessionMgr.isAuditor = function(){
        return this.userInRole('ROLE_ADMIN_AUDITOR') ? true : false;
    };
    SessionMgr.isKMSAuditor = function(){
        return this.userInRole('ROLE_KEY_ADMIN_AUDITOR') ? true : false;
    };
	return SessionMgr;
});	
