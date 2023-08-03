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

 
define(function(require){
	'use strict';	

	var XABaseModel	= require('models/XABaseModel');
	var XAGlobals	= require('utils/XAGlobals');

	var VXPortalUserBase = XABaseModel.extend(
	/** @lends VXPortalUserBase.prototype */
	{
		urlRoot: XAGlobals.baseURL + 'users',
		
		defaults: {},

		serverSchema : {
			"id" : {
				"dataType" : "Long"
			},
			"createDate" : {
				"dataType" : "Date"
			},
			"updateDate" : {
				"dataType" : "Date"
			},
			"permList" : {
				"dataType" : "list",
				"listType" : "VNameValue"
			},
			"password" : {
				"dataType" : "String"
			},
			"emailAddress" : {
				"dataType" : "String"
			},
			"firstName" : {
				"dataType" : "String"
			},
			"lastName" : {
				"dataType" : "String"
			},
			"publicScreenName" : {
				"dataType" : "String"
			},
			"userSource" : {
				"dataType" : "int"
			},
			"userRoleList" : {
				"dataType" : "list",
				"listType" : "String"
			}
		},
		
		
		idAttribute: 'id',

		/**
		 * VXPortalUserBase initialize method
		 * @augments XABaseModel
		 * @constructs
		 */
		initialize: function() {
			this.modelName = 'VXPortalUserBase';
			this.bindErrorEvents();
		},

		/*************************
		 * Non - CRUD operations
		 *************************/

		getUserProfile: function(options) {
			return this.constructor.nonCrudOperation.call(this, this.urlRoot + '/profile', 'GET', options);
		},

		setUserRoles : function(userId, postData , options){
			var url = this.urlRoot  + '/' + userId + '/roles';
			
			options = _.extend({
				data : JSON.stringify(postData),
				contentType : 'application/json',
				dataType : 'json'
			}, options);

			return this.constructor.nonCrudOperation.call(this, url, 'PUT', options);
		},
		changePassword : function(userId, postData , options){
			var url = this.urlRoot  + '/' + userId + '/passwordchange';
			
			options = _.extend({
				data : JSON.stringify(postData),
				contentType : 'application/json',
				dataType : 'json'
			}, options);

			return this.constructor.nonCrudOperation.call(this, url, 'POST', options);
		},
		saveUserProfile : function(vXPortalUser, options){
			var url = this.urlRoot ;
			
			options = _.extend({
				data : JSON.stringify(vXPortalUser),
				contentType : 'application/json',
				dataType : 'json'
			}, options);

			return this.constructor.nonCrudOperation.call(this, url, 'PUT', options);
		}
		
	}, {
		// static class members
	});

    return VXPortalUserBase;
	
});


