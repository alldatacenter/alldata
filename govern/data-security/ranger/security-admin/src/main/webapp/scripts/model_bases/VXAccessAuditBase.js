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

	var VXGroupBase = XABaseModel.extend(
	/** @lends VXGroupBase.prototype */
	{
		urlRoot: XAGlobals.baseURL + 'xusers/authSessions',
		
		defaults: {},

		serverSchema : {
			"id" : {
				"dataType" : "Long"
			},
			"version" : {
				"dataType" : "int"
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
			"forUserId" : {
				"dataType" : "Long"
			},
			"status" : {
				"dataType" : "int"
			},
			"priGrpId" : {
				"dataType" : "Long"
			},
			"updatedBy" : {
				"dataType" : "String"
			},
			"isSystem" : {
				"dataType" : "boolean"
			},
			"name" : {
				"dataType" : "String"
			},
			"description" : {
				"dataType" : "String"
			},
			"groupType" : {
				"dataType" : "int"
			},
			"credStoreId" : {
				"dataType" : "Long"
			}
		},
		
		
		idAttribute: 'id',

		/**
		 * VXGroupBase initialize method
		 * @augments XABaseModel
		 * @constructs
		 */
		initialize: function() {
			this.modelName = 'VXGroupBase';
		},
		getSession : function(extSessionId,options){
			var url = this.urlRoot  + '/' +'info?extSessionId='+extSessionId;
			
			options = _.extend({
			//	data : JSON.stringify(postData),
				contentType : 'application/json',
				dataType : 'json'
			}, options);

			return this.constructor.nonCrudOperation.call(this, url, 'GET', options);
		}

	}, {
		// static class members
	});

    return VXGroupBase;
	
});


