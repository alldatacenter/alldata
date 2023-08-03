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


	var VXResourceBase = XABaseModel.extend(
	/** @lends VXResourceBase.prototype */
	{
		urlRoot: XAGlobals.baseURL + 'assets/resources',
		
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
			"resourceType" : {
				"dataType" : "int"
			},
			"assetId" : {
				"dataType" : "Long"
			},
			"parentId" : {
				"dataType" : "Long"
			},
			"parentPath" : {
				"dataType" : "String"
			},
			"isEncrypt" : {
				"dataType" : "int"
			},
			"permMapList" : {
				"dataType" : "list",
				"listType" : "VXPermMap"
			},
			"auditList" : {
				"dataType" : "list",
				"listType" : "VXAuditMap"
			}
		},
		
		idAttribute: 'id',

		/*modelRel: {
			permMapList: VXPermMapList,
		},*/
		//localStorage: new Backbone.LocalStorage("VXResourceModel"),

		/**
		 * VXResourceBase initialize method
		 * @augments XABaseModel
		 * @constructs
		 */
		initialize: function() {
			this.modelName = 'VXResourceBase';
			//this.permMapList = this.constructor.nestCollection(this, 'permMapList', new VXPermMapList(this.get('permMapList')));
			//this.auditList = this.constructor.nestCollection(this, 'auditList', new VXAuditMapList(this.get('auditList')));
			
		}

	}, {
		// static class members
	});

    return VXResourceBase;
	
});


